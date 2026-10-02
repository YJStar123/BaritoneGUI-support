package baritonegui;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;

/**
 * Small i18n helper. All user-facing strings in the mod go through here so they
 * can be translated via the standard Minecraft language files
 * ({@code assets/baritonegui/lang/*.json}).
 *
 * <p>{@link #tx(String, String)} falls back to a literal (the original text) when
 * the key is missing. This is what lets the runtime-generated Baritone settings
 * actions (whose ids like "set_allowBreak" have no translation entry) keep showing
 * their raw English setting name instead of an untranslated key.
 */
public final class Lang {

    private Lang() {
    }

    /** Translatable text, falling back to a literal if the key is absent. */
    public static Text tx(String key, String fallback) {
        return I18n.hasTranslation(key) ? Text.translatable(key) : Text.literal(fallback);
    }

    /** Translatable text with one %s argument and a literal fallback. */
    public static Text tx1(String key, String fallback, Object a) {
        if (I18n.hasTranslation(key)) return Text.translatable(key, a);
        return Text.literal(String.format(fallback, a));
    }

    /** Translatable text with two %s arguments and a literal fallback. */
    public static Text tx2(String key, String fallback, Object a, Object b) {
        if (I18n.hasTranslation(key)) return Text.translatable(key, a, b);
        return Text.literal(String.format(fallback, a, b));
    }
}
