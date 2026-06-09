package com.cuzz.rookiepostbox.service;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class AdminDeleteMailResult {
    private final boolean success;
    private final long packageId;
    private final String message;
}
