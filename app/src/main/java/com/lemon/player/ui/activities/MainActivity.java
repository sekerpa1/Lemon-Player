package com.lemon.player.ui.activities;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.afollestad.aesthetic.Aesthetic;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.greysonparrelli.permiso.Permiso;
import com.lemon.player.BuildConfig;
import com.lemon.player.IabManager;
import com.lemon.player.R;
import com.lemon.player.ShuttleApplication;
import com.lemon.player.model.Playlist;
import com.lemon.player.model.Query;
import com.lemon.player.playback.MusicService;
import com.lemon.player.sql.sqlbrite.SqlBriteUtils;
import com.lemon.player.ui.dialog.ChangelogDialog;
import com.lemon.player.ui.drawer.DrawerProvider;
import com.lemon.player.ui.drawer.EndDrawerToggle;
import com.lemon.player.ui.drawer.NavigationEventRelay;
import com.lemon.player.ui.fragments.MainController;
import com.lemon.player.utils.AnalyticsManager;
import com.lemon.player.utils.LogUtils;
import com.lemon.player.utils.MusicServiceConnectionUtils;
import com.lemon.player.utils.MusicUtils;
import com.lemon.player.utils.PlaylistUtils;
import com.lemon.player.utils.SettingsManager;
import com.lemon.player.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import test.com.androidnavigation.fragment.BackPressHandler;
import test.com.androidnavigation.fragment.BackPressListener;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends BaseCastActivity implements
        ToolbarListener,
        BackPressHandler,
        DrawerProvider {

    private static final String TAG = "MainActivity";

    private List<BackPressListener> backPressListeners = new ArrayList<>();

    private DrawerLayout drawerLayout;

    private View navigationView;

    private boolean hasPendingPlaybackRequest;

    private AdView mAdView;

    @Inject
    NavigationEventRelay navigationEventRelay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ShuttleApplication.getInstance().getAppComponent().inject(this);

        // If we haven't set any defaults, do that now
        if (Aesthetic.isFirstTime(this)) {

            ThemeUtils.Theme theme = ThemeUtils.getRandom();

            Aesthetic.get(this)
                    .activityTheme(theme.isDark ? R.style.AppTheme : R.style.AppTheme_Light)
                    .isDark(theme.isDark)
                    .colorPrimaryRes(theme.primaryColor)
                    .colorAccentRes(theme.accentColor)
                    .colorStatusBarAuto()
                    .apply();

            AnalyticsManager.logInitialTheme(theme);
        }

        setContentView(R.layout.activity_main);

        Permiso.getInstance().setActivity(this);

        navigationView = findViewById(R.id.navView);


        //View mainContainer = findViewById(R.id.mainContainer);
        View adView = findViewById(R.id.adView);

        adView.bringToFront();

        //Ensure the drawer draws a content scrim over the status bar.
        drawerLayout = findViewById(R.id.drawer_layout);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            drawerLayout.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                navigationView.dispatchApplyWindowInsets(windowInsets);
                return windowInsets.replaceSystemWindowInsets(0, 0, 0, 0);
            });
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.mainContainer, MainController.newInstance())
                    .commit();
        }

        MobileAds.initialize(this, "ca-app-pub-4292232813158643~1894833544");

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        handleIntent(getIntent());

        // Calls through to IabManager.setup()
        IabManager.getInstance();


    }

    @Override
    public void onResume() {
        super.onResume();

        showChangelogDialog();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);

        handlePendingPlaybackRequest();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Single.fromCallable(() -> {
            boolean handled = false;
            if (MusicService.ShortcutCommands.PLAYLIST.equals(intent.getAction())) {
                Playlist playlist = (Playlist) intent.getExtras().getSerializable(PlaylistUtils.ARG_PLAYLIST);
                NavigationEventRelay.NavigationEvent navigationEvent = new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.PLAYLIST_SELECTED, playlist, true);
                navigationEventRelay.sendEvent(navigationEvent);
                handled = true;
            } else if (MusicService.ShortcutCommands.FOLDERS.equals(intent.getAction())) {
                NavigationEventRelay.NavigationEvent foldersSelectedEvent = new NavigationEventRelay.NavigationEvent(NavigationEventRelay.NavigationEvent.Type.FOLDERS_SELECTED, null, true);
                navigationEventRelay.sendEvent(foldersSelectedEvent);
                handled = true;
            }

            if (!handled) {
                handlePlaybackRequest(intent);
            } else {
                setIntent(new Intent());
            }

            return true;
        })
                .delaySubscription(350, TimeUnit.MILLISECONDS)
                .subscribe();
    }

    private void handlePendingPlaybackRequest() {
        if (hasPendingPlaybackRequest) {
            handlePlaybackRequest(getIntent());
        }
    }

    @SuppressLint("CheckResult")
    private void handlePlaybackRequest(Intent intent) {
        if (intent == null) {
            return;
        } else if (MusicServiceConnectionUtils.serviceBinder == null) {
            hasPendingPlaybackRequest = true;
            return;
        }

        final Uri uri = intent.getData();
        final String mimeType = intent.getType();

        if (uri != null && uri.toString().length() > 0) {
            MusicUtils.playFile(uri);
            // Make sure to process intent only once
            setIntent(new Intent());
        } else if (MediaStore.Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
            long id = parseIdFromIntent(intent, "playlistId", "playlist");
            if (id >= 0) {
                Query query = Playlist.getQuery();
                query.uri = ContentUris.withAppendedId(query.uri, id);
                SqlBriteUtils.createSingle(this, Playlist::new, query, null)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(playlist -> {
                            MusicUtils.playAll(playlist.getSongsObservable().first(new ArrayList<>()),
                                    message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
                            // Make sure to process intent only once
                            setIntent(new Intent());
                        }, error -> LogUtils.logException(TAG, "Error handling playback request", error));
            }
        }

        hasPendingPlaybackRequest = false;
    }

    private long parseIdFromIntent(Intent intent, String longKey, String stringKey) {
        long id = intent.getLongExtra(longKey, -1);
        if (id < 0) {
            String idString = intent.getStringExtra(stringKey);
            if (idString != null) {
                try {
                    id = Long.parseLong(idString);
                } catch (NumberFormatException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return id;
    }

    private void showChangelogDialog() {
        int storedVersionCode = SettingsManager.getInstance().getStoredVersionCode();

        // If we've stored a version code in the past, and it's lower than the current version code,
        // we can show the changelog.
        // Don't show the changelog for first time users.
        if (storedVersionCode != -1 && storedVersionCode < BuildConfig.VERSION_CODE) {
            if (SettingsManager.getInstance().getShowChangelogOnLaunch()) {
                ChangelogDialog.getChangelogDialog(this).show();
            }
        }
        SettingsManager.getInstance().setVersionCode();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            if (!backPressListeners.isEmpty()) {
                for (int i = backPressListeners.size() - 1; i >= 0; i--) {
                    BackPressListener backPressListener = backPressListeners.get(i);
                    if (backPressListener.consumeBackPress()) {
                        return;
                    }
                }
            }
            super.onBackPressed();
        }
    }

    @Override
    public void toolbarAttached(Toolbar toolbar) {


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, 0);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {

               @Override
                public void onClick(View v) {
                    if (drawer.isDrawerOpen(Gravity.RIGHT)) {
                        drawer.closeDrawer(Gravity.RIGHT);
                    } else {
                        drawer.openDrawer(Gravity.RIGHT);
                   }
                }
            });
    }

    @Override
    public void addBackPressListener(@NonNull BackPressListener listener) {
        if (!backPressListeners.contains(listener)) {
            backPressListeners.add(listener);
        }
    }

    @Override
    public void removeBackPressListener(@NonNull BackPressListener listener) {
        if (backPressListeners.contains(listener)) {
            backPressListeners.remove(listener);
        }
    }

    @Override
    protected String screenName() {
        return "MainActivity";
    }

    @Override
    public DrawerLayout getDrawerLayout() {
        return drawerLayout;
    }
}