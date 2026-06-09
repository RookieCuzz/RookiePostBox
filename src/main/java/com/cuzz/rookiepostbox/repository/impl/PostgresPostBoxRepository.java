package com.cuzz.rookiepostbox.repository.impl;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Repository;
import com.cuzz.rookiepostbox.bootstrap.MybatisExecutor;
import com.cuzz.rookiepostbox.domain.entity.PostBoxRecord;
import com.cuzz.rookiepostbox.repository.spi.PostBoxRepository;
import com.cuzz.rookiepostbox.repository.mybatis.spi.PostBoxMapper;
import com.cuzz.rookiepostbox.repository.mybatis.row.PostBoxRow;

import java.util.Optional;
import java.util.UUID;

@Repository
public final class PostgresPostBoxRepository implements PostBoxRepository {

    private final MybatisExecutor mybatisExecutor;

    @Autowired
    public PostgresPostBoxRepository(MybatisExecutor mybatisExecutor) {
        this.mybatisExecutor = mybatisExecutor;
    }

    @Override
    public void createIfAbsent(UUID ownerUuid, String ownerName) {
        mybatisExecutor.withSession(session -> {
            PostBoxMapper mapper = session.getMapper(PostBoxMapper.class);
            mapper.insertIfAbsent(ownerUuid.toString(), ownerName);
            session.commit();
        });
    }

    @Override
    public Optional<PostBoxRecord> findByOwnerUuid(UUID ownerUuid) {
        return mybatisExecutor.withSessionResult(session -> Optional.ofNullable(
                session.getMapper(PostBoxMapper.class).selectByOwnerUuid(ownerUuid.toString())
        ).map(this::toRecord));
    }

    private PostBoxRecord toRecord(PostBoxRow row) {
        PostBoxRecord record = new PostBoxRecord();
        record.setOwnerUuid(parseUuid(row.getOwnerUuid()));
        record.setOwnerNameCache(row.getOwnerNameCache());
        record.setMailboxEnabled(row.isMailboxEnabled());
        record.setUnreadCount(row.getUnreadCount());
        record.setLastOpenedAt(row.getLastOpenedAt());
        record.setCreatedAt(row.getCreatedAt());
        record.setUpdatedAt(row.getUpdatedAt());
        return record;
    }

    private UUID parseUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}
