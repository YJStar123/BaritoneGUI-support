package baritonegui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * "GUI模组设置" entry screen. Hosts the mod's own preferences (e.g. auto-close
 * after Execute) plus a passive, automatic Baritone-compatibility detector.
 *
 * <p>The detector runs with no user interaction: on entry it shows a yellow
 * "正在检测Baritone是否适配..." line, then after a short delay scans whether the
 * Baritone mod is loaded and bridges its public API, reporting the result in the
 * center below the configuration item (red = not found / not API, green = API
 * build present).
 */
public class GuiSettingsScreen extends Screen {

    private final Screen parent;

    // ---- Baritone auto-detection state ----
    private String detectText;
    private int detectColor;
    private boolean detected = false;
    private long enterTime;

    // Configuration item rendered manually (kept out of the widget tree so the
    // source compiles identically on 1.20.1 and 1.21.x).
    private ExitCheckbox exitChk;

    public GuiSettingsScreen(Screen parent) {
        super(Lang.tx("baritonegui.screen.settings", "GUI模组设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        enterTime = System.currentTimeMillis();
        detectText = Lang.tx("baritonegui.detect.detecting", "正在检测Baritone是否适配...").getString();
        detectColor = 0xFFFFFF00; // yellow
        detected = false;

        // Configuration item: close GUI after clicking "执行".
        exitChk = new ExitCheckbox(this.width / 2 - 115, 64, 230, 20,
                BaritoneGuiMod.CONFIG.exitAfterExecute,
                Lang.tx("baritonegui.checkbox.exitAfterExecute", "点击\"执行\"后退出GUI界面"));

        // Back button.
        this.addDrawableChild(ButtonWidget.builder(Lang.tx("baritonegui.button.back", "返回"), b -> this.close())
                .dimensions(this.width / 2 - 60, this.height - 30, 120, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);

        // Keep showing the yellow "detecting" state briefly, then run the scan.
        if (!detected && System.currentTimeMillis() - enterTime >= 400) {
            performDetection();
            detected = true;
        }

        // Render the checkbox manually (not a widget, for cross-version safety).
        if (exitChk != null) {
            exitChk.render(ctx, mx, my, delta);
        }

        // Detection result, centered just below the config item (checkbox at y=64,
        // height 20 -> draw around y=96).
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(detectText),
                this.width / 2, 96, detectColor);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0 && exitChk != null && exitChk.isMouseOver(mouseX, mouseY)) {
            exitChk.toggle();
            return true;
        }
        return super.mouseClicked(click, bl);
    }

    /**
     * Check Baritone compatibility without guessing from file names (Fabric
     * Baritone jars are usually just "baritone-&lt;version&gt;.jar" with no "api"
     * in the name). Instead we (1) ask FabricLoader whether a mod with the
     * "baritone" modid is actually loaded, and (2) verify the GUI can bridge
     * Baritone's public API (baritone.api.BaritoneAPI) — the exact capability
     * this mod relies on to send commands.
     */
    private void performDetection() {
        boolean modLoaded = FabricLoader.getInstance().getModContainer("baritone").isPresent();
        boolean apiOk = BaritoneBridge.isAvailable();
        if (!modLoaded) {
            detectText = Lang.tx("baritonegui.detect.noBaritone", "未检测到Baritone").getString();
            detectColor = 0xFFFF5555; // red
        } else if (apiOk) {
            detectText = Lang.tx("baritonegui.detect.compatible", "Baritone适配").getString();
            detectColor = 0xFF55FF55; // green
        } else {
            detectText = Lang.tx("baritonegui.detect.notApi", "Baritone未适配，请下载api版本！").getString();
            detectColor = 0xFFFF5555; // red
        }
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    /**
     * Minimal checkbox (box + label) for the "退出GUI" option. Implemented as a
     * plain {@link Drawable} so the source compiles unchanged across every MC
     * version this mod targets; click handling is done in the screen's
     * {@link #mouseClicked(double, double, int)}.
     */
    private class ExitCheckbox implements Drawable {
        private final int x, y, w, h;
        private boolean checked;
        private final Text label;

        ExitCheckbox(int x, int y, int w, int h, boolean checked, Text label) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.checked = checked;
            this.label = label;
        }

        boolean isMouseOver(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        void toggle() {
            checked = !checked;
            BaritoneGuiMod.CONFIG.exitAfterExecute = checked;
            BaritoneGuiMod.Settings.save();
        }

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int box = 12;
            int bx = x + 2;
            int by = y + (h - box) / 2;
            // Draw a 1px white border manually (DrawContext.drawBorder is not
            // stable across the 1.21.x generations; fill is).
            ctx.fill(bx, by, bx + box, by + 1, 0xFFFFFFFF);
            ctx.fill(bx, by + box - 1, bx + box, by + box, 0xFFFFFFFF);
            ctx.fill(bx, by, bx + 1, by + box, 0xFFFFFFFF);
            ctx.fill(bx + box - 1, by, bx + box, by + box, 0xFFFFFFFF);
            if (checked) ctx.fill(bx + 3, by + 3, bx + box - 3, by + box - 3, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, label, bx + box + 6,
                    y + (h - 8) / 2, 0xFFFFFF);
        }
    }
}
