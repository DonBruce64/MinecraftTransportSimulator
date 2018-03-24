package minecrafttransportsimulator.rendering;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.dataclasses.MTSInstruments.Instruments;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle.LightTypes;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.sounds.StallSound;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Minecraft;

public final class RenderInstrumentsAircraft extends RenderInstruments{
	private static StallSound stallBuzzer;
	/**
	 * Call this to draw aircraft gauges.
	 * EngineNumber can be 0 to draw a uni-gauge.
	 */
	protected static void drawAircraftInstrument(EntityMultipartVehicle vehicle, int x, int y, Instruments instrument, boolean hud, byte engineNumber){
		boolean lightsOn = vehicle.isLightOn(LightTypes.NAVIGATIONLIGHT) && vehicle.electricPower > 3;
		
		GL11.glPushMatrix();
		if(!hud){
			GL11.glDepthMask(false);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glRotatef(180, 0, 0, 1);
			GL11.glScalef(0.75F, 0.75F, 0.75F);
		}
		if(lightsOn){
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}
		textureManager.bindTexture(instrumentTexture);
		switch (instrument){
			case AIRCRAFT_BLANK: drawGaugeBase(lightsOn); break; 
			case AIRCRAFT_ATTITUDE: drawAttitudeIndicator(vehicle, lightsOn); break;
			case AIRCRAFT_ALTIMETER: drawAltimeter(vehicle, lightsOn); break;
			case AIRCRAFT_HEADING: drawHeadingIndicator(vehicle, lightsOn); break;
			case AIRCRAFT_AIRSPEED: drawAirspeedIndicator(vehicle, lightsOn); break;
			case AIRCRAFT_TURNCOORD: drawTurnCoordinator((EntityPlane) vehicle, lightsOn); break;
			case AIRCRAFT_TURNSLIP: drawTurnAndSlipIndicator((EntityPlane) vehicle, lightsOn); break;
			case AIRCRAFT_VERTICALSPEED: drawVerticalSpeedIndicator(vehicle, lightsOn); break;
			case AIRCRAFT_LIFTRESERVE: drawLiftReserveIndicator((EntityPlane) vehicle, lightsOn); break;
			case AIRCRAFT_TRIM: drawTrimIndicator((EntityPlane) vehicle, lightsOn); break;
			case AIRCRAFT_TACHOMETER: drawTachometer(vehicle, lightsOn, engineNumber); break;
			case AIRCRAFT_FUELQTY: drawFuelGauge(vehicle, lightsOn); break;
			case AIRCRAFT_FUELFLOW: drawFuelFlowGauge(vehicle, lightsOn, engineNumber); break;
			case AIRCRAFT_ENGINETEMP: drawEngineTempGauge(vehicle, lightsOn, engineNumber); break;
			case AIRCRAFT_OILPRESSURE: drawOilPressureGauge(vehicle, lightsOn, engineNumber); break;
			case AIRCRAFT_ELECTRIC: drawElectricalGauge(vehicle, lightsOn); break;
		}

		if(lightsOn){
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			textureManager.bindTexture(instrumentTexture);
			GL11.glTranslatef(x,  y,  0);
			renderSquareUV(60, 60, 0, 0.5F, 0.75F, 0.5F, 0.25F);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}
		if(!hud){
			GL11.glDepthMask(true);
        	GL11.glEnable(GL11.GL_LIGHTING);
        }
    	GL11.glPopMatrix();
	}
    
	private static void drawGaugeBase(boolean lightsOn){
		if(lightsOn){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
	    	renderSquareUV(60, 60, 0, 0.75F, 1F, 0.25F, 0F);
	    	Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}else{
			renderSquareUV(60, 60, 0, 0.75F, 1F, 0.25F, 0F);
		}
    }
    
