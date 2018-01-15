package com.lemon.player.ui.presenters;

import android.app.Activity;

import com.lemon.player.ui.dialog.UpgradeDialog;
import com.lemon.player.ui.views.PurchaseView;

public class PurchasePresenter<V extends PurchaseView> extends Presenter<V> {

    private Activity activity;

    public PurchasePresenter(Activity activity) {
        this.activity = activity;
    }

    public void upgradeClicked() {
        PurchaseView purchaseView = getView();
        if (purchaseView != null) {
            purchaseView.showUpgradeDialog(UpgradeDialog.getUpgradeDialog(activity));
        }
    }
}