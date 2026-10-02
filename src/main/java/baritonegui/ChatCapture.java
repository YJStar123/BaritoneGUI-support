package baritonegui;

/**
 * Holds the most recent Baritone chat line (captured by {@code ChatHudMixin})
 * and the current on-screen overlay text.
 *
 * <p>The toggle button executes {@code set toggle} and then surfaces Baritone's
 * latest hint here; {@link OverlayRenderer} draws it on a black block. This
 * deliberately avoids re-reading Baritone's internal settings state, which was
 * the source of the previous "always shows 禁用" bug.
 */
public final class ChatCapture {

    private static volatile String latest = "";
    private static volatile String overlayText = "";
    private static volatile long overlayUntil = 0;

    /** Called by the ChatHud mixin for every captured Baritone chat line. */
    public static void onBaritoneMessage(String msg) {
        if (msg != null) latest = msg;
    }

    /** The most recent Baritone chat line captured (may be empty). */
    public static String latest() {
        return latest;
    }

    /** Show {@code msg} on the black overlay for {@code durationMs} milliseconds.
     *  If {@code msg} is null/empty, falls back to the latest captured line. */
    public static void showOverlay(String msg, long durationMs) {
        String t = (msg == null || msg.isEmpty()) ? latest : msg;
        if (t == null) t = "";
        overlayText = t;
        overlayUntil = System.currentTimeMillis() + durationMs;
    }

    /** The overlay text to draw now, or null if it has expired / never shown. */
    public static String currentOverlay() {
        if (overlayUntil <= 0) return null;
        if (System.currentTimeMillis() > overlayUntil) return null;
        return overlayText;
    }
}
