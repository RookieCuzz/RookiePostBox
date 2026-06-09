package com.cuzz.rookiepostbox.service.spi;

import com.cuzz.rookiepostbox.service.ClaimPackageAttachmentsResult;
import com.cuzz.rookiepostbox.service.ClaimPackageResult;
import com.cuzz.rookiepostbox.service.MailboxMailView;
import com.cuzz.rookiepostbox.service.SendMailResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public interface MailboxService {

    List<MailboxMailView> getInbox(Player player);

    boolean saveMainHandToSelf(Player player, String message);

    default SendMailResult sendMail(UUID senderUuid, String senderName, UUID receiverUuid, String receiverName, String message, ItemStack itemStack) {
        return sendMail(senderUuid, senderName, receiverUuid, receiverName, message, itemStack, UUID.randomUUID());
    }

    SendMailResult sendMail(UUID senderUuid, String senderName, UUID receiverUuid, String receiverName, String message, ItemStack itemStack, UUID requestId);

    SendMailResult sendMail(UUID senderUuid, String senderName, UUID receiverUuid, String receiverName, String message, List<ItemStack> itemStacks, UUID requestId);

    ClaimPackageResult claimPackage(Player player, String packageId);

    ClaimPackageAttachmentsResult previewPackageAttachments(Player player, String packageId);

    @Deprecated(forRemoval = false)
    ClaimPackageAttachmentsResult claimPackageAttachments(Player player, String packageId);
}
