package fr.elias.extraTools.utils.config;

import com.google.common.collect.Lists;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Msgs {
    // Parsers/serializers
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SEC = LegacyComponentSerializer.legacySection();

    // &-code pattern
    private static final Pattern LEGACY_AMP = Pattern.compile("&([0-9a-fk-or])", Pattern.CASE_INSENSITIVE);

    private final Map<String, String> placeholders = new HashMap<>();
    private final String message;

    public static Msgs of(String message) { return new Msgs(message); }
    public static Msgs of(Component component) { return new Msgs(component); }

    private Msgs(Component component) {
        // store as legacy text so it can be re-parsed anywhere
        this.message = SEC.serialize(component);
    }

    private Msgs(String message) {
        if (Cfgs.of().get().isList(message)) {
            List<String> list = Cfgs.of().get(message, Lists.newArrayList());
            this.message = String.join("\n", list);
        } else {
            this.message = Cfgs.of().get(message, message);
        }
    }

    public Msgs vars(Map<String, String> placeholders) { this.placeholders.putAll(placeholders); return this; }
    public Msgs var(String name, Object value) { placeholders.put(name, String.valueOf(value)); return this; }

    public String build() {
        return StrSubstitutor.replace(message, placeholders, "%", "%");
    }

    public List<Component> asList() {
        String[] arr = build().split("\n");
        return Arrays.stream(arr).map(Msgs::of).map(Msgs::asComp).collect(Collectors.toList());
    }

    public List<String> asStringList() {
        String[] arr = build().split("\n");
        return Arrays.stream(arr).collect(Collectors.toList());
    }

    /** Returns a Component. Accepts MiniMessage and/or &-codes in the same string. */
    public Component asComp() {
        String s = build();

        // If there are any § codes, normalize them to &
        s = s.replace('§', '&');

        // Convert &-codes to MiniMessage tags, keeping any existing <tags> as-is
        String mmReady = ampersandToMiniMessage(s);

        try {
            return MM.deserialize(mmReady).decoration(TextDecoration.ITALIC, false);
        } catch (Exception ignored) {
            // Fallbacks if something in MiniMessage fails
            if (s.indexOf('&') >= 0) return AMP.deserialize(s).decoration(TextDecoration.ITALIC, false);
            return SEC.deserialize(s).decoration(TextDecoration.ITALIC, false);
        }
    }

    /** Plain text (no colors) */
    public String asPlain() {
        return PlainTextComponentSerializer.plainText().serialize(asComp());
    }

    /** §-colored legacy string (use this for inventory titles, etc.) */
    public String asLegacy() {
        return SEC.serialize(asComp());
    }

    /** Back-compat alias used in your codebase */
    public String buildLegacy() { return asLegacy(); }

    public void send(CommandSender audience) {
        if (audience != null) audience.sendMessage(asLegacy());
    }

    // ---- helpers ----

    private static String ampersandToMiniMessage(String in) {
        Matcher m = LEGACY_AMP.matcher(in);
        StringBuffer out = new StringBuffer();

        while (m.find()) {
            String code = m.group(1).toLowerCase();
            String repl = switch (code) {
                // colors
                case "0" -> "<black>";
                case "1" -> "<dark_blue>";
                case "2" -> "<dark_green>";
                case "3" -> "<dark_aqua>";
                case "4" -> "<dark_red>";
                case "5" -> "<dark_purple>";
                case "6" -> "<gold>";
                case "7" -> "<gray>";
                case "8" -> "<dark_gray>";
                case "9" -> "<blue>";
                case "a" -> "<green>";
                case "b" -> "<aqua>";
                case "c" -> "<red>";
                case "d" -> "<light_purple>";
                case "e" -> "<yellow>";
                case "f" -> "<white>";
                // formats
                case "l" -> "<bold>";
                case "o" -> "<italic>";
                case "n" -> "<underlined>";
                case "m" -> "<strikethrough>";
                case "k" -> "<obfuscated>";
                case "r" -> "<reset>";
                default -> "";
            };
            m.appendReplacement(out, Matcher.quoteReplacement(repl));
        }
        m.appendTail(out);
        return out.toString();
    }
}
