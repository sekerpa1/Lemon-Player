package com.lemon.player.model;

import android.support.annotation.Nullable;

import com.lemon.player.http.itunes.ItunesResult;
import com.lemon.player.http.lastfm.LastFmResult;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import retrofit2.Call;

public interface ArtworkProvider {

    @interface Type {
        int MEDIA_STORE = 0;
        int TAG = 1;
        int FOLDER = 2;
        int LAST_FM = 3;
        int ITUNES = 4;
    }

    String getArtworkKey();

    @Nullable
    Call<? extends LastFmResult> getLastFmArtwork();

    @Nullable
    Call<ItunesResult> getItunesArtwork();

    @Nullable
    InputStream getMediaStoreArtwork();

    @Nullable
    InputStream getFolderArtwork();

    @Nullable
    InputStream getTagArtwork();

    @Nullable
    List<File> getFolderArtworkFiles();
}