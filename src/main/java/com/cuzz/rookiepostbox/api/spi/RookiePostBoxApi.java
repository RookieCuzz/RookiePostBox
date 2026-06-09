package com.cuzz.rookiepostbox.api.spi;

import com.cuzz.rookiepostbox.service.ClaimPackageResult;
import com.cuzz.rookiepostbox.service.MailboxMailView;
import com.cuzz.rookiepostbox.service.SendMailResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public interface RookiePostBoxApi {

    SendMailResult sendPackage(@NotNull UUID senderUuid,
                               @NotNull String senderName,
                               @NotNull UUID receiverUuid,
                               @NotNull String receiverName,
                               @NotNull String message,
                               @NotNull ItemStack itemStack);

    SendMailResult sendPackage(@NotNull UUID senderUuid,
                               @NotNull String senderName,
                               @NotNull UUID receiverUuid,
                               @NotNull String receiverName,
                               @NotNull String message,
                               @NotNull ItemStack itemStack,
                               @NotNull UUID requestId);

    List<MailboxMailView> getInbox(@NotNull Player player);

    ClaimPackageResult claimPackage(@NotNull Player player, @NotNull String packageId);
}
