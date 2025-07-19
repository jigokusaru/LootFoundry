package net.jigokusaru.lootfoundry.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.jigokusaru.lootfoundry.LootFoundry;
import net.jigokusaru.lootfoundry.data.component.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

public class LootBagRenderer extends BlockEntityWithoutLevelRenderer {

    public static final LootBagRenderer INSTANCE = new LootBagRenderer();

    public LootBagRenderer() {
        super(null, null);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // 1. Determine which item we should be rendering.
        // This has the crucial fix to prevent infinite recursion.
        ItemStack stackToRender = this.getModelStack(stack);

        if (stackToRender.isEmpty()) {
            return;
        }

        // 2. Get the vanilla item renderer and the model for the item we want to display.
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel modelToRender = itemRenderer.getModel(stackToRender, null, null, 0);

        // --- THE FINAL, SIMPLIFIED RENDER LOGIC ---
        // After fixing the recursion bug, we can return to the simplest, most reliable
        // rendering method. This high-level call lets vanilla handle all the complex
        // buffer and render type logic internally, which is much less error-prone.
        itemRenderer.render(stackToRender, displayContext, false, poseStack, buffer, packedLight, packedOverlay, modelToRender);
    }

    /**
     * Reads the custom model ID from the loot bag's data components and returns
     * an ItemStack representing the model to be rendered.
     */
    private ItemStack getModelStack(ItemStack lootBagStack) {
        String modelId = lootBagStack.get(ModDataComponents.CUSTOM_MODEL_ID.get());

        // CRITICAL FIX: Fallback to a default item to prevent infinite recursion.
        if (modelId == null || modelId.isBlank()) {
            return new ItemStack(Items.CHEST);
        }

        ResourceLocation location = ResourceLocation.tryParse(modelId);
        if (location == null) {
            LootFoundry.LOGGER.warn("[Renderer] Invalid model ID format on loot bag: {}", modelId);
            return new ItemStack(Items.BARRIER);
        }

        Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(location);
        if (itemOpt.isEmpty()) {
            LootFoundry.LOGGER.warn("[Renderer] Could not find item for model ID: {}", modelId);
            return new ItemStack(Items.BARRIER);
        }

        return new ItemStack(itemOpt.get());
    }
}