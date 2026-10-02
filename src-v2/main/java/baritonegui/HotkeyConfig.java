package baritonegui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Load / save hotkeys and provide conflict detection. */
public class HotkeyConfig {

    public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("baritonegui");
    public static final Path FILE = DIR.resolve("hotkeys.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Hotkey>>() {}.getType();

    public static List<Hotkey> load() {
        try {
            if (!Files.exists(FILE)) return new ArrayList<>();
            try (java.io.Reader r = Files.newBufferedReader(FILE)) {
                List<Hotkey> list = GSON.fromJson(r, LIST_TYPE);
                if (list == null) return new ArrayList<>();
                for (Hotkey h : list) {
                    if (h.id == null) h.id = UUID.randomUUID().toString();
                    if (h.label == null) h.label = "";
                    if (h.command == null) h.command = "";
                }
                return list;
            }
        } catch (Exception e) {
            BaritoneGuiMod.LOG.error("Failed to load hotkeys: {}", e.toString());
            return new ArrayList<>();
        }
    }

    public static void save(List<Hotkey> list) {
        try {
            if (!Files.exists(DIR)) Files.createDirectories(DIR);
            try (java.io.Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(list, w);
            }
        } catch (Exception e) {
            BaritoneGuiMod.LOG.error("Failed to save hotkeys: {}", e.toString());
        }
    }

    /**
     * Returns the set of hotkey ids that share an identical key+modifier combo
     * with at least one other assigned hotkey.
     */
    public static Map<String, Boolean> findConflicts(List<Hotkey> list) {
        Map<String, List<Hotkey>> groups = new HashMap<>();
        for (Hotkey h : list) {
            if (!h.isAssigned()) continue;
            String key = h.keyCode + "|" + h.ctrl + "|" + h.alt + "|" + h.shift;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(h);
        }
        Map<String, Boolean> conflicts = new HashMap<>();
        for (List<Hotkey> group : groups.values()) {
            if (group.size() > 1) {
                for (Hotkey h : group) conflicts.put(h.id, true);
            }
        }
        return conflicts;
    }
}
