package com.cuzz.rookiepostbox.menu.pagination;
import com.cuzz.rookiepostbox.RookiePostBox;
import com.cuzz.rookiepostbox.database.Cache;
import com.cuzz.rookiepostbox.menu.common.ItemBuilder;
import com.cuzz.rookiepostbox.model.Package;
import com.cuzz.rookiepostbox.model.PostBox;
import com.cuzz.rookiepostbox.model.item.AbstractItem;
import me.trytofeel.rookieFonts.RookieFonts;
import me.trytofeel.rookieFonts.manager.TemplateManager;
import me.trytofeel.rookieFonts.models.Template;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import nl.odalitadevelopments.menus.annotations.Menu;
import nl.odalitadevelopments.menus.contents.MenuContents;
import nl.odalitadevelopments.menus.items.ClickableItem;
import nl.odalitadevelopments.menus.iterators.MenuIterator;
import nl.odalitadevelopments.menus.iterators.MenuIteratorType;
import nl.odalitadevelopments.menus.menu.MenuSession;
import nl.odalitadevelopments.menus.menu.providers.PlayerMenuProvider;
import nl.odalitadevelopments.menus.menu.type.MenuType;
import nl.odalitadevelopments.menus.pagination.Pagination;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.jetbrains.annotations.NotNull;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Menu(
        title = "Pagination Example",
        type = MenuType.CHEST_6_ROW
)
public final class PostBoxMenu implements PlayerMenuProvider {

    public static Map<Player, Pagination> cacheMenu;

    static List<ItemStack> list = new ArrayList<>();
    static int currentPageX = 0;
    static {
        for (int i = 0 + 1; i < 66; i++) { // Add 36 * 2 (2 pages) of items
            final int finalIndex = i;
            ItemStack item = ItemBuilder.of(Material.BRICK, "测试邮件" + finalIndex).lore("左键领取").build();
            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.setCustomModelData(40005);
            item.setItemMeta(itemMeta);
            list.add(item);
        }

    }

    static ItemStack itemStackNext, itemStackPrevious;
    static {
        itemStackNext = new ItemStack(Material.BRICK);
        ItemMeta itemMeta = itemStackNext.getItemMeta();
        itemMeta.setDisplayName("下一页");
        itemMeta.setItemModel(new NamespacedKey("oraxen", "mailbox_next"));
        itemStackNext.setItemMeta(itemMeta);
        itemStackPrevious = new ItemStack(Material.BRICK);
        itemMeta.setDisplayName("上一页");
        itemMeta.setItemModel(new NamespacedKey("oraxen", "mailbox_previous"));
        itemStackPrevious.setItemMeta(itemMeta);
    }

    Pagination pagination;

    public void handler(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        String name = player.getName();
        player.sendMessage("邮箱>>> 正在收取 " + event.getCurrentItem().getItemMeta().getDisplayName());
        ItemStack currentItem = event.getCurrentItem();
        AtomicBoolean success = new AtomicBoolean(true);
        Optional.of(currentItem).ifPresent(
                (itemStack)->{
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();

                    // 创建一个 NamespacedKey
                    NamespacedKey key = new NamespacedKey("rpostbox", "packagedisplay");
                    // 设置值
                    PostBox playerPostBox = Cache.postBoxes.get(player.getName());
                    if (playerPostBox == null){
                        playerPostBox = RookiePostBox.getInstance().getMongoDBManager().getPostBoxByPlayer(player);
                    }
                    String packageId = persistentDataContainer.get(key, PersistentDataType.STRING);
                    playerPostBox.getPackages().forEach(packagex -> {
                        if (packagex.getId().toString().equals(packageId)){
                            packagex.getItemContent().forEach(item -> {
                                if(player.getInventory().firstEmpty() == -1){
                                    player.sendMessage("邮箱>>> 你的背包已满，请清理后再领取！");
                                    success.set(false);
                                    return;
                                }
                                player.getInventory().addItem(item.getBukkitItem());
                                player.sendMessage("邮箱>>> 获得了: " + item.getItemDisplayName() + " x " + item.getAmount());
                            });
                        }
                    });
                    if(!success.get()) return;
                    RookiePostBox.getInstance().getMongoDBManager().deletePackageFromPostBox(packageId, playerPostBox);
                }
        );
        if(!success.get()) return;
        int startSlot = pagination.iterator().getStartSlotPos().getSlot();
        ItemStack item = event.getClickedInventory().getItem(startSlot + 2);
        boolean lastOne = false;
        if (item == null || item.getType().isAir()){
            lastOne = true;
        }

        boolean lastPage = pagination.isLastPage();
        if (lastPage && startSlot == event.getSlot() - 1 && lastOne){
            currentPageX = Math.max(pagination.currentPage() - 1,0);
            System.out.println("修改！");
        }else {
            currentPageX = pagination.currentPage();
        }

        MenuSession openMenuSession = RookiePostBox.getInstance().getOdalitaMenus().getOpenMenuSession(player.getPlayer());

        RookiePostBox.getInstance().getOdalitaMenus().openMenuBuilder(new PostBoxMenu(), (Player) player)
                .pagination("mail_pagination", currentPageX) // Use same id as provided in the menu
                .open();

        Map<String, String> stringStringMap = RookieFonts.playerPapiMap.get(player.getName());
        if(stringStringMap == null){
            stringStringMap = new HashMap<>();
        }
        stringStringMap.put("%currentPage%",String.valueOf(currentPageX + 1));
        stringStringMap.put("%pageAmount%",String.valueOf(pagination.lastPage() + 1));
        RookieFonts.playerPapiMap.put(player.getName(),stringStringMap);
        Template template = TemplateManager.getTemplateManager().TemplateList.get("mail");
        Component defaultComponentByString = template.getDefaultComponentByString(player.getName());
        String jsonText = GsonComponentSerializer.gson().serialize(defaultComponentByString);
        openMenuSession.getMenuContents().setTitle(jsonText);
    }

