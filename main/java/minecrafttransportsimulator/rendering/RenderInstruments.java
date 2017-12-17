package minecrafttransportsimulator.rendering;

import java.awt.Color;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Instruments;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public abstract class RenderInstruments{
	protected static final TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
	protected static final ResourceLocation instrumentTexture = new ResourceLocation(MTS.MODID, "textures/instruments_aircraft.png");
	
	public static void drawInstrument(EntityMultipartVehicle vehicle, int x, int y, Instruments instrument, boolean hud, byte engineNumber){
		if(instrument.name().startsWith("AIRCRAFT_")){
			RenderInstrumentsAircraft.drawAircraftInstrument(vehicle, x, y, instrument, hud, engineNumber);
		}
	}
	
    /**
     * Renders a textured quad from the current bound texture of a specific width and height.
     * Used for rendering control and instrument textures off their texture sheets.
     */
	protected static void renderSquareUV(float width, float height, float depth, float u, float U, float v, float V){
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(u, v);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3f(width/2, height/2, depth/2);
		GL11.glTexCoord2f(u, V);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3f(width/2, -height/2, -depth/2);
		GL11.glTexCoord2f(U, V);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3f(-width/2, -height/2, -depth/2);
		GL11.glTexCoord2f(U, v);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3f(-width/2, height/2, depth/2);
		GL11.glEnd();
	}
	
    /**
     * Draws a series of white lines in a polar array.  Used for gauge markers.
     * Lines draw inward from offset, so offset should be the edge of the gauge.
     */
	protected static void drawDialIncrements(int centerX, int centerY, float startingAngle, float endingAngle, int offset, int length, int numberElements){
    	float angleIncrement = (endingAngle-startingAngle)/(numberElements-1);
        GL11.glPushMatrix();
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(2);
        for(float theta=startingAngle; theta<=endingAngle; theta+=angleIncrement){
        	GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2d(centerX+offset*Math.sin(Math.toRadians(theta)), centerY-offset*Math.cos(Math.toRadians(-theta)));
            GL11.glVertex2d(centerX+(offset-length)*Math.sin(Math.toRadians(theta)), centerY-(offset-length)*Math.cos(Math.toRadians(-theta)));
            GL11.glEnd();
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }
    /**
     * Draws numbers in a clockwise rotation offset from a center point.
     * Angles are in degrees.  The number size can be altered with the scale parameter.
     */
	protected static void drawDialNumbers(int centerX, int centerY, float startingAngle, float endingAngle,  int offset, int startingNumber, int numberDelta, int numberNumbers, float scale){
    	float angleIncrement = (endingAngle-startingAngle)/(numberNumbers);
    	float currentNumber = startingNumber;
    	float corrector=0;
    	for(float theta = startingAngle; currentNumber <= numberNumbers*numberDelta+startingNumber; theta += angleIncrement){
    		if(currentNumber>=100){
    			corrector=8.5F;
    		}else if(currentNumber>=10){
    			corrector=5.5F;
    		}else{
    			corrector=1.0F;
    		}
        	GL11.glPushMatrix();
        	GL11.glScalef(scale, scale, 1);
        	GL11.glTranslated(-corrector,-0.75F,0);
        	GL11.glTranslated(
        			(centerX + offset*Math.sin(Math.toRadians(theta)))/scale,
        			(centerY-offset*Math.cos(Math.toRadians(theta)))/scale,
        			0);
        	drawString(String.valueOf(Math.round(currentNumber)), Math.round(-3*scale), Math.round(-3*scale));
        	GL11.glPopMatrix();
        	currentNumber+=numberDelta;
        }
    }
    
    /**
     * Draws what can be considered a curved, colored line in an arc between the 
     * specified angles.  Offset is the distance from the center point to the outside
     * of the line, while color is a standard float color array.
     */
	protected static void drawDialColoring(int centerX, int centerY, float startingAngle, float endingAngle, int offset, int thickness, float[] colorRGB){
        GL11.glPushMatrix();
        GL11.glColor3f(colorRGB[0], colorRGB[1], colorRGB[2]);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(2);
        for(float theta = startingAngle; theta <= endingAngle; theta += 0.25F){
        	GL11.glBegin(GL11.GL_LINES);
	        GL11.glVertex2d(centerX+offset*Math.sin(Math.toRadians(theta)), centerY-offset*Math.cos(-Math.toRadians(theta)));
	        GL11.glVertex2d(centerX+(offset-thickness)*Math.sin(Math.toRadians(theta)), centerY-(offset-thickness)*Math.cos(-Math.toRadians(theta)));
	        GL11.glEnd();	        
	    }	    
	    GL11.glEnable(GL11.GL_TEXTURE_2D);
	    GL11.glColor3f(1, 1, 1);
        GL11.glPopMatrix();
    }
    
    /**
     * Draws a string without scaling with the bottom-left at x, y.
     */
	protected static void drawString(String string, int x, int y){
		Minecraft.getMinecraft().fontRendererObj.drawString(string, x, y, Color.WHITE.getRGB());
	}
	
    /**
     * Draws a scaled string with the bottom-left at x, y.
     */
	protected static void drawScaledString(String string, int x, int y, float scale){
    	GL11.glPushMatrix();
    	GL11.glScalef(scale, scale, scale);
    	Minecraft.getMinecraft().fontRendererObj.drawString(string, x, y, Color.WHITE.getRGB());
    	GL11.glPopMatrix();
    }
    
    /**
     * Rotates an object on the given coordinates.
     */
	protected static void rotationHelper(int x, int y, float angle){
        GL11.glTranslatef(x, y, 0);
        GL11.glRotatef(angle, 0, 0, 1);
        GL11.glTranslatef(-x, -y, 0);
    }
}

