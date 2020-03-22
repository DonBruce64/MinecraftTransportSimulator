package minecrafttransportsimulator.rendering.blocks;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.pole.BlockPoleSign;
import minecrafttransportsimulator.blocks.pole.TileEntityPoleSign;
import minecrafttransportsimulator.jsondefs.JSONSign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;

public class RenderPoleSign extends TileEntitySpecialRenderer<TileEntityPoleSign>{
	private static final ResourceLocation defaultSignTexture = new ResourceLocation(MTS.MODID, "textures/blocks/trafficsign.png");
	private static final Map<JSONSign, ResourceLocation> textureMap = new HashMap<JSONSign, ResourceLocation>();	
	
	public RenderPoleSign(){}
	
	@Override
	public void render(TileEntityPoleSign sign, double x, double y, double z, float partialTicks, int destroyStage, float alpha){
		super.render(sign, x, y, z, partialTicks, destroyStage, alpha);
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glTranslatef(0.5F, 0.5F, 0.5F);
		if(sign.getWorld() != null){
			GL11.glRotatef(-sign.getWorld().getBlockState(sign.getPos()).getValue(BlockPoleSign.FACING).getHorizontalAngle(), 0, 1, 0);
		}
		GL11.glTranslatef(0F, 0F, 0.0635F);
		//Bind the sign texture.
		if(sign.definition == null){
			bindTexture(defaultSignTexture);
		}else{
			if(!textureMap.containsKey(sign.definition)){
				textureMap.put(sign.definition, new ResourceLocation(sign.definition.packID, "textures/signs/" + sign.definition.systemName + ".png"));
			}
			bindTexture(textureMap.get(sign.definition));
		}
	
		//Now render the texture.
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glBegin(GL11.GL_QUADS);
		//First do the front using the top half of the texture.
		//Top-left
		GL11.glTexCoord2f(0.0F, 0.0F);
		GL11.glNormal3f(0.0F, 0.0F, 1.0F);
		GL11.glVertex3f(-0.5F, 0.5F, 0.0F);
		//Bottom-left
		GL11.glTexCoord2f(0.0F, 0.5F);
		GL11.glNormal3f(0.0F, 0.0F, 1.0F);
		GL11.glVertex3f(-0.5F, -0.5F, 0.0F);
		//Bottom-right
		GL11.glTexCoord2f(1.0F, 0.5F);
		GL11.glNormal3f(0.0F, 0.0F, 1.0F);
		GL11.glVertex3f(0.5F, -0.5F, 0.0F);
		//Top-right
		GL11.glTexCoord2f(1.0F, 0.0F);
		GL11.glNormal3f(0.0F, 0.0F, 1.0F);
		GL11.glVertex3f(0.5F, 0.5F, 0.0F);
		
		//Now do the back using the bottom half.
		//Top-left
		GL11.glTexCoord2f(0.0F, 0.5F);
		GL11.glNormal3f(0.0F, 0.0F, -1.0F);
		GL11.glVertex3f(0.5F, 0.5F, 0.0F);
		//Bottom-left
		GL11.glTexCoord2f(0.0F, 1.0F);
		GL11.glNormal3f(0.0F, 0.0F, -1.0F);
		GL11.glVertex3f(0.5F, -0.5F, 0.0F);
		//Bottom-right
		GL11.glTexCoord2f(1.0F, 1.0F);
		GL11.glNormal3f(0.0F, 0.0F, -1.0F);
		GL11.glVertex3f(-0.5F, -0.5F, 0.0F);
		//Top-right
		GL11.glTexCoord2f(1.0F, 0.5F);
		GL11.glNormal3f(0.0F, 0.0F, -1.0F);
		GL11.glVertex3f(-0.5F, 0.5F, 0.0F);
		GL11.glEnd();
		
		//Now render the text.
		if(sign.definition != null){
			if(sign.definition.general.textLines != null){
				for(byte i=0; i<sign.definition.general.textLines.length; ++i){
					GL11.glPushMatrix();
					GL11.glTranslatef(sign.definition.general.textLines[i].xPos - 0.5F, sign.definition.general.textLines[i].yPos - 0.5F, 0.01F);
					GL11.glScalef(sign.definition.general.textLines[i].scale/16F, sign.definition.general.textLines[i].scale/16F, sign.definition.general.textLines[i].scale/16F);
					GL11.glRotatef(180, 1, 0, 0);
					Minecraft.getMinecraft().fontRenderer.drawString(sign.text.get(i), -Minecraft.getMinecraft().fontRenderer.getStringWidth(sign.text.get(i))/2, 0, Color.decode(sign.definition.general.textLines[i].color).getRGB());
					GL11.glPopMatrix();
				}
			}
		}
		GL11.glPopMatrix();
	}
}
