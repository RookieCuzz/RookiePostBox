package com.cuzz.rookiepostbox.service;


import com.cuzz.rookiepostbox.service.spi.MailAuditService;
import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Service;
import com.cuzz.rookiepostbox.domain.entity.MailEventRecord;
import com.cuzz.rookiepostbox.domain.enumtype.MailEventType;
import com.cuzz.rookiepostbox.domain.enumtype.MailSenderType;
import com.cuzz.rookiepostbox.repository.spi.MailEventRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public final class DefaultMailAuditService implements MailAuditService {

    private final MailEventRepository mailEventRepository;

    @Autowired
    public DefaultMailAuditService(MailEventRepository mailEventRepository) {
        this.mailEventRepository = mailEventRepository;
    }

    @Override
    public void recordCreated(long packageId, UUID actorUuid, String actorName, MailSenderType actorType, UUID requestId, String sourcePlugin) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId == null ? "" : requestId.toString());
        payload.put("sourcePlugin", sourcePlugin == null ? "" : sourcePlugin);
        append(packageId, actorUuid, actorName, actorType, MailEventType.CREATED, payload);
    }

    @Override
    public void recordRead(long packageId, UUID actorUuid, String actorName) {
        append(packageId, actorUuid, actorName, MailSenderType.PLAYER, MailEventType.READ, Map.of());
    }

    @Override
    public void recordClaimStarted(long packageId, UUID actorUuid, String actorName, UUID claimToken) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("claimToken", claimToken == null ? "" : claimToken.toString());
        append(packageId, actorUuid, actorName, MailSenderType.PLAYER, MailEventType.CLAIM_STARTED, payload);
    }

    @Override
    public void recordClaimed(long packageId, UUID actorUuid, String actorName, UUID claimToken) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("claimToken", claimToken == null ? "" : claimToken.toString());
        append(packageId, actorUuid, actorName, MailSenderType.PLAYER, MailEventType.CLAIMED, payload);
    }

    @Override
    public void recordClaimFailed(long packageId, UUID actorUuid, String actorName, UUID claimToken, String reason) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("claimToken", claimToken == null ? "" : claimToken.toString());
        payload.put("reason", reason == null ? "" : reason);
        MailSenderType actorType = actorUuid == null ? MailSenderType.SYSTEM : MailSenderType.PLAYER;
        append(packageId, actorUuid, actorName == null ? "system" : actorName, actorType, MailEventType.CLAIM_FAILED, payload);
    }

    @Override
    public void recordExpired(long packageId, String reason) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("reason", reason == null ? "" : reason);
        append(packageId, null, "system", MailSenderType.SYSTEM, MailEventType.EXPIRED, payload);
    }

    @Override
    public void recordDeleted(long packageId, UUID actorUuid, String actorName, MailSenderType actorType, String reason) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("reason", reason == null ? "" : reason);
        append(packageId, actorUuid, actorName, actorType, MailEventType.DELETED, payload);
    }

    private void append(long packageId, UUID actorUuid, String actorName, MailSenderType actorType, MailEventType eventType, Map<String, String> payload) {
        MailEventRecord record = new MailEventRecord();
        record.setPackageId(packageId);
        record.setActorUuid(actorUuid);
        record.setActorNameSnapshot(actorName == null ? "" : actorName);
        record.setActorType(actorType);
        record.setEventType(eventType);
        record.setPayloadJson(toJson(payload));
        record.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        mailEventRepository.append(record);
    }

    private String toJson(Map<String, String> payload) {
        if (payload.isEmpty()) {
            return "{}";
        }

        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"').append(escape(entry.getKey())).append('"')
                    .append(':')
                    .append('"').append(escape(entry.getValue())).append('"');
        }
        builder.append('}');
        return builder.toString();
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
