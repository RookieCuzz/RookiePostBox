package com.cuzz.rookiepostbox.command;



import com.cuzz.rookiepostbox.command.spi.OfflinePlayerResolver;
import com.cuzz.rookiepostbox.command.spi.MailComposeMenuOpener;
import com.cuzz.rookiepostbox.menu.compose.MailComposeMode;
import com.cuzz.rookiepostbox.menu.compose.MailComposeRequest;
import com.cuzz.rookiepostbox.service.AdminDeleteMailResult;
import com.cuzz.rookiepostbox.service.spi.AdminMailboxService;
import com.cuzz.rookiepostbox.service.AdminMailboxView;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMailboxCommandHandlerTest {

    @Mock
    private AdminMailboxService adminMailboxService;
    @Mock
    private OfflinePlayerResolver offlinePlayerResolver;
    @Mock
    private MailComposeMenuOpener composeMenuOpener;
    @Mock
    private CommandSender commandSender;
    @Mock
    private Player player;
    @Mock
    private OfflinePlayer target;

    @Test
    void inboxCommandShouldQueryAdminMailboxService() {
        AdminMailboxCommandHandler handler = handler();
        UUID targetUuid = UUID.randomUUID();
        AdminMailboxView view = AdminMailboxView.builder()
                .packageId(5L)
                .senderName("system")
                .message("reward")
                .firstItemDisplayName("Diamond")
                .firstItemAmount(1)
                .createdAt(OffsetDateTime.now())
                .build();

        when(offlinePlayerResolver.resolve("target")).thenReturn(target);
        when(target.getUniqueId()).thenReturn(targetUuid);
        when(target.getName()).thenReturn("target");
        when(adminMailboxService.queryInbox(targetUuid, 20)).thenReturn(List.of(view));

        boolean result = handler.handle(commandSender, new String[]{"admin", "inbox", "target"});

        assertTrue(result);
        verify(adminMailboxService).queryInbox(targetUuid, 20);
    }

    @Test
    void grantCommandShouldFailWhenSenderIsNotPlayer() {
        AdminMailboxCommandHandler handler = handler();

        boolean result = handler.handle(commandSender, new String[]{"admin", "grant", "target", "reward"});

        assertTrue(result);
        verify(composeMenuOpener, never()).open(any(), any(), any(), any());
    }

    @Test
    void grantCommandShouldOpenComposeMenu() {
        AdminMailboxCommandHandler handler = handler();
        UUID playerUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();

        when(offlinePlayerResolver.resolve("target")).thenReturn(target);
        when(target.getUniqueId()).thenReturn(targetUuid);
        when(target.getName()).thenReturn("target");
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getName()).thenReturn("admin");

        boolean result = handler.handle(player, new String[]{"admin", "grant", "target", "reward", "item"});

        assertTrue(result);
        MailComposeRequest request = captureComposeRequest();
        assertEquals(MailComposeMode.ADMIN_SINGLE, request.mode());
        assertEquals(playerUuid, request.senderUuid());
        assertEquals(targetUuid, request.recipients().get(0).uuid());
        assertEquals("target", request.recipients().get(0).name());
        assertEquals("reward item", request.message());
        assertEquals("Granted via admin command", request.adminNote());
    }

    @Test
    void deleteCommandShouldRejectInvalidPackageId() {
        AdminMailboxCommandHandler handler = handler();

        boolean result = handler.handle(commandSender, new String[]{"admin", "delete", "not-a-number"});

        assertFalse(result);
        verify(adminMailboxService, never()).deleteMail(anyLong(), any(), any(), any());
    }

    @Test
    void deleteCommandShouldDelegateToAdminMailboxService() {
        AdminMailboxCommandHandler handler = handler();
        when(commandSender.getName()).thenReturn("console");
        when(adminMailboxService.deleteMail(42L, null, "console", "cleanup"))
                .thenReturn(AdminDeleteMailResult.builder().success(true).packageId(42L).message("deleted").build());

        boolean result = handler.handle(commandSender, new String[]{"admin", "delete", "42", "cleanup"});

        assertTrue(result);
        verify(adminMailboxService).deleteMail(42L, null, "console", "cleanup");
    }

    private AdminMailboxCommandHandler handler() {
        return new AdminMailboxCommandHandler(adminMailboxService, offlinePlayerResolver, composeMenuOpener);
    }

    private MailComposeRequest captureComposeRequest() {
        ArgumentCaptor<MailComposeRequest> requestCaptor = ArgumentCaptor.forClass(MailComposeRequest.class);
        verify(composeMenuOpener).open(eq(player), eq(null), eq(adminMailboxService), requestCaptor.capture());
        return requestCaptor.getValue();
    }
}
