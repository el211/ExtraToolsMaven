package fr.elias.extraTools.utils.gson;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.bukkit.potion.PotionEffect;
import java.lang.reflect.Type;

public class PotionEffectSerializer implements JsonSerializer<PotionEffect> {

    @Override
    public JsonElement serialize(PotionEffect src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.add("type", context.serialize(src.getType())); // Use EffectTypeSerializer for type
        obj.addProperty("amplifier", src.getAmplifier());
        obj.addProperty("duration", src.getDuration() == Integer.MAX_VALUE ? 2147483647 : src.getDuration());
        return obj;
    }
}
