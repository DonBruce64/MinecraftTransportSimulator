package minecrafttransportsimulator.guis;

import minecrafttransportsimulator.MTS;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

/**Base GUI class, and interface to MC's GUI systems.  All MTS GUIs should extend this class
 * rather than user their own.  This prevents the need to use MC classes and makes updating easier.
 *
 * @author don_bruce
 */
public abstract class GUIBase extends GuiScreen{
	protected static final ResourceLocation standardTexture = new ResourceLocation(MTS.MODID, "textures/guis/standard.png");
	
	/**
	 *  Draws the specified portion of the currently-bound texture.  Normally, this will be the standardTexture,
	 *  but other textures are possible if they are bound prior to calling this method.  A texture size
	 *  of 256x256 is assumed here, so don't use anything but that!  Draw starts at the bottom-left
	 *  point and goes counter-clockwise to the top-left point.
	 */
	public static void drawSheetTexture(int x, int y, int width, int height, int u, int v, int U, int V){
	 	float widthPixelPercent = 1.0F/256F;
        float heightPixelPercent = 1.0F/256F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, 			y + height, 0.0D).tex(u * widthPixelPercent, 	V * heightPixelPercent).endVertex();
        bufferbuilder.pos(x + width, 	y + height, 0.0D).tex(U * widthPixelPercent, 	V * heightPixelPercent).endVertex();
        bufferbuilder.pos(x + width, 	y, 			0.0D).tex(U * widthPixelPercent, 	v * heightPixelPercent).endVertex();
        bufferbuilder.pos(x, 			y, 			0.0D).tex(u * widthPixelPercent, 	v * heightPixelPercent).endVertex();
        tessellator.draw();
	}
}
