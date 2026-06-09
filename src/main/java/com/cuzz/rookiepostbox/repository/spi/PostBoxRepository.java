package com.cuzz.rookiepostbox.repository.spi;

import com.cuzz.rookiepostbox.domain.entity.PostBoxRecord;

import java.util.Optional;
import java.util.UUID;

public interface PostBoxRepository {

    void createIfAbsent(UUID ownerUuid, String ownerName);

    Optional<PostBoxRecord> findByOwnerUuid(UUID ownerUuid);
}
