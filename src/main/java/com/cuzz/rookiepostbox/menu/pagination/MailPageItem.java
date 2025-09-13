package com.cuzz.rookiepostbox.menu.pagination;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import nl.odalitadevelopments.menus.OdalitaMenus;
import nl.odalitadevelopments.menus.contents.MenuContents;
import nl.odalitadevelopments.menus.items.PageUpdatableItem;
import nl.odalitadevelopments.menus.items.buttons.PageItem;
import nl.odalitadevelopments.menus.pagination.IPagination;
import nl.odalitadevelopments.menus.providers.providers.DefaultItemProvider;
import nl.odalitadevelopments.menus.utils.cooldown.Cooldown;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class MailPageItem extends PageUpdatableItem {
    public static @NotNull MailPageItem previous(@NotNull IPagination<?, ?> pagination, @NotNull ItemStack itemStack, boolean showOnFirstPage) {
        return new MailPageItem(MailPageItem.Type.PREVIOUS, pagination, itemStack, showOnFirstPage);
    }

    public static @NotNull MailPageItem previous(@NotNull IPagination<?, ?> pagination, boolean showOnFirstPage) {
        return new MailPageItem(MailPageItem.Type.PREVIOUS, pagination, showOnFirstPage);
    }

    public static @NotNull MailPageItem previous(@NotNull IPagination<?, ?> pagination, @NotNull ItemStack itemStack) {
        return new MailPageItem(MailPageItem.Type.PREVIOUS, pagination, itemStack, false);
    }

    public static @NotNull MailPageItem previous(@NotNull IPagination<?, ?> pagination) {
        return new MailPageItem(MailPageItem.Type.PREVIOUS, pagination, false);
    }

    public static @NotNull MailPageItem next(@NotNull IPagination<?, ?> pagination, @NotNull ItemStack itemStack, boolean showOnLastPage) {
        return new MailPageItem(MailPageItem.Type.NEXT, pagination, itemStack, showOnLastPage);
    }

    public static @NotNull MailPageItem next(@NotNull IPagination<?, ?> pagination, boolean showOnLastPage) {
        return new MailPageItem(MailPageItem.Type.NEXT, pagination, showOnLastPage);
    }

    public static @NotNull MailPageItem next(@NotNull IPagination<?, ?> pagination, @NotNull ItemStack itemStack) {
        return new MailPageItem(MailPageItem.Type.NEXT, pagination, itemStack, false);
    }

    public static @NotNull MailPageItem next(@NotNull IPagination<?, ?> pagination) {
        return new MailPageItem(MailPageItem.Type.NEXT, pagination, false);
    }

    private final MailPageItem.Type type;
    private final IPagination<?, ?> pagination;
    private final boolean showOnFirstOrLastPage;

    private final ItemStack itemStack;

    private MailPageItem(MailPageItem.Type type, IPagination<?, ?> pagination, ItemStack itemStack, boolean showOnFirstOrLastPage) {
        this.type = type;
        this.pagination = pagination;
        this.itemStack = itemStack;
        this.showOnFirstOrLastPage = showOnFirstOrLastPage;
    }

    private MailPageItem(MailPageItem.Type type, IPagination<?, ?> pagination, boolean showOnFirstOrLastPage) {

        this(type, pagination, null, showOnFirstOrLastPage);
    }

    @Override
    protected @NotNull ItemStack getItemStack(@NotNull OdalitaMenus instance, @NotNull MenuContents contents) {
        if (!this.showOnFirstOrLastPage && !this.canBeUsed()) {
            return new ItemStack(Material.AIR);
        }

        if (this.itemStack == null) {
            DefaultItemProvider defaultItemProvider = instance.getProvidersContainer().getDefaultItemProvider();
            return switch (this.type) {
                case PREVIOUS -> defaultItemProvider.previousPageItem(this.pagination);
                case NEXT -> defaultItemProvider.nextPageItem(this.pagination);
            };
        }

        return this.itemStack;
    }

    @Override
    public @NotNull Consumer<InventoryClickEvent> onClick(@NotNull OdalitaMenus instance, @NotNull MenuContents contents) {
        return (event) -> {
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }


            Cooldown cooldown = instance.getProvidersContainer().getCooldownProvider().pageCooldown();
            if (cooldown != null && instance.getCooldownContainer().checkAndCreate(player.getUniqueId(), "INTERNAL_PAGE_COOLDOWN", cooldown)) {
                return;
            }

            if (this.canBeUsed()) {


                this.type.handle(this.pagination,contents);
            }
        };
    }

    private boolean canBeUsed() {
        return (this.type == MailPageItem.Type.PREVIOUS && !this.pagination.isFirstPage())
                || (this.type == MailPageItem.Type.NEXT && !this.pagination.isLastPage());
    }

    private enum Type {

        PREVIOUS {
            @Override
            void handle(@NotNull IPagination<?, ?> pagination,@NotNull MenuContents contents) {
//                int newPageNumber = pagination.currentPage()-1;
//                Template template = TemplateManager.getTemplateManager().TemplateList.get("example");
//                Block block = template.layout.get(3);
//                Line lineById = block.getLineById(1);
//                lineById.setDefaultText(lineById.getDefaultText().replace("",""));
//                Component defaultComponentByString = template.getDefaultComponentByString();
//                final String jsonText = GsonComponentSerializer.gson().serialize(defaultComponentByString);
//                contents.setTitle();
                pagination.previousPage();

            }
        },

        NEXT {
            @Override
            void handle(@NotNull IPagination<?, ?> pagination,@NotNull MenuContents contents) {
//                int newPageNumber = pagination.currentPage()+1;
//                contents.setTitle();
                pagination.nextPage();
            }
        };

        abstract void handle(@NotNull IPagination<?, ?> pagination,@NotNull MenuContents contents);
    }
}
