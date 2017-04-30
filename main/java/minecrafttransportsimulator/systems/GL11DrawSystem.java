package minecrafttransportsimulator.systems;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.minecrafthelpers.BlockHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

/**This class is responsible for most of the legwork of the custom rendering system.
 * Contains multiple methods for drawing textured quads and the like.
 * 
 * @author don_bruce
 */
public final class GL11DrawSystem{
	private static final TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
	private static final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
	private static final Map<String, ResourceLocation> mtsTextureArray = new HashMap<String, ResourceLocation>();
	
	public static final ResourceLocation glassTexture = new ResourceLocation("minecraft", "textures/blocks/glass.png");
	
	/**
	 * Binds the specified texture.  Used for vanilla textures.
	 */
	public static void bindTexture(ResourceLocation texture){
		textureManager.bindTexture(texture);
	}
	
	/**
	 * Binds the specified texture.  Used for MTS textures.  Cached for efficiency.
	 */
	public static void bindTexture(String textureName){
		if(!mtsTextureArray.containsKey(textureName)){
			File imageFile = new File(MTS.assetDir + File.separator + textureName);
			if(!imageFile.exists()){
				System.err.println("ERROR: COULD NOT FIND IMAGE FILE: " + imageFile.getAbsolutePath());
				mtsTextureArray.put(textureName, new ResourceLocation(MTS.MODID, "missing/texture/file/"+textureName));
			}else{
				try{
					mtsTextureArray.put(textureName, textureManager.getDynamicTextureLocation(textureName, new DynamicTexture(ImageIO.read(imageFile))));
				}catch(Exception e){
					System.err.println("ERROR: COULD NOT READ IMAGE FILE: " + imageFile.getAbsolutePath());
				}
			}
		}
		bindTexture(mtsTextureArray.get(textureName));
	}
	
	/**
	 * Basic string draw function for 2D GUIs.
	 */
	public static void drawString(String string, int x, int y, Color color){
		fontRenderer.drawString(string, x, y, color.getRGB());
		GL11.glColor3f(1, 1, 1);
	}
	
