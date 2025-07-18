package net.jigokusaru.lootfoundry.data.loot_entry;

import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.ui.BuilderType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class EffectLootEntry extends LootEntry {
    private final ResourceLocation effectId;
    private final int duration;
    private final int amplifier;

    public EffectLootEntry(UUID id, int weight, ResourceLocation effectId, int duration, int amplifier) {
        super(id, weight, BuilderType.EFFECT);
        this.effectId = effectId;
        this.duration = duration;
        this.amplifier = amplifier;
    }

    public ResourceLocation getEffectId() { return this.effectId; }
    public int getDuration() { return this.duration; }
    public int getAmplifier() { return this.amplifier; }

    @Override
    public void writeToBuffer(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.type);
        buffer.writeUUID(this.id);
        buffer.writeInt(this.weight);
        buffer.writeResourceLocation(this.effectId);
        buffer.writeVarInt(this.duration);
        buffer.writeVarInt(this.amplifier);
    }

    public static EffectLootEntry readFromBuffer(RegistryFriendlyByteBuf buffer) {
        // The type has already been read by the parent fromBuffer method
        UUID id = buffer.readUUID();
        int weight = buffer.readInt();
        ResourceLocation effectId = buffer.readResourceLocation();
        int duration = buffer.readVarInt();
        int amplifier = buffer.readVarInt();
        return new EffectLootEntry(id, weight, effectId, duration, amplifier);
    }
}