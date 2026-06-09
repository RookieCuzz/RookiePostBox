package com.cuzz.rookiepostbox.service;

import com.cuzz.rookiepostbox.domain.enumtype.MailSenderType;
import lombok.Builder;
import lombok.Value;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class MailCreateRequest {
    UUID mailboxOwnerUuid;
    UUID senderUuid;
    String senderName;
    MailSenderType senderType;
    String receiverName;
    String message;
    ItemStack itemStack;
    List<ItemStack> itemStacks;
    UUID requestId;
    String sourcePlugin;
    String adminNote;
    String storeId;
}