	private static void drawAttitudeIndicator(EntityMultipartVehicle aircraft, boolean lightsOn){
		GL11.glRotatef(-aircraft.rotationRoll + 90, 0, 0, 1);
		if(aircraft.rotationPitch >= 24){
			renderSquareUV(40, 40, 0, 0.25F, 0.5625F, 0.84375F, 0.53125F);
		}else if(aircraft.rotationPitch <= -24){
			renderSquareUV(40, 40, 0, 0.4375F, 0.75F, 0.84375F, 0.53125F);
		}else{
			renderSquareUV(40, 40, 0, 0.34375F - aircraft.rotationPitch*0.00390625F, 0.65625F - 0.00390625F*aircraft.rotationPitch, 0.84375F, 0.53125F);
		}
    	
		GL11.glRotatef(-90, 0, 0, 1);
    	renderSquareUV(60, 60, 0, 0.25F, 0.5F, 0.25F, 0F);
    	GL11.glRotatef(aircraft.rotationRoll, 0, 0, 1);
    	
    	if(lightsOn){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
			renderSquareUV(60, 60, 0, 0.5F, 0.75F, 0.25F, 0F);
	    	Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}else{
			renderSquareUV(60, 60, 0, 0.5F, 0.75F, 0.25F, 0F);
		}
	}
	
	private static void drawAltimeter(EntityMultipartVehicle aircraft, boolean lightsOn){
    	drawGaugeBase(lightsOn);
    	drawScaledString("ALTITUDE", -20, +14, 0.5F);
        drawDialIncrements(0, 0, -180, 180, 25, 2, 51);
        drawDialIncrements(0, 0, -180, 180, 25, 5, 11);
        drawDialNumbers(0, 0, 0, 320,  17, 0, 1, 9, 0.7F);
        drawShortPointer(0, 0, (float) (.36*(aircraft.posY - (ConfigSystem.getBooleanConfig("SeaLevelOffset") ? aircraft.worldObj.provider.getAverageGroundLevel() : 0))), 20, 6);
        drawLongPointer(0, 0, (float) (3.6*(aircraft.posY - (ConfigSystem.getBooleanConfig("SeaLevelOffset") ? aircraft.worldObj.provider.getAverageGroundLevel() : 0))), 35, 3);
    }
    
	private static void drawHeadingIndicator(EntityMultipartVehicle aircraft, boolean lightsOn){
    	drawGaugeBase(lightsOn);
    	renderSquareUV(40, 40, 0, 0.75F, 1F, 0.5F, 0.25F);
    	
    	drawScaledString("HEADING", -18, +14, 0.5F);
    	
        GL11.glPushMatrix();
        rotationHelper(0, 0, -aircraft.rotationYaw);
        drawDialIncrements(0, 0, 0, 360, 25, 5, 25);
        drawDialIncrements(0, 0, 7.5F, 367.5F, 25, 3, 25);
        GL11.glScalef(0.60F, 0.60F, 0.60F);
        drawString("S", -3, -32);
        GL11.glRotatef(30, 0, 0, 1);
        drawString("3", -3, -32);
        GL11.glRotatef(30, 0, 0, 1);
        drawString("6", -3, -32);
        GL11.glRotatef(30, 0, 0, 1);
        drawString("W", -2, -32);
        GL11.glRotatef(30, 0, 0, 1);
        drawString("12", -4, -32);
        GL11.glRotatef(30, 0, 0, 1);
        drawString("15", -4, -32);
        GL11.glRotatef(30, 0, 0, 1);
        drawString("N", -2, -32);
        GL11.glRotatef(30, 0, 0, 1);
        drawString("21", -4, -32);
        GL11.glRotatef(30, 0, 0, 1);
        drawString("24", -4, -32);
        GL11.glRotatef(30, 0, 0, 1);
        drawString("E", -3, -32);
        GL11.glRotatef(30, 0, 0, 1);
        drawString("30", -5, -32);
        GL11.glRotatef(30, 0, 0, 1);
        drawString("33", -5, -32);
        GL11.glRotatef(30, 0, 0, 1);
        GL11.glPopMatrix();
    }
    
