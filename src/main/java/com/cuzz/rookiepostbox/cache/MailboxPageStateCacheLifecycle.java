package com.cuzz.rookiepostbox.cache;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.bukkitspring.api.annotation.PreDestroy;
import com.cuzz.rookiepostbox.RookiePostBox;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Component
public final class MailboxPageStateCacheLifecycle implements Listener {

    private final MailboxPageStateCache pageStateCache;

    @Autowired
    public MailboxPageStateCacheLifecycle(MailboxPageStateCache pageStateCache) {
        this.pageStateCache = pageStateCache;
    }

    @PostConstruct
    public void registerListener() {
        Bukkit.getPluginManager().registerEvents(this, RookiePostBox.getInstance());
    }

    @PreDestroy
    public void cleanup() {
        HandlerList.unregisterAll(this);
        pageStateCache.clearAll();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pageStateCache.clear(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        pageStateCache.clear(event.getPlayer().getUniqueId());
    }
}
