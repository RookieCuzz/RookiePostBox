package com.cuzz.rookiepostbox;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MenuTemplate {

    private ItemStack closeIcon;
    private ItemStack infoIcon;
    private ItemStack blankIcon;

    // 初始化图标
    {
        // 初始化关闭图标
        closeIcon = createIcon(Material.BARRIER, ChatColor.RED + "关闭");

        // 初始化信息图标
        infoIcon = createIcon(Material.PAPER, ChatColor.AQUA + "信息");

        // 初始化空白图标
        blankIcon = createIcon(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    // 创建一个图标的方法，简化初始化过程
    private ItemStack createIcon(Material material, String displayName) {
        ItemStack itemStack = new ItemStack(material, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(displayName);
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    // 获取关闭图标
    public ItemStack getCloseIcon() {
        return closeIcon;
    }

    // 获取信息图标
    public ItemStack getInfoIcon() {
        return infoIcon;
    }

    // 获取空白图标
    public ItemStack getBlankIcon() {
        return blankIcon;
    }

    // 创建并返回一个 45 格的 Inventory，第一格是关闭图标，第九格是信息图标
    public Inventory getMenu() {
        // 创建一个45格的Inventory
        Inventory menu = Bukkit.createInventory(null, 45, ChatColor.GOLD + "Menu Template");

        // 将关闭图标放在第一格（索引0）
        menu.setItem(0, getCloseIcon());

        // 将信息图标放在第九格（索引8）
        menu.setItem(8, getInfoIcon());

        // 将空白图标填充其他格子（根据需要）
        for (int i = 0; i < 45; i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, getBlankIcon());
            }
        }

        return menu;
    }
}