	private static void drawAirspeedIndicator(EntityMultipartVehicle aircraft, boolean lightsOn){
    	drawGaugeBase(lightsOn);

    	drawScaledString("BLK/S", -15, +14, 0.5F);
    	drawDialColoring(0, 0, 292.5F, 330, 25, 3, new float[] {1, 0, 0});
    	drawDialColoring(0, 0, 217.5F, 292.5F, 25, 3, new float[] {1, 1, 0});
    	drawDialColoring(0, 0, 105F, 217.5F, 25, 3, new float[] {0, 1, 0});
    	
    	drawDialIncrements(0, 0, 30, 330, 25, 8, 9);
    	drawDialIncrements(0, 0, 30, 330, 25, 3, 41);
    	drawDialNumbers(0, 0, 30, 330, 15, 0, 10, 4, 0.6F);
    	drawLongPointer(0, 0, (float) (30+7.5*aircraft.velocity*ConfigSystem.getDoubleConfig("SpeedFactor")*20), 35, 2);
    }
    
	private static void drawTurnCoordinator(EntityPlane plane, boolean lightsOn){
		drawGaugeBase(lightsOn);
		GL11.glPushMatrix();
    	
    	drawDialIncrements(0, 0, -90, 90, 20, 5, 2);
    	drawDialIncrements(0, 0, -115, 115, 20, 5, 2);
    	
    	GL11.glTranslatef(0, 12.5F, 0);
    	renderSquareUV(50, 13, 0, 0.75F, 1F, 0.625F, 0.5625F);
    	GL11.glTranslatef(0, -12.5F, 0);
    	
    	float turn = Math.max(Math.min(((plane.rotationRoll - plane.prevRotationRoll)/10 + plane.rotationYaw - plane.prevRotationYaw)/0.15F*25F, 50), -50);
    	GL11.glRotatef(turn, 0, 0, 1);
    	renderSquareUV(50, 13, 0, 0.75F, 1F, 0.5625F, 0.5F);
    	GL11.glRotatef(-turn, 0, 0, 1);
    	
    	float slip = (float) plane.sideVec.dot(plane.velocityVec);
    	GL11.glTranslatef(20*slip, 12.5F - Math.abs(slip), 0);
    	renderSquareUV(5, 5, 0, 0.75F, 0.875F, 1F, 0.875F);
    	GL11.glTranslatef(-20*slip, -12.5F + Math.abs(slip), 0);

    	drawScaledString("L", -34, 20, 0.5F);
    	drawScaledString("R", 30, 20, 0.5F);
    	drawScaledString("M.J.", -8, -46, 0.5F);
    	drawScaledString("ELEC", -12, -36, 0.5F);
    	drawScaledString("2 MIN", -14, 36, 0.5F);
    	GL11.glPopMatrix();
	}
	
