package minecrafttransportsimulator.rendering;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.dataclasses.MTSControls.Controls;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Plane;

public final class RenderControls extends RenderInstruments{	
	public static void drawControl(EntityVehicleE_Powered vehicle, Controls control, boolean hud){
		textureManager.bindTexture(controlsTexture);
		GL11.glScalef(2.0F, 2.0F, 2.0F);
		switch(control){
			case PARKING_BRAKE: drawParkingBrake((EntityVehicleE_Powered) vehicle, hud); break;	
			case AIRCRAFT_THROTTLE: drawAircraftThrottle((EntityVehicleF_Air) vehicle, hud); break;
			case AIRCRAFT_FLAPS: drawFlapIndicator((EntityVehicleG_Plane) vehicle, hud); break;
		}
	}
	
	private static void drawParkingBrake(EntityVehicleE_Powered vehicle, boolean hud){
    	byte offset = (byte) (vehicle.parkingBrakeOn || vehicle.brakeOn ? -10 : 6);
		byte rotation = (byte) (vehicle.parkingBrakeOn || vehicle.brakeOn ? -90 : 0);
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
        	if(vehicle.parkingBrakeOn || vehicle.brakeOn){
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
	
	private static void drawAircraftThrottle(EntityVehicleF_Air aircraft, boolean hud){
		renderSquareUV(10.5F, 10.5F, 0.0F, 0.75F, 0.875F, 0.875F, 1);
		float offset = 3-(10 - aircraft.throttle/10F);
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
    
	private static void drawFlapIndicator(EntityVehicleG_Plane plane, boolean hud){
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
		
    	GL11.glTranslatef(0, 7-plane.flapDesiredAngle/25, 0);
    	textureManager.bindTexture(controlsTexture);
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

