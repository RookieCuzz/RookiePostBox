package com.cuzz.rookiepostbox.service;

import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.domain.enumtype.MailReadState;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class AdminMailboxView {
    long packageId;
    String senderName;
    String message;
    String firstItemDisplayName;
    int firstItemAmount;
    MailReadState readState;
    MailLifecycleState lifecycleState;
    String adminNote;
    OffsetDateTime createdAt;
}
