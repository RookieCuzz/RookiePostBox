package com.cuzz.rookiepostbox.menu.pagination.usage;

import com.cuzz.rookiepostbox.menu.pagination.ObjectPaginationExampleMenu;
import com.cuzz.rookiepostbox.menu.pagination.PostBoxMenu;
import nl.odalitadevelopments.menus.OdalitaMenus;

import org.bukkit.entity.Player;

final class PaginationUsage {

    public void openPagination(OdalitaMenus instance, Player player) {
        instance.openMenu(new PostBoxMenu(), player);
    }

    public void openPaginationWithPredefinedPage(OdalitaMenus instance, Player player, int page) {
        instance.openMenuBuilder(new PostBoxMenu(), player)
                .pagination("example_pagination", page) // Use same id as provided in the menu
                .open();
    }

    public void openObjectPagination(OdalitaMenus instance, Player player) {
        instance.openMenu(new ObjectPaginationExampleMenu(), player);
    }

    public void openObjectPaginationWithPredefinedPage(OdalitaMenus instance, Player player, int page) {
        instance.openMenuBuilder(new ObjectPaginationExampleMenu(), player)
                .pagination("player_example_pagination", page) // Use same id as provided in the menu
                .open();
    }
}