package com.lemon.player.ui.drawer;

import com.lemon.player.ui.views.PurchaseView;

import java.util.List;

public interface DrawerView extends PurchaseView {

    void setPlaylistItems(List<DrawerChild> drawerChildren);

    void closeDrawer();

    void setDrawerItemSelected(@DrawerParent.Type int type);
    }
