package com.cuzz.rookiepostbox.menu.pagination;

import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.domain.enumtype.MailReadState;
import com.cuzz.rookiepostbox.menu.common.ItemBuilder;
import com.cuzz.rookiepostbox.service.MailboxMailView;
import nl.odalitadevelopments.menus.items.ClickableItem;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

final class MailboxMenuItemFactory {

    private static final NamespacedKey PACKAGE_KEY = new NamespacedKey("rpostbox", "packagedisplay");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    ClickableItem createPackageItem(
            MailboxMailView mail,
            RookiePostBoxProperties.MailItemProperties properties,
            Consumer<InventoryClickEvent> clickHandler
    ) {
        return ClickableItem.of(
                ItemBuilder.of(material(properties.getMaterial(), Material.BUNDLE)).meta(meta -> {
                    applyCustomModelData(meta, properties.getCustomModelData());
                    meta.setDisplayName(ItemBuilder.colorize(buildDisplayName(mail, properties)));
                    meta.setLore(buildLore(mail, properties));
                    meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                    if (meta instanceof BundleMeta bundleMeta) {
                        applyBundlePreview(bundleMeta, mail.getAttachmentPreviewItems());
                    }
                    meta.getPersistentDataContainer().set(PACKAGE_KEY, PersistentDataType.STRING, mail.getMailId());
                }).build(),
                clickHandler
        );
    }

    String extractMailId(org.bukkit.inventory.ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().get(PACKAGE_KEY, PersistentDataType.STRING);
    }

    org.bukkit.inventory.ItemStack createNavigationItem(
            RookiePostBoxProperties.ButtonProperties properties,
            int currentPage,
            int pageAmount
    ) {
        return ItemBuilder.of(material(properties.getMaterial(), Material.BRICK)).meta(meta -> {
            applyCustomModelData(meta, properties.getCustomModelData());
            meta.setDisplayName(ItemBuilder.colorize(replacePagePlaceholders(properties.getDisplayName(), currentPage, pageAmount)));
            meta.setLore(properties.getLore().stream()
                    .map(line -> replacePagePlaceholders(line, currentPage, pageAmount))
                    .map(ItemBuilder::colorize)
                    .toList());
        }).build();
    }

    private String buildDisplayName(MailboxMailView mail, RookiePostBoxProperties.MailItemProperties properties) {
        String template = mail.getReadState() == MailReadState.UNREAD
                ? properties.getUnreadDisplayName()
                : properties.getReadDisplayName();
        return replaceMailPlaceholders(template, mail);
    }

    private List<String> buildLore(MailboxMailView mail, RookiePostBoxProperties.MailItemProperties properties) {
        return properties.getLore().stream()
                .map(line -> replaceMailPlaceholders(line, mail))
                .map(ItemBuilder::colorize)
                .toList();
    }

    private String statusText(MailboxMailView mail) {
        if (mail.getLifecycleState() == MailLifecycleState.CLAIM_FAILED) {
            return "&cClaim failed, retry available";
        }
        if (mail.getReadState() == MailReadState.UNREAD) {
            return "&aUnread";
        }
        return "&7Read";
    }

    private String replaceMailPlaceholders(String template, MailboxMailView mail) {
        return template
                .replace("%mailId%", nullToText(mail.getMailId(), "-"))
                .replace("%sender%", nullToText(mail.getSenderName(), "System"))
                .replace("%message%", nullToText(mail.getMessage(), "No message"))
                .replace("%status%", statusText(mail))
                .replace("%createdAt%", formatDateTime(mail.getCreatedAt()))
                .replace("%expiresAt%", formatDateTime(mail.getExpiresAt()))
                .replace("%firstItem%", nullToText(mail.getFirstItemDisplayName(), "UNKNOWN_ITEM"))
                .replace("%firstAmount%", String.valueOf(Math.max(mail.getFirstItemAmount(), 1)));
    }

    private String replacePagePlaceholders(String template, int currentPage, int pageAmount) {
        return template
                .replace("%currentPage%", String.valueOf(currentPage))
                .replace("%pageAmount%", String.valueOf(pageAmount));
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "Never";
        }
        return DATE_TIME_FORMATTER.format(dateTime.atZoneSameInstant(ZoneId.systemDefault()));
    }

    private void applyBundlePreview(BundleMeta meta, List<ItemStack> attachmentPreviewItems) {
        if (attachmentPreviewItems == null || attachmentPreviewItems.isEmpty()) {
            return;
        }

        meta.setItems(attachmentPreviewItems.stream()
                .filter(itemStack -> itemStack != null && itemStack.getType() != Material.AIR)
                .map(ItemStack::clone)
                .toList());
    }

    private void applyCustomModelData(org.bukkit.inventory.meta.ItemMeta meta, Integer customModelData) {
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }
    }

    private String nullToText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Material material(String materialName, Material fallback) {
        if (materialName == null || materialName.isBlank()) {
            return fallback;
        }
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

}
