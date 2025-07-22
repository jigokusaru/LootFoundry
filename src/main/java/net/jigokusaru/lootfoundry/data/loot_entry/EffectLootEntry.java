package net.jigokusaru.lootfoundry.data.loot_entry;

import net.jigokusaru.lootfoundry.data.LootEntry;
import net.jigokusaru.lootfoundry.ui.BuilderType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.List;
import java.util.Optional;
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

    @Override
    public ItemStack getIcon() {
        ItemStack potion = new ItemStack(Items.POTION);
        ResourceKey<MobEffect> effectKey = ResourceKey.create(Registries.MOB_EFFECT, this.effectId);

        BuiltInRegistries.MOB_EFFECT.getHolder(effectKey).ifPresent(effectHolder -> {
            // In modern versions, potion effects are stored in a data component.
            // Create a list containing our single custom effect.
            List<MobEffectInstance> customEffects = List.of(new MobEffectInstance(effectHolder, this.duration, this.amplifier));

            // THE FIX: Use the correct PotionContents constructor.
            // We provide empty optionals for the base potion and custom color to signify a purely custom potion.
            PotionContents potionContents = new PotionContents(Optional.empty(), Optional.empty(), customEffects);

            // Apply the PotionContents to the ItemStack using the POTION_CONTENTS data component.
            // This automatically handles the color and glint.
            potion.set(DataComponents.POTION_CONTENTS, potionContents);
        });
        return potion;
    }

    @Override
    public Component getDisplayName() {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(this.effectId);
        if (effect != null) {
            return effect.getDisplayName();
        }
        return Component.literal("Unknown Effect").withStyle(ChatFormatting.RED);
    }

    @Override
    public void execute(ServerPlayer player) {
        ResourceKey<MobEffect> effectKey = ResourceKey.create(Registries.MOB_EFFECT, this.getEffectId());
        BuiltInRegistries.MOB_EFFECT.getHolder(effectKey).ifPresent(effectHolder -> {
            player.addEffect(new MobEffectInstance(effectHolder, this.getDuration(), this.getAmplifier()));
        });
    }

    public static EffectLootEntry readFromBuffer(RegistryFriendlyByteBuf buffer) {
        UUID id = buffer.readUUID();
        int weight = buffer.readInt();
        ResourceLocation effectId = buffer.readResourceLocation();
        int duration = buffer.readVarInt();
        int amplifier = buffer.readVarInt();
        return new EffectLootEntry(id, weight, effectId, duration, amplifier);
    }
}