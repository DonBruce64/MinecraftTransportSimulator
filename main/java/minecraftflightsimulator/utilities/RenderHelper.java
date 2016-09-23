package minecraftflightsimulator.utilities;

import java.awt.Color;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.MFSClientRegistry;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.entities.parts.EntitySeat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**This class is responsible for most of the legwork of the custom rendering system.
 * Contains multiple methods for drawing textured quads, as well as custom classes
 * for models and registration functions.  Texturing functions shouldn't need updating
 * each MCVersion, but the classes at the end may.
 * 
 * @author don_bruce
 */
@SideOnly(Side.CLIENT)
public class RenderHelper{	
	public static boolean lockedView = true;
	public static int hudMode = 2;
	private static int zoomLevel = 4;
	private static final String[] rollNames = new String[] {"camRoll", "R", "field_78495_O"};
	private static final String[] zoomNames = new String[] {"thirdPersonDistance", "thirdPersonDistanceTemp", "field_78490_B", "field_78491_C"};
	private static final TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
	private static final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
	
	public static void changeCameraZoom(int zoom){
		if(zoomLevel < 15 && zoom == 1){
			++zoomLevel;
		}else if(zoomLevel > 4 && zoom == -1){
			--zoomLevel;
		}else if(zoom == 0){
			zoomLevel = 4;
		}else{
			return;
		}
		
		try{
			ObfuscationReflectionHelper.setPrivateValue(EntityRenderer.class, Minecraft.getMinecraft().entityRenderer, zoomLevel, zoomNames);
		}catch (Exception e){
			System.err.println("ERROR IN AIRCRAFT ZOOM REFLECTION!");
			throw new RuntimeException(e);
		}
	}
	
	public static void changeCameraRoll(float roll){
		try{
			ObfuscationReflectionHelper.setPrivateValue(EntityRenderer.class, Minecraft.getMinecraft().entityRenderer, roll, rollNames);
		}catch (Exception e){
			System.err.println("ERROR IN AIRCRAFT ROLL REFLECTION!");
		}
	}
	
	public static void changeCameraLock(){
		lockedView = !lockedView;
		MFS.proxy.playSound(Minecraft.getMinecraft().thePlayer, "gui.button.press", 1, 1);
	}
	
	/**
	 * Binds the specified texture.
	 */
	public static void bindTexture(ResourceLocation texture){
		textureManager.bindTexture(texture);
	}
	
	/**
	 * Basic string draw function for 2D GUIs.
	 */
	public static void drawString(String string, int x, int y, Color color){
		fontRenderer.drawString(string, x, y, color.getRGB());
	}
	
	/**
	 * Complex draw function for strings in 3D space.
	 */
	public static void drawScaledStringAt(String string, float x, float y, float z, float scale, Color color){
		GL11.glPushMatrix();
		GL11.glTranslatef(x, y, z);
		GL11.glScalef(scale, scale, scale);
		fontRenderer.drawString(string, -fontRenderer.getStringWidth(string)/2, 0, color.getRGB());
		GL11.glPopMatrix();
	}
	
	/**
     * Draws a quad clockwise starting from top-left point.
     */
    public static void renderQuad(double x1, double x2, double x3, double x4, double y1, double y2, double y3, double y4, double z1, double z2, double z3, double z4, boolean mirror){
    	renderQuadUV(x1, x2, x3, x4, y1, y2, y3, y4, z1, z2, z3, z4, 0, 1, 0, 1, mirror);
    }
    
	/**
     * Draws a quad clockwise starting from top-left point with custom UV mapping.
     */
    public static void renderQuadUV(double x1, double x2, double x3, double x4, double y1, double y2, double y3, double y4, double z1, double z2, double z3, double z4, double u, double U, double v, double V, boolean mirror){
    	renderQuadUVCustom(x1, x2, x3, x4, y1, y2, y3, y4, z1, z2, z3, z4, u, u, U, U, v, V, V, v, mirror);
    }
    
	/**
     * Draws a quad clockwise starting from top-left point with custom UV mapping for each vertex.
     */
    public static void renderQuadUVCustom(double x1, double x2, double x3, double x4, double y1, double y2, double y3, double y4, double z1, double z2, double z3, double z4, double u1, double u2, double u3, double u4, double v1, double v2, double v3, double v4, boolean mirror){
    	GL11.glPushMatrix();
		GL11.glBegin(GL11.GL_QUADS);
		
		GL11.glTexCoord2d(u1, v1);
		GL11.glVertex3d(x1, y1, z1);
		GL11.glTexCoord2d(u2, v2);
		GL11.glVertex3d(x2, y2, z2);
		GL11.glTexCoord2d(u3, v3);
		GL11.glVertex3d(x3, y3, z3);
		GL11.glTexCoord2d(u4, v4);
		GL11.glVertex3d(x4, y4, z4);
    	
    	if(mirror){
    		GL11.glTexCoord2d(u1, v1);
    		GL11.glVertex3d(x4, y4, z4);
    		GL11.glTexCoord2d(u2, v2);
    		GL11.glVertex3d(x3, y3, z3);
    		GL11.glTexCoord2d(u3, v3);
    		GL11.glVertex3d(x2, y2, z2);
    		GL11.glTexCoord2d(u4, v4);
    		GL11.glVertex3d(x1, y1, z1);
    	}
    	GL11.glEnd();
		GL11.glPopMatrix();
    }
    
    /**
     * Draws a triangle clockwise starting from top-left point
     */
    public static  void renderTriangle(double x1, double x2, double x3, double y1, double y2, double y3, double z1, double z2, double z3, boolean mirror){
    	renderTriangleUV(x1, x2, x3, y1, y2, y3, z1, z2, z3, 0, 1, 0, 1, mirror);
    }
    
    /**
     * Draws a triangle clockwise starting from top-left point with custom UV mapping.
     */
    public static  void renderTriangleUV(double x1, double x2, double x3, double y1, double y2, double y3, double z1, double z2, double z3, double u, double U, double v, double V, boolean mirror){
    	GL11.glPushMatrix();
		GL11.glBegin(GL11.GL_TRIANGLES);
		
		GL11.glTexCoord2d(u, v);
		GL11.glVertex3d(x1, y1, z1);
		GL11.glTexCoord2d(u, V);
		GL11.glVertex3d(x2, y2, z2);
		GL11.glTexCoord2d(U, V);
		GL11.glVertex3d(x3, y3, z3);
    	
    	if(mirror){
    		GL11.glTexCoord2d(U, V);
    		GL11.glVertex3d(x3, y3, z3);
    		GL11.glTexCoord2d(u, V);
    		GL11.glVertex3d(x2, y2, z2);
    		GL11.glTexCoord2d(u, v);
    		GL11.glVertex3d(x1, y1, z1);
    	}
    	
    	GL11.glEnd();
		GL11.glPopMatrix();
    }
    
    /**
     * Draws a square with specified bounds.  Use for vertical draws only.
     */
    public static  void renderSquare(double x1, double x2, double y1, double y2, double z1, double z2, boolean mirror){
    	renderQuad(x1, x1, x2, x2, y2, y1, y1, y2, z1, z1, z2, z2, mirror);
    }
    
    /**
     * Draws a square with specified bounds and custom UV mapping.  Use for vertical draws only.
     */
    public static  void renderSquareUV(double x1, double x2, double y1, double y2, double z1, double z2, double u, double U, double v, double V, boolean mirror){
    	renderQuadUV(x1, x1, x2, x2, y2, y1, y1, y2, z1, z1, z2, z2, u, U, v, V, mirror);
    }
    
    public static void drawCone(double r, double l, double n, boolean reverse){
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glVertex3d(0, 0, 0);
    	if(reverse){
    		for(float theta=0; theta < 2*Math.PI + 0.1; theta += 2F*Math.PI/n){
    			GL11.glVertex3d(r*MathHelper.cos(theta), r*MathHelper.sin(theta), l);
    		}
    	}else{
    		for(float theta=(float) (2*Math.PI); theta>=0 - 0.1; theta -= 2F*Math.PI/n){
    			GL11.glVertex3d(r*MathHelper.cos(theta), r*MathHelper.sin(theta), l);
    		}
    	}
    	GL11.glEnd();
    }
    
