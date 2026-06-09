package com.cuzz.rookiepostbox.service;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.rookiepostbox.RookiePostBox;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import org.bukkit.Bukkit;

@Component
public final class MailMaintenanceScheduler {

    private final MailStateService mailStateService;
    private final RookiePostBoxProperties properties;

    @Autowired
    public MailMaintenanceScheduler(MailStateService mailStateService, RookiePostBoxProperties properties) {
        this.mailStateService = mailStateService;
        this.properties = properties;
    }

    @PostConstruct
    public void schedule() {
        long intervalTicks = Math.max(20L, properties.getMaintenanceIntervalSeconds() * 20L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                RookiePostBox.getInstance(),
                mailStateService::runMaintenanceCycle,
                intervalTicks,
                intervalTicks
        );
    }
}
