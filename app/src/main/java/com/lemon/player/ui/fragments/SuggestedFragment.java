package com.lemon.player.ui.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.bumptech.glide.RequestManager;
import com.lemon.player.R;
import com.lemon.player.ShuttleApplication;
import com.lemon.player.dagger.module.FragmentModule;
import com.lemon.player.model.Album;
import com.lemon.player.model.AlbumArtist;
import com.lemon.player.model.Playlist;
import com.lemon.player.model.Song;
import com.lemon.player.model.SuggestedHeader;
import com.lemon.player.ui.adapters.LoggingViewModelAdapter;
import com.lemon.player.ui.adapters.ViewType;
import com.lemon.player.ui.detail.PlaylistDetailFragment;
import com.lemon.player.ui.dialog.UpgradeDialog;
import com.lemon.player.ui.modelviews.AlbumView;
import com.lemon.player.ui.modelviews.EmptyView;
import com.lemon.player.ui.modelviews.HorizontalRecyclerView;
import com.lemon.player.ui.modelviews.SuggestedHeaderView;
import com.lemon.player.ui.modelviews.SuggestedSongView;
import com.lemon.player.ui.views.SuggestedDividerDecoration;
import com.lemon.player.utils.ComparisonUtils;
import com.lemon.player.utils.DataManager;
import com.lemon.player.utils.LogUtils;
import com.lemon.player.utils.MenuUtils;
import com.lemon.player.utils.MusicUtils;
import com.lemon.player.utils.Operators;
import com.lemon.player.utils.PermissionUtils;
import com.lemon.player.utils.ShuttleUtils;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class SuggestedFragment extends BaseFragment implements
        SuggestedHeaderView.ClickListener,
        AlbumView.ClickListener {

    public interface SuggestedClickListener {

        void onAlbumArtistClicked(AlbumArtist albumArtist, View transitionView);

        void onAlbumClicked(Album album, View transitionView);
    }

    public class SongClickListener implements SuggestedSongView.ClickListener {

        List<Song> songs;

        public SongClickListener(List<Song> songs) {
            this.songs = songs;
        }

        @Override
        public void onSongClick(Song song, SuggestedSongView.ViewHolder holder) {
            MusicUtils.playAll(songs, songs.indexOf(song), true, (String message) -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }

        @Override
        public boolean onSongLongClick(Song song) {
            return false;
        }

        @Override
        public void onSongOverflowClicked(View v, Song song) {
            PopupMenu popupMenu = new PopupMenu(getContext(), v);
            MenuUtils.setupSongMenu(popupMenu, false);
            popupMenu.setOnMenuItemClickListener(MenuUtils.getSongMenuClickListener(
                    getContext(),
                    song,
                    taggerDialog -> {
                        if (!ShuttleUtils.isUpgraded()) {
                            UpgradeDialog.getUpgradeDialog(getActivity()).show();
                        } else {
                            taggerDialog.show(getChildFragmentManager());
                        }
                    },
                    deleteDialog -> deleteDialog.show(getChildFragmentManager()),
                    null,
                    null));
            popupMenu.show();
        }
    }

    private static final String TAG = "SuggestedFragment";

    private static final String ARG_PAGE_TITLE = "page_title";

    private RecyclerView recyclerView;

    private ViewModelAdapter adapter;

    private CompositeDisposable refreshDisposables = new CompositeDisposable();

    @Nullable
    private Disposable setItemsDisposable;

    @Inject
    RequestManager requestManager;

    private HorizontalRecyclerView favoriteRecyclerView;
    private HorizontalRecyclerView mostPlayedRecyclerView;

    @Nullable
    private SuggestedClickListener suggestedClickListener;

    public SuggestedFragment() {
    }

    public static SuggestedFragment newInstance(String pageTitle) {
        SuggestedFragment fragment = new SuggestedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PAGE_TITLE, pageTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (getParentFragment() instanceof SuggestedClickListener) {
            suggestedClickListener = (SuggestedClickListener) getParentFragment();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        suggestedClickListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent()
                .plus(new FragmentModule(this))
                .inject(this);

        adapter = new LoggingViewModelAdapter("SuggestedFragment");
        mostPlayedRecyclerView = new HorizontalRecyclerView("SuggestedFragment - mostPlayed");
        favoriteRecyclerView = new HorizontalRecyclerView("SuggestedFragment - favorite");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (recyclerView == null) {
            recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_suggested, container, false);
            recyclerView.addItemDecoration(new SuggestedDividerDecoration(getResources()));
            recyclerView.setRecyclerListener(new RecyclerListener());

            int spanCount = ShuttleUtils.isTablet() ? 12 : 6;

            GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), spanCount);
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (!adapter.items.isEmpty() && position >= 0) {
                        ViewModel item = adapter.items.get(position);
                        if (item instanceof HorizontalRecyclerView
                                || item instanceof SuggestedHeaderView
                                || (item instanceof AlbumView && item.getViewType() == ViewType.ALBUM_LIST)
                                || (item instanceof AlbumView && item.getViewType() == ViewType.ALBUM_LIST_SMALL)
                                || item instanceof EmptyView) {
                            return spanCount;
                        }
                        if (item instanceof AlbumView && item.getViewType() == ViewType.ALBUM_CARD_LARGE) {
                            return 3;
                        }
                    }

                    return 2;
                }
            });

            recyclerView.setLayoutManager(gridLayoutManager);
        }
        if (recyclerView.getAdapter() != adapter) {
            recyclerView.setAdapter(adapter);
        }

        return recyclerView;
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshAdapterItems();
    }

    Observable<List<ViewModel>> getMostPlayedViewModels() {
        return Playlist.mostPlayedPlaylist
                .getSongsObservable()
                .map(songs -> {
                    if (!songs.isEmpty()) {
                        List<ViewModel> viewModels = new ArrayList<>();

                        SuggestedHeader mostPlayedHeader = new SuggestedHeader(getString(R.string.mostplayed), getString(R.string.suggested_most_played_songs_subtitle), Playlist.mostPlayedPlaylist);
                        SuggestedHeaderView mostPlayedHeaderView = new SuggestedHeaderView(mostPlayedHeader);
                        mostPlayedHeaderView.setClickListener(this);
                        viewModels.add(mostPlayedHeaderView);

                        viewModels.add(mostPlayedRecyclerView);

                        Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.playCount, a.playCount));
                        SongClickListener songClickListener = new SongClickListener(songs);

                        mostPlayedRecyclerView.viewModelAdapter.setItems(Stream.of(songs)
                                .map(song -> {
                                    SuggestedSongView suggestedSongView = new SuggestedSongView(song, requestManager);
                                    suggestedSongView.setClickListener(songClickListener);
                                    return (ViewModel) suggestedSongView;
                                })
                                .limit(20)
                                .toList());

                        return viewModels;
                    } else {
                        return Collections.emptyList();
                    }
                });
    }

    Observable<List<ViewModel>> getRecentlyPlayedViewModels() {
        return Playlist.recentlyPlayedPlaylist
                .getSongsObservable()
                .flatMap(songs -> Observable.just(Operators.songsToAlbums(songs)))
                .flatMapSingle(albums -> Observable.fromIterable(albums)
                        .sorted((a, b) -> ComparisonUtils.compareLong(b.lastPlayed, a.lastPlayed))
                        .flatMapSingle(album ->
                                // We need to populate the song count
                                album.getSongsSingle()
                                        .map(songs -> {
                                            album.numSongs = songs.size();
                                            return album;
                                        })
                                        .filter(a -> a.numSongs > 0)
                                        .toSingle()
                        )
                        .sorted((a, b) -> ComparisonUtils.compareLong(b.lastPlayed, a.lastPlayed))
                        .take(6)
                        .toList()
                )
                .map(albums -> {
                    if (!albums.isEmpty()) {
                        List<ViewModel> viewModels = new ArrayList<>();

                        SuggestedHeader recentlyPlayedHeader = new SuggestedHeader(getString(R.string.suggested_recent_title), getString(R.string.suggested_recent_subtitle), Playlist.recentlyPlayedPlaylist);
                        SuggestedHeaderView recentlyPlayedHeaderView = new SuggestedHeaderView(recentlyPlayedHeader);
                        recentlyPlayedHeaderView.setClickListener(this);
                        viewModels.add(recentlyPlayedHeaderView);

                        viewModels.addAll(Stream.of(albums)
                                .map(album -> {
                                    AlbumView albumView = new AlbumView(album, ViewType.ALBUM_LIST_SMALL, requestManager);
                                    albumView.setClickListener(this);
                                    return albumView;
                                }).toList());

                        return viewModels;
                    } else {
                        return Collections.emptyList();
                    }
                });
    }

    @SuppressLint("CheckResult")
    Observable<List<ViewModel>> getFavoriteSongViewModels() {

        Observable<List<Song>> favoritesSongs = DataManager.getInstance().getFavoriteSongsRelay()
                .take(20);

        return Observable.combineLatest(
                favoritesSongs,
                Playlist.favoritesPlaylist()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toObservable(),
                (songs, playlist) -> {
                    if (!songs.isEmpty()) {
                        List<ViewModel> viewModels = new ArrayList<>();

                        SuggestedHeader favoriteHeader = new SuggestedHeader(getString(R.string.fav_title), getString(R.string.suggested_favorite_subtitle), playlist);
                        SuggestedHeaderView favoriteHeaderView = new SuggestedHeaderView(favoriteHeader);
                        favoriteHeaderView.setClickListener(SuggestedFragment.this);
                        viewModels.add(favoriteHeaderView);

                        viewModels.add(favoriteRecyclerView);

                        SongClickListener songClickListener = new SongClickListener(songs);
                        favoriteRecyclerView.viewModelAdapter.setItems(Stream.of(songs).map(song -> {
                            SuggestedSongView suggestedSongView = new SuggestedSongView(song, requestManager);
                            suggestedSongView.setClickListener(songClickListener);
                            return (ViewModel) suggestedSongView;
                        }).toList());

                        return viewModels;
                    } else {
                        return Collections.emptyList();
                    }
                });
    }

    Observable<List<ViewModel>> getRecentlyAddedViewModels() {
        return Playlist.recentlyAddedPlaylist
                .getSongsObservable()
                .flatMap(songs -> Observable.just(Operators.songsToAlbums(songs)))
                .flatMapSingle(source -> Observable.fromIterable(source)
                        .sorted((a, b) -> ComparisonUtils.compareLong(b.songPlayCount, a.songPlayCount))
                        .take(20)
                        .toList())
                .map(albums -> {
                    if (!albums.isEmpty()) {
                        List<ViewModel> viewModels = new ArrayList<>();

                        SuggestedHeader recentlyAddedHeader = new SuggestedHeader(getString(R.string.recentlyadded), getString(R.string.suggested_recently_added_subtitle), Playlist.recentlyAddedPlaylist);
                        SuggestedHeaderView recentlyAddedHeaderView = new SuggestedHeaderView(recentlyAddedHeader);
                        recentlyAddedHeaderView.setClickListener(this);
                        viewModels.add(recentlyAddedHeaderView);

                        viewModels.addAll(Stream.of(albums).map(album -> {
                            AlbumView albumView = new AlbumView(album, ViewType.ALBUM_CARD, requestManager);
                            albumView.setClickListener(this);
                            return albumView;
                        }).toList());

                        return viewModels;
                    } else {
                        return Collections.emptyList();
                    }
                });
    }

    void refreshAdapterItems() {

        if (setItemsDisposable != null) {
            setItemsDisposable.dispose();
        }

        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {
                refreshDisposables.add(Observable.combineLatest(
                        getMostPlayedViewModels(),
                        getRecentlyPlayedViewModels(),
                        getFavoriteSongViewModels().switchIfEmpty(Observable.just(Collections.emptyList())),
                        getRecentlyAddedViewModels(),
                        (mostPlayedSongs1, recentlyPlayedAlbums1, favoriteSongs1, recentlyAddedAlbums1) -> {
                            List<ViewModel> items = new ArrayList<>();
                            items.addAll(mostPlayedSongs1);
                            items.addAll(recentlyPlayedAlbums1);
                            items.addAll(favoriteSongs1);
                            items.addAll(recentlyAddedAlbums1);
                            return items;
                        })
                        .debounce(200, TimeUnit.MILLISECONDS)
                        .switchIfEmpty(Observable.just(Collections.emptyList()))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(adaptableItems -> {
                            if (adaptableItems.isEmpty()) {
                                setItemsDisposable = adapter.setItems(Collections.singletonList((new EmptyView(R.string.empty_suggested))));
                            } else {
                                setItemsDisposable = adapter.setItems(adaptableItems);
                            }
                        }, error -> LogUtils.logException(TAG, "Error setting items", error)));
            }
        });
    }

    @Override
    public void onPause() {

        if (refreshDisposables != null) {
            refreshDisposables.clear();
        }

        super.onPause();
    }

    @Override
    public void onAlbumClick(int position, AlbumView albumView, AlbumView.ViewHolder viewHolder) {
        if (suggestedClickListener != null) {
            suggestedClickListener.onAlbumClicked(albumView.album, viewHolder.imageOne);
        }
    }

    @Override
    public boolean onAlbumLongClick(int position, AlbumView albumView) {
        return false;
    }

    @Override
    public void onAlbumOverflowClicked(View v, Album album) {
        PopupMenu menu = new PopupMenu(getContext(), v);
        MenuUtils.setupAlbumMenu(menu);
        menu.setOnMenuItemClickListener(MenuUtils.getAlbumMenuClickListener(getContext(), album,
                taggerDialog -> taggerDialog.show(getChildFragmentManager()),
                deleteDialog -> deleteDialog.show(getChildFragmentManager()),
                () -> UpgradeDialog.getUpgradeDialog(getActivity()).show()));
        menu.show();
    }

    @Override
    public void onSuggestedHeaderClick(SuggestedHeader suggestedHeader) {
        getNavigationController().pushViewController(PlaylistDetailFragment.newInstance(suggestedHeader.playlist), "PlaylistFragment");
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
