package com.cuzz.rookiepostbox.bootstrap;

import com.cuzz.bukkitspring.api.annotation.Component;

import java.util.function.Function;

@Component
public final class MybatisExecutor {

    private static final String MYBATIS_SERVICE_CLASS = "com.cuzz.starter.bukkitspring.mybatis.core.MybatisService";

    public boolean isAvailable() {
        return StarterBeanBridge.isEnabled(MYBATIS_SERVICE_CLASS);
    }

    public void withSession(SessionConsumer consumer) {
        Object mybatisService = requireService();
        StarterBeanBridge.invoke(
                mybatisService,
                "withSession",
                new Class<?>[]{java.util.function.Consumer.class},
                (java.util.function.Consumer<Object>) rawSession -> consumer.accept(new Session(rawSession))
        );
    }

    @SuppressWarnings("unchecked")
    public <T> T withSessionResult(SessionFunction<T> function) {
        Object mybatisService = requireService();
        return (T) StarterBeanBridge.invoke(
                mybatisService,
                "withSession",
                new Class<?>[]{Function.class},
                (Function<Object, T>) rawSession -> function.apply(new Session(rawSession))
        );
    }

    private Object requireService() {
        Object mybatisService = StarterBeanBridge.getGlobalBean(MYBATIS_SERVICE_CLASS);
        if (mybatisService == null || !StarterBeanBridge.invokeBoolean(mybatisService, "isEnabled")) {
            throw new IllegalStateException("MyBatis starter is unavailable.");
        }
        return mybatisService;
    }

    @FunctionalInterface
    public interface SessionConsumer {
        void accept(Session session);
    }

    @FunctionalInterface
    public interface SessionFunction<T> {
        T apply(Session session);
    }

    public static final class Session {

        private final Object delegate;

        private Session(Object delegate) {
            this.delegate = delegate;
        }

        @SuppressWarnings("unchecked")
        public <T> T getMapper(Class<T> mapperType) {
            return (T) StarterBeanBridge.invoke(
                    delegate,
                    "getMapper",
                    new Class<?>[]{Class.class},
                    mapperType
            );
        }

        public void commit() {
            StarterBeanBridge.invoke(delegate, "commit", new Class<?>[0]);
        }
    }
}
