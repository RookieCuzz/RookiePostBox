package com.cuzz.rookiepostbox.command;


import com.cuzz.rookiepostbox.command.spi.PlayerMailFeedback;
import com.cuzz.rookiepostbox.nms.Toast;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class ToastPlayerMailFeedback implements PlayerMailFeedback {

    @Override
    public void notifySaved(Player player, ItemStack itemStack, String message) {
        Toast.displayTo(
                player,
                itemStack.getType().toString().toLowerCase(),
                "Saved to mailbox: " + message,
                Toast.Style.TASK
        );
    }

    @Override
    public void notifySent(Player player, ItemStack itemStack, String receiverName, String message) {
        Toast.displayTo(
                player,
                itemStack.getType().toString().toLowerCase(),
                "Sent to " + receiverName + ": " + message,
                Toast.Style.TASK
        );
    }
}
