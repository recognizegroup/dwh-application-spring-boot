package nl.recognize.dwh.application.loader;

import java.lang.reflect.Field;

public class ClassPropertyAccessor {

    public static boolean isReadable(Object entity, String fieldName) {
        Class<?> classForEntity = entity.getClass();
        try {
            classForEntity.getDeclaredField(fieldName);

            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    public static Object getValue(Object entity, String fieldName) {
        Class<?> classForEntity = entity.getClass();
        try {
            Field privateField = classForEntity.getDeclaredField(fieldName);
            privateField.setAccessible(true);

            return privateField.get(entity);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

}
