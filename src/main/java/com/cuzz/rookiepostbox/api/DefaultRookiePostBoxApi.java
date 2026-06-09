package com.cuzz.rookiepostbox.api;


import com.cuzz.rookiepostbox.api.spi.RookiePostBoxApi;
import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Service;
import com.cuzz.rookiepostbox.service.ClaimPackageResult;
import com.cuzz.rookiepostbox.service.MailboxMailView;
import com.cuzz.rookiepostbox.service.spi.MailboxService;
import com.cuzz.rookiepostbox.service.SendMailResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

@Service
public final class DefaultRookiePostBoxApi implements RookiePostBoxApi {

    private final MailboxService mailboxService;

    @Autowired
    public DefaultRookiePostBoxApi(MailboxService mailboxService) {
        this.mailboxService = mailboxService;
    }

    @Override
    public SendMailResult sendPackage(@NotNull UUID senderUuid,
                                      @NotNull String senderName,
                                      @NotNull UUID receiverUuid,
                                      @NotNull String receiverName,
                                      @NotNull String message,
                                      @NotNull ItemStack itemStack) {
        return mailboxService.sendMail(senderUuid, senderName, receiverUuid, receiverName, message, itemStack);
    }

    @Override
    public SendMailResult sendPackage(@NotNull UUID senderUuid,
                                      @NotNull String senderName,
                                      @NotNull UUID receiverUuid,
                                      @NotNull String receiverName,
                                      @NotNull String message,
                                      @NotNull ItemStack itemStack,
                                      @NotNull UUID requestId) {
        return mailboxService.sendMail(senderUuid, senderName, receiverUuid, receiverName, message, itemStack, requestId);
    }

    @Override
    public List<MailboxMailView> getInbox(@NotNull Player player) {
        return mailboxService.getInbox(player);
    }

    @Override
    public ClaimPackageResult claimPackage(@NotNull Player player, @NotNull String packageId) {
        return mailboxService.claimPackage(player, packageId);
    }
}
