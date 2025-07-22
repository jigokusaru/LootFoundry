package net.jigokusaru.lootfoundry.item;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.jigokusaru.lootfoundry.data.component.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;

/**
 * A custom item renderer for the Loot Bag.
 * This implementation corrects the transformation order for all contexts.
 */
public class LootBagRenderer extends BlockEntityWithoutLevelRenderer {

    public static final LootBagRenderer INSTANCE = new LootBagRenderer();

    // Standard lighting vectors used by vanilla
    private static final Vector3f DIFFUSE_LIGHT_0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
    private static final Vector3f DIFFUSE_LIGHT_1 = new Vector3f(-0.2F, 1.0F, 0.7F).normalize();
    private static final Vector3f FLAT_LIGHT_0 = new Vector3f(0.0F, 1.0F, 0.0F).normalize();
    private static final Vector3f FLAT_LIGHT_1 = new Vector3f(0.0F, 1.0F, 0.0F).normalize();

    public LootBagRenderer() {
        super(null, null);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        // Pass the whole stack to the getModel method, which now handles all logic.
        BakedModel modelToRender = getModel(mc, stack);

        poseStack.pushPose();

        boolean isGui = displayContext == ItemDisplayContext.GUI || displayContext == ItemDisplayContext.FIXED;
        boolean isLeftHand = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

        // --- YOUR UNIFIED VANILLA PIPELINE IS PRESERVED ---
        poseStack.translate(0.5F, 0.5F, 0.5F);
        modelToRender.applyTransform(displayContext, poseStack, isLeftHand);
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        // Now, manage lighting and render the model
        if (isGui) {
            if (modelToRender.isGui3d()) {
                RenderSystem.setupGuiFlatDiffuseLighting(FLAT_LIGHT_0, FLAT_LIGHT_1);
                renderModel(itemRenderer, modelToRender, stack, LightTexture.FULL_BRIGHT, packedOverlay, poseStack, buffer);
            } else {
                RenderSystem.setupGui3DDiffuseLighting(DIFFUSE_LIGHT_0, DIFFUSE_LIGHT_1);
                renderModel(itemRenderer, modelToRender, stack, LightTexture.FULL_BRIGHT, packedOverlay, poseStack, buffer);
                RenderSystem.setupGuiFlatDiffuseLighting(FLAT_LIGHT_0, FLAT_LIGHT_1);
            }
        } else {
            renderModel(itemRenderer, modelToRender, stack, packedLight, packedOverlay, poseStack, buffer);
        }

        poseStack.popPose();
    }

    private void renderModel(ItemRenderer itemRenderer, BakedModel model, ItemStack stack, int packedLight, int packedOverlay, PoseStack poseStack, MultiBufferSource buffer) {
        for (RenderType renderType : model.getRenderTypes(stack, true)) {
            VertexConsumer vertexConsumer = ItemRenderer.getFoilBufferDirect(buffer, renderType, true, stack.hasFoil());
            itemRenderer.renderModelLists(model, stack, packedLight, packedOverlay, poseStack, vertexConsumer);
        }
    }

    /**
     * Retrieves the correct BakedModel for the ItemStack.
     * This is the definitive logic that handles our custom models and falls back to a barrier for all errors.
     */
    private BakedModel getModel(Minecraft mc, ItemStack stack) {
        ModelManager modelManager = mc.getModelManager();
        BakedModel barrierModel = mc.getItemRenderer().getModel(new ItemStack(Items.BARRIER), null, null, 0);

        // 1. Get the custom model ID from the stack.
        String modelIdStr = stack.get(ModDataComponents.CUSTOM_MODEL_ID.get());

        // 2. If no ID is present, render the default loot bag.
        if (modelIdStr == null || modelIdStr.isBlank()) {
            return modelManager.getModel(new ModelResourceLocation(ModItems.LOOT_BAG.getId(), "inventory"));
        }

        // 3. An ID is present, try to parse and load it.
        ResourceLocation location = ResourceLocation.tryParse(modelIdStr);

        // 4. If the ID string is malformed, return the barrier.
        if (location == null) {
            return barrierModel;
        }

        // 5. The ID is valid. Attempt to load the corresponding model.
        BakedModel modelToRender;

        // It's one of our custom loot bag models, so we MUST use the "standalone" variant.
        if (location.getPath().startsWith("lootfoundry/item/")) {
            ModelResourceLocation mrl = new ModelResourceLocation(location, "standalone");
            modelToRender = modelManager.getModel(mrl);
        } else {
            // It's a different item (e.g., "minecraft:stone"). Try to load its inventory model for preview.
            ModelResourceLocation mrl = new ModelResourceLocation(location, "inventory");
            modelToRender = modelManager.getModel(mrl);
        }

        // 6. If the model manager couldn't find the model, it returns the missing model.
        // We'll return the barrier instead for a consistent error display.
        if (modelToRender == modelManager.getMissingModel()) {
            return barrierModel;
        }

        return modelToRender;
    }
}