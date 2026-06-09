package com.cuzz.rookiepostbox.command.spi;

import org.bukkit.OfflinePlayer;

public interface OfflinePlayerResolver {

    OfflinePlayer resolve(String inputName);
}
