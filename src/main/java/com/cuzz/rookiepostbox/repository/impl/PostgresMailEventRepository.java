package com.cuzz.rookiepostbox.repository.impl;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Repository;
import com.cuzz.rookiepostbox.bootstrap.MybatisExecutor;
import com.cuzz.rookiepostbox.domain.entity.MailEventRecord;
import com.cuzz.rookiepostbox.repository.spi.MailEventRepository;
import com.cuzz.rookiepostbox.repository.mybatis.spi.MailEventMapper;
import com.cuzz.rookiepostbox.repository.mybatis.row.MailEventRow;

import java.util.List;
import java.util.UUID;

@Repository
public final class PostgresMailEventRepository implements MailEventRepository {

    private final MybatisExecutor mybatisExecutor;

    @Autowired
    public PostgresMailEventRepository(MybatisExecutor mybatisExecutor) {
        this.mybatisExecutor = mybatisExecutor;
    }

    @Override
    public void append(MailEventRecord record) {
        mybatisExecutor.withSession(session -> {
            session.getMapper(MailEventMapper.class).insert(toRow(record));
            session.commit();
        });
    }

    @Override
    public List<MailEventRecord> findByPackageId(long packageId, int limit) {
        return mybatisExecutor.withSessionResult(session -> session.getMapper(MailEventMapper.class)
                .selectByPackageId(packageId, limit)
                .stream()
                .map(this::toRecord)
                .toList());
    }

    private MailEventRow toRow(MailEventRecord record) {
        MailEventRow row = new MailEventRow();
        row.setId(record.getId());
        row.setPackageId(record.getPackageId());
        row.setActorUuid(stringifyUuid(record.getActorUuid()));
        row.setActorNameSnapshot(record.getActorNameSnapshot());
        row.setActorType(record.getActorType());
        row.setEventType(record.getEventType());
        row.setPayloadJson(record.getPayloadJson());
        row.setCreatedAt(record.getCreatedAt());
        return row;
    }

    private MailEventRecord toRecord(MailEventRow row) {
        MailEventRecord record = new MailEventRecord();
        record.setId(row.getId());
        record.setPackageId(row.getPackageId());
        record.setActorUuid(parseUuid(row.getActorUuid()));
        record.setActorNameSnapshot(row.getActorNameSnapshot());
        record.setActorType(row.getActorType());
        record.setEventType(row.getEventType());
        record.setPayloadJson(row.getPayloadJson());
        record.setCreatedAt(row.getCreatedAt());
        return record;
    }

    private String stringifyUuid(UUID value) {
        return value == null ? null : value.toString();
    }

    private UUID parseUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}
