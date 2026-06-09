package com.cuzz.rookiepostbox.repository.mybatis.row;

import com.cuzz.rookiepostbox.domain.enumtype.MailEventType;
import com.cuzz.rookiepostbox.domain.enumtype.MailSenderType;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MailEventRow {
    private Long id;
    private Long packageId;
    private String actorUuid;
    private String actorNameSnapshot;
    private MailSenderType actorType;
    private MailEventType eventType;
    private String payloadJson;
    private OffsetDateTime createdAt;
}
