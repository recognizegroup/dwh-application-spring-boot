package nl.recognize.dwh.application.loader;

import java.lang.reflect.Field;

public class ClassPropertyAccessor {

    public static boolean isReadable(Object entity, String fieldName) {
        return getDeclaredField(entity, fieldName) != null;
    }

    public static Object getValue(Object entity, String fieldName) {
        Field declaredField = getDeclaredField(entity, fieldName);
        if (declaredField != null) {
            declaredField.setAccessible(true);

            try {
                return declaredField.get(entity);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("Unable to access fieldName " + fieldName);
    }

    private static Field getDeclaredField(Object entity, String fieldName) {
        return getDeclaredField(entity.getClass(), fieldName);
    }

    private static Field getDeclaredField(Class classForEntity, String fieldName) {
        try {
            return classForEntity.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (classForEntity.getSuperclass() != null) {
                return getDeclaredField(classForEntity.getSuperclass(), fieldName);
            } else {
                return null;
            }
        }
    }
}
