package com.lemon.player.ui.fragments;

import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.cantrowitz.rxbroadcast.RxBroadcast;
import com.lemon.player.R;
import com.lemon.player.ShuttleApplication;
import com.lemon.player.model.Album;
import com.lemon.player.model.AlbumArtist;
import com.lemon.player.model.Genre;
import com.lemon.player.model.Playlist;
import com.lemon.player.playback.MusicService;
import com.lemon.player.rx.UnsafeAction;
import com.lemon.player.ui.detail.AlbumDetailFragment;
import com.lemon.player.ui.detail.ArtistDetailFragment;
import com.lemon.player.ui.detail.GenreDetailFragment;
import com.lemon.player.ui.detail.PlaylistDetailFragment;
import com.lemon.player.ui.drawer.DrawerLockController;
import com.lemon.player.ui.drawer.DrawerLockManager;
import com.lemon.player.ui.drawer.DrawerProvider;
import com.lemon.player.ui.drawer.MiniPlayerLockManager;
import com.lemon.player.ui.drawer.NavigationEventRelay;
import com.lemon.player.ui.settings.SettingsParentFragment;
import com.lemon.player.ui.views.UpNextView;
import com.lemon.player.ui.views.multisheet.CustomMultiSheetView;
import com.lemon.player.ui.views.multisheet.MultiSheetEventRelay;
import com.lemon.player.utils.MusicUtils;
import com.lemon.player.utils.SleepTimer;
import com.lemon.multisheetview.ui.view.MultiSheetView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import test.com.androidnavigation.fragment.BackPressHandler;
import test.com.androidnavigation.fragment.BaseNavigationController;
import test.com.androidnavigation.fragment.FragmentInfo;

public class MainController extends BaseNavigationController implements BackPressHandler, DrawerLockController {

    private static final String TAG = "MainController";

    public static final String STATE_CURRENT_SHEET = "current_sheet";

    @Inject
    NavigationEventRelay navigationEventRelay;

    @Inject
    MultiSheetEventRelay multiSheetEventRelay;

    private Handler delayHandler;

    @BindView(R.id.multiSheetView)
    CustomMultiSheetView multiSheetView;

    private CompositeDisposable disposables = new CompositeDisposable();

    public static MainController newInstance() {
        Bundle args = new Bundle();
        MainController fragment = new MainController();
        fragment.setArguments(args);
        return fragment;
    }

    public MainController() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ButterKnife.bind(this, rootView);

        ShuttleApplication.getInstance().getAppComponent().inject(this);

        if (savedInstanceState == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .add(multiSheetView.getSheetContainerViewResId(MultiSheetView.Sheet.FIRST), PlayerFragment.newInstance())
                    .add(multiSheetView.getSheetPeekViewResId(MultiSheetView.Sheet.FIRST), MiniPlayerFragment.newInstance())
                    .add(multiSheetView.getSheetContainerViewResId(MultiSheetView.Sheet.SECOND), QueueFragment.newInstance())
                    .commit();
        } else {
            multiSheetView.restoreSheet(savedInstanceState.getInt(STATE_CURRENT_SHEET));
        }

        ((ViewGroup) multiSheetView.findViewById(multiSheetView.getSheetPeekViewResId(MultiSheetView.Sheet.SECOND))).addView(new UpNextView(getContext()));

        toggleBottomSheetVisibility(false, false);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (delayHandler != null) {
            delayHandler.removeCallbacksAndMessages(null);
        }
        delayHandler = new Handler();

