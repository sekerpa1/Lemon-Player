package com.lemon.player.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.lemon.player.BuildConfig;
import com.lemon.player.ShuttleApplication;
import com.lemon.player.ui.presenters.Presenter;
import com.lemon.player.utils.SettingsManager;
import com.lemon.player.utils.ShuttleUtils;

import javax.inject.Inject;

public class SupportPresenter extends Presenter<SupportView> {

    @Inject
    public SupportPresenter() {

    }

    @Override
    public void bindView(@NonNull SupportView view) {
        super.bindView(view);

        setAppVersion();
    }

    private void setAppVersion() {
        SupportView supportView = getView();
        if (supportView != null) {
            supportView.setVersion("Shuttle Music Player " + BuildConfig.VERSION_NAME + (ShuttleUtils.isUpgraded() ? " (Upgraded)" : " (Free)"));
        }
    }

    public void faqClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://www.shuttlemusicplayer.com/#faq"));
        SupportView supportView = getView();
        if (supportView != null) {
            supportView.showFaq(intent);
        }
    }

    public void helpClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/communities/112365043563095486408"));
        SupportView supportView = getView();
        if (supportView != null) {
            supportView.showHelp(intent);
        }
    }

    public void rateClicked() {

        SettingsManager.getInstance().setHasRated();

        SupportView supportView = getView();
        if (supportView != null) {
            Intent intent = ShuttleUtils.getShuttleStoreIntent(ShuttleApplication.getInstance().getPackageName());
            supportView.showRate(intent);
        }
    }
}