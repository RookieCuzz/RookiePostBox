package com.cuzz.rookiepostbox.model.item;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import lombok.NoArgsConstructor;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Entity(value="abstractItems",discriminator = "nonAdminItem")
@NoArgsConstructor
public class NonAdminItem extends AbstractItem{

    public NonAdminItem(ItemStack itemStack, @Nullable String itemDisplayName) {
        super(itemStack, itemDisplayName);
    }


}