    @Override
    public void onLoad(@NotNull Player player, @NotNull MenuContents contents) {
        MenuIterator iterator = contents.createIterator("TESTP", MenuIteratorType.HORIZONTAL, 2, 0);
        iterator.blacklist(9,18,27,36,17,26,35,45);
        pagination = contents.pagination("mail_pagination", 21) // 21 items per page
                .asyncPageSwitching(false) // Optionally, default is false
                .iterator(iterator)
                .create();
        PostBox playerPostBox = Cache.postBoxes.get(player.getName());
        if(playerPostBox == null) {
            playerPostBox = RookiePostBox.getInstance().getMongoDBManager().getPostBoxByPlayer(player);
        }
        if(playerPostBox == null) {
            RookiePostBox.getInstance().getMongoDBManager().createNewPostBox(player.getUniqueId().toString(),player.getName());
            playerPostBox = RookiePostBox.getInstance().getMongoDBManager().getPostBoxByPlayer(player);
        }
        List<Package> packages = playerPostBox.getPackages();

        for (Package packagex:packages) {
            pagination.addItem(() -> {
//                ItemStack bukkitItem = packagex.getItemContent().get(0).getBukkitItem();
                ItemStack bukkitItem = new ItemStack(Material.BRICK);
                ItemMeta itemMeta = bukkitItem.getItemMeta();
                PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();

                // 创建一个 NamespacedKey
                NamespacedKey key = new NamespacedKey("oraxen","unopened_box");
                itemMeta.setItemModel(key);
                itemMeta.setDisplayName(ChatColor.BOLD + "来自 " + packagex.getSenderName() + " 的邮件");
                List<String> itemLore = new ArrayList<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                AbstractItem mailItem = packagex.getItemContent().get(0);
                itemLore.add(ChatColor.WHITE + packagex.getMessage());
                itemLore.add(ChatColor.GRAY + "----------");
                itemLore.add(ChatColor.GRAY + "发送时间: " + dateFormat.format(packagex.getCreateTime()));
                itemLore.add(ChatColor.GREEN + "邮件内容: " + mailItem.getItemDisplayName() + "x" + mailItem.getAmount());
                itemMeta.setLore(itemLore);
                // 设置值
                key = new NamespacedKey("rpostbox", "packagedisplay");
                persistentDataContainer.set(key, PersistentDataType.STRING, packagex.getId().toString());

                bukkitItem.setItemMeta(itemMeta);

                return ClickableItem.of(bukkitItem, this::handler);
            });
        }

        Map<String, String> stringStringMap = RookieFonts.playerPapiMap.get(player.getName());
        if(stringStringMap == null){
            stringStringMap = new HashMap<>();
        }
        stringStringMap.put("%currentPage%",String.valueOf(currentPageX + 1));
        stringStringMap.put("%pageAmount%",String.valueOf(pagination.lastPage() + 1));
        RookieFonts.playerPapiMap.put(player.getName(),stringStringMap);

        Template template = TemplateManager.getTemplateManager().TemplateList.get("mail");
        Component defaultComponentByString = template.getDefaultComponentByString(player.getName());
        String jsonText = GsonComponentSerializer.gson().serialize(defaultComponentByString);

        contents.setTitle(jsonText);
        contents.set(27, MailPageItem.previous(pagination,itemStackPrevious,false)); // Create previous page item with the itemstack provided in DefaultItemProvider
        contents.set(35, MailPageItem.next(pagination,itemStackNext,false)); // Create next page item with the itemstack provided in DefaultItemProvider

        contents.events().onInventoryEvent(InventoryClickEvent.class, inventoryClickEvent -> {

            Bukkit.getScheduler().runTaskLater(RookiePostBox.getInstance(), () -> {
                Map<String, String> ssM = RookieFonts.playerPapiMap.get(player.getName());
                if(ssM == null){
                    ssM = new HashMap<>();
                }
                ssM.put("%currentPage%",String.valueOf(pagination.currentPage() + 1));
                ssM.put("%pageAmount%",String.valueOf(pagination.lastPage() + 1));
                RookieFonts.playerPapiMap.put(player.getName(),ssM);

                Template t = TemplateManager.getTemplateManager().TemplateList.get("mail");
                Component dC = t.getDefaultComponentByString(player.getName());
                String j = GsonComponentSerializer.gson().serialize(dC);

                contents.setTitle(j);
            }, 2);

        });
    }
}