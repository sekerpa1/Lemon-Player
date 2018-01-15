package com.lemon.player.ui.dialog;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.lemon.player.IabManager;
import com.lemon.player.R;
import com.lemon.player.ui.activities.MainActivity;
import com.lemon.player.utils.ShuttleUtils;

public class UpgradeDialog {

    private UpgradeDialog() {
        //no instance
    }

    public static MaterialDialog getUpgradeDialog(@NonNull Activity activity) {
        return new MaterialDialog.Builder(activity)
                .title(activity.getResources().getString(R.string.get_pro_title))
                .content(activity.getResources().getString(R.string.upgrade_dialog_message))
                .positiveText(R.string.btn_upgrade)
                .onPositive((dialog, which) -> {
                    if (ShuttleUtils.isAmazonBuild()) {
                        activity.startActivity(ShuttleUtils.getShuttleStoreIntent("com.lemon.player"));
                    } else {
                        purchaseUpgrade(activity);
                    }
                })
                .negativeText(R.string.get_pro_button_no)
                .build();
    }

    private static void purchaseUpgrade(@NonNull Activity activity) {
        IabManager.getInstance().purchaseUpgrade(activity, success -> {
            if (success) {
                getUpgradeSuccessDialog(activity).show();
            } else {
                Toast.makeText(activity, R.string.iab_purchase_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static MaterialDialog getUpgradeSuccessDialog(@NonNull Activity activity) {
        return new MaterialDialog.Builder(activity)
                .title(activity.getResources().getString(R.string.upgraded_title))
                .content(activity.getResources().getString(R.string.upgraded_message))
                .positiveText(R.string.restart_button)
                .onPositive((materialDialog, dialogAction) -> {
                    Intent intent = new Intent(activity, MainActivity.class);
                    ComponentName componentName = intent.getComponent();
                    Intent mainIntent = Intent.makeRestartActivityTask(componentName);
                    activity.startActivity(mainIntent);
                })
                .build();
    }
}