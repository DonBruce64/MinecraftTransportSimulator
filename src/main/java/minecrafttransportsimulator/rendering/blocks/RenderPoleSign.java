package minecrafttransportsimulator.rendering.blocks;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.instances.BlockPoleSign;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleSign;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperRender;

public class RenderPoleSign extends ARenderTileEntityBase<TileEntityPoleSign, BlockPoleSign>{
	
	public RenderPoleSign(){}
	
	@Override
	public void render(TileEntityPoleSign tile, BlockPoleSign block, float partialTicks){
		//Start rendering by translating to the position of the TE and rotating it.
		GL11.glPushMatrix();
		GL11.glTranslated(tile.position.x, tile.position.y, tile.position.z);
		GL11.glTranslatef(0.5F, 0.5F, 0.5F);
		GL11.glRotatef(-block.getRotation(tile.world, tile.position), 0, 1, 0);
		
		//Translate a little to make the sign offset from the pole.
		GL11.glTranslatef(0F, 0F, 0.0635F);
		
		//Bind the sign texture.
		if(tile.definition == null){
			WrapperRender.bindTexture(MTS.MODID, "textures/blocks/traffictile.png");
		}else{
			WrapperRender.bindTexture(tile.definition.packID, "textures/signs/" + tile.definition.systemName + ".png");
		}
	
		//Set color to normal and render the texture as a quad.
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
		if(tile.definition != null){
			if(tile.definition.general.textLines != null){
				for(byte i=0; i<tile.definition.general.textLines.length; ++i){
					GL11.glPushMatrix();
					GL11.glTranslatef(tile.definition.general.textLines[i].xPos - 0.5F, tile.definition.general.textLines[i].yPos - 0.5F, 0.01F);
					GL11.glScalef(tile.definition.general.textLines[i].scale/16F, tile.definition.general.textLines[i].scale/16F, tile.definition.general.textLines[i].scale/16F);
					GL11.glRotatef(180, 1, 0, 0);
					WrapperGUI.drawText(tile.text.get(i), 0, 0, Color.decode(tile.definition.general.textLines[i].color), true, false, 0);
					GL11.glPopMatrix();
				}
			}
		}
		GL11.glPopMatrix();
	}
}
