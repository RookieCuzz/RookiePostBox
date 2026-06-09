package com.cuzz.rookiepostbox.service.spi;

import com.cuzz.rookiepostbox.service.MailCreateRequest;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface MailDeliveryNotificationService {

    void notifyMailCreated(MailCreateRequest request, List<ItemStack> attachments, long packageId);
}
