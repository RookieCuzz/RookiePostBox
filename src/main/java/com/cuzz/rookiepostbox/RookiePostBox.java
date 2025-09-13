package com.cuzz.rookiepostbox;

import com.cuzz.rookiepostbox.command.PostBoxCommandExecutor;
import com.cuzz.rookiepostbox.database.MongoDBManager;
import lombok.Getter;
import nl.odalitadevelopments.menus.OdalitaMenus;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class RookiePostBox extends JavaPlugin implements Listener {



    @Getter
    private static   RookiePostBox instance;
    @Getter
    private OdalitaMenus odalitaMenus;
    @Getter
    private MongoDBManager mongoDbManager;


    @Override
    public void onEnable() {
        // Plugin startup logic
        odalitaMenus = OdalitaMenus.createInstance(this);
        this.getCommand("RookiePostBox").setExecutor(new PostBoxCommandExecutor(this));
        Bukkit.getPluginManager().registerEvents(this, this);
        mongoDbManager = new MongoDBManager();
        mongoDbManager.connect("mongodb://localhost:27017", "RookiePostBox");
        instance = this;
    }

//    @EventHandler
//    public void onInvClose(InventoryCloseEvent uiCloseEvent){
//        Player player = (Player) uiCloseEvent.getPlayer();
//        player.openInventory(uiCloseEvent.getInventory());
//        player.sendMessage("不能关闭！");
//        String title = uiCloseEvent.getView().getTitle();
//        if (title.equalsIgnoreCase("test")){
//            uiCloseEvent.getPlayer().sendMessage("邮件已经发送给对方");
//            //发邮件逻辑
//            Player sender =(Player) uiCloseEvent.getPlayer();
//            ItemStack[] contents = uiCloseEvent.getInventory().getContents();
//            Package aPackage = Package.builder().createTime(new Date()).message("新年好!").build();
//            aPackage.setUpItemContent(contents);
//            Player receiver = RookiePostBox.giftInv.get(sender);
//            aPackage.setUpOtherInfo(sender,RookiePostBox.giftInv.get(sender));
//            PostBox postBox = PostBox.builder().ownerUUID(receiver.getUniqueId().toString()).build();
//            MongoDBManager manager = new MongoDBManager();
//            manager.connect("mongodb://localhost:27017", "postBox");
//            PostBox postBoxById = manager.getPostBoxById(receiver.getUniqueId().toString());
//            manager.insertPackage(aPackage);
//            ObjectId id = aPackage.getId();
//            postBox.addPackage(aPackage);
//            if (postBoxById==null){
//                manager.insertBox(postBox);
//            }
//            manager.close();
//        }
//
//
//    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
