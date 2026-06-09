package com.cuzz.rookiepostbox.menu.pagination;

import org.bukkit.entity.Player;

import java.util.function.BiFunction;

/**
 * Legacy placeholder adapter kept for compatibility with older menu templates.
 * Current page state is refreshed through the menu session instead.
 */
public class PageVariable implements BiFunction<String, Player, String> {

    @Override
    public String apply(String string, Player player) {
        return string;
    }
}
