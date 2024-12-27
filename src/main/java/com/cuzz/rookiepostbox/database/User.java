package com.cuzz.rookiepostbox.database;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;

@Entity("users")  // 映射到 "users" 集合
public class User {

    @Id  // 指定此字段为 MongoDB 的 _id 字段
    private String id;

    @Property("username")  // 将 name 映射为 "username"
    private String name;

    private String email;

    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    // Getters 和 Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
