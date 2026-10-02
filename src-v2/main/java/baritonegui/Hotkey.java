package baritonegui;

import java.util.UUID;

/** Data model for a single hotkey binding. Pure POJO so Gson can serialize it. */
public class Hotkey {

    public String id = UUID.randomUUID().toString();
    public String label = "";
    /** GLFW key code, or -1 when unassigned. */
    public int keyCode = -1;
    public boolean ctrl = false;
    public boolean alt = false;
    public boolean shift = false;
    /** The Baritone command to run (without leading dot). */
    public String command = "";
    public boolean enabled = true;

    public Hotkey() {
    }

    public Hotkey(String label, int keyCode, boolean ctrl, boolean alt, boolean shift, String command, boolean enabled) {
        this.label = label;
        this.keyCode = keyCode;
        this.ctrl = ctrl;
        this.alt = alt;
        this.shift = shift;
        this.command = command;
        this.enabled = enabled;
    }

    public boolean isAssigned() {
        return keyCode >= 0;
    }

    public String keyComboText() {
        if (!isAssigned()) return Lang.tx("baritonegui.key.unbound", "未绑定").getString();
        StringBuilder sb = new StringBuilder();
        if (ctrl) sb.append("Ctrl+");
        if (alt) sb.append("Alt+");
        if (shift) sb.append("Shift+");
        sb.append(KeyNames.name(keyCode));
        return sb.toString();
    }

    /** Stable identity for conflict / runtime state lookups. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hotkey)) return false;
        return id != null ? id.equals(((Hotkey) o).id) : super.equals(o);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
