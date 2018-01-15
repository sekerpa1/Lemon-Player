package com.lemon.player.ui.views;


import android.support.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.lemon.player.model.Song;
import com.lemon.player.playback.MusicService;
import com.lemon.player.tagger.TaggerDialog;

public interface PlayerView {

    void setSeekProgress(int progress);

    void currentTimeVisibilityChanged(boolean visible);

    void currentTimeChanged(long seconds);

    void totalTimeChanged(long seconds);

    void queueChanged(int queuePosition, int queueLength);

    void playbackChanged(boolean isPlaying);

    void shuffleChanged(@MusicService.ShuffleMode int shuffleMode);

    void repeatChanged(@MusicService.RepeatMode int repeatMode);

    void favoriteChanged(boolean isFavorite);

    void trackInfoChanged(@Nullable Song song);

    void showToast(String message, int duration);

    void showLyricsDialog(MaterialDialog dialog);

    void showTaggerDialog(TaggerDialog taggerDialog);

    void showSongInfoDialog(MaterialDialog dialog);

    void showUpgradeDialog(MaterialDialog dialog);
}