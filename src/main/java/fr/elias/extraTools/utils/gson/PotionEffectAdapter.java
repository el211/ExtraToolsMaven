package fr.elias.extraTools.utils.gson;


import com.google.gson.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Type;

public class PotionEffectAdapter implements JsonSerializer<PotionEffect>, JsonDeserializer<PotionEffect> {

    @Override
    public JsonElement serialize(PotionEffect src, Type typeOfSrc, JsonSerializationContext ctx) {
        JsonObject obj = new JsonObject();
        obj.add("type", ctx.serialize(src.getType(), PotionEffectType.class));
        obj.addProperty("amplifier", src.getAmplifier());
        obj.addProperty("duration", src.getDuration() == Integer.MAX_VALUE ? Integer.MAX_VALUE : src.getDuration());
        obj.addProperty("ambient", src.isAmbient());
        obj.addProperty("particles", src.hasParticles());
        obj.addProperty("icon", src.hasIcon());
        return obj;
    }

    @Override
    public PotionEffect deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        PotionEffectType effectType = ctx.deserialize(obj.get("type"), PotionEffectType.class);
        int amplifier = obj.get("amplifier").getAsInt();
        int duration = obj.get("duration").getAsInt();

        if (duration == -1 || duration == Integer.MAX_VALUE) {
            duration = Integer.MAX_VALUE;
        }

        boolean ambient   = obj.has("ambient")   && obj.get("ambient").getAsBoolean();
        boolean particles = obj.has("particles") && obj.get("particles").getAsBoolean();
        boolean icon      = obj.has("icon")      && obj.get("icon").getAsBoolean();

        return new PotionEffect(effectType, duration, amplifier, ambient, particles, icon);
    }
}

