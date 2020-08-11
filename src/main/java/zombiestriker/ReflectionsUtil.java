package zombiestriker;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Pattern;

public class ReflectionsUtil {

    // Deduce the net.minecraft.server.v* package
    private static String OBC_PREFIX = Bukkit.getServer().getClass().getPackage().getName();
    private static String NMS_PREFIX = OBC_PREFIX.replace("org.bukkit.craftbukkit", "net.minecraft.server");
    private static String VERSION = OBC_PREFIX.replace("org.bukkit.craftbukkit", "").replace(".", "");
    // Variable replacement
    private static Pattern MATCH_VARIABLE = Pattern.compile("\\{([^\\}]+)\\}");

    private static Material skull;
    private static final String SERVER_VERSION;


    static {
        String name = Bukkit.getServer().getClass().getName();
        name = name.substring(name.indexOf("craftbukkit.")
                + "craftbukkit.".length());
        name = name.substring(0, name.indexOf("."));
        SERVER_VERSION = name;
    }

    private ReflectionsUtil() {
    }


    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param target
     *            the target type
     * @param name
     *            the name of the field, or NULL to ignore
     * @param fieldType
     *            a compatible field type
     * @return the field accessor
     */
    public static <T> FieldAccessor<T> getField(Class<?> target, String name, Class<T> fieldType) {
        return getField(target, name, fieldType, 0);
    }


    // Common method
    private static <T> FieldAccessor<T> getField(Class<?> target, String name, Class<T> fieldType, int indx) {
        int index = indx;
        for (final Field field : target.getDeclaredFields()) {
            if ((name == null || field.getName().equals(name)) && fieldType.isAssignableFrom(field.getType())
                    && index-- <= 0) {
                field.setAccessible(true);

                // A function for retrieving a specific field value
                return new FieldAccessor<T>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public T get(Object target) {
                        try {
                            return (T) field.get(target);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Cannot access reflection.", e);
                        }
                    }

                    @Override
                    public void set(Object target, Object value) {
                        try {
                            field.set(target, value);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Cannot access reflection.", e);
                        }
                    }

                    @Override
                    public boolean hasField(Object target) {
                        // target instanceof DeclaringClass
                        return field.getDeclaringClass().isAssignableFrom(target.getClass());
                    }
                };
            }
        }

        // Search in parent classes
        if (target.getSuperclass() != null)
            return getField(target.getSuperclass(), name, fieldType, index);
        throw new IllegalArgumentException("Cannot find field with type " + fieldType);
    }


    /**
     * Search for the first publicly and privately defined method of the given name
     * and parameter count.
     *
     * @param clazz
     *            a class to start with
     * @param methodName
     *            the method name, or NULL to skip
     * @param params
     *            the expected parameters
     * @return an object that invokes this specific method
     * @throws IllegalStateException
     *             If we cannot find this method
     */
    public static MethodInvoker getMethod(Class<?> clazz, String methodName, Class<?>... params) {
        return getTypedMethod(clazz, methodName, null, params);
    }


    /**
     * Search for the first publicly and privately defined method of the given name
     * and parameter count.
     *
     * @param clazz
     *            a class to start with
     * @param methodName
     *            the method name, or NULL to skip
     * @param returnType
     *            the expected return type, or NULL to ignore
     * @param params
     *            the expected parameters
     * @return an object that invokes this specific method
     * @throws IllegalStateException
     *             If we cannot find this method
     */
    public static MethodInvoker getTypedMethod(Class<?> clazz, String methodName, Class<?> returnType,
                                               Class<?>... params) {
        for (final Method method : clazz.getDeclaredMethods()) {
            if ((methodName == null || method.getName().equals(methodName)) && (returnType == null)
                    || method.getReturnType().equals(returnType) && Arrays.equals(method.getParameterTypes(), params)) {

                method.setAccessible(true);
                return new MethodInvoker() {
                    @Override
                    public Object invoke(Object target, Object... arguments) {
                        try {
                            return method.invoke(target, arguments);
                        } catch (Exception e) {
                            throw new RuntimeException("Cannot invoke method " + method, e);
                        }
                    }
                };
            }
        }
        // Search in every superclass
        if (clazz.getSuperclass() != null)
            return getMethod(clazz.getSuperclass(), methodName, params);
        throw new IllegalStateException(
                String.format("Unable to find method %s (%s).", methodName, Arrays.asList(params)));
    }


    /**
     * An interface for invoking a specific method.
     */
    public interface MethodInvoker {
        /**
         * Invoke a method on a specific target object.
         *
         * @param target
         *            the target object, or NULL for a static method.
         * @param arguments
         *            the arguments to pass to the method.
         * @return the return value, or NULL if is void.
         */
        public Object invoke(Object target, Object... arguments);
    }

    /**
     * An interface for retrieving the field content.
     *
     * @param <T>
     *            field type
     */
    public interface FieldAccessor<T> {
        /**
         * Retrieve the content of a field.
         *
         * @param target
         *            the target object, or NULL for a static field
         * @return the value of the field
         */
        public T get(Object target);

        /**
         * Set the content of a field.
         *
         * @param target
         *            the target object, or NULL for a static field
         * @param value
         *            the new value of the field
         */
        public void set(Object target, Object value);

        /**
         * Determine if the given object has this field.
         *
         * @param target
         *            the object to test
         * @return TRUE if it does, FALSE otherwise
         */
        public boolean hasField(Object target);
    }

    public static Material getSkull() {
        if (skull == null) {
            try {
                skull = Material.matchMaterial("SKULL_ITEM");
            } catch (Error | Exception ignored) {
            }
            if (skull == null)
                skull = Material.PLAYER_HEAD;
        }
        return skull;
    }
}