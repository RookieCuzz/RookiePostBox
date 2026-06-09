package com.cuzz.rookiepostbox.service;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class ClaimPackageResult {
    private final boolean success;
    private final boolean inventoryFull;
    private final String packageId;
    private final String message;
}
