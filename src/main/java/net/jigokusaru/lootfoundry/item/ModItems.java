package net.jigokusaru.lootfoundry.item;

import net.jigokusaru.lootfoundry.LootFoundry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, LootFoundry.MODID);

    // This is now a simple item registration.
    // The renderer is attached in LootBagItem.java, which is the correct, stable approach.
    public static final DeferredHolder<Item, Item> LOOT_BAG = ITEMS.register("loot_bag",
            () -> new LootBagItem(new Item.Properties())
    );
}