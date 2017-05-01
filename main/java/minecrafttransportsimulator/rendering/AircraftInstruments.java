package minecrafttransportsimulator.rendering;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.GL11DrawSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public final class AircraftInstruments{
	private static final ResourceLocation instrumentTexture = new ResourceLocation(MTS.MODID, "textures/instruments_aircraft.png");
	public static final ResourceLocation[] gauges = getGaugeTextures();
	
	/**
	 * Call this to draw aircraft gauges.
	 * EngineNumber can be -1 to draw a uni-gauge.
	 */
	public static void drawFlyableInstrument(EntityPlane plane, int x, int y, byte instrumentCode, boolean hud, byte engineNumber){
		boolean lighted = PlaneHUD.areLightsOn(plane);
		
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		if(!hud){
			GL11.glDepthMask(false);
			GL11.glDisable(GL11.GL_LIGHTING);
		}
		if(lighted){
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}
		if(instrumentCode != -1){
			switch (AircraftGauges.values()[instrumentCode]){
				case BLANK: drawGaugeBase(plane, x, y, lighted); break; 
				case ATTITUDE: drawAttitudeIndicator(plane, x, y, lighted); break;
				case ALTIMETER: drawAltimeter(plane, x, y, lighted); break;
				case HEADING: drawHeadingIndicator(plane, x, y, lighted); break;
				case AIRSPEED: drawAirspeedIndicator(plane, x, y, lighted); break;
				case TURNCOORD: drawTurnCoordinator(plane, x, y, lighted); break;
				case TURNSLIP: drawTurnAndSlipIndicator(plane, x, y, lighted); break;
				case VERTICALSPEED: drawVerticalSpeedIndicator(plane, x, y, lighted); break;
				case LIFTRESERVE: drawLiftReserveIndicator(plane, x, y, lighted); break;
				case TRIM: drawTrimIndicator(plane, x, y, lighted); break;
				case DUMMY: break;
				case TACHOMETER: drawTachometer(plane, x, y, lighted, engineNumber); break;
				case FUELQTY: drawFuelGauge(plane, x, y, lighted); break;
				case FUELFLOW: drawFuelFlowGauge(plane, x, y, lighted, engineNumber); break;
				case ENGINETEMP: drawEngineTempGauge(plane, x, y, lighted, engineNumber); break;
				case OILPRESSURE: drawOilPressureGauge(plane, x, y, lighted, engineNumber); break;
				case ELECTRIC: drawElectricalGauge(plane, x, y, lighted); break;
			}
		}else{
			 drawGaugeBase(plane, x, y, lighted);
		}
		if(lighted){
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			GL11DrawSystem.bindTexture(instrumentTexture);
			GL11DrawSystem.renderSquareUV(x-30, x+30, y+30, y-30, 0, 0, 0.5, 0.75, 0.25, 0.5, false);
			GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ZERO);
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}
		if(!hud){
			GL11.glDepthMask(true);
        	GL11.glEnable(GL11.GL_LIGHTING);
        }
    	GL11.glPopMatrix();
	}
	
	public static void drawFlyableControl(EntityPlane plane, int x, int y, AircraftControls type, boolean hud){
		if(!hud){
			GL11.glDepthMask(false);
			GL11.glDisable(GL11.GL_LIGHTING);
		}
		switch (type){
			case THROTTLE: drawThrottle(plane, x, y, hud); break;
			case BRAKE: drawParkingBrake(plane, x, y, hud); break;
			case FLAPS: drawFlapIndicator((EntityPlane) plane, x, y, hud); break;
		}
		if(!hud){
			GL11.glDepthMask(true);
        	GL11.glEnable(GL11.GL_LIGHTING);
        }
	}
    
	private static void drawGaugeBase(EntityPlane plane, int centerX, int centerY, boolean lighted){
		if(lighted){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
			GL11DrawSystem.bindTexture(instrumentTexture);
	    	GL11DrawSystem.renderSquareUV(centerX-30, centerX+30, centerY+30, centerY-30, 0, 0, 0.75, 1, 0, 0.25, false);
	    	Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}else{
			GL11DrawSystem.bindTexture(instrumentTexture);
	    	GL11DrawSystem.renderSquareUV(centerX-30, centerX+30, centerY+30, centerY-30, 0, 0, 0.75, 1, 0, 0.25, false);
		}
    }
    
	private static void drawAttitudeIndicator(EntityPlane plane, int centerX, int centerY, boolean lighted){
		GL11.glPushMatrix();
		GL11DrawSystem.bindTexture(instrumentTexture);
		
		rotationHelper(centerX, centerY, -plane.rotationRoll);
		if(plane.rotationPitch >= 24){
			GL11DrawSystem.renderQuadUV(centerX-20, centerX+20, centerX+20, centerX-20, centerY+20, centerY+20, centerY-20, centerY-20, 0, 0, 0, 0, 0.25, 0.5625, 0.53125, 0.84375, false);
		}else if(plane.rotationPitch <= -24){
			GL11DrawSystem.renderQuadUV(centerX-20, centerX+20, centerX+20, centerX-20, centerY+20, centerY+20, centerY-20, centerY-20, 0, 0, 0, 0, 0.4375, 0.75, 0.53125, 0.84375, false);
		}else{
			GL11DrawSystem.renderQuadUV(centerX-20, centerX+20, centerX+20, centerX-20, centerY+20, centerY+20, centerY-20, centerY-20, 0, 0, 0, 0, 0.34375 - plane.rotationPitch*0.00390625, 0.65625 - 0.00390625*plane.rotationPitch, 0.53125,  0.84375, false);
		}
    	
    	GL11DrawSystem.renderSquareUV(centerX-30, centerX+30, centerY+30, centerY-30, 0, 0, 0.25, 0.5, 0, 0.25, false);
    	
    	rotationHelper(centerX, centerY, plane.rotationRoll);
    	if(lighted){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
			GL11DrawSystem.renderSquareUV(centerX-30, centerX+30, centerY+30, centerY-30, 0, 0, 0.5, 0.75, 0, 0.25, false);
	    	Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}else{
			GL11DrawSystem.renderSquareUV(centerX-30, centerX+30, centerY+30, centerY-30, 0, 0, 0.5, 0.75, 0, 0.25, false);
		}
		GL11.glPopMatrix();
	}
	
	private static void drawAltimeter(EntityPlane plane, int centerX, int centerY, boolean lighted){
    	drawGaugeBase(plane, centerX, centerY, lighted);
    	drawScaledString("ALTITUDE", centerX*2-20, centerY*2+14, 0.5F);
        drawDialIncrements(centerX, centerY, -180, 180, 25, 2, 51);
        drawDialIncrements(centerX, centerY, -180, 180, 25, 5, 11);
        drawDialNumbers(centerX, centerY, 0, 320,  17, 0, 1, 9, 0.7F);
        drawShortPointer(centerX, centerY, (float) (.36*(plane.posY - (ConfigSystem.getBooleanConfig("SeaLevelOffset") ? plane.worldObj.provider.getAverageGroundLevel() : 0))), 20, 6);
        drawLongPointer(centerX, centerY, (float) (3.6*(plane.posY - (ConfigSystem.getBooleanConfig("SeaLevelOffset") ? plane.worldObj.provider.getAverageGroundLevel() : 0))), 35, 3);
    }
    
	private static void drawHeadingIndicator(EntityPlane plane, int centerX, int centerY, boolean lighted){
    	drawGaugeBase(plane, centerX, centerY, lighted);
    	
    	GL11DrawSystem.bindTexture(instrumentTexture);
    	GL11DrawSystem.renderSquareUV(centerX-20, centerX+20, centerY+20, centerY-20, 0, 0, 0.75, 1, 0.25, 0.5, false);
    	
    	drawScaledString("HEADING", centerX*2-18, centerY*2+14, 0.5F);
    	
        GL11.glPushMatrix();
        rotationHelper(centerX, centerY, -plane.rotationYaw);
        drawDialIncrements(centerX, centerY, 0, 360, 25, 5, 25);
        drawDialIncrements(centerX, centerY, 7.5F, 367.5F, 25, 3, 25);
        GL11.glScalef(0.60F, 0.60F, 0.60F);
        centerX=Math.round(((float)centerX)*(1/0.60F));
        centerY=Math.round(((float)centerY)*(1/0.60F));
        GL11DrawSystem.drawString("S", centerX-3, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        GL11DrawSystem.drawString("3", centerX-3, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        GL11DrawSystem.drawString("6", centerX-3, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        GL11DrawSystem.drawString("W", centerX-2, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        GL11DrawSystem.drawString("12", centerX-4, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        GL11DrawSystem.drawString("15", centerX-4, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        GL11DrawSystem.drawString("N", centerX-2, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        GL11DrawSystem.drawString("21", centerX-4, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        GL11DrawSystem.drawString("24", centerX-4, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        GL11DrawSystem.drawString("E", centerX-3, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        GL11DrawSystem.drawString("30", centerX-5, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        GL11DrawSystem.drawString("33", centerX-5, centerY-32, Color.WHITE);
        rotationHelper(centerX, centerY, 30);
        GL11.glPopMatrix();
    }
    
	private static void drawAirspeedIndicator(EntityPlane plane, int centerX, int centerY, boolean lighted){
    	drawGaugeBase(plane, centerX, centerY, lighted);

    	drawScaledString("BLK/S", centerX*2-15, centerY*2+14, 0.5F);
    	drawDialColoring(centerX, centerY, 292.5F, 330, 25, 3, new float[] {1, 0, 0});
    	drawDialColoring(centerX, centerY, 217.5F, 292.5F, 25, 3, new float[] {1, 1, 0});
    	drawDialColoring(centerX, centerY, 105F, 217.5F, 25, 3, new float[] {0, 1, 0});
    	
    	drawDialIncrements(centerX, centerY, 30, 330, 25, 8, 9);
    	drawDialIncrements(centerX, centerY, 30, 330, 25, 3, 41);
    	drawDialNumbers(centerX, centerY, 30, 330, 15, 0, 10, 4, 0.6F);
    	drawLongPointer(centerX, centerY, (float) (30+7.5*plane.velocity*ConfigSystem.getDoubleConfig("SpeedFactor")*20), 35, 2);
    }
    
	private static void drawTurnCoordinator(EntityPlane plane, int centerX, int centerY, boolean lighted){
		GL11DrawSystem.bindTexture(instrumentTexture);
		drawGaugeBase(plane, centerX, centerY, lighted);
		GL11.glPushMatrix();
    	
    	drawDialIncrements(centerX, centerY, -90, 90, 20, 5, 2);
    	drawDialIncrements(centerX, centerY, -115, 115, 20, 5, 2);
    	
    	GL11DrawSystem.renderSquareUV(centerX-25, centerX+25, centerY+18.75, centerY+6.25, 0, 0, 0.75, 1, 0.5625, 0.625, false);
    	
    	float turn = Math.max(Math.min(((plane.rotationRoll - plane.prevRotationRoll)/10 + plane.rotationYaw - plane.prevRotationYaw)/0.15F*25F, 50), -50);
    	rotationHelper(centerX, centerY, turn);
    	GL11DrawSystem.renderSquareUV(centerX-25, centerX+25, centerY+6.25, centerY-6.25, 0, 0, 0.75, 1, 0.5, 0.5625, false);
    	rotationHelper(centerX, centerY, -turn);
    	
    	double slip = plane.sideVec.dot(plane.velocityVec);
    	GL11DrawSystem.renderSquareUV(centerX-2.5 + 20*slip, centerX+2.5 + 20*slip, centerY+15 - Math.abs(slip), centerY+10 - Math.abs(slip), 0, 0, 0.75, 0.875, 0.875, 1, false);

    	drawScaledString("L", centerX*2-34, centerY*2+20, 0.5F);
    	drawScaledString("R", centerX*2+30, centerY*2+20, 0.5F);
    	drawScaledString("M.J.", centerX*2-8, centerY*2-46, 0.5F);
    	drawScaledString("ELEC", centerX*2-12, centerY*2-36, 0.5F);
    	drawScaledString("2 MIN", centerX*2-14, centerY*2+36, 0.5F);
    	GL11.glPopMatrix();
	}
	
	private static void drawTurnAndSlipIndicator(EntityPlane plane, int centerX, int centerY, boolean lighted){
		GL11DrawSystem.bindTexture(instrumentTexture);
		drawGaugeBase(plane, centerX, centerY, lighted);
		GL11.glPushMatrix();
    	
    	GL11DrawSystem.renderSquareUV(centerX-25, centerX+25, centerY+18.75, centerY+6.25, 0, 0, 0.75, 1, 0.5625, 0.625, false);
    	
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
    	
    	float turn = Math.max(Math.min((plane.rotationYaw - plane.prevRotationYaw)/0.15F*25F, 50), -50);
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
    	
    	double slip = plane.sideVec.dot(plane.velocityVec);
    	GL11DrawSystem.renderSquareUV(centerX-2.5 + 20*slip, centerX+2.5 + 20*slip, centerY+15 - Math.abs(slip), centerY+10 - Math.abs(slip), 0, 0, 0.75, 0.875, 0.875, 1, false);

    	drawScaledString("L", centerX*2-30, centerY*2-30, 0.5F);
    	drawScaledString("R", centerX*2+26, centerY*2-30, 0.5F);
    	drawScaledString("2 MIN", centerX*2-14, centerY*2+36, 0.5F);
    	GL11.glPopMatrix();
	}
	
	private static void drawVerticalSpeedIndicator(EntityPlane plane, int centerX, int centerY, boolean lighted){
		drawGaugeBase(plane, centerX, centerY, lighted);

    	drawScaledString("CLIMB", centerX*2-14, centerY*2-14, 0.5F);
    	drawScaledString("BLK/S", centerX*2-14, centerY*2+10, 0.5F);
    	drawDialNumbers(centerX, centerY, -90, 90, 16, 0, 1, 4, 0.7F);
    	drawDialNumbers(centerX, centerY, -132.5F, -217.5F, 16, 1, 1, 2, 0.7F);
    	drawDialIncrements(centerX, centerY, -260, 80, 25, 5, 9);
    	drawDialIncrements(centerX, centerY, -132.5F, -47.5F, 25, 3, 11);
    	drawDialIncrements(centerX, centerY, -47.5F, 80, 25, 2, 16);
    	drawDialIncrements(centerX, centerY, -260, -132.5F, 25, 2, 16);
    	drawLongPointer(centerX, centerY, (float) (-90+10.625*plane.motionY*20), 35, 2);
	}
	
	private static void drawLiftReserveIndicator(EntityPlane plane, int centerX, int centerY, boolean lighted){
		drawGaugeBase(plane, centerX, centerY, lighted);
    	GL11.glPushMatrix();
    	
    	drawScaledString("LIFT RESERVE", centerX*2-32, centerY*2+14, 0.5F);
    	drawDialColoring(centerX, centerY+20, -37, -35, 35, 10, new float[] {0, 0, 0});
    	drawDialColoring(centerX, centerY+20, -35, -26, 32, 7, new float[] {1, 0, 0});
    	drawDialColoring(centerX, centerY+20, -26, -24, 32, 7, new float[] {0, 0, 0});
    	drawDialColoring(centerX, centerY+20, -24, -11, 32, 7, new float[] {1, 1, 0});
    	drawDialColoring(centerX, centerY+20, -11, -9, 32, 7, new float[] {0, 0, 0});
    	drawDialColoring(centerX, centerY+20, -9, 35, 32, 7, new float[] {0, 1, 0});
    	drawDialColoring(centerX, centerY+20, -35, 35, 35, 3, new float[] {1, 1, 1});
    	drawDialColoring(centerX, centerY+20, 35, 37, 35, 10, new float[] {0, 0, 0});
    	
    	GL11.glDisable(GL11.GL_TEXTURE_2D);
    	GL11.glColor3f(0, 0, 0);
        for(float theta=-33; theta<=33; theta+=3){
        	GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2d(centerX+34.5*Math.sin(Math.toRadians(theta)), centerY+20-34.5*Math.cos(Math.toRadians(-theta)));
            GL11.glVertex2d(centerX+(34.5-2)*Math.sin(Math.toRadians(theta)), centerY+20-(34.5-2)*Math.cos(Math.toRadians(-theta)));
            GL11.glEnd();
        }
    	

        float angle = (float) Math.max(Math.min(plane.trackAngle*3 + 20, 35), -35);
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
	
	private static void drawTrimIndicator(EntityPlane plane, int centerX, int centerY, boolean lighted){
    	drawGaugeBase(plane, centerX, centerY, lighted);
    	
    	drawScaledString("TRIM", centerX*2-10, centerY*2+40, 0.5F);
    	drawScaledString("NOSE", centerX*2-10, centerY*2-49, 0.5F);
    	drawScaledString("WING", centerX*2-44, centerY*2-13, 0.5F);
    	drawScaledString("STAB", centerX*2+24, centerY*2-13, 0.5F);
    	
    	drawDialIncrements(centerX, centerY-10, -135, 135, 10, 3, 9);
    	drawDialIncrements(centerX-12, centerY+8, -165, -15, 10, 3, 5);
    	drawDialIncrements(centerX-12, centerY+8, 15, 165, 10, 3, 5);
    	drawDialIncrements(centerX+12, centerY+8, -45, 225, 10, 3, 7);
    	
    	drawDialColoring(centerX, centerY-10, -135, 135, 10, 1, new float[] {0, 0.5F, 0});
    	drawDialColoring(centerX-12, centerY+8, -165, -15, 10, 1, new float[] {0, 0.5F, 0});
    	drawDialColoring(centerX-12, centerY+8, 15, 165, 10, 1, new float[] {0, 0.5F, 0});
    	drawDialColoring(centerX+12, centerY+8, -45, 225, 10, 1, new float[] {0, 0.5F, 0});
    	
    	if(plane instanceof EntityPlane){
    		drawLongPointer(centerX, centerY-10, 135F/20F*((EntityPlane) plane).rudderTrim/10F, 14, 2);
    		rotationHelper(centerX-12, centerY+8, 75F/10F*((EntityPlane) plane).aileronTrim/10F + 90);
    		GL11DrawSystem.renderSquareUV(centerX-13, centerX-11, centerY+8, centerY-4, 0, 0, 0.09375, 0.15625, 0.5, 0.875, false);
    		GL11DrawSystem.renderSquareUV(centerX-13, centerX-11, centerY+22, centerY+8, 0, 0, 0.09375, 0.15625, 0.875, 0.5, false);
    		rotationHelper(centerX-12, centerY+8, -(75F/10F*((EntityPlane) plane).aileronTrim/10F + 90));
    		drawLongPointer(centerX+12, centerY+8, 135F/15F*((EntityPlane) plane).elevatorTrim/10F + 90, 14, 2);
    	}
    }
	
	private static void drawTachometer(EntityPlane plane, int centerX, int centerY, boolean lighted, byte engineNumber){
    	drawGaugeBase(plane, centerX, centerY, lighted);
    	drawScaledString("RPM", centerX*2-10, centerY*2+14, 0.5F);
    	if(engineNumber == -1){
    		int lowestMaxRPM = 9999;
    		for(byte i=0; i<plane.getNumberEngineBays(); ++i){
    			if(plane.getEngineByNumber(i) != null){
    				lowestMaxRPM = Math.min(lowestMaxRPM, plane.getEngineByNumber(i).maxRPM);
    			}
    		}
    		drawDialColoring(centerX, centerY, -135+lowestMaxRPM/10, 165, 25, 4, new float[] {1, 0, 0});
		}else{
			if(plane.getEngineByNumber(engineNumber) != null){
				drawDialColoring(centerX, centerY, -135+plane.getEngineByNumber(engineNumber).maxRPM/10, 165, 25, 4, new float[] {1, 0, 0});
			}
		}
		drawDialIncrements(centerX, centerY, -135, 165, 25, 5, 61);
		drawDialIncrements(centerX, centerY, -135, 165, 25, 9, 13);
        drawDialNumbers(centerX, centerY, -135, 165, 13, 0, 5, 6, 0.6F);
        if(engineNumber == -1){
    		for(byte i=0; i<plane.getNumberEngineBays(); ++i){
    			if(plane.getEngineByNumber(i) != null){
	    			drawLongPointer(centerX, centerY, (float) (-135+plane.getEngineByNumber(i).RPM/10), 30, 3);
    			}else{
    				drawLongPointer(centerX, centerY, (float) (-135), 30, 3);
    			}
    		}
    	}else{
    		if(plane.getEngineByNumber(engineNumber) != null){
    			drawLongPointer(centerX, centerY, (float) (-135+plane.getEngineByNumber(engineNumber).RPM/10), 30, 3);
    		}else{
    			drawLongPointer(centerX, centerY, (float) (-135), 30, 3);
    		}
    	}
    }
	
	private static void drawFuelGauge(EntityPlane plane, int centerX, int centerY, boolean lighted){
    	drawGaugeBase(plane, centerX, centerY, lighted);
    	
    	drawScaledString("BUCKETS", centerX*2-20, centerY*2+14, 0.5F);
    	drawScaledString("FUEL", centerX*2-10, centerY*2+24, 0.5F);
    	drawScaledString("0", centerX*2-40, centerY*2-10, 0.5F);
    	drawScaledString(String.valueOf(plane.fuelCapacity/1000/2F), centerX*2-7, centerY*2-45, 0.5F);
    	drawScaledString(String.valueOf(plane.fuelCapacity/1000), centerX*2+35, centerY*2-10, 0.5F);
        drawDialIncrements(centerX, centerY+8, -50, 50, 25, 7, 5);
        drawDialColoring(centerX, centerY+8, -50, 50, 18, 2, new float[] {1, 1, 1});
    	drawDialColoring(centerX, centerY+8, -50, -45, 25, 9, new float[] {1, 0, 0});
        drawLongPointer(centerX, centerY+8, (float) (-50+plane.fuel/plane.fuelCapacity*100F), 35, 3);
    }
	
	private static void drawFuelFlowGauge(EntityPlane plane, int centerX, int centerY, boolean lighted, byte engineNumber){
    	drawGaugeBase(plane, centerX, centerY, lighted);
    	
    	drawScaledString("FUEL", centerX*2-10, centerY*2+14, 0.5F);
    	drawScaledString("FLOW", centerX*2-12, centerY*2+24, 0.5F);
    	drawScaledString("BKT/M", centerX*2-14, centerY*2-20, 0.5F);
    	
        drawDialIncrements(centerX, centerY, -135, 135, 25, 3, 41);
        drawDialIncrements(centerX, centerY, -135, 135, 25, 5, 9);
        drawDialNumbers(centerX, centerY, -135, 135, 16, 0, 1, 4, 0.6F);
        if(engineNumber == -1){
        	double totalFuelFlow = 0;
    		for(byte i=0; i<plane.getNumberEngineBays(); ++i){
    			if(plane.getEngineByNumber(i) != null){
    				totalFuelFlow += plane.getEngineByNumber(i).fuelFlow;
    			}
    		}
    		drawLongPointer(centerX, centerY, (float) (-135 + totalFuelFlow*20*60*60/1000), 30, 3);
    	}else{
    		if(plane.getEngineByNumber(engineNumber) != null){
    			drawLongPointer(centerX, centerY, (float) (-135 + plane.getEngineByNumber(engineNumber).fuelFlow*20*60*60/1000), 30, 3);
    		}else{
    			drawLongPointer(centerX, centerY, (float) (-135), 30, 3);
    		}
    	}
    	
    }
	
	private static void drawEngineTempGauge(EntityPlane plane, int centerX, int centerY, boolean lighted, byte engineNumber){
		drawGaugeBase(plane, centerX, centerY, lighted);
    	
    	drawScaledString("TEMP", centerX*2-12, centerY*2+14, 0.5F);
    	drawDialColoring(centerX, centerY, -110.7F, -86.4F, 25, 3, new float[] {1, 1, 0});
    	drawDialColoring(centerX, centerY, -86.4F, 131, 25, 3, new float[] {0, 1, 0});
    	
    	drawDialIncrements(centerX, centerY, -135, 135, 25, 6, 5);
    	drawDialIncrements(centerX, centerY, -135, 135, 25, 3, 21);
    	
    	drawDialColoring(centerX, centerY, 131, 135, 25, 6, new float[] {1, 0, 0});
    	GL11.glColor3f(1, 1, 1);
    	drawDialNumbers(centerX, centerY, -135, 135, 16, 50, 50, 4, 0.5F);
        
        if(engineNumber == -1){
    		for(byte i=0; i<plane.getNumberEngineBays(); ++i){
    			if(plane.getEngineByNumber(i) != null){
	    			drawLongPointer(centerX, centerY, (float) (-135 + ((plane.getEngineByNumber(i).temp*9F/5F + 32) - 50)*1.35), 30, 3);
    			}else{
    				drawLongPointer(centerX, centerY, (float) (-135), 30, 3);
    			}
    		}
    	}else{
    		if(plane.getEngineByNumber(engineNumber) != null){
    			drawLongPointer(centerX, centerY, (float) (-135 + ((plane.getEngineByNumber(engineNumber).temp*9F/5F + 32) - 50)*1.35), 30, 3);
    		}else{
    			drawLongPointer(centerX, centerY, (float) (-135), 30, 3);
    		}
    	}
	}
	
	private static void drawOilPressureGauge(EntityPlane plane, int centerX, int centerY, boolean lighted, byte engineNumber){
		drawGaugeBase(plane, centerX, centerY, lighted);
		
    	drawScaledString("PSI", centerX*2-10, centerY*2+14, 0.5F);
    	drawDialColoring(centerX, centerY, -135, -63, 25, 3, new float[] {1, 0, 0});
    	drawDialColoring(centerX, centerY, -63, -27, 25, 3, new float[] {1, 1, 0});
    	drawDialColoring(centerX, centerY, -27, 45, 25, 3, new float[] {0, 1, 0});
    	
    	drawDialIncrements(centerX, centerY, -135, 135, 25, 6, 11);
    	drawDialIncrements(centerX, centerY, -121.5F, 121.5F, 25, 3, 10);
    	GL11.glColor3f(1, 1, 1);
    	drawDialNumbers(centerX, centerY, -135, 135, 16, 0, 25, 6, 0.5F);
        if(engineNumber == -1){
    		for(byte i=0; i<plane.getNumberEngineBays(); ++i){
    			if(plane.getEngineByNumber(i) != null){
	    			drawLongPointer(centerX, centerY, (float) (-135 + plane.getEngineByNumber(i).oilPressure*1.8F), 30, 3);
    			}else{
    				drawLongPointer(centerX, centerY, (float) (-135), 30, 3);
    			}
    		}
    	}else{
    		if(plane.getEngineByNumber(engineNumber) != null){
    			drawLongPointer(centerX, centerY, (float) (-135 + plane.getEngineByNumber(engineNumber).oilPressure*1.8F), 30, 3);
    		}else{
    			drawLongPointer(centerX, centerY, (float) (-135), 30, 3);
    		}
    	}
	}
	
	private static void drawElectricalGauge(EntityPlane plane, int centerX, int centerY, boolean lighted){
		drawGaugeBase(plane, centerX, centerY, lighted);
    	
		drawDialColoring(centerX - 17, centerY, 48.75F, 76.25F, 16, 3, new float[] {0, 1, 0});
		drawDialColoring(centerX - 17, centerY, 124.375F, 145F, 16, 3, new float[] {1, 0, 0});		

    	drawDialIncrements(centerX - 17, centerY, 35, 145, 16, 3, 5);
    	drawDialIncrements(centerX + 17, centerY, -145, -35, 16, 3, 5);
    	drawScaledString("16", centerX*2-32, centerY*2 - 30, 0.5F);
    	drawScaledString("8", centerX*2-18, centerY*2 - 3, 0.5F);
    	drawScaledString("0", centerX*2-28, centerY*2 + 22, 0.5F);
    	drawScaledString("+2", centerX*2+22, centerY*2 - 30, 0.5F);
    	drawScaledString("0", centerX*2+12, centerY*2 - 3, 0.5F);
    	drawScaledString("-2", centerX*2+22, centerY*2 + 22, 0.5F);
    	
    	drawLongPointer(centerX - 17, centerY, (float) (145 - 6.875F*plane.electricPower), 26, 2);
    	drawLongPointer(centerX + 17, centerY, (float) (-90 - 27.5F*Math.min(plane.electricFlow*100F, 2)), 26, 2);
    	
    	drawScaledString("V", centerX*2-40, centerY*2 - 10, 0.5F);
    	drawScaledString("M", centerX*2-40, centerY*2, 0.5F);
    	drawScaledString("J", centerX*2-40, centerY*2 + 10, 0.5F);
    	drawScaledString("A", centerX*2+38, centerY*2 - 10, 0.5F);
    	drawScaledString("M", centerX*2+38, centerY*2, 0.5F);
    	drawScaledString("P", centerX*2+38, centerY*2 + 10, 0.5F);
	}
	
	private static void drawThrottle(EntityPlane plane, int centerX, int centerY, boolean hud){		
    	GL11DrawSystem.bindTexture(instrumentTexture);
		if(!hud){
    		GL11DrawSystem.renderSquareUV(centerX-5.25, centerX+5.25, centerY+5.25, centerY-5.25, 0, 0, 0.75, 0.875, 0.875, 1, false);
    		
    		GL11DrawSystem.renderQuadUV(centerX-1.75, centerX-1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY+1.75, centerY+1.75, -7-plane.throttle/10F, 0, 0, -7-plane.throttle/10F, 0.640625, 0.734375, 0.890625, 0.984375, false);
    		GL11DrawSystem.renderQuadUV(centerX+1.75, centerX+1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY-1.75, centerY-1.75, -7-plane.throttle/10F, 0, 0, -7-plane.throttle/10F, 0.640625, 0.734375, 0.890625, 0.984375, false);
        	GL11DrawSystem.renderQuadUV(centerX+1.75, centerX+1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY-1.75, centerY-1.75, -7-plane.throttle/10F, 0, 0, -7-plane.throttle/10F, 0.640625, 0.734375, 0.890625, 0.984375, false);
        	GL11DrawSystem.renderQuadUV(centerX-1.75, centerX-1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY+1.75, centerY+1.75, -7-plane.throttle/10F, 0, 0, -7-plane.throttle/10F, 0.640625, 0.734375, 0.890625, 0.984375, false);
        	
        	GL11DrawSystem.renderSquareUV(centerX-7, centerX+7, centerY+7, centerY-7, -7-plane.throttle/10F, -7-plane.throttle/10F, 0.75, 0.875, 0.875, 1, true);
    	}else{
        	GL11DrawSystem.renderSquareUV(centerX-5.25, centerX+5.25, centerY+0.175, centerY-10.5, 0, 0, 0.75, 0.875, 0.875, 1, false);
        	GL11DrawSystem.renderSquareUV(centerX-1.75, centerX+1.75, centerY+7+plane.throttle/10, centerY-7, 0, 0, 0.640625, 0.734375, 0.890625, 0.984375, false);
        	GL11DrawSystem.renderSquareUV(centerX-7, centerX+7, centerY+7+plane.throttle/10F, centerY-7+plane.throttle/10F, 0, 0, 0.75, 0.875, 0.875, 1, false);
    	}
    }
	
	private static void drawParkingBrake(EntityPlane plane, int centerX, int centerY, boolean hud){
    	GL11DrawSystem.bindTexture(instrumentTexture);
    	
    	if(!hud){
    		GL11DrawSystem.renderSquareUV(centerX-5.25, centerX+5.25, centerY+5.25, centerY-5.25, 0, 0, 0.75, 0.875, 0.875, 1, false);
        	
    		if(plane.parkingBrakeOn || plane.brakeOn){        		
        		GL11DrawSystem.renderQuadUV(centerX-1.75, centerX-1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY+1.75, centerY+1.75, -20, 0, 0, -20, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		GL11DrawSystem.renderQuadUV(centerX+1.75, centerX+1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY-1.75, centerY-1.75, -20, 0, 0, -20, 0.640625, 0.734375, 0.890625, 0.984375, false);
            	GL11DrawSystem.renderQuadUV(centerX+1.75, centerX+1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY-1.75, centerY-1.75, -20, 0, 0, -20, 0.640625, 0.734375, 0.890625, 0.984375, false);
            	GL11DrawSystem.renderQuadUV(centerX-1.75, centerX-1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY+1.75, centerY+1.75, -20, 0, 0, -20, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		
        		GL11DrawSystem.renderQuadUV(centerX-5, centerX+5, centerX+5, centerX-5, centerY+25, centerY+25, centerY-10, centerY-10, -20, -20, -20, -20, 0.2578125, 0.3671875, 0.921875, 0.953125, false);
    	        
    	        GL11.glPushMatrix();
    	        GL11.glTranslatef(0, 0, -20.01F);
    	    	rotationHelper(centerX, centerY, -90);
    	    	drawScaledString("BRAKE", centerX*2-30, centerY*2-4, 0.5F);
    	    	GL11.glPopMatrix();
        	}else{
        		GL11DrawSystem.renderQuadUV(centerX-1.75, centerX-1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY+1.75, centerY+1.75, -2, 0, 0, -2, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		GL11DrawSystem.renderQuadUV(centerX+1.75, centerX+1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY-1.75, centerY-1.75, -2, 0, 0, -2, 0.640625, 0.734375, 0.890625, 0.984375, false);
            	GL11DrawSystem.renderQuadUV(centerX+1.75, centerX+1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY-1.75, centerY-1.75, -2, 0, 0, -2, 0.640625, 0.734375, 0.890625, 0.984375, false);
            	GL11DrawSystem.renderQuadUV(centerX-1.75, centerX-1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY+1.75, centerY+1.75, -2, 0, 0, -2, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		
    	        GL11DrawSystem.renderSquareUV(centerX-22.5, centerX+12.5, centerY+5, centerY-5, -2, -2, 0.2578125, 0.3671875, 0.921875, 0.953125, false);    		
    	        
    	        GL11.glPushMatrix();
    	        GL11.glTranslatef(0, 0, -2.01F);
    	        drawScaledString("BRAKE", centerX*2-25, centerY*2-4, 0.5F);
    	        GL11.glPopMatrix();
        	}
    	}else{
    		GL11DrawSystem.renderSquareUV(centerX-5.25, centerX+5.25, centerY+0.175, centerY-10.5, 0, 0, 0.75, 0.875, 0.875, 1, true);
        	if(plane.parkingBrakeOn || plane.brakeOn){
        		GL11DrawSystem.renderSquareUV(centerX-1.75, centerX+1.75, centerY+15, centerY-7, 0, 0, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		GL11DrawSystem.renderQuadUV(centerX-5, centerX+5, centerX+5, centerX-5, centerY+35, centerY+35, centerY, centerY, 0, 0, 0, 0, 0.2578125, 0.3671875, 0.921875, 0.953125, false);
    	        
    	        GL11.glPushMatrix();
    	    	rotationHelper(centerX, centerY, -90);
    	        drawScaledString("BRAKE", centerX*2-50, centerY*2-4, 0.5F);
    	    	GL11.glPopMatrix();
        	}else{
        		GL11DrawSystem.renderSquareUV(centerX-1.75, centerX+1.75, centerY+5, centerY-7, 0, 0, 0.640625, 0.734375, 0.890625, 0.984375, false);
    	        GL11DrawSystem.renderSquareUV(centerX-22.5, centerX+12.5, centerY+5, centerY-5, 0, 0, 0.2578125, 0.3671875, 0.921875, 0.953125, false);    		
    	        drawScaledString("BRAKE", centerX*2-25, centerY*2-4, 0.5F);
        	}
    	}
    }
    
	private static void drawFlapIndicator(EntityPlane plane, int centerX, int centerY, boolean hud){
    	GL11DrawSystem.bindTexture(instrumentTexture);
    	GL11DrawSystem.renderSquareUV(centerX-11.25, centerX+11.25, centerY+15, centerY-15, 0, 0, 0.515625, 0.609375, 0.875, 1, false);
    	    	
        GL11.glPushMatrix();
    	rotationHelper(centerX, centerY, -90);
    	drawScaledString("FLAPS", centerX*2-15, centerY*2-15, 0.5F);
    	GL11.glPopMatrix();
    	
    	GL11.glPushMatrix();
    	drawScaledString("0", centerX*2+8, centerY*2-16, 0.5F);
    	drawScaledString("35", centerX*2+8, centerY*2+10, 0.5F);
    	GL11.glPopMatrix();
        
    	GL11DrawSystem.bindTexture(instrumentTexture);
    	if(!hud){
    		GL11DrawSystem.renderQuadUV(centerX-5.625, centerX-5.625, centerX-5.625, centerX-5.625, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, -7, 0, 0, -7, 0.421875, 0.453125, 0.921875, 0.953125, false);
    		GL11DrawSystem.renderQuadUV(centerX+1.875, centerX+1.875, centerX+1.875, centerX+1.875, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, -7, 0, 0, -7, 0.421875, 0.453125, 0.921875, 0.953125, false);
        	GL11DrawSystem.renderQuadUV(centerX-5.625, centerX-5.625, centerX+1.875, centerX+1.875, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, 0, -7, -7, 0, 0.421875, 0.453125, 0.921875, 0.953125, false);
        	GL11DrawSystem.renderQuadUV(centerX-5.625, centerX-5.625, centerX+1.875, centerX+1.875, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, -7, 0, 0, -7, 0.421875, 0.453125, 0.921875, 0.953125, false);
        	GL11DrawSystem.renderQuadUV(centerX-5.625, centerX-5.625, centerX+1.875, centerX+1.875, centerY-8+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-8+plane.flapAngle/25, -7, -7, -7, -7, 0.421875, 0.453125, 0.921875, 0.953125, false);
    	}else{
    		GL11DrawSystem.renderSquareUV(centerX-5.625, centerX+1.875, centerY-0.5+plane.flapAngle/25, centerY-8+plane.flapAngle/25, 0, 0, 0.421875, 0.453125, 0.921875, 0.953125, false);
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
        	GL11DrawSystem.drawString(String.valueOf(Math.round(currentNumber)), Math.round(-3*scale), Math.round(-3*scale), Color.WHITE);
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
	    GL11.glColor3f(1, 1, 1);
        GL11.glPopMatrix();
    }
    /**
     * Draws a long pointer with the given parameters.
     */
    private static void drawLongPointer(int centerX, int centerY, float angle, int length, int width){
    	GL11.glPushMatrix();
    	GL11.glColor3f(1,1,1);
    	GL11DrawSystem.bindTexture(instrumentTexture);
    	rotationHelper(centerX, centerY, angle);
    	GL11.glTranslatef(0, -length*0.25F, 0);
    	GL11DrawSystem.renderSquareUV(centerX-width/2, centerX+width/2, centerY+length/2, centerY-length/2, 0, 0, 0.09375, 0.15625, 0.5, 1, false);
        GL11.glPopMatrix();
    }
    
    /**
     * Draws a short pointer with the given rotation.
     */
    private static void drawShortPointer(int centerX, int centerY, float angle, int length, int width){
        GL11.glPushMatrix();
        GL11.glColor3f(1,1,1);
        GL11DrawSystem.bindTexture(instrumentTexture);
        rotationHelper(centerX, centerY, angle);
        GL11.glTranslatef(0, -length*0.0625F, 0);
        GL11DrawSystem.renderSquareUV(centerX-width/2, centerX+width/2, centerY+length/2, centerY-length/2, 0, 0, 0.03125, 0.21875, 0, 0.5, true);
        GL11.glPopMatrix();
    }
    
    /**
     * Draws a scaled string with the bottom-left at x, y.
     */
    private static void drawScaledString(String string, int x, int y, float scale){
    	GL11.glPushMatrix();
    	GL11.glScalef(scale, scale, scale);
    	GL11DrawSystem.drawString(string, x, y, Color.WHITE);
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

    public enum AircraftGauges{
    	BLANK,
    	ATTITUDE,
    	ALTIMETER,
    	HEADING,
    	AIRSPEED,
    	TURNCOORD,
    	TURNSLIP,
    	VERTICALSPEED,
    	LIFTRESERVE,
    	TRIM,
    	DUMMY,
    	TACHOMETER,
    	FUELQTY,
    	FUELFLOW,
    	ENGINETEMP,
    	OILPRESSURE,
    	ELECTRIC;
    }
    
    public enum AircraftControls{
    	THROTTLE,
    	BRAKE,
    	FLAPS;
    }
    
	private static ResourceLocation[] getGaugeTextures(){
		ResourceLocation[] texArray = new ResourceLocation[AircraftGauges.values().length];
		for(byte i=0; i<texArray.length; ++i){
			texArray[i] = new ResourceLocation(MTS.MODID, "textures/items/flightinstrument" + String.valueOf(i) + ".png");
		}
		return texArray;
	}
}

