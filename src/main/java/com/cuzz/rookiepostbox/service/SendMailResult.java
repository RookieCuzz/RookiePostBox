package com.cuzz.rookiepostbox.service;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public final class SendMailResult {
    private final boolean success;
    private final long packageId;
    private final boolean deduplicated;
    private final UUID requestId;
    private final String message;
}
