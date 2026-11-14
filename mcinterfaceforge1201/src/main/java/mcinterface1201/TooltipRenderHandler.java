package mcinterface1201;

import com.mojang.blaze3d.systems.RenderSystem;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.rendering.TooltipTextureMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Renders per-pack tooltip textures while retaining vanilla fallbacks for omitted layers. */
public class TooltipRenderHandler {
    private static TooltipTextures pendingTextures;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltipColor(RenderTooltipEvent.Color event) {
        TooltipTextures textures = getTextures(event.getItemStack());
        pendingTextures = textures;
        if (textures == null) {
            return;
        }
        if (textures.background != null) {
            event.setBackground(0);
        }
        if (textures.border != null) {
            event.setBorderStart(0);
            event.setBorderEnd(0);
        }
    }

    public static void renderBackground(GuiGraphics graphics, int x, int y, int width, int height, int z) {
        TooltipTextures textures = pendingTextures;
        if (textures != null && textures.background != null) {
            drawTexture(graphics, textures.background, x, y, width, height, z);
        }
    }

    public static void renderBorder(GuiGraphics graphics, int x, int y, int width, int height, int z) {
        TooltipTextures textures = pendingTextures;
        pendingTextures = null;
        if (textures != null && textures.border != null) {
            drawTexture(graphics, textures.border, x, y, width, height, z);
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
        return TooltipTextureMetadata.get(packID, path, (metadataLocation) -> {
            net.minecraft.server.packs.resources.Resource resource = Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(metadataLocation)).orElse(null);
            return resource != null ? resource.open() : null;
        });
    }

    private static void drawTexture(GuiGraphics graphics, TooltipTextureMetadata texture, int x, int y, int width, int height, int z) {
        ResourceLocation location = new ResourceLocation(texture.resourceLocation);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0D, 0.0D, z);
        texture.renderTooltip(x, y, width, height, 4, (drawX, drawY, drawWidth, drawHeight, u, v, sourceWidth, sourceHeight, textureWidth, textureHeight) -> graphics.blit(location, drawX, drawY, drawWidth, drawHeight, (float) u, (float) v, sourceWidth, sourceHeight, textureWidth, textureHeight));
        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private static class TooltipTextures {
        private final TooltipTextureMetadata background;
        private final TooltipTextureMetadata border;

        private TooltipTextures(TooltipTextureMetadata background, TooltipTextureMetadata border) {
            this.background = background;
            this.border = border;
        }
    }
}
