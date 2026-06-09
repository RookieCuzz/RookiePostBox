package com.cuzz.rookiepostbox.command.spi;

import com.cuzz.rookiepostbox.menu.compose.MailComposeRequest;
import com.cuzz.rookiepostbox.service.spi.AdminMailboxService;
import com.cuzz.rookiepostbox.service.spi.MailboxService;
import org.bukkit.entity.Player;

public interface MailComposeMenuOpener {

    void open(Player player, MailboxService mailboxService, AdminMailboxService adminMailboxService, MailComposeRequest request);
}