	private static void drawTurnAndSlipIndicator(EntityPlane plane, boolean lightsOn){
		drawGaugeBase(lightsOn);
		GL11.glPushMatrix();
    	
		GL11.glTranslatef(0, 12.5F, 0);
    	renderSquareUV(50, 13, 0, 0.75F, 1F, 0.625F, 0.5625F);
    	GL11.glTranslatef(0, -12.5F, 0);
    	
    	GL11.glDisable(GL11.GL_TEXTURE_2D);
    	GL11.glColor3f(1, 1, 1);
    	GL11.glBegin(GL11.GL_QUADS);
    	GL11.glVertex2d(-2, -18);
    	GL11.glVertex2d(2, -18);
    	GL11.glVertex2d(3, -20);
    	GL11.glVertex2d(-3, -20);
    	
    	GL11.glVertex2d(-3, -20);
    	GL11.glVertex2d(3, -20);
    	GL11.glVertex2d(3, -24);
    	GL11.glVertex2d(-3, -24);
    	GL11.glEnd();
    	
    	GL11.glRotatef(25, 0, 0, 1);
    	GL11.glBegin(GL11.GL_QUADS);
    	GL11.glVertex2d(-2, -18);
    	GL11.glVertex2d(2, -18);
    	GL11.glVertex2d(2, -20);
    	GL11.glVertex2d(-2, -20);
    	GL11.glEnd();
    	GL11.glBegin(GL11.GL_TRIANGLES);
    	GL11.glVertex2d(0, -22);
    	GL11.glVertex2d(-2, -20);
    	GL11.glVertex2d(2, -20);
    	GL11.glEnd();
    	
    	GL11.glRotatef(-50, 0, 0, 1);
    	GL11.glBegin(GL11.GL_QUADS);
    	GL11.glVertex2d(-2, -18);
    	GL11.glVertex2d(2, -18);
    	GL11.glVertex2d(2, -20);
    	GL11.glVertex2d(-2, -20);
    	GL11.glEnd();
    	GL11.glBegin(GL11.GL_TRIANGLES);
    	GL11.glVertex2d(0, -22);
    	GL11.glVertex2d(-2, -20);
    	GL11.glVertex2d(2, -20);
    	GL11.glEnd();
    	
    	float turn = Math.max(Math.min((plane.rotationYaw - plane.prevRotationYaw)/0.15F*25F, 50), -50);
    	GL11.glRotatef(turn + 25, 0, 0, 1);
    	GL11.glBegin(GL11.GL_QUADS);
    	GL11.glVertex2d(-2, -10);
    	GL11.glVertex2d(2, -10);
    	GL11.glVertex2d(2, -18);
    	GL11.glVertex2d(-2, -18);
    	GL11.glEnd();
    	
    	GL11.glColor3f(0, 0, 0);
    	GL11.glBegin(GL11.GL_QUADS);
    	GL11.glVertex2d(-2, 3);
    	GL11.glVertex2d(2, 3);
    	GL11.glVertex2d(2, -10);
    	GL11.glVertex2d(-2, -10);
    	
    	GL11.glVertex2d(-3, 3);
    	GL11.glVertex2d(3, 3);
    	GL11.glVertex2d(3, -3);
    	GL11.glVertex2d(-3, -3);
    	GL11.glEnd();
    	GL11.glRotatef(-turn, 0, 0, 1);
    	GL11.glColor3f(1, 1, 1);
    	GL11.glEnable(GL11.GL_TEXTURE_2D);
    	
    	float slip = (float) plane.sideVec.dot(plane.velocityVec);
    	GL11.glTranslatef(20*slip, 12.5F - Math.abs(slip), 0);
    	renderSquareUV(5, 5, 0, 0.75F, 0.875F, 1F, 0.875F);
    	GL11.glTranslatef(-20*slip, -12.5F + Math.abs(slip), 0);

    	drawScaledString("L", -30, -30, 0.5F);
    	drawScaledString("R", 26, -30, 0.5F);
    	drawScaledString("2 MIN", -14, 36, 0.5F);
    	GL11.glPopMatrix();
	}
	
	private static void drawVerticalSpeedIndicator(EntityMultipartVehicle aircraft, boolean lightsOn){
		drawGaugeBase(lightsOn);

    	drawScaledString("CLIMB", -14, -14, 0.5F);
    	drawScaledString("BLK/S", -14, 10, 0.5F);
    	drawDialNumbers(0, 0, -90, 90, 16, 0, 1, 4, 0.7F);
    	drawDialNumbers(0, 0, -132.5F, -217.5F, 16, 1, 1, 2, 0.7F);
    	drawDialIncrements(0, 0, -260, 80, 25, 5, 9);
    	drawDialIncrements(0, 0, -132.5F, -47.5F, 25, 3, 11);
    	drawDialIncrements(0, 0, -47.5F, 80, 25, 2, 16);
    	drawDialIncrements(0, 0, -260, -132.5F, 25, 2, 16);
    	drawLongPointer(0, 0, (float) (-90+10.625*aircraft.motionY*20), 35, 2);
	}
	
