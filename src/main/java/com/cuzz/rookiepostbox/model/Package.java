package com.cuzz.rookiepostbox.model;

import com.cuzz.rookiepostbox.model.item.AbstractItem;
import com.cuzz.rookiepostbox.model.item.NonAdminItem;
import dev.morphia.annotations.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
@Data
@Entity(value = "packages",discriminator="package")
@NoArgsConstructor  // 生成无参构造函数
@AllArgsConstructor // 生成全参构造函数
public class Package {

    @Id
    private ObjectId id;  // 使用 MongoDB 的 ObjectId 作为主键

    @Builder.Default
    private List<AbstractItem> itemContent=new ArrayList<>();

    public boolean setUpItemContent(ItemStack[] contents){

        Stream<ItemStack> stream = Arrays.stream(contents);
        stream.forEach(item->{
            this.itemContent.add(new NonAdminItem(item,null));
        });
        return true;
    }
    public boolean setUpOtherInfo(Player sender,Player receiver){
        this.owner=receiver.getDisplayName();
        this.ownerUUID=receiver.getUniqueId().toString();
        this.senderName=sender.getDisplayName();
        this.senderUUID=sender.getUniqueId().toString();
        return true;
    }
    public boolean addItem(AbstractItem abstractItem){

        itemContent.add(abstractItem);
        return true;
    }
    public boolean removeItem(AbstractItem abstractItem){

        itemContent.remove(abstractItem);
        return true;
    }
    private String message;

    private Date createTime;

    private Date receiveTime;

    @Indexed
    private String senderName;
    private String senderUUID;
    @Indexed
    private String owner;

    private String ownerUUID;
}
