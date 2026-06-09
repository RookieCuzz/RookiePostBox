package com.cuzz.rookiepostbox.menu.facade;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.rookiepostbox.RookiePostBox;
import com.cuzz.rookiepostbox.cache.MailboxPageStateCache;
import com.cuzz.rookiepostbox.menu.pagination.PostBoxMenu;
import org.bukkit.entity.Player;

@Component
public final class MenuFacade {

    private final MailboxPageStateCache pageStateCache;

    @Autowired
    public MenuFacade(MailboxPageStateCache pageStateCache) {
        this.pageStateCache = pageStateCache;
    }

    public void openMailbox(Player player) {
        RookiePostBox.getInstance().getOdalitaMenus().openMenuBuilder(new PostBoxMenu(), player)
                .pagination("mail_pagination", pageStateCache.getCurrentPage(player.getUniqueId()))
                .open();
    }
}
