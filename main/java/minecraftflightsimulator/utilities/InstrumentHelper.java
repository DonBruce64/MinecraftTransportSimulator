package minecraftflightsimulator.utilities;

import java.awt.Color;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityFlyable;
import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.entities.core.EntityVehicle;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/**Class responsible for drawing instruments and HUDs.
 * 
 * @author don_bruce
 */
public class InstrumentHelper{
	private static final ResourceLocation instrumentTexture = new ResourceLocation("mfs", "textures/instruments.png");
	private static byte numberEngines;
	private static double[] engineTemps = new double[4];
	private static double[] engineRPMs = new double[4];
	private static int maxEngineRPMs;
	private static float offset;
	
	public static void updateAircraftEngineProperties(EntityFlyable parent){
		numberEngines = 0;
		maxEngineRPMs = 2500;
		for(double[] property : parent.getEngineProperties()){
			engineTemps[numberEngines] = property[0];
			engineRPMs[numberEngines] = property[1];
			maxEngineRPMs = (int) Math.max(maxEngineRPMs, property[2] - (property[2] - 2500)/2);
			++numberEngines;
        }
	}
	
	public static void drawBasicFlyableHUD(EntityFlyable flyer, int width, int height, ResourceLocation backplateTexture, ResourceLocation moldingTexture){
		Minecraft.getMinecraft().entityRenderer.enableLightmap(0);
		if(RenderHelper.hudMode == 3){
			drawLowerConsole(width, height, backplateTexture, moldingTexture);
			for(int i=5; i<flyer.instrumentList.size(); ++i){
				if(flyer.instrumentList.get(i) != null){
					drawFlyableInstrument(flyer, (5*(i-5)+6)*width/32, height - 32, flyer.instrumentList.get(i).getItemDamage(), true);
				}
			}
			height -= 64;
		}
		if(RenderHelper.hudMode > 1){
			drawUpperConsole(width, height, backplateTexture, moldingTexture);
			drawLeftConsole(width, height, backplateTexture, moldingTexture);
			drawRightConsole(width, height, backplateTexture, moldingTexture);
	    	
	    	GL11.glPushMatrix();
	    	GL11.glScalef(0.75F, 0.75F, 0.75F);
	    	if(flyer.instrumentList.get(0) != null){
	    		drawFlyableInstrument(flyer, width*17/64, (height - 24)*4/3, flyer.instrumentList.get(0).getItemDamage(), true);
	    	}
	    	if(flyer.instrumentList.get(4) != null){
	    		drawFlyableInstrument(flyer, width*17/16, (height - 24)*4/3, flyer.instrumentList.get(4).getItemDamage(), true);
	    	}
	    	GL11.glPopMatrix();
	    	
			if(flyer instanceof EntityPlane){
				if(((EntityPlane) flyer).hasFlaps){
					drawFlapIndicator((EntityPlane) flyer, width/8 - 15, height - 19, true);
				}
			}
	    	drawThrottle(flyer, 7*width/8 + 10, height - 18, true);
	    	drawParkingBrake(flyer, 15*width/16 + 14, height - 18, true);
		}
		if(RenderHelper.hudMode != 0){
			for(int i=1; i<4; ++i){
				if(flyer.instrumentList.get(i) != null){
					drawFlyableInstrument(flyer, (5*i+6)*width/32, height - 32, flyer.instrumentList.get(i).getItemDamage(), true);
				}
			}
		}
		Minecraft.getMinecraft().entityRenderer.disableLightmap(0);
	}
	
	public static void drawFlyableInstrument(EntityFlyable flyer, int x, int y, int type, boolean hud){
		GL11.glPushMatrix();
		if(hud){
			offset = 0;
		}else{
			offset = -0.01F;
			GL11.glDisable(GL11.GL_LIGHTING);
		}
		if(flyer.lightsOn && type < 15){
			Minecraft.getMinecraft().entityRenderer.disableLightmap(0);
		}
		if(type == 0){
			drawAttitudeIndicator(flyer, x, y);
		}else if(type == 1){
			drawAltimeter(flyer, x, y);
		}else if(type == 2){
			drawHeadingIndicator(flyer, x, y);
		}else if(type == 3){
			drawAirspeedIndicator(flyer, x, y);
		}else if(type == 4){
			drawTurnCoordinator(flyer, x, y);
		}else if(type == 5){
			drawTurnAndSlipIndicator(flyer, x, y);
		}else if(type == 6){
			drawVerticalSpeedIndicator(flyer, x, y);
		}else if(type == 7){
			drawLiftReserveIndicator(flyer, x, y);
		}else if(type == 8){
			//DUMMY
		}else if(type == 9){
			//DUMMY
		}else if(type == 10){
			drawTachometer(flyer, x, y);
		}else if(type == 11){
			drawFuelGauge(flyer, x, y);
		}else if(type == 12){
			drawFuelFlowGauge(flyer, x, y);
		}else if(type == 13){
			drawEngineTempGauge(flyer, x, y);
		}else if(type == 14){
			//drawOilPressureGauge()
		}else if(type == 15){
			drawThrottle(flyer, x, y, hud);
		}else if(type == 16){
			drawParkingBrake(flyer, x, y, hud);
		}else if(type == 17){
			drawFlapIndicator((EntityPlane) flyer, x, y, hud);
		}
		
		if(flyer.lightsOn && type < 15){
			GL11.glTranslatef(0, 0, 11*offset);
			if(!hud){
				GL11.glEnable(GL11.GL_BLEND);
			}
			RenderHelper.bindTexture(instrumentTexture);
			RenderHelper.renderSquareUV(x-30, x+30, y+30, y-30, 0, 0, 0.5, 0.75, 0.25, 0.5, false);
	        if(!hud){
	        	GL11.glDisable(GL11.GL_BLEND);
	        }
			Minecraft.getMinecraft().entityRenderer.enableLightmap(0);
		}
		if(!hud){
        	GL11.glEnable(GL11.GL_LIGHTING);
        }
    	GL11.glPopMatrix();
	}
    
