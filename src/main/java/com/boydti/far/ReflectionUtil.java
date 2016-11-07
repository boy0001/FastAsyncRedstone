package com.boydti.far;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectionUtil {
    public static <T> T getOfT(final Object obj, final Class<T> type) {
        for (final Field field : obj.getClass().getDeclaredFields()) {
            if (type.equals(field.getType())) {
                return ReflectionUtil.<T> get(obj, field, type);
            }
        }
        return null;
    }
    
    public static <T> T get(final Object obj, final String name, final Class<T> type) {
        return ReflectionUtil.<T> get(obj, obj.getClass(), name, type);
    }
    
    public static <T> T get(final Object obj, final Class<?> clazz, final String name, final Class<T> type) {
        for (final Field field : clazz.getDeclaredFields()) {
            if (name.equals(field.getName())) {
                return ReflectionUtil.<T> get(obj, field, type);
            }
        }
        throw new IllegalArgumentException("No field: " + name);
    }
    
    public static void setStatic(final String name, final Class<?> clazz, final Object val) {
        try {
            final Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            if (Modifier.isFinal(field.getModifiers())) {
                final Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & 0xFFFFFFEF);
            }
            field.set(null, val);
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
        }
    }
    
    public static <T> T get(final Object obj, final Field field, final Class<T> type) {
        try {
            field.setAccessible(true);
            return type.cast(field.get(obj));
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