	private static void drawLiftReserveIndicator(EntityPlane plane, boolean lightsOn){
		drawGaugeBase(lightsOn);
    	GL11.glPushMatrix();
    	
    	drawScaledString("LIFT RESERVE", -32, 14, 0.5F);
    	drawDialColoring(0, 20, -37, -35, 35, 10, new float[] {0, 0, 0});
    	drawDialColoring(0, 20, -35, -26, 32, 7, new float[] {1, 0, 0});
    	drawDialColoring(0, 20, -26, -24, 32, 7, new float[] {0, 0, 0});
    	drawDialColoring(0, 20, -24, -11, 32, 7, new float[] {1, 1, 0});
    	drawDialColoring(0, 20, -11, -9, 32, 7, new float[] {0, 0, 0});
    	drawDialColoring(0, 20, -9, 35, 32, 7, new float[] {0, 1, 0});
    	drawDialColoring(0, 20, -35, 35, 35, 3, new float[] {1, 1, 1});
    	drawDialColoring(0, 20, 35, 37, 35, 10, new float[] {0, 0, 0});
    	
    	GL11.glDisable(GL11.GL_TEXTURE_2D);
    	GL11.glColor3f(0, 0, 0);
        for(float theta=-33; theta<=33; theta+=3){
        	GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2d(34.5*Math.sin(Math.toRadians(theta)), 20-34.5*Math.cos(Math.toRadians(-theta)));
            GL11.glVertex2d((34.5-2)*Math.sin(Math.toRadians(theta)), 20-(34.5-2)*Math.cos(Math.toRadians(-theta)));
            GL11.glEnd();
        }
    	

        float angle = (float) Math.max(Math.min(plane.trackAngle*3 + 20, 35), -35);
        rotationHelper(0, 0 + 20, angle);
    	GL11.glColor3f(0, 0, 0);
    	GL11.glBegin(GL11.GL_QUADS);
    	GL11.glVertex2d(-0.5, -5);
    	GL11.glVertex2d(0.5, -5);
    	GL11.glVertex2d(0.5, -18);
    	GL11.glVertex2d(-0.5, -18);
    	GL11.glEnd();
    	
    	GL11.glColor3f(1, 1, 1);
    	GL11.glBegin(GL11.GL_QUADS);
    	GL11.glVertex2d(-1.5, 20);
    	GL11.glVertex2d(1.5, 20);
    	GL11.glVertex2d(1.5, 12);
    	GL11.glVertex2d(-1.5, 12);
    	
    	GL11.glVertex2d(-0.5, 12);
    	GL11.glVertex2d(0.5, 12);
    	GL11.glVertex2d(0.5, -5);
    	GL11.glVertex2d(-0.5, -5);
    	GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
        
        //This indicator has a special function of sounding a buzzer if the track angle is too high (stall).
        if(plane.trackAngle<=-17){
        	if(stallBuzzer == null){
        		stallBuzzer = new StallSound();
        		Minecraft.getMinecraft().getSoundHandler().playSound(stallBuzzer);
        	}
        	stallBuzzer.setOn(plane);
        }else if(stallBuzzer != null){
        	stallBuzzer.setOff(plane);
        }
	}
	
