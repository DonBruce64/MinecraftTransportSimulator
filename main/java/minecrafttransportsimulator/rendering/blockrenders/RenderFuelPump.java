package minecrafttransportsimulator.rendering.blockrenders;

import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntityFuelPump;
import minecrafttransportsimulator.dataclasses.MTSRegistryClient;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class RenderFuelPump extends TileEntitySpecialRenderer{
	private static final ResourceLocation texture = new ResourceLocation(MTS.MODID, "textures/blockmodels/fuelpump.png");
	private static int displayListIndex = -1;
	
	private TileEntityFuelPump pump;

	public RenderFuelPump(){}
	
	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks, int destroyStage){
		super.renderTileEntityAt(tile, x, y, z, partialTicks, destroyStage);
		this.pump = (TileEntityFuelPump) tile;
		
		if(displayListIndex == -1){
			displayListIndex = GL11.glGenLists(1);
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			for(Entry<String, Float[][]> entry : MTSRegistryClient.modelMap.get("fuelpump").entrySet()){
				for(Float[] vertex : entry.getValue()){
					GL11.glTexCoord2f(vertex[3], vertex[4]);
					GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
					GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
				}
			}
			GL11.glEnd();
			GL11.glEndList();
		}

		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glTranslatef(0.5F, 0F, 0.5F);
		GL11.glRotatef(180 - 45*pump.rotation, 0, 1, 0);
		this.bindTexture(texture);
		GL11.glCallList(displayListIndex);
		GL11.glPopMatrix();
	}
}
