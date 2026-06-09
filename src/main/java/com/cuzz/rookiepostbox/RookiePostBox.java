package com.cuzz.rookiepostbox;

import com.cuzz.bukkitspring.BukkitSpring;
import com.cuzz.bukkitspring.api.ApplicationContext;
import com.cuzz.bukkitspring.platform.bukkit.BukkitPlatformContext;
import com.cuzz.rookiepostbox.command.PostBoxCommandExecutor;
import com.cuzz.rookiepostbox.command.PostBoxCommandTabCompleter;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.listener.MailboxJoinNotificationListener;
import com.cuzz.rookiepostbox.repository.spi.MailPackageRepository;
import lombok.Getter;
import nl.odalitadevelopments.menus.OdalitaMenus;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class RookiePostBox extends JavaPlugin implements Listener {

    @Getter
    private static RookiePostBox instance;
    @Getter
    private OdalitaMenus odalitaMenus;
    @Getter
    private ApplicationContext applicationContext;

    @Override
    public void onLoad() {
        instance = this;
        applicationContext = BukkitSpring.registerPlugin(
                this,
                new BukkitPlatformContext(this),
                "com.cuzz.rookiepostbox"
        );
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        odalitaMenus = OdalitaMenus.createInstance(this);
        if (applicationContext != null) {
            applicationContext.refresh();
        }
        PluginCommand postBoxCommand = this.getCommand("rookiepostbox");
        if (postBoxCommand != null) {
            postBoxCommand.setExecutor(new PostBoxCommandExecutor(this));
            postBoxCommand.setTabCompleter(new PostBoxCommandTabCompleter());
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new MailboxJoinNotificationListener(
                this,
                applicationContext.get(RookiePostBoxProperties.class),
                applicationContext.get(MailPackageRepository.class)
        ), this);
    }

    @Override
    public void onDisable() {
        BukkitSpring.unregisterPlugin(this);
    }
}