	private static void drawUpperConsole(int width, int height, ResourceLocation backplateTexture, ResourceLocation moldingTexture){
		RenderHelper.bindTexture(backplateTexture);
    	RenderHelper.renderQuadUV(width/4, width/4, 3*width/4, 3*width/4, height-64, height, height, height-64, 0, 0, 0, 0, 0, 3, 0, 1, false);
    	RenderHelper.bindTexture(moldingTexture);
    	RenderHelper.renderQuadUV(width/4, 3*width/4, 3*width/4, width/4, height-64, height-64, height-80, height-80, 0, 0, 0, 0, 0, 1, 0, 8, false);
    }
        
	private static void drawLeftConsole(int width, int height, ResourceLocation backplateTexture, ResourceLocation moldingTexture){
    	RenderHelper.bindTexture(backplateTexture);
    	RenderHelper.renderQuadUVCustom(0, width/4, width/4, 0, height, height, height-64, height-32, 0, 0, 0, 0, 0, 1.5, 1.5, 0, 1, 1, 0, 0.5, false);
    	RenderHelper.bindTexture(moldingTexture);
    	RenderHelper.renderQuadUV(0, width/4, width/4, 0, height-32, height-64, height-80, height-48, 0, 0, 0, 0, 0, 1, 0, 4, false);
    }
    
	private static void drawRightConsole(int width, int height, ResourceLocation backplateTexture, ResourceLocation moldingTexture){
    	RenderHelper.bindTexture(backplateTexture);
    	RenderHelper.renderQuadUVCustom(3*width/4, width, width, 3*width/4, height, height, height-32, height-64, 0, 0, 0, 0, 0, 1.5, 1.5, 0, 1, 1, 0.5, 0, false);
    	RenderHelper.bindTexture(moldingTexture);
    	RenderHelper.renderQuadUV(3*width/4, width, width, 3*width/4, height-63, height-32, height-48, height-80, 0, 0, 0, 0, 0, 1, 0, 4, false);
    }
    
	private static void drawLowerConsole(int width, int height, ResourceLocation backplateTexture, ResourceLocation moldingTexture){
    	RenderHelper.bindTexture(backplateTexture);
    	RenderHelper.renderQuadUV(0, 0, width, width, height-64, height, height, height-64, 0, 0, 0, 0, 0, 6, 0, 1, false);
    }
    
	private static void drawThrottle(EntityVehicle vehicle, int centerX, int centerY, boolean hud){		
    	RenderHelper.bindTexture(instrumentTexture);
		if(!hud){
    		RenderHelper.renderSquareUV(centerX-5.25, centerX+5.25, centerY+5.25, centerY-5.25, 0, 0, 0.75, 0.875, 0.875, 1, false);
    		
    		RenderHelper.renderQuadUV(centerX-1.75, centerX-1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY+1.75, centerY+1.75, -7-vehicle.throttle/10F, 0, 0, -7-vehicle.throttle/10F, 0.640625, 0.734375, 0.890625, 0.984375, false);
    		RenderHelper.renderQuadUV(centerX+1.75, centerX+1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY-1.75, centerY-1.75, -7-vehicle.throttle/10F, 0, 0, -7-vehicle.throttle/10F, 0.640625, 0.734375, 0.890625, 0.984375, false);
        	RenderHelper.renderQuadUV(centerX+1.75, centerX+1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY-1.75, centerY-1.75, -7-vehicle.throttle/10F, 0, 0, -7-vehicle.throttle/10F, 0.640625, 0.734375, 0.890625, 0.984375, false);
        	RenderHelper.renderQuadUV(centerX-1.75, centerX-1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY+1.75, centerY+1.75, -7-vehicle.throttle/10F, 0, 0, -7-vehicle.throttle/10F, 0.640625, 0.734375, 0.890625, 0.984375, false);
        	
        	RenderHelper.renderSquareUV(centerX-7, centerX+7, centerY+7, centerY-7, -7-vehicle.throttle/10F, -7-vehicle.throttle/10F, 0.75, 0.875, 0.875, 1, true);
    	}else{
        	RenderHelper.renderSquareUV(centerX-5.25, centerX+5.25, centerY+0.175, centerY-10.5, 0, 0, 0.75, 0.875, 0.875, 1, false);
        	RenderHelper.renderSquareUV(centerX-1.75, centerX+1.75, centerY+7+vehicle.throttle/10, centerY-7, 0, 0, 0.640625, 0.734375, 0.890625, 0.984375, false);
        	RenderHelper.renderSquareUV(centerX-7, centerX+7, centerY+7+vehicle.throttle/10F, centerY-7+vehicle.throttle/10F, 0, 0, 0.75, 0.875, 0.875, 1, false);
    	}
    }
    
