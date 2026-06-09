package com.cuzz.rookiepostbox.command;


import com.cuzz.rookiepostbox.command.spi.OfflinePlayerResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

final class BukkitOfflinePlayerResolver implements OfflinePlayerResolver {

    @Override
    public OfflinePlayer resolve(String inputName) {
        Player onlinePlayer = Bukkit.getPlayerExact(inputName);
        if (onlinePlayer != null) {
            return onlinePlayer;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(inputName);
        if (!offlinePlayer.isOnline() && !offlinePlayer.hasPlayedBefore()) {
            return null;
        }
        return offlinePlayer;
    }
}
