package com.cuzz.rookiepostbox.domain.entity;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class PostBoxRecord {
    private UUID ownerUuid;
    private String ownerNameCache;
    private boolean mailboxEnabled;
    private int unreadCount;
    private OffsetDateTime lastOpenedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
