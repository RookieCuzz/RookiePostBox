package com.cuzz.rookiepostbox.cache;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.rookiepostbox.bootstrap.StarterBeanBridge;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public final class MailboxPageStateCache {

    private static final String CACHE_NAME = "rookiepostbox-mailbox-page-state";
    private static final String CAFFEINE_SERVICE_CLASS = "com.cuzz.starter.bukkitspring.caffeine.api.CaffeineService";

    private final Object caffeineService;
    private final ConcurrentHashMap<UUID, Integer> currentPages = new ConcurrentHashMap<>();

    @Autowired
    public MailboxPageStateCache() {
        this.caffeineService = StarterBeanBridge.getGlobalBean(CAFFEINE_SERVICE_CLASS);
    }

    public int getCurrentPage(UUID playerUuid) {
        Integer page = hasCaffeine() ? getCachedPage(playerUuid) : currentPages.get(playerUuid);
        return page == null ? 0 : Math.max(page, 0);
    }

    public void setCurrentPage(UUID playerUuid, int page) {
        int normalizedPage = Math.max(page, 0);
        if (hasCaffeine()) {
            StarterBeanBridge.invoke(
                    caffeineService,
                    "typedPut",
                    new Class<?>[]{String.class, Object.class, Object.class},
                    CACHE_NAME,
                    playerUuid,
                    normalizedPage
            );
            return;
        }
        currentPages.put(playerUuid, normalizedPage);
    }

    public void clear(UUID playerUuid) {
        if (hasCaffeine()) {
            StarterBeanBridge.invoke(
                    caffeineService,
                    "invalidate",
                    new Class<?>[]{String.class, Object.class},
                    CACHE_NAME,
                    playerUuid
            );
            return;
        }
        currentPages.remove(playerUuid);
    }

    public void clearAll() {
        if (hasCaffeine()) {
            StarterBeanBridge.invoke(
                    caffeineService,
                    "invalidateAll",
                    new Class<?>[]{String.class},
                    CACHE_NAME
            );
            return;
        }
        currentPages.clear();
    }

    private boolean hasCaffeine() {
        return caffeineService != null && StarterBeanBridge.invokeBoolean(caffeineService, "isEnabled");
    }

    private Integer getCachedPage(UUID playerUuid) {
        Object page = StarterBeanBridge.invoke(
                caffeineService,
                "typedGetIfPresent",
                new Class<?>[]{String.class, Object.class},
                CACHE_NAME,
                playerUuid
        );
        return page instanceof Integer ? (Integer) page : null;
    }
}
