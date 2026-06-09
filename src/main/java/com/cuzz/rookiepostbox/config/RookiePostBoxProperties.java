package com.cuzz.rookiepostbox.config;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

@Component
public final class RookiePostBoxProperties {

    private final FileConfiguration configuration;
    private boolean debug;
    private int pageSize;
    private int maxInboxMails;
    private int defaultExpireDays;
    private boolean allowPlayerSend;
    private boolean allowOfflineSend;
    private int recoverClaimingAfterSeconds;
    private boolean enableClaimFailedRetry;
    private int maintenanceIntervalSeconds;
    private JoinNotificationProperties joinNotification;
    private MenuProperties menu;

    @Autowired
    public RookiePostBoxProperties(FileConfiguration configuration) {
        this.configuration = configuration;
    }

    @PostConstruct
    public void reload() {
        debug = configuration.getBoolean("rookiepostbox.debug", false);
        pageSize = configuration.getInt("rookiepostbox.mail.page-size", 21);
        maxInboxMails = configuration.getInt("rookiepostbox.mail.max-inbox-mails", 200);
        defaultExpireDays = configuration.getInt("rookiepostbox.mail.default-expire-days", 30);
        allowPlayerSend = configuration.getBoolean("rookiepostbox.mail.allow-player-send", false);
        allowOfflineSend = configuration.getBoolean("rookiepostbox.mail.allow-offline-send", true);
        recoverClaimingAfterSeconds = configuration.getInt("rookiepostbox.claim.recover-claiming-after-seconds", 120);
        enableClaimFailedRetry = configuration.getBoolean("rookiepostbox.claim.enable-claim-failed-retry", true);
        maintenanceIntervalSeconds = configuration.getInt("rookiepostbox.maintenance.interval-seconds", 60);
        joinNotification = JoinNotificationProperties.load(configuration);
        menu = MenuProperties.load(configuration);
    }

