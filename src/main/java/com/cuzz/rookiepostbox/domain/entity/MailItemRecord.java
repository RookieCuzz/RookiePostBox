package com.cuzz.rookiepostbox.domain.entity;

import com.cuzz.rookiepostbox.domain.enumtype.MailItemKind;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MailItemRecord {
    private Long id;
    private Long packageId;
    private Integer slotIndex;
    private MailItemKind itemKind;
    private String materialKey;
    private String displayName;
    private Integer amount;
    private String storeId;
    private String uniqueKey;
    private String base64Item;
    private OffsetDateTime createdAt;
}
