package com.cuzz.rookiepostbox.service;



import com.cuzz.rookiepostbox.service.spi.ItemSerializationService;
import com.cuzz.rookiepostbox.service.spi.MailAuditService;
import com.cuzz.rookiepostbox.service.spi.MailDeliveryNotificationService;
import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.domain.entity.MailItemRecord;
import com.cuzz.rookiepostbox.domain.entity.MailPackageRecord;
import com.cuzz.rookiepostbox.domain.enumtype.MailItemKind;
import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.domain.enumtype.MailReadState;
import com.cuzz.rookiepostbox.repository.spi.MailPackageRepository;
import com.cuzz.rookiepostbox.repository.spi.PostBoxRepository;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public final class MailWriteSupport {

    private final PostBoxRepository postBoxRepository;
    private final MailPackageRepository mailPackageRepository;
    private final ItemSerializationService itemSerializationService;
    private final RookiePostBoxProperties properties;
    private final MailAuditService mailAuditService;
    private final MailDeliveryNotificationService mailDeliveryNotificationService;

    @Autowired
    public MailWriteSupport(
            PostBoxRepository postBoxRepository,
            MailPackageRepository mailPackageRepository,
            ItemSerializationService itemSerializationService,
            RookiePostBoxProperties properties,
            MailAuditService mailAuditService,
            MailDeliveryNotificationService mailDeliveryNotificationService
    ) {
        this.postBoxRepository = postBoxRepository;
        this.mailPackageRepository = mailPackageRepository;
        this.itemSerializationService = itemSerializationService;
        this.properties = properties;
        this.mailAuditService = mailAuditService;
        this.mailDeliveryNotificationService = mailDeliveryNotificationService;
    }

    public SendMailResult createSingleItemMail(MailCreateRequest request) {
        return createMail(request);
    }

    public SendMailResult createMail(MailCreateRequest request) {
        List<ItemStack> attachmentStacks = attachmentStacks(request);
        if (attachmentStacks.isEmpty()) {
            return SendMailResult.builder()
                    .success(false)
                    .packageId(0)
                    .deduplicated(false)
                    .requestId(request.getRequestId())
                    .message("Mail requires at least one attachment.")
                    .build();
        }

        UUID effectiveRequestId = request.getRequestId() == null ? UUID.randomUUID() : request.getRequestId();
        postBoxRepository.createIfAbsent(request.getMailboxOwnerUuid(), request.getReceiverName());

        MailPackageRecord existingPackage = mailPackageRepository.findByRequestId(effectiveRequestId).orElse(null);
        if (existingPackage != null && existingPackage.getId() != null) {
            return SendMailResult.builder()
                    .success(true)
                    .packageId(existingPackage.getId())
                    .deduplicated(true)
                    .requestId(effectiveRequestId)
                    .message("Mail request already processed.")
                    .build();
        }

        int maxInboxMails = properties.getMaxInboxMails();
        if (maxInboxMails > 0 && mailPackageRepository.countInbox(request.getMailboxOwnerUuid()) >= maxInboxMails) {
            return SendMailResult.builder()
                    .success(false)
                    .packageId(0)
                    .deduplicated(false)
                    .requestId(effectiveRequestId)
                    .message("Mailbox is full.")
                    .build();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        MailPackageRecord packageRecord = new MailPackageRecord();
        packageRecord.setMailboxOwnerUuid(request.getMailboxOwnerUuid());
        packageRecord.setSenderUuid(request.getSenderUuid());
        packageRecord.setSenderNameSnapshot(request.getSenderName());
        packageRecord.setSenderType(request.getSenderType());
        packageRecord.setMessageText(request.getMessage());
        packageRecord.setReadState(MailReadState.UNREAD);
        packageRecord.setLifecycleState(MailLifecycleState.AVAILABLE);
        packageRecord.setRequestId(effectiveRequestId);
        packageRecord.setVersion(0);
        packageRecord.setSourcePlugin(request.getSourcePlugin());
        packageRecord.setAdminNote(request.getAdminNote());
        packageRecord.setCreatedAt(now);
        packageRecord.setUpdatedAt(now);
        packageRecord.setExpiresAt(now.plusDays(properties.getDefaultExpireDays()));

        List<MailItemRecord> itemRecords = new ArrayList<>();
        for (int index = 0; index < attachmentStacks.size(); index++) {
            ItemStack attachment = attachmentStacks.get(index);
            MailItemRecord itemRecord = new MailItemRecord();
            itemRecord.setSlotIndex(index);
            itemRecord.setItemKind(request.getSenderType() == com.cuzz.rookiepostbox.domain.enumtype.MailSenderType.ADMIN
                    ? MailItemKind.ADMIN_ITEM
                    : MailItemKind.NORMAL_ITEM);
            itemRecord.setMaterialKey(attachment.getType().getKey().toString());
            itemRecord.setDisplayName(attachment.getItemMeta() != null && attachment.getItemMeta().hasDisplayName()
                    ? attachment.getItemMeta().getDisplayName()
                    : attachment.getType().name());
            itemRecord.setAmount(attachment.getAmount());
            itemRecord.setStoreId(request.getStoreId());
            itemRecord.setBase64Item(itemSerializationService.serialize(attachment));
            itemRecord.setCreatedAt(now);
            itemRecords.add(itemRecord);
        }

        try {
            long packageId = mailPackageRepository.createPackage(packageRecord, itemRecords);
            if (packageId > 0) {
                mailAuditService.recordCreated(
                        packageId,
                        request.getSenderUuid(),
                        request.getSenderName(),
                        request.getSenderType(),
                        effectiveRequestId,
                        request.getSourcePlugin()
                );
                mailDeliveryNotificationService.notifyMailCreated(request, attachmentStacks, packageId);
            }
            return SendMailResult.builder()
                    .success(packageId > 0)
                    .packageId(packageId)
                    .deduplicated(false)
                    .requestId(effectiveRequestId)
                    .message(packageId > 0 ? "Mail created." : "Failed to create mail package.")
                    .build();
        } catch (RuntimeException exception) {
            MailPackageRecord duplicatedPackage = mailPackageRepository.findByRequestId(effectiveRequestId).orElse(null);
            if (duplicatedPackage != null && duplicatedPackage.getId() != null) {
                return SendMailResult.builder()
                        .success(true)
                        .packageId(duplicatedPackage.getId())
                        .deduplicated(true)
                        .requestId(effectiveRequestId)
                        .message("Mail request already processed.")
                        .build();
            }
            throw exception;
        }
    }

    private List<ItemStack> attachmentStacks(MailCreateRequest request) {
        if (request.getItemStacks() != null && !request.getItemStacks().isEmpty()) {
            return request.getItemStacks().stream()
                    .filter(this::hasAttachmentMaterial)
                    .map(ItemStack::clone)
                    .toList();
        }
        if (!hasAttachmentMaterial(request.getItemStack())) {
            return List.of();
        }
        return List.of(request.getItemStack().clone());
    }

    private boolean hasAttachmentMaterial(ItemStack itemStack) {
        return itemStack != null
                && itemStack.getType() != null
                && itemStack.getType() != Material.AIR
                && itemStack.getAmount() > 0;
    }
}
