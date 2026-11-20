package mcinterface1182;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.rendering.TooltipTextureMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.gui.GuiUtils;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Renders per-pack tooltip textures while retaining vanilla fallbacks for omitted layers. */
public class TooltipRenderHandler {
    private static PendingTooltip pendingTooltip;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltipColor(RenderTooltipEvent.Color event) {
        TooltipTextures textures = getTextures(event.getItemStack());
        if (textures == null) {
            pendingTooltip = null;
            return;
        }

        int width = 0;
        int height = event.getComponents().size() == 1 ? -2 : 0;
        for (ClientTooltipComponent component : event.getComponents()) {
            width = Math.max(width, component.getWidth(event.getFont()));
            height += component.getHeight();
        }
        pendingTooltip = new PendingTooltip(textures, event.getX(), event.getY(), width, height, event.getBorderStart(), event.getBorderEnd());
        if (textures.background != null) {
            event.setBackground(0);
        }
        if (textures.border != null) {
            event.setBorderStart(0);
            event.setBorderEnd(0);
        }
    }

    /** Called by the Screen mixin immediately after vanilla flushes its tooltip background. */
    public static void renderPending(PoseStack poseStack) {
        PendingTooltip pending = pendingTooltip;
        pendingTooltip = null;
        if (pending == null) {
            return;
        }

        if (pending.textures.background != null) {
            drawTexture(poseStack, pending.textures.background, pending.x, pending.y, pending.width, pending.height);
        }
        if (pending.textures.border != null) {
            drawTexture(poseStack, pending.textures.border, pending.x, pending.y, pending.width, pending.height);
        } else if (pending.textures.background != null) {
            drawVanillaBorder(poseStack, pending);
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

    private static void drawTexture(PoseStack poseStack, TooltipTextureMetadata texture, int x, int y, int width, int height) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, new ResourceLocation(texture.resourceLocation));
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 400.0D);
        texture.renderTooltip(x, y, width, height, 4, (drawX, drawY, drawWidth, drawHeight, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight) -> GuiComponent.blit(poseStack, drawX, drawY, drawWidth, drawHeight, (float) u, (float) v, sourceWidth, sourceHeight, textureWidth, textureHeight));
        poseStack.popPose();
    }

    private static void drawVanillaBorder(PoseStack poseStack, PendingTooltip pending) {
        GuiUtils.drawGradientRect(poseStack.last().pose(), 400, pending.x - 3, pending.y - 2, pending.x - 2, pending.y + pending.height + 2, pending.borderStart, pending.borderEnd);
        GuiUtils.drawGradientRect(poseStack.last().pose(), 400, pending.x + pending.width + 2, pending.y - 2, pending.x + pending.width + 3, pending.y + pending.height + 2, pending.borderStart, pending.borderEnd);
        GuiUtils.drawGradientRect(poseStack.last().pose(), 400, pending.x - 3, pending.y - 3, pending.x + pending.width + 3, pending.y - 2, pending.borderStart, pending.borderStart);
        GuiUtils.drawGradientRect(poseStack.last().pose(), 400, pending.x - 3, pending.y + pending.height + 2, pending.x + pending.width + 3, pending.y + pending.height + 3, pending.borderEnd, pending.borderEnd);
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
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int borderStart;
        private final int borderEnd;

        private PendingTooltip(TooltipTextures textures, int x, int y, int width, int height, int borderStart, int borderEnd) {
            this.textures = textures;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.borderStart = borderStart;
            this.borderEnd = borderEnd;
        }
    }
}
