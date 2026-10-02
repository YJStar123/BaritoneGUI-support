package baritonegui;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** Friendly names for GLFW key codes (independent of Minecraft mappings). */
public final class KeyNames {

    private static final Map<Integer, String> NAMES = new HashMap<>();

    static {
        for (int c = 'A'; c <= 'Z'; c++) NAMES.put(c, String.valueOf((char) c));
        for (int c = '0'; c <= '9'; c++) NAMES.put(c, String.valueOf((char) c));

        NAMES.put(GLFW.GLFW_KEY_SPACE, "Space");
        NAMES.put(GLFW.GLFW_KEY_APOSTROPHE, "'");
        NAMES.put(GLFW.GLFW_KEY_COMMA, ",");
        NAMES.put(GLFW.GLFW_KEY_MINUS, "-");
        NAMES.put(GLFW.GLFW_KEY_PERIOD, ".");
        NAMES.put(GLFW.GLFW_KEY_SLASH, "/");
        NAMES.put(GLFW.GLFW_KEY_SEMICOLON, ";");
        NAMES.put(GLFW.GLFW_KEY_EQUAL, "=");
        NAMES.put(GLFW.GLFW_KEY_LEFT_BRACKET, "[");
        NAMES.put(GLFW.GLFW_KEY_BACKSLASH, "\\");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_BRACKET, "]");
        NAMES.put(GLFW.GLFW_KEY_GRAVE_ACCENT, "`");
        NAMES.put(GLFW.GLFW_KEY_ESCAPE, "Esc");
        NAMES.put(GLFW.GLFW_KEY_ENTER, "Enter");
        NAMES.put(GLFW.GLFW_KEY_TAB, "Tab");
        NAMES.put(GLFW.GLFW_KEY_BACKSPACE, "Backspace");
        NAMES.put(GLFW.GLFW_KEY_INSERT, "Insert");
        NAMES.put(GLFW.GLFW_KEY_DELETE, "Delete");
        NAMES.put(GLFW.GLFW_KEY_RIGHT, "Right");
        NAMES.put(GLFW.GLFW_KEY_LEFT, "Left");
        NAMES.put(GLFW.GLFW_KEY_DOWN, "Down");
        NAMES.put(GLFW.GLFW_KEY_UP, "Up");
        NAMES.put(GLFW.GLFW_KEY_PAGE_UP, "PageUp");
        NAMES.put(GLFW.GLFW_KEY_PAGE_DOWN, "PageDown");
        NAMES.put(GLFW.GLFW_KEY_HOME, "Home");
        NAMES.put(GLFW.GLFW_KEY_END, "End");
        NAMES.put(GLFW.GLFW_KEY_CAPS_LOCK, "CapsLock");
        NAMES.put(GLFW.GLFW_KEY_SCROLL_LOCK, "ScrollLock");
        NAMES.put(GLFW.GLFW_KEY_NUM_LOCK, "NumLock");
        NAMES.put(GLFW.GLFW_KEY_PRINT_SCREEN, "PrintScreen");
        NAMES.put(GLFW.GLFW_KEY_PAUSE, "Pause");
        NAMES.put(GLFW.GLFW_KEY_LEFT_SHIFT, "LShift");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_SHIFT, "RShift");
        NAMES.put(GLFW.GLFW_KEY_LEFT_CONTROL, "LCtrl");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_CONTROL, "RCtrl");
        NAMES.put(GLFW.GLFW_KEY_LEFT_ALT, "LAlt");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_ALT, "RAlt");
        NAMES.put(GLFW.GLFW_KEY_LEFT_SUPER, "LSuper");
        NAMES.put(GLFW.GLFW_KEY_RIGHT_SUPER, "RSuper");
        NAMES.put(GLFW.GLFW_KEY_MENU, "Menu");

        for (int i = 0; i <= 9; i++) NAMES.put(GLFW.GLFW_KEY_KP_0 + i, "Num" + i);
        for (int i = 1; i <= 25; i++) NAMES.put(GLFW.GLFW_KEY_F1 + (i - 1), "F" + i);
    }

    public static String name(int code) {
        if (code < 0) return Lang.tx("baritonegui.key.none", "无").getString();
        String s = NAMES.get(code);
        if (s != null) return s;
        try {
            String n = GLFW.glfwGetKeyName(code, 0);
            if (n != null && !n.isEmpty()) return n.toUpperCase();
        } catch (Throwable ignored) {
            // lwjgl not fully initialised for this key
        }
        return "Key#" + code;
    }

    private KeyNames() {
    }
}
