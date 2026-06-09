package com.cuzz.rookiepostbox.repository.spi;

import com.cuzz.rookiepostbox.domain.entity.MailEventRecord;

import java.util.List;

public interface MailEventRepository {

    void append(MailEventRecord record);

    List<MailEventRecord> findByPackageId(long packageId, int limit);
}
