package com.lemon.player.ui.detail;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;
import com.lemon.player.R;
import com.lemon.player.model.Album;
import com.lemon.player.model.AlbumArtist;
import com.lemon.player.model.ArtworkProvider;
import com.lemon.player.model.Song;
import com.lemon.player.tagger.TaggerDialog;
import com.lemon.player.ui.dialog.BiographyDialog;
import com.lemon.player.ui.modelviews.SongView;
import com.lemon.player.utils.ArtworkDialog;
import com.lemon.player.utils.Operators;
import com.lemon.player.utils.PlaceholderProvider;
import com.lemon.player.utils.SortManager;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;

import io.reactivex.Single;

public class ArtistDetailFragment extends BaseDetailFragment {

    private static final String ARG_TRANSITION_NAME = "transition_name";

    public static String ARG_ALBUM_ARTIST = "album_artist";

    private AlbumArtist albumArtist;

    public static ArtistDetailFragment newInstance(AlbumArtist albumArtist, String transitionName) {
        Bundle args = new Bundle();
        ArtistDetailFragment fragment = new ArtistDetailFragment();
        args.putSerializable(ARG_ALBUM_ARTIST, albumArtist);
        args.putString(ARG_TRANSITION_NAME, transitionName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        albumArtist = (AlbumArtist) getArguments().getSerializable(ARG_ALBUM_ARTIST);
    }

    @Override
    void setSongSortOrder(int sortOrder) {
        SortManager.getInstance().setArtistDetailSongsSortOrder(sortOrder);
    }

    @Override
    int getSongSortOrder() {
        return SortManager.getInstance().getArtistDetailSongsSortOrder();
    }

    @Override
    void setSongsAscending(boolean ascending) {
        SortManager.getInstance().setArtistDetailSongsAscending(ascending);
    }

    @Override
    boolean getSongsAscending() {
        return SortManager.getInstance().getArtistDetailSongsAscending();
    }

    @Override
    void setAlbumSortOrder(int sortOrder) {
        SortManager.getInstance().setArtistDetailAlbumsSortOrder(sortOrder);
    }

    @Override
    int getAlbumSort() {
        return SortManager.getInstance().getArtistDetailAlbumsSortOrder();
    }

    @Override
    void setAlbumsAscending(boolean ascending) {
        SortManager.getInstance().setArtistDetailAlbumsAscending(ascending);
    }

    @Override
    boolean getAlbumsAscending() {
        return SortManager.getInstance().getArtistDetailAlbumsAscending();
    }

    @NonNull
    @Override
    public Single<List<Song>> getSongs() {
        return albumArtist.getSongsSingle().map(songs -> {
            sortSongs(songs);
            return songs;
        });
    }

    @NonNull
    @Override
    public Single<List<Album>> getAlbums() {
        return getSongs().map(Operators::songsToAlbums).map(albums -> {
            sortAlbums(albums);
            return albums;
        });
    }

    @NonNull
    @Override
    protected String getToolbarTitle() {
        return albumArtist.name;
    }

    @Override
    protected MaterialDialog getArtworkDialog() {
        return ArtworkDialog.build(getContext(), albumArtist);
    }

    @Override
    protected TaggerDialog getTaggerDialog() {
        return TaggerDialog.newInstance(albumArtist);
    }

    @Nullable
    @Override
    MaterialDialog getInfoDialog() {
        return BiographyDialog.getArtistBiographyDialog(getContext(), albumArtist.name);
    }

    @Override
    ArtworkProvider getArtworkProvider() {
        return albumArtist;
    }

    @NonNull
    @Override
    Drawable getPlaceHolderDrawable() {
        return PlaceholderProvider.getInstance().getPlaceHolderDrawable(albumArtist.name, true);
    }

    @NonNull
    @Override
    public List<ViewModel> getSongViewModels(List<Song> songs) {
        List<ViewModel> songViewModels = super.getSongViewModels(songs);
        Stream.of(songViewModels)
                .filter(viewModel -> viewModel instanceof SongView)
                .forEach(viewModel -> ((SongView) viewModel).showArtistName(false));
        return songViewModels;
    }

    @Override
    protected void setupToolbarMenu(Toolbar toolbar) {
        super.setupToolbarMenu(toolbar);

        toolbar.getMenu().findItem(R.id.editTags).setVisible(true);
        toolbar.getMenu().findItem(R.id.info).setVisible(true);
        toolbar.getMenu().findItem(R.id.artwork).setVisible(true);
    }

    @Override
    protected String screenName() {
        return "ArtistDetailFragment";
    }

    @Override
    public void showUpgradeDialog(MaterialDialog upgradeDialog) {
        upgradeDialog.show();
    }
}