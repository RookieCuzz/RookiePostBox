package com.cuzz.rookiepostbox;

import com.cuzz.rookiepostbox.model.Package;
import com.cuzz.rookiepostbox.model.PostBox;
import com.cuzz.rookiepostbox.database.MongoDBManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Date;
import java.util.HashMap;

public final class RookiePostBox extends JavaPlugin implements Listener {



    private static   RookiePostBox instance;

    public static HashMap<Player,Player> giftInv=new HashMap<>();
    @Override
    public void onEnable() {
        // Plugin startup logic
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        //On Bukkit, calling this here is essential, hence the name "load"
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().getEventManager().registerListener(
                new PacketEventsPacketListener(), PacketListenerPriority.NORMAL);

        PacketEvents.getAPI().init();




        this.getCommand("test").setExecutor(new TestCommandExecutor());
        Bukkit.getPluginManager().registerEvents(this, this);
        this.instance=this;
    }

    // 监听玩家聊天事件
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // 检查玩家是否输入了 "我要测试"
        if (message.equals("我要测试")) {
            // 在主线程上打开菜单（因为聊天事件是异步的）
            Bukkit.getScheduler().runTask(this, () -> {
                // 创建并打开自定义菜单
                MenuTemplate menuTemplate = new MenuTemplate();
                Inventory menu = menuTemplate.getMenu();
                System.out.println("我执行了几次");
                player.openInventory(menu);
            });
        }
    }
    public static RookiePostBox getInstance() {
        return instance;
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent uiCloseEvent){
        Player player = (Player) uiCloseEvent.getPlayer();
        player.openInventory(uiCloseEvent.getInventory());
        player.sendMessage("不能关闭！");
        String title = uiCloseEvent.getView().getTitle();
        if (title.equalsIgnoreCase("test")){
            uiCloseEvent.getPlayer().sendMessage("邮件已经发送给对方");
            //发邮件逻辑
            Player sender =(Player) uiCloseEvent.getPlayer();
            ItemStack[] contents = uiCloseEvent.getInventory().getContents();
            Package aPackage = Package.builder().createTime(new Date()).message("新年好!").build();
            aPackage.setUpItemContent(contents);
            Player receiver = RookiePostBox.giftInv.get(sender);
            aPackage.setUpOtherInfo(sender,RookiePostBox.giftInv.get(sender));
            PostBox postBox = PostBox.builder().ownerUUID(receiver.getUniqueId().toString()).build();
            MongoDBManager manager = new MongoDBManager();
            manager.connect("mongodb://localhost:27017", "postBox");
            PostBox postBoxById = manager.getPostBoxById(receiver.getUniqueId().toString());
            manager.insertPackage(aPackage);
            ObjectId id = aPackage.getId();
            postBox.addPackage(aPackage);
            if (postBoxById==null){
                manager.insertBox(postBox);
            }
            manager.close();
        }


    }
    // 定义 /test 命令的处理逻辑
    public class TestCommandExecutor implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                // 当玩家输入 /test 时发送消息 666
                if (args.length<1){
                    System.out.println("请输入/test 接收者id");
                    return false;
                }
                String receiver = args[0];
                RookiePostBox.giftInv.put(player,Bukkit.getPlayer(receiver));
                Test.openDiamondInventory(player);
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
