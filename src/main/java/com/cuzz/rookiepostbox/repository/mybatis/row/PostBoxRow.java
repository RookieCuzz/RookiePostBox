package com.cuzz.rookiepostbox.repository.mybatis.row;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class PostBoxRow {
    private String ownerUuid;
    private String ownerNameCache;
    private boolean mailboxEnabled;
    private int unreadCount;
    private OffsetDateTime lastOpenedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
