package com.cuzz.rookiepostbox.menu.pagination;


import com.cuzz.rookiepostbox.RookiePostBox;
import nl.odalitadevelopments.menus.menu.MenuSession;
import nl.odalitadevelopments.menus.pagination.Pagination;
import org.bukkit.entity.Player;

import java.util.function.BiFunction;

public class PageVariable implements BiFunction<String, Player, String> {

    @Override
    public String apply(String string, Player player) {

        Pagination pagination = PaginationExampleMenu.cacheMenu.get(player);
        int i = pagination.currentPage();

        String replace = string.replace("{currentPage}", String.valueOf(i));
        int i1 = pagination.lastPage();
        replace = string.replace("{pageAmount}",String.valueOf(i1));


        return replace;
    }
}
