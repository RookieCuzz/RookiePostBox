package com.cuzz.rookiepostbox.service;

import com.cuzz.bukkitspring.api.annotation.Service;
import com.cuzz.rookiepostbox.RookiePostBox;
import com.cuzz.rookiepostbox.nms.Toast;
import com.cuzz.rookiepostbox.service.spi.MailDeliveryNotificationService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Service
public final class ToastMailDeliveryNotificationService implements MailDeliveryNotificationService {

    @Override
    public void notifyMailCreated(MailCreateRequest request, List<ItemStack> attachments, long packageId) {
        if (request == null || request.getMailboxOwnerUuid() == null) {
            return;
        }

        Runnable notification = () -> notifyOnlineReceiver(request, attachments);
        if (Bukkit.isPrimaryThread()) {
            notification.run();
            return;
        }
        Bukkit.getScheduler().runTask(RookiePostBox.getInstance(), notification);
    }

    private void notifyOnlineReceiver(MailCreateRequest request, List<ItemStack> attachments) {
        Player receiver = Bukkit.getPlayer(request.getMailboxOwnerUuid());
        if (receiver == null || !receiver.isOnline()) {
            return;
        }

        try {
            Toast.displayTo(
                    receiver,
                    iconName(attachments),
                    "New mail|From " + senderName(request),
                    Toast.Style.TASK
            );
        } catch (RuntimeException exception) {
            RookiePostBox.getInstance().getLogger().warning("Failed to show mail toast to " + receiver.getName() + ": " + exception.getMessage());
        }
    }

    private String senderName(MailCreateRequest request) {
        String senderName = request.getSenderName();
        return senderName == null || senderName.isBlank() ? "System" : senderName;
    }

    private String iconName(List<ItemStack> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return "bundle";
        }

        ItemStack firstAttachment = attachments.get(0);
        if (firstAttachment == null || firstAttachment.getType() == Material.AIR) {
            return "bundle";
        }
        return firstAttachment.getType().getKey().getKey();
    }
}
