package fr.elias.extraTools.utils.gson;

import com.google.gson.*;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Type;

public class EffectTypeAdapter implements JsonSerializer<PotionEffectType>, JsonDeserializer<PotionEffectType> {

    @Override
    public PotionEffectType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {
        String s = json.getAsString();

        // Try modern namespaced key first (e.g. "speed")
        PotionEffectType t = PotionEffectType.getByKey(NamespacedKey.minecraft(s));
        if (t != null) return t;

        // Fallback to legacy Bukkit name
        t = PotionEffectType.getByName(s);
        if (t != null) return t;

        throw new JsonParseException("Unknown PotionEffectType: " + s);
    }

    @Override
    public JsonElement serialize(PotionEffectType src, Type typeOfSrc, JsonSerializationContext ctx) {
        // Prefer modern key ("speed"), fallback to legacy getName()
        String out = (src.getKey() != null) ? src.getKey().getKey() : src.getName();
        return new JsonPrimitive(out);
    }
}
