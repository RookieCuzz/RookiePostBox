package com.cuzz.rookiepostbox.service;

import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.domain.enumtype.MailReadState;
import lombok.Builder;
import lombok.Value;
import org.bukkit.inventory.ItemStack;

import java.time.OffsetDateTime;
import java.util.List;

@Value
@Builder
public class MailboxMailView {
    String mailId;
    String senderName;
    String message;
    String firstItemDisplayName;
    int firstItemAmount;
    List<ItemStack> attachmentPreviewItems;
    MailReadState readState;
    MailLifecycleState lifecycleState;
    OffsetDateTime createdAt;
    OffsetDateTime expiresAt;
}
