package com.cuzz.rookiepostbox.listener;

import com.cuzz.rookiepostbox.RookiePostBox;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.menu.common.ItemBuilder;
import com.cuzz.rookiepostbox.repository.spi.MailPackageRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class MailboxJoinNotificationListener implements Listener {

    private final RookiePostBox plugin;
    private final RookiePostBoxProperties properties;
    private final MailPackageRepository mailPackageRepository;

    public MailboxJoinNotificationListener(
            RookiePostBox plugin,
            RookiePostBoxProperties properties,
            MailPackageRepository mailPackageRepository
    ) {
        this.plugin = plugin;
        this.properties = properties;
        this.mailPackageRepository = mailPackageRepository;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        RookiePostBoxProperties.JoinNotificationProperties notification = properties.getJoinNotification();
        if (notification == null || !notification.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        int delayTicks = Math.max(notification.getDelayTicks(), 0);
        Bukkit.getScheduler().runTaskLater(plugin, () -> notifyUnreadMail(player, notification), delayTicks);
    }

    private void notifyUnreadMail(Player player, RookiePostBoxProperties.JoinNotificationProperties notification) {
        if (!player.isOnline()) {
            return;
        }
        int unreadCount = mailPackageRepository.countUnreadInbox(player.getUniqueId());
        if (unreadCount <= 0) {
            return;
        }
        String message = notification.getMessage()
                .replace("%player%", player.getName())
                .replace("%unread%", String.valueOf(unreadCount));
        player.sendMessage(ItemBuilder.colorize(message));
    }
}