    public boolean isDebug() {
        return debug;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getMaxInboxMails() {
        return maxInboxMails;
    }

    public int getDefaultExpireDays() {
        return defaultExpireDays;
    }

    public boolean isAllowPlayerSend() {
        return allowPlayerSend;
    }

    public boolean isAllowOfflineSend() {
        return allowOfflineSend;
    }

    public int getRecoverClaimingAfterSeconds() {
        return recoverClaimingAfterSeconds;
    }

    public boolean isEnableClaimFailedRetry() {
        return enableClaimFailedRetry;
    }

    public int getMaintenanceIntervalSeconds() {
        return maintenanceIntervalSeconds;
    }

    public MenuProperties getMenu() {
        return menu;
    }

    public JoinNotificationProperties getJoinNotification() {
        return joinNotification;
    }

    public static final class JoinNotificationProperties {

        private final boolean enabled;
        private final int delayTicks;
        private final String message;

        private JoinNotificationProperties(boolean enabled, int delayTicks, String message) {
            this.enabled = enabled;
            this.delayTicks = delayTicks;
            this.message = message;
        }

        private static JoinNotificationProperties load(FileConfiguration configuration) {
            return new JoinNotificationProperties(
                    configuration.getBoolean("rookiepostbox.notification.join.enabled", true),
                    configuration.getInt("rookiepostbox.notification.join.delay-ticks", 40),
                    configuration.getString(
                            "rookiepostbox.notification.join.message",
                            "&eMailbox &7>> &fYou have &a%unread% &funread mail. Use &b/rookiepostbox menu &fto open it."
                    )
            );
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getDelayTicks() {
            return delayTicks;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class MenuProperties {

        private final String titleTemplate;
        private final String detailTitleTemplate;
        private final ButtonProperties previousButton;
        private final ButtonProperties nextButton;
        private final MailItemProperties mailItem;

        private MenuProperties(String titleTemplate, String detailTitleTemplate, ButtonProperties previousButton, ButtonProperties nextButton, MailItemProperties mailItem) {
            this.titleTemplate = titleTemplate;
            this.detailTitleTemplate = detailTitleTemplate;
            this.previousButton = previousButton;
            this.nextButton = nextButton;
            this.mailItem = mailItem;
        }

        private static MenuProperties load(FileConfiguration configuration) {
            return new MenuProperties(
                    configuration.getString("rookiepostbox.menu.title-template", "letter"),
                    configuration.getString("rookiepostbox.menu.detail-title-template", "letter_detail"),
                    ButtonProperties.load(
                            configuration,
                            "rookiepostbox.menu.buttons.previous",
                            "BRICK",
                            "&ePrevious Page",
                            List.of(
                                    "&7Go to the previous page.",
                                    "",
                                    "&fPage: &e%currentPage% / %pageAmount%",
                                    "&aClick to switch"
                            )
                    ),
                    ButtonProperties.load(
                            configuration,
                            "rookiepostbox.menu.buttons.next",
                            "BRICK",
                            "&eNext Page",
                            List.of(
                                    "&7Go to the next page.",
                                    "",
                                    "&fPage: &e%currentPage% / %pageAmount%",
                                    "&aClick to switch"
                            )
                    ),
                    MailItemProperties.load(configuration)
            );
        }

        public String getTitleTemplate() {
            return titleTemplate;
        }

        public String getDetailTitleTemplate() {
            return detailTitleTemplate;
        }

        public ButtonProperties getPreviousButton() {
            return previousButton;
        }

        public ButtonProperties getNextButton() {
            return nextButton;
        }

        public MailItemProperties getMailItem() {
            return mailItem;
        }
    }

    public static final class ButtonProperties {

        private final String material;
        private final String displayName;
        private final Integer customModelData;
        private final List<String> lore;

        private ButtonProperties(String material, String displayName, Integer customModelData, List<String> lore) {
            this.material = material;
            this.displayName = displayName;
            this.customModelData = customModelData;
            this.lore = lore;
        }

        private static ButtonProperties load(
                FileConfiguration configuration,
                String path,
                String defaultMaterial,
                String defaultDisplayName,
                List<String> defaultLore
        ) {
            return new ButtonProperties(
                    configuration.getString(path + ".material", defaultMaterial),
                    configuration.getString(path + ".display-name", defaultDisplayName),
                    readInteger(configuration, path + ".custom-model-data"),
                    readStringList(configuration, path + ".lore", defaultLore)
            );
        }

        public String getMaterial() {
            return material;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Integer getCustomModelData() {
            return customModelData;
        }

        public List<String> getLore() {
            return lore;
        }
    }

    public static final class MailItemProperties {

        private final String material;
        private final String unreadDisplayName;
        private final String readDisplayName;
        private final Integer customModelData;
        private final List<String> lore;

        private MailItemProperties(String material, String unreadDisplayName, String readDisplayName, Integer customModelData, List<String> lore) {
            this.material = material;
            this.unreadDisplayName = unreadDisplayName;
            this.readDisplayName = readDisplayName;
            this.customModelData = customModelData;
            this.lore = lore;
        }

        private static MailItemProperties load(FileConfiguration configuration) {
            return new MailItemProperties(
                    configuration.getString("rookiepostbox.menu.mail-item.material", "BUNDLE"),
                    configuration.getString("rookiepostbox.menu.mail-item.unread-display-name", "&6&lNew Mail &ffrom &e%sender%"),
                    configuration.getString("rookiepostbox.menu.mail-item.read-display-name", "&fMail &7from &f%sender%"),
                    readInteger(configuration, "rookiepostbox.menu.mail-item.custom-model-data"),
                    readStringList(
                            configuration,
                            "rookiepostbox.menu.mail-item.lore",
                            List.of(
                                    "&8Mail #%mailId%",
                                    "",
                                    "&fSender: &e%sender%",
                                    "&fStatus: %status%",
                                    "&fCreated: &b%createdAt%",
                                    "&fExpires: &b%expiresAt%",
                                    "",
                                    "&fMessage:",
                                    "&7%message%",
                                    "",
                                    "&aLeft click to claim attachments"
                            )
                    )
            );
        }

        public String getMaterial() {
            return material;
        }

        public String getUnreadDisplayName() {
            return unreadDisplayName;
        }

        public String getReadDisplayName() {
            return readDisplayName;
        }

        public Integer getCustomModelData() {
            return customModelData;
        }

        public List<String> getLore() {
            return lore;
        }
    }

    private static Integer readInteger(FileConfiguration configuration, String path) {
        if (!configuration.contains(path)) {
            return null;
        }
        return configuration.getInt(path);
    }

    private static List<String> readStringList(FileConfiguration configuration, String path, List<String> defaultValue) {
        List<String> configured = configuration.getStringList(path);
        if (configured == null || configured.isEmpty()) {
            return defaultValue;
        }
        return List.copyOf(configured);
    }
}