	private static void drawTrimIndicator(EntityPlane plane, boolean lightsOn){
    	drawGaugeBase(lightsOn);
    	
    	drawScaledString("TRIM", -10, 40, 0.5F);
    	drawScaledString("NOSE", -10, -49, 0.5F);
    	drawScaledString("WING", -44, -13, 0.5F);
    	drawScaledString("STAB", 24, -13, 0.5F);
    	
    	drawDialIncrements(0, -10, -135, 135, 10, 3, 9);
    	drawDialIncrements(-12, 8, -165, -15, 10, 3, 5);
    	drawDialIncrements(-12, 8, 15, 165, 10, 3, 5);
    	drawDialIncrements(12, 8, -45, 225, 10, 3, 7);
    	
    	drawDialColoring(0, -10, -135, 135, 10, 1, new float[] {0, 0.5F, 0});
    	drawDialColoring(-12, 8, -165, -15, 10, 1, new float[] {0, 0.5F, 0});
    	drawDialColoring(-12, 8, 15, 165, 10, 1, new float[] {0, 0.5F, 0});
    	drawDialColoring(12, 8, -45, 225, 10, 1, new float[] {0, 0.5F, 0});
    	
		drawLongPointer(0, -10, 135F/20F*plane.rudderTrim/10F, 14, 2);
		rotationHelper(-12, 8, 75F/10F*plane.aileronTrim/10F + 90);
		GL11.glTranslated(-12, 2, 0);
		renderSquareUV(2, 12, 0, 0.09375F, 0.15625F, 0.875F, 0.5F);
		GL11.glTranslated(0, 13, 0);
		renderSquareUV(2, 12, 0, 0.09375F, 0.15625F, 0.5F, 0.875F);
		GL11.glTranslated(12, -15, 0);
		rotationHelper(-12, 8, -(75F/10F*plane.aileronTrim/10F + 90));
		drawLongPointer(12, 8, 135F/15F*plane.elevatorTrim/10F + 90, 14, 2);
    }
	
	private static void drawElectricalGauge(EntityMultipartVehicle aircraft, boolean lightsOn){
		drawGaugeBase(lightsOn);
    	
		drawDialColoring(-17, 0, 48.75F, 76.25F, 16, 3, new float[] {0, 1, 0});
		drawDialColoring(-17, 0, 124.375F, 145F, 16, 3, new float[] {1, 0, 0});		

    	drawDialIncrements(-17, 0, 35, 145, 16, 3, 5);
    	drawDialIncrements(17, 0, -145, -35, 16, 3, 5);
    	drawScaledString("16", -32, -30, 0.5F);
    	drawScaledString("8", -18, -3, 0.5F);
    	drawScaledString("0", -28, 22, 0.5F);
    	drawScaledString("+2", 22, -30, 0.5F);
    	drawScaledString("0", 12, -3, 0.5F);
    	drawScaledString("-2", 22, 22, 0.5F);
    	
    	drawLongPointer(-17, 0, (float) (145 - 6.875F*aircraft.electricPower), 26, 2);
    	drawLongPointer(17, 0, (float) (-90 - 27.5F*Math.min(aircraft.electricFlow*100F, 2)), 26, 2);
    	
    	drawScaledString("V", -40, -10, 0.5F);
    	drawScaledString("M", -40, 0, 0.5F);
    	drawScaledString("J", -40, 10, 0.5F);
    	drawScaledString("A", 38, -10, 0.5F);
    	drawScaledString("M", 38, 0, 0.5F);
    	drawScaledString("P", 38, 10, 0.5F);
	}
	
	private static void drawFuelGauge(EntityMultipartVehicle aircraft, boolean lightsOn){
    	drawGaugeBase(lightsOn);
    	
    	drawScaledString("BUCKETS", -20, 14, 0.5F);
    	drawScaledString("FUEL", -10, 24, 0.5F);
    	drawScaledString("0", -40, -10, 0.5F);
    	drawScaledString(String.valueOf(aircraft.pack.motorized.fuelCapacity/1000/2F), -7, -45, 0.5F);
    	drawScaledString(String.valueOf(aircraft.pack.motorized.fuelCapacity/1000), 35, -10, 0.5F);
        drawDialIncrements(0, 8, -50, 50, 25, 7, 5);
        drawDialColoring(0, 8, -50, 50, 18, 2, new float[] {1, 1, 1});
    	drawDialColoring(0, 8, -50, -45, 25, 9, new float[] {1, 0, 0});
        drawLongPointer(0, 8, (float) (-50+aircraft.fuel/aircraft.pack.motorized.fuelCapacity*100F), 35, 3);
    }
	
