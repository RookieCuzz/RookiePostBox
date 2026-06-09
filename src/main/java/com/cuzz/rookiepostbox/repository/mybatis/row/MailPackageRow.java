package com.cuzz.rookiepostbox.repository.mybatis.row;

import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.domain.enumtype.MailReadState;
import com.cuzz.rookiepostbox.domain.enumtype.MailSenderType;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MailPackageRow {
    private Long id;
    private String mailboxOwnerUuid;
    private String senderUuid;
    private String senderNameSnapshot;
    private MailSenderType senderType;
    private String messageText;
    private MailReadState readState;
    private MailLifecycleState lifecycleState;
    private String requestId;
    private String claimToken;
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
