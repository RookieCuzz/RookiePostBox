package com.cuzz.rookiepostbox.service.spi;

import com.cuzz.rookiepostbox.domain.enumtype.MailSenderType;

import java.util.UUID;

public interface MailAuditService {

    void recordCreated(long packageId, UUID actorUuid, String actorName, MailSenderType actorType, UUID requestId, String sourcePlugin);

    void recordRead(long packageId, UUID actorUuid, String actorName);

    void recordClaimStarted(long packageId, UUID actorUuid, String actorName, UUID claimToken);

    void recordClaimed(long packageId, UUID actorUuid, String actorName, UUID claimToken);

    void recordClaimFailed(long packageId, UUID actorUuid, String actorName, UUID claimToken, String reason);

    void recordExpired(long packageId, String reason);

    void recordDeleted(long packageId, UUID actorUuid, String actorName, MailSenderType actorType, String reason);
}
