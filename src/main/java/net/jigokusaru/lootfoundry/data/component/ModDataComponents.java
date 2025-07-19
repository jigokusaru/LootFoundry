package net.jigokusaru.lootfoundry.data.component;

import com.mojang.serialization.Codec;
import net.jigokusaru.lootfoundry.LootFoundry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

/**
 * Registers all custom data components for the mod.
 */
public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, LootFoundry.MODID);

    /**
     * A component to store the unique ID of a loot bag on its ItemStack.
     * This is what links a physical item to its definition file.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BAG_ID =
            COMPONENTS.register("bag_id", () ->
                    createComponent(builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8))
            );

    /**
     * A component to store an item ID string, used to dynamically select a model for the loot bag.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CUSTOM_MODEL_ID =
            COMPONENTS.register("custom_model_id", () ->
                    createComponent(builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8))
            );


    private static <T> DataComponentType<T> createComponent(UnaryOperator<DataComponentType.Builder<T>> builder) {
        return builder.apply(DataComponentType.builder()).build();
    }
}