package com.lemon.player.ui.appwidget;

import com.lemon.player.R;
import com.lemon.player.ui.widgets.WidgetProviderMedium;

public class WidgetConfigureMedium extends BaseWidgetConfigure {

    private static final String TAG = "WidgetConfigureMedium";

    @Override
    int[] getWidgetLayouts() {
        return new int[]{R.layout.widget_layout_medium, R.layout.widget_layout_medium_alt};
    }

    @Override
    String getLayoutIdString() {
        return WidgetProviderMedium.ARG_MEDIUM_LAYOUT_ID;
    }

    @Override
    String getUpdateCommandString() {
        return WidgetProviderMedium.CMDAPPWIDGETUPDATE;
    }

    @Override
    int getRootViewId() {
        return R.id.widget_layout_medium;
    }

    @Override
    protected String screenName() {
        return TAG;
    }
}
