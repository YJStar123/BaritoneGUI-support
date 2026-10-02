package baritonegui;

import baritonegui.BaritoneActions.Action;
import baritonegui.BaritoneActions.Category;
import baritonegui.BaritoneActions.Param;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main operations panel opened by the menu hotkey.
 * Left column = big categories, right column = functions of the selected category.
 * Functions that need input get a custom text field WITH vanilla-style autocompletion
 * (mouse wheel / Tab / arrows / click, ghost preview) driven by the field's "kind".
 *
 * <p>The right column is a scrollable area (mouse wheel + a scissored background).
 * Clicking a category recreates this screen with the new selection.
 */
public class GuiActionPanel extends Screen {

    private final Screen parent;
    private final int selected;

    private int scrollY = 0;
    private int maxScroll = 0;

    private int panelX, panelW, panelTop, panelBottom;

    private static class Row {
        final Action action;
        final int baseY;
        final int h;
        final int fieldW;
        final TextFieldWidget[] fields;
        final ButtonWidget exec;
        final ButtonWidget favBtn;
        final ButtonWidget toggleBtn;
        final boolean isToggle;
        final String settingName;

        Row(Action action, int baseY, int h, int fieldW, TextFieldWidget[] fields, ButtonWidget exec, ButtonWidget favBtn, ButtonWidget toggleBtn, boolean isToggle, String settingName) {
            this.action = action;
            this.baseY = baseY;
            this.h = h;
            this.fieldW = fieldW;
            this.fields = fields;
            this.exec = exec;
            this.favBtn = favBtn;
            this.toggleBtn = toggleBtn;
            this.isToggle = isToggle;
            this.settingName = settingName;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    /** Maps each input field to its autocomplete kind. */
    private final Map<TextFieldWidget, String> fieldKind = new HashMap<>();

    // ---- Autocomplete state ----
    private TextFieldWidget sugField;
    private List<String> sugCands = new ArrayList<>();
    private int sugSel = 0;
    private boolean sugOpen = true;
    private String lastSugText = "\u0000";
    private final List<int[]> sugRects = new ArrayList<>();

    private static final Map<String, List<String>> FULL = new HashMap<>();

    public GuiActionPanel(Screen parent) {
        this(parent, 0);
    }

    public GuiActionPanel(Screen parent, int selected) {
        this(parent, selected, 0);
    }

    public GuiActionPanel(Screen parent, int selected, int scrollY) {
        super(Lang.tx("baritonegui.screen.panel", "BaritoneGUI - 操作面板"));
        this.parent = parent;
        this.selected = selected;
        this.scrollY = scrollY;
    }

    @Override
    protected void init() {
        int rightX = 132;
        int rightW = this.width - 140;
        this.panelX = rightX;
        this.panelW = rightW;
        this.panelTop = 40;
        this.panelBottom = this.height - 36;

        final int frX = rightX, frW = rightW, top = panelTop, bottom = panelBottom;
        this.addDrawable(new Drawable() {
            @Override
            public void render(DrawContext ctx, int mx, int my, float d) {
                ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);
                ctx.drawCenteredTextWithShadow(textRenderer,
                        Lang.tx("baritonegui.credit", "by B站云烬Star"),
                        width / 2, 22, 0xAAAAAA);
                ctx.enableScissor(frX, top, frX + frW + 18, bottom);
                for (Row r : rows) {
                    int ry = r.baseY - scrollY;
                    if (ry + r.h < top || ry > bottom) continue;
                    boolean fav = Favorites.isFav(r.action.id);
                    ctx.fill(frX, ry, frX + frW, ry + r.h - 2, fav ? 0x55443322 : 0x55222222);
                    ctx.drawTextWithShadow(textRenderer, Lang.tx(r.action.key, r.action.display), frX + 6, ry + 5, 0xFFFFFF);
                    for (int p = 0; p < r.fields.length; p++) {
                        int fx = frX + 6 + p * (r.fieldW + 8);
                        ctx.drawTextWithShadow(textRenderer, Lang.tx("baritonegui.param." + r.action.params[p].label, r.action.params[p].label), fx, ry + 14, 0x88CCFF);
                    }
                }
                ctx.disableScissor();
                // Divider between the left category column (ends at x=120) and the
                // right action list (starts at x=132).
                ctx.drawVerticalLine(126, panelTop, panelBottom, 0xFF555555);
            }
        });

        // Left: category buttons.
        Category[] cats = BaritoneActions.allCategories();
        for (int c = 0; c < cats.length; c++) {
            final int ci = c;
            String marker = (c == selected) ? "▸ " : "  ";
            this.addDrawableChild(ButtonWidget.builder(Text.literal(marker).append(Lang.tx(cats[c].key, cats[c].name)), b ->
                    this.client.setScreen(new GuiActionPanel(this.parent, ci, scrollY))
            ).dimensions(8, 40 + c * 24, 112, 20).build());
        }

        // Right: functions of the selected category, with favorites pinned to the top.
        Category cat = cats[selected];
        List<Action> list = new ArrayList<>(Arrays.asList(cat.actions));
        list.sort((a, b) -> Boolean.compare(Favorites.isFav(b.id), Favorites.isFav(a.id)));
        int y = 40;
        for (Action a : list) {
            boolean isToggle = a.command.startsWith("set toggle ") && a.command.indexOf('%') < 0;
            boolean hasParams = a.params != null && a.params.length > 0;
            int rowH = hasParams ? 52 : 26;
            String settingName = isToggle ? a.command.substring("set toggle ".length()) : null;
            // Reserve horizontal space on the right for the action buttons
            // (favorite + execute / toggle) so input fields never overlap them.
            int btnReserve = hasParams ? 110 : 0;
            int fieldW = hasParams ? Math.min(150, (rightW - 12 - btnReserve) / a.params.length - 8) : 0;

            TextFieldWidget[] fields;
            if (hasParams) {
                fields = new TextFieldWidget[a.params.length];
                for (int p = 0; p < a.params.length; p++) {
                    int fx = rightX + 6 + p * (fieldW + 8);
                    TextFieldWidget tf = new TextFieldWidget(this.textRenderer, fx, y + 26, fieldW, 18,
                            Lang.tx("baritonegui.param." + a.params[p].label, a.params[p].label));
                    tf.setPlaceholder(Lang.tx("baritonegui.ph." + a.params[p].placeholder, a.params[p].placeholder));
                    tf.setMaxLength(64);
                    this.addDrawableChild(tf);
                    fields[p] = tf;
                    if (a.params[p].kind != null) fieldKind.put(tf, a.params[p].kind);
                }
            } else {
                fields = new TextFieldWidget[0];
            }

            ButtonWidget exec = null;
            ButtonWidget toggleBtn = null;
            int btnY = hasParams ? y + 26 : y + 4;
            if (isToggle) {
                // A single "切换" button. We do NOT track enable/disable state in
                // the GUI; instead we surface Baritone's latest hint on a black
                // overlay. The hint is taken from the chat (captured by the
                // ChatHud mixin); if that missed for any reason we fall back to
                // reading the setting's live value so the overlay always shows
                // the real post-toggle state.
                toggleBtn = ButtonWidget.builder(Lang.tx("baritonegui.button.toggle", "切换"), b -> {
                    if (!BaritoneBridge.isAvailable()) {
                        ChatCapture.showOverlay(Lang.tx("baritonegui.msg.noBaritone", "未检测到 Baritone，无法切换").getString(), 3000);
                        return;
                    }
                    BaritoneBridge.execute("set toggle " + settingName);
                    // Show "已切换 <name> 为 true/false". The true/false is taken
                    // from Baritone's own chat hint (captured by the ChatHud mixin,
                    // e.g. "[Baritone] Toggled setting allowBreak to true"); if that
                    // was missed we fall back to reading the live value so the
                    // displayed state is still correct.
                    boolean on;
                    String hint = ChatCapture.latest();
                    String lower = (hint == null) ? "" : hint.toLowerCase(Locale.ROOT);
                    if (lower.contains("to true")) {
                        on = true;
                    } else if (lower.contains("to false")) {
                        on = false;
                    } else {
                        on = Boolean.TRUE.equals(BaritoneActions.booleanValue(settingName));
                    }
                    ChatCapture.showOverlay(Lang.tx2("baritonegui.msg.toggled", "已切换 %s 为 %s", settingName, on ? "true" : "false").getString(), 4000);
                }).dimensions(rightX + rightW - 80, btnY, 74, 18).build();
                this.addDrawableChild(toggleBtn);
            } else {
                exec = ButtonWidget.builder(Lang.tx("baritonegui.button.execute", "执行"), b -> execute(a, fields))
                        .dimensions(rightX + rightW - 80, btnY, 74, 18).build();
                this.addDrawableChild(exec);
            }

            ButtonWidget favBtn = ButtonWidget.builder(
                    Text.literal(Favorites.isFav(a.id) ? "★" : "☆"), b -> {
                        Favorites.toggle(a.id);
                        this.client.setScreen(new GuiActionPanel(this.parent, selected, scrollY));
                    }).dimensions(rightX + rightW - 104, btnY, 20, 18).build();
            this.addDrawableChild(favBtn);

            rows.add(new Row(a, y, rowH, fieldW, fields, exec, favBtn, toggleBtn, isToggle, settingName));
            y += rowH + 4;
        }

        int contentBottom = y - 4;
        maxScroll = Math.max(0, contentBottom - (panelBottom - panelTop));
        if (scrollY > maxScroll) scrollY = maxScroll;

        // Bottom buttons.
        this.addDrawableChild(ButtonWidget.builder(Lang.tx("baritonegui.button.hotkeys", "快捷键管理"), b ->
                this.client.setScreen(new GuiListScreen(this))
        ).dimensions(8, this.height - 28, 112, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Lang.tx("baritonegui.button.close", "关闭"), b -> this.close())
                .dimensions(this.width - 90, this.height - 28, 80, 20).build());

        // Refresh Baritone's live settings snapshot. (The per-toggle on/off
        // labels were removed; the black overlay now shows the real state via
        // Baritone's own chat confirmation instead.)
        this.addDrawableChild(ButtonWidget.builder(Lang.tx("baritonegui.button.refresh", "读取当前设置"), b -> {
            BaritoneActions.refresh();
            BaritoneGuiMod.feedback(Lang.tx("baritonegui.msg.readSettings", "§a[BaritoneGUI] 已读取 Baritone 当前设置").getString());
        }).dimensions(this.width - 210, this.height - 28, 112, 20).build());

        // Entry to the mod's own settings screen ("GUI模组设置"), placed to the
        // left of "读取当前设置". Hosts the auto-close-after-Execute option and
        // the passive Baritone compatibility detector.
        this.addDrawableChild(ButtonWidget.builder(Lang.tx("baritonegui.button.guiSettings", "GUI模组设置"), b ->
                this.client.setScreen(new GuiSettingsScreen(this))
        ).dimensions(this.width - 332, this.height - 28, 112, 20).build());

        applyScroll();
    }

