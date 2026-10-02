package baritonegui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Add / edit a single hotkey. Uses toggle buttons instead of CheckboxWidget
 *  because the CheckboxWidget constructor argument order is not stable across
 *  Minecraft versions. */
public class GuiEditScreen extends Screen {

    private final Screen parent;
    private final Hotkey target;
    private final List<Hotkey> list;
    private final boolean isNew;

    /** Working copy so "cancel" discards changes. */
    private final Hotkey draft;

    private TextFieldWidget labelField;
    private TextFieldWidget commandField;
    private ButtonWidget ctrlBtn;
    private ButtonWidget altBtn;
    private ButtonWidget shiftBtn;
    private ButtonWidget enabledBtn;
    private ButtonWidget keyButton;

    private boolean capturing = false;
    private String conflictMsg = "";
    private int conflictColor = 0xCCCCCC;
    private String testMsg = "";
    private int testColor = 0xCCCCCC;

    public GuiEditScreen(Screen parent, Hotkey target, List<Hotkey> list, boolean isNew) {
        super(Lang.tx(isNew ? "baritonegui.screen.addHotkey" : "baritonegui.screen.editHotkey",
                isNew ? "添加快捷键" : "编辑快捷键"));
        this.parent = parent;
        this.target = target;
        this.list = list;
        this.isNew = isNew;
        this.draft = new Hotkey();
        this.draft.id = target.id;
        this.draft.label = target.label;
        this.draft.keyCode = target.keyCode;
        this.draft.ctrl = target.ctrl;
        this.draft.alt = target.alt;
        this.draft.shift = target.shift;
        this.draft.command = target.command;
        this.draft.enabled = target.enabled;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        this.labelField = new TextFieldWidget(this.textRenderer, cx - 200, 50, 400, 20, Lang.tx("baritonegui.edit.labelField", "标签"));
        this.labelField.setText(draft.label == null ? "" : draft.label);
        this.labelField.setMaxLength(64);
        this.addDrawableChild(this.labelField);

        this.commandField = new TextFieldWidget(this.textRenderer, cx - 200, 152, 400, 20, Lang.tx("baritonegui.edit.commandField", "命令"));
        this.commandField.setText(draft.command == null ? "" : draft.command);
        this.commandField.setMaxLength(512);
        this.addDrawableChild(this.commandField);

        int by = 90;
        this.ctrlBtn = ButtonWidget.builder(Lang.tx1("baritonegui.edit.ctrl", "Ctrl: %s", draft.ctrl ? "✔" : "✘"), b -> draft.ctrl = !draft.ctrl)
                .dimensions(cx - 200, by, 70, 20).build();
        this.addDrawableChild(this.ctrlBtn);
        this.altBtn = ButtonWidget.builder(Lang.tx1("baritonegui.edit.alt", "Alt: %s", draft.alt ? "✔" : "✘"), b -> draft.alt = !draft.alt)
                .dimensions(cx - 120, by, 70, 20).build();
        this.addDrawableChild(this.altBtn);
        this.shiftBtn = ButtonWidget.builder(Lang.tx1("baritonegui.edit.shift", "Shift: %s", draft.shift ? "✔" : "✘"), b -> draft.shift = !draft.shift)
                .dimensions(cx - 40, by, 70, 20).build();
        this.addDrawableChild(this.shiftBtn);
        this.enabledBtn = ButtonWidget.builder(Lang.tx1("baritonegui.edit.enabled", "启用: %s", draft.enabled ? "✔" : "✘"), b -> draft.enabled = !draft.enabled)
                .dimensions(cx + 40, by, 90, 20).build();
        this.addDrawableChild(this.enabledBtn);

        keyButton = ButtonWidget.builder(keyButtonText(), b -> startCapture())
                .dimensions(cx - 200, by + 30, 220, 20).build();
        this.addDrawableChild(keyButton);

        this.addDrawableChild(ButtonWidget.builder(Lang.tx("baritonegui.button.clearKey", "清除按键"), b -> {
            draft.keyCode = -1;
            updateKeyButton();
        }).dimensions(cx + 30, by + 30, 110, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Lang.tx("baritonegui.button.test", "测试执行"), b -> testCommand())
                .dimensions(cx - 200, 200, 130, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Lang.tx("baritonegui.button.save", "保存"), b -> save())
                .dimensions(cx - 60, 200, 120, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Lang.tx("baritonegui.button.cancel", "取消"), b -> cancel())
                .dimensions(cx + 80, 200, 120, 20).build());
    }

    private Text keyButtonText() {
        if (capturing) return Lang.tx("baritonegui.edit.capturing", "按下按键… (Esc 取消)");
        return Text.literal(Lang.tx("baritonegui.edit.key", "按键: ").getString() + draft.keyComboText());
    }

    private void updateKeyButton() {
        if (keyButton != null) keyButton.setMessage(keyButtonText());
    }

    private void startCapture() {
        capturing = true;
        if (labelField != null) labelField.setFocused(false);
        if (commandField != null) commandField.setFocused(false);
        updateKeyButton();
    }

