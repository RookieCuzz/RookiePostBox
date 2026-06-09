package com.cuzz.rookiepostbox.repository.impl;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Repository;
import com.cuzz.rookiepostbox.bootstrap.MybatisExecutor;
import com.cuzz.rookiepostbox.domain.entity.AdminMailRecord;
import com.cuzz.rookiepostbox.domain.entity.InboxMailRecord;
import com.cuzz.rookiepostbox.domain.entity.MailItemRecord;
import com.cuzz.rookiepostbox.domain.entity.MailPackageRecord;
import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.repository.spi.MailPackageRepository;
import com.cuzz.rookiepostbox.repository.mybatis.spi.MailItemMapper;
import com.cuzz.rookiepostbox.repository.mybatis.spi.MailPackageMapper;
import com.cuzz.rookiepostbox.repository.mybatis.row.MailPackageRow;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public final class PostgresMailPackageRepository implements MailPackageRepository {

    private final MybatisExecutor mybatisExecutor;

    @Autowired
    public PostgresMailPackageRepository(MybatisExecutor mybatisExecutor) {
        this.mybatisExecutor = mybatisExecutor;
    }

    @Override
    public long createPackage(MailPackageRecord mailPackageRecord, List<MailItemRecord> items) {
        return mybatisExecutor.withSessionResult(session -> {
            MailPackageMapper packageMapper = session.getMapper(MailPackageMapper.class);
            MailItemMapper itemMapper = session.getMapper(MailItemMapper.class);
            MailPackageRow row = toRow(mailPackageRecord);
            packageMapper.insert(row);
            mailPackageRecord.setId(row.getId());
            if (row.getId() != null) {
                for (MailItemRecord item : items) {
                    item.setPackageId(row.getId());
                    itemMapper.insert(item);
                }
            }
            session.commit();
            return row.getId() == null ? -1L : row.getId();
        });
    }

    @Override
    public List<InboxMailRecord> findInbox(UUID ownerUuid, int offset, int limit) {
        return mybatisExecutor.withSessionResult(session ->
                session.getMapper(MailPackageMapper.class).selectInbox(ownerUuid.toString(), offset, limit)
        );
    }

    @Override
    public int countInbox(UUID ownerUuid) {
        return mybatisExecutor.withSessionResult(session ->
                session.getMapper(MailPackageMapper.class).countInbox(ownerUuid.toString())
        );
    }

    @Override
    public int countUnreadInbox(UUID ownerUuid) {
        return mybatisExecutor.withSessionResult(session ->
                session.getMapper(MailPackageMapper.class).countUnreadInbox(ownerUuid.toString())
        );
    }

    @Override
    public List<AdminMailRecord> findAdminInbox(UUID ownerUuid, int offset, int limit) {
        return mybatisExecutor.withSessionResult(session ->
                session.getMapper(MailPackageMapper.class).selectAdminInbox(ownerUuid.toString(), offset, limit)
        );
    }

    @Override
    public Optional<MailPackageRecord> findById(long packageId) {
        return mybatisExecutor.withSessionResult(session -> Optional.ofNullable(
                session.getMapper(MailPackageMapper.class).selectById(packageId)
        ).map(this::toRecord));
    }

    @Override
    public Optional<MailPackageRecord> findByRequestId(UUID requestId) {
        return mybatisExecutor.withSessionResult(session -> Optional.ofNullable(
                session.getMapper(MailPackageMapper.class).selectByRequestId(requestId.toString())
        ).map(this::toRecord));
    }

    @Override
    public List<MailPackageRecord> findExpirablePackages(OffsetDateTime now, int limit) {
        return mybatisExecutor.withSessionResult(session -> session.getMapper(MailPackageMapper.class)
                .selectExpirablePackages(
                        now == null ? null : now.toString(),
                        limit
                )
                .stream()
                .map(this::toRecord)
                .toList());
    }

    @Override
    public List<MailPackageRecord> findStaleClaimingPackages(OffsetDateTime cutoff, int limit) {
        return mybatisExecutor.withSessionResult(session -> session.getMapper(MailPackageMapper.class)
                .selectStaleClaimingPackages(
                        cutoff == null ? null : cutoff.toString(),
                        limit
                )
                .stream()
                .map(this::toRecord)
                .toList());
    }

    @Override
    public int markInboxRead(UUID ownerUuid, List<Long> packageIds, OffsetDateTime readAt) {
        return mybatisExecutor.withSessionResult(session -> {
            int updated = session.getMapper(MailPackageMapper.class).markInboxRead(
                    ownerUuid.toString(),
                    packageIds,
                    readAt == null ? null : readAt.toString()
            );
            session.commit();
            return updated;
        });
    }

    @Override
    public boolean markDeleted(long packageId, OffsetDateTime deletedAt, String adminNote) {
        return mybatisExecutor.withSessionResult(session -> {
            int updated = session.getMapper(MailPackageMapper.class).markDeleted(
                    packageId,
                    deletedAt == null ? null : deletedAt.toString(),
                    adminNote
            );
            session.commit();
            return updated > 0;
        });
    }

    @Override
    public List<MailItemRecord> findItems(long packageId) {
        return mybatisExecutor.withSessionResult(session ->
                session.getMapper(MailItemMapper.class).selectByPackageId(packageId)
        );
    }

    @Override
    public List<MailItemRecord> findItemsByPackageIds(List<Long> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            return List.of();
        }
        return mybatisExecutor.withSessionResult(session ->
                session.getMapper(MailItemMapper.class).selectByPackageIds(packageIds)
        );
    }

    @Override
    public boolean updateLifecycleState(long packageId, MailLifecycleState expectedState, MailLifecycleState targetState, UUID claimToken, OffsetDateTime transitionAt) {
        return mybatisExecutor.withSessionResult(session -> {
            int updated = session.getMapper(MailPackageMapper.class).updateLifecycleState(
                    packageId,
                    expectedState.name(),
                    targetState.name(),
                    claimToken == null ? null : claimToken.toString(),
                    transitionAt == null ? null : transitionAt.toString()
            );
            session.commit();
            return updated > 0;
        });
    }

    private MailPackageRow toRow(MailPackageRecord record) {
        MailPackageRow row = new MailPackageRow();
        row.setId(record.getId());
        row.setMailboxOwnerUuid(stringifyUuid(record.getMailboxOwnerUuid()));
        row.setSenderUuid(stringifyUuid(record.getSenderUuid()));
        row.setSenderNameSnapshot(record.getSenderNameSnapshot());
        row.setSenderType(record.getSenderType());
        row.setMessageText(record.getMessageText());
        row.setReadState(record.getReadState());
        row.setLifecycleState(record.getLifecycleState());
        row.setRequestId(stringifyUuid(record.getRequestId()));
        row.setClaimToken(stringifyUuid(record.getClaimToken()));
        row.setVersion(record.getVersion());
        row.setSourcePlugin(record.getSourcePlugin());
        row.setAdminNote(record.getAdminNote());
        row.setCreatedAt(record.getCreatedAt());
        row.setReadAt(record.getReadAt());
        row.setClaimStartedAt(record.getClaimStartedAt());
        row.setClaimedAt(record.getClaimedAt());
        row.setExpiresAt(record.getExpiresAt());
        row.setDeletedAt(record.getDeletedAt());
        row.setUpdatedAt(record.getUpdatedAt());
        return row;
    }

    private MailPackageRecord toRecord(MailPackageRow row) {
        MailPackageRecord record = new MailPackageRecord();
        record.setId(row.getId());
        record.setMailboxOwnerUuid(parseUuid(row.getMailboxOwnerUuid()));
        record.setSenderUuid(parseUuid(row.getSenderUuid()));
        record.setSenderNameSnapshot(row.getSenderNameSnapshot());
        record.setSenderType(row.getSenderType());
        record.setMessageText(row.getMessageText());
        record.setReadState(row.getReadState());
        record.setLifecycleState(row.getLifecycleState());
        record.setRequestId(parseUuid(row.getRequestId()));
        record.setClaimToken(parseUuid(row.getClaimToken()));
        record.setVersion(row.getVersion());
        record.setSourcePlugin(row.getSourcePlugin());
        record.setAdminNote(row.getAdminNote());
        record.setCreatedAt(row.getCreatedAt());
        record.setReadAt(row.getReadAt());
        record.setClaimStartedAt(row.getClaimStartedAt());
        record.setClaimedAt(row.getClaimedAt());
        record.setExpiresAt(row.getExpiresAt());
        record.setDeletedAt(row.getDeletedAt());
        record.setUpdatedAt(row.getUpdatedAt());
        return record;
    }

    private String stringifyUuid(UUID value) {
        return value == null ? null : value.toString();
    }

    private UUID parseUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}
