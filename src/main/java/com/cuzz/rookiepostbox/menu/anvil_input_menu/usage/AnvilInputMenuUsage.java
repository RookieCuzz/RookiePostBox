package com.cuzz.rookiepostbox.menu.anvil_input_menu.usage;

import com.cuzz.rookiepostbox.menu.anvil_input_menu.AnvilInputMenu;
import nl.odalitadevelopments.menus.OdalitaMenus;
import org.bukkit.entity.Player;

public class AnvilInputMenuUsage {

    public void openAnvilInputMenu(OdalitaMenus instance, Player player) {
        instance.openMenu(new AnvilInputMenu((input) -> {
            player.sendMessage("You entered: " + input);
        }), player);
    }
}