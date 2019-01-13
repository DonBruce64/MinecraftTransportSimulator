package minecrafttransportsimulator.rendering.blockrenders;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.pole.TileEntityPoleSign;
import minecrafttransportsimulator.dataclasses.PackSignObject;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3i;

public class RenderDecorSign extends TileEntitySpecialRenderer<TileEntityPoleSign>{
	private static final ResourceLocation defaultSignTexture = new ResourceLocation(MTS.MODID, "textures/blocks/trafficsign.png");
	private static final Map<String, ResourceLocation> textureMap = new HashMap<String, ResourceLocation>();
	private static final Map<String, FontRenderer> fontMap = new HashMap<String, FontRenderer>();
	
	
	public RenderDecorSign(){}
	
	@Override
	public void renderTileEntityAt(TileEntityPoleSign decor, double x, double y, double z, float partialTicks, int destroyStage){
		super.renderTileEntityAt(decor, x, y, z, partialTicks, destroyStage);
		final Vec3i facingVec = EnumFacing.VALUES[decor.rotation].getDirectionVec();		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glTranslatef(0.5F, 0.5F, 0.5F);
	
		if(facingVec.getX() == 1){
			GL11.glRotatef(90, 0, 1, 0);
		}else if(facingVec.getX() == -1){
			GL11.glRotatef(270, 0, 1, 0);
		}else if(facingVec.getZ() == -1){
			GL11.glRotatef(180, 0, 1, 0);
		}
		
		GL11.glTranslatef(0F, 0F, 0.0635F);
		//Bind the sign texture.
		if(decor.definition.isEmpty()){
			bindTexture(defaultSignTexture);
		}else{
			if(!textureMap.containsKey(decor.definition)){
				textureMap.put(decor.definition, new ResourceLocation(decor.definition.substring(0, decor.definition.indexOf(':')), "textures/signs/" + decor.definition.substring(decor.definition.indexOf(':') + 1) + ".png"));
			}
			bindTexture(textureMap.get(decor.definition));
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
		PackSignObject pack = PackParserSystem.getSign(decor.definition);
		if(pack != null){
			if(pack.general.textLines != null){
				if(!fontMap.containsKey(pack.general.font)){
					if(pack.general.font.equals("default")){
						fontMap.put(pack.general.font, Minecraft.getMinecraft().fontRendererObj);
					}else{
						String fontName = pack.general.font;
						ResourceLocation fontLocation = new ResourceLocation(fontName.substring(0, fontName.indexOf(':')), "textures/fonts/" + fontName.substring(fontName.indexOf(':') + 1) + ".png");;
						fontMap.put(pack.general.font, new FontRenderer(Minecraft.getMinecraft().gameSettings, fontLocation, Minecraft.getMinecraft().renderEngine, false));
					}
				}
				FontRenderer currentFont = fontMap.get(pack.general.font);
				
				for(byte i=0; i<pack.general.textLines.length; ++i){
					GL11.glPushMatrix();
					GL11.glTranslatef(pack.general.textLines[i].xPos - 0.5F, pack.general.textLines[i].yPos - 0.5F, 0.01F);
					GL11.glScalef(pack.general.textLines[i].scale/16F, pack.general.textLines[i].scale/16F, pack.general.textLines[i].scale/16F);
					GL11.glRotatef(180, 1, 0, 0);
					currentFont.drawString(decor.text.get(i), -currentFont.getStringWidth(decor.text.get(i))/2, 0, Color.decode(pack.general.textLines[i].color).getRGB());
					GL11.glPopMatrix();
				}
			}
		}
		GL11.glPopMatrix();
	}
}
