package com.cuzz.rookiepostbox;

import com.cuzz.rookiepostbox.menu.anvil_input_menu.AnvilInputMenu;
import com.cuzz.rookiepostbox.menu.pagination.PaginationExampleMenu;
import com.cuzz.rookiepostbox.model.Package;
import com.cuzz.rookiepostbox.database.MongoDBManager;
//import com.github.retrooper.packetevents.PacketEvents;
//import com.github.retrooper.packetevents.event.PacketListenerPriority;
//import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import com.cuzz.rookiepostbox.model.item.AdminItem;
import com.cuzz.rookiepostbox.nms.Toast;
import com.github.retrooper.packetevents.PacketEvents;
import dev.wuason.toastapi.SimpleToast;
import dev.wuason.toastapi.nms.EToastType;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import nl.odalitadevelopments.menus.OdalitaMenus;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Date;

public final class RookiePostBox extends JavaPlugin implements Listener {



    private static   RookiePostBox instance;
    private OdalitaMenus odalitaMenus;

    public OdalitaMenus getOdalitaMenus() {
        return odalitaMenus;
    }

    private MongoDBManager mongoDBManager;

    public MongoDBManager getMongoDBManager(){

        return this.mongoDBManager;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        odalitaMenus = OdalitaMenus.createInstance(this);
        this.getCommand("RookiePostBox").setExecutor(new TestCommandExecutor());
        Bukkit.getPluginManager().registerEvents(this, this);
        mongoDBManager=new MongoDBManager();
        mongoDBManager.connect("mongodb://103.205.253.165:27017","RookiePostBox");
        this.instance=this;
        setupPacket();
    }
    public  void setupPacket(){
        try {
                PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
                //On Bukkit, calling this here is essential, hence the name "load"
                PacketEvents.getAPI().load();
//                PacketEvents.getAPI().getEventManager().registerListener(
//                        new PacketEventsPacketListener(), PacketListenerPriority.NORMAL);

                PacketEvents.getAPI().init();
                // 注册 PacketListener

//            // 初始化 EntityLib
//            SpigotEntityLibPlatform spigotEntityLibPlatform = new SpigotEntityLibPlatform(this);
//            APIConfig aPIConfig = new APIConfig(PacketEvents.getAPI()).usePlatformLogger();
//            EntityLib.init(spigotEntityLibPlatform, aPIConfig);

            getLogger().info("Holograms plugin enabled!");
        } catch (Exception e) {
            getLogger().severe("Error enabling Holograms plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // 监听玩家聊天事件
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

    }
    public static RookiePostBox getInstance() {
        return instance;
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
    // 定义 /test 命令的处理逻辑
    public class TestCommandExecutor implements CommandExecutor {
//    EntityShootBowEvent
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                // 当玩家输入 /test 时发送消息 666
                if (args[0].equalsIgnoreCase("menu")){
                    odalitaMenus.openMenu(new PaginationExampleMenu(), player);
                }
                if (args[0].equalsIgnoreCase("write")){
                    odalitaMenus.openMenu(new AnvilInputMenu((input) -> {
                        player.sendMessage("You entered: " + input);
                    }), player);
                }
                if (args.length<2){
                    System.out.println("请输入/test save 消息内容");
                    return false;
                }
                if (args[0].equalsIgnoreCase("save")){

                    Package aPackage = Package.builder().ownerUUID(String.valueOf(player.getUniqueId()))
                            .senderName(player.getName())
                            .message(args[1])
                            .createTime(new Date()).build();
                    ItemStack item = player.getInventory().getItemInMainHand();
                    AdminItem adminItem = new AdminItem();
                    adminItem.setAmount(item.getAmount());
                    adminItem.setStoreID("测试商品");
                    adminItem.setBukkitItem(item);
                    adminItem.setItemDisplayName(item.getItemMeta().getDisplayName());
                    String string = adminItem.serializeItemStackToBase64(item);
                    adminItem.setBase64Item(string);
                    aPackage.addItem(adminItem);
                    boolean t = mongoDBManager.addPackageToPostBox(aPackage, player, true);
                    System.out.println(t?"保存成功":"保存失败");


                    String text = GsonComponentSerializer.gson().serialize(
                            Component.text("保存成功22222222")
                    );

//                    SimpleToast.sendToast(item, player,args[1] , EToastType.GOAL);
                    Toast.displayTo(player,item.getType().toString().toLowerCase(),args[1], Toast.Style.TASK);
                }


                return true;
            }
            return false;
        }
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
