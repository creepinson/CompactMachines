package org.dave.cm2.jei;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.BlankRecipeWrapper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fluids.UniversalBucket;
import org.dave.cm2.init.Fluidss;
import org.dave.cm2.miniaturization.MiniaturizationEvents;
import org.dave.cm2.miniaturization.MultiblockRecipe;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class MultiblockRecipeWrapper extends BlankRecipeWrapper {
    public final MultiblockRecipe recipe;
    private final List<ItemStack> input = new ArrayList<>();
    private int requiredBuckets;

    public MultiblockRecipeWrapper(MultiblockRecipe recipe) {
        this.recipe = recipe;

        // Guess the number of required buckets
        BlockPos minPos = recipe.getMinPos();
        BlockPos maxPos = recipe.getMaxPos();
        int diffX = maxPos.getX() - minPos.getX() + 1;
        int diffZ = maxPos.getZ() - minPos.getZ() + 1;

        int reqX = (int) Math.ceil(diffX / 2.0f);
        int reqZ = (int) Math.ceil(diffZ / 2.0f);
        this.requiredBuckets = reqX * reqZ;

        int added = 0;
        for(ItemStack stack : this.recipe.getRequiredItemStacks()) {
            this.input.add(stack);
            added++;
        }

        for(int emptySlot = 0; emptySlot < 6 - added; emptySlot++) {
            this.input.add(null);
        }

        this.input.add(UniversalBucket.getFilledBucket(ForgeModContainer.getInstance().universalBucket, Fluidss.miniaturizationFluid));
        this.input.add(this.recipe.getCatalystStack());
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputs(ItemStack.class, input);
        ingredients.setOutput(ItemStack.class, this.recipe.getTargetStack());
    }

    public void renderLayer(BlockRendererDispatcher blockrendererdispatcher, VertexBuffer buffer, BlockRenderLayer renderLayer, List<BlockPos> toRender) {
        for (BlockPos pos : toRender) {

            IBlockState state = recipe.getStateAtBlockPos(pos);
            if (!state.getBlock().canRenderInLayer(state, renderLayer)) {
                continue;
            }


            ForgeHooksClient.setRenderLayer(renderLayer);
            try {
                blockrendererdispatcher.renderBlock(state, pos, recipe.getBlockAccess(), buffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ForgeHooksClient.setRenderLayer(null);
        }
    }

    @Override
    public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        BlockPos minPos = recipe.getMinPos();
        BlockPos maxPos = recipe.getMaxPos();

        int reqX = (int) Math.ceil((float)(maxPos.getX() - minPos.getX() +2) / 2.0f);
        int reqZ = (int) Math.ceil((float)(maxPos.getZ() - minPos.getZ() +2) / 2.0f);
        this.requiredBuckets = reqX * reqZ;

        // This was the code used to draw the bucket count previously.
        // At the moment we do not estimate the required amount of buckets, so we can not display anything.
        if(requiredBuckets > 1) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0F, 0F, 216.5F);

            if (requiredBuckets < 10) {
                // TODO: Fontrenderer compat
                mc.fontRenderer.drawStringWithShadow("~" + requiredBuckets, 135+6, 19 * 4 + 10, 0xFFFFFF);
            } else {
                mc.fontRenderer.drawStringWithShadow("~" + requiredBuckets, 135, 19 * 4 + 10, 0xFFFFFF);
            }

            GlStateManager.popMatrix();
        }

        List<BlockPos> toRender = recipe.getShapeAsBlockPosList();
        if(toRender.isEmpty()) {
            return;
        }

        float angle = MiniaturizationEvents.renderTicks * 45.0f / 128.0f;

        // When we want to render translucent blocks we might need this
        //double c = MathHelper.cos((float)(Math.PI * (double)angle / 180.0));
        //double s = MathHelper.sin((float)(Math.PI * (double)angle / 180.0));
        //Collections.sort(toRender, (a,b) -> - Double.compare((double)a.getZ() * c - (double)a.getX() * s, (double)b.getX() * c - (double)b.getX() * s));
        BlockRendererDispatcher blockrendererdispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();

        // Init GlStateManager
        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableFog();
        GlStateManager.disableLighting();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        if (Minecraft.isAmbientOcclusionEnabled()) {
            GlStateManager.shadeModel(7425);
        } else {
            GlStateManager.shadeModel(7424);
        }

        GlStateManager.pushMatrix();

        // Center on recipe area
        GlStateManager.translate((float)(recipeWidth / 2), (float)(recipeHeight / 2), 100.0f);

        // Shift it a bit down so one can properly see 3d
        GlStateManager.rotate(-25.0f, 1.0f, 0.0f, 0.0f);

        // Rotate per our calculated time
        GlStateManager.rotate(angle, 0.0f, 1.0f, 0.0f);

        // Scale down to gui scale
        GlStateManager.scale(16.0f, -16.0f, 16.0f);

        Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer buffer = tessellator.getBuffer();

        // Calculate the maximum size the shape has
        BlockPos mn = recipe.getMinPos();
        BlockPos mx = recipe.getMaxPos();
        int diffX = mx.getX() - mn.getX();
        int diffY = mx.getY() - mn.getY();
        int diffZ = mx.getZ() - mn.getZ();

        // We have big recipes, we need to adjust the size accordingly.
        int maxDiff = Math.max(Math.max(diffZ, diffX), diffY) + 1;
        float scale = 1.0f / ((float)maxDiff / 4.0f);

        GlStateManager.enableCull();
        GlStateManager.scale(scale, scale, scale);

        // Move the shape to the center of the crafting window
        GlStateManager.translate(
            (diffX + 1) / -2.0f,
            (diffY + 1) / -2.0f,
            (diffZ + 1) / -2.0f
        );

        // If the client holds down the shift button, render everything as wireframe
        boolean renderWireframe = false;
        if(GuiScreen.isShiftKeyDown()) {
            renderWireframe = true;
            GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        }

        // Aaaand render
        buffer.begin(7, DefaultVertexFormats.BLOCK);
        GlStateManager.disableAlpha();
        this.renderLayer(blockrendererdispatcher, buffer, BlockRenderLayer.SOLID, toRender);
        GlStateManager.enableAlpha();
        this.renderLayer(blockrendererdispatcher, buffer, BlockRenderLayer.CUTOUT_MIPPED, toRender);
        this.renderLayer(blockrendererdispatcher, buffer, BlockRenderLayer.CUTOUT, toRender);
        GlStateManager.shadeModel(7425);
        this.renderLayer(blockrendererdispatcher, buffer, BlockRenderLayer.TRANSLUCENT, toRender);
        tessellator.draw();

        // Stop wireframe rendering
        if(renderWireframe) {
            GlStateManager.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        }

        GlStateManager.popMatrix();
    }
}
