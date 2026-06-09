package com.cuzz.rookiepostbox.service;


import com.cuzz.rookiepostbox.service.spi.MailAuditService;
import com.cuzz.rookiepostbox.service.spi.ItemSerializationService;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.domain.entity.InboxMailRecord;
import com.cuzz.rookiepostbox.domain.entity.MailItemRecord;
import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.domain.enumtype.MailReadState;
import com.cuzz.rookiepostbox.repository.spi.MailPackageRepository;
import com.cuzz.rookiepostbox.repository.spi.PostBoxRepository;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailInboxServiceTest {

    @Mock
    private PostBoxRepository postBoxRepository;
    @Mock
    private MailPackageRepository mailPackageRepository;
    @Mock
    private MailAuditService mailAuditService;
    @Mock
    private MailStateService mailStateService;
    @Mock
    private ItemSerializationService itemSerializationService;
    @Mock
    private FileConfiguration fileConfiguration;
    @Mock
    private Player player;

    private RookiePostBoxProperties properties;
    private UUID ownerUuid;

    @BeforeEach
    void setUp() {
        when(fileConfiguration.getBoolean("rookiepostbox.debug", false)).thenReturn(false);
        when(fileConfiguration.getInt("rookiepostbox.mail.page-size", 21)).thenReturn(21);
        when(fileConfiguration.getInt("rookiepostbox.mail.max-inbox-mails", 200)).thenReturn(200);
        when(fileConfiguration.getInt("rookiepostbox.mail.default-expire-days", 30)).thenReturn(30);
        when(fileConfiguration.getBoolean("rookiepostbox.mail.allow-player-send", false)).thenReturn(false);
        when(fileConfiguration.getBoolean("rookiepostbox.mail.allow-offline-send", true)).thenReturn(true);
        when(fileConfiguration.getInt("rookiepostbox.claim.recover-claiming-after-seconds", 120)).thenReturn(120);
        when(fileConfiguration.getBoolean("rookiepostbox.claim.enable-claim-failed-retry", true)).thenReturn(true);
        when(fileConfiguration.getInt("rookiepostbox.maintenance.interval-seconds", 60)).thenReturn(60);

        properties = new RookiePostBoxProperties(fileConfiguration);
        properties.reload();

        ownerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(ownerUuid);
        when(player.getName()).thenReturn("owner");
    }

    @Test
    void getInboxShouldMarkUnreadMailAsRead() {
        MailInboxService service = new MailInboxService(
                postBoxRepository,
                mailPackageRepository,
                properties,
                mailAuditService,
                mailStateService,
                itemSerializationService
        );
        InboxMailRecord record = new InboxMailRecord();
        record.setId(7L);
        record.setSenderNameSnapshot("sender");
        record.setMessageText("hello");
        record.setReadState(MailReadState.UNREAD);
        record.setLifecycleState(MailLifecycleState.AVAILABLE);
        record.setFirstItemDisplayName("Diamond");
        record.setFirstItemAmount(1);
        record.setCreatedAt(OffsetDateTime.now());
        MailItemRecord itemRecord = new MailItemRecord();
        itemRecord.setPackageId(7L);
        itemRecord.setBase64Item("serialized-diamond");

        when(mailPackageRepository.findInbox(ownerUuid, 0, 210)).thenReturn(List.of(record));
        when(mailPackageRepository.findItemsByPackageIds(List.of(7L))).thenReturn(List.of(itemRecord));
        when(itemSerializationService.deserialize("serialized-diamond")).thenReturn(new ItemStack(Material.DIAMOND, 1));

        List<MailboxMailView> inbox = service.getInbox(player);

        assertEquals(1, inbox.size());
        assertEquals(MailReadState.READ, inbox.get(0).getReadState());
        assertEquals(Material.DIAMOND, inbox.get(0).getAttachmentPreviewItems().get(0).getType());
        verify(postBoxRepository).createIfAbsent(ownerUuid, "owner");
        verify(mailStateService).runMaintenanceCycle();
        verify(mailPackageRepository).markInboxRead(eq(ownerUuid), eq(List.of(7L)), any());
        verify(mailAuditService).recordRead(7L, ownerUuid, "owner");
    }

    @Test
    void getInboxShouldHideClaimFailedMailWhenRetryDisabled() {
        when(fileConfiguration.getBoolean("rookiepostbox.claim.enable-claim-failed-retry", true)).thenReturn(false);
        properties.reload();

        MailInboxService service = new MailInboxService(
                postBoxRepository,
                mailPackageRepository,
                properties,
                mailAuditService,
                mailStateService,
                itemSerializationService
        );
        InboxMailRecord record = new InboxMailRecord();
        record.setId(9L);
        record.setReadState(MailReadState.UNREAD);
        record.setLifecycleState(MailLifecycleState.CLAIM_FAILED);
        record.setCreatedAt(OffsetDateTime.now());

        when(mailPackageRepository.findInbox(ownerUuid, 0, 210)).thenReturn(List.of(record));

        List<MailboxMailView> inbox = service.getInbox(player);

        assertTrue(inbox.isEmpty());
    }
}
