package com.lemon.player.ui.detail;

import android.support.annotation.NonNull;

import com.lemon.player.model.Album;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;

import io.reactivex.Single;

public interface AlbumsProvider {

    @NonNull
    Single<List<Album>> getAlbums();

    @NonNull
    List<ViewModel> getAlbumViewModels(List<Album> albums);

}