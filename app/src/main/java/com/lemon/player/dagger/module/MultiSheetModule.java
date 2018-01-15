package com.lemon.player.dagger.module;

import com.lemon.player.ui.views.multisheet.MultiSheetEventRelay;
import com.lemon.player.ui.views.multisheet.MultiSheetSlideEventRelay;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class MultiSheetModule {

    @Provides
    @Singleton
    MultiSheetEventRelay provideMultiSheetEventRelay() {
        return new MultiSheetEventRelay();
    }

    @Provides
    @Singleton
    MultiSheetSlideEventRelay provideMultiSheetSlideEventRelay() {
        return new MultiSheetSlideEventRelay();
    }

}