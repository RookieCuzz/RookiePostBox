package com.cuzz.rookiepostbox.api;

import com.cuzz.rookiepostbox.RookiePostBox;
import com.cuzz.rookiepostbox.database.MongoDBManager;
import com.cuzz.rookiepostbox.model.Package;
import com.cuzz.rookiepostbox.model.PostBox;
import com.cuzz.rookiepostbox.model.item.AdminItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PackageAPI {
    public void sendPackage(@NotNull Player sender, @NotNull Player receiver, @NotNull String message, @NotNull ItemStack item){ //发送包裹
        com.cuzz.rookiepostbox.model.Package aPackage = Package.builder().ownerUUID(sender.getUniqueId().toString())
                .senderName(sender.getName())
                .message(message)
                .createTime(new Date()).build();
        AdminItem adminItem = new AdminItem();
        adminItem.setAmount(item.getAmount());
        adminItem.setStoreID("测试商品");
        adminItem.setBukkitItem(item);
        adminItem.setItemDisplayName(item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name());
        String string = adminItem.serializeItemStackToBase64(item);
        adminItem.setBase64Item(string);
        aPackage.addItem(adminItem);
        boolean t = RookiePostBox.getInstance().getMongoDbManager().addPackageToPostBox(aPackage, receiver, true);
    }

    public void deletePackage(@NotNull Player player, @NotNull String packageID){
        PostBox postBox = RookiePostBox.getInstance().getMongoDbManager().getPostBoxByPlayer(player);
        boolean t = RookiePostBox.getInstance().getMongoDbManager().deletePackageFromPostBox(packageID, postBox);
    }

    public List<String> getPackageIdsByPlayer(@NotNull Player player){
        List<String> packages = new ArrayList<>();
        PostBox postBox = RookiePostBox.getInstance().getMongoDbManager().getPostBoxByPlayer(player);
        if (postBox != null){
            postBox.getPackages().forEach(packagex -> packages.add(packagex.getId().toString()));
        }
        return packages;
    }
}
