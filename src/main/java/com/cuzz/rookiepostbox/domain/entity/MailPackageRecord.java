package com.cuzz.rookiepostbox.domain.entity;

import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.domain.enumtype.MailReadState;
import com.cuzz.rookiepostbox.domain.enumtype.MailSenderType;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class MailPackageRecord {
    private Long id;
    private UUID mailboxOwnerUuid;
    private UUID senderUuid;
    private String senderNameSnapshot;
    private MailSenderType senderType;
    private String messageText;
    private MailReadState readState;
    private MailLifecycleState lifecycleState;
    private UUID requestId;
    private UUID claimToken;
    private Integer version;
    private String sourcePlugin;
    private String adminNote;
    private OffsetDateTime createdAt;
    private OffsetDateTime readAt;
    private OffsetDateTime claimStartedAt;
    private OffsetDateTime claimedAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime deletedAt;
    private OffsetDateTime updatedAt;
}
