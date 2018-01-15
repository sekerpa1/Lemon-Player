package com.lemon.player.glide.fetcher;

import com.lemon.player.model.ArtworkProvider;

import java.io.InputStream;

public class TagFetcher extends BaseFetcher {

    String TAG = "TagFetcher";

    public TagFetcher(ArtworkProvider artworkProvider) {
        super(artworkProvider);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected InputStream getStream() {
        return artworkProvider.getTagArtwork();
    }
}