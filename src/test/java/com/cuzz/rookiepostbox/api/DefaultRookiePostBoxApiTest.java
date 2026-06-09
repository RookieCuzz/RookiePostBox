package com.cuzz.rookiepostbox.api;

import com.cuzz.rookiepostbox.service.ClaimPackageResult;
import com.cuzz.rookiepostbox.service.MailboxMailView;
import com.cuzz.rookiepostbox.service.spi.MailboxService;
import com.cuzz.rookiepostbox.service.SendMailResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultRookiePostBoxApiTest {

    @Mock
    private MailboxService mailboxService;
    @Mock
    private Player player;
    @Mock
    private ItemStack itemStack;

    @Test
    void sendPackageShouldDelegateToMailboxService() {
        DefaultRookiePostBoxApi api = new DefaultRookiePostBoxApi(mailboxService);
        SendMailResult expected = SendMailResult.builder().success(true).packageId(7L).build();
        when(mailboxService.sendMail(any(), any(), any(), any(), any(), any(ItemStack.class))).thenReturn(expected);

        SendMailResult result = api.sendPackage(
                UUID.randomUUID(),
                "sender",
                UUID.randomUUID(),
                "receiver",
                "hello",
                itemStack
        );

        assertSame(expected, result);
        verify(mailboxService).sendMail(any(), eq("sender"), any(), eq("receiver"), eq("hello"), eq(itemStack));
    }

    @Test
    void getInboxShouldDelegateToMailboxService() {
        DefaultRookiePostBoxApi api = new DefaultRookiePostBoxApi(mailboxService);
        List<MailboxMailView> expected = List.of(MailboxMailView.builder().mailId("1").build());
        when(mailboxService.getInbox(player)).thenReturn(expected);

        List<MailboxMailView> result = api.getInbox(player);

        assertSame(expected, result);
        verify(mailboxService).getInbox(player);
    }

    @Test
    void claimPackageShouldDelegateToMailboxService() {
        DefaultRookiePostBoxApi api = new DefaultRookiePostBoxApi(mailboxService);
        ClaimPackageResult expected = ClaimPackageResult.builder().success(true).packageId("42").message("ok").build();
        when(mailboxService.claimPackage(player, "42")).thenReturn(expected);

        ClaimPackageResult result = api.claimPackage(player, "42");

        assertSame(expected, result);
        verify(mailboxService).claimPackage(player, "42");
    }
}
