package com.cuzz.rookiepostbox.menu.pagination;

import com.cuzz.rookiepostbox.cache.MailboxPageStateCache;
import me.trytofeel.rookiefonts.RookieFonts;
import me.trytofeel.rookiefonts.entity.Template;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import nl.odalitadevelopments.menus.contents.MenuContents;
import nl.odalitadevelopments.menus.menu.MenuSession;
import nl.odalitadevelopments.menus.pagination.Pagination;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

final class MailboxMenuTitleService {

    void applyTitle(Player player, MenuContents contents, Pagination pagination, MailboxPageStateCache pageStateCache, String templateId) {
        int currentPage = clampPage(pagination.currentPage(), pagination.lastPage());
        pageStateCache.setCurrentPage(player.getUniqueId(), currentPage);
        updatePlaceholders(player, currentPage, pagination.lastPage());
        contents.setTitle(resolveTitle(player, templateId));
    }

    void refreshTitle(Player player, MenuContents contents, Pagination pagination, MailboxPageStateCache pageStateCache, String templateId) {
        int currentPage = clampPage(pagination.currentPage(), pagination.lastPage());
        pageStateCache.setCurrentPage(player.getUniqueId(), currentPage);
        updatePlaceholders(player, currentPage, pagination.lastPage());
        contents.setTitle(resolveTitle(player, templateId));
    }

    void syncOpenSessionTitle(Player player, MenuSession menuSession, Pagination pagination, MailboxPageStateCache pageStateCache, String templateId) {
        if (menuSession == null) {
            return;
        }

        int currentPage = clampPage(pageStateCache.getCurrentPage(player.getUniqueId()), pagination.lastPage());
        pageStateCache.setCurrentPage(player.getUniqueId(), currentPage);
        updatePlaceholders(player, currentPage, pagination.lastPage());
        menuSession.getMenuContents().setTitle(resolveTitle(player, templateId));
    }

    private int clampPage(int page, int lastPage) {
        if (lastPage < 0) {
            return 0;
        }
        return Math.max(0, Math.min(page, lastPage));
    }

    private void updatePlaceholders(Player player, int currentPage, int lastPage) {
        Map<String, String> placeholders = RookieFonts.getInstance()
                .getPlayerPlaceholderHashMap()
                .computeIfAbsent(player.getName(), ignored -> new HashMap<>());
        placeholders.put("%currentPage%", String.valueOf(currentPage + 1));
        placeholders.put("%pageAmount%", String.valueOf(lastPage + 1));
    }

    private String resolveTitle(Player player, String templateId) {
        String resolvedTemplateId = templateId == null || templateId.isBlank() ? "letter" : templateId;
        Template template = RookieFonts.getInstance().getTemplateManager().getTemplateHashMap().get(resolvedTemplateId);
        if (template == null) {
            return "Rookie Post Box";
        }
        Component component = template.getDefaultComponent(player.getName());
        return GsonComponentSerializer.gson().serialize(component);
    }
}
