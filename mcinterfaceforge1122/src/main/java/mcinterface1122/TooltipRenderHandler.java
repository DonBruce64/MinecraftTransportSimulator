package mcinterface1122;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.rendering.TooltipTextureMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.client.config.GuiUtils;

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
        if (pending.textures.background != null) {
            drawTexture(pending.textures.background, x, y, width, height, 300);
        }
        if (pending.textures.border != null) {
            drawTexture(pending.textures.border, x, y, width, height, 301);
        } else if (pending.textures.background != null) {
            drawVanillaBorder(x, y, width, height, pending.borderStart, pending.borderEnd);
        }
    }

    private static TooltipTextures getTextures(ItemStack stack) {
        if (!(stack.getItem() instanceof BuilderItem)) {
            return null;
        }
        AItemBase item = ((BuilderItem) stack.getItem()).getItem();
        if (!(item instanceof AItemPack)) {
            return null;
        }
        AJSONItem definition = ((AItemPack<?>) item).definition;
        TooltipTextureMetadata background = getTextureMetadata(definition.packID, definition.general.tooltipBackground);
        TooltipTextureMetadata border = getTextureMetadata(definition.packID, definition.general.tooltipBorder);
        return background != null || border != null ? new TooltipTextures(background, border) : null;
    }

    private static TooltipTextureMetadata getTextureMetadata(String packID, String path) {
        return TooltipTextureMetadata.get(packID, path, (metadataLocation) -> Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(metadataLocation)).getInputStream());
    }

    private static void drawTexture(TooltipTextureMetadata texture, int x, int y, int width, int height, int z) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(texture.resourceLocation));
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        texture.renderTooltip(x, y, width, height, 4, (drawX, drawY, drawWidth, drawHeight, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight) -> drawSlice(drawX, drawY, drawWidth, drawHeight, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight, z));
        GlStateManager.disableBlend();
    }

    private static void drawSlice(int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight, int z) {
        float minU = (float) u / textureWidth;
        float maxU = (float) (u + sourceWidth) / textureWidth;
        float minV = (float) v / textureHeight;
        float maxV = (float) (v + sourceHeight) / textureHeight;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + height, z).tex(minU, maxV).endVertex();
        buffer.pos(x + width, y + height, z).tex(maxU, maxV).endVertex();
        buffer.pos(x + width, y, z).tex(maxU, minV).endVertex();
        buffer.pos(x, y, z).tex(minU, minV).endVertex();
        tessellator.draw();
    }

    private static void drawVanillaBorder(int x, int y, int width, int height, int borderStart, int borderEnd) {
        GuiUtils.drawGradientRect(301, x - 3, y - 2, x - 2, y + height + 2, borderStart, borderEnd);
        GuiUtils.drawGradientRect(301, x + width + 2, y - 2, x + width + 3, y + height + 2, borderStart, borderEnd);
        GuiUtils.drawGradientRect(301, x - 3, y - 3, x + width + 3, y - 2, borderStart, borderStart);
        GuiUtils.drawGradientRect(301, x - 3, y + height + 2, x + width + 3, y + height + 3, borderEnd, borderEnd);
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
