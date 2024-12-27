package com.cuzz.rookiepostbox.model.item;

import org.bukkit.inventory.ItemStack;

import java.io.IOException;

public interface SerializeItem {
    public String serializeItemStackToBase64(ItemStack item)  throws IOException;


    public ItemStack deserializeItemStackFromBase64(String base64) throws IOException, ClassNotFoundException ;

}
