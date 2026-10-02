package baritonegui;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Persisted per-action favorites. The set of favorited action ids is stored as a
 * plain newline-separated file inside the Fabric config directory
 * ({@code <game>/config/baritonegui_favorites.txt}) so it survives restarts and
 * is shared across all built MC versions.
 */
public final class Favorites {

    private static final Set<String> ids = new HashSet<>();
    private static volatile boolean loaded = false;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("baritonegui_favorites.txt");
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Path f = file();
        if (Files.exists(f)) {
            try (BufferedReader r = Files.newBufferedReader(f)) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) ids.add(line);
                }
            } catch (IOException ignored) {
                // corrupt file — start fresh
            }
        }
    }

    public static boolean isFav(String id) {
        ensureLoaded();
        return ids.contains(id);
    }

    /** Toggle the favorite state of an action and persist the change. */
    public static void toggle(String id) {
        ensureLoaded();
        if (ids.contains(id)) ids.remove(id);
        else ids.add(id);
        save();
    }

    private static void save() {
        Path f = file();
        try {
            if (f.getParent() != null) Files.createDirectories(f.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(f)) {
                for (String id : ids) w.write(id + "\n");
            }
        } catch (IOException ignored) {
            // best-effort persistence
        }
    }
}
