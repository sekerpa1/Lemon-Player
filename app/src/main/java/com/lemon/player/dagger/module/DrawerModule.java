package com.lemon.player.dagger.module;

import com.lemon.player.ui.drawer.NavigationEventRelay;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class DrawerModule {

    @Provides
    @Singleton
    NavigationEventRelay provideDrawerEventRelay() {
        return new NavigationEventRelay();
    }

}