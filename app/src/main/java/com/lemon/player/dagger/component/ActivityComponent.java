package com.lemon.player.dagger.component;

import com.lemon.player.dagger.module.ActivityModule;
import com.lemon.player.dagger.module.PresenterModule;
import com.lemon.player.dagger.scope.ActivityScope;
import com.lemon.player.ui.drawer.DrawerFragment;
import com.lemon.player.ui.settings.SettingsParentFragment;

import dagger.Subcomponent;

@ActivityScope
@Subcomponent(modules = {
        ActivityModule.class,
        PresenterModule.class})

public interface ActivityComponent {

    void inject(DrawerFragment target);

    void inject(SettingsParentFragment.SettingsFragment target);
}