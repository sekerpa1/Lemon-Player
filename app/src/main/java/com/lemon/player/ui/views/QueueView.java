package com.lemon.player.ui.views;

import com.lemon.player.tagger.TaggerDialog;
import com.lemon.player.ui.dialog.DeleteDialog;
import com.lemon.player.ui.modelviews.SongView;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;

public interface QueueView {

    void loadData(List<ViewModel> items, int position);

    void updateQueuePosition(int position, boolean fromUser);

    void showToast(String message, int duration);

    void startDrag(SongView.ViewHolder holder);

    void showTaggerDialog(TaggerDialog taggerDialog);

    void showDeleteDialog(DeleteDialog deleteDialog);

    void removeFromQueue(int position);

    void moveQueueItem(int from, int to);

    void showUpgradeDialog();
}