    /** Reposition / show-hide the per-row widgets according to the current scroll. */
    private void applyScroll() {
        for (Row r : rows) {
            int ay = r.baseY - scrollY;
            boolean vis = ay + r.h > panelTop && ay < panelBottom;
            int execY = (r.fields.length > 0) ? ay + 26 : ay + 4;
            for (TextFieldWidget tf : r.fields) {
                tf.setY(ay + 26);
                tf.visible = vis;
                if (!vis && tf.isFocused()) tf.setFocused(false);
            }
            if (r.exec != null) {
                r.exec.setY(execY);
                r.exec.visible = vis;
            }
            if (r.toggleBtn != null) {
                r.toggleBtn.setY(execY);
                r.toggleBtn.visible = vis;
            }
            r.favBtn.setY(execY);
            r.favBtn.visible = vis;
        }
    }

    // ---- Autocomplete data ----

    private static List<String> full(String kind) {
        List<String> c = FULL.get(kind);
        if (c == null) {
            c = compute(kind);
            FULL.put(kind, c);
        }
        return c;
    }

    private static List<String> compute(String kind) {
        switch (kind) {
            case "player": {
                List<String> r = new ArrayList<>();
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.getNetworkHandler() != null) {
                    for (var e : mc.getNetworkHandler().getPlayerList()) {
                        r.add(e.getProfile().getName());
                    }
                }
                return r;
            }
            case "block": {
                List<String> r = new ArrayList<>();
                for (Identifier id : Registries.BLOCK.getIds()) {
                    r.add(id.toString());
                }
                return r;
            }
            case "setting":
                return baritoneSettings();
            case "axis":
                return Arrays.asList("x", "y", "z", "xy", "xz", "yz");
            case "selmode":
                return Arrays.asList("pos1", "pos2", "chunk", "clear");
            case "tag":
                return Arrays.asList("user", "home", "death", "nether", "end", "overworld", "spawn");
            case "value":
                return Arrays.asList("true", "false");
            case "schematic":
                return schematicFiles();
            default:
                return new ArrayList<>();
        }
    }