	private static void drawTachometer(EntityMultipartVehicle aircraft, boolean lightsOn, byte engineNumber){
    	drawGaugeBase(lightsOn);
    	drawScaledString("RPM", -10, 14, 0.5F);
    	if(engineNumber == 0){
    		int lowestMaxRPM = 9999;
    		for(byte i=1; i<=aircraft.getNumberEngineBays(); ++i){
    			if(aircraft.getEngineByNumber(i) != null){
    				lowestMaxRPM = Math.min(lowestMaxRPM, aircraft.getEngineByNumber(i).maxRPM);
    			}
    		}
    		drawDialColoring(0, 0, -135+lowestMaxRPM/10, 165, 25, 4, new float[] {1, 0, 0});
		}else{
			if(aircraft.getEngineByNumber(engineNumber ) != null){
				drawDialColoring(0, 0, -135+aircraft.getEngineByNumber(engineNumber).maxRPM/10, 165, 25, 4, new float[] {1, 0, 0});
			}
		}
		drawDialIncrements(0, 0, -135, 165, 25, 5, 61);
		drawDialIncrements(0, 0, -135, 165, 25, 9, 13);
        drawDialNumbers(0, 0, -135, 165, 13, 0, 5, 6, 0.6F);
        if(engineNumber == 0){
    		for(byte i=1; i<=aircraft.getNumberEngineBays(); ++i){
    			if(aircraft.getEngineByNumber(i) != null){
	    			drawLongPointer(0, 0, (float) (-135+aircraft.getEngineByNumber(i).RPM/10), 30, 3);
    			}
    		}
    	}else{
    		if(aircraft.getEngineByNumber(engineNumber) != null){
    			drawLongPointer(0, 0, (float) (-135+aircraft.getEngineByNumber(engineNumber).RPM/10), 30, 3);
    		}
    	}
    }
	
	private static void drawFuelFlowGauge(EntityMultipartVehicle aircraft, boolean lightsOn, byte engineNumber){
    	drawGaugeBase(lightsOn);
    	
    	drawScaledString("FUEL", -10, 14, 0.5F);
    	drawScaledString("FLOW", -12, 24, 0.5F);
    	drawScaledString("BKT/M", -14, -20, 0.5F);
    	
        drawDialIncrements(0, 0, -135, 135, 25, 3, 41);
        drawDialIncrements(0, 0, -135, 135, 25, 5, 9);
        drawDialNumbers(0, 0, -135, 135, 16, 0, 1, 4, 0.6F);
        if(engineNumber == 0){
        	double totalFuelFlow = 0;
        	for(byte i=1; i<=aircraft.getNumberEngineBays(); ++i){
    			if(aircraft.getEngineByNumber(i) != null){
    				totalFuelFlow += aircraft.getEngineByNumber(i).fuelFlow;
    			}
    		}
    		drawLongPointer(0, 0, (float) (-135 + totalFuelFlow*20*60*60/1000), 30, 3);
    	}else{
    		if(aircraft.getEngineByNumber(engineNumber) != null){
    			drawLongPointer(0, 0, (float) (-135 + aircraft.getEngineByNumber(engineNumber).fuelFlow*20*60*60/1000), 30, 3);
    		}
    	}
    	
    }
	
