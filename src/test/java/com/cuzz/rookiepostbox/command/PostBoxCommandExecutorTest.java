package com.cuzz.rookiepostbox.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostBoxCommandExecutorTest {

    @Mock
    private PlayerMailboxCommandHandler playerHandler;
    @Mock
    private AdminMailboxCommandHandler adminHandler;
    @Mock
    private CommandSender sender;
    @Mock
    private Command command;

    @Test
    void reloadCommandShouldRequireAdminPermission() {
        AtomicBoolean reloaded = new AtomicBoolean(false);
        PostBoxCommandExecutor executor = new PostBoxCommandExecutor(
                () -> playerHandler,
                () -> adminHandler,
                () -> {
                    reloaded.set(true);
                    return true;
                }
        );

        when(sender.hasPermission("rookiepostbox.admin")).thenReturn(false);

        boolean result = executor.onCommand(sender, command, "rookiepostbox", new String[]{"reload"});

        assertTrue(result);
        assertTrue(!reloaded.get());
        verify(sender).sendMessage("You do not have permission to reload RookiePostBox.");
    }

    @Test
    void reloadCommandShouldInvokeReloadAction() {
        AtomicBoolean reloaded = new AtomicBoolean(false);
        PostBoxCommandExecutor executor = new PostBoxCommandExecutor(
                () -> playerHandler,
                () -> adminHandler,
                () -> {
                    reloaded.set(true);
                    return true;
                }
        );

        when(sender.hasPermission("rookiepostbox.admin")).thenReturn(true);

        boolean result = executor.onCommand(sender, command, "rookiepostbox", new String[]{"reload"});

        assertTrue(result);
        assertTrue(reloaded.get());
        verify(sender).sendMessage("RookiePostBox config reloaded.");
        verify(playerHandler, never()).handle(null, new String[]{"reload"});
    }
}
