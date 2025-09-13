package com.cuzz.rookiepostbox.command;

import com.cuzz.rookiepostbox.RookiePostBox;
import com.cuzz.rookiepostbox.menu.pagination.PostBoxMenu;
import com.cuzz.rookiepostbox.model.Package;
import com.cuzz.rookiepostbox.model.item.AdminItem;
import com.cuzz.rookiepostbox.nms.Toast;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * RookiePostBox插件的命令执行器
 * 处理所有与邮箱相关的命令
 */
public class PostBoxCommandExecutor implements CommandExecutor {

    private final RookiePostBox plugin;

    public PostBoxCommandExecutor(RookiePostBox plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此命令只能由玩家执行！");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("请使用 /rookiepostbox menu 或 /rookiepostbox save <消息>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "menu" -> {
                openPostBoxMenu(player);
                return true;
            }
            case "save" -> {
                return handleSaveCommand(player, args);
            }
            default -> {
                player.sendMessage("未知命令！请使用 /rookiepostbox menu 或 /rookiepostbox save <消息>");
                return true;
            }
        }
    }

    /**
     * 打开邮箱菜单
     */
    private void openPostBoxMenu(Player player) {
        plugin.getOdalitaMenus().openMenu(new PostBoxMenu(), player);
    }

    /**
     * 处理保存命令
     */
    private boolean handleSaveCommand(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("请输入/rookiepostbox save 消息内容");
            return false;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage("请先选择一个物品");
            return false;
        }

        // 创建包裹
        Package aPackage = Package.builder()
                .ownerUUID(player.getUniqueId().toString())
                .senderName(player.getName())
                .message(args[1])
                .createTime(new Date())
                .build();

        // 创建管理员物品
        AdminItem adminItem = new AdminItem();
        adminItem.setAmount(item.getAmount());
        adminItem.setStoreID("测试商品");
        adminItem.setBukkitItem(item);
        adminItem.setItemDisplayName(item.getItemMeta().hasDisplayName() 
                ? item.getItemMeta().getDisplayName() 
                : item.getType().name());
        
        String serializedItem = adminItem.serializeItemStackToBase64(item);
        adminItem.setBase64Item(serializedItem);
        aPackage.addItem(adminItem);

        // 保存到数据库
        boolean success = plugin.getMongoDbManager().addPackageToPostBox(aPackage, player, true);
        System.out.println(success ? "保存成功" : "保存失败");

        // 显示Toast通知
        Toast.displayTo(player, item.getType().toString().toLowerCase(), 
                "已发送：" + args[1], Toast.Style.TASK);

        return true;
    }
}