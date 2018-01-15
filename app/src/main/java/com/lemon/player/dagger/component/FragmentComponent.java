package com.lemon.player.dagger.component;

import com.lemon.player.dagger.module.FragmentModule;
import com.lemon.player.dagger.module.PresenterModule;
import com.lemon.player.dagger.scope.FragmentScope;
import com.lemon.player.search.SearchFragment;
import com.lemon.player.ui.fragments.AlbumArtistFragment;
import com.lemon.player.ui.fragments.AlbumFragment;
import com.lemon.player.ui.fragments.BaseFragment;
import com.lemon.player.ui.fragments.MiniPlayerFragment;
import com.lemon.player.ui.fragments.PlayerFragment;
import com.lemon.player.ui.fragments.QueueFragment;
import com.lemon.player.ui.fragments.QueuePagerFragment;
import com.lemon.player.ui.fragments.SuggestedFragment;
import com.lemon.player.ui.presenters.PlayerPresenter;

import dagger.Subcomponent;

@FragmentScope
@Subcomponent(modules = {
        FragmentModule.class,
        PresenterModule.class})

public interface FragmentComponent {

    void inject(BaseFragment target);

    void inject(PlayerFragment target);

    void inject(MiniPlayerFragment target);

    void inject(PlayerPresenter target);

    void inject(QueuePagerFragment target);

    void inject(QueueFragment target);

    void inject(AlbumArtistFragment target);

    void inject(AlbumFragment target);

    void inject(SuggestedFragment target);

    void inject(SearchFragment target);
}