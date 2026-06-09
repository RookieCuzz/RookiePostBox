package com.cuzz.rookiepostbox.domain.entity;

import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.domain.enumtype.MailReadState;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class InboxMailRecord {
    private Long id;
    private String senderNameSnapshot;
    private String messageText;
    private MailReadState readState;
    private MailLifecycleState lifecycleState;
    private String firstItemDisplayName;
    private Integer firstItemAmount;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
}
