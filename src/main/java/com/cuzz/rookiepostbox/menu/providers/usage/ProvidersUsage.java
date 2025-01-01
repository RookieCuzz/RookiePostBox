package com.cuzz.rookiepostbox.menu.providers.usage;

import com.cuzz.rookiepostbox.menu.providers.CustomColorProvider;
import com.cuzz.rookiepostbox.menu.providers.CustomCooldownProvider;
import com.cuzz.rookiepostbox.menu.providers.CustomDefaultItemProvider;
import nl.odalitadevelopments.menus.OdalitaMenus;


public final class ProvidersUsage {

    // Example method to register providers for this instance
    public void registerProviders(OdalitaMenus instance) {
        instance.getProvidersContainer().setColorProvider(new CustomColorProvider());
        instance.getProvidersContainer().setCooldownProvider(new CustomCooldownProvider());
        instance.getProvidersContainer().setDefaultItemProvider(new CustomDefaultItemProvider());
    }
}