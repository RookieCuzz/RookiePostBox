package com.cuzz.rookiepostbox.service;


import com.cuzz.rookiepostbox.service.spi.MailAuditService;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.domain.entity.MailPackageRecord;
import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.repository.spi.MailPackageRepository;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailStateServiceTest {

    @Mock
    private MailPackageRepository mailPackageRepository;
    @Mock
    private MailAuditService mailAuditService;
    @Mock
    private FileConfiguration fileConfiguration;

    private RookiePostBoxProperties properties;

    @BeforeEach
    void setUp() {
        when(fileConfiguration.getBoolean("rookiepostbox.debug", false)).thenReturn(false);
        when(fileConfiguration.getInt("rookiepostbox.mail.page-size", 21)).thenReturn(21);
        when(fileConfiguration.getInt("rookiepostbox.mail.max-inbox-mails", 200)).thenReturn(200);
        when(fileConfiguration.getInt("rookiepostbox.mail.default-expire-days", 30)).thenReturn(30);
        when(fileConfiguration.getBoolean("rookiepostbox.mail.allow-player-send", false)).thenReturn(false);
        when(fileConfiguration.getBoolean("rookiepostbox.mail.allow-offline-send", true)).thenReturn(true);
        when(fileConfiguration.getInt("rookiepostbox.claim.recover-claiming-after-seconds", 120)).thenReturn(120);
        when(fileConfiguration.getBoolean("rookiepostbox.claim.enable-claim-failed-retry", true)).thenReturn(true);
        when(fileConfiguration.getInt("rookiepostbox.maintenance.interval-seconds", 60)).thenReturn(60);

        properties = new RookiePostBoxProperties(fileConfiguration);
        properties.reload();
    }

    @Test
    void expireDueMailsShouldMarkPackagesExpiredAndAuditThem() {
        MailStateService service = new MailStateService(mailPackageRepository, properties, mailAuditService);
        MailPackageRecord mail = new MailPackageRecord();
        mail.setId(10L);
        mail.setLifecycleState(MailLifecycleState.AVAILABLE);

        when(mailPackageRepository.findExpirablePackages(any(), eq(200))).thenReturn(List.of(mail), List.of());
        when(mailPackageRepository.updateLifecycleState(eq(10L), eq(MailLifecycleState.AVAILABLE), eq(MailLifecycleState.EXPIRED), eq(null), any()))
                .thenReturn(true);

        int expired = service.expireDueMails();

        assertEquals(1, expired);
        verify(mailAuditService).recordExpired(10L, "expired-by-maintenance");
    }

    @Test
    void recoverStaleClaimingMailsShouldMovePackagesToClaimFailed() {
        MailStateService service = new MailStateService(mailPackageRepository, properties, mailAuditService);
        MailPackageRecord mail = new MailPackageRecord();
        mail.setId(11L);
        mail.setLifecycleState(MailLifecycleState.CLAIMING);
        mail.setClaimToken(UUID.randomUUID());
        mail.setClaimStartedAt(OffsetDateTime.now().minusMinutes(10));

        when(mailPackageRepository.findStaleClaimingPackages(any(), eq(200))).thenReturn(List.of(mail), List.of());
        when(mailPackageRepository.updateLifecycleState(eq(11L), eq(MailLifecycleState.CLAIMING), eq(MailLifecycleState.CLAIM_FAILED), eq(mail.getClaimToken()), any()))
                .thenReturn(true);

        int recovered = service.recoverStaleClaimingMails();

        assertEquals(1, recovered);
        verify(mailAuditService).recordClaimFailed(11L, null, "system", mail.getClaimToken(), "stale-claim-recovered");
    }
}
