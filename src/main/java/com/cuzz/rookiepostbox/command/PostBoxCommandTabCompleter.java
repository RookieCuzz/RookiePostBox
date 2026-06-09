package com.cuzz.rookiepostbox.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class PostBoxCommandTabCompleter implements TabCompleter {

    private static final String PERMISSION_SEND = "rookiepostbox.send";
    private static final String PERMISSION_ADMIN = "rookiepostbox.admin";
    private static final int MAX_PLAYER_SUGGESTIONS = 80;

    private final Supplier<Collection<? extends Player>> onlinePlayersSupplier;

    public PostBoxCommandTabCompleter() {
        this(Bukkit::getOnlinePlayers);
    }

    PostBoxCommandTabCompleter(Supplier<Collection<? extends Player>> onlinePlayersSupplier) {
        this.onlinePlayersSupplier = onlinePlayersSupplier;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return completeRoot(sender, args[0]);
        }

        if (equalsIgnoreCase(args[0], "send")) {
            if (args.length == 2 && sender instanceof Player && sender.hasPermission(PERMISSION_SEND)) {
                return completeOnlinePlayerNames(args[1]);
            }
            return Collections.emptyList();
        }

        if (!equalsIgnoreCase(args[0], "admin") || !sender.hasPermission(PERMISSION_ADMIN)) {
            return Collections.emptyList();
        }

        if (args.length == 2) {
            return completeAdminSubcommand(args[1]);
        }

        if (args.length == 3 && (equalsIgnoreCase(args[1], "inbox") || equalsIgnoreCase(args[1], "grant"))) {
            return completeOnlinePlayerNames(args[2]);
        }

        return Collections.emptyList();
    }

    private static List<String> completeRoot(CommandSender sender, String prefix) {
        List<String> suggestions = new ArrayList<>(5);
        if (sender instanceof Player) {
            addIfMatching(suggestions, "menu", prefix);
            addIfMatching(suggestions, "save", prefix);
            if (sender.hasPermission(PERMISSION_SEND)) {
                addIfMatching(suggestions, "send", prefix);
            }
        }
        if (sender.hasPermission(PERMISSION_ADMIN)) {
            addIfMatching(suggestions, "reload", prefix);
            addIfMatching(suggestions, "admin", prefix);
        }
        return suggestions.isEmpty() ? Collections.emptyList() : suggestions;
    }

    private static List<String> completeAdminSubcommand(String prefix) {
        List<String> suggestions = new ArrayList<>(4);
        addIfMatching(suggestions, "inbox", prefix);
        addIfMatching(suggestions, "grant", prefix);
        addIfMatching(suggestions, "grantall", prefix);
        addIfMatching(suggestions, "delete", prefix);
        return suggestions.isEmpty() ? Collections.emptyList() : suggestions;
    }

    private List<String> completeOnlinePlayerNames(String prefix) {
        Collection<? extends Player> onlinePlayers = onlinePlayersSupplier.get();
        if (onlinePlayers.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> suggestions = new ArrayList<>(Math.min(onlinePlayers.size(), MAX_PLAYER_SUGGESTIONS));
        for (Player player : onlinePlayers) {
            String name = player.getName();
            if (name != null && startsWithIgnoreCase(name, prefix)) {
                suggestions.add(name);
                if (suggestions.size() >= MAX_PLAYER_SUGGESTIONS) {
                    break;
                }
            }
        }
        return suggestions.isEmpty() ? Collections.emptyList() : suggestions;
    }

    private static void addIfMatching(List<String> suggestions, String candidate, String prefix) {
        if (startsWithIgnoreCase(candidate, prefix)) {
            suggestions.add(candidate);
        }
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value.length() >= prefix.length() && value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static boolean equalsIgnoreCase(String value, String expected) {
        return value.length() == expected.length() && value.regionMatches(true, 0, expected, 0, expected.length());
    }
}
