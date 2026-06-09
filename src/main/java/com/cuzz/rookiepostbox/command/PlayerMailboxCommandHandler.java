package com.cuzz.rookiepostbox.command;




import com.cuzz.rookiepostbox.command.spi.PlayerMailFeedback;
import com.cuzz.rookiepostbox.command.spi.OfflinePlayerResolver;
import com.cuzz.rookiepostbox.command.spi.MailComposeMenuOpener;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.menu.compose.MailComposeMenu;
import com.cuzz.rookiepostbox.menu.compose.MailComposeMode;
import com.cuzz.rookiepostbox.menu.compose.MailComposeRecipient;
import com.cuzz.rookiepostbox.menu.compose.MailComposeRequest;
import com.cuzz.rookiepostbox.menu.facade.MenuFacade;
import com.cuzz.rookiepostbox.service.spi.MailboxService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

final class PlayerMailboxCommandHandler {

    private final MenuFacade menuFacade;
    private final MailboxService mailboxService;
    private final RookiePostBoxProperties properties;
    private final OfflinePlayerResolver offlinePlayerResolver;
    private final PlayerMailFeedback feedback;
    private final MailComposeMenuOpener composeMenuOpener;

    PlayerMailboxCommandHandler(
            MenuFacade menuFacade,
            MailboxService mailboxService,
            RookiePostBoxProperties properties,
            OfflinePlayerResolver offlinePlayerResolver,
            PlayerMailFeedback feedback,
            MailComposeMenuOpener composeMenuOpener
    ) {
        this.menuFacade = menuFacade;
        this.mailboxService = mailboxService;
        this.properties = properties;
        this.offlinePlayerResolver = offlinePlayerResolver;
        this.feedback = feedback;
        this.composeMenuOpener = composeMenuOpener;
    }

    boolean handle(Player player, String[] args) {
        return switch (args[0].toLowerCase()) {
            case "menu" -> {
                menuFacade.openMailbox(player);
                yield true;
            }
            case "save" -> handleSaveCommand(player, args);
            case "send" -> handleSendCommand(player, args);
            default -> {
                player.sendMessage("Unknown subcommand.");
                yield true;
            }
        };
    }

    private boolean handleSaveCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /rookiepostbox save <message>");
            return false;
        }

        String message = CommandArguments.joinFrom(args, 1);
        openComposeMenu(
                player,
                List.of(new MailComposeRecipient(player.getUniqueId(), player.getName())),
                message
        );
        return true;
    }

    private boolean handleSendCommand(Player player, String[] args) {
        if (!player.hasPermission("rookiepostbox.send")) {
            player.sendMessage("You do not have permission to send mail.");
            return true;
        }
        if (!properties.isAllowPlayerSend()) {
            player.sendMessage("Player-to-player mail is disabled on this server.");
            return true;
        }
        if (args.length < 3) {
            player.sendMessage("Usage: /rookiepostbox send <player> <message>");
            return false;
        }

        OfflinePlayer target = offlinePlayerResolver.resolve(args[1]);
        if (target == null) {
            player.sendMessage("Target player was not found.");
            return false;
        }
        if (!target.isOnline() && !properties.isAllowOfflineSend()) {
            player.sendMessage("Offline mail is disabled on this server.");
            return false;
        }
        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage("Use /rookiepostbox save <message> to mail yourself.");
            return false;
        }

        String receiverName = target.getName() == null ? args[1] : target.getName();
        String message = CommandArguments.joinFrom(args, 2);
        openComposeMenu(
                player,
                List.of(new MailComposeRecipient(target.getUniqueId(), receiverName)),
                message
        );
        return true;
    }

    private void openComposeMenu(Player player, List<MailComposeRecipient> recipients, String message) {
        composeMenuOpener.open(
                player,
                mailboxService,
                null,
                new MailComposeRequest(
                        MailComposeMode.PLAYER,
                        player.getUniqueId(),
                        player.getName(),
                        recipients,
                        message,
                        null
                )
        );
    }
}
