package com.cuzz.rookiepostbox.service.spi;

import org.bukkit.inventory.ItemStack;

public interface ItemSerializationService {

    String serialize(ItemStack itemStack);

    ItemStack deserialize(String serializedItem);
}
