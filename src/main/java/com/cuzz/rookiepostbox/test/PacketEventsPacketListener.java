package com.cuzz.rookiepostbox.test;

import com.cuzz.rookiepostbox.RookiePostBox;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataType;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.StaticItemType;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static com.github.retrooper.packetevents.protocol.player.DiggingAction.RELEASE_USE_ITEM;

public class PacketEventsPacketListener implements PacketListener {

    public static Component queueComponent;
    @Override
    public void onPacketSend(PacketSendEvent event){
        String packetName = event.getPacketName();
        if (event.getPacketType() == PacketType.Play.Server.ACKNOWLEDGE_BLOCK_CHANGES){

            event.setCancelled(true);
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA){

            WrapperPlayServerEntityMetadata wrapperPlayServerEntityMetadata =new WrapperPlayServerEntityMetadata(event);
            List<EntityData> entityMetadata = wrapperPlayServerEntityMetadata.getEntityMetadata();
            EntityData entityData = entityMetadata.get(0);
            Object value = entityData.getValue();

            EntityDataType<?> type = entityData.getType();
            System.out.println("数据类型"+value);
            event.setCancelled(true);
            return;

        }
        if (packetName.equalsIgnoreCase("CHUNK_DATA")||packetName.equalsIgnoreCase("ENTITY_VELOCITY")
                ||packetName.equalsIgnoreCase("ENTITY_HEAD_LOOK")
                ||packetName.equalsIgnoreCase("ENTITY_RELATIVE_MOVE_AND_ROTATION")
                ||packetName.equalsIgnoreCase("ENTITY_TELEPORT")
                ||packetName.equalsIgnoreCase("ENTITY_RELATIVE_MOVE")
                ||packetName.equalsIgnoreCase("SOUND_EFFECT")
                ||packetName.equalsIgnoreCase("TIME_UPDATE")

        ){
            return;

        }

        System.out.println("发送"+event.getPacketName());

    }
    private void shootFireball(Player player) {
        // 创建火球实体
        Fireball fireball = player.getWorld().spawn(player.getEyeLocation(), Fireball.class);
        // 设置火球的速度和方向
        fireball.setDirection(player.getLocation().getDirection().multiply(2));
        // 可以设置发射者，这样火球不会伤害到发射者自己
        fireball.setShooter(player);
    }
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // The user represents the player.
        if (event.getPacketType() == PacketType.Play.Client.DEBUG_PING){
            return;
        }
        System.out.println(event.getPacketName());
        // Identify what kind of packet it is.

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging wrapperPlayClientPlayerDigging =new WrapperPlayClientPlayerDigging(event);
            String name = wrapperPlayClientPlayerDigging.getAction().name();
            System.out.println("@@@@@@@"+name);
            if (name.equalsIgnoreCase(RELEASE_USE_ITEM.toString())){
                Player player = (Player)event.getPlayer();
                Bukkit.getScheduler().runTask(RookiePostBox.getInstance(),()->{
                   shootFireball(player);
                });
                player.sendMessage("Biu~~~~");



                WrapperPlayServerTransfer wrapperPlayServerTransfer = new WrapperPlayServerTransfer("43.248.186.18",25565);
                System.out.println("tp tp");









                com.github.retrooper.packetevents.protocol.item.ItemStack deitemStack = SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getItem(16));
                WrapperPlayServerSetSlot wrapperPlayServerSetSlot =new WrapperPlayServerSetSlot(
                        0,1,16,deitemStack
                );
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, wrapperPlayServerTransfer);
            }
        }
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            System.out.println("is bow？");
            WrapperPlayClientUseItem wrapperPlayClientUseItem = new WrapperPlayClientUseItem(event);
            com.github.retrooper.packetevents.protocol.item.ItemStack itemStack = SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.ARROW));

            Player player = (Player) event.getPlayer();
            if (player.getInventory().getItemInMainHand().getType() == Material.BOW) {
                WrapperPlayServerSetSlot wrapperPlayServerSetSlot =new WrapperPlayServerSetSlot(
                        0,1,16,itemStack
                );
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, wrapperPlayServerSetSlot);
            }
//            player.sendMessage("给你一个箭");
//        System.out.println("who open a window？");
//        WrapperPlayServerOpenWindow wrapperPlayServerOpenWindow = new WrapperPlayServerOpenWindow(event);
//        int type = wrapperPlayServerOpenWindow.getType();
//        System.out.println(type);
//
//        final Component textComponent = Component.text("我是第一行 ")
//                .color(TextColor.color(0x443344)).font(Key.key("minecraft", "example/line1_font"))// Dark grayish color
//                .append(Component.text("我ꐢ是第二ꐭ行", NamedTextColor.LIGHT_PURPLE).font(Key.key("minecraft", "example/line2_font")) ) // Light purple "Bunny"// Custom f
//                .append(Component.text("我ꐣ是第三ꐮ行", NamedTextColor.YELLOW).font(Key.key("minecraft", "example/line3_font")) )
//                .append(Component.text("我ꐤ是第四ꐯ行", NamedTextColor.GREEN).font(Key.key("minecraft", "example/line4_font")) )
//                .append(Component.text(" to jump!", NamedTextColor.WHITE))
//                .append(Component.text("我是第六行", NamedTextColor.BLUE).font(Key.key("minecraft", "example/line6_font")))
//                .append(Component.text("我是第七行", NamedTextColor.GRAY).font(Key.key("minecraft", "example/line7_font")))
//                .append(Component.text("与众不同!我是第八行", NamedTextColor.DARK_AQUA).font(Key.key("minecraft", "example/line8_font")))
//                .append(Component.text("与众不同!我是第九行", NamedTextColor.DARK_BLUE).font(Key.key("minecraft", "example/line9_font")))
//                .append(Component.text("与众不同!我是第十行", NamedTextColor.DARK_GRAY).font(Key.key("minecraft", "example/line10_font")))
//                ; // White " to jump!"
//        // 使用 Bukkit/Spigot API 创建一个箱子界面，指定行数和标题
//
//        wrapperPlayServerOpenWindow.setTitle(queueComponent);
//        event.setLastUsedWrapper(wrapperPlayClientPlayerDigging);
//        event.markForReEncode(true);}
            return;
        }
    }
}