	private static void drawFlapIndicator(EntityPlane plane, int centerX, int centerY, boolean hud){
    	RenderHelper.bindTexture(instrumentTexture);
    	RenderHelper.renderSquareUV(centerX-11.25, centerX+11.25, centerY+15, centerY-15, 0, 0, 0.515625, 0.609375, 0.875, 1, false);
    	
    	GL11.glTranslatef(0, 0, offset);
    	
        GL11.glPushMatrix();
    	rotationHelper(centerX, centerY, -90);
    	drawScaledString("FLAPS", centerX*2-15, centerY*2-15, 0.5F);
    	GL11.glPopMatrix();
    	
    	GL11.glPushMatrix();
    	drawScaledString("0", centerX*2+8, centerY*2-16, 0.5F);
    	drawScaledString("35", centerX*2+8, centerY*2+10, 0.5F);
    	GL11.glPopMatrix();
        
    	RenderHelper.bindTexture(instrumentTexture);
    	if(!hud){
    		RenderHelper.renderQuadUV(centerX-5.625, centerX-5.625, centerX-5.625, centerX-5.625, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, -7, 0, 0, -7, 0.421875, 0.453125, 0.921875, 0.953125, false);
    		RenderHelper.renderQuadUV(centerX+1.875, centerX+1.875, centerX+1.875, centerX+1.875, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, -7, 0, 0, -7, 0.421875, 0.453125, 0.921875, 0.953125, false);
        	RenderHelper.renderQuadUV(centerX-5.625, centerX-5.625, centerX+1.875, centerX+1.875, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, 0, -7, -7, 0, 0.421875, 0.453125, 0.921875, 0.953125, false);
        	RenderHelper.renderQuadUV(centerX-5.625, centerX-5.625, centerX+1.875, centerX+1.875, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, -7, 0, 0, -7, 0.421875, 0.453125, 0.921875, 0.953125, false);
        	RenderHelper.renderQuadUV(centerX-5.625, centerX-5.625, centerX+1.875, centerX+1.875, centerY-8+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-8+plane.flapAngle/25, -7, -7, -7, -7, 0.421875, 0.453125, 0.921875, 0.953125, false);
    	}else{
    		RenderHelper.renderSquareUV(centerX-5.625, centerX+1.875, centerY-0.5+plane.flapAngle/25, centerY-8+plane.flapAngle/25, 0, 0, 0.421875, 0.453125, 0.921875, 0.953125, false);
    	}
    }
    
	private static void drawParkingBrake(EntityVehicle vehicle, int centerX, int centerY, boolean hud){
    	RenderHelper.bindTexture(instrumentTexture);
    	
    	if(!hud){
    		RenderHelper.renderSquareUV(centerX-5.25, centerX+5.25, centerY+5.25, centerY-5.25, 0, 0, 0.75, 0.875, 0.875, 1, false);
        	
    		if(vehicle.parkingBrakeOn || vehicle.brakeOn){        		
        		RenderHelper.renderQuadUV(centerX-1.75, centerX-1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY+1.75, centerY+1.75, -20, 0, 0, -20, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		RenderHelper.renderQuadUV(centerX+1.75, centerX+1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY-1.75, centerY-1.75, -20, 0, 0, -20, 0.640625, 0.734375, 0.890625, 0.984375, false);
            	RenderHelper.renderQuadUV(centerX+1.75, centerX+1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY-1.75, centerY-1.75, -20, 0, 0, -20, 0.640625, 0.734375, 0.890625, 0.984375, false);
            	RenderHelper.renderQuadUV(centerX-1.75, centerX-1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY+1.75, centerY+1.75, -20, 0, 0, -20, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		
        		RenderHelper.renderQuadUV(centerX-5, centerX+5, centerX+5, centerX-5, centerY+25, centerY+25, centerY-10, centerY-10, -20, -20, -20, -20, 0.2578125, 0.3671875, 0.921875, 0.953125, false);
    	        GL11.glTranslatef(0, 0, -20.01F);
    	        
    	        GL11.glPushMatrix();
    	    	rotationHelper(centerX, centerY, -90);
    	    	drawScaledString("BRAKE", centerX*2-30, centerY*2-4, 0.5F);
    	    	GL11.glPopMatrix();
        	}else{
        		RenderHelper.renderQuadUV(centerX-1.75, centerX-1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY+1.75, centerY+1.75, -2, 0, 0, -2, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		RenderHelper.renderQuadUV(centerX+1.75, centerX+1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY-1.75, centerY-1.75, -2, 0, 0, -2, 0.640625, 0.734375, 0.890625, 0.984375, false);
            	RenderHelper.renderQuadUV(centerX+1.75, centerX+1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY-1.75, centerY-1.75, -2, 0, 0, -2, 0.640625, 0.734375, 0.890625, 0.984375, false);
            	RenderHelper.renderQuadUV(centerX-1.75, centerX-1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY+1.75, centerY+1.75, -2, 0, 0, -2, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		
    	        RenderHelper.renderSquareUV(centerX-22.5, centerX+12.5, centerY+5, centerY-5, -2, -2, 0.2578125, 0.3671875, 0.921875, 0.953125, false);    		
    	        GL11.glTranslatef(0, 0, -2.01F);
    	        drawScaledString("BRAKE", centerX*2-25, centerY*2-4, 0.5F);
        	}
    	}else{
    		RenderHelper.renderSquareUV(centerX-5.25, centerX+5.25, centerY+0.175, centerY-10.5, 0, 0, 0.75, 0.875, 0.875, 1, true);
        	if(vehicle.parkingBrakeOn || vehicle.brakeOn){
        		RenderHelper.renderSquareUV(centerX-1.75, centerX+1.75, centerY+15, centerY-7, 0, 0, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		RenderHelper.renderQuadUV(centerX-5, centerX+5, centerX+5, centerX-5, centerY+35, centerY+35, centerY, centerY, 0, 0, 0, 0, 0.2578125, 0.3671875, 0.921875, 0.953125, false);
    	        
    	        GL11.glPushMatrix();
    	    	rotationHelper(centerX, centerY, -90);
    	        drawScaledString("BRAKE", centerX*2-50, centerY*2-4, 0.5F);
    	    	GL11.glPopMatrix();
        	}else{
        		RenderHelper.renderSquareUV(centerX-1.75, centerX+1.75, centerY+5, centerY-7, 0, 0, 0.640625, 0.734375, 0.890625, 0.984375, false);
    	        RenderHelper.renderSquareUV(centerX-22.5, centerX+12.5, centerY+5, centerY-5, 0, 0, 0.2578125, 0.3671875, 0.921875, 0.953125, false);    		
    	        drawScaledString("BRAKE", centerX*2-25, centerY*2-4, 0.5F);
        	}
    	}
    }
	
	private static void drawGaugeBase(EntityVehicle vehicle, int centerX, int centerY){
		if(vehicle.lightsOn){
			Minecraft.getMinecraft().entityRenderer.enableLightmap(0);
			RenderHelper.bindTexture(instrumentTexture);
	    	RenderHelper.renderSquareUV(centerX-30, centerX+30, centerY+30, centerY-30, 0, 0, 0.75, 1, 0, 0.25, false);
	    	Minecraft.getMinecraft().entityRenderer.disableLightmap(0);
		}else{
			RenderHelper.bindTexture(instrumentTexture);
	    	RenderHelper.renderSquareUV(centerX-30, centerX+30, centerY+30, centerY-30, 0, 0, 0.75, 1, 0, 0.25, false);
		}
    }
    
	private static void drawAttitudeIndicator(EntityFlyable flyer, int centerX, int centerY){
		GL11.glPushMatrix();
		RenderHelper.bindTexture(instrumentTexture);
		
		rotationHelper(centerX, centerY, -flyer.rotationRoll);
		if(flyer.rotationPitch >= 24){
			RenderHelper.renderQuadUV(centerX-20, centerX+20, centerX+20, centerX-20, centerY+20, centerY+20, centerY-20, centerY-20, 0, 0, 0, 0, 0.25, 0.5625, 0.53125, 0.84375, false);
		}else if(flyer.rotationPitch <= -24){
			RenderHelper.renderQuadUV(centerX-20, centerX+20, centerX+20, centerX-20, centerY+20, centerY+20, centerY-20, centerY-20, 0, 0, 0, 0, 0.4375, 0.75, 0.53125, 0.84375, false);
		}else{
			RenderHelper.renderQuadUV(centerX-20, centerX+20, centerX+20, centerX-20, centerY+20, centerY+20, centerY-20, centerY-20, 0, 0, 0, 0, 0.34375 - flyer.rotationPitch*0.00390625, 0.65625 - 0.00390625*flyer.rotationPitch, 0.53125,  0.84375, false);
		}
    	
		GL11.glTranslatef(0, 0, offset);
    	RenderHelper.renderSquareUV(centerX-30, centerX+30, centerY+30, centerY-30, 0, 0, 0.25, 0.5, 0, 0.25, false);
    	
    	GL11.glTranslatef(0, 0, offset);
    	rotationHelper(centerX, centerY, flyer.rotationRoll);
    	if(flyer.lightsOn){
			Minecraft.getMinecraft().entityRenderer.enableLightmap(0);
			RenderHelper.renderSquareUV(centerX-30, centerX+30, centerY+30, centerY-30, 0, 0, 0.5, 0.75, 0, 0.25, false);
	    	Minecraft.getMinecraft().entityRenderer.disableLightmap(0);
		}else{
			RenderHelper.renderSquareUV(centerX-30, centerX+30, centerY+30, centerY-30, 0, 0, 0.5, 0.75, 0, 0.25, false);
		}
		GL11.glPopMatrix();
	}
	
	private static void drawAltimeter(EntityFlyable flyer, int centerX, int centerY){
    	drawGaugeBase(flyer, centerX, centerY);
    	GL11.glTranslatef(0, 0, offset);
    	
    	drawScaledString("ALTITUDE", centerX*2-20, centerY*2+14, 0.5F);
        drawDialIncrements(centerX, centerY, -180, 180, 25, 2, 51);
        drawDialIncrements(centerX, centerY, -180, 180, 25, 5, 11);
        drawDialNumbers(centerX, centerY, 0, 320,  17, 0, 1, 9, 0.7F);
        GL11.glTranslatef(0, 0, offset);
        drawShortPointer(centerX, centerY, (float) (.36*(flyer.posY - (ControlHelper.seaLevelOffset ? 64 : 0))), 20, 6);
        GL11.glTranslatef(0, 0, offset);
        drawLongPointer(centerX, centerY, (float) (3.6*(flyer.posY - (ControlHelper.seaLevelOffset ? 64 : 0))), 35, 3);
    }
    
	private static void drawHeadingIndicator(EntityFlyable flyer, int centerX, int centerY){
    	drawGaugeBase(flyer, centerX, centerY);
    	GL11.glTranslatef(0, 0, offset);
    	
    	RenderHelper.bindTexture(instrumentTexture);
    	RenderHelper.renderSquareUV(centerX-20, centerX+20, centerY+20, centerY-20, 0, 0, 0.75, 1, 0.25, 0.5, false);
    	
    	drawScaledString("HEADING", centerX*2-18, centerY*2+14, 0.5F);
    	
        GL11.glPushMatrix();
        rotationHelper(centerX, centerY, -flyer.rotationYaw);
        drawDialIncrements(centerX, centerY, 0, 360, 25, 5, 25);
        drawDialIncrements(centerX, centerY, 7.5F, 367.5F, 25, 3, 25);
        GL11.glScalef(0.60F, 0.60F, 0.60F);
        centerX=Math.round(((float)centerX)*(1/0.60F));
        centerY=Math.round(((float)centerY)*(1/0.60F));
        RenderHelper.drawString("S", centerX-3, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        RenderHelper.drawString("3", centerX-3, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        RenderHelper.drawString("6", centerX-3, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        RenderHelper.drawString("W", centerX-2, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        RenderHelper.drawString("12", centerX-4, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        RenderHelper.drawString("15", centerX-4, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        RenderHelper.drawString("N", centerX-2, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        RenderHelper.drawString("21", centerX-4, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        RenderHelper.drawString("24", centerX-4, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        RenderHelper.drawString("E", centerX-3, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        RenderHelper.drawString("30", centerX-5, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        RenderHelper.drawString("33", centerX-5, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        GL11.glPopMatrix();
    }
    
	private static void drawAirspeedIndicator(EntityFlyable flyer, int centerX, int centerY){
    	drawGaugeBase(flyer, centerX, centerY);
    	GL11.glTranslatef(0, 0, offset);

    	drawScaledString("BLK/S", centerX*2-15, centerY*2+14, 0.5F);
    	drawDialColoring(centerX, centerY, 292.5F, 330, 25, 3, new float[] {1, 0, 0});
    	drawDialColoring(centerX, centerY, 217.5F, 292.5F, 25, 3, new float[] {1, 1, 0});
    	drawDialColoring(centerX, centerY, 105F, 217.5F, 25, 3, new float[] {0, 1, 0});
    	GL11.glTranslatef(0, 0, offset);
    	
    	drawDialIncrements(centerX, centerY, 30, 330, 25, 8, 9);
    	drawDialIncrements(centerX, centerY, 30, 330, 25, 3, 41);
    	drawDialNumbers(centerX, centerY, 30, 330, 15, 0, 10, 4, 0.6F);
    	drawLongPointer(centerX, centerY, (float) (30+7.5*flyer.velocity*MFS.planeSpeedFactor*20), 35, 2);
    }
    
	private static void drawTurnCoordinator(EntityFlyable flyer, int centerX, int centerY){
		RenderHelper.bindTexture(instrumentTexture);
		drawGaugeBase(flyer, centerX, centerY);
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 0, offset);
    	
    	drawDialIncrements(centerX, centerY, -90, 90, 20, 5, 2);
    	drawDialIncrements(centerX, centerY, -115, 115, 20, 5, 2);
    	
    	RenderHelper.renderSquareUV(centerX-25, centerX+25, centerY+18.75, centerY+6.25, 0, 0, 0.75, 1, 0.5625, 0.625, false);
    	GL11.glTranslatef(0, 0, offset);
    	
    	float turn = Math.max(Math.min(((flyer.rotationRoll - flyer.prevRotationRoll)/10 + flyer.rotationYaw - flyer.prevRotationYaw)/0.15F*25F, 50), -50);
    	rotationHelper(centerX, centerY, turn);
    	RenderHelper.renderSquareUV(centerX-25, centerX+25, centerY+6.25, centerY-6.25, 0, 0, 0.75, 1, 0.5, 0.5625, false);
    	rotationHelper(centerX, centerY, -turn);
    	
    	double slip = flyer.sideVec.dot(flyer.velocityVec);
    	RenderHelper.renderSquareUV(centerX-2.5 + 20*slip, centerX+2.5 + 20*slip, centerY+15 - Math.abs(slip), centerY+10 - Math.abs(slip), 0, 0, 0.75, 0.875, 0.875, 1, false);
    	GL11.glTranslatef(0, 0, offset);

    	drawScaledString("L", centerX*2-34, centerY*2+20, 0.5F);
    	drawScaledString("R", centerX*2+30, centerY*2+20, 0.5F);
    	drawScaledString("M.J.", centerX*2-8, centerY*2-46, 0.5F);
    	drawScaledString("ELEC", centerX*2-12, centerY*2-36, 0.5F);
    	drawScaledString("2 MIN", centerX*2-14, centerY*2+36, 0.5F);
    	GL11.glPopMatrix();
	}
	
	private static void drawTurnAndSlipIndicator(EntityFlyable flyer, int centerX, int centerY){
		RenderHelper.bindTexture(instrumentTexture);
		drawGaugeBase(flyer, centerX, centerY);
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 0, offset);
    	
    	RenderHelper.renderSquareUV(centerX-25, centerX+25, centerY+18.75, centerY+6.25, 0, 0, 0.75, 1, 0.5625, 0.625, false);
    	
    	GL11.glDisable(GL11.GL_TEXTURE_2D);
    	GL11.glColor3f(1, 1, 1);
    	GL11.glBegin(GL11.GL_QUADS);
    	GL11.glVertex2d(centerX - 2, centerY - 18);
    	GL11.glVertex2d(centerX + 2, centerY - 18);
    	GL11.glVertex2d(centerX + 3, centerY - 20);
    	GL11.glVertex2d(centerX - 3, centerY - 20);
    	
    	GL11.glVertex2d(centerX - 3, centerY - 20);
    	GL11.glVertex2d(centerX + 3, centerY - 20);
    	GL11.glVertex2d(centerX + 3, centerY - 24);
    	GL11.glVertex2d(centerX - 3, centerY - 24);
    	GL11.glEnd();
    	
    	rotationHelper(centerX, centerY, 25);
    	GL11.glBegin(GL11.GL_QUADS);
    	GL11.glVertex2d(centerX - 2, centerY - 18);
    	GL11.glVertex2d(centerX + 2, centerY - 18);
    	GL11.glVertex2d(centerX + 2, centerY - 20);
    	GL11.glVertex2d(centerX - 2, centerY - 20);
    	GL11.glEnd();
    	GL11.glBegin(GL11.GL_TRIANGLES);
    	GL11.glVertex2d(centerX, centerY - 22);
    	GL11.glVertex2d(centerX - 2, centerY - 20);
    	GL11.glVertex2d(centerX + 2, centerY - 20);
    	GL11.glEnd();
    	
    	rotationHelper(centerX, centerY, -50);
    	GL11.glBegin(GL11.GL_QUADS);
    	GL11.glVertex2d(centerX - 2, centerY - 18);
    	GL11.glVertex2d(centerX + 2, centerY - 18);
    	GL11.glVertex2d(centerX + 2, centerY - 20);
    	GL11.glVertex2d(centerX - 2, centerY - 20);
    	GL11.glEnd();
    	GL11.glBegin(GL11.GL_TRIANGLES);
    	GL11.glVertex2d(centerX, centerY - 22);
    	GL11.glVertex2d(centerX - 2, centerY - 20);
    	GL11.glVertex2d(centerX + 2, centerY - 20);
    	GL11.glEnd();
    	
    	float turn = Math.max(Math.min((flyer.rotationYaw - flyer.prevRotationYaw)/0.15F*25F, 50), -50);
    	rotationHelper(centerX, centerY, turn + 25);
    	GL11.glBegin(GL11.GL_QUADS);
    	GL11.glVertex2d(centerX - 2, centerY - 10);
    	GL11.glVertex2d(centerX + 2, centerY - 10);
    	GL11.glVertex2d(centerX + 2, centerY - 18);
    	GL11.glVertex2d(centerX - 2, centerY - 18);
    	GL11.glEnd();
    	
    	GL11.glColor3f(0, 0, 0);
    	GL11.glBegin(GL11.GL_QUADS);
    	GL11.glVertex2d(centerX - 2, centerY + 3);
    	GL11.glVertex2d(centerX + 2, centerY + 3);
    	GL11.glVertex2d(centerX + 2, centerY - 10);
    	GL11.glVertex2d(centerX - 2, centerY - 10);
    	
    	GL11.glVertex2d(centerX - 3, centerY + 3);
    	GL11.glVertex2d(centerX + 3, centerY + 3);
    	GL11.glVertex2d(centerX + 3, centerY - 3);
    	GL11.glVertex2d(centerX - 3, centerY - 3);
    	GL11.glEnd();
    	rotationHelper(centerX, centerY, -turn);
    	GL11.glColor3f(1, 1, 1);
    	GL11.glEnable(GL11.GL_TEXTURE_2D);
    	
    	GL11.glTranslatef(0, 0, offset);
    	double slip = flyer.sideVec.dot(flyer.velocityVec);
    	RenderHelper.renderSquareUV(centerX-2.5 + 20*slip, centerX+2.5 + 20*slip, centerY+15 - Math.abs(slip), centerY+10 - Math.abs(slip), 0, 0, 0.75, 0.875, 0.875, 1, false);
    	GL11.glTranslatef(0, 0, -offset);

    	drawScaledString("L", centerX*2-30, centerY*2-30, 0.5F);
    	drawScaledString("R", centerX*2+26, centerY*2-30, 0.5F);
    	drawScaledString("2 MIN", centerX*2-14, centerY*2+36, 0.5F);
    	GL11.glPopMatrix();
	}
	
	private static void drawVerticalSpeedIndicator(EntityFlyable flyer, int centerX, int centerY){
		drawGaugeBase(flyer, centerX, centerY);
		GL11.glTranslatef(0, 0, 2*offset);

    	drawScaledString("CLIMB", centerX*2-14, centerY*2-14, 0.5F);
    	drawScaledString("BLK/S", centerX*2-14, centerY*2+10, 0.5F);
    	drawDialNumbers(centerX, centerY, -90, 90, 16, 0, 1, 4, 0.7F);
    	drawDialNumbers(centerX, centerY, -132.5F, -217.5F, 16, 1, 1, 2, 0.7F);
    	drawDialIncrements(centerX, centerY, -260, 80, 25, 5, 9);
    	drawDialIncrements(centerX, centerY, -132.5F, -47.5F, 25, 3, 11);
    	drawDialIncrements(centerX, centerY, -47.5F, 80, 25, 2, 16);
    	drawDialIncrements(centerX, centerY, -260, -132.5F, 25, 2, 16);
    	GL11.glTranslatef(0, 0, offset);
    	drawLongPointer(centerX, centerY, (float) (-90+10.625*flyer.motionY*20), 35, 2);
	}
	
	private static void drawLiftReserveIndicator(EntityFlyable flyer, int centerX, int centerY){
		drawGaugeBase(flyer, centerX, centerY);
    	GL11.glPushMatrix();
    	GL11.glTranslatef(0, 0, offset);
    	
    	drawScaledString("LIFT RESERVE", centerX*2-32, centerY*2+14, 0.5F);
    	drawDialColoring(centerX, centerY+20, -37, -35, 35, 10, new float[] {0, 0, 0});
    	drawDialColoring(centerX, centerY+20, -35, -26, 32, 7, new float[] {1, 0, 0});
    	drawDialColoring(centerX, centerY+20, -26, -24, 32, 7, new float[] {0, 0, 0});
    	drawDialColoring(centerX, centerY+20, -24, -11, 32, 7, new float[] {1, 1, 0});
    	drawDialColoring(centerX, centerY+20, -11, -9, 32, 7, new float[] {0, 0, 0});
    	drawDialColoring(centerX, centerY+20, -9, 35, 32, 7, new float[] {0, 1, 0});
    	drawDialColoring(centerX, centerY+20, -35, 35, 35, 3, new float[] {1, 1, 1});
    	drawDialColoring(centerX, centerY+20, 35, 37, 35, 10, new float[] {0, 0, 0});
    	GL11.glTranslatef(0, 0, offset);
    	
    	GL11.glDisable(GL11.GL_TEXTURE_2D);
    	GL11.glColor3f(0, 0, 0);
        for(float theta=-33; theta<=33; theta+=3){
        	GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2d(centerX+34.5*Math.sin(Math.toRadians(theta)), centerY+20-34.5*Math.cos(Math.toRadians(-theta)));
            GL11.glVertex2d(centerX+(34.5-2)*Math.sin(Math.toRadians(theta)), centerY+20-(34.5-2)*Math.cos(Math.toRadians(-theta)));
            GL11.glEnd();
        }
    	

        float angle = (float) Math.max(Math.min(flyer.trackAngle*3 + 20, 35), -35);
        rotationHelper(centerX, centerY + 20, angle);    	
    	GL11.glColor3f(0, 0, 0);
    	GL11.glBegin(GL11.GL_QUADS);
    	GL11.glVertex2d(centerX - 0.5, centerY - 5);
    	GL11.glVertex2d(centerX + 0.5, centerY - 5);
    	GL11.glVertex2d(centerX + 0.5, centerY - 18);
    	GL11.glVertex2d(centerX - 0.5, centerY - 18);
    	GL11.glEnd();
    	
    	GL11.glColor3f(1, 1, 1);
    	GL11.glBegin(GL11.GL_QUADS);
    	GL11.glVertex2d(centerX - 1.5, centerY + 20);
    	GL11.glVertex2d(centerX + 1.5, centerY + 20);
    	GL11.glVertex2d(centerX + 1.5, centerY + 12);
    	GL11.glVertex2d(centerX - 1.5, centerY + 12);
    	
    	GL11.glVertex2d(centerX - 0.5, centerY + 12);
    	GL11.glVertex2d(centerX + 0.5, centerY + 12);
    	GL11.glVertex2d(centerX + 0.5, centerY - 5);
    	GL11.glVertex2d(centerX - 0.5, centerY - 5);
    	GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
	}
	
	private static void drawTachometer(EntityVehicle vehicle, int centerX, int centerY){
    	drawGaugeBase(vehicle, centerX, centerY);
    	GL11.glTranslatef(0, 0, offset);
    	drawScaledString("RPM", centerX*2-10, centerY*2+14, 0.5F);
    	drawDialColoring(centerX, centerY, -135+maxEngineRPMs/10, 165, 25, 4, new float[] {1, 0, 0});
    	GL11.glTranslatef(0, 0, offset);
		drawDialIncrements(centerX, centerY, -135, 165, 25, 5, 61);
		drawDialIncrements(centerX, centerY, -135, 165, 25, 9, 13);
        drawDialNumbers(centerX, centerY, -135, 165, 13, 0, 5, 6, 0.6F);
        for(byte i=0; i<numberEngines; ++i){
        	drawLongPointer(centerX, centerY, (float) (-135+engineRPMs[i]/10), 30, 3);
        	GL11.glTranslatef(0, 0, offset);
        }
    }
	
	
    
	private static void drawFuelGauge(EntityVehicle vehicle, int centerX, int centerY){
    	drawGaugeBase(vehicle, centerX, centerY);
    	GL11.glTranslatef(0, 0, offset);
    	
    	drawScaledString("BUCKETS", centerX*2-20, centerY*2+14, 0.5F);
    	drawScaledString("FUEL", centerX*2-10, centerY*2+24, 0.5F);
    	drawScaledString("0", centerX*2-40, centerY*2-10, 0.5F);
    	drawScaledString(String.valueOf(vehicle.maxFuel/1000/2F), centerX*2-7, centerY*2-45, 0.5F);
    	drawScaledString(String.valueOf(vehicle.maxFuel/1000), centerX*2+35, centerY*2-10, 0.5F);
        drawDialIncrements(centerX, centerY+8, -50, 50, 25, 7, 5);
        drawDialColoring(centerX, centerY+8, -50, 50, 18, 2, new float[] {1, 1, 1});
        GL11.glTranslatef(0, 0, offset);
    	drawDialColoring(centerX, centerY+8, -50, -45, 25, 9, new float[] {1, 0, 0});
    	GL11.glTranslatef(0, 0, offset);
        drawLongPointer(centerX, centerY+8, (float) (-50+vehicle.fuel/vehicle.maxFuel*100F), 35, 3);
    }
	
	
	
	private static void drawFuelFlowGauge(EntityVehicle vehicle, int centerX, int centerY){
    	drawGaugeBase(vehicle, centerX, centerY);
    	GL11.glTranslatef(0, 0, offset);
    	
    	drawScaledString("FUEL", centerX*2-10, centerY*2+14, 0.5F);
    	drawScaledString("FLOW", centerX*2-12, centerY*2+24, 0.5F);
    	drawScaledString("BKT/M", centerX*2-14, centerY*2-20, 0.5F);
    	
        drawDialIncrements(centerX, centerY, -135, 135, 25, 3, 41);
        drawDialIncrements(centerX, centerY, -135, 135, 25, 5, 9);
        drawDialNumbers(centerX, centerY, -135, 135, 16, 0, 1, 4, 0.6F);
    	drawLongPointer(centerX, centerY, (float) (-135 + vehicle.fuelFlow*20*60*60/1000), 30, 3);
    }
	
	private static void drawEngineTempGauge(EntityVehicle vehicle, int centerX, int centerY){
		drawGaugeBase(vehicle, centerX, centerY);
		GL11.glTranslatef(0, 0, offset);
    	
    	drawScaledString("TEMP", centerX*2-12, centerY*2+14, 0.5F);
    	drawDialColoring(centerX, centerY, -110.7F, -86.4F, 25, 3, new float[] {1, 1, 0});
    	drawDialColoring(centerX, centerY, -86.4F, 67.5F, 25, 3, new float[] {0, 1, 0});
    	
    	GL11.glTranslatef(0, 0, offset);
    	drawDialIncrements(centerX, centerY, -135, 135, 25, 6, 5);
    	drawDialIncrements(centerX, centerY, -135, 135, 25, 3, 21);
    	
    	GL11.glTranslatef(0, 0, offset);
    	drawDialColoring(centerX, centerY, 67.0F, 71.0F, 25, 6, new float[] {1, 0, 0});
    	GL11.glColor3f(1, 1, 1);
    	drawDialNumbers(centerX, centerY, -135, 135, 16, 50, 50, 4, 0.5F);
        
    	GL11.glTranslatef(0, 0, offset);
    	for(byte i=0; i<numberEngines; ++i){
    		drawLongPointer(centerX, centerY, (float) (-135 + ((engineTemps[i]*9F/5F + 32) - 50)*1.35), 30, 3);
    		GL11.glTranslatef(0, 0, offset);
    	}
	}
    
    /**
     * Draws a series of white lines in a polar array.  Used for gauge markers.
     * Lines draw inward from offset, so offset should be the edge of the gauge.
     */
	private static void drawDialIncrements(int centerX, int centerY, float startingAngle, float endingAngle, int offset, int length, int numberElements){
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
    private static void drawDialNumbers(int centerX, int centerY, float startingAngle, float endingAngle,  int offset, int startingNumber, int numberDelta, int numberNumbers, float scale){
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
        	RenderHelper.drawString(String.valueOf(Math.round(currentNumber)), Math.round(-3*scale), Math.round(-3*scale), Color.WHITE);
        	GL11.glPopMatrix();
        	currentNumber+=numberDelta;
        }
    }
    
    /**
     * Draws what can be considered a curved, colored line in an arc between the 
     * specified angles.  Offset is the distance from the center point to the outside
     * of the line, while color is a standard float color array.
     */
    private static void drawDialColoring(int centerX, int centerY, float startingAngle, float endingAngle, int offset, int thickness, float[] colorRGB){
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
        GL11.glPopMatrix();
    }
    /**
     * Draws a long pointer with the given parameters.
     */
    private static void drawLongPointer(int centerX, int centerY, float angle, int length, int width){
    	GL11.glPushMatrix();
    	GL11.glColor3f(1,1,1);
    	RenderHelper.bindTexture(instrumentTexture);
    	rotationHelper(centerX, centerY, angle);
    	GL11.glTranslatef(0, -length*0.25F, 0);
    	RenderHelper.renderSquareUV(centerX-width/2, centerX+width/2, centerY+length/2, centerY-length/2, 0, 0, 0.09375, 0.15625, 0.5, 1, false);
        GL11.glPopMatrix();
    }
    
    /**
     * Draws a short pointer with the given rotation.
     */
    private static void drawShortPointer(int centerX, int centerY, float angle, int length, int width){
        GL11.glPushMatrix();
        GL11.glColor3f(1,1,1);
        RenderHelper.bindTexture(instrumentTexture);
        rotationHelper(centerX, centerY, angle);
        GL11.glTranslatef(0, -length*0.0625F, 0);
        RenderHelper.renderSquareUV(centerX-width/2, centerX+width/2, centerY+length/2, centerY-length/2, 0, 0, 0.03125, 0.21875, 0, 0.5, true);
        GL11.glPopMatrix();
    }
    
    /**
     * Draws a scaled string with the bottom-left at x, y.
     */
    private static void drawScaledString(String string, int x, int y, float scale){
    	GL11.glPushMatrix();
    	GL11.glScalef(scale, scale, scale);
    	RenderHelper.drawString(string, x, y, Color.WHITE);
    	GL11.glPopMatrix();
    }
    
    /**
     * Rotates an object on the given coordinates.
     */
    private static void rotationHelper(int x, int y, float angle){
        GL11.glTranslatef(x, y, 0);
        GL11.glRotatef(angle, 0, 0, 1);
        GL11.glTranslatef(-x, -y, 0);
    }
}

