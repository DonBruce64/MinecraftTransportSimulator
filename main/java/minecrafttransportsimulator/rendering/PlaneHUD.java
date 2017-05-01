package minecrafttransportsimulator.rendering;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.rendering.AircraftInstruments.AircraftControls;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.GL11DrawSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public final class PlaneHUD{
	private static ResourceLocation backplateTexture;
	private static ResourceLocation moldingTexture;
	
	public static void drawPlaneHUD(EntityPlane plane, int width, int height){
		startHUDDraw(plane);
		if(CameraSystem.hudMode == 3){
			drawLowerPlanePanel(width, height);
			drawLowerFlyableGauges(plane, width, height);
			height -= 64;
		}
		if(CameraSystem.hudMode == 2 || CameraSystem.hudMode == 3){
			drawUpperPlanePanel(width, height);
			drawLeftPlanePanel(width, height);
			drawRightPlanePanel(width, height);
			drawOuterFlyableGauges(plane, width, height);
			AircraftInstruments.drawFlyableControl(plane, 7*width/8 + 10, height - 18, AircraftControls.THROTTLE, true);
			AircraftInstruments.drawFlyableControl(plane, 15*width/16 + 14, height - 18, AircraftControls.BRAKE, true);
			if(plane.hasFlaps){
				AircraftInstruments.drawFlyableControl(plane, width/8 - 15, height - 19, AircraftControls.FLAPS, true);
			}
		}
		if(CameraSystem.hudMode > 0){
			drawMiddleFlyableGauges(plane, width, height);
		}
		endHUDDraw();
	}
	
	public static void startHUDDraw(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glColor4f(1, 1, 1, 1);
		Minecraft.getMinecraft().entityRenderer.enableLightmap();
		backplateTexture = plane.getBackplateTexture();
		moldingTexture = plane.getMouldingTexture();
	}
	
	public static void endHUDDraw(){
		GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
		Minecraft.getMinecraft().entityRenderer.disableLightmap(0);
		GL11.glPopMatrix();
	}
	
	public static boolean areLightsOn(EntityPlane plane){
		return (plane.lightStatus & 1) == 1 && plane.electricPower > 3;
	}
	
	private static void drawMiddleFlyableGauges(EntityPlane plane, int width, int height){
		for(byte i=1; i<4; ++i){
			if(plane.instruments.get(i) != null){
				AircraftInstruments.drawFlyableInstrument(plane, (5*i+6)*width/32, height - 32, plane.instruments.get(i), true, (byte) -1);
			}
		}
	}
	
	private static void drawOuterFlyableGauges(EntityPlane plane, int width, int height){
		GL11.glPushMatrix();
    	GL11.glScalef(0.75F, 0.75F, 0.75F);
    	if(plane.instruments.get((byte) 0) != null){
    		AircraftInstruments.drawFlyableInstrument(plane, width*17/64, (height - 24)*4/3, plane.instruments.get((byte) 0), true, (byte) -1);
    	}
    	if(plane.instruments.get((byte) 4) != null){
    		AircraftInstruments.drawFlyableInstrument(plane, width*17/16, (height - 24)*4/3, plane.instruments.get((byte) 4), true, (byte) -1);
    	}
    	GL11.glPopMatrix();
	}
	
	private static void drawLowerFlyableGauges(EntityPlane plane, int width, int height){
		for(byte i=5; i<10; ++i){
			if(plane.instruments.get(i) != null){
				AircraftInstruments.drawFlyableInstrument(plane, (5*(i-5)+6)*width/32, height - 32, plane.instruments.get(i), true, (byte) -1);
			}
		}
	}
	
	public static void drawUpperPlanePanel(int width, int height){
		GL11DrawSystem.bindTexture(backplateTexture);
    	GL11DrawSystem.renderQuadUV(width/4, width/4, 3*width/4, 3*width/4, height-64, height, height, height-64, 0, 0, 0, 0, 0, 3, 0, 1, false);
    	GL11DrawSystem.bindTexture(moldingTexture);
    	GL11DrawSystem.renderQuadUV(width/4, 3*width/4, 3*width/4, width/4, height-64, height-64, height-80, height-80, 0, 0, 0, 0, 0, 1, 0, 8, false);
    }
        
	public static void drawLeftPlanePanel(int width, int height){
    	GL11DrawSystem.bindTexture(backplateTexture);
    	GL11DrawSystem.renderQuadUVCustom(0, width/4, width/4, 0, height, height, height-64, height-32, 0, 0, 0, 0, 0, 1.5, 1.5, 0, 1, 1, 0, 0.5, false);
    	GL11DrawSystem.bindTexture(moldingTexture);
    	GL11DrawSystem.renderQuadUV(0, width/4, width/4, 0, height-32, height-64, height-80, height-48, 0, 0, 0, 0, 0, 1, 0, 4, false);
    }
    
	public static void drawRightPlanePanel(int width, int height){
    	GL11DrawSystem.bindTexture(backplateTexture);
    	GL11DrawSystem.renderQuadUVCustom(3*width/4, width, width, 3*width/4, height, height, height-32, height-64, 0, 0, 0, 0, 0, 1.5, 1.5, 0, 1, 1, 0.5, 0, false);
    	GL11DrawSystem.bindTexture(moldingTexture);
    	GL11DrawSystem.renderQuadUV(3*width/4, width, width, 3*width/4, height-63, height-32, height-48, height-80, 0, 0, 0, 0, 0, 1, 0, 4, false);
    }
    
	public static void drawLowerPlanePanel(int width, int height){
    	GL11DrawSystem.bindTexture(backplateTexture);
    	GL11DrawSystem.renderQuadUV(0, 0, width, width, height-64, height, height, height-64, 0, 0, 0, 0, 0, 6, 0, 1, false);
    }
	
	public static void drawPanelPanel(int width, int height){
		GL11DrawSystem.bindTexture(moldingTexture);
    	GL11DrawSystem.renderQuadUV(0, width, width, 0, height-112, height-112, height-128, height-128, 0, 0, 0, 0, 0, 1, 0, 16, false);
    	GL11DrawSystem.bindTexture(backplateTexture);
    	GL11DrawSystem.renderQuadUV(0, 0, width, width, height-112, height, height, height-112, 0, 0, 0, 0, 0, 6, 0, 1.75, false);
    }
}
