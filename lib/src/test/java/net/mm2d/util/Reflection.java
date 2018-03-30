/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 */

package net.mm2d.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Reflection {
    @Nonnull
    public static <T> Constructor<T> getConstructor(
            @Nonnull final Class<T> cls,
            final Class<?>... parameterTypes) throws NoSuchMethodException {
        final Constructor<T> constructor = cls.getDeclaredConstructor(parameterTypes);
        constructor.setAccessible(true);
        return constructor;
    }

    @Nonnull
    public static Field getField(
            @Nonnull final Object target,
            @Nonnull final String fieldName) throws NoSuchFieldException {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    @Nullable
    public static <T> T getFieldValue(
            @Nonnull final Object target,
            @Nonnull final String fieldName) throws NoSuchFieldException, IllegalAccessException {
        return (T) getField(target, fieldName).get(target);
    }

    public static void setFieldValue(
            @Nonnull final Object target,
            @Nonnull final String fieldName,
            @Nullable final Object data) throws NoSuchFieldException, IllegalAccessException {
        getField(target, fieldName).set(target, data);
    }

    @Nonnull
    public static Method getMethod(
            @Nonnull final Object target,
            @Nonnull final String methodName,
            final Class<?>... parameterTypes) throws NoSuchMethodException {
        final Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    @Nonnull
    public static MethodInvoker getMethodInvoker(
            @Nonnull final Object target,
            @Nonnull final String methodName,
            final Class<?>... parameterTypes) throws NoSuchMethodException {
        return new MethodInvoker(target, methodName, parameterTypes);
    }

    public static class MethodInvoker {
        @Nonnull
        private final Object mTarget;
        @Nonnull
        private final Method mMethod;

        private MethodInvoker(
                @Nonnull final Object target,
                @Nonnull final String methodName,
                final Class<?>... parameterTypes) throws NoSuchMethodException {
            mTarget = target;
            mMethod = getMethod(target, methodName, parameterTypes);
        }

        public <T> T invoke(Object... args) throws InvocationTargetException, IllegalAccessException {
            return (T) mMethod.invoke(mTarget, args);
        }
    }
}
