package baritonegui.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures Baritone's chat lines so the toggle button can surface the latest
 * hint without relying on reading Baritone's internal settings state (which is
 * brittle across Baritone / Minecraft versions).
 *
 * <p>Baritone prefixes every chat line with "[Baritone]"; our own
 * "[BaritoneGUI]" feedback must NOT match, hence the exact prefix check.
 *
 * <p>The injectors target several {@code addMessage} overloads with
 * {@code require = 0} so that, on any Minecraft version where a given overload
 * does not exist, the mixin simply skips it instead of failing to apply. At
 * least one overload is present on every supported version (1.20 … 26.x).
 */
@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), require = 0)
    private void onAddMessageText(Text message, CallbackInfo ci) {
        capture(message);
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;I)V", at = @At("HEAD"), require = 0)
    private void onAddMessageTextId(Text message, int messageId, CallbackInfo ci) {
        capture(message);
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;IZ)V", at = @At("HEAD"), require = 0)
    private void onAddMessageTextIdRefresh(Text message, int messageId, boolean refresh, CallbackInfo ci) {
        capture(message);
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), require = 0)
    private void onAddMessageTextIndicator(Text message, MessageIndicator indicator, CallbackInfo ci) {
        capture(message);
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;ILnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), require = 0)
    private void onAddMessageTextIdIndicator(Text message, int messageId, MessageIndicator indicator, CallbackInfo ci) {
        capture(message);
    }

    private static void capture(Text message) {
        if (message == null) return;
        String s = message.getString();
        if (s == null) return;
        if (s.contains("[Baritone]")) {
            baritonegui.ChatCapture.onBaritoneMessage(s);
        }
    }
}
