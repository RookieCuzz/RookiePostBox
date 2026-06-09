package com.cuzz.rookiepostbox.service;


import com.cuzz.rookiepostbox.service.spi.MailAuditService;
import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Service;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.domain.entity.InboxMailRecord;
import com.cuzz.rookiepostbox.domain.entity.MailItemRecord;
import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.domain.enumtype.MailReadState;
import com.cuzz.rookiepostbox.repository.spi.MailPackageRepository;
import com.cuzz.rookiepostbox.repository.spi.PostBoxRepository;
import com.cuzz.rookiepostbox.service.spi.ItemSerializationService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public final class MailInboxService {

    private final PostBoxRepository postBoxRepository;
    private final MailPackageRepository mailPackageRepository;
    private final RookiePostBoxProperties properties;
    private final MailAuditService mailAuditService;
    private final MailStateService mailStateService;
    private final ItemSerializationService itemSerializationService;

    @Autowired
    public MailInboxService(
            PostBoxRepository postBoxRepository,
            MailPackageRepository mailPackageRepository,
            RookiePostBoxProperties properties,
            MailAuditService mailAuditService,
            MailStateService mailStateService,
            ItemSerializationService itemSerializationService
    ) {
        this.postBoxRepository = postBoxRepository;
        this.mailPackageRepository = mailPackageRepository;
        this.properties = properties;
        this.mailAuditService = mailAuditService;
        this.mailStateService = mailStateService;
        this.itemSerializationService = itemSerializationService;
    }

    public List<MailboxMailView> getInbox(Player player) {
        UUID ownerUuid = player.getUniqueId();
        String ownerName = player.getName();
        postBoxRepository.createIfAbsent(ownerUuid, ownerName);
        mailStateService.runMaintenanceCycle();

        List<InboxMailRecord> inboxRecords = mailPackageRepository.findInbox(
                ownerUuid,
                0,
                Math.max(properties.getPageSize() * 10, 54)
        ).stream().filter(this::isVisibleToPlayer).toList();
        Map<Long, List<ItemStack>> attachmentPreviewItems = attachmentPreviewItems(inboxRecords.stream()
                .map(InboxMailRecord::getId)
                .toList());

        List<Long> unreadIds = inboxRecords.stream()
                .filter(record -> record.getReadState() == MailReadState.UNREAD)
                .map(InboxMailRecord::getId)
                .toList();

        if (!unreadIds.isEmpty()) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            mailPackageRepository.markInboxRead(ownerUuid, unreadIds, now);
            unreadIds.forEach(packageId -> mailAuditService.recordRead(packageId, ownerUuid, ownerName));
        }

        return inboxRecords.stream()
                .map(record -> toView(record, unreadIds.contains(record.getId()), attachmentPreviewItems.getOrDefault(record.getId(), List.of())))
                .toList();
    }

    private boolean isVisibleToPlayer(InboxMailRecord record) {
        if (record.getLifecycleState() != MailLifecycleState.CLAIM_FAILED) {
            return true;
        }
        return properties.isEnableClaimFailedRetry();
    }

    private MailboxMailView toView(InboxMailRecord record, boolean markedAsReadOnOpen, List<ItemStack> attachmentPreviewItems) {
        return MailboxMailView.builder()
                .mailId(String.valueOf(record.getId()))
                .senderName(record.getSenderNameSnapshot())
                .message(record.getMessageText())
                .firstItemDisplayName(record.getFirstItemDisplayName() == null ? "UNKNOWN_ITEM" : record.getFirstItemDisplayName())
                .firstItemAmount(record.getFirstItemAmount() == null ? 1 : record.getFirstItemAmount())
                .attachmentPreviewItems(attachmentPreviewItems)
                .readState(markedAsReadOnOpen ? MailReadState.READ : record.getReadState())
                .lifecycleState(record.getLifecycleState())
                .createdAt(record.getCreatedAt())
                .expiresAt(record.getExpiresAt())
                .build();
    }

    private Map<Long, List<ItemStack>> attachmentPreviewItems(List<Long> packageIds) {
        if (packageIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<ItemStack>> itemsByPackageId = new HashMap<>();
        for (MailItemRecord itemRecord : mailPackageRepository.findItemsByPackageIds(packageIds)) {
            if (itemRecord.getPackageId() == null) {
                continue;
            }
            ItemStack itemStack = deserializePreviewItem(itemRecord);
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                continue;
            }
            itemsByPackageId.computeIfAbsent(itemRecord.getPackageId(), ignored -> new ArrayList<>()).add(itemStack);
        }
        return itemsByPackageId;
    }

    private ItemStack deserializePreviewItem(MailItemRecord itemRecord) {
        return itemSerializationService.deserialize(itemRecord.getBase64Item()).clone();
    }
}
