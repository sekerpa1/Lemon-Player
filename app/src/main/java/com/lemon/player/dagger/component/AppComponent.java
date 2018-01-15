package com.lemon.player.dagger.component;


import com.lemon.player.dagger.module.AppModule;
import com.lemon.player.dagger.module.DrawerModule;
import com.lemon.player.dagger.module.FragmentModule;
import com.lemon.player.dagger.module.ActivityModule;
import com.lemon.player.dagger.module.ModelsModule;
import com.lemon.player.dagger.module.MultiSheetModule;
import com.lemon.player.ui.activities.MainActivity;
import com.lemon.player.ui.fragments.LibraryController;
import com.lemon.player.ui.fragments.MainController;
import com.lemon.player.ui.views.UpNextView;
import com.lemon.player.ui.views.multisheet.CustomMultiSheetView;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppModule.class,
        ModelsModule.class,
        MultiSheetModule.class,
        DrawerModule.class})

public interface AppComponent {

    FragmentComponent plus(FragmentModule module);

    ActivityComponent plus(ActivityModule module);

    void inject(MainActivity target);

    void inject(CustomMultiSheetView target);

    void inject(MainController target);

    void inject(LibraryController target);

    void inject(UpNextView target);
}