    /**
     * Draws a semi-conical light beam with
     * an end radius of r. length of l, and n segments.
     */
    public static void drawLightBeam(double r, double l, int n){    	
    	GL11.glPushMatrix();
    	GL11.glDisable(GL11.GL_TEXTURE_2D);
    	GL11.glDisable(GL11.GL_LIGHTING);
    	GL11.glShadeModel(GL11.GL_SMOOTH);
    	GL11.glEnable(GL11.GL_BLEND);
    	GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE);
    	drawCone(r, l, n, false);
    	drawCone(r, l, n, false);
    	drawCone(r, l, n, true);
    	drawCone(r, l, n, true);    	
    	GL11.glDisable(GL11.GL_BLEND);
    	GL11.glEnable(GL11.GL_LIGHTING);
    	GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glPopMatrix();
    }
    
    /**Abstract class for parent rendering.
     * Renders the parent model, and all child models that have been registered by
     * {@link registerChildRender}.  Ensures all parts are rendered in the exact
     * location they should be in as all rendering is done in the same operation.
     * 
     * @author don_bruce
     */
    public static abstract class RenderParent extends Render{
    	private boolean playerRiding;
    	private MFSVector childOffset;
    	private static EntityPlayer player;
    	
    	public RenderParent(RenderManager manager){
            super();
            shadowSize = 0;
        }
    	
    	@Override
    	public void doRender(Entity entity, double x, double y, double z, float yaw, float pitch){
    		this.render((EntityParent) entity, x, y, z);
    	}
    	
    	/**
    	 * Entities don't render above 255 well in later versions due to the
    	 * new chunk visibility system.  This code is for the default system
    	 * only, and will be called directly from Minecraft's rendering
    	 * system or {@link ClientEventHandler#on(RenderWorldLastEvent)}.
    	 * The latter method only calls if this method hasn't been called first
    	 * by Minecraft's system.
		 **/
    	private void render(EntityParent parent, double x, double y, double z){
    		/*INS180
    		if(!parent.rendered && parent.posY >= 255){return;}
    		INS180*/
    		parent.rendered = true;
    		player = Minecraft.getMinecraft().thePlayer;
    		GL11.glPushMatrix();
    		playerRiding = false;
    		if(player.ridingEntity instanceof EntitySeat){
    			if(parent.equals(((EntitySeat) player.ridingEntity).parent)){
    				playerRiding = true;
    			}
    		}
    		//For some reason x, y, z aren't correct.
    		//Have to do this or put up with shaking while in the plane. 
    		if(playerRiding){
    			GL11.glTranslated(parent.posX - player.posX, parent.posY - player.posY, parent.posZ - player.posZ);
    		}else{
    			GL11.glTranslated(x, y, z);
    		}
            for(EntityChild child : parent.getChildren()){
            	if(MFSClientRegistry.childRenderMap.get(child.getClass()) != null){
            		childOffset = RotationHelper.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, parent.rotationPitch, parent.rotationYaw, parent.rotationRoll);
            		MFSClientRegistry.childRenderMap.get(child.getClass()).renderChildModel(child, childOffset.xCoord, childOffset.yCoord, childOffset.zCoord);
        		}
            }
            this.renderParentModel(parent);
            GL11.glPopMatrix();
    	}
    	
    	protected abstract void renderParentModel(EntityParent parent);
    	
    	@Override
    	protected ResourceLocation getEntityTexture(Entity propellor){
    		return null;
    	}
    }
    
    /**Abstract class for child rendering.
     * Register with {@link registerChildRender} to activate rendering in {@link RenderParent}
     * 
     * @author don_bruce
     */
    public static abstract class RenderChild{
    	public RenderChild(){}
    	public abstract void renderChildModel(EntityChild child, double x, double y, double z);
    }
    
    public static abstract class RenderTileBase extends TileEntitySpecialRenderer{
    	
    	@Override
    	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float scale){
    		this.doRender(tile, x, y, z);
    	}
    	
    	protected abstract void doRender(TileEntity tile, double x, double y, double z);
    }
    
    public static class RenderNull extends Render{
    	public RenderNull(RenderManager manager){
            super();
    	}
    	@Override
    	public void doRender(Entity entity, double x, double y, double z, float yaw,float pitch){}
    	@Override
    	protected ResourceLocation getEntityTexture(Entity entity){return null;}
    }
}
