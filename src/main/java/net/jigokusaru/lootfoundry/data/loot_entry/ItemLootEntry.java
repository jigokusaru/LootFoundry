package net.jigokusaru.lootfoundry.data.loot_entry;

import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.ui.BuilderType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class ItemLootEntry extends LootEntry {
    private final ItemStack itemStack;
    private final int minCount;
    private final int maxCount;

    public ItemLootEntry(UUID id, int weight, ItemStack itemStack, int minCount, int maxCount) {
        super(id, weight, BuilderType.ITEM);
        this.itemStack = itemStack;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public int getMinCount() {
        return this.minCount;
    }

    public int getMaxCount() {
        return this.maxCount;
    }

    @Override
    public void writeToBuffer(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.type);
        buffer.writeUUID(this.id);
        buffer.writeInt(this.weight);
        // We also need to write the count, since that's a property of the item entry now
        buffer.writeVarInt(this.minCount);
        buffer.writeVarInt(this.maxCount);
        ItemStack.STREAM_CODEC.encode(buffer, this.itemStack);
    }

    public static ItemLootEntry readFromBuffer(RegistryFriendlyByteBuf buffer) {
        // The type has already been read by the parent fromBuffer method
        UUID id = buffer.readUUID();
        int weight = buffer.readInt();
        int minCount = buffer.readVarInt();
        int maxCount = buffer.readVarInt();
        ItemStack stack = ItemStack.STREAM_CODEC.decode(buffer);

        return new ItemLootEntry(id, weight, stack, minCount, maxCount);
    }
}