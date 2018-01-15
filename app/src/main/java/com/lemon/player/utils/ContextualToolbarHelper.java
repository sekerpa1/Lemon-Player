package com.lemon.player.utils;

import android.support.annotation.NonNull;

import com.annimon.stream.Stream;
import com.lemon.player.R;
import com.lemon.player.ShuttleApplication;
import com.lemon.player.ui.modelviews.SelectableViewModel;
import com.lemon.player.ui.views.ContextualToolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextualToolbarHelper<T> {

    public interface Callback {
        void notifyItemChanged(int position, SelectableViewModel viewModel);

        void notifyDatasetChanged();
    }

    @NonNull
    private ContextualToolbar contextualToolbar;

    private boolean isActive = false;

    private boolean canChangeTitle = true;

    private Map<SelectableViewModel, T> map = new HashMap<>();

    @NonNull
    private Callback callback;

    public ContextualToolbarHelper(@NonNull ContextualToolbar contextualToolbar, @NonNull Callback callback) {
        this.contextualToolbar = contextualToolbar;
        this.callback = callback;
    }

    public void setCanChangeTitle(boolean canChangeTitle) {
        this.canChangeTitle = canChangeTitle;
    }

    /**
     * Call to show the Contextual Toolbar, and begin tracking item selections.
     */
    public void start() {
        contextualToolbar.show();
        contextualToolbar.setNavigationOnClickListener(v -> finish());
        isActive = true;
    }

    /**
     * Deselects all current items and notifies the adapter. Hides the Contextual Toolbar, removes callbacks
     * and sets isActive to false.
     */
    public void finish() {
        if (!map.isEmpty()) {
            Stream.of(map.keySet()).forEach(viewModel -> viewModel.setSelected(false));
            callback.notifyDatasetChanged();
        }

        map.clear();

        contextualToolbar.hide();

        contextualToolbar.setNavigationOnClickListener(null);

        isActive = false;
    }

    /**
     * Called ot update the toolbar's title to reflect the number of selected items.
     */
    private void updateCount() {
        if (canChangeTitle) {
            contextualToolbar.setTitle(ShuttleApplication.getInstance().getString(R.string.action_mode_selection_count, map.size()));
        }
    }

    /**
     * If the item is not present in the list, it will be added and set to 'selected', and vise-versa.
     * The selection count is updated each time this is called.
     * <p>
     * If removing the passed in item results in an empty list of selected items, finish() is called.
     *
     * @param viewModel the item to select/deselect.
     */
    private void addOrRemoveItem(SelectableViewModel viewModel, T items) {
        if (map.keySet().contains(viewModel)) {
            map.remove(viewModel);
            viewModel.setSelected(false);
        } else {
            map.put(viewModel, items);
            viewModel.setSelected(true);
        }

        updateCount();

        if (map.isEmpty()) {
            finish();
        }
    }

    /**
     * If the contextual toolbar helper is 'active', then the clicked item will be added/removed to/from
     * the list of items, the adapter will be notified, and the selected count will be updated.
     * <p>
     * If not active, nothing happens, and this method will return false.
     *
     * @param position            the position of the click
     * @param selectableViewModel the selectableViewModel which was clicked
     * @return true if the click was consumed by the ContextualToolbarHelper, else false.
     */
    public boolean handleClick(int position, SelectableViewModel selectableViewModel, T items) {
        if (isActive) {
            addOrRemoveItem(selectableViewModel, items);
            callback.notifyItemChanged(position, selectableViewModel);
            return true;
        }
        return false;
    }

    /**
     * If the contextual toolbar helper is not 'active', it will be put into the active state.
     * <p>
     * If already active, nothing happens, and this method will return false.
     *
     * @param position            the position of the long click
     * @param selectableViewModel the selectableViewModel which was clicked
     * @return true if the long press was consumed by the ContextualToolbarHelper, else false.
     */
    public boolean handleLongClick(int position, SelectableViewModel selectableViewModel, T items) {
        if (!isActive) {
            start();
            addOrRemoveItem(selectableViewModel, items);
            callback.notifyItemChanged(position, selectableViewModel);
            return true;
        }
        return false;
    }

    /**
     * @return a List of the currently selected SelectableViewModels
     */
    public List<T> getItems() {
        return new ArrayList<>(map.values());
    }
}