package com.cuzz.rookiepostbox.service;

import com.cuzz.bukkitspring.api.annotation.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public final class MailboxClaimLockManager {

    private final Set<Long> inFlightClaims = ConcurrentHashMap.newKeySet();

    public boolean tryLock(long packageId) {
        return inFlightClaims.add(packageId);
    }

    public void unlock(long packageId) {
        inFlightClaims.remove(packageId);
    }
}
