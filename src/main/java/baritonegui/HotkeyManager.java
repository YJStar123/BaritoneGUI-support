package baritonegui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Holds the active hotkey list and triggers Baritone commands on key edges. */
public class HotkeyManager {

    private static List<Hotkey> hotkeys = new ArrayList<>();
    private static final Map<String, Boolean> prevPressed = new HashMap<>();

    public static void setHotkeys(List<Hotkey> list) {
        hotkeys = list;
    }

    public static List<Hotkey> getHotkeys() {
        return hotkeys;
    }

    /** Called every client tick from the mod initializer. */
    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // Never fire gameplay hotkeys while a screen (chat, inventory, our GUI) is open.
        if (client.currentScreen != null) {
            prevPressed.clear();
            return;
        }

        long window = client.getWindow().getHandle();
        boolean ctrl = isDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || isDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean alt = isDown(window, GLFW.GLFW_KEY_LEFT_ALT) || isDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
        boolean shift = isDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || isDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);

        for (Hotkey h : hotkeys) {
            if (!h.enabled || !h.isAssigned()) {
                prevPressed.put(h.id, false);
                continue;
            }
            boolean down = isDown(window, h.keyCode);
            boolean wasDown = prevPressed.getOrDefault(h.id, false);
            boolean modifiersMatch = (ctrl == h.ctrl) && (alt == h.alt) && (shift == h.shift);

            if (down && !wasDown && modifiersMatch) {
                trigger(h);
            }
            prevPressed.put(h.id, down);
        }
    }

    private static boolean isDown(long window, int key) {
        return InputUtil.isKeyPressed(window, key);
    }

    private static void trigger(Hotkey h) {
        boolean ok = BaritoneBridge.execute(h.command);
        if (BaritoneGuiMod.CONFIG.notifyInChat) {
            if (ok) {
                BaritoneGuiMod.feedback(Lang.tx1("baritonegui.feedback.exec", "[BaritoneGUI] 执行: %s", h.command).getString());
            } else {
                BaritoneGuiMod.feedback(Lang.tx1("baritonegui.feedback.execFail", "[BaritoneGUI] 执行失败(确认Baritone已安装): %s", h.command).getString());
            }
        }
        BaritoneGuiMod.LOG.info("Hotkey '{}' triggered command '{}' -> success={}", h.label, h.command, ok);
    }
}
