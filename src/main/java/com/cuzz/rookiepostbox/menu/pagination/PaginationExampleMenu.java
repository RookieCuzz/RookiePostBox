package com.cuzz.rookiepostbox.menu.pagination;
import com.cuzz.rookiepostbox.RookiePostBox;
import com.cuzz.rookiepostbox.database.Cache;
import com.cuzz.rookiepostbox.menu.common.ItemBuilder;
import com.cuzz.rookiepostbox.model.Package;
import com.cuzz.rookiepostbox.model.PostBox;
import com.cuzz.rookiepostbox.model.item.AbstractItem;
import me.trytofeel.rookieFonts.manager.TemplateManager;
import me.trytofeel.rookieFonts.models.Template;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import nl.odalitadevelopments.menus.annotations.Menu;
import nl.odalitadevelopments.menus.contents.MenuContents;
import nl.odalitadevelopments.menus.contents.pos.SlotPos;
import nl.odalitadevelopments.menus.items.ClickableItem;
import nl.odalitadevelopments.menus.items.DisplayItem;
import nl.odalitadevelopments.menus.items.MenuItem;
import nl.odalitadevelopments.menus.items.PageUpdatableItem;
import nl.odalitadevelopments.menus.items.buttons.PageItem;
import nl.odalitadevelopments.menus.iterators.AbstractMenuIterator;
import nl.odalitadevelopments.menus.iterators.MenuIterator;
import nl.odalitadevelopments.menus.iterators.MenuIteratorType;
import nl.odalitadevelopments.menus.menu.MenuSession;
import nl.odalitadevelopments.menus.menu.providers.PlayerMenuProvider;
import nl.odalitadevelopments.menus.menu.type.MenuType;
import nl.odalitadevelopments.menus.pagination.Pagination;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
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
import java.util.*;
import java.util.function.Supplier;

@Menu(
        title = "Pagination Example",
        type = MenuType.CHEST_6_ROW
)
public final class PaginationExampleMenu implements PlayerMenuProvider {

    public ItemStack deserializeItemStackFromBase64(String base64) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(base64);
        ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
        BukkitObjectInputStream in = new BukkitObjectInputStream(byteIn);
        ItemStack item = (ItemStack) in.readObject();
        in.close();
        return item;
    }
    public static Map<Player,Pagination> cacheMenu;

    static List<ItemStack> list =new ArrayList<>();
    static int currentPageX=0;
    static {
        for (int i = 0+1; i < 66; i++) { // Add 36 * 2 (2 pages) of items
            final int finalIndex = i;
            ItemStack item = ItemBuilder.of(Material.BRICK, "测试邮件" + finalIndex).lore("左键领取").build();
            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.setCustomModelData(40005);
            item.setItemMeta(itemMeta);
            list.add(item);

        }

    }
    static  ItemStack itemStackNext;
    static ItemStack itemStackPrevious;
    static {
        itemStackNext = new ItemStack(Material.BRICK);
        ItemMeta itemMeta = itemStackNext.getItemMeta();
        itemMeta.setCustomModelData(40007);
        itemStackNext.setItemMeta(itemMeta);
        itemStackPrevious = new ItemStack(Material.BRICK);
        itemMeta.setCustomModelData(40006);
        itemStackPrevious.setItemMeta(itemMeta);
    }
    Pagination pagination;
    public void handler(InventoryClickEvent event){

        Player player = (Player)event.getWhoClicked();
        String name = player.getName();
        player.sendMessage(">>>"+ name+ "你点你马呢"+event.getCurrentItem().getItemMeta().getDisplayName());
        ItemStack currentItem = event.getCurrentItem();
        Optional.of(currentItem).ifPresent(
                (itemStack)->{

                    ItemMeta itemMeta = itemStack.getItemMeta();
                    PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();

                    // 创建一个 NamespacedKey
                    NamespacedKey key = new NamespacedKey("rpostbox", "packagedisplay");
                    NamespacedKey base64Key = new NamespacedKey("rpostbox", "realItemBase64");
                    // 设置值
                    String packageId = persistentDataContainer.get(key, PersistentDataType.STRING);
                    PostBox playerPostBox = Cache.postBoxes.get(player.getName());
                    if (playerPostBox== null){
                        playerPostBox = RookiePostBox.getInstance().getMongoDBManager().getPostBoxByPlayer(player);
//                        Cache.postBoxes.put(player.getName(),playerPostBox);
                    }
//                    String realItemBase64 = persistentDataContainer.get(base64Key, PersistentDataType.STRING);
//                    try {
//                        player.getInventory().addItem(deserializeItemStackFromBase64(realItemBase64));
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    } catch (ClassNotFoundException e) {
//                        throw new RuntimeException(e);
//                    }

                    player.sendMessage(ChatColor.GREEN+ " 东西不给你！搭12315投诉吧");
                    RookiePostBox.getInstance().getMongoDBManager().deletePackageFromPostBox(packageId,playerPostBox);
//                    playerPostBox.deletePackage(packageId);
//                    RookiePostBox.getInstance().getMongoDBManager().savePostBoxToDB(playerPostBox);
//                    System.out.println("删除包裹");
                }
        );
        int startSlot = pagination.iterator().getStartSlotPos().getSlot();
        ItemStack item = event.getClickedInventory().getItem(startSlot + 2);
        boolean lastOne=false;
        if (item==null||item.getType().isAir()){
            lastOne=true;
        }

        boolean lastPage = pagination.isLastPage();
        if (lastPage  &&startSlot == event.getSlot()-1&& lastOne){
            currentPageX =Math.max(pagination.currentPage()-1,0);
            System.out.println("修改！");
        }else {
            currentPageX =pagination.currentPage();
        }

        MenuSession openMenuSession = RookiePostBox.getInstance().getOdalitaMenus().getOpenMenuSession(player.getPlayer());

        RookiePostBox.getInstance().getOdalitaMenus().openMenuBuilder(new PaginationExampleMenu(), (Player) player)
                .pagination("example_pagination", currentPageX) // Use same id as provided in the menu
                .open();
    }
