package baritonegui;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import baritonegui.GuiActionPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BaritoneGuiMod implements ClientModInitializer {

    public static final Logger LOG = LoggerFactory.getLogger("BaritoneGUI");
    public static final Settings CONFIG = new Settings();
    public static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        LOG.info("BaritoneGUI support mod initializing...");
        Settings.load();
        HotkeyManager.setHotkeys(HotkeyConfig.load());

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.baritonegui.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                new KeyBinding.Category(Identifier.of("category.baritonegui"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openGuiKey.wasPressed() && client.currentScreen == null) {
                client.setScreen(new GuiActionPanel(null));
            }
            HotkeyManager.tick();
        });

        if (!BaritoneBridge.isAvailable()) {
            LOG.warn("Baritone was NOT detected on the classpath. Hotkeys will not execute until Baritone is installed.");
        } else {
            LOG.info("Baritone detected. GUI hotkeys ready.");
        }

        // Render the black "latest Baritone hint" overlay on top of the HUD,
        // independent of whether the action panel is open. The panel also draws
        // it from its own render so it stays visible while the GUI is up.
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            String txt = ChatCapture.currentOverlay();
            if (txt != null) OverlayRenderer.draw(drawContext, txt);
        });
    }

    /** Send a local chat message as user feedback. */
    public static void feedback(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null && client.inGameHud.getChatHud() != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(message));
        }
    }

    /** Mod-wide settings (kept simple; persisted separately if needed). */
    public static class Settings {
        public boolean notifyInChat = true;
        /** When true, the GUI auto-closes right after the user clicks "执行". */
        public boolean exitAfterExecute = false;

        private static Path file() {
            return FabricLoader.getInstance().getConfigDir().resolve("baritonegui_settings.txt");
        }

        /** Load persisted settings from the config directory (best-effort). */
        public static void load() {
            Path f = file();
            if (!Files.exists(f)) return;
            try (BufferedReader r = Files.newBufferedReader(f)) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("exitAfterExecute=")) {
                        CONFIG.exitAfterExecute = Boolean.parseBoolean(
                                line.substring("exitAfterExecute=".length()).trim());
                    }
                }
            } catch (IOException ignored) {
                // corrupt file — keep defaults
            }
        }

        /** Persist current settings (best-effort). */
        public static void save() {
            Path f = file();
            try {
                if (f.getParent() != null) Files.createDirectories(f.getParent());
                try (BufferedWriter w = Files.newBufferedWriter(f)) {
                    w.write("exitAfterExecute=" + CONFIG.exitAfterExecute + "\n");
                }
            } catch (IOException ignored) {
                // best-effort persistence
            }
        }
    }
}
