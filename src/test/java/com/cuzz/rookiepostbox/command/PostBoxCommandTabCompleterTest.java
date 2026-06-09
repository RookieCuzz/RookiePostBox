package com.cuzz.rookiepostbox.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostBoxCommandTabCompleterTest {

    @Mock
    private Command command;
    @Mock
    private Player playerSender;
    @Mock
    private CommandSender consoleSender;
    @Mock
    private Player alice;
    @Mock
    private Player bob;

    @Test
    void rootSuggestionsShouldRespectSenderTypeAndPermissions() {
        PostBoxCommandTabCompleter completer = new PostBoxCommandTabCompleter(List::of);

        when(playerSender.hasPermission("rookiepostbox.send")).thenReturn(true);
        when(playerSender.hasPermission("rookiepostbox.admin")).thenReturn(false);
        assertEquals(
                List.of("menu", "save", "send"),
                completer.onTabComplete(playerSender, command, "rookiepostbox", new String[]{""})
        );

        when(consoleSender.hasPermission("rookiepostbox.admin")).thenReturn(true);
        assertEquals(
                List.of("reload", "admin"),
                completer.onTabComplete(consoleSender, command, "rookiepostbox", new String[]{""})
        );
    }

    @Test
    void rootSuggestionsShouldFilterByPrefixIgnoringCase() {
        PostBoxCommandTabCompleter completer = new PostBoxCommandTabCompleter(List::of);

        when(playerSender.hasPermission("rookiepostbox.send")).thenReturn(true);
        when(playerSender.hasPermission("rookiepostbox.admin")).thenReturn(true);

        assertEquals(
                List.of("save", "send"),
                completer.onTabComplete(playerSender, command, "rookiepostbox", new String[]{"S"})
        );
    }

    @Test
    void sendCommandShouldSuggestOnlinePlayersOnlyForTargetArgument() {
        AtomicInteger supplierCalls = new AtomicInteger();
        PostBoxCommandTabCompleter completer = new PostBoxCommandTabCompleter(() -> {
            supplierCalls.incrementAndGet();
            return List.of(alice, bob);
        });

        when(playerSender.hasPermission("rookiepostbox.send")).thenReturn(true);
        when(alice.getName()).thenReturn("Alice");
        when(bob.getName()).thenReturn("Bob");

        assertEquals(
                List.of("Alice"),
                completer.onTabComplete(playerSender, command, "rookiepostbox", new String[]{"send", "a"})
        );
        assertEquals(
                List.of(),
                completer.onTabComplete(playerSender, command, "rookiepostbox", new String[]{"send", "Alice", ""})
        );
        assertEquals(1, supplierCalls.get());
    }

    @Test
    void adminCommandShouldCompleteSubcommandsAndPlayerArguments() {
        PostBoxCommandTabCompleter completer = new PostBoxCommandTabCompleter(() -> List.of(alice, bob));

        when(playerSender.hasPermission("rookiepostbox.admin")).thenReturn(true);
        when(alice.getName()).thenReturn("Alice");
        when(bob.getName()).thenReturn("Bob");

        assertEquals(
                List.of("grant", "grantall"),
                completer.onTabComplete(playerSender, command, "rookiepostbox", new String[]{"admin", "gr"})
        );
        assertEquals(
                List.of("Bob"),
                completer.onTabComplete(playerSender, command, "rookiepostbox", new String[]{"admin", "grant", "b"})
        );
        assertEquals(
                List.of(),
                completer.onTabComplete(playerSender, command, "rookiepostbox", new String[]{"admin", "grantall", ""})
        );
    }

    @Test
    void adminSuggestionsShouldRequireAdminPermission() {
        AtomicInteger supplierCalls = new AtomicInteger();
        PostBoxCommandTabCompleter completer = new PostBoxCommandTabCompleter(() -> {
            supplierCalls.incrementAndGet();
            return List.of(alice);
        });

        when(playerSender.hasPermission("rookiepostbox.admin")).thenReturn(false);

        assertEquals(
                List.of(),
                completer.onTabComplete(playerSender, command, "rookiepostbox", new String[]{"admin", ""})
        );
        assertEquals(
                List.of(),
                completer.onTabComplete(playerSender, command, "rookiepostbox", new String[]{"admin", "inbox", ""})
        );
        assertEquals(0, supplierCalls.get());
    }
}
