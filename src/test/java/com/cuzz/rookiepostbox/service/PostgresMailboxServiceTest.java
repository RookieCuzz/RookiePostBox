package com.cuzz.rookiepostbox.service;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresMailboxServiceTest {

    @Mock
    private MailInboxService mailInboxService;
    @Mock
    private MailWriteSupport mailWriteSupport;
    @Mock
    private MailClaimService mailClaimService;
    @Mock
    private Player player;
    @Mock
    private ItemStack itemStack;

    @Test
    void getInboxShouldDelegateToMailInboxService() {
        PostgresMailboxService service = new PostgresMailboxService(mailInboxService, mailWriteSupport, mailClaimService);
        List<MailboxMailView> expected = List.of(MailboxMailView.builder().mailId("1").build());
        when(mailInboxService.getInbox(player)).thenReturn(expected);

        List<MailboxMailView> result = service.getInbox(player);

        assertSame(expected, result);
        verify(mailInboxService).getInbox(player);
    }

    @Test
    void sendMailShouldDelegateToMailWriteSupport() {
        PostgresMailboxService service = new PostgresMailboxService(mailInboxService, mailWriteSupport, mailClaimService);
        SendMailResult expected = SendMailResult.builder().success(true).packageId(88L).build();
        when(mailWriteSupport.createSingleItemMail(ArgumentMatchers.any(MailCreateRequest.class))).thenReturn(expected);

        SendMailResult result = service.sendMail(
                UUID.randomUUID(),
                "sender",
                UUID.randomUUID(),
                "receiver",
                "hello",
                itemStack,
                UUID.randomUUID()
        );

        assertEquals(88L, result.getPackageId());
        verify(mailWriteSupport).createSingleItemMail(ArgumentMatchers.any(MailCreateRequest.class));
    }

    @Test
    void claimPackageShouldDelegateToMailClaimService() {
        PostgresMailboxService service = new PostgresMailboxService(mailInboxService, mailWriteSupport, mailClaimService);
        ClaimPackageResult expected = ClaimPackageResult.builder().success(true).packageId("42").message("ok").build();
        when(mailClaimService.claimPackage(player, "42")).thenReturn(expected);

        ClaimPackageResult result = service.claimPackage(player, "42");

        assertSame(expected, result);
        verify(mailClaimService).claimPackage(player, "42");
    }
}
