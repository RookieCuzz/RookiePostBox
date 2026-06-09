package com.cuzz.rookiepostbox.service.spi;

import com.cuzz.rookiepostbox.service.AdminDeleteMailResult;
import com.cuzz.rookiepostbox.service.AdminMailboxView;
import com.cuzz.rookiepostbox.service.SendMailResult;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public interface AdminMailboxService {

    SendMailResult grantMail(UUID adminUuid, String adminName, UUID receiverUuid, String receiverName, String message, ItemStack itemStack, String reason);

    SendMailResult grantMail(UUID adminUuid, String adminName, UUID receiverUuid, String receiverName, String message, List<ItemStack> itemStacks, String reason);

    List<AdminMailboxView> queryInbox(UUID ownerUuid, int limit);

    AdminDeleteMailResult deleteMail(long packageId, UUID actorUuid, String actorName, String reason);
}
