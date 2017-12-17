package minecrafttransportsimulator.rendering;

import minecrafttransportsimulator.dataclasses.MTSInstruments.Controls;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.main.EntityPlane;

import org.lwjgl.opengl.GL11;

public final class RenderControls extends RenderInstruments{	
	public static void drawControl(EntityMultipartVehicle vehicle, Controls control, boolean hud){
		textureManager.bindTexture(instrumentTexture);
		switch(control){
			case PARKING_BRAKE: drawParkingBrake((EntityPlane) vehicle, hud); break;	
			case AIRCRAFT_THROTTLE: drawAircraftThrottle((EntityPlane) vehicle, hud); break;
			case AIRCRAFT_FLAPS: drawFlapIndicator((EntityPlane) vehicle, hud); break;
		}
	}
	
	private static void drawParkingBrake(EntityPlane plane, boolean hud){
    	byte offset = (byte) (plane.parkingBrakeOn || plane.brakeOn ? -10 : 6);
		byte rotation = (byte) (plane.parkingBrakeOn || plane.brakeOn ? -90 : 0);
    	if(!hud){
    		renderSquareUV(10.5F, 10.5F, 0.0F, 0.75F, 0.875F, 0.875F, 1.0F);
        	
			GL11.glPushMatrix();
			GL11.glTranslatef(0, 1.75F, offset);
			renderSquareUV(3.5F, 0.0F, 20.0F, 0.640625F, 0.734375F, 0.890625F, 0.984375F);
			GL11.glRotatef(-90, 0, 0, 1);
			GL11.glTranslatef(1.75F, 1.75F, 0);
			renderSquareUV(3.5F, 0.0F, 20.0F, 0.640625F, 0.734375F, 0.890625F, 0.984375F);
			GL11.glRotatef(-90, 0, 0, 1);
			GL11.glTranslatef(1.75F, 1.75F, 0);
			renderSquareUV(3.5F, 0.0F, 20.0F, 0.640625F, 0.734375F, 0.890625F, 0.984375F);
			GL11.glRotatef(-90, 0, 0, 1);
			GL11.glTranslatef(1.75F, 1.75F, 0);
			renderSquareUV(3.5F, 0.0F, 20.0F, 0.640625F, 0.734375F, 0.890625F, 0.984375F);
			GL11.glPopMatrix();
    		
			GL11.glTranslatef(0, 0, offset-10);
			GL11.glRotatef(rotation, 0, 0, 1);
			GL11.glTranslatef(7F, 0, 0);
			renderSquareUV(35F, 10F, 0.0F, 0.2578125F, 0.3671875F, 0.921875F, 0.953125F);
			GL11.glTranslatef(0, 0, -0.01F);
			GL11.glRotatef(180, 0, 0, 1);
	    	drawScaledString("BRAKE", -15, -4, 0.5F);
    	}else{
    		renderSquareUV(10.5F, 10.5F, 0.0F, 0.75F, 0.875F, 0.875F, 1F);
        	if(plane.parkingBrakeOn || plane.brakeOn){
        		GL11.glTranslatef(0, 10F, 0);
        		renderSquareUV(3.5F, 20F, 0.0F, 0.640625F, 0.734375F, 0.890625F, 0.984375F);
        		GL11.glRotatef(-rotation, 0, 0, 1);
        		GL11.glTranslatef(7F, 0, 0);
        		renderSquareUV(35F, 10F, 0.0F, 0.2578125F, 0.3671875F, 0.921875F, 0.953125F);
    			GL11.glRotatef(180, 0, 0, 1);
    	    	drawScaledString("BRAKE", -15, -4, 0.5F);
        	}else{
        		GL11.glRotatef(180, 0, 0, 1);
        		GL11.glTranslatef(7F, -2F, 0);
        		renderSquareUV(35F, 10F, 0.0F, 0.2578125F, 0.3671875F, 0.921875F, 0.953125F);
    			GL11.glRotatef(180, 0, 0, 1);
    	    	drawScaledString("BRAKE", -15, -4, 0.5F);
        	}
    	}
    }
	
