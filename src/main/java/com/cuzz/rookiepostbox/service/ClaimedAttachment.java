package com.cuzz.rookiepostbox.service;

import lombok.Builder;
import lombok.Value;
import org.bukkit.inventory.ItemStack;

@Value
@Builder
public class ClaimedAttachment {
    ItemStack itemStack;
    String displayName;
    int amount;
}
