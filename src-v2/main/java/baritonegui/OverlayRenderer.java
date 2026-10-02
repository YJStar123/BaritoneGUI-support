package baritonegui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Draws a translucent black block with white text, centered near the top of the
 * screen. Used to surface Baritone's latest hint after a toggle is pressed.
 */
public final class OverlayRenderer {

    private OverlayRenderer() {
    }

    public static void draw(DrawContext ctx, String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        // Strip Minecraft color codes (§x) so they don't render as literal text.
        String clean = (text == null) ? "" : text.replaceAll("§.", "");
        int sw = mc.getWindow().getScaledWidth();
        int textW = mc.textRenderer.getWidth(clean);
        int padX = 14;
        int padY = 8;
        int boxW = Math.max(textW + padX * 2, 40);
        int boxH = mc.textRenderer.fontHeight + padY * 2;
        int x = (sw - boxW) / 2;
        int y = 36;
        // Near-opaque black block with a bright border so it stands out on the
        // (often dark) game screen.
        ctx.fill(x, y, x + boxW, y + boxH, 0xEE000000);
        // Draw a 1px white border manually (DrawContext.drawBorder is not stable
        // across the 1.21.x generations; fill is).
        ctx.fill(x, y, x + boxW, y + 1, 0xFFFFFFFF);
        ctx.fill(x, y + boxH - 1, x + boxW, y + boxH, 0xFFFFFFFF);
        ctx.fill(x, y, x + 1, y + boxH, 0xFFFFFFFF);
        ctx.fill(x + boxW - 1, y, x + boxW, y + boxH, 0xFFFFFFFF);
        if (!clean.isEmpty()) {
            ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(clean), sw / 2, y + padY, 0xFFFFFFFF);
        }
    }
}
