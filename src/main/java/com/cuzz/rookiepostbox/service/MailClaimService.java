package com.cuzz.rookiepostbox.service;



import com.cuzz.rookiepostbox.service.spi.ItemSerializationService;
import com.cuzz.rookiepostbox.service.spi.MailAuditService;
import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Service;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.domain.entity.MailItemRecord;
import com.cuzz.rookiepostbox.domain.entity.MailPackageRecord;
import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.repository.spi.MailPackageRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public final class MailClaimService {

    private final MailPackageRepository mailPackageRepository;
    private final MailboxClaimLockManager claimLockManager;
    private final RookiePostBoxProperties properties;
    private final MailStateService mailStateService;
    private final MailAuditService mailAuditService;
    private final ItemSerializationService itemSerializationService;

    @Autowired
    public MailClaimService(
            MailPackageRepository mailPackageRepository,
            MailboxClaimLockManager claimLockManager,
            RookiePostBoxProperties properties,
            MailStateService mailStateService,
            MailAuditService mailAuditService,
            ItemSerializationService itemSerializationService
    ) {
        this.mailPackageRepository = mailPackageRepository;
        this.claimLockManager = claimLockManager;
        this.properties = properties;
        this.mailStateService = mailStateService;
        this.mailAuditService = mailAuditService;
        this.itemSerializationService = itemSerializationService;
    }

    public ClaimPackageResult claimPackage(Player player, String packageId) {
        long numericPackageId;
        try {
            numericPackageId = Long.parseLong(packageId);
        } catch (NumberFormatException exception) {
            return failure(packageId, "Mailbox >>> Invalid package id.");
        }

        mailStateService.runMaintenanceCycle();

        if (!claimLockManager.tryLock(numericPackageId)) {
            return failure(packageId, "Mailbox >>> This package is already being processed.");
        }

        try {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            MailPackageRecord packageRecord = mailPackageRepository.findById(numericPackageId).orElse(null);
            if (packageRecord == null) {
                return failure(packageId, "Mailbox >>> Package does not exist.");
            }
            if (!player.getUniqueId().equals(packageRecord.getMailboxOwnerUuid())) {
                return failure(packageId, "Mailbox >>> You do not own this package.");
            }
            if (isExpired(packageRecord, now)) {
                expireIfNeeded(packageRecord, now);
                return failure(packageId, "Mailbox >>> Package has expired.");
            }
            if (!isClaimable(packageRecord.getLifecycleState())) {
                return failure(packageId, "Mailbox >>> Package is not claimable.");
            }

            List<MailItemRecord> items = mailPackageRepository.findItems(numericPackageId);
            if (items.isEmpty()) {
                return failure(packageId, "Mailbox >>> Package has no attachments.");
            }

            PlayerInventory inventory = player.getInventory();
            if (!hasEnoughSpace(inventory, items.size())) {
                return ClaimPackageResult.builder()
                        .success(false)
                        .inventoryFull(true)
                        .packageId(packageId)
                        .message("Mailbox >>> Not enough inventory space.")
                        .build();
            }

            UUID claimToken = UUID.randomUUID();
            boolean reserved = mailPackageRepository.updateLifecycleState(
                    numericPackageId,
                    packageRecord.getLifecycleState(),
                    MailLifecycleState.CLAIMING,
                    claimToken,
                    now
            );
            if (!reserved) {
                return failure(packageId, "Mailbox >>> Package state changed. Reopen the mailbox.");
            }
            mailAuditService.recordClaimStarted(numericPackageId, player.getUniqueId(), player.getName(), claimToken);

            try {
                for (MailItemRecord item : items) {
                    ItemStack itemStack = itemSerializationService.deserialize(item.getBase64Item());
                    inventory.addItem(itemStack);
                    player.sendMessage("Mailbox >>> Received " + item.getDisplayName() + " x " + item.getAmount());
                }
            } catch (RuntimeException exception) {
                rollbackClaim(numericPackageId, claimToken, player.getUniqueId(), player.getName(), exception.getMessage());
                throw exception;
            }

            boolean claimed = mailPackageRepository.updateLifecycleState(
                    numericPackageId,
                    MailLifecycleState.CLAIMING,
                    MailLifecycleState.CLAIMED,
                    claimToken,
                    OffsetDateTime.now(ZoneOffset.UTC)
            );
            if (!claimed) {
                mailAuditService.recordClaimFailed(numericPackageId, player.getUniqueId(), player.getName(), claimToken, "claim-commit-failed");
                return failure(packageId, "Mailbox >>> Claim commit failed.");
            }

            mailAuditService.recordClaimed(numericPackageId, player.getUniqueId(), player.getName(), claimToken);
            return ClaimPackageResult.builder()
                    .success(true)
                    .inventoryFull(false)
                    .packageId(packageId)
                    .message("Mailbox >>> Claim successful.")
                    .build();
        } finally {
            claimLockManager.unlock(numericPackageId);
        }
    }

    public ClaimPackageAttachmentsResult previewPackageAttachments(Player player, String packageId) {
        long numericPackageId;
        try {
            numericPackageId = Long.parseLong(packageId);
        } catch (NumberFormatException exception) {
            return attachmentsFailure(packageId, "Mailbox >>> Invalid package id.");
        }

        mailStateService.runMaintenanceCycle();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        MailPackageRecord packageRecord = mailPackageRepository.findById(numericPackageId).orElse(null);
        if (packageRecord == null) {
            return attachmentsFailure(packageId, "Mailbox >>> Package does not exist.");
        }
        if (!player.getUniqueId().equals(packageRecord.getMailboxOwnerUuid())) {
            return attachmentsFailure(packageId, "Mailbox >>> You do not own this package.");
        }
        if (isExpired(packageRecord, now)) {
            expireIfNeeded(packageRecord, now);
            return attachmentsFailure(packageId, "Mailbox >>> Package has expired.");
        }
        if (!isClaimable(packageRecord.getLifecycleState())) {
            return attachmentsFailure(packageId, "Mailbox >>> Package is not claimable.");
        }

        List<MailItemRecord> items = mailPackageRepository.findItems(numericPackageId);
        if (items.isEmpty()) {
            return attachmentsFailure(packageId, "Mailbox >>> Package has no attachments.");
        }

        List<ClaimedAttachment> attachments = new ArrayList<>();
        for (MailItemRecord item : items) {
            attachments.add(ClaimedAttachment.builder()
                    .itemStack(itemSerializationService.deserialize(item.getBase64Item()))
                    .displayName(item.getDisplayName())
                    .amount(item.getAmount())
                    .build());
        }

        return ClaimPackageAttachmentsResult.builder()
                .success(true)
                .packageId(packageId)
                .message("Mailbox >>> Mail detail loaded.")
                .attachments(attachments)
                .senderName(packageRecord.getSenderNameSnapshot())
                .messageText(packageRecord.getMessageText())
                .createdAt(packageRecord.getCreatedAt())
                .build();
    }

    @Deprecated(forRemoval = false)
    public ClaimPackageAttachmentsResult claimPackageAttachments(Player player, String packageId) {
        return previewPackageAttachments(player, packageId);
    }

    private boolean isClaimable(MailLifecycleState lifecycleState) {
        if (lifecycleState == MailLifecycleState.AVAILABLE) {
            return true;
        }
        return lifecycleState == MailLifecycleState.CLAIM_FAILED && properties.isEnableClaimFailedRetry();
    }

    private void expireIfNeeded(MailPackageRecord packageRecord, OffsetDateTime now) {
        if (packageRecord.getLifecycleState() != MailLifecycleState.AVAILABLE
                && packageRecord.getLifecycleState() != MailLifecycleState.CLAIM_FAILED) {
            return;
        }

        boolean expired = mailPackageRepository.updateLifecycleState(
                packageRecord.getId(),
                packageRecord.getLifecycleState(),
                MailLifecycleState.EXPIRED,
                null,
                now
        );
        if (expired) {
            mailAuditService.recordExpired(packageRecord.getId(), "expired-before-claim");
        }
    }

    private boolean hasEnoughSpace(PlayerInventory inventory, int requiredSlots) {
        ItemStack[] contents = inventory.getStorageContents();
        int freeSlots = 0;
        for (ItemStack content : contents) {
            if (content == null) {
                freeSlots++;
            }
        }
        return freeSlots >= requiredSlots;
    }

    private boolean isExpired(MailPackageRecord packageRecord, OffsetDateTime now) {
        return packageRecord.getExpiresAt() != null && !packageRecord.getExpiresAt().isAfter(now);
    }

    private void rollbackClaim(long packageId, UUID claimToken, UUID actorUuid, String actorName, String reason) {
        boolean rolledBack = mailPackageRepository.updateLifecycleState(
                packageId,
                MailLifecycleState.CLAIMING,
                MailLifecycleState.CLAIM_FAILED,
                claimToken,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        if (rolledBack) {
            mailAuditService.recordClaimFailed(packageId, actorUuid, actorName, claimToken, reason);
        }
    }

    private ClaimPackageResult failure(String packageId, String message) {
        return ClaimPackageResult.builder()
                .success(false)
                .inventoryFull(false)
                .packageId(packageId)
                .message(message)
                .build();
    }

    private ClaimPackageAttachmentsResult attachmentsFailure(String packageId, String message) {
        return ClaimPackageAttachmentsResult.builder()
                .success(false)
                .packageId(packageId)
                .message(message)
                .attachments(List.of())
                .build();
    }
}
