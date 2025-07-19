package net.jigokusaru.lootfoundry.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.Base64;

/**
 * A GSON TypeAdapter for ItemStack.
 * This prevents GSON's default reflection-based serialization, which can cause
 * StackOverflowErrors due to the complexity of the ItemStack class.
 * It works by using Minecraft's own network serialization logic (StreamCodec)
 * and encoding the resulting byte array as a Base64 string in the JSON.
 */
public class ItemStackAdapter extends TypeAdapter<ItemStack> {

    private final RegistryAccess registryAccess;

    public ItemStackAdapter(RegistryAccess registryAccess) {
        this.registryAccess = registryAccess;
    }

    @Override
    public void write(JsonWriter out, ItemStack value) throws IOException {
        if (value == null || value.isEmpty()) {
            out.nullValue();
            return;
        }
        // THE FIX: Use RegistryFriendlyByteBuf and provide the registry access.
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), this.registryAccess);
        ItemStack.STREAM_CODEC.encode(buf, value);

        // Read the bytes from the buffer
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);

        // Encode the byte array to a Base64 string and write it to the JSON
        out.value(Base64.getEncoder().encodeToString(bytes));
    }

    @Override
    public ItemStack read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return ItemStack.EMPTY;
        }
        // Read the Base64 string from the JSON
        String base64 = in.nextString();

        // Decode the string back into a byte array
        byte[] bytes = Base64.getDecoder().decode(base64);

        // THE FIX: Use RegistryFriendlyByteBuf for decoding as well.
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), this.registryAccess);
        return ItemStack.STREAM_CODEC.decode(buf);
    }
}