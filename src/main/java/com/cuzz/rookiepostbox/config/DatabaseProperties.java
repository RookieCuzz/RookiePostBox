package com.cuzz.rookiepostbox.config;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import org.bukkit.configuration.file.FileConfiguration;

@Component
public final class DatabaseProperties {

    private final FileConfiguration configuration;
    private String schema;

    @Autowired
    public DatabaseProperties(FileConfiguration configuration) {
        this.configuration = configuration;
    }

    @PostConstruct
    public void reload() {
        schema = configuration.getString("rookiepostbox.database.schema", "public");
    }

    public String getSchema() {
        return schema;
    }
}
