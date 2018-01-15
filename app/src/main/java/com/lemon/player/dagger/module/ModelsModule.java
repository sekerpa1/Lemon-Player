package com.lemon.player.dagger.module;

import android.support.annotation.NonNull;

import com.lemon.player.model.PlaylistsModel;

import dagger.Module;
import dagger.Provides;

@Module
public class ModelsModule {

    @Provides
    @NonNull
    public PlaylistsModel providePlaylistsModel() {
        return new PlaylistsModel();
    }

}