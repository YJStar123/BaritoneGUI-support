package baritonegui;

import java.lang.reflect.Method;

/**
 * Reflection-based bridge to Baritone's public API.
 *
 * <p>Baritone is an OPTIONAL runtime dependency. We never reference Baritone
 * classes at compile time so the mod builds without Baritone on the classpath
 * and keeps working across every Baritone / Minecraft version that exposes the
 * stable {@code baritone.api} package.
 *
 * <p>Resolved call chain:
 * <pre>
 *   BaritoneAPI.getProvider()
 *     .getPrimaryBaritone()
 *     .getCommandManager()
 *     .execute(String) -&gt; boolean
 * </pre>
 */
public final class BaritoneBridge {

    private static boolean initialized = false;
    private static boolean available = false;
    private static Object commandManager = null;
    private static Method executeMethod = null;

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> api = Class.forName("baritone.api.BaritoneAPI");
            Method getProvider = api.getMethod("getProvider");
            Object provider = getProvider.invoke(null);

            Method getPrimary = provider.getClass().getMethod("getPrimaryBaritone");
            Object baritone = getPrimary.invoke(provider);

            Method getCmd = baritone.getClass().getMethod("getCommandManager");
            Object cm = getCmd.invoke(baritone);

            Method exec = cm.getClass().getMethod("execute", String.class);

            commandManager = cm;
            executeMethod = exec;
            available = true;
            BaritoneGuiMod.LOG.info("Baritone API bridged successfully.");
        } catch (Throwable t) {
            available = false;
            BaritoneGuiMod.LOG.warn("Baritone not found / could not be bridged: {}", t.toString());
        }
    }

    public static boolean isAvailable() {
        init();
        return available;
    }

    /** Execute a Baritone command (without the leading dot). Returns success. */
    public static boolean execute(String command) {
        init();
        if (!available || command == null || command.trim().isEmpty()) return false;
        if (command.startsWith(".")) command = command.substring(1);
        try {
            if (commandManager == null || executeMethod == null) init();
            if (commandManager == null || executeMethod == null) return false;
            Object result = executeMethod.invoke(commandManager, command);
            return result instanceof Boolean ? (Boolean) result : true;
        } catch (Throwable t) {
            BaritoneGuiMod.LOG.error("Failed to execute Baritone command '{}': {}", command, t.toString());
            return false;
        }
    }

    /** Convenience: cancel any current Baritone pathing/process. */
    public static boolean stop() {
        return execute("cancel");
    }
}
