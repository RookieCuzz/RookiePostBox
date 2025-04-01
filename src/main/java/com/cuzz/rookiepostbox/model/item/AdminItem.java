package com.cuzz.rookiepostbox.model.item;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
@Entity(value="abstractItems", discriminator="adminItem")
@Data
@AllArgsConstructor // 生成全参构造函数
@NoArgsConstructor
public class AdminItem extends AbstractItem{

    //所属的商品编号
    private String storeID;

    //若为贵重物品(武器 装备)
    @Indexed
    private String uniqueKey;

    public AdminItem(ItemStack itemStack, @Nullable String itemDisplayName) {
        super(itemStack, itemDisplayName);
    }
}