    private void recheck() {
        boolean conflict = computeConflict();
        if (conflict) {
            conflictMsg = Lang.tx("baritonegui.edit.conflict", "冲突：已有相同按键组合的快捷键，请修改。").getString();
            conflictColor = 0xFF5555;
        } else if (!draft.isAssigned()) {
            conflictMsg = Lang.tx("baritonegui.edit.notBound", "尚未绑定按键（点击上方按钮进行绑定）。").getString();
            conflictColor = 0xFFAA55;
        } else {
            conflictMsg = Lang.tx("baritonegui.edit.noConflict", "无冲突。").getString();
            conflictColor = 0x55FF55;
        }
    }

    private boolean computeConflict() {
        if (!draft.isAssigned()) return false;
        String combo = draft.keyCode + "|" + draft.ctrl + "|" + draft.alt + "|" + draft.shift;
        for (Hotkey h : list) {
            if (h == target) continue;
            if (!h.isAssigned()) continue;
            String c = h.keyCode + "|" + h.ctrl + "|" + h.alt + "|" + h.shift;
            if (c.equals(combo)) return true;
        }
        if (draft.keyCode == GLFW.GLFW_KEY_B && !draft.ctrl && !draft.alt && !draft.shift) {
            conflictMsg = Lang.tx("baritonegui.edit.warnDefaultB", "警告：与“打开界面”默认键 B 冲突，建议更换。").getString();
            conflictColor = 0xFFAA55;
        }
        return false;
    }

    private void testCommand() {
        if (draft.command == null || draft.command.trim().isEmpty()) {
            testMsg = Lang.tx("baritonegui.edit.pleaseInput", "请先输入命令。").getString();
            testColor = 0xFFAA55;
            return;
        }
        if (!BaritoneBridge.isAvailable()) {
            testMsg = Lang.tx("baritonegui.edit.baritoneMissing", "Baritone 未检测到，无法测试。").getString();
            testColor = 0xFF5555;
            return;
        }
        boolean ok = BaritoneBridge.execute(draft.command);
        testMsg = ok ? Lang.tx1("baritonegui.edit.testOk", "已执行: %s", draft.command).getString()
                : Lang.tx1("baritonegui.edit.testFail", "执行失败: %s", draft.command).getString();
        testColor = ok ? 0x55FF55 : 0xFF5555;
    }

    private void save() {
        if (computeConflict()) {
            testMsg = Lang.tx("baritonegui.edit.saveConflict", "存在冲突，无法保存。").getString();
            testColor = 0xFF5555;
            return;
        }
        target.label = draft.label;
        target.keyCode = draft.keyCode;
        target.ctrl = draft.ctrl;
        target.alt = draft.alt;
        target.shift = draft.shift;
        target.command = draft.command;
        target.enabled = draft.enabled;
        if (isNew) list.add(target);
        HotkeyConfig.save(list);
        HotkeyManager.setHotkeys(list);
        this.client.setScreen(this.parent);
    }

    private void cancel() {
        this.client.setScreen(this.parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturing) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                capturing = false;
                updateKeyButton();
                return true;
            }
            draft.keyCode = keyCode;
            capturing = false;
            updateKeyButton();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);

        // Sync text fields; toggle buttons already maintain draft flags on press.
        if (labelField != null) draft.label = labelField.getText();
        if (commandField != null) draft.command = commandField.getText();
        if (ctrlBtn != null) ctrlBtn.setMessage(Lang.tx1("baritonegui.edit.ctrl", "Ctrl: %s", draft.ctrl ? "✔" : "✘"));
        if (altBtn != null) altBtn.setMessage(Lang.tx1("baritonegui.edit.alt", "Alt: %s", draft.alt ? "✔" : "✘"));
        if (shiftBtn != null) shiftBtn.setMessage(Lang.tx1("baritonegui.edit.shift", "Shift: %s", draft.shift ? "✔" : "✘"));
        if (enabledBtn != null) enabledBtn.setMessage(Lang.tx1("baritonegui.edit.enabled", "启用: %s",
                draft.enabled ? Lang.tx("baritonegui.yes", "是").getString() : Lang.tx("baritonegui.no", "否").getString()));
        recheck();

        int cx = this.width / 2;
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 12, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Lang.tx("baritonegui.edit.labelLabel", "标签 (名称):"), cx - 200, 38, 0xCCCCCC);
        ctx.drawTextWithShadow(this.textRenderer, Lang.tx("baritonegui.edit.modLabel", "修饰键 (点击切换):"), cx - 200, 78, 0xCCCCCC);
        ctx.drawTextWithShadow(this.textRenderer,
                Lang.tx("baritonegui.edit.cmdHint", "Baritone 命令 (不含前缀点, 例如: goto 100 64 100  或  mine diamond_ore):"),
                cx - 200, 138, 0xCCCCCC);

        String line = testMsg != null && !testMsg.isEmpty() ? testMsg : conflictMsg;
        int color = testMsg != null && !testMsg.isEmpty() ? testColor : conflictColor;
        ctx.drawTextWithShadow(this.textRenderer, Text.literal(line), cx - 200, 230, color);
    }
}
