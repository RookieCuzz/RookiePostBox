package com.cuzz.rookiepostbox.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
@Builder
@Data
@Entity(value = "PostBoxes", discriminator = "PostBox")
@NoArgsConstructor  // 生成无参构造函数
@AllArgsConstructor // 生成全参构造函数
public class PostBox {


    @Builder.Default
    @Reference
    private List<Package> packages=new ArrayList<>();

    @Id
    private String ownerUUID;
    @Transient
    private Player owner;

    public int getPackagesSize(){

        return this.packages.size();
    }

    public boolean deletePackage(Package packageX){
        this.packages.remove(packageX);
        return true;
    }

    public boolean addPackage(Package packageX){
        this.packages.add(packageX);

        return true;
    }
}