        disposables.add(navigationEventRelay.getEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .filter(NavigationEventRelay.NavigationEvent::isActionable)
                .subscribe(navigationEvent -> {
                    switch (navigationEvent.type) {
                        case NavigationEventRelay.NavigationEvent.Type.LIBRARY_SELECTED:
                            popToRootViewController();
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.FOLDERS_SELECTED:
                            delayHandler.postDelayed(() -> pushViewController(FolderFragment.newInstance(getString(R.string.folders_title), false), "FolderFragment"), 250);
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.SLEEP_TIMER_SELECTED:
                            UnsafeAction showToast = () -> Toast.makeText(getContext(), R.string.sleep_timer_started, Toast.LENGTH_SHORT).show();
                            SleepTimer.getInstance().getDialog(
                                    getContext(),
                                    () -> SleepTimer.getInstance().showMinutesDialog(getContext(), showToast),
                                    showToast
                            ).show();
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.EQUALIZER_SELECTED:
                            delayHandler.postDelayed(() -> multiSheetEventRelay.sendEvent(new MultiSheetEventRelay.MultiSheetEvent(MultiSheetEventRelay.MultiSheetEvent.Action.HIDE, MultiSheetView.Sheet.FIRST)), 100);
                            delayHandler.postDelayed(() -> pushViewController(EqualizerFragment.newInstance(), "EqualizerFragment"), 250);
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.SETTINGS_SELECTED:
                            delayHandler.postDelayed(() -> multiSheetEventRelay.sendEvent(new MultiSheetEventRelay.MultiSheetEvent(MultiSheetEventRelay.MultiSheetEvent.Action.HIDE, MultiSheetView.Sheet.FIRST)), 100);
                            delayHandler.postDelayed(() -> pushViewController(SettingsParentFragment.newInstance(R.xml.settings_headers, R.string.settings), "Settings Fragment"), 250);
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.SUPPORT_SELECTED:
                            delayHandler.postDelayed(() -> multiSheetEventRelay.sendEvent(new MultiSheetEventRelay.MultiSheetEvent(MultiSheetEventRelay.MultiSheetEvent.Action.HIDE, MultiSheetView.Sheet.FIRST)), 100);
                            delayHandler.postDelayed(() -> pushViewController(SettingsParentFragment.newInstance(R.xml.settings_support, R.string.pref_title_support), "Support Fragment"), 250);
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.PLAYLIST_SELECTED:
                            delayHandler.postDelayed(() -> pushViewController(PlaylistDetailFragment.newInstance((Playlist) navigationEvent.data), "PlaylistDetailFragment"), 250);
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.GO_TO_ARTIST:
                            multiSheetView.goToSheet(MultiSheetView.Sheet.NONE);
                            AlbumArtist albumArtist = (AlbumArtist) navigationEvent.data;
                            delayHandler.postDelayed(() -> {
                                popToRootViewController();
                                pushViewController(ArtistDetailFragment.newInstance(albumArtist, null), "ArtistDetailFragment");
                            }, 250);
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.GO_TO_ALBUM:
                            multiSheetView.goToSheet(MultiSheetView.Sheet.NONE);
                            Album album = (Album) navigationEvent.data;
                            delayHandler.postDelayed(() -> {
                                popToRootViewController();
                                pushViewController(AlbumDetailFragment.newInstance(album, null), "AlbumDetailFragment");
                            }, 250);
                            break;
                        case NavigationEventRelay.NavigationEvent.Type.GO_TO_GENRE:
                            multiSheetView.goToSheet(MultiSheetView.Sheet.NONE);
                            Genre genre = (Genre) navigationEvent.data;
                            delayHandler.postDelayed(() -> {
                                popToRootViewController();
                                pushViewController(GenreDetailFragment.newInstance(genre), "GenreDetailFragment");
                            }, 250);
                            break;

                    }
                }));

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicService.InternalIntents.SERVICE_CONNECTED);
        intentFilter.addAction(MusicService.InternalIntents.QUEUE_CHANGED);
        disposables.add(
                RxBroadcast.fromBroadcast(getContext(), intentFilter)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(intent -> toggleBottomSheetVisibility(true, true))
        );

        DrawerLockManager.getInstance().setDrawerLockController(this);
    }

    @Override
    public void onPause() {
        delayHandler.removeCallbacksAndMessages(null);
        delayHandler = null;

        disposables.clear();

        DrawerLockManager.getInstance().setDrawerLockController(null);

        super.onPause();
    }

    /**
     * Hide/show the bottom sheet, depending on whether the queue is empty.
     */
    private void toggleBottomSheetVisibility(boolean collapse, boolean animate) {
        if (MusicUtils.getQueue().isEmpty()) {
            multiSheetView.hide(collapse, false);
        } else if (MiniPlayerLockManager.getInstance().canShowMiniPlayer()) {
            multiSheetView.unhide(animate);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_SHEET, multiSheetView.getCurrentSheet());
        super.onSaveInstanceState(outState);
    }

    @Override
    public FragmentInfo getRootViewControllerInfo() {
        return LibraryController.fragmentInfo();
    }

    @Override
    public boolean consumeBackPress() {
        if (multiSheetView.consumeBackPress()) {
            return true;
        }

        return super.consumeBackPress();
    }

    @Override
    public void lockDrawer() {
        ((DrawerProvider) getActivity()).getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    public void unlockDrawer() {
        // Don't unlock the drawer if one of the sheets is expanded
        if (multiSheetView.getCurrentSheet() == MultiSheetView.Sheet.FIRST || multiSheetView.getCurrentSheet() == MultiSheetView.Sheet.SECOND) {
            return;
        }

        ((DrawerProvider) getActivity()).getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }
}