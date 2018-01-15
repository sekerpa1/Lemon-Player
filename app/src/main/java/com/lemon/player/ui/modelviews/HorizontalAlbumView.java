package com.lemon.player.ui.modelviews;

import com.bumptech.glide.RequestManager;
import com.lemon.player.R;
import com.lemon.player.model.Album;
import com.lemon.player.ui.adapters.ViewType;

public class HorizontalAlbumView extends AlbumView {

    public HorizontalAlbumView(Album album, RequestManager requestManager) {
        super(album, ViewType.ALBUM_CARD, requestManager);
    }

    @Override
    public int getLayoutResId() {
        return R.layout.grid_item_horizontal;
    }
}