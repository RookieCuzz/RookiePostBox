package com.cuzz.rookiepostbox.service;


import com.cuzz.rookiepostbox.service.spi.MailboxService;
import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Service;
import com.cuzz.rookiepostbox.domain.enumtype.MailSenderType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

@Service
public final class PostgresMailboxService implements MailboxService {

    private final MailInboxService mailInboxService;
    private final MailWriteSupport mailWriteSupport;
    private final MailClaimService mailClaimService;

    @Autowired
    public PostgresMailboxService(
            MailInboxService mailInboxService,
            MailWriteSupport mailWriteSupport,
            MailClaimService mailClaimService
    ) {
        this.mailInboxService = mailInboxService;
        this.mailWriteSupport = mailWriteSupport;
        this.mailClaimService = mailClaimService;
    }

    @Override
    public List<MailboxMailView> getInbox(Player player) {
        return mailInboxService.getInbox(player);
    }

    @Override
    public boolean saveMainHandToSelf(Player player, String message) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }
        return sendMail(player.getUniqueId(), player.getName(), player.getUniqueId(), player.getName(), message, itemStack).isSuccess();
    }

    @Override
    public SendMailResult sendMail(UUID senderUuid, String senderName, UUID receiverUuid, String receiverName, String message, ItemStack itemStack, UUID requestId) {
        return mailWriteSupport.createSingleItemMail(MailCreateRequest.builder()
                .mailboxOwnerUuid(receiverUuid)
                .senderUuid(senderUuid)
                .senderName(senderName)
                .senderType(MailSenderType.PLAYER)
                .receiverName(receiverName)
                .message(message)
                .itemStack(itemStack)
                .requestId(requestId)
                .sourcePlugin("RookiePostBox")
                .storeId("player-send")
                .build());
    }

    @Override
    public SendMailResult sendMail(UUID senderUuid, String senderName, UUID receiverUuid, String receiverName, String message, List<ItemStack> itemStacks, UUID requestId) {
        return mailWriteSupport.createMail(MailCreateRequest.builder()
                .mailboxOwnerUuid(receiverUuid)
                .senderUuid(senderUuid)
                .senderName(senderName)
                .senderType(MailSenderType.PLAYER)
                .receiverName(receiverName)
                .message(message)
                .itemStacks(itemStacks)
                .requestId(requestId)
                .sourcePlugin("RookiePostBox")
                .storeId("player-send")
                .build());
    }

    @Override
    public ClaimPackageResult claimPackage(Player player, String packageId) {
        return mailClaimService.claimPackage(player, packageId);
    }

    @Override
    public ClaimPackageAttachmentsResult previewPackageAttachments(Player player, String packageId) {
        return mailClaimService.previewPackageAttachments(player, packageId);
    }

    @Override
    @Deprecated(forRemoval = false)
    public ClaimPackageAttachmentsResult claimPackageAttachments(Player player, String packageId) {
        return mailClaimService.claimPackageAttachments(player, packageId);
    }
}
