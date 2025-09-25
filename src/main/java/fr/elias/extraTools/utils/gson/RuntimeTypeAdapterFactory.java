package fr.elias.extraTools.utils.gson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Polymorphic (de)serialization helper for Gson.
 * Stores a discriminator field (default: "type") in JSON.
 */
public final class RuntimeTypeAdapterFactory<T> implements TypeAdapterFactory {

    private final Class<?> baseType;
    private final String typeFieldName;
    private final boolean maintainTypeField;

    private final Map<String, Class<?>> labelToSubtype = new LinkedHashMap<>();
    private final Map<Class<?>, String> subtypeToLabel = new LinkedHashMap<>();

    private RuntimeTypeAdapterFactory(Class<?> baseType, String typeFieldName, boolean maintainTypeField) {
        if (baseType == null) throw new NullPointerException("baseType == null");
        if (typeFieldName == null) throw new NullPointerException("typeFieldName == null");
        this.baseType = baseType;
        this.typeFieldName = typeFieldName;
        this.maintainTypeField = maintainTypeField;
    }

    /** Use a custom discriminator field name and choose whether to keep it when reading. */
    public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType, String typeFieldName, boolean maintainTypeField) {
        return new RuntimeTypeAdapterFactory<>(baseType, typeFieldName, maintainTypeField);
    }

    /** Use a custom discriminator field name (removed when reading). */
    public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType, String typeFieldName) {
        return new RuntimeTypeAdapterFactory<>(baseType, typeFieldName, false);
    }

    /** Use "type" as the discriminator field (removed when reading). */
    public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType) {
        return new RuntimeTypeAdapterFactory<>(baseType, "type", false);
    }

    /** Register a subtype with a specific label. */
    public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> type, String label) {
        if (type == null) throw new NullPointerException("type == null");
        if (label == null) throw new NullPointerException("label == null");
        if (subtypeToLabel.containsKey(type) || labelToSubtype.containsKey(label)) {
            throw new IllegalArgumentException("types and labels must be unique");
        }
        labelToSubtype.put(label, type);
        subtypeToLabel.put(type, label);
        return this;
    }

    /** Register a subtype using its simple class name as the label. */
    public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> type) {
        return registerSubtype(type, type.getSimpleName());
    }

    @Override
    public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> type) {
        if (type.getRawType() != baseType) {
            return null;
        }

        final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
        final Map<String, TypeAdapter<?>> labelToDelegate = new LinkedHashMap<>();
        final Map<Class<?>, TypeAdapter<?>> subtypeToDelegate = new LinkedHashMap<>();

        for (Map.Entry<String, Class<?>> entry : labelToSubtype.entrySet()) {
            TypeAdapter<?> delegate = gson.getDelegateAdapter(this, TypeToken.get(entry.getValue()));
            labelToDelegate.put(entry.getKey(), delegate);
            subtypeToDelegate.put(entry.getValue(), delegate);
        }

        return new TypeAdapter<R>() {
            @Override
            public void write(JsonWriter out, R value) throws IOException {
                Class<?> srcType = value.getClass();
                String label = subtypeToLabel.get(srcType);
                @SuppressWarnings("unchecked")
                TypeAdapter<R> delegate = (TypeAdapter<R>) subtypeToDelegate.get(srcType);
                if (delegate == null) {
                    throw new JsonParseException("Cannot serialize " + srcType.getName()
                            + "; did you forget to register a subtype?");
                }

                JsonObject jsonObject = delegate.toJsonTree(value).getAsJsonObject();

                if (maintainTypeField) {
                    // If we keep the field, ensure we don't double-write.
                    if (jsonObject.has(typeFieldName)) {
                        // already present; trust the POJO
                        elementAdapter.write(out, jsonObject);
                        return;
                    }
                } else {
                    // If we don't keep the field from the POJO, ensure it's not present.
                    if (jsonObject.has(typeFieldName)) {
                        throw new JsonParseException("Cannot serialize " + srcType.getName()
                                + " because it already defines a field named " + typeFieldName);
                    }
                }

                JsonObject clone = new JsonObject();
                clone.add(typeFieldName, new JsonPrimitive(label));
                for (Map.Entry<String, JsonElement> e : jsonObject.entrySet()) {
                    clone.add(e.getKey(), e.getValue());
                }
                elementAdapter.write(out, clone);
            }

            @Override
            public R read(JsonReader in) throws IOException {
                JsonElement element = elementAdapter.read(in);
                JsonObject jsonObject = element.getAsJsonObject();

                JsonElement labelElement = maintainTypeField
                        ? jsonObject.get(typeFieldName)
                        : jsonObject.remove(typeFieldName);

                if (labelElement == null) {
                    throw new JsonParseException("Cannot deserialize " + baseType.getName()
                            + " because it does not define a field named " + typeFieldName);
                }

                String label = labelElement.getAsString();
                @SuppressWarnings("unchecked")
                TypeAdapter<R> delegate = (TypeAdapter<R>) labelToDelegate.get(label);
                if (delegate == null) {
                    throw new JsonParseException("Cannot deserialize " + baseType.getName()
                            + " subtype named " + label + "; did you forget to register a subtype?");
                }

                return delegate.fromJsonTree(jsonObject);
            }
        }.nullSafe();
    }
}
