package net.jigokusaru.lootfoundry.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.CommandLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.EffectLootEntry;
import net.jigokusaru.lootfoundry.data.loot_entry.ItemLootEntry;
import net.jigokusaru.lootfoundry.ui.BuilderType;

import java.io.IOException;

/**
 * A GSON TypeAdapterFactory to handle the serialization and deserialization
 * of the abstract LootEntry class and its concrete subclasses. This pattern
 * is necessary to handle polymorphism correctly and avoid infinite recursion.
 */
public class LootEntryAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() != LootEntry.class) {
            return null;
        }

        return (TypeAdapter<T>) new TypeAdapter<LootEntry>() {
            @Override
            public void write(JsonWriter out, LootEntry value) throws IOException {
                Class<? extends LootEntry> runtimeType = value.getClass();

                TypeAdapter<LootEntry> delegate = (TypeAdapter<LootEntry>) gson.getDelegateAdapter(LootEntryAdapterFactory.this, TypeToken.get(runtimeType));

                JsonObject wrapper = new JsonObject();
                wrapper.addProperty("type", value.getType().name());
                wrapper.add("data", delegate.toJsonTree(value));

                gson.getAdapter(JsonObject.class).write(out, wrapper);
            }

            @Override
            public LootEntry read(JsonReader in) throws IOException {
                JsonObject wrapper = gson.getAdapter(JsonObject.class).read(in).getAsJsonObject();
                if (wrapper == null || wrapper.isJsonNull()) {
                    return null;
                }

                JsonElement typeElement = wrapper.get("type");
                if (typeElement == null || typeElement.isJsonNull()) {
                    throw new JsonParseException("LootEntry is missing the 'type' field in JSON");
                }
                String typeName = typeElement.getAsString();

                JsonElement dataElement = wrapper.get("data");
                if (dataElement == null || dataElement.isJsonNull()) {
                    throw new JsonParseException("LootEntry is missing the 'data' field in JSON");
                }

                // --- THE ROBUST FIX ---
                // This makes the loading process backward-compatible.
                // If we are loading an old file that is missing the inner "type" field,
                // we add it back into the JSON object before deserializing.
                // This prevents the field from being null in the final object.
                JsonObject dataObject = dataElement.getAsJsonObject();
                if (!dataObject.has("type")) {
                    dataObject.addProperty("type", typeName);
                }
                // --- END OF FIX ---

                Class<?> concreteClass = switch (BuilderType.valueOf(typeName)) {
                    case ITEM -> ItemLootEntry.class;
                    case EFFECT -> EffectLootEntry.class;
                    case COMMAND -> CommandLootEntry.class;
                };

                // Now, deserialize from the (potentially modified) data object.
                return (LootEntry) gson.fromJson(dataObject, concreteClass);
            }
        }.nullSafe();
    }
}