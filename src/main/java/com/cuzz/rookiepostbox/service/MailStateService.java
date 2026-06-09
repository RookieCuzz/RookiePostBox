package com.cuzz.rookiepostbox.service;


import com.cuzz.rookiepostbox.service.spi.MailAuditService;
import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Service;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.domain.entity.MailPackageRecord;
import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.repository.spi.MailPackageRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public final class MailStateService {

    private static final int BATCH_LIMIT = 200;

    private final MailPackageRepository mailPackageRepository;
    private final RookiePostBoxProperties properties;
    private final MailAuditService mailAuditService;

    @Autowired
    public MailStateService(
            MailPackageRepository mailPackageRepository,
            RookiePostBoxProperties properties,
            MailAuditService mailAuditService
    ) {
        this.mailPackageRepository = mailPackageRepository;
        this.properties = properties;
        this.mailAuditService = mailAuditService;
    }

    public int runMaintenanceCycle() {
        return expireDueMails() + recoverStaleClaimingMails();
    }

    public int expireDueMails() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int expiredCount = 0;

        while (true) {
            List<MailPackageRecord> dueMails = mailPackageRepository.findExpirablePackages(now, BATCH_LIMIT);
            if (dueMails.isEmpty()) {
                return expiredCount;
            }

            for (MailPackageRecord mail : dueMails) {
                if (mail.getId() == null || mail.getLifecycleState() == null) {
                    continue;
                }

                boolean expired = mailPackageRepository.updateLifecycleState(
                        mail.getId(),
                        mail.getLifecycleState(),
                        MailLifecycleState.EXPIRED,
                        null,
                        now
                );
                if (expired) {
                    mailAuditService.recordExpired(mail.getId(), "expired-by-maintenance");
                    expiredCount++;
                }
            }

            if (dueMails.size() < BATCH_LIMIT) {
                return expiredCount;
            }
        }
    }

    public int recoverStaleClaimingMails() {
        if (properties.getRecoverClaimingAfterSeconds() <= 0) {
            return 0;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime cutoff = now.minusSeconds(properties.getRecoverClaimingAfterSeconds());
        int recoveredCount = 0;

        while (true) {
            List<MailPackageRecord> staleMails = mailPackageRepository.findStaleClaimingPackages(cutoff, BATCH_LIMIT);
            if (staleMails.isEmpty()) {
                return recoveredCount;
            }

            for (MailPackageRecord mail : staleMails) {
                if (mail.getId() == null) {
                    continue;
                }

                boolean recovered = mailPackageRepository.updateLifecycleState(
                        mail.getId(),
                        MailLifecycleState.CLAIMING,
                        MailLifecycleState.CLAIM_FAILED,
                        mail.getClaimToken(),
                        now
                );
                if (recovered) {
                    mailAuditService.recordClaimFailed(
                            mail.getId(),
                            null,
                            "system",
                            mail.getClaimToken(),
                            "stale-claim-recovered"
                    );
                    recoveredCount++;
                }
            }

            if (staleMails.size() < BATCH_LIMIT) {
                return recoveredCount;
            }
        }
    }
}
