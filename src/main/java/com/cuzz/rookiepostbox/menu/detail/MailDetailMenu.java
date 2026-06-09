package com.cuzz.rookiepostbox.menu.detail;

import com.cuzz.rookiepostbox.menu.common.ItemBuilder;
import com.cuzz.rookiepostbox.service.ClaimPackageResult;
import com.cuzz.rookiepostbox.service.ClaimPackageAttachmentsResult;
import com.cuzz.rookiepostbox.service.ClaimedAttachment;
import com.cuzz.rookiepostbox.service.spi.MailboxService;
import me.trytofeel.rookiefonts.RookieFonts;
import me.trytofeel.rookiefonts.entity.Template;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import nl.odalitadevelopments.menus.annotations.Menu;
import nl.odalitadevelopments.menus.contents.MenuContents;
import nl.odalitadevelopments.menus.items.ClickableItem;
import nl.odalitadevelopments.menus.items.DisplayItem;
import nl.odalitadevelopments.menus.menu.providers.PlayerMenuProvider;
import nl.odalitadevelopments.menus.menu.type.MenuType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Menu(title = "Mail Detail", type = MenuType.CHEST_6_ROW)
public final class MailDetailMenu implements PlayerMenuProvider {

    private static final int[] ATTACHMENT_SLOTS = new int[]{38, 39, 40, 41, 47, 48, 49, 50};
    private static final int CLAIM_BUTTON_SLOT = 53;
    private static final int BODY_LINE_COUNT = 6;
    private static final int BODY_LINE_LENGTH = 20;
    private static final DateTimeFormatter SENT_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final ClaimPackageAttachmentsResult result;
    private final String templateId;
    private final MailboxService mailboxService;

    public MailDetailMenu(ClaimPackageAttachmentsResult result, String templateId, MailboxService mailboxService) {
        this.result = result;
        this.templateId = templateId;
        this.mailboxService = mailboxService;
    }

    @Override
    public void onLoad(@NotNull Player player, @NotNull MenuContents contents) {
        contents.setTitle(resolveTitle(player));

        List<ClaimedAttachment> attachments = result.getAttachments();
        for (int index = 0; index < Math.min(attachments.size(), ATTACHMENT_SLOTS.length); index++) {
            contents.set(ATTACHMENT_SLOTS[index], DisplayItem.of(attachments.get(index).getItemStack().clone()));
        }
        if (attachments.size() > ATTACHMENT_SLOTS.length) {
            contents.set(42, DisplayItem.of(ItemBuilder.of(Material.CHEST, "&eMore Attachments")
                    .lore("&7Click the claim button to receive all attachments.")
                    .build()));
        }
        contents.set(CLAIM_BUTTON_SLOT, ClickableItem.of(ItemBuilder.of(Material.LIME_WOOL, "&aClaim Attachments")
                .lore("&7Items are delivered directly to your inventory.", "&7The mail stays claimable until this succeeds.")
                .build(), event -> claimAttachments(player)));
    }

    private String resolveTitle(Player player) {
        String resolvedTemplateId = templateId == null || templateId.isBlank() ? "letter_detail" : templateId;
        updatePlaceholders(player);

        Template template = RookieFonts.getInstance().getTemplate(resolvedTemplateId);
        if (template == null) {
            return "Mail Detail";
        }

        Component component = template.getDefaultComponent(player.getName());
        return GsonComponentSerializer.gson().serialize(component);
    }

    private void updatePlaceholders(Player player) {
        Map<String, String> placeholders = RookieFonts.getInstance()
                .getPlayerPlaceholderHashMap()
                .computeIfAbsent(player.getName(), ignored -> new HashMap<>());

        placeholders.put("%receiverName%", player.getName());
        placeholders.put("%senderName%", nonBlank(result.getSenderName(), "系统"));
        placeholders.put("%sentAt%", formatSentAt(result.getCreatedAt()));

        List<String> lines = wrapBody(nonBlank(result.getMessageText(), ""));
        for (int index = 0; index < BODY_LINE_COUNT; index++) {
            String line = index < lines.size() ? lines.get(index) : "";
            placeholders.put("%mailTextLine" + (index + 1) + "%", line);
        }
    }

    private List<String> wrapBody(String text) {
        String normalized = text.replace("\r", "").replace('\n', ' ').trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        int cursor = 0;
        while (cursor < normalized.length() && lines.size() < BODY_LINE_COUNT) {
            int end = Math.min(cursor + BODY_LINE_LENGTH, normalized.length());
            lines.add(normalized.substring(cursor, end));
            cursor = end;
        }
        return lines;
    }

    private String formatSentAt(OffsetDateTime createdAt) {
        if (createdAt == null) {
            return "";
        }
        return SENT_AT_FORMATTER.format(createdAt.atZoneSameInstant(ZoneId.systemDefault()));
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void claimAttachments(Player player) {
        player.sendMessage("Mailbox >>> Claiming package " + result.getPackageId());
        ClaimPackageResult claimResult = mailboxService.claimPackage(player, result.getPackageId());
        player.sendMessage(claimResult.getMessage());
        if (claimResult.isSuccess()) {
            player.closeInventory();
        }
    }
}
