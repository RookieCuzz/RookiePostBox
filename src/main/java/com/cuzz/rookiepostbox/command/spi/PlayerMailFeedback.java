package com.cuzz.rookiepostbox.command.spi;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PlayerMailFeedback {

    void notifySaved(Player player, ItemStack itemStack, String message);

    void notifySent(Player player, ItemStack itemStack, String receiverName, String message);
}
