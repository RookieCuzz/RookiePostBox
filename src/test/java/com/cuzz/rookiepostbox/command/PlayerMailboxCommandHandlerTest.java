package com.cuzz.rookiepostbox.command;




import com.cuzz.rookiepostbox.command.spi.PlayerMailFeedback;
import com.cuzz.rookiepostbox.command.spi.OfflinePlayerResolver;
import com.cuzz.rookiepostbox.command.spi.MailComposeMenuOpener;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.menu.compose.MailComposeMode;
import com.cuzz.rookiepostbox.menu.compose.MailComposeRequest;
import com.cuzz.rookiepostbox.menu.facade.MenuFacade;
import com.cuzz.rookiepostbox.service.spi.MailboxService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerMailboxCommandHandlerTest {

    @Mock
    private MenuFacade menuFacade;
    @Mock
    private MailboxService mailboxService;
    @Mock
    private RookiePostBoxProperties properties;
    @Mock
    private OfflinePlayerResolver offlinePlayerResolver;
    @Mock
    private PlayerMailFeedback feedback;
    @Mock
    private MailComposeMenuOpener composeMenuOpener;
    @Mock
    private Player player;
    @Mock
    private OfflinePlayer target;

    @Test
    void menuCommandShouldDelegateToMenuFacade() {
        PlayerMailboxCommandHandler handler = handler();

        boolean result = handler.handle(player, new String[]{"menu"});

        assertTrue(result);
        verify(menuFacade).openMailbox(player);
    }

    @Test
    void saveCommandShouldOpenComposeMenu() {
        PlayerMailboxCommandHandler handler = handler();
        UUID senderUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(senderUuid);
        when(player.getName()).thenReturn("sender");

        boolean result = handler.handle(player, new String[]{"save", "hello"});

        assertTrue(result);
        MailComposeRequest request = captureComposeRequest();
        assertEquals(MailComposeMode.PLAYER, request.mode());
        assertEquals(senderUuid, request.senderUuid());
        assertEquals(senderUuid, request.recipients().get(0).uuid());
        assertEquals("hello", request.message());
    }

    @Test
    void sendCommandShouldOpenComposeMenu() {
        PlayerMailboxCommandHandler handler = handler();
        UUID senderUuid = UUID.randomUUID();
        UUID receiverUuid = UUID.randomUUID();

        when(player.hasPermission("rookiepostbox.send")).thenReturn(true);
        when(properties.isAllowPlayerSend()).thenReturn(true);
        when(properties.isAllowOfflineSend()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(senderUuid);
        when(player.getName()).thenReturn("sender");
        when(offlinePlayerResolver.resolve("receiver")).thenReturn(target);
        when(target.getUniqueId()).thenReturn(receiverUuid);
        when(target.getName()).thenReturn("receiver");
        boolean result = handler.handle(player, new String[]{"send", "receiver", "hello", "world"});

        assertTrue(result);
        MailComposeRequest request = captureComposeRequest();
        assertEquals(MailComposeMode.PLAYER, request.mode());
        assertEquals(senderUuid, request.senderUuid());
        assertEquals(receiverUuid, request.recipients().get(0).uuid());
        assertEquals("receiver", request.recipients().get(0).name());
        assertEquals("hello world", request.message());
    }

    private PlayerMailboxCommandHandler handler() {
        return new PlayerMailboxCommandHandler(
                menuFacade,
                mailboxService,
                properties,
                offlinePlayerResolver,
                feedback,
                composeMenuOpener
        );
    }

    private MailComposeRequest captureComposeRequest() {
        ArgumentCaptor<MailComposeRequest> requestCaptor = ArgumentCaptor.forClass(MailComposeRequest.class);
        verify(composeMenuOpener).open(eq(player), eq(mailboxService), eq(null), requestCaptor.capture());
        return requestCaptor.getValue();
    }
}
