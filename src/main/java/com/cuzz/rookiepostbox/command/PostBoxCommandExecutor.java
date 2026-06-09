package com.cuzz.rookiepostbox.command;

import com.cuzz.rookiepostbox.RookiePostBox;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.menu.facade.MenuFacade;
import com.cuzz.rookiepostbox.service.spi.AdminMailboxService;
import com.cuzz.rookiepostbox.service.spi.MailboxService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class PostBoxCommandExecutor implements CommandExecutor {

    private final Supplier<PlayerMailboxCommandHandler> playerHandlerSupplier;
    private final Supplier<AdminMailboxCommandHandler> adminHandlerSupplier;
    private final Supplier<Boolean> reloadAction;

    public PostBoxCommandExecutor(RookiePostBox plugin) {
        this(
                () -> new PlayerMailboxCommandHandler(
                        plugin.getApplicationContext().get(MenuFacade.class),
                        plugin.getApplicationContext().get(MailboxService.class),
                        plugin.getApplicationContext().get(RookiePostBoxProperties.class),
                        new BukkitOfflinePlayerResolver(),
                        new ToastPlayerMailFeedback(),
                        new OdalitaMailComposeMenuOpener()
                ),
                () -> new AdminMailboxCommandHandler(
                        plugin.getApplicationContext().get(AdminMailboxService.class),
                        new BukkitOfflinePlayerResolver(),
                        new OdalitaMailComposeMenuOpener()
                ),
                () -> {
                    plugin.reloadConfig();
                    plugin.getApplicationContext().get(RookiePostBoxProperties.class).reload();
                    return true;
                }
        );
    }

    PostBoxCommandExecutor(
            Supplier<PlayerMailboxCommandHandler> playerHandlerSupplier,
            Supplier<AdminMailboxCommandHandler> adminHandlerSupplier,
            Supplier<Boolean> reloadAction
    ) {
        this.playerHandlerSupplier = playerHandlerSupplier;
        this.adminHandlerSupplier = adminHandlerSupplier;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /rookiepostbox menu | save <message> | send <player> <message> | reload | admin <inbox|grant|grantall|delete> ...");
            return true;
        }

        if ("admin".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("rookiepostbox.admin")) {
                sender.sendMessage("You do not have permission to use admin mailbox commands.");
                return true;
            }
            return adminHandlerSupplier.get().handle(sender, args);
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("rookiepostbox.admin")) {
                sender.sendMessage("You do not have permission to reload RookiePostBox.");
                return true;
            }
            boolean reloaded = reloadAction.get();
            sender.sendMessage(reloaded ? "RookiePostBox config reloaded." : "RookiePostBox config reload failed.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        return playerHandlerSupplier.get().handle(player, args);
    }
}
