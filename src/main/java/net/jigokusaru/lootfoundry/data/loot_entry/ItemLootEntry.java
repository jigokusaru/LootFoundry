package net.jigokusaru.lootfoundry.data.loot_entry;

import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.ui.BuilderType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

    /**
     * --- THIS IS THE FIX ---
     * A new, simplified constructor for creating a display-only entry from a final reward ItemStack.
     * This allows the RewardScreen to easily convert its list of rewards for rendering.
     * @param itemStack The final reward item.
     */
    public ItemLootEntry(ItemStack itemStack) {
        // Call the main constructor with default/dummy values.
        // The UUID is only for editing, so a random one is fine.
        // Weight is irrelevant for display.
        // Min/Max count can just be the stack's actual count.
        this(UUID.randomUUID(), 0, itemStack.copy(), itemStack.getCount(), itemStack.getCount());
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
        buffer.writeVarInt(this.minCount);
        buffer.writeVarInt(this.maxCount);
        ItemStack.STREAM_CODEC.encode(buffer, this.itemStack);
    }

    @Override
    public void execute(ServerPlayer player) {
        // Item rewards are handled directly in LootBagItem, so this is empty.
    }

    @Override
    public ItemStack getIcon() {
        return this.itemStack;
    }

    @Override
    public Component getDisplayName() {
        return this.itemStack.getHoverName();
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