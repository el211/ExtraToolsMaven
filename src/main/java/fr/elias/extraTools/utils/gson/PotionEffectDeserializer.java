package fr.elias.extraTools.utils.gson;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Type;

public class PotionEffectDeserializer implements JsonDeserializer<PotionEffect> {

    @Override
    public PotionEffect deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        JsonObject obj = json.getAsJsonObject();
        PotionEffectType effectType = context.deserialize(obj.get("type"), PotionEffectType.class); // Use EffectTypeSerializer
        int amplifier = obj.get("amplifier").getAsInt();
        int duration = obj.get("duration").getAsInt();

        // Handle unlimited duration
        if (duration == -1 || duration == 2147483647) {
            duration = Integer.MAX_VALUE;
        }

        return new PotionEffect(effectType, duration, amplifier);
    }
}