	private static void drawAircraftThrottle(EntityPlane plane, boolean hud){
		renderSquareUV(10.5F, 10.5F, 0.0F, 0.75F, 0.875F, 0.875F, 1);
		float offset = 3-plane.throttle/10F;
		if(!hud){
			GL11.glPushMatrix();
			GL11.glTranslatef(0, 1.75F, offset);
			renderSquareUV(3.5F, 0.0F, 20.0F, 0.640625F, 0.734375F, 0.890625F, 0.984375F);
			GL11.glRotatef(-90, 0, 0, 1);
			GL11.glTranslatef(1.75F, 1.75F, 0);
			renderSquareUV(3.5F, 0.0F, 20.0F, 0.640625F, 0.734375F, 0.890625F, 0.984375F);
			GL11.glRotatef(-90, 0, 0, 1);
			GL11.glTranslatef(1.75F, 1.75F, 0);
			renderSquareUV(3.5F, 0.0F, 20.0F, 0.640625F, 0.734375F, 0.890625F, 0.984375F);
			GL11.glRotatef(-90, 0, 0, 1);
			GL11.glTranslatef(1.75F, 1.75F, 0);
			renderSquareUV(3.5F, 0.0F, 20.0F, 0.640625F, 0.734375F, 0.890625F, 0.984375F);
			GL11.glPopMatrix();
			
			GL11.glTranslatef(0, 0, offset-10);
			renderSquareUV(14F, 14F, 0, 0.75F, 0.875F, 0.875F, 1);
			GL11.glRotatef(180, 0, 1, 0);
			renderSquareUV(14F, 14F, 0, 0.75F, 0.875F, 0.875F, 1);
    	}else{
    		GL11.glTranslated(0, 5-offset/2, 0);
    		renderSquareUV(3.5F, 10-offset, 0, 0.640625F, 0.734375F, 0.890625F, 0.984375F);
    		GL11.glTranslated(0, -offset/2, 0);
    		renderSquareUV(14F, 14F, 0, 0.75F, 0.875F, 0.875F, 1);
    	}
    }
    
	private static void drawFlapIndicator(EntityPlane plane, boolean hud){
		if(!hud){
			renderSquareUV(23F, 30F, 0, 0.515625F, 0.609375F, 0.875F, 1);
			GL11.glTranslatef(0, 0, -0.01F);
		}else{
			GL11.glRotatef(180, 0, 0, 1);
			renderSquareUV(23F, 30F, 0, 0.515625F, 0.609375F, 0.875F, 1);
		}
		GL11.glRotatef(90, 0, 0, 1);
		drawScaledString("FLAPS", -15, -15, 0.5F);
		GL11.glRotatef(90, 0, 0, 1);
		drawScaledString("0", 8, -15, 0.5F);
    	drawScaledString("35", 8, 10, 0.5F);
    	GL11.glRotatef(-180, 0, 0, 1);
		
    	GL11.glTranslatef(0, 7-plane.flapAngle/25, 0);
    	textureManager.bindTexture(instrumentTexture);
    	if(!hud){
    		GL11.glTranslatef(1.875F, 0.5F, -3.75F);
    		renderSquareUV(7.5F, 0, 7F, 0.421875F, 0.453125F, 0.921875F, 0.953125F);
    		GL11.glTranslatef(3.75F, -0.5F, 0);
    		GL11.glRotatef(-90, 0, 0, 1);
    		renderSquareUV(1F, 0, 7F, 0.421875F, 0.453125F, 0.921875F, 0.953125F);
    		GL11.glRotatef(-90, 0, 0, 1);
    		GL11.glTranslatef(3.75F, 0.5F, 0);
    		renderSquareUV(7.5F, 0, 7F, 0.421875F, 0.453125F, 0.921875F, 0.953125F);
    		GL11.glTranslatef(3.75F, -0.5F, 0);
    		GL11.glRotatef(-90, 0, 0, 1);
    		renderSquareUV(1F, 0, 7F, 0.421875F, 0.453125F, 0.921875F, 0.953125F);
    		GL11.glRotatef(-90, 0, 0, 1);
    		GL11.glTranslatef(3.75F, 0, -3.5F);
    		renderSquareUV(7F, 1F, 0, 0.421875F, 0.453125F, 0.921875F, 0.953125F);
    	}else{
    		GL11.glTranslatef(1.875F, -1.875F, 0);
    		renderSquareUV(7.5F, 7F, 0, 0.421875F, 0.453125F, 0.921875F, 0.953125F);
    	}
    }
}