//    @Override
//    public void onLoad(@NotNull Player player, @NotNull MenuContents contents) {
//
//        MenuIterator  iterator = contents.createIterator("TESTP", MenuIteratorType.HORIZONTAL, 2, 0);
//        iterator.blacklist(9,18,27,36,17,26,35,45);
//        pagination = contents.pagination("example_pagination", 21) // 28 is items per page
//                .asyncPageSwitching(false) // Optionally, default is false
//                .iterator(iterator)
//                .create();
//        PostBox playerPostBox = Cache.postBoxes.get(player.getName());
//        if (playerPostBox== null){
//            playerPostBox = RookiePostBox.getInstance().getMongoDBManager().getPostBoxByPlayer(player);
//            Cache.postBoxes.put(player.getName(),playerPostBox);
//        }
//        List<Package> packages = playerPostBox.getPackages();
//
//        for (Package packagex:packages) {
//            pagination.addItem(() -> {
//                ItemStack bukkitItem = packagex.getItemContent().get(0).getBukkitItem();
//                ItemMeta itemMeta = bukkitItem.getItemMeta();
//                PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();
//
//                // 创建一个 NamespacedKey
//                NamespacedKey key = new NamespacedKey("rpostbox", "packagedisplay");
//
//                // 设置值
//                persistentDataContainer.set(key, PersistentDataType.STRING, packagex.getId().toString());
//                // 重新应用 ItemMeta
//                bukkitItem.setItemMeta(itemMeta);
//                return ClickableItem.of(bukkitItem, this::handler);
//
//            });
//        }
//        player.sendMessage(ChatColor.GREEN+"打开菜单    剩余邮件数量为"+list.size());
//        Template template = TemplateManager.getTemplateManager().TemplateList.get("example");
//        Component defaultComponentByString = template.getDefaultComponentByString();
////        contents.g
//        final String jsonText = GsonComponentSerializer.gson().serialize(defaultComponentByString);
//        contents.setTitle(jsonText);
//        contents.set(27, MailPageItem.previous(pagination,itemStackPrevious,false)); // Create previous page item with the itemstack provided in DefaultItemProvider
//        contents.set(35, PageItem.next(pagination,itemStackNext,false)); // Create next page item with the itemstack provided in DefaultItemProvider
//
//    }


    @Override
    public void onLoad(@NotNull Player player, @NotNull MenuContents contents) {

        MenuIterator  iterator = contents.createIterator("TESTP", MenuIteratorType.HORIZONTAL, 2, 0);
        iterator.blacklist(9,18,27,36,17,26,35,45);
        pagination = contents.pagination("example_pagination", 21) // 28 is items per page
                .asyncPageSwitching(false) // Optionally, default is false
                .iterator(iterator)
                .create();
        PostBox playerPostBox = Cache.postBoxes.get(player.getName());
        if (playerPostBox== null){
            playerPostBox = RookiePostBox.getInstance().getMongoDBManager().getPostBoxByPlayer(player);
//            Cache.postBoxes.put(player.getName(),playerPostBox);
        }
        List<Package> packages = playerPostBox.getPackages();

        for (Package packagex:packages) {
            pagination.addItem(() -> {
                ItemStack bukkitItem = packagex.getItemContent().get(0).getBukkitItem();
                ItemMeta itemMeta = bukkitItem.getItemMeta();
                PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();

                // 创建一个 NamespacedKey
                NamespacedKey key = new NamespacedKey("rpostbox", "packagedisplay");
                itemMeta.setDisplayName(packagex.getMessage());
                // 设置值
                persistentDataContainer.set(key, PersistentDataType.STRING, packagex.getId().toString());
                // 重新应用 ItemMeta
                bukkitItem.setItemMeta(itemMeta);
                return ClickableItem.of(bukkitItem, this::handler);

            });
        }
//        player.sendMessage(ChatColor.GREEN+"打开菜单 剩余邮件数量为"+list.size());
        Template template = TemplateManager.getTemplateManager().TemplateList.get("example");
        Component defaultComponentByString = template.getDefaultComponentByString();
        final String jsonText = GsonComponentSerializer.gson().serialize(defaultComponentByString);
        contents.setTitle(jsonText);
        contents.set(27, MailPageItem.previous(pagination,itemStackPrevious,false)); // Create previous page item with the itemstack provided in DefaultItemProvider
        contents.set(35, PageItem.next(pagination,itemStackNext,false)); // Create next page item with the itemstack provided in DefaultItemProvider

    }

}