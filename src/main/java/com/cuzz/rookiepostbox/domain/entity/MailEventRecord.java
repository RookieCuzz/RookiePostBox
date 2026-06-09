package com.cuzz.rookiepostbox.domain.entity;

import com.cuzz.rookiepostbox.domain.enumtype.MailEventType;
import com.cuzz.rookiepostbox.domain.enumtype.MailSenderType;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class MailEventRecord {
    private Long id;
    private Long packageId;
    private UUID actorUuid;
    private String actorNameSnapshot;
    private MailSenderType actorType;
    private MailEventType eventType;
    private String payloadJson;
    private OffsetDateTime createdAt;
}
