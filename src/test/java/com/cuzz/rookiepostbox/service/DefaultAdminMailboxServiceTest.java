package com.cuzz.rookiepostbox.service;


import com.cuzz.rookiepostbox.service.spi.MailAuditService;
import com.cuzz.rookiepostbox.domain.entity.AdminMailRecord;
import com.cuzz.rookiepostbox.domain.entity.MailPackageRecord;
import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.domain.enumtype.MailReadState;
import com.cuzz.rookiepostbox.domain.enumtype.MailSenderType;
import com.cuzz.rookiepostbox.repository.spi.MailPackageRepository;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
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
class DefaultAdminMailboxServiceTest {

    @Mock
    private MailWriteSupport mailWriteSupport;
    @Mock
    private MailPackageRepository mailPackageRepository;
    @Mock
    private MailAuditService mailAuditService;

    @Test
    void grantMailShouldDelegateToMailWriteSupport() {
        DefaultAdminMailboxService service = new DefaultAdminMailboxService(
                mailWriteSupport,
                mailPackageRepository,
                mailAuditService
        );
        SendMailResult expected = SendMailResult.builder().success(true).packageId(55L).build();
        when(mailWriteSupport.createSingleItemMail(any(MailCreateRequest.class))).thenReturn(expected);

        SendMailResult result = service.grantMail(
                UUID.randomUUID(),
                "admin",
                UUID.randomUUID(),
                "receiver",
                "grant message",
                mock(ItemStack.class),
                "manual grant"
        );

        assertTrue(result.isSuccess());
        assertEquals(55L, result.getPackageId());
        verify(mailWriteSupport).createSingleItemMail(any(MailCreateRequest.class));
    }

    @Test
    void queryInboxShouldMapRepositoryResult() {
        DefaultAdminMailboxService service = new DefaultAdminMailboxService(
                mailWriteSupport,
                mailPackageRepository,
                mailAuditService
        );
        AdminMailRecord record = new AdminMailRecord();
        record.setId(9L);
        record.setSenderNameSnapshot("system");
        record.setMessageText("reward");
        record.setReadState(MailReadState.READ);
        record.setLifecycleState(MailLifecycleState.AVAILABLE);
        record.setFirstItemDisplayName("Diamond");
        record.setFirstItemAmount(3);
        record.setAdminNote("note");
        record.setCreatedAt(OffsetDateTime.now());

        when(mailPackageRepository.findAdminInbox(any(UUID.class), eq(0), eq(20))).thenReturn(List.of(record));

        List<AdminMailboxView> result = service.queryInbox(UUID.randomUUID(), 20);

        assertEquals(1, result.size());
        assertEquals(9L, result.get(0).getPackageId());
        assertEquals("Diamond", result.get(0).getFirstItemDisplayName());
    }

    @Test
    void deleteMailShouldMarkDeletedAndAuditEvent() {
        DefaultAdminMailboxService service = new DefaultAdminMailboxService(
                mailWriteSupport,
                mailPackageRepository,
                mailAuditService
        );
        MailPackageRecord existing = new MailPackageRecord();
        existing.setId(12L);
        existing.setLifecycleState(MailLifecycleState.AVAILABLE);

        when(mailPackageRepository.findById(12L)).thenReturn(Optional.of(existing));
        when(mailPackageRepository.markDeleted(eq(12L), any(), eq("cleanup"))).thenReturn(true);

        AdminDeleteMailResult result = service.deleteMail(12L, UUID.randomUUID(), "admin", "cleanup");

        assertTrue(result.isSuccess());
        verify(mailAuditService).recordDeleted(eq(12L), any(), eq("admin"), eq(MailSenderType.ADMIN), eq("cleanup"));
    }

    @Test
    void deleteMailShouldRejectClaimingMail() {
        DefaultAdminMailboxService service = new DefaultAdminMailboxService(
                mailWriteSupport,
                mailPackageRepository,
                mailAuditService
        );
        MailPackageRecord existing = new MailPackageRecord();
        existing.setId(12L);
        existing.setLifecycleState(MailLifecycleState.CLAIMING);

        when(mailPackageRepository.findById(12L)).thenReturn(Optional.of(existing));

        AdminDeleteMailResult result = service.deleteMail(12L, UUID.randomUUID(), "admin", "cleanup");

        assertFalse(result.isSuccess());
        verify(mailPackageRepository, never()).markDeleted(anyLong(), any(), any());
    }
}
