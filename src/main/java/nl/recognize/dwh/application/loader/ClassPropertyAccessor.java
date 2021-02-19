package nl.recognize.dwh.application.loader;

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
            return classForEntity.getDeclaredField(fieldName).get(entity);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

}