    private static List<String> schematicFiles() {
        List<String> r = new ArrayList<>();
        File dir = new File(MinecraftClient.getInstance().runDirectory, "schematics");
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                String n = f.getName();
                if (n.endsWith(".schematic") || n.endsWith(".schem") || n.endsWith(".nbt")) {
                    r.add(n.substring(0, n.lastIndexOf('.')));
                }
            }
        }
        return r;
    }

    /** All Baritone setting names, resolved via reflection (with a curated fallback). */
    private static List<String> baritoneSettings() {
        Object container = BaritoneActions.settingsContainer();
        if (container != null) {
            // Prefer byLowerName (lowercased names) when present.
            try {
                java.lang.reflect.Field f = container.getClass().getDeclaredField("byLowerName");
                f.setAccessible(true);
                Map<?, ?> m = (Map<?, ?>) f.get(container);
                List<String> r = new ArrayList<>();
                for (Object k : m.keySet()) r.add(k.toString());
                return r;
            } catch (Exception ignored) {
            }
            // Fallback: allSettings list -> Setting.getName().
            try {
                java.lang.reflect.Field f = container.getClass().getDeclaredField("allSettings");
                f.setAccessible(true);
                List<?> l = (List<?>) f.get(container);
                List<String> r = new ArrayList<>();
                for (Object s : l) {
                    try {
                        r.add((String) s.getClass().getMethod("getName").invoke(s));
                    } catch (Exception ignored) {
                    }
                }
                if (!r.isEmpty()) return r;
            } catch (Exception ignored) {
            }
        }
        return fallbackSettings();
    }

    private static List<String> fallbackSettings() {
        List<String> fb = new ArrayList<>();
        for (Category c : BaritoneActions.allCategories()) {
            for (Action a : c.actions) {
                String cmd = a.command;
                if (cmd.startsWith("set toggle ")) {
                    fb.add(cmd.substring("set toggle ".length()));
                } else if (cmd.startsWith("set ")) {
                    String rest = cmd.substring(4).trim();
                    int sp = rest.indexOf(' ');
                    String name = sp < 0 ? rest : rest.substring(0, sp);
                    if (!name.isEmpty() && !name.contains("%")) fb.add(name);
                }
            }
        }
        return fb;
    }

    // ---- Rendering ---

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);

        Element focused = this.getFocused();
        String kind = (focused instanceof TextFieldWidget) ? fieldKind.get((TextFieldWidget) focused) : null;
        if (kind == null) {
            sugField = null;
            sugCands = new ArrayList<>();
        } else {
            sugField = (TextFieldWidget) focused;
            String txt = sugField.getText();
            if (!txt.equals(lastSugText)) {
                sugOpen = true;
                lastSugText = txt;
            }
            List<String> all = full(kind);
            List<String> cands = new ArrayList<>();
            if (sugOpen) {
                String lt = txt.toLowerCase(Locale.ROOT);
                for (String s : all) {
                    if (lt.isEmpty() || s.toLowerCase(Locale.ROOT).contains(lt)) {
                        cands.add(s);
                        if (cands.size() >= 12) break;
                    }
                }
            }
            sugCands = cands;
            if (sugSel >= sugCands.size()) sugSel = 0;
            if (!sugCands.isEmpty()) {
                // Vanilla ghost preview of the top completion after the typed text.
                if (!txt.isEmpty()) {
                    String top = sugCands.get(0);
                    if (top.toLowerCase(Locale.ROOT).startsWith(txt.toLowerCase(Locale.ROOT))) {
                        String ghost = top.substring(txt.length());
                        int gx = sugField.getX() + 4 + textRenderer.getWidth(txt);
                        ctx.drawTextWithShadow(textRenderer, Text.literal(ghost), gx, sugField.getY() + 6, 0x808080);
                    }
                }
            }
        }

        // Toggle hint (black block) — drawn BEFORE the autocomplete popup so the
        // autocomplete popup stays on the topmost layer.
        String ov = ChatCapture.currentOverlay();
        if (ov != null) {
            OverlayRenderer.draw(ctx, ov);
        }

        // Autocomplete popup: kept on the very top layer with an opaque background
        // so it never visually overlaps the interface text behind it.
        if (!sugCands.isEmpty() && sugField != null) {
            drawPopup(ctx);
        }
    }

    private void drawPopup(DrawContext ctx) {
        sugRects.clear();
        int itemH = 14;
        int n = sugCands.size();
        int totalH = n * itemH + 4;
        int x = sugField.getX();
        int w = Math.max(sugField.getWidth() - 4, 120);
        for (String s : sugCands) w = Math.max(w, textRenderer.getWidth(s) + 16);
        if (x + w > this.width) w = this.width - x;
        int y = sugField.getY() - totalH - 4;
        if (y < panelTop) y = sugField.getY() + 18 + 4;

        // Fully opaque black background + bright border so the popup cleanly
        // covers any interface text behind it (no visual overlap), and stays on
        // the topmost layer.
        ctx.fill(x, y, x + w, y + totalH, 0xFF000000);
        ctx.drawBorder(x, y, w, totalH, 0xFFFFFFFF);
        for (int i = 0; i < n; i++) {
            int iy = y + 2 + i * itemH;
            if (i == sugSel) ctx.fill(x, iy, x + w, iy + itemH, 0xFF3B6EA5);
            ctx.drawTextWithShadow(textRenderer, Text.literal(sugCands.get(i)), x + 4, iy + 3, 0xFFFFFF);
            sugRects.add(new int[]{x, iy, w, itemH});
        }
    }

    private void acceptSuggestion(int i) {
        if (sugField == null || i < 0 || i >= sugCands.size()) return;
        sugField.setText(sugCands.get(i));
        sugSel = 0;
    }

    // ---- Input handling ----

    // NOTE: no @Override — Screen.mouseScrolled is 3-arg on 1.20.x and 4-arg on 1.21+.
    // Defining both overloads (without the annotation) lets the correct one override
    // at runtime on every targeted version.
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (amount != 0) {
            scrollY = Math.max(0, Math.min(maxScroll, scrollY - (int) (amount * 20)));
            applyScroll();
        }
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (vertical != 0) {
            scrollY = Math.max(0, Math.min(maxScroll, scrollY - (int) (vertical * 20)));
            applyScroll();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (sugOpen && !sugCands.isEmpty()) {
            for (int[] r : sugRects) {
                if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) {
                    acceptSuggestion(sugRects.indexOf(r));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (sugField != null && sugOpen && !sugCands.isEmpty()) {
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                acceptSuggestion(sugSel);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                sugSel = (sugSel + 1) % sugCands.size();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                sugSel = (sugSel - 1 + sugCands.size()) % sugCands.size();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                acceptSuggestion(sugSel);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                sugOpen = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void execute(Action a, TextFieldWidget[] fields) {
        List<String> values = new ArrayList<>();
        if (fields != null && fields.length > 0) {
            for (TextFieldWidget tf : fields) {
                String v = tf.getText();
                if (v == null || v.trim().isEmpty()) {
                    BaritoneGuiMod.feedback(Lang.tx1("baritonegui.msg.fillParams", "§c[BaritoneGUI] 请先填写所有参数再执行：%s",
                            Lang.tx(a.key, a.display).getString()).getString());
                    return;
                }
                values.add(v);
            }
        }
        String cmd = a.build(values);
        BaritoneBridge.execute(cmd);
        BaritoneGuiMod.feedback(Lang.tx1("baritonegui.msg.executed", "§a[BaritoneGUI] 执行：%s", cmd).getString());
        // Only the "执行" button (not 切换 / other buttons) honors the
        // "退出GUI" preference, and only when the user has it toggled on.
        if (BaritoneGuiMod.CONFIG.exitAfterExecute) {
            this.close();
        }
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
