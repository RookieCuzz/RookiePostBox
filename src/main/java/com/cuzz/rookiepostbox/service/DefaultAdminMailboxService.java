package com.cuzz.rookiepostbox.service;



import com.cuzz.rookiepostbox.service.spi.AdminMailboxService;
import com.cuzz.rookiepostbox.service.spi.MailAuditService;
import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Service;
import com.cuzz.rookiepostbox.domain.entity.AdminMailRecord;
import com.cuzz.rookiepostbox.domain.entity.MailPackageRecord;
import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.domain.enumtype.MailSenderType;
import com.cuzz.rookiepostbox.repository.spi.MailPackageRepository;
import org.bukkit.inventory.ItemStack;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public final class DefaultAdminMailboxService implements AdminMailboxService {

    private final MailWriteSupport mailWriteSupport;
    private final MailPackageRepository mailPackageRepository;
    private final MailAuditService mailAuditService;

    @Autowired
    public DefaultAdminMailboxService(
            MailWriteSupport mailWriteSupport,
            MailPackageRepository mailPackageRepository,
            MailAuditService mailAuditService
    ) {
        this.mailWriteSupport = mailWriteSupport;
        this.mailPackageRepository = mailPackageRepository;
        this.mailAuditService = mailAuditService;
    }

    @Override
    public SendMailResult grantMail(UUID adminUuid, String adminName, UUID receiverUuid, String receiverName, String message, ItemStack itemStack, String reason) {
        return mailWriteSupport.createSingleItemMail(MailCreateRequest.builder()
                .mailboxOwnerUuid(receiverUuid)
                .senderUuid(adminUuid)
                .senderName(adminName)
                .senderType(MailSenderType.ADMIN)
                .receiverName(receiverName)
                .message(message)
                .itemStack(itemStack)
                .sourcePlugin("RookiePostBoxAdmin")
                .adminNote(reason)
                .storeId("admin-grant")
                .build());
    }

    @Override
    public SendMailResult grantMail(UUID adminUuid, String adminName, UUID receiverUuid, String receiverName, String message, List<ItemStack> itemStacks, String reason) {
        return mailWriteSupport.createMail(MailCreateRequest.builder()
                .mailboxOwnerUuid(receiverUuid)
                .senderUuid(adminUuid)
                .senderName(adminName)
                .senderType(MailSenderType.ADMIN)
                .receiverName(receiverName)
                .message(message)
                .itemStacks(itemStacks)
                .sourcePlugin("RookiePostBoxAdmin")
                .adminNote(reason)
                .storeId("admin-grant")
                .build());
    }

    @Override
    public List<AdminMailboxView> queryInbox(UUID ownerUuid, int limit) {
        return mailPackageRepository.findAdminInbox(ownerUuid, 0, limit).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public AdminDeleteMailResult deleteMail(long packageId, UUID actorUuid, String actorName, String reason) {
        MailPackageRecord existing = mailPackageRepository.findById(packageId).orElse(null);
        if (existing == null) {
            return failure(packageId, "Mail package does not exist.");
        }
        if (existing.getLifecycleState() == MailLifecycleState.DELETED) {
            return failure(packageId, "Mail package is already deleted.");
        }
        if (existing.getLifecycleState() == MailLifecycleState.CLAIMING) {
            return failure(packageId, "Mail package is being claimed and cannot be deleted.");
        }

        boolean deleted = mailPackageRepository.markDeleted(
                packageId,
                OffsetDateTime.now(ZoneOffset.UTC),
                reason
        );
        if (!deleted) {
            return failure(packageId, "Delete failed because the package state changed.");
        }

        mailAuditService.recordDeleted(packageId, actorUuid, actorName, MailSenderType.ADMIN, reason);
        return AdminDeleteMailResult.builder()
                .success(true)
                .packageId(packageId)
                .message("Mail package deleted.")
                .build();
    }

    private AdminMailboxView toView(AdminMailRecord record) {
        return AdminMailboxView.builder()
                .packageId(record.getId())
                .senderName(record.getSenderNameSnapshot())
                .message(record.getMessageText())
                .firstItemDisplayName(record.getFirstItemDisplayName() == null ? "UNKNOWN_ITEM" : record.getFirstItemDisplayName())
                .firstItemAmount(record.getFirstItemAmount() == null ? 1 : record.getFirstItemAmount())
                .readState(record.getReadState())
                .lifecycleState(record.getLifecycleState())
                .adminNote(record.getAdminNote())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private AdminDeleteMailResult failure(long packageId, String message) {
        return AdminDeleteMailResult.builder()
                .success(false)
                .packageId(packageId)
                .message(message)
                .build();
    }
}
