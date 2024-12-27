package com.cuzz.rookiepostbox;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.mappings.IRegistryHolder;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PacketEventsPacketListener implements PacketListener {

    public static Component queueComponent;
    public static Integer count=0;
    private BukkitTask countdownTask;
    private int countdownValue=0;
    @Override
    public void onPacketReceive(PacketReceiveEvent event){

        //接受玩家发来的包 ,若发来的包是 关闭gui  gui可能是很多类型 需要判断
        if (event.getPacketType()==PacketType.Play.Client.CLOSE_WINDOW){

        WrapperPlayClientCloseWindow datapack=new WrapperPlayClientCloseWindow((event));
            int windowId1 = datapack.getWindowId();



        }
//            ItemStack carriedItemStack = wrapperPlayClientClickWindow.getCarriedItemStack();
//        if (carriedItemStack==null){
//            return;
//        }
//        System.out.println(carriedItemStack.getType().getName());
//        System.out.println("点击了"+slot+" 物品为"+carriedItemStack.getType().getName());
//        Player player=event.getPlayer();
//
//        int windowId = wrapperPlayClientClickWindow.getWindowId();
//        player.sendMessage("点击的窗口id"+windowId);
//        player.sendMessage("map大小"+Test.hashMap.keySet());
//        Collection<Inventory> values = Test.hashMap.values();
//        for (Inventory inv: values){
//            player.sendMessage("inv"+inv.toString());
//            player.sendMessage("size"+inv.getContents().length);
//            player.sendMessage("+++++++++++++++++++++++");
//        }
//        Inventory itemStacks = Test.hashMap.get(String.valueOf(windowId));
//        if (itemStacks!=null){
//            player.sendMessage("取消你的点击");
//            event.setCancelled(true);
//            // 手动更新玩家的界面，确保客户端与服务器同步
//            Bukkit.getScheduler().runTask(RookiePostBox.getInstance(), player::updateInventory);
//        }
        }

    private void startCountdown(Player player) {
        countdownValue = 10;  // 设置倒计时初始值

        // 启动每秒更新标题的任务
        countdownTask = Bukkit.getScheduler().runTaskTimerAsynchronously(RookiePostBox.getInstance(), new Runnable() {
            @Override
            public void run() {
                if (countdownValue > 0) {
                    // 发送 OPEN_WINDOW 封包，更新标题
                    updateWindowTitle(player, countdownValue);
                    countdownValue--;
                } else {
                    // 倒计时结束后停止任务
                    countdownTask.cancel();
                }
            }
        }, 0L, 100L);  // 每秒（20 tick）执行一次
    }
    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            // 获取玩家对象
            Player player = (Player) event.getPlayer();

            WrapperPlayServerOpenWindow wrapperPlayServerOpenWindow = new WrapperPlayServerOpenWindow(event);
            int type = wrapperPlayServerOpenWindow.getType();
            System.out.println("打开的类型是"+type);
            int containerId = wrapperPlayServerOpenWindow.getContainerId();
            player.sendMessage("打开的窗口id" + containerId);
            Inventory topInventory = player.getOpenInventory().getTopInventory();
            Test.hashMap.put(String.valueOf(containerId),topInventory);
            System.out.println(topInventory.getContents().length);
            System.out.println(topInventory.getItem(1).getType());
            System.out.println(containerId+":  "+topInventory.getItem(1).hashCode());
        }
    }

    private void updateWindowTitle(Player player, int countdown) {
        // 构建新的窗口标题
        String newTitle = "倒计时: " + countdown;
        Component titleComponent = Component.text(newTitle);

        // 获取当前窗口的 ID（假设为1，窗口ID可能需要动态获取）
        int windowId = 1;

        // 发送新的 OPEN_WINDOW 封包，更新标题
        WrapperPlayServerOpenWindow openWindowPacket = new WrapperPlayServerOpenWindow(windowId, 5, titleComponent);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, openWindowPacket);

        // 获取当前箱子的物品并重新发送物品数据
        // 获取当前箱子的物品并转换为 PacketEvents 的 ItemStack 类型
        org.bukkit.inventory.ItemStack[] bukkitItems = player.getOpenInventory().getTopInventory().getContents();
        ItemStack[] packetItems = new ItemStack[bukkitItems.length];


        for (int i = 0; i < bukkitItems.length; i++) {
            org.bukkit.inventory.ItemStack bukkitItem = bukkitItems[i];
            ItemStack itemStack = SpigotConversionUtil.fromBukkitItemStack(bukkitItem);
            packetItems[i]=itemStack;
        }
        WrapperPlayServerWindowItems windowItemsPacket = new WrapperPlayServerWindowItems(windowId,++countdown, Arrays.asList(packetItems),null);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, windowItemsPacket);
    }

    // 创建并发送自定义窗口给玩家
    public void sendOpenWindowPacket(Player player,int id,int type,WrapperPlayServerOpenWindow openWindowPacket) {
        // 构建自定义的窗口标题
        Component windowTitle = Component.text("Custom Window Title"+count)
                .color(NamedTextColor.AQUA);

        openWindowPacket.setTitle(windowTitle);

        // 发送封包给玩家
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, openWindowPacket);
    }
}