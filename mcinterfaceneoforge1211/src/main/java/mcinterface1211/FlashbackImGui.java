package mcinterface1211;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

// Access class for Flashback's Dear ImGui, so IV doesn't need it.
public final class FlashbackImGui {
    private static Class<?> imGui;
    private static Class<?> imBoolean;
    private static boolean initialized;
    private static boolean failed;

    public static boolean ready() {
        if (!initialized) {
            try {
                imGui = Class.forName("imgui.moulberry90.ImGui");
                imBoolean = Class.forName("imgui.moulberry90.type.ImBoolean");
                initialized = true;
            } catch (ClassNotFoundException e) {
                failed = true;
            }
        }
        return !failed;
    }

    private static Object call(String name, Class<?>[] types, Object... args) {
        try {
            Method method = imGui.getMethod(name, types);
            return method.invoke(null, args);
        } catch (Throwable e) {
            failed = true;
            return null;
        }
    }

    public static Object newImBoolean(final boolean value) {
        try {
            Constructor<?> constructor = imBoolean.getConstructor(boolean.class);
            return constructor.newInstance(value);
        } catch (Throwable e) {
            failed = true;
            return null;
        }
    }

    public static boolean imBooleanGet(Object value) {
        try {
            return (boolean) imBoolean.getMethod("get").invoke(value);
        } catch (Throwable e) {
            failed = true;
            return false;
        }
    }

    public static void imBooleanSet(Object value, final boolean newValue) {
        try {
            imBoolean.getMethod("set", boolean.class).invoke(value, newValue);
        } catch (Throwable e) {
            failed = true;
        }
    }

    public static Object newImString() {
        try {
            return Class.forName("imgui.moulberry90.type.ImString").getConstructor().newInstance();
        } catch (Throwable e) {
            failed = true;
            return null;
        }
    }

    public static String imStringGet(Object value) {
        try {
            return (String) value.getClass().getMethod("get").invoke(value);
        } catch (Throwable e) {
            failed = true;
            return null;
        }
    }

    public static void setNextWindowSizeConstraints(final float sizeMinX, final float sizeMinY, final float sizeMaxX, final float sizeMaxY) {
        call("setNextWindowSizeConstraints", new Class<?>[] { float.class, float.class, float.class, float.class }, sizeMinX, sizeMinY, sizeMaxX, sizeMaxY);
    }

    public static void pushID(final int intId) {
        call("pushID", new Class<?>[] { int.class }, intId);
    }

    public static void popID() {
        call("popID", new Class<?>[0]);
    }

    public static void separator() {
        call("separator", new Class<?>[0]);
    }

    public static void sameLine() {
        call("sameLine", new Class<?>[0]);
    }

    public static void text(final String text) {
        call("text", new Class<?>[] { String.class }, text);
    }

    public static boolean button(final String label) {
        return Boolean.TRUE.equals(call("button", new Class<?>[] { String.class }, label));
    }

    public static boolean checkbox(String label, boolean active) {
        return Boolean.TRUE.equals(call("checkbox", new Class<?>[] { String.class, boolean.class }, label, active));
    }

    public static boolean menuItem(final String label) {
        return Boolean.TRUE.equals(call("menuItem", new Class<?>[] { String.class }, label));
    }

    public static boolean inputTextWithHint(final String label, final String hint, final Object text) {
        return Boolean.TRUE.equals(call("inputTextWithHint", new Class<?>[] { String.class, String.class, text.getClass() }, label, hint, text));
    }

    public static boolean begin(final String title, final Object pOpen) {
        return Boolean.TRUE.equals(call("begin", new Class<?>[] { String.class, imBoolean }, title, pOpen));
    }

    public static void end() {
        call("end", new Class<?>[0]);
    }


}
