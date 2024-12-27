package com.cuzz.rookiepostbox;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Test {

    public static HashMap<String,Inventory> hashMap=new HashMap<>();

    public static void openDiamondInventory(Player player) {
        // 创建一个45格的物品栏，标题为 "test"
        Inventory inventory = Bukkit.createInventory(null, 45, "test");

        // 用钻石填满物品栏
        ItemStack diamondStack = new ItemStack(Material.DIAMOND, 64); // 一组64个钻石
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, diamondStack); // 将每个格子都填满钻石
        }

        // 给玩家打开物品栏
        player.openInventory(inventory);
    }



        public static void main(String[] args) {
            // 创建一个Map
            Map<String, String> map = new HashMap<>();
            map.put("a", "apple");
            map.put("b", "banana");
            map.put("c", "cherry");

            // 获取Map的keySet
            Set<String> keySet = map.keySet();
            Iterator<String> iterator = keySet.iterator();

            // 使用Iterator删除元素
            while (iterator.hasNext()) {
                String key = iterator.next();
                if ("b".equals(key)) {
                    iterator.remove();  // 从Map中删除key "b" 对应的条目
                }
            }

            // 输出修改后的Map
            System.out.println(map);  // 输出: {a=apple, c=cherry}
        }



}
