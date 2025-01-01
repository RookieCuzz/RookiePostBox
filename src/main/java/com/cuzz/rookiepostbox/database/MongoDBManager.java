package com.cuzz.rookiepostbox.database;

import com.cuzz.rookiepostbox.model.Package;
import com.cuzz.rookiepostbox.model.PostBox;
import com.cuzz.rookiepostbox.model.item.AdminItem;
import com.cuzz.rookiepostbox.model.item.NonAdminItem;
import com.mongodb.DBRef;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Morphia;
import dev.morphia.Datastore;

import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filter;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperator;
import dev.morphia.query.experimental.updates.UpdateOperators;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MongoDBManager implements Dao {

    private Datastore datastore;
    private MongoClient mongoClient;

    // 连接到 MongoDB 并初始化 Datastore
    public void connect(String connectionString, String dbName) {
        try {
            // 创建 MongoDB 客户端
            mongoClient = MongoClients.create(connectionString);
//            ManualMorphiaConfig configure = ManualMorphiaConfig.configure();
//            List<String> list = Arrays.asList("com.cuzz.rookiepostbox");
////            ManualMorphiaConfig packages = (ManualMorphiaConfig)configure.packages(list).database("test")
////                    .applyIndexes(true).autoImportModels(true)
////                    .applyDocumentValidations(true);
//            // 创建 Datastore，Morphia 会自动扫描并映射所有实体类
            datastore = Morphia.createDatastore(mongoClient,dbName);
            datastore.getMapper().mapPackage("com.cuzz.rookiepostbox");
            datastore.getMapper().map(AdminItem.class);
            datastore.getMapper().map(PostBox.class);
            datastore.getMapper().map(Package.class);
            datastore.getMapper().map(NonAdminItem.class);
            System.out.println("MongoDB connected and datastore initialized.");
        } catch (Exception e) {
            System.err.println("Error connecting to MongoDB: " + e.getMessage());
        }
    }




    @Override
    public boolean addPackageToPostBox(Package packageX, String uuid, boolean isOnline) {
        if (datastore != null) {
            DBRef dbRef = new DBRef("packages",packageX.getId());
            UpdateOperator pullOperator = UpdateOperators.addToSet("packages",dbRef);
            this.datastore.find(PostBox.class).filter(Filters.eq("_id",uuid)).update(pullOperator).execute();
            return true;
        } else {
            System.err.println("Datastore is not initialized.");
            return false;
        }
    }


    // 关闭 MongoDB 连接
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB connection closed.");
        } else {
            System.err.println("MongoClient is not initialized.");
        }
    }

//    public boolean updateBox(String owner,OB){
//        // 创建更新操作
//
//        final Query<PostBox> exitBox = datastore.find(PostBox.class)
//                .filter(Filters.eq("_id", owner));
//        UpdateOperator updateOperator = new UpdateOperator("add",);
//        exitBox.update()
//
//    }



//    public static void main(String[] args) throws InterruptedException {
//        // 初始化 MongoDB 管理器
//        MongoDBManager manager = new MongoDBManager();
//        manager.connect("mongodb://localhost:27017", "postBox");
//
//
//        PostBox postBoxByUUID = manager.getPostBoxByUUID("hi");
//        Package aPackage = postBoxByUUID.getPackages().get(0);
//        System.out.println(aPackage.getItemContent().get(0).getBase64Item());
//        manager.close();
//    }
    // 创建一个 PostBox 对象
//    public static Package getPackage(){
//        Package aPackage = Package.builder()
//                .message("这是给你的礼物")
//                .createTime(new Date())
//                .senderName("胡伟")
//                .senderUUID(UUID.randomUUID().toString())
//                .owner("你好")
//                .ownerUUID("test uuid")
//                .build();
//
//        AdminItem adminItem = new AdminItem();
//        adminItem.setStoreID("屠龙宝刀");
//        adminItem.setBase64Item("111111111111111111111111");
//        adminItem.setUniqueKey(UUID.randomUUID().toString());
//        adminItem.setAmount(2);
//
//        // 添加多件物品
//        aPackage.addItem(adminItem);
//        aPackage.addItem(adminItem);
//        aPackage.addItem(adminItem);
//
//        return aPackage;
//
//    }




    @Override
    public boolean createNewPostBox(String ownerUUID, String ownerName) {
        PostBox postbox = PostBox.builder().ownerUUID(ownerUUID).build();
        if (datastore != null) {
            this.datastore.save(postbox);
            return true;
        } else {
            System.err.println("Datastore is not initialized.");
            return false;
        }
    }

    @Override
    public PostBox getPostBoxByUUID(String ownerUUID) {

        if (datastore != null) {
            PostBox postBox = this.datastore.find(PostBox.class).
                    filter(Filters.eq("_id", ownerUUID)).first();
            return postBox;
        } else {
            System.err.println("Datastore is not initialized.");
            return null;
        }

    }

    @Override
    public PostBox getPostBoxByPlayer(Player player) {
        return this.getPostBoxByUUID(player.getUniqueId().toString());

    }

    @Override
    public boolean deletePackageFromPostBox(Package packageX, PostBox postBox) {
        String ownerUUID = postBox.getOwnerUUID();
        if (datastore != null) {
            DBRef dbRef = new DBRef("packages", packageX.getId());
            Filter filter = Filters.and(
                    Filters.eq("$ref", "packages"),
                    Filters.eq("$id", packageX.getId())
            );
            // 创建过滤条件，用于匹配要移除的 Package 引用
            UpdateOperator pullOperator = UpdateOperators.pull("packages", filter);
            this.datastore.find(PostBox.class).filter(Filters.eq("_id",ownerUUID)).update(pullOperator).execute();
            return true;
        } else {
            System.err.println("Datastore is not initialized.");
            return false;
        }

    }

    @Override
    public boolean deletePackageFromPostBox(Package packageX, String uuid) {
        PostBox postBoxByUUID = this.getPostBoxByUUID(uuid);
        this.deletePackageFromPostBox(packageX,postBoxByUUID);
        return true;
    }

    @Override
    public Package savePackageToDB(Package packageX) {
        if (datastore != null) {
            return this.datastore.save(packageX);
        } else {
            System.err.println("Datastore is not initialized.");
            return null;
        }
    }
}
