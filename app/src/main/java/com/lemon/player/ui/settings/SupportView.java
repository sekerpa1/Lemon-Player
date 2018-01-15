package com.lemon.player.ui.settings;

import android.content.Intent;

public interface SupportView {

    void setVersion(String version);

    void showFaq(Intent intent);

    void showHelp(Intent intent);

    void showRate(Intent intent);

}
