package net.jigokusaru.lootfoundry.util;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * A GSON TypeAdapterFactory for java.util.Optional. This allows GSON to
 * correctly serialize and deserialize Optional objects without resorting to
 * reflection on its private fields, which is blocked by modern Java versions.
 */
public class OptionalTypeAdapter<E> extends TypeAdapter<Optional<E>> {

    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (type.getRawType() != Optional.class) {
                return null;
            }
            final ParameterizedType parameterizedType = (ParameterizedType) type.getType();
            final Type actualType = parameterizedType.getActualTypeArguments()[0];
            final TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(actualType));
            return (TypeAdapter<T>) new OptionalTypeAdapter(adapter);
        }
    };

    private final TypeAdapter<E> adapter;

    public OptionalTypeAdapter(TypeAdapter<E> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void write(JsonWriter out, Optional<E> value) throws IOException {
        if (value != null && value.isPresent()) {
            adapter.write(out, value.get());
        } else {
            out.nullValue();
        }
    }

    @Override
    public Optional<E> read(JsonReader in) throws IOException {
        return Optional.ofNullable(adapter.read(in));
    }
}