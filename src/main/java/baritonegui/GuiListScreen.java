package baritonegui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import baritonegui.GuiActionPanel;

import java.util.ArrayList;
import java.util.List;

/** Lists configured hotkeys with edit / delete, plus an "add" button. */
public class GuiListScreen extends Screen {

    private final Screen parent;
    private List<Hotkey> hotkeys;
    private int scroll = 0;
    private int maxScroll = 0;

    private static class Region {
        int x, y, w, ht;
        Hotkey hotkey;
        boolean delete;

        Region(int x, int y, int w, int ht, Hotkey hotkey, boolean delete) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.ht = ht;
            this.hotkey = hotkey;
            this.delete = delete;
        }
    }

    private final List<Region> regions = new ArrayList<>();

    public GuiListScreen(Screen parent) {
        super(Lang.tx("baritonegui.screen.hotkeys", "BaritoneGUI - 快捷键管理"));
        this.parent = parent;
        this.hotkeys = new ArrayList<>(HotkeyManager.getHotkeys());
    }

    @Override
    protected void init() {
        int by = this.height - 28;
        this.addDrawableChild(ButtonWidget.builder(Lang.tx("baritonegui.button.add", "+ 添加快捷键"), b -> {
            Hotkey h = new Hotkey(Lang.tx("baritonegui.list.newHotkey", "新快捷键").getString(), -1, false, false, false, "", true);
            this.client.setScreen(new GuiEditScreen(this, h, this.hotkeys, true));
        }).dimensions(this.width / 2 - 205, by, 130, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Lang.tx("baritonegui.button.done", "完成"), b -> this.close())
                .dimensions(this.width / 2 + 75, by, 130, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Lang.tx("baritonegui.button.panel", "操作面板"), b ->
                this.client.setScreen(new GuiActionPanel(this))
        ).dimensions(8, by, 112, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        this.regions.clear();

        int listTop = 46;
        int listBottom = this.height - 40;
        int rowH = 26;
        int left = 20;
        int right = this.width - 36;
        int visible = Math.max(1, (listBottom - listTop) / rowH);

        maxScroll = Math.max(0, this.hotkeys.size() - visible);
        if (scroll < 0) scroll = 0;
        if (scroll > maxScroll) scroll = maxScroll;

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        boolean ba = BaritoneBridge.isAvailable();
        String status = ba ? Lang.tx("baritonegui.status.detected", "已检测到").getString()
                : Lang.tx("baritonegui.status.notDetected", "未检测到(需安装Baritone)").getString();
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Lang.tx1("baritonegui.list.info", "打开界面默认键: B   |   Baritone状态: %s", status),
                this.width / 2, 26, ba ? 0x55FF55 : 0xFFAA55);

        if (this.hotkeys.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Lang.tx("baritonegui.list.empty", "暂无快捷键，点击左下方 “+ 添加快捷键”"),
                    this.width / 2, listTop + 20, 0xAAAAAA);
            return;
        }

        int count = Math.min(visible, this.hotkeys.size() - scroll);
        int cmdLeft = left + 200;
        int cmdMaxW = (right - cmdLeft) - 70;
        for (int i = 0; i < count; i++) {
            int idx = scroll + i;
            Hotkey h = this.hotkeys.get(idx);
            int y = listTop + i * rowH;
            int bg = (i % 2 == 0) ? 0x55222222 : 0x55333333;
            ctx.fill(left, y, right, y + rowH - 2, bg);

            String label = (h.label == null || h.label.isEmpty()) ? Lang.tx("baritonegui.list.unnamed", "(未命名)").getString() : h.label;
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(label), left + 6, y + 4, 0xFFFFFF);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(h.keyComboText()), left + 6, y + 14, 0x66CCFF);

            String cmd = (h.command == null) ? "" : h.command;
            if (this.textRenderer.getWidth(cmd) > cmdMaxW) {
                cmd = this.textRenderer.trimToWidth(cmd, Math.max(10, cmdMaxW - 10)) + "…";
            }
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(cmd), cmdLeft, y + 4, 0xCCCCCC);
            ctx.drawTextWithShadow(this.textRenderer,
                    Lang.tx(h.enabled ? "baritonegui.state.enabled" : "baritonegui.state.disabled",
                            h.enabled ? "启用" : "禁用"),
                    cmdLeft, y + 14,
                    h.enabled ? 0x55FF55 : 0xFF5555);

            int delW = 54;
            int delX = right - delW;
            regions.add(new Region(left, y, right - left - delW, rowH - 2, h, false));
            ctx.fill(delX, y, delX + delW, y + rowH - 2, 0x66333333);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Lang.tx("baritonegui.button.delete", "删除"),
                    delX + delW / 2, y + 7, 0xFF7777);
            regions.add(new Region(delX, y, delW, rowH - 2, h, true));
        }
    }

    // NOTE: no @Override — Screen.mouseScrolled is 3-arg on 1.20.x and 4-arg on 1.21+.
    // Defining both overloads (without the annotation) lets the correct one override at runtime.
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (amount != 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) amount));
        }
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (vertical != 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) vertical));
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        for (Region r : regions) {
            if (mx >= r.x && mx <= r.x + r.w && my >= r.y && my <= r.y + r.ht) {
                if (r.delete) {
                    this.hotkeys.remove(r.hotkey);
                    HotkeyConfig.save(this.hotkeys);
                    HotkeyManager.setHotkeys(this.hotkeys);
                    this.client.setScreen(new GuiListScreen(this.parent));
                } else {
                    this.client.setScreen(new GuiEditScreen(this, r.hotkey, this.hotkeys, false));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
        HotkeyConfig.save(this.hotkeys);
        HotkeyManager.setHotkeys(this.hotkeys);
        this.client.setScreen(this.parent);
    }
}