	/**
	 * Complex draw function for strings in 3D space.
	 */
	public static void drawScaledStringAt(String string, float x, float y, float z, float scale, Color color){
		GL11.glPushMatrix();
		GL11.glTranslatef(x, y, z);
		GL11.glScalef(scale, scale, scale);
		fontRenderer.drawString(string, -fontRenderer.getStringWidth(string)/2, 0, color.getRGB());
		GL11.glColor3f(1, 1, 1);
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
    public static void renderTriangle(double x1, double x2, double x3, double y1, double y2, double y3, double z1, double z2, double z3, boolean mirror){
    	renderTriangleUV(x1, x2, x3, y1, y2, y3, z1, z2, z3, 0, 1, 0, 1, mirror);
    }
    
    /**
     * Draws a triangle clockwise starting from top-left point with custom UV mapping.
     */
    public static void renderTriangleUV(double x1, double x2, double x3, double y1, double y2, double y3, double z1, double z2, double z3, double u, double U, double v, double V, boolean mirror){
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
    public static void renderSquare(double x1, double x2, double y1, double y2, double z1, double z2, boolean mirror){
    	renderQuad(x1, x1, x2, x2, y2, y1, y1, y2, z1, z1, z2, z2, mirror);
    }
    
    /**
     * Draws a square with specified bounds and custom UV mapping.  Use for vertical draws only.
     */
    public static void renderSquareUV(double x1, double x2, double y1, double y2, double z1, double z2, double u, double U, double v, double V, boolean mirror){
    	renderQuadUV(x1, x1, x2, x2, y2, y1, y1, y2, z1, z1, z2, z2, u, U, v, V, mirror);
    }
    
    /**
     * Draws a semi-conical light beam with
     * an end radius of r. length of l, and n segments.
     */
    public static void drawLightBeam(EntityMultipartVehicle vehicle, double r, double l, int n, boolean highPower){
    	float strength = (float) (vehicle.electricPower/12F*(15F - vehicle.worldObj.getSunBrightness(1.0F)*BlockHelper.getBlockLight(vehicle.worldObj, (int) Math.floor(vehicle.posX - 4*Math.sin(vehicle.rotationYaw * 0.017453292F)), (int) Math.floor(vehicle.posY), (int) Math.floor(vehicle.posZ + 4*Math.cos(vehicle.rotationYaw * 0.017453292F))))/15F);
    	GL11.glPushMatrix();
    	GL11.glColor4f(1, 1, 1, Math.min(vehicle.electricPower > 4 ? 1.0F : 0, strength));
    	GL11.glDisable(GL11.GL_TEXTURE_2D);
    	GL11.glDisable(GL11.GL_LIGHTING);
    	GL11.glEnable(GL11.GL_BLEND);
    	//Allows changing by changing alpha value.
    	GL11.glDepthMask(false);
    	GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
    	drawCone(r, l, n, false);
    	if(highPower){
        	drawCone(r, l, n, false);
        	drawCone(r, l, n, false);
    	}
    	drawCone(r, l, n, true);
    	GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ZERO);
    	GL11.glDisable(GL11.GL_BLEND);
    	GL11.glEnable(GL11.GL_LIGHTING);
    	GL11.glEnable(GL11.GL_TEXTURE_2D);
    	GL11.glColor4f(1, 1, 1, 1);
		GL11.glPopMatrix();
    }
    
    private static void drawCone(double r, double l, double n, boolean reverse){
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glVertex3d(0, 0, 0);
    	if(reverse){
    		for(float theta=0; theta < 2*Math.PI + 0.1; theta += 2F*Math.PI/n){
    			GL11.glVertex3d(r*Math.cos(theta), r*Math.sin(theta), l);
    		}
    	}else{
    		for(float theta=(float) (2*Math.PI); theta>=0 - 0.1; theta -= 2F*Math.PI/n){
    			GL11.glVertex3d(r*Math.cos(theta), r*Math.sin(theta), l);
    		}
    	}
    	GL11.glEnd();
    }
    
    /**
     * Draws a square light with the specified color.
     * Also draws a cover over the light.
     */
	public static void drawLight(float red, float green, float blue, float alpha, float size){
		//Light cover
		GL11.glPushMatrix();
		GL11.glColor3f(1, 1, 1);
		bindTexture(glassTexture);
		renderSquare(-size/2, size/2, 0, size, 0.0002, 0.0002, false);
		GL11.glPopMatrix();
		
		//Light pre-operations
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glColor4f(red, green, blue, alpha);
		Minecraft.getMinecraft().entityRenderer.disableLightmap(0);
		
		//Light render
		GL11.glPushMatrix();
		renderSquare(-size/2, size/2, 0, size, 0.0001, 0.0001, false);
		GL11.glPopMatrix();
		
		//Light post operations.
		Minecraft.getMinecraft().entityRenderer.enableLightmap(0);
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glPopMatrix();
	}
	
    /**
     * Like drawLight, but draws a rounded 3D light instead.
     * Light is drawn with the bottom section missing, so rotate prior to rendering if you need that somewhere else.
     */
	public static void drawBulbLight(float red, float green, float blue, float alpha, float diameter, float height){
		//Light cover
		GL11.glPushMatrix();
		GL11.glColor3f(1, 1, 1);
		bindTexture(glassTexture);
		for(byte i=0; i<=3; ++i){
			GL11.glPushMatrix();
			GL11.glRotatef(90*i, 0, 1, 0);
			GL11.glTranslatef(0, 0, diameter/2);
			renderSquare(-diameter/2, diameter/2, 0, height, 0, 0, false);
			GL11.glPopMatrix();
		}
		renderQuad(-diameter/2, diameter/2, diameter/2, -diameter/2, height, height, height, height, -diameter/2, -diameter/2, diameter/2, diameter/2, true);
		GL11.glPopMatrix();
		
		//Light pre-operations
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glColor4f(red, green, blue, alpha);
		Minecraft.getMinecraft().entityRenderer.disableLightmap(0);
		
		//Light render
		GL11.glPushMatrix();
		for(byte i=0; i<=3; ++i){
			GL11.glPushMatrix();
			GL11.glRotatef(90*i, 0, 1, 0);
			GL11.glTranslatef(0, 0, diameter/2);
			renderSquare(-diameter/2, diameter/2, 0, height, -0.0001, -0.0001, false);
			GL11.glPopMatrix();
		}
		renderQuad(-diameter/2, diameter/2, diameter/2, -diameter/2, height-0.0001, height-0.0001, height-0.0001, height-0.0001, -diameter/2, -diameter/2, diameter/2, diameter/2, true);
		GL11.glPopMatrix();
		
		//Light post operations.
		Minecraft.getMinecraft().entityRenderer.enableLightmap(0);
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glPopMatrix();
	}
}
