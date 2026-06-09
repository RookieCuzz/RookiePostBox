package com.cuzz.rookiepostbox.service;



import com.cuzz.rookiepostbox.service.spi.ItemSerializationService;
import com.cuzz.rookiepostbox.service.spi.MailAuditService;
import com.cuzz.rookiepostbox.service.spi.MailDeliveryNotificationService;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.domain.entity.MailPackageRecord;
import com.cuzz.rookiepostbox.domain.enumtype.MailSenderType;
import com.cuzz.rookiepostbox.repository.spi.MailPackageRepository;
import com.cuzz.rookiepostbox.repository.spi.PostBoxRepository;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailWriteSupportTest {

    @Mock
    private PostBoxRepository postBoxRepository;
    @Mock
    private MailPackageRepository mailPackageRepository;
    @Mock
    private ItemSerializationService itemSerializationService;
    @Mock
    private FileConfiguration fileConfiguration;
    @Mock
    private MailAuditService mailAuditService;
    @Mock
    private MailDeliveryNotificationService mailDeliveryNotificationService;

    private MailWriteSupport mailWriteSupport;

    @BeforeEach
    void setUp() {
        when(fileConfiguration.getBoolean("rookiepostbox.debug", false)).thenReturn(false);
        when(fileConfiguration.getInt("rookiepostbox.mail.page-size", 21)).thenReturn(21);
        when(fileConfiguration.getInt("rookiepostbox.mail.max-inbox-mails", 200)).thenReturn(200);
        when(fileConfiguration.getInt("rookiepostbox.mail.default-expire-days", 30)).thenReturn(30);
        when(fileConfiguration.getBoolean("rookiepostbox.mail.allow-player-send", false)).thenReturn(false);
        when(fileConfiguration.getBoolean("rookiepostbox.mail.allow-offline-send", true)).thenReturn(true);

        RookiePostBoxProperties properties = new RookiePostBoxProperties(fileConfiguration);
        properties.reload();

        mailWriteSupport = new MailWriteSupport(
                postBoxRepository,
                mailPackageRepository,
                itemSerializationService,
                properties,
                mailAuditService,
                mailDeliveryNotificationService
        );
    }

    @Test
    void createSingleItemMailShouldPersistMailAndAuditCreateEvent() {
        UUID senderUuid = UUID.randomUUID();
        UUID receiverUuid = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        ItemStack itemStack = mock(ItemStack.class);

        when(itemStack.getType()).thenReturn(Material.DIAMOND);
        when(itemStack.getAmount()).thenReturn(2);
        when(itemStack.getItemMeta()).thenReturn(null);
        when(itemStack.clone()).thenReturn(itemStack);
        when(mailPackageRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(mailPackageRepository.countInbox(receiverUuid)).thenReturn(0);
        when(mailPackageRepository.createPackage(any(), any())).thenReturn(101L);
        when(itemSerializationService.serialize(itemStack)).thenReturn("serialized");

        SendMailResult result = mailWriteSupport.createSingleItemMail(MailCreateRequest.builder()
                .mailboxOwnerUuid(receiverUuid)
                .senderUuid(senderUuid)
                .senderName("sender")
                .senderType(MailSenderType.PLAYER)
                .receiverName("receiver")
                .message("hello")
                .itemStack(itemStack)
                .requestId(requestId)
                .sourcePlugin("RookiePostBox")
                .storeId("player-send")
                .build());

        assertTrue(result.isSuccess());
        assertEquals(101L, result.getPackageId());
        assertFalse(result.isDeduplicated());
        verify(postBoxRepository).createIfAbsent(receiverUuid, "receiver");
        verify(mailAuditService).recordCreated(eq(101L), eq(senderUuid), eq("sender"), eq(MailSenderType.PLAYER), eq(requestId), eq("RookiePostBox"));
        verify(mailDeliveryNotificationService).notifyMailCreated(any(MailCreateRequest.class), any(), eq(101L));
    }

    @Test
    void createSingleItemMailShouldDeduplicateByRequestId() {
        UUID receiverUuid = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        MailPackageRecord existing = new MailPackageRecord();
        existing.setId(77L);
        existing.setRequestId(requestId);
        ItemStack itemStack = mock(ItemStack.class);

        when(itemStack.getType()).thenReturn(Material.DIAMOND);
        when(itemStack.getAmount()).thenReturn(1);
        when(itemStack.clone()).thenReturn(itemStack);
        when(mailPackageRepository.findByRequestId(requestId)).thenReturn(Optional.of(existing));

        SendMailResult result = mailWriteSupport.createSingleItemMail(MailCreateRequest.builder()
                .mailboxOwnerUuid(receiverUuid)
                .senderUuid(UUID.randomUUID())
                .senderName("sender")
                .senderType(MailSenderType.ADMIN)
                .receiverName("receiver")
                .message("hello")
                .itemStack(itemStack)
                .requestId(requestId)
                .sourcePlugin("RookiePostBoxAdmin")
                .storeId("admin-grant")
                .build());

        assertTrue(result.isSuccess());
        assertTrue(result.isDeduplicated());
        assertEquals(77L, result.getPackageId());
        verify(mailPackageRepository, never()).createPackage(any(), any());
        verify(mailAuditService, never()).recordCreated(anyLong(), any(), any(), any(), any(), any());
        verify(mailDeliveryNotificationService, never()).notifyMailCreated(any(), any(), anyLong());
    }

    @Test
    void createSingleItemMailShouldRejectWhenInboxIsFull() {
        UUID receiverUuid = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        ItemStack itemStack = mock(ItemStack.class);

        when(itemStack.getType()).thenReturn(Material.DIAMOND);
        when(itemStack.getAmount()).thenReturn(1);
        when(itemStack.clone()).thenReturn(itemStack);
        when(mailPackageRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(mailPackageRepository.countInbox(receiverUuid)).thenReturn(200);

        SendMailResult result = mailWriteSupport.createSingleItemMail(MailCreateRequest.builder()
                .mailboxOwnerUuid(receiverUuid)
                .senderUuid(UUID.randomUUID())
                .senderName("sender")
                .senderType(MailSenderType.ADMIN)
                .receiverName("receiver")
                .message("hello")
                .itemStack(itemStack)
                .requestId(requestId)
                .sourcePlugin("RookiePostBoxAdmin")
                .storeId("admin-grant")
                .build());

        assertFalse(result.isSuccess());
        assertEquals("Mailbox is full.", result.getMessage());
        verify(mailPackageRepository, never()).createPackage(any(), any());
        verify(mailAuditService, never()).recordCreated(anyLong(), any(), any(), any(), any(), any());
        verify(mailDeliveryNotificationService, never()).notifyMailCreated(any(), any(), anyLong());
    }
}
