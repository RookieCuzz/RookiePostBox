package com.cuzz.rookiepostbox.command;


import com.cuzz.rookiepostbox.command.spi.MailComposeMenuOpener;
import com.cuzz.rookiepostbox.RookiePostBox;
import com.cuzz.rookiepostbox.menu.compose.MailComposeMenu;
import com.cuzz.rookiepostbox.menu.compose.MailComposeRequest;
import com.cuzz.rookiepostbox.service.spi.AdminMailboxService;
import com.cuzz.rookiepostbox.service.spi.MailboxService;
import org.bukkit.entity.Player;

final class OdalitaMailComposeMenuOpener implements MailComposeMenuOpener {

    @Override
    public void open(Player player, MailboxService mailboxService, AdminMailboxService adminMailboxService, MailComposeRequest request) {
        RookiePostBox.getInstance().getOdalitaMenus().openMenuBuilder(
                        new MailComposeMenu(mailboxService, adminMailboxService, request),
                        player
                )
                .open();
    }
}
