package com.cuzz.rookiepostbox.service;



import com.cuzz.rookiepostbox.service.spi.ItemSerializationService;
import com.cuzz.rookiepostbox.service.spi.MailAuditService;
import com.cuzz.rookiepostbox.config.RookiePostBoxProperties;
import com.cuzz.rookiepostbox.domain.entity.MailItemRecord;
import com.cuzz.rookiepostbox.domain.entity.MailPackageRecord;
import com.cuzz.rookiepostbox.domain.enumtype.MailLifecycleState;
import com.cuzz.rookiepostbox.repository.spi.MailPackageRepository;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailClaimServiceTest {

    @Mock
    private MailPackageRepository mailPackageRepository;
    @Mock
    private Player player;
    @Mock
    private PlayerInventory inventory;
    @Mock
    private FileConfiguration fileConfiguration;
    @Mock
    private MailStateService mailStateService;
    @Mock
    private MailAuditService mailAuditService;
    @Mock
    private ItemSerializationService itemSerializationService;

    private RookiePostBoxProperties properties;
    private UUID ownerUuid;

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

        ownerUuid = UUID.randomUUID();
        lenient().when(player.getUniqueId()).thenReturn(ownerUuid);
        lenient().when(player.getName()).thenReturn("owner");
        lenient().when(player.getInventory()).thenReturn(inventory);
    }

    @Test
    void claimPackageShouldSucceedWhenInventoryHasEnoughSpace() {
        MailClaimService service = new MailClaimService(
                mailPackageRepository,
                new MailboxClaimLockManager(),
                properties,
                mailStateService,
                mailAuditService,
                itemSerializationService
        );
        MailPackageRecord mail = availableMail(ownerUuid);
        MailItemRecord item = mailItem("serialized-1", "Diamond", 3);

        when(mailPackageRepository.findById(42L)).thenReturn(Optional.of(mail));
        when(mailPackageRepository.findItems(42L)).thenReturn(List.of(item));
        when(mailPackageRepository.updateLifecycleState(eq(42L), eq(MailLifecycleState.AVAILABLE), eq(MailLifecycleState.CLAIMING), any(), any()))
                .thenReturn(true);
        when(mailPackageRepository.updateLifecycleState(eq(42L), eq(MailLifecycleState.CLAIMING), eq(MailLifecycleState.CLAIMED), any(), any()))
                .thenReturn(true);
        when(itemSerializationService.deserialize("serialized-1")).thenReturn(new ItemStack(Material.DIAMOND, 3));
        when(inventory.getStorageContents()).thenReturn(storage(2));

        ClaimPackageResult result = service.claimPackage(player, "42");

        assertTrue(result.isSuccess());
        verify(mailStateService).runMaintenanceCycle();
        verify(mailAuditService).recordClaimStarted(eq(42L), eq(ownerUuid), eq("owner"), any());
        verify(mailAuditService).recordClaimed(eq(42L), eq(ownerUuid), eq("owner"), any());
    }

    @Test
    void claimPackageShouldFailWithoutGrantingAnyItemWhenFreeSlotsAreLessThanAttachments() {
        MailClaimService service = new MailClaimService(
                mailPackageRepository,
                new MailboxClaimLockManager(),
                properties,
                mailStateService,
                mailAuditService,
                itemSerializationService
        );
        MailPackageRecord mail = availableMail(ownerUuid);
        MailItemRecord first = mailItem("serialized-1", "Stone", 1);
        MailItemRecord second = mailItem("serialized-2", "Gold", 1);

        when(mailPackageRepository.findById(42L)).thenReturn(Optional.of(mail));
        when(mailPackageRepository.findItems(42L)).thenReturn(List.of(first, second));
        when(inventory.getStorageContents()).thenReturn(storage(1));

        ClaimPackageResult result = service.claimPackage(player, "42");

        assertFalse(result.isSuccess());
        assertTrue(result.isInventoryFull());
        verify(inventory, never()).addItem(any(ItemStack.class));
        verify(mailPackageRepository, never()).updateLifecycleState(eq(42L), eq(MailLifecycleState.AVAILABLE), eq(MailLifecycleState.CLAIMING), any(), any());
    }

    @Test
    void claimPackageShouldMarkExpiredMailAndRejectClaim() {
        MailClaimService service = new MailClaimService(
                mailPackageRepository,
                new MailboxClaimLockManager(),
                properties,
                mailStateService,
                mailAuditService,
                itemSerializationService
        );
        MailPackageRecord mail = availableMail(ownerUuid);
        mail.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

        when(mailPackageRepository.findById(42L)).thenReturn(Optional.of(mail));
        when(mailPackageRepository.updateLifecycleState(eq(42L), eq(MailLifecycleState.AVAILABLE), eq(MailLifecycleState.EXPIRED), eq(null), any()))
                .thenReturn(true);

        ClaimPackageResult result = service.claimPackage(player, "42");

        assertFalse(result.isSuccess());
        verify(mailPackageRepository).updateLifecycleState(eq(42L), eq(MailLifecycleState.AVAILABLE), eq(MailLifecycleState.EXPIRED), eq(null), any());
        verify(mailAuditService).recordExpired(42L, "expired-before-claim");
        verify(mailPackageRepository, never()).findItems(42L);
    }

    private MailPackageRecord availableMail(UUID mailboxOwnerUuid) {
        MailPackageRecord record = new MailPackageRecord();
        record.setId(42L);
        record.setMailboxOwnerUuid(mailboxOwnerUuid);
        record.setLifecycleState(MailLifecycleState.AVAILABLE);
        record.setCreatedAt(OffsetDateTime.now());
        return record;
    }

    private MailItemRecord mailItem(String base64, String name, int amount) {
        MailItemRecord record = new MailItemRecord();
        record.setBase64Item(base64);
        record.setDisplayName(name);
        record.setAmount(amount);
        return record;
    }

    private ItemStack[] storage(int freeSlots) {
        ItemStack[] contents = new ItemStack[9];
        for (int i = 0; i < contents.length - freeSlots; i++) {
            contents[i] = new ItemStack(Material.STONE, 64);
        }
        return contents;
    }
}