	private static void drawEngineTempGauge(EntityMultipartVehicle aircraft, boolean lightsOn, byte engineNumber){
		drawGaugeBase(lightsOn);
    	
    	drawScaledString("TEMP", -12, 14, 0.5F);
    	drawDialColoring(0, 0, -110.7F, -86.4F, 25, 3, new float[] {1, 1, 0});
    	drawDialColoring(0, 0, -86.4F, 131, 25, 3, new float[] {0, 1, 0});
    	
    	drawDialIncrements(0, 0, -135, 135, 25, 6, 5);
    	drawDialIncrements(0, 0, -135, 135, 25, 3, 21);
    	
    	drawDialColoring(0, 0, 131, 135, 25, 6, new float[] {1, 0, 0});
    	GL11.glColor3f(1, 1, 1);
    	drawDialNumbers(0, 0, -135, 135, 16, 50, 50, 4, 0.5F);
        
        if(engineNumber == 0){
        	for(byte i=1; i<=aircraft.getNumberEngineBays(); ++i){
    			if(aircraft.getEngineByNumber(i) != null){
	    			drawLongPointer(0, 0, (float) (-135 + ((aircraft.getEngineByNumber(i).temp*9F/5F + 32) - 50)*1.35), 30, 3);
    			}
    		}
    	}else{
    		if(aircraft.getEngineByNumber(engineNumber) != null){
    			drawLongPointer(0, 0, (float) (-135 + ((aircraft.getEngineByNumber(engineNumber).temp*9F/5F + 32) - 50)*1.35), 30, 3);
    		}
    	}
	}
	
	private static void drawOilPressureGauge(EntityMultipartVehicle aircraft, boolean lightsOn, byte engineNumber){
		drawGaugeBase(lightsOn);
		
    	drawScaledString("PSI", -10, 14, 0.5F);
    	drawDialColoring(0, 0, -135, -63, 25, 3, new float[] {1, 0, 0});
    	drawDialColoring(0, 0, -63, -27, 25, 3, new float[] {1, 1, 0});
    	drawDialColoring(0, 0, -27, 45, 25, 3, new float[] {0, 1, 0});
    	
    	drawDialIncrements(0, 0, -135, 135, 25, 6, 11);
    	drawDialIncrements(0, 0, -121.5F, 121.5F, 25, 3, 10);
    	GL11.glColor3f(1, 1, 1);
    	drawDialNumbers(0, 0, -135, 135, 16, 0, 25, 6, 0.5F);
        if(engineNumber == 0){
        	for(byte i=1; i<=aircraft.getNumberEngineBays(); ++i){
    			if(aircraft.getEngineByNumber(i) != null){
	    			drawLongPointer(0, 0, (float) (-135 + aircraft.getEngineByNumber(i).oilPressure*1.8F), 30, 3);
    			}
    		}
    	}else{
    		if(aircraft.getEngineByNumber(engineNumber) != null){
    			drawLongPointer(0, 0, (float) (-135 + aircraft.getEngineByNumber(engineNumber).oilPressure*1.8F), 30, 3);
    		}
    	}
	}
    /**
     * Draws a long pointer with the given parameters.
     */
    private static void drawLongPointer(int centerX, int centerY, float angle, int length, int width){
    	GL11.glPushMatrix();
    	GL11.glColor3f(1,1,1);
    	textureManager.bindTexture(instrumentTexture);
    	rotationHelper(centerX, centerY, angle);
    	//GL11.glTranslatef(centerX, -length*0.25F + centerY, 0);
    	GL11.glTranslatef(centerX, -length*0.25F + centerY, 0);
    	renderSquareUV(width, length, 0, 0.09375F, 0.15625F, 1F, 0.5F);
        GL11.glPopMatrix();
    }
    
    /**
     * Draws a short pointer with the given rotation.
     */
    private static void drawShortPointer(int centerX, int centerY, float angle, int length, int width){
        GL11.glPushMatrix();
        GL11.glColor3f(1,1,1);
        textureManager.bindTexture(instrumentTexture);
        rotationHelper(centerX, centerY, angle);
        GL11.glTranslatef(centerX, -length*0.0625F + centerY, 0);
        renderSquareUV(width, length, 0, 0.03125F, 0.21875F, 0.5F, 0F);
        GL11.glPopMatrix();
    }
}
