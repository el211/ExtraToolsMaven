package fr.elias.extraTools.utils.gson;

import com.google.gson.*;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Type;

public class EffectTypeSerializer implements JsonSerializer<PotionEffectType>, JsonDeserializer<PotionEffectType> {

    @Override
    public PotionEffectType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {
        String s = json.getAsJsonPrimitive().getAsString();

        // Try modern namespaced key first (e.g., "speed")
        PotionEffectType t = PotionEffectType.getByKey(NamespacedKey.minecraft(s));
        if (t != null) return t;

        // Fallback to legacy Bukkit name (e.g., "SPEED")
        t = PotionEffectType.getByName(s);
        if (t != null) return t;

        throw new JsonParseException("Unknown PotionEffectType: " + s);
    }

    @Override
    public JsonElement serialize(PotionEffectType src, Type typeOfSrc, JsonSerializationContext ctx) {
        // Prefer modern key ("speed"), fallback to legacy name
        String out = (src.getKey() != null) ? src.getKey().getKey() : src.getName();
        return new JsonPrimitive(out);
    }
}
