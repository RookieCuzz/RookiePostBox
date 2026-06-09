package com.cuzz.rookiepostbox.repository.spi;

import com.cuzz.rookiepostbox.domain.entity.AdminMailRecord;
import com.cuzz.rookiepostbox.domain.entity.InboxMailRecord;
import com.cuzz.rookiepostbox.domain.entity.MailItemRecord;
import com.cuzz.rookiepostbox.domain.entity.MailPackageRecord;
import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MailPackageRepository {

    long createPackage(MailPackageRecord mailPackageRecord, List<MailItemRecord> items);

    List<InboxMailRecord> findInbox(UUID ownerUuid, int offset, int limit);

    int countInbox(UUID ownerUuid);

    int countUnreadInbox(UUID ownerUuid);

    List<AdminMailRecord> findAdminInbox(UUID ownerUuid, int offset, int limit);

    Optional<MailPackageRecord> findById(long packageId);

    Optional<MailPackageRecord> findByRequestId(UUID requestId);

    List<MailPackageRecord> findExpirablePackages(OffsetDateTime now, int limit);

    List<MailPackageRecord> findStaleClaimingPackages(OffsetDateTime cutoff, int limit);

    int markInboxRead(UUID ownerUuid, List<Long> packageIds, OffsetDateTime readAt);

    boolean markDeleted(long packageId, OffsetDateTime deletedAt, String adminNote);

    List<MailItemRecord> findItems(long packageId);

    List<MailItemRecord> findItemsByPackageIds(List<Long> packageIds);

    boolean updateLifecycleState(long packageId, MailLifecycleState expectedState, MailLifecycleState targetState, UUID claimToken, OffsetDateTime transitionAt);
}
