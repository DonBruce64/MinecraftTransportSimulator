package minecrafttransportsimulator.rendering.blocks;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.wrappers.WrapperRender;
import minecrafttransportsimulator.wrappers.WrapperTileEntityRender;

/**Base Tile Entity rendering class (TESR).  This type is used in the constructor of {@link WrapperTileEntityRender} 
 * to allow us to use completely custom render code that is not associated with MC's standard render code.  This should
 * be used with all blocks that need fancy rendering that can't be done with JSON.
 *
 * @author don_bruce
 */
public abstract class ARenderTileEntityBase<RenderedTileEntity extends ATileEntityBase<?>, RenderedBlock extends IBlockTileEntity<?>>{
	
	/**
	 *  Called to render this tile entity.  The currently-bound texture is undefined, so you will need
	 *  to bind whichever texture you see fit to do so.  This can be done via {@link WrapperRender#bindTexture(String, String)}
	 */
	public abstract void render(RenderedTileEntity tileEntity, RenderedBlock block, float partialTicks);
	
	/**
	 *  Renders a lighted square at the passed-in location.  Used for internal lights on JSON models.
	 *  Renders both a colored square as well as a lens flare.
	 */
	protected static void renderLightedSquare(float lightSize, float lightBrightness, Color lightColor, String lightTexture){
		final float flareSize = lightSize*4F;
		WrapperRender.bindTexture(MTS.MODID, lightTexture);
		GL11.glColor3f(lightColor.getRed()/255F, lightColor.getGreen()/255F, lightColor.getBlue()/255F);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3f(-lightSize/2F, lightSize/2F, 0);
		GL11.glTexCoord2f(0, 1);
		GL11.glVertex3f(-lightSize/2F, -lightSize/2F, 0);
		GL11.glTexCoord2f(1, 1);
		GL11.glVertex3f(lightSize/2F, -lightSize/2F, 0);
		GL11.glTexCoord2f(1, 0);
		GL11.glVertex3f(lightSize/2F, lightSize/2F, 0);
		
		
		GL11.glEnd();
		
		GL11.glTranslatef(0, 0, -0.001F);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		WrapperRender.bindTexture(MTS.MODID, "textures/rendering/lensflare.png");
		GL11.glColor4f(lightColor.getRed()/255F, lightColor.getGreen()/255F, lightColor.getBlue()/255F, lightBrightness);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(1, 0);
		GL11.glVertex3f(-flareSize/2F, -flareSize/2F, 0);
		GL11.glTexCoord2f(1, 1);
		GL11.glVertex3f(flareSize/2F, -flareSize/2F, 0);
		GL11.glTexCoord2f(0, 1);
		GL11.glVertex3f(flareSize/2F, flareSize/2F, 0);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3f(-flareSize/2F, flareSize/2F, 0);
		GL11.glEnd();
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	/**
	 *  Renders a lighted cone at the passed-in location.  If reverse is true, the cone is rendered
	 *  on both the outer and inner sections.  This can be used to create lights that shine on
	 *  both sides of the cone, but one more than the other.
	 */
	protected static void drawLightCone(Point3d endPoint, boolean reverse){
		WrapperRender.bindTexture(MTS.MODID, "textures/rendering/lightbeam.png");
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3d(0, 0, 0);
		if(reverse){
			for(float theta=0; theta < 2*Math.PI + 0.1; theta += 2F*Math.PI/40F){
				GL11.glTexCoord2f(theta, 1);
				GL11.glVertex3d(endPoint.x + 3.0F*Math.cos(theta), endPoint.y + 3.0F*Math.sin(theta), endPoint.z);
			}
		}else{
			for(float theta=(float) (2*Math.PI); theta>=0 - 0.1; theta -= 2F*Math.PI/40F){
				GL11.glTexCoord2f(theta, 1);
				GL11.glVertex3d(endPoint.x + 3.0F*Math.cos(theta), endPoint.y + 3.0F*Math.sin(theta), endPoint.z);
			}
		}
		GL11.glEnd();
	}
}
