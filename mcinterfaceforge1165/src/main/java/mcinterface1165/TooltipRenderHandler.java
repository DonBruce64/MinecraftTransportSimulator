package mcinterface1165;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.rendering.TooltipTextureMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.gui.GuiUtils;

/** Renders per-pack tooltip textures while retaining vanilla fallbacks for omitted layers. */
public class TooltipRenderHandler {
    private PendingTooltip pendingTooltip;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltipColor(RenderTooltipEvent.Color event) {
        TooltipTextures textures = getTextures(event.getStack());
        if (textures == null) {
            pendingTooltip = null;
            return;
        }

        pendingTooltip = new PendingTooltip(textures, event.getBorderStart(), event.getBorderEnd());
        if (textures.background != null) {
            event.setBackground(0);
        }
        if (textures.border != null) {
            event.setBorderStart(0);
            event.setBorderEnd(0);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltipPostBackground(RenderTooltipEvent.PostBackground event) {
        PendingTooltip pending = pendingTooltip;
        pendingTooltip = null;
        if (pending == null) {
            return;
        }

        int x = event.getX();
        int y = event.getY();
        int width = event.getWidth();
        int height = event.getHeight();
        MatrixStack matrixStack = event.getMatrixStack();
        if (pending.textures.background != null) {
            drawTexture(matrixStack, pending.textures.background, x, y, width, height, 400);
        }
        if (pending.textures.border != null) {
            drawTexture(matrixStack, pending.textures.border, x, y, width, height, 401);
        } else if (pending.textures.background != null) {
            drawVanillaBorder(matrixStack, x, y, width, height, pending.borderStart, pending.borderEnd);
        }
    }

    private static TooltipTextures getTextures(ItemStack stack) {
        if (!(stack.getItem() instanceof BuilderItem)) {
            return null;
        }
        AItemBase item = ((BuilderItem) stack.getItem()).getWrappedItem();
        if (!(item instanceof AItemPack)) {
            return null;
        }
        AJSONItem definition = ((AItemPack<?>) item).definition;
        TooltipTextureMetadata background = getTextureMetadata(definition.packID, definition.general.tooltipBackground);
        TooltipTextureMetadata border = getTextureMetadata(definition.packID, definition.general.tooltipBorder);
        return background != null || border != null ? new TooltipTextures(background, border) : null;
    }

    private static TooltipTextureMetadata getTextureMetadata(String packID, String path) {
        return TooltipTextureMetadata.get(packID, path, (metadataLocation) -> Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(metadataLocation)).getInputStream());
    }

    private static void drawTexture(MatrixStack matrixStack, TooltipTextureMetadata texture, int x, int y, int width, int height, int z) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getInstance().getTextureManager().bind(new ResourceLocation(texture.resourceLocation));
        matrixStack.pushPose();
        matrixStack.translate(0.0D, 0.0D, z);
        texture.renderTooltip(x, y, width, height, 4, (drawX, drawY, drawWidth, drawHeight, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight) -> AbstractGui.blit(matrixStack, drawX, drawY, drawWidth, drawHeight, (float) u, (float) v, sourceWidth, sourceHeight, textureWidth, textureHeight));
        matrixStack.popPose();
        RenderSystem.disableBlend();
    }

    private static void drawVanillaBorder(MatrixStack matrixStack, int x, int y, int width, int height, int borderStart, int borderEnd) {
        GuiUtils.drawGradientRect(matrixStack.last().pose(), 401, x - 3, y - 2, x - 2, y + height + 2, borderStart, borderEnd);
        GuiUtils.drawGradientRect(matrixStack.last().pose(), 401, x + width + 2, y - 2, x + width + 3, y + height + 2, borderStart, borderEnd);
        GuiUtils.drawGradientRect(matrixStack.last().pose(), 401, x - 3, y - 3, x + width + 3, y - 2, borderStart, borderStart);
        GuiUtils.drawGradientRect(matrixStack.last().pose(), 401, x - 3, y + height + 2, x + width + 3, y + height + 3, borderEnd, borderEnd);
    }

    private static class TooltipTextures {
        private final TooltipTextureMetadata background;
        private final TooltipTextureMetadata border;

        private TooltipTextures(TooltipTextureMetadata background, TooltipTextureMetadata border) {
            this.background = background;
            this.border = border;
        }
    }

    private static class PendingTooltip {
        private final TooltipTextures textures;
        private final int borderStart;
        private final int borderEnd;

        private PendingTooltip(TooltipTextures textures, int borderStart, int borderEnd) {
            this.textures = textures;
            this.borderStart = borderStart;
            this.borderEnd = borderEnd;
        }
    }
}
