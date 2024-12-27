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

    // 插入用户
    public void insertUser(User user) {
        if (datastore != null) {
            datastore.save(user);
            System.out.println("User inserted!");
        } else {
            System.err.println("Datastore is not initialized.");
        }
    }
    public synchronized   void insertBox(PostBox box) {
        if (datastore != null) {
            datastore.save(box);
            System.out.println("box inserted!");
        } else {
            System.err.println("Datastore is not initialized.");
        }
    }
    public synchronized   void insertPackage(Package packageX) {
        if (datastore != null) {
            datastore.save(packageX);
            System.out.println("packageX inserted!");
        } else {
            System.err.println("Datastore is not initialized.");
        }
    }
    // 根据 ID 查询用户
    public User getUserById(String id) {
        if (datastore != null) {
            Query<User> query = datastore.find(User.class)
                    .filter(Filters.eq("_id",id));  // 使用 Filters.eq 进行 ID 过滤
            return query.first();
        } else {
            System.err.println("Datastore is not initialized.");
            return null;
        }
    }
    public PostBox getPostBoxById(String id) {
        if (datastore != null) {
            Query<PostBox> query = datastore.find(PostBox.class)
                    .filter(Filters.eq("_id",id));  // 使用 Filters.eq 进行 ID 过滤
            return query.first();
        } else {
            System.err.println("Datastore is not initialized.");
            return null;
        }
    }
    // 更新用户
    public void updateUserName(String id, String newName) {
        User user = getUserById(id);
        if (user != null) {
            user.setName(newName);
            insertUser(user);  // 保存更新后的对象
        } else {
            System.err.println("User not found.");
        }
    }

    // 删除用户
    public void deleteUserById(String id) {
        if (datastore != null) {
            datastore.find(User.class)
                    .filter(Filters.eq("_id", id))
                    .delete();
            System.out.println("User deleted!");
        } else {
            System.err.println("Datastore is not initialized.");
        }
    }

    // 关闭 MongoDB 连接
    // 关闭 MongoDB 连接
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB connection closed.");
        } else {
            System.err.println("MongoClient is not initialized.");
        }
    }
    public Package getPackageByOwnerName(String string){
        if (datastore != null) {
            Query<Package> query = datastore.find(Package.class)
                    .filter(Filters.eq("owner",string));  // 使用 Filters.eq 进行 ID 过滤
            return query.first();
        } else {
            System.err.println("Datastore is not initialized.");
            return null;
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

    public boolean addPostBox(PostBox postBox){
        String ownerUUID = postBox.getOwnerUUID();
        PostBox postBoxById = getPostBoxById(ownerUUID);
        if (postBoxById==null){
            System.out.println("此玩家没有邮箱,为他新建邮箱");
            insertBox(postBox);
            return true;
        }else {
            System.out.println("该玩家已有邮箱,更新包裹内容");
            List<Package> packages = postBox.getPackages();
            packages.stream().forEach(item->{
                postBoxById.addPackage(item);
            });
            insertBox(postBoxById);
            return true;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 初始化 MongoDB 管理器
        MongoDBManager manager = new MongoDBManager();
        manager.connect("mongodb://localhost:27017", "postBox");


        PostBox postBoxByUUID = manager.getPostBoxByUUID("hi");
        Package aPackage = postBoxByUUID.getPackages().get(0);
        System.out.println(aPackage.getItemContent().get(0).getBase64Item());
//        Thread.sleep(10000);
//        manager.deletePackageFromPostBox(aPackage,"hi");
//        NonAdminItem nonAdminItem = new NonAdminItem();
//        nonAdminItem.setAmount(333);
//        nonAdminItem.setBase64Item("非系统物品的id");
//        aPackage.addItem(adminItem);
//        aPackage.addItem(nonAdminItem);
//        manager.insertPackage(aPackage);
//        postBoxById.addPackage(aPackage);
//        postBoxById.addPackage(aPackage);
//        manager.insertBox(postBoxById);
//        Query<Package> packages = manager.datastore.find(Package.class);
//        System.out.println(packages.stream().count());
//        PostBox cppppp = createPostBox("CPPPPP");
//        Package aPackage = getPackage();
//        cppppp.addPackage(aPackage);
//        manager.insertPackage(aPackage);
//        manager.insertBox(cppppp);
//        // 创建一个固定大小的线程池
//        ExecutorService executorService = Executors.newFixedThreadPool(10); // 创建 10 个线程的线程池
//
//        for (int i = 0; i < 10; i++) {
//            int finalI = i;
//            executorService.submit(() -> {
//                try {
//                    UUID uuid = UUID.randomUUID();
//                    System.out.println("任务" + finalI);
//
//                    // 插入数据
//                    PostBox abox = createPostBox("ownerUUID_" + uuid + finalI);
//                    manager.insertBox(abox);
//
////                    // 查询数据
////                    PostBox queriedBox = manager.getPostBoxById("ownerUUID_" + uuid + finalI);
////                    System.out.println("Task ID: " + finalI + ", Queried packages size: " + (queriedBox != null ? queriedBox.getPackagesSize() : "Not found"));
//                } catch (Exception e) {
//                    // 捕获并打印任何异常
//                    System.err.println("任务 " + finalI + " 出现异常");
//                    e.printStackTrace();
//                }
//            });
//        }


        // 关闭线程池
//        executorService.shutdown();
//        if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
//            executorService.shutdownNow();
//        }

        // 关闭 MongoDB 连接
        manager.close();
    }
    // 创建一个 PostBox 对象
    public static Package getPackage(){
        Package aPackage = Package.builder()
                .message("这是给你的礼物")
                .createTime(new Date())
                .senderName("胡伟")
                .senderUUID(UUID.randomUUID().toString())
                .owner("你好")
                .ownerUUID("test uuid")
                .build();

        AdminItem adminItem = new AdminItem();
        adminItem.setStoreID("屠龙宝刀");
        adminItem.setBase64Item("111111111111111111111111");
        adminItem.setUniqueKey(UUID.randomUUID().toString());
        adminItem.setAmount(2);

        // 添加多件物品
        aPackage.addItem(adminItem);
        aPackage.addItem(adminItem);
        aPackage.addItem(adminItem);

        return aPackage;

    }
    public static PostBox createPostBox(String ownerUUID) {
        PostBox abox = PostBox.builder().ownerUUID(ownerUUID).build();
        return abox;
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
