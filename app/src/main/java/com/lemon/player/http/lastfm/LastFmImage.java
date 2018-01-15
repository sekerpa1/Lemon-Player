package com.lemon.player.http.lastfm;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("WeakerAccess")
public class LastFmImage {

    @SerializedName("#text")
    public String url;

    public String size;
}