package com.lemon.player.dagger.module;

import com.lemon.player.search.SearchPresenter;
import com.lemon.player.search.SearchView;
import com.lemon.player.ui.presenters.PlayerPresenter;
import com.lemon.player.ui.presenters.Presenter;
import com.lemon.player.ui.presenters.QueuePagerPresenter;
import com.lemon.player.ui.presenters.QueuePresenter;
import com.lemon.player.ui.views.PlayerView;
import com.lemon.player.ui.views.QueuePagerView;
import com.lemon.player.ui.views.QueueView;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class PresenterModule {

    @Binds
    abstract Presenter<PlayerView> bindPlayerPresenter(PlayerPresenter playerPresenter);

    @Binds
    abstract Presenter<QueuePagerView> bindQueuePagerPresenter(QueuePagerPresenter queuePagerPresenter);

    @Binds
    abstract Presenter<QueueView> bindQueuePresenter(QueuePresenter queuePresenter);

    @Binds
    abstract Presenter<SearchView> bindSearchPresenter(SearchPresenter queuePresenter);
}