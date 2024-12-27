package com.cuzz.rookiepostbox.model.item;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Transient;
import lombok.Data;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
@Entity(discriminator = "AbstractItem")
@Data
public abstract class AbstractItem implements SerializeItem {

    private String base64Item;

    @Transient
    private ItemStack bukkitItem;

    private String itemDisplayName;

    private  Integer amount;
    public AbstractItem(){

    }

    public AbstractItem(ItemStack itemStack, @Nullable String itemDisplayName){
        this.bukkitItem=itemStack;
        this.base64Item=this.serializeItemStackToBase64(itemStack);
        if (itemDisplayName!=null&&!itemDisplayName.equalsIgnoreCase("")){
            this.itemDisplayName=itemDisplayName;
        }else {
            this.itemDisplayName=itemStack.getItemMeta().getDisplayName();
        }
        this.amount=itemStack.getAmount();
    }
    public String serializeItemStackToBase64(ItemStack item) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        BukkitObjectOutputStream out = null;

        try {
            // 序列化对象
            out = new BukkitObjectOutputStream(byteOut);
            out.writeObject(item);
            out.flush();  // 确保所有数据都写入输出流

            // 将序列化的字节数组转换为Base64字符串
            return Base64.getEncoder().encodeToString(byteOut.toByteArray());
        } catch (IOException exception) {
            exception.printStackTrace();
            // 如果发生异常，返回null或你自定义的错误处理
            return null;
        } finally {
            // 关闭输出流，并检查是否为null
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    public ItemStack deserializeItemStackFromBase64(String base64) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(base64);
        ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
        BukkitObjectInputStream in = new BukkitObjectInputStream(byteIn);
        ItemStack item = (ItemStack) in.readObject();
        in.close();
        return item;
    }


}
