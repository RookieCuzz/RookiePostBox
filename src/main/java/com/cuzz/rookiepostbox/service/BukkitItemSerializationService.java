package com.cuzz.rookiepostbox.service;


import com.cuzz.rookiepostbox.service.spi.ItemSerializationService;
import com.cuzz.bukkitspring.api.annotation.Service;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public final class BukkitItemSerializationService implements ItemSerializationService {

    @Override
    public String serialize(ItemStack itemStack) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(byteOut)) {
            out.writeObject(itemStack);
            out.flush();
            return Base64.getEncoder().encodeToString(byteOut.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize ItemStack.", exception);
        }
    }

    @Override
    public ItemStack deserialize(String serializedItem) {
        byte[] data = Base64.getDecoder().decode(serializedItem);
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
             BukkitObjectInputStream in = new BukkitObjectInputStream(byteIn)) {
            return (ItemStack) in.readObject();
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Failed to deserialize ItemStack.", exception);
        }
    }
}
