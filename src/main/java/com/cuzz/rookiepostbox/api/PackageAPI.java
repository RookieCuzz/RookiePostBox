package com.cuzz.rookiepostbox.api;

import com.cuzz.rookiepostbox.RookiePostBox;
import com.cuzz.rookiepostbox.api.spi.RookiePostBoxApi;
import com.cuzz.rookiepostbox.service.MailboxMailView;
import com.cuzz.rookiepostbox.service.SendMailResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Deprecated(forRemoval = false)
public class PackageAPI {

    public long sendPackage(@NotNull Player sender, @NotNull Player receiver, @NotNull String message, @NotNull ItemStack item) {
        return api().sendPackage(
                sender.getUniqueId(),
                sender.getName(),
                receiver.getUniqueId(),
                receiver.getName(),
                message,
                item
        ).getPackageId();
    }

    public SendMailResult sendPackageWithRequestId(@NotNull Player sender,
                                                   @NotNull Player receiver,
                                                   @NotNull String message,
                                                   @NotNull ItemStack item,
                                                   @NotNull UUID requestId) {
        return api().sendPackage(
                sender.getUniqueId(),
                sender.getName(),
                receiver.getUniqueId(),
                receiver.getName(),
                message,
                item,
                requestId
        );
    }

    public List<String> getPackageIdsByPlayer(@NotNull Player player) {
        List<String> packages = new ArrayList<>();
        for (MailboxMailView mail : api().getInbox(player)) {
            packages.add(mail.getMailId());
        }
        return packages;
    }

    private RookiePostBoxApi api() {
        return RookiePostBox.getInstance().getApplicationContext().get(RookiePostBoxApi.class);
    }
}
