package minecraftflightsimulator.utilities;

import java.awt.Color;

import minecraftflightsimulator.MFS;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

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
    
    public static abstract class RenderEntityBase extends Render{
    	public RenderEntityBase(RenderManager manager){
            super();
        }
    	
    	@Override
    	protected ResourceLocation getEntityTexture(Entity propellor){
    		return null;
    	}
    }
    
    public static abstract class RenderTileBase extends TileEntitySpecialRenderer{
    	
    	@Override
    	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float scale){
    		this.doRender(tile, x, y, z);
    	}
    	
    	protected abstract void doRender(TileEntity tile, double x, double y, double z);
    }
}
