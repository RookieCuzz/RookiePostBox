package com.cuzz.rookiepostbox.command;



import com.cuzz.rookiepostbox.command.spi.OfflinePlayerResolver;
import com.cuzz.rookiepostbox.command.spi.MailComposeMenuOpener;
import com.cuzz.rookiepostbox.menu.compose.MailComposeMode;
import com.cuzz.rookiepostbox.menu.compose.MailComposeRecipient;
import com.cuzz.rookiepostbox.menu.compose.MailComposeRequest;
import com.cuzz.rookiepostbox.service.AdminDeleteMailResult;
import com.cuzz.rookiepostbox.service.spi.AdminMailboxService;
import com.cuzz.rookiepostbox.service.AdminMailboxView;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

final class AdminMailboxCommandHandler {

    private final AdminMailboxService adminMailboxService;
    private final OfflinePlayerResolver offlinePlayerResolver;
    private final MailComposeMenuOpener composeMenuOpener;

    AdminMailboxCommandHandler(AdminMailboxService adminMailboxService, OfflinePlayerResolver offlinePlayerResolver, MailComposeMenuOpener composeMenuOpener) {
        this.adminMailboxService = adminMailboxService;
        this.offlinePlayerResolver = offlinePlayerResolver;
        this.composeMenuOpener = composeMenuOpener;
    }

    boolean handle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /rookiepostbox admin <inbox|grant|grantall|delete> ...");
            return false;
        }

        return switch (args[1].toLowerCase()) {
            case "inbox" -> handleAdminInbox(sender, args);
            case "grant" -> handleAdminGrant(sender, args);
            case "grantall" -> handleAdminGrantAll(sender, args);
            case "delete" -> handleAdminDelete(sender, args);
            default -> {
                sender.sendMessage("Unknown admin subcommand.");
                yield false;
            }
        };
    }

    private boolean handleAdminInbox(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /rookiepostbox admin inbox <player>");
            return false;
        }

        OfflinePlayer target = offlinePlayerResolver.resolve(args[2]);
        if (target == null) {
            sender.sendMessage("Target player was not found.");
            return false;
        }

        List<AdminMailboxView> inbox = adminMailboxService.queryInbox(target.getUniqueId(), 20);
        sender.sendMessage("Mailbox records for " + (target.getName() == null ? args[2] : target.getName()) + ":");
        if (inbox.isEmpty()) {
            sender.sendMessage("- empty");
            return true;
        }

        for (AdminMailboxView mail : inbox) {
            sender.sendMessage(String.format(
                    "- #%d [%s/%s] %s x%d | %s | %s",
                    mail.getPackageId(),
                    mail.getLifecycleState(),
                    mail.getReadState(),
                    mail.getFirstItemDisplayName(),
                    mail.getFirstItemAmount(),
                    mail.getSenderName(),
                    mail.getMessage()
            ));
        }
        return true;
    }

    private boolean handleAdminGrant(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Admin grant currently requires a player sender because it reads the main-hand item.");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage("Usage: /rookiepostbox admin grant <player> <message>");
            return false;
        }

        OfflinePlayer target = offlinePlayerResolver.resolve(args[2]);
        if (target == null) {
            player.sendMessage("Target player was not found.");
            return false;
        }

        String receiverName = target.getName() == null ? args[2] : target.getName();
        String message = CommandArguments.joinFrom(args, 3);
        openComposeMenu(
                player,
                MailComposeMode.ADMIN_SINGLE,
                List.of(new MailComposeRecipient(target.getUniqueId(), receiverName)),
                message,
                "Granted via admin command"
        );
        return true;
    }

    private boolean handleAdminGrantAll(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Admin grantall currently requires a player sender because it reads the main-hand item.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("Usage: /rookiepostbox admin grantall <message>");
            return false;
        }

        List<? extends Player> recipients = Bukkit.getOnlinePlayers().stream()
                .filter(target -> !target.getUniqueId().equals(player.getUniqueId()))
                .toList();
        if (recipients.isEmpty()) {
            player.sendMessage("No online recipients found for admin grantall.");
            return false;
        }

        String message = CommandArguments.joinFrom(args, 2);
        openComposeMenu(
                player,
                MailComposeMode.ADMIN_ALL,
                recipients.stream()
                        .map(recipient -> new MailComposeRecipient(recipient.getUniqueId(), recipient.getName()))
                        .toList(),
                message,
                "Granted to all online players via admin command"
        );
        return true;
    }

    private boolean handleAdminDelete(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /rookiepostbox admin delete <packageId> [reason]");
            return false;
        }

        long packageId;
        try {
            packageId = Long.parseLong(args[2]);
        } catch (NumberFormatException exception) {
            sender.sendMessage("packageId must be a number.");
            return false;
        }

        String reason = args.length >= 4 ? CommandArguments.joinFrom(args, 3) : "Deleted by admin command";
        UUID actorUuid = sender instanceof Player player ? player.getUniqueId() : null;
        String actorName = sender.getName();

        AdminDeleteMailResult result = adminMailboxService.deleteMail(packageId, actorUuid, actorName, reason);
        sender.sendMessage(result.getMessage() + " (#" + result.getPackageId() + ")");
        return result.isSuccess();
    }

    private void openComposeMenu(Player player, MailComposeMode mode, List<MailComposeRecipient> recipients, String message, String adminNote) {
        composeMenuOpener.open(
                player,
                null,
                adminMailboxService,
                new MailComposeRequest(
                        mode,
                        player.getUniqueId(),
                        player.getName(),
                        recipients,
                        message,
                        adminNote
                )
        );
    }
}
