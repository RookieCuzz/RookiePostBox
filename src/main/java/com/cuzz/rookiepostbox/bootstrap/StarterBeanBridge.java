package com.cuzz.rookiepostbox.bootstrap;

import com.cuzz.bukkitspring.BukkitSpring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class StarterBeanBridge {

    private static final ClassLoader STARTER_CLASS_LOADER = BukkitSpring.class.getClassLoader();

    private StarterBeanBridge() {
    }

    public static Object getGlobalBean(String className) {
        Class<?> beanClass = loadStarterClass(className);
        if (beanClass == null) {
            return null;
        }
        return BukkitSpring.getGlobalBean(beanClass);
    }

    public static boolean isEnabled(String className) {
        Object bean = getGlobalBean(className);
        return bean != null && invokeBoolean(bean, "isEnabled");
    }

    public static Class<?> loadStarterClass(String className) {
        try {
            return Class.forName(className, false, STARTER_CLASS_LOADER);
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    public static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, arguments);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Failed to invoke starter method " + methodName, exception);
        }
    }

    public static boolean invokeBoolean(Object target, String methodName) {
        Object result = invoke(target, methodName, new Class<?>[0]);
        return result instanceof Boolean && (Boolean) result;
    }
}
