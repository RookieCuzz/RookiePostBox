package com.cuzz.rookiepostbox.menu.compose;

import com.cuzz.rookiepostbox.menu.common.ItemBuilder;
import com.cuzz.rookiepostbox.service.spi.AdminMailboxService;
import com.cuzz.rookiepostbox.service.spi.MailboxService;
import com.cuzz.rookiepostbox.service.SendMailResult;
import nl.odalitadevelopments.menus.annotations.Menu;
import nl.odalitadevelopments.menus.contents.MenuContents;
import nl.odalitadevelopments.menus.contents.placeableitem.PlaceableItemsCloseAction;
import nl.odalitadevelopments.menus.items.ClickableItem;
import nl.odalitadevelopments.menus.items.DisplayItem;
import nl.odalitadevelopments.menus.menu.providers.PlayerMenuProvider;
import nl.odalitadevelopments.menus.menu.type.MenuType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Menu(title = "Attach Mail Items", type = MenuType.CHEST_3_ROW)
public final class MailComposeMenu implements PlayerMenuProvider {

    private static final int[] ATTACHMENT_SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};

    private final MailboxService mailboxService;
    private final AdminMailboxService adminMailboxService;
    private final MailComposeRequest request;

    public MailComposeMenu(MailboxService mailboxService, AdminMailboxService adminMailboxService, MailComposeRequest request) {
        this.mailboxService = mailboxService;
        this.adminMailboxService = adminMailboxService;
        this.request = request;
    }

    @Override
    public void onLoad(@NotNull Player player, @NotNull MenuContents contents) {
        contents.fillRow(1, DisplayItem.of(ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE, "&0").build()));
        contents.fillRow(2, DisplayItem.of(ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE, "&0").build()));
        contents.registerPlaceableItemSlots(ATTACHMENT_SLOTS);
        contents.allowPlaceableItemDrag(true);
        contents.allowPlaceableItemShiftClick(true);
        contents.placeableItemsCloseAction(PlaceableItemsCloseAction.RETURN);
        contents.events().onPlaceableItemClick((slotPos, event) -> isAttachment(event.getCursor()));
        contents.events().onPlaceableItemShiftClick((slotPosses, added, event) -> isAttachment(added));
        contents.events().onPlaceableItemDrag((slotPosses, event) -> isAttachment(event.getOldCursor()));

        contents.setRefreshable(13, () -> DisplayItem.of(ItemBuilder.of(Material.PAPER, "&eMail Attachments")
                .lore(
                        "&7Recipients: &f" + request.recipients().size(),
                        "&7Attached: &f" + contents.getPlaceableItems().size() + " / 9",
                        "&7Message: &f" + request.message()
                )
                .build()));
        contents.set(21, ClickableItem.of(ItemBuilder.of(Material.LIME_WOOL, "&aConfirm Send").build(), event -> confirm(player, contents)));
        contents.set(23, ClickableItem.of(ItemBuilder.of(Material.RED_WOOL, "&cCancel").build(), event -> player.closeInventory()));
    }

    private boolean isAttachment(ItemStack stack) {
        return stack != null && !stack.getType().isAir();
    }

    private void confirm(Player player, MenuContents contents) {
        List<ItemStack> attachments = contents.getPlaceableItems().values().stream()
                .filter(this::isAttachment)
                .map(ItemStack::clone)
                .toList();
        if (attachments.isEmpty()) {
            player.sendMessage("Mailbox >>> Add at least one attachment.");
            return;
        }

        int successCount = 0;
        int failureCount = 0;
        long lastPackageId = 0;
        for (MailComposeRecipient recipient : request.recipients()) {
            SendMailResult result = send(recipient, attachments);
            if (result.isSuccess()) {
                successCount++;
                lastPackageId = result.getPackageId();
            } else {
                failureCount++;
            }
        }

        if (successCount > 0) {
            contents.placeableItemsCloseAction(PlaceableItemsCloseAction.REMOVE);
            player.closeInventory();
        }

        switch (request.mode()) {
            case PLAYER -> player.sendMessage("Mailbox >>> Mail sent. recipients=" + successCount + ", failed=" + failureCount);
            case ADMIN_SINGLE -> {
                if (successCount == 1 && failureCount == 0) {
                    player.sendMessage("Admin grant succeeded. Package #" + lastPackageId);
                } else {
                    player.sendMessage("Admin grant failed.");
                }
            }
            case ADMIN_ALL -> player.sendMessage("Admin grantall completed. recipients=" + successCount + ", failed=" + failureCount);
        }
    }

    private SendMailResult send(MailComposeRecipient recipient, List<ItemStack> attachments) {
        if (request.mode() == MailComposeMode.PLAYER) {
            return mailboxService.sendMail(
                    request.senderUuid(),
                    request.senderName(),
                    recipient.uuid(),
                    recipient.name(),
                    request.message(),
                    attachments,
                    null
            );
        }
        return adminMailboxService.grantMail(
                request.senderUuid(),
                request.senderName(),
                recipient.uuid(),
                recipient.name(),
                request.message(),
                attachments,
                request.adminNote()
        );
    }
}
