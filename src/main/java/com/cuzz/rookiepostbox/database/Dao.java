package com.cuzz.rookiepostbox.database;

import com.cuzz.rookiepostbox.model.Package;
import com.cuzz.rookiepostbox.model.PostBox;
import org.bukkit.entity.Player;

public interface Dao {


    //新增某个包裹到邮箱
    boolean addPackageToPostBox(Package packageX, String uuid, boolean isOnline);

    //创建一个新邮箱
    boolean createNewPostBox(String ownerUUID,String ownerName);


    //通过一个玩家的uuid获取一个玩家的邮箱
    PostBox getPostBoxByUUID(String ownerUUID);

    //通过一个玩家获取其邮箱
    PostBox getPostBoxByPlayer(Player player);


    // 从玩家邮箱中删除某个包裹
    boolean deletePackageFromPostBox(Package packageX,PostBox postBox);
    public boolean addPackageToPostBox(Package packageX, Player player, boolean isOnline);
    public boolean deletePackageFromPostBox(String packageId, PostBox postBox);
    boolean deletePackageFromPostBox(Package packageX,String uuid);

    Package savePackageToDB(Package packageX);

    public PostBox savePostBoxToDB(PostBox postBox);
}
