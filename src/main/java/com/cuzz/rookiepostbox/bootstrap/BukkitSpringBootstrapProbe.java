package com.cuzz.rookiepostbox.bootstrap;

import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.rookiepostbox.RookiePostBox;

@Component
public final class BukkitSpringBootstrapProbe {

    private static final String CAFFEINE_SERVICE_CLASS = "com.cuzz.starter.bukkitspring.caffeine.api.CaffeineService";
    private static final String CONFIG_SERVICE_CLASS = "com.cuzz.starter.bukkitspring.config.api.ConfigService";
    private static final String MYBATIS_SERVICE_CLASS = "com.cuzz.starter.bukkitspring.mybatis.core.MybatisService";

    @PostConstruct
    public void logBootstrapStatus() {
        boolean caffeineStarterAvailable = StarterBeanBridge.isEnabled(CAFFEINE_SERVICE_CLASS);
        boolean configStarterAvailable = StarterBeanBridge.isEnabled(CONFIG_SERVICE_CLASS);
        boolean mybatisStarterAvailable = StarterBeanBridge.isEnabled(MYBATIS_SERVICE_CLASS);

        RookiePostBox.getInstance().getLogger().info(String.format(
                "BukkitSpring context ready. caffeineStarter=%s, configStarter=%s, mybatisStarter=%s",
                caffeineStarterAvailable,
                configStarterAvailable,
                mybatisStarterAvailable
        ));
    }
}
