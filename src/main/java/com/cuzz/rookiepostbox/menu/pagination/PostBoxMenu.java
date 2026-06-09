package com.cuzz.rookiepostbox.menu.pagination;

import com.cuzz.rookiepostbox.RookiePostBox;
import com.cuzz.rookiepostbox.cache.MailboxPageStateCache;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.menu.detail.MailDetailMenu;
import com.cuzz.rookiepostbox.service.ClaimPackageAttachmentsResult;
import com.cuzz.rookiepostbox.service.ClaimPackageResult;
import com.cuzz.rookiepostbox.service.MailboxMailView;
import com.cuzz.rookiepostbox.service.spi.MailboxService;
import nl.odalitadevelopments.menus.annotations.Menu;
import nl.odalitadevelopments.menus.contents.MenuContents;
import nl.odalitadevelopments.menus.iterators.MenuIterator;
import nl.odalitadevelopments.menus.iterators.MenuIteratorType;
import nl.odalitadevelopments.menus.menu.MenuSession;
import nl.odalitadevelopments.menus.menu.providers.PlayerMenuProvider;
import nl.odalitadevelopments.menus.menu.type.MenuType;
import nl.odalitadevelopments.menus.pagination.Pagination;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@Menu(title = "Rookie Post Box", type = MenuType.CHEST_6_ROW)
public final class PostBoxMenu implements PlayerMenuProvider {

    private static final MailboxMenuItemFactory DEFAULT_ITEM_FACTORY = new MailboxMenuItemFactory();

    private final Supplier<MailboxService> mailboxServiceSupplier;
    private final Supplier<MailboxPageStateCache> pageStateCacheSupplier;
    private final Supplier<RookiePostBoxProperties> propertiesSupplier;
    private final Supplier<MailboxMenuItemFactory> itemFactorySupplier;
    private final Supplier<MailboxMenuTitleService> titleServiceSupplier;

    private Pagination pagination;

    public PostBoxMenu() {
        this(
                () -> RookiePostBox.getInstance().getApplicationContext().get(MailboxService.class),
                () -> RookiePostBox.getInstance().getApplicationContext().get(MailboxPageStateCache.class),
                () -> RookiePostBox.getInstance().getApplicationContext().get(RookiePostBoxProperties.class),
                () -> DEFAULT_ITEM_FACTORY,
                MailboxMenuTitleService::new
        );
    }

    PostBoxMenu(
            Supplier<MailboxService> mailboxServiceSupplier,
            Supplier<MailboxPageStateCache> pageStateCacheSupplier,
            Supplier<RookiePostBoxProperties> propertiesSupplier,
            Supplier<MailboxMenuItemFactory> itemFactorySupplier,
            Supplier<MailboxMenuTitleService> titleServiceSupplier
    ) {
        this.mailboxServiceSupplier = mailboxServiceSupplier;
        this.pageStateCacheSupplier = pageStateCacheSupplier;
        this.propertiesSupplier = propertiesSupplier;
        this.itemFactorySupplier = itemFactorySupplier;
        this.titleServiceSupplier = titleServiceSupplier;
    }

    @Override
    public void onLoad(@NotNull Player player, @NotNull MenuContents contents) {
        MenuIterator iterator = contents.createIterator("mail-grid", MenuIteratorType.HORIZONTAL, 2, 0);
        iterator.blacklist(9, 18, 27, 36, 17, 26, 35, 45);
        pagination = contents.pagination("mail_pagination", properties().getPageSize())
                .asyncPageSwitching(false)
                .iterator(iterator)
                .create();

        for (MailboxMailView mail : mailboxService().getInbox(player)) {
            pagination.addItem(() -> itemFactory().createPackageItem(mail, properties().getMenu().getMailItem(), this::handleMailClick));
        }

        titleService().applyTitle(player, contents, pagination, pageStateCache(), properties().getMenu().getTitleTemplate());
        contents.set(27, MailPageItem.previous(
                pagination,
                (currentPage, pageAmount) -> itemFactory().createNavigationItem(
                        properties().getMenu().getPreviousButton(),
                        currentPage,
                        pageAmount
                ),
                false
        ));
        contents.set(35, MailPageItem.next(
                pagination,
                (currentPage, pageAmount) -> itemFactory().createNavigationItem(
                        properties().getMenu().getNextButton(),
                        currentPage,
                        pageAmount
                ),
                false
        ));
        contents.events().onInventoryEvent(InventoryClickEvent.class, event -> {
            if (isPaginationControl(event)) {
                scheduleTitleRefresh(player, contents);
            }
        });
    }

