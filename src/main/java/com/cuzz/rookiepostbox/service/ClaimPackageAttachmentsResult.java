package com.cuzz.rookiepostbox.service;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public final class ClaimPackageAttachmentsResult {
    private final boolean success;
    private final String packageId;
    private final String message;
    private final List<ClaimedAttachment> attachments;
    private final String senderName;
    private final String messageText;
    private final OffsetDateTime createdAt;
}