    private void handleMailClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String mailId = itemFactory().extractMailId(event.getCurrentItem());
        if (mailId == null) {
            return;
        }

        if (event.getClick() == ClickType.SHIFT_LEFT) {
            quickClaim(player, mailId, event);
            return;
        }
        if (event.getClick() == ClickType.LEFT) {
            openMailDetail(player, mailId);
        }
    }

    private void quickClaim(Player player, String mailId, InventoryClickEvent event) {
        player.sendMessage("Mailbox >>> Claiming package " + mailId);
        ClaimPackageResult result = mailboxService().claimPackage(player, mailId);
        if (!result.isSuccess()) {
            player.sendMessage(result.getMessage());
            return;
        }

        int reopenPage = calculateReopenPage(event);
        pageStateCache().setCurrentPage(player.getUniqueId(), reopenPage);
        MenuSession openMenuSession = RookiePostBox.getInstance().getOdalitaMenus().getOpenMenuSession(player);

        RookiePostBox.getInstance().getOdalitaMenus().openMenuBuilder(new PostBoxMenu(), player)
                .pagination("mail_pagination", reopenPage)
                .open();

        titleService().syncOpenSessionTitle(player, openMenuSession, pagination, pageStateCache(), properties().getMenu().getTitleTemplate());
    }

    private void openMailDetail(Player player, String mailId) {
        ClaimPackageAttachmentsResult result = mailboxService().previewPackageAttachments(player, mailId);
        if (!result.isSuccess()) {
            player.sendMessage(result.getMessage());
            return;
        }

        RookiePostBox.getInstance().getOdalitaMenus().openMenuBuilder(
                        new MailDetailMenu(result, properties().getMenu().getDetailTitleTemplate(), mailboxService()),
                        player
                )
                .open();
    }

    private int calculateReopenPage(InventoryClickEvent event) {
        int startSlot = pagination.iterator().getStartSlotPos().getSlot();
        ItemStack item = event.getClickedInventory().getItem(startSlot + 2);
        boolean lastItemOnPage = item == null || item.getType().isAir();
        if (pagination.isLastPage() && startSlot == event.getSlot() - 1 && lastItemOnPage) {
            return Math.max(pagination.currentPage() - 1, 0);
        }
        return pagination.currentPage();
    }

    private void scheduleTitleRefresh(Player player, MenuContents contents) {
        Bukkit.getScheduler().runTaskLater(
                RookiePostBox.getInstance(),
                () -> titleService().refreshTitle(player, contents, pagination, pageStateCache(), properties().getMenu().getTitleTemplate()),
                2
        );
    }

    private boolean isPaginationControl(InventoryClickEvent event) {
        return event.getSlot() == 27 || event.getSlot() == 35;
    }

    private MailboxService mailboxService() {
        return mailboxServiceSupplier.get();
    }

    private MailboxPageStateCache pageStateCache() {
        return pageStateCacheSupplier.get();
    }

    private RookiePostBoxProperties properties() {
        return propertiesSupplier.get();
    }

    private MailboxMenuItemFactory itemFactory() {
        return itemFactorySupplier.get();
    }

    private MailboxMenuTitleService titleService() {
        return titleServiceSupplier.get();
    }
}
