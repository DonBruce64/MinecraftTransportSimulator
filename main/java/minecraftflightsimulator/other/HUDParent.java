package minecraftflightsimulator.other;

import java.awt.Color;

import minecraftflightsimulator.ClientProxy;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.EntityParent;
import minecraftflightsimulator.entities.EntityPlane;
import minecraftflightsimulator.modelrenders.ModelRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class HUDParent{
	protected static final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
	private static final TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
	private static final ResourceLocation instruments = new ResourceLocation("mfs", "textures/instruments.png");
	public static HUDParent instance = new HUDParent();
	
	public HUDParent(){}
	
	public static void drawBasicPlaneHUD(EntityPlane plane, int width, int height, ResourceLocation backplateTexture, ResourceLocation moldingTexture){
		if(ClientProxy.hudMode == 3){
			drawLowerConsole(width, height, backplateTexture, moldingTexture);
			for(int i=5; i<plane.instrumentList.size(); ++i){
				if(plane.instrumentList.get(i) != null){
					drawInstrument(plane, (5*(i-5)+6)*width/32, height - 32, plane.instrumentList.get(i).getItemDamage(), true);
				}
			}
			height -= 64;
		}
		if(ClientProxy.hudMode > 1){
			drawUpperConsole(width, height, backplateTexture, moldingTexture);
			drawLeftConsole(width, height, backplateTexture, moldingTexture);
			drawRightConsole(width, height, backplateTexture, moldingTexture);
			
			if(plane.hasFlaps){
				drawFlapIndicator(plane, width/8 - 15, height - 19, true);
			}
	    	drawThrottle(plane, 7*width/8 + 10, height - 18, true);
	    	drawParkingBrake(plane, 15*width/16 + 14, height - 18, true);
	    	
	    	GL11.glPushMatrix();
	    	GL11.glScalef(0.75F, 0.75F, 0.75F);
	    	if(plane.instrumentList.get(0) != null){
	    		drawInstrument(plane, width/4, (height - 24)*4/3, plane.instrumentList.get(0).getItemDamage(), true);
	    	}
	    	if(plane.instrumentList.get(4) != null){
	    		drawInstrument(plane, width*17/16, (height - 24)*4/3, plane.instrumentList.get(4).getItemDamage(), true);
	    	}
	    	GL11.glPopMatrix();
		}
		if(ClientProxy.hudMode != 0){
			for(int i=1; i<4; ++i){
				if(plane.instrumentList.get(i) != null){
					drawInstrument(plane, (5*i+6)*width/32, height - 32, plane.instrumentList.get(i).getItemDamage(), true);
				}
			}
		}
	}
	
	public static void drawInstrument(EntityPlane plane, int x, int y, int type, boolean hud){
		if(type == 0){
			drawAttitudeIndicator(plane, x, y, hud);
		}else if(type == 1){
			drawAltimeter(plane, x, y, hud);
		}else if(type == 2){
			drawHeadingIndicator(plane, x, y, hud);
		}else if(type == 3){
			drawAirspeedIndicator(plane, x, y, hud);
		}else if(type == 4){
			drawTurnCoordinator(plane, x, y, hud);
		}else if(type == 5){
			//drawTurnAndSlipIndicator
		}else if(type == 6){
			//drawVerticalSpeedIndicator
		}else if(type == 7){
			//drawLiftReserveIndicator
		}else if(type == 8){
			//DUMMY
		}else if(type == 9){
			//DUMMY
		}else if(type == 10){
			drawTachometer(plane, x, y, hud);
		}else if(type == 11){
			drawFuelGauge(plane, x, y, hud);
		}else if(type == 12){
			drawFuelFlowGauge(plane, x, y, hud);
		}else if(type == 13){
			//drawEngineTempGauge()
		}else if(type == 14){
			//drawOilPressureGauge()
		}else if(type == 15){
			drawThrottle(plane, x, y, hud);
		}else if(type == 16){
			drawParkingBrake(plane, x, y, hud);
		}else if(type == 17){
			drawFlapIndicator(plane, x, y, hud);
		}
	}
    
	private static void drawUpperConsole(int width, int height, ResourceLocation backplateTexture, ResourceLocation moldingTexture){
		ModelRenderHelper.startRender();
    	textureManager.bindTexture(backplateTexture);
    	ModelRenderHelper.renderQuadUV(width/4, width/4, 3*width/4, 3*width/4, height-64, height, height, height-64, 0, 0, 0, 0, 0, 3, 0, 1, false);
    	ModelRenderHelper.endRender();
    	
    	ModelRenderHelper.startRender();
    	textureManager.bindTexture(moldingTexture);
    	ModelRenderHelper.renderQuadUV(width/4, 3*width/4, 3*width/4, width/4, height-64, height-64, height-80, height-80, 0, 0, 0, 0, 0, 1, 0, 8, false);
    	ModelRenderHelper.endRender();
    }
        
	private static void drawLeftConsole(int width, int height, ResourceLocation backplateTexture, ResourceLocation moldingTexture){
		ModelRenderHelper.startRender();
    	textureManager.bindTexture(backplateTexture);
    	ModelRenderHelper.renderQuadUVCustom(0, width/4, width/4, 0, height, height, height-64, height-32, 0, 0, 0, 0, 0, 1.5, 1.5, 0, 1, 1, 0, 0.5, false);
    	ModelRenderHelper.endRender();
    	
    	ModelRenderHelper.startRender();
    	textureManager.bindTexture(moldingTexture);
    	ModelRenderHelper.renderQuadUV(0, width/4, width/4, 0, height-32, height-64, height-80, height-48, 0, 0, 0, 0, 0, 1, 0, 4, false);
    	ModelRenderHelper.endRender();
    }
    
	private static void drawRightConsole(int width, int height, ResourceLocation backplateTexture, ResourceLocation moldingTexture){
		ModelRenderHelper.startRender();
    	textureManager.bindTexture(backplateTexture);
    	ModelRenderHelper.renderQuadUVCustom(3*width/4, width, width, 3*width/4, height, height, height-32, height-64, 0, 0, 0, 0, 0, 1.5, 1.5, 0, 1, 1, 0.5, 0, false);
    	ModelRenderHelper.endRender();
    	
    	ModelRenderHelper.startRender();
    	textureManager.bindTexture(moldingTexture);
    	ModelRenderHelper.renderQuadUV(3*width/4, width, width, 3*width/4, height-63, height-32, height-48, height-80, 0, 0, 0, 0, 0, 1, 0, 4, false);
    	ModelRenderHelper.endRender();
    }
    
	private static void drawLowerConsole(int width, int height, ResourceLocation backplateTexture, ResourceLocation moldingTexture){
    	ModelRenderHelper.startRender();
    	textureManager.bindTexture(backplateTexture);
    	ModelRenderHelper.renderQuadUV(0, 0, width, width, height-64, height, height, height-64, 0, 0, 0, 0, 0, 6, 0, 1, false);
    	ModelRenderHelper.endRender();
    }
    
	private static void drawGaugeBase(int centerX, int centerY, int size){
    	ModelRenderHelper.startRender();
    	textureManager.bindTexture(instruments);
    	ModelRenderHelper.renderSquareUV(centerX-size, centerX+size, centerY+size, centerY-size, 0, 0, 0.75, 1, 0, 0.25, false);
    	ModelRenderHelper.endRender();
    }
    
	private static void drawThrottle(EntityParent parent, int centerX, int centerY, boolean hud){
		ModelRenderHelper.startRender();
    	textureManager.bindTexture(instruments);
		if(!hud){
			GL11.glPushMatrix();
    		GL11.glDisable(GL11.GL_LIGHTING);
    		ModelRenderHelper.renderSquareUV(centerX-5.25, centerX+5.25, centerY+5.25, centerY-5.25, 0, 0, 0.75, 0.875, 0.875, 1, false);
    		
    		ModelRenderHelper.renderQuadUV(centerX-1.75, centerX-1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY+1.75, centerY+1.75, -7-parent.throttle/10F, 0, 0, -7-parent.throttle/10F, 0.640625, 0.734375, 0.890625, 0.984375, false);
    		ModelRenderHelper.renderQuadUV(centerX+1.75, centerX+1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY-1.75, centerY-1.75, -7-parent.throttle/10F, 0, 0, -7-parent.throttle/10F, 0.640625, 0.734375, 0.890625, 0.984375, false);
        	ModelRenderHelper.renderQuadUV(centerX+1.75, centerX+1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY-1.75, centerY-1.75, -7-parent.throttle/10F, 0, 0, -7-parent.throttle/10F, 0.640625, 0.734375, 0.890625, 0.984375, false);
        	ModelRenderHelper.renderQuadUV(centerX-1.75, centerX-1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY+1.75, centerY+1.75, -7-parent.throttle/10F, 0, 0, -7-parent.throttle/10F, 0.640625, 0.734375, 0.890625, 0.984375, false);
        	
        	ModelRenderHelper.renderSquareUV(centerX-7, centerX+7, centerY+7, centerY-7, -7-parent.throttle/10F, -7-parent.throttle/10F, 0.75, 0.875, 0.875, 1, true);
    		GL11.glEnable(GL11.GL_LIGHTING);
    		GL11.glPopMatrix();
    	}else{
        	ModelRenderHelper.renderSquareUV(centerX-5.25, centerX+5.25, centerY+0.175, centerY-10.5, 0, 0, 0.75, 0.875, 0.875, 1, false);
        	ModelRenderHelper.renderSquareUV(centerX-1.75, centerX+1.75, centerY+7+parent.throttle/10, centerY-7, 0, 0, 0.640625, 0.734375, 0.890625, 0.984375, false);
        	ModelRenderHelper.renderSquareUV(centerX-7, centerX+7, centerY+7+parent.throttle/10F, centerY-7+parent.throttle/10F, 0, 0, 0.75, 0.875, 0.875, 1, false);
    	}
		ModelRenderHelper.endRender();    	
    }
    
	private static void drawFlapIndicator(EntityPlane plane, int centerX, int centerY, boolean hud){
		ModelRenderHelper.startRender();
    	textureManager.bindTexture(instruments);
    	ModelRenderHelper.renderSquareUV(centerX-11.25, centerX+11.25, centerY+15, centerY-15, 0, 0, 0.515625, 0.609375, 0.875, 1, false);
    	ModelRenderHelper.endRender();
    	
    	if(!hud){
			GL11.glPushMatrix();
    		GL11.glDisable(GL11.GL_LIGHTING);
    		GL11.glTranslatef(0, 0, -0.1F);
    	}
    	
        GL11.glPushMatrix();
    	rotationHelper(centerX, centerY, -90);
    	GL11.glScalef(0.5F, 0.5F, 0.5F);
    	fontRenderer.drawString("FLAPS", centerX*2-15, centerY*2-15, Color.WHITE.getRGB());
    	GL11.glPopMatrix();
    	
    	GL11.glPushMatrix();
    	GL11.glScalef(0.5F, 0.5F, 1);
    	fontRenderer.drawString("0", centerX*2+8, centerY*2-16, Color.WHITE.getRGB());
    	fontRenderer.drawString("35", centerX*2+8, centerY*2+10, Color.WHITE.getRGB());
    	GL11.glPopMatrix();
        
    	ModelRenderHelper.startRender();
    	textureManager.bindTexture(instruments);
    	if(!hud){
    		ModelRenderHelper.renderQuadUV(centerX-5.625, centerX-5.625, centerX-5.625, centerX-5.625, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, -7, 0, 0, -7, 0.421875, 0.453125, 0.921875, 0.953125, false);
    		ModelRenderHelper.renderQuadUV(centerX+1.875, centerX+1.875, centerX+1.875, centerX+1.875, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, -7, 0, 0, -7, 0.421875, 0.453125, 0.921875, 0.953125, false);
        	ModelRenderHelper.renderQuadUV(centerX-5.625, centerX-5.625, centerX+1.875, centerX+1.875, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, centerY-8+plane.flapAngle/25, 0, -7, -7, 0, 0.421875, 0.453125, 0.921875, 0.953125, false);
        	ModelRenderHelper.renderQuadUV(centerX-5.625, centerX-5.625, centerX+1.875, centerX+1.875, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, -7, 0, 0, -7, 0.421875, 0.453125, 0.921875, 0.953125, false);
        	ModelRenderHelper.renderQuadUV(centerX-5.625, centerX-5.625, centerX+1.875, centerX+1.875, centerY-8+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-7+plane.flapAngle/25, centerY-8+plane.flapAngle/25, -7, -7, -7, -7, 0.421875, 0.453125, 0.921875, 0.953125, false);
    		ModelRenderHelper.endRender();
    		GL11.glEnable(GL11.GL_LIGHTING);
    		GL11.glPopMatrix();
    	}else{
    		ModelRenderHelper.renderSquareUV(centerX-5.625, centerX+1.875, centerY-0.5+plane.flapAngle/25, centerY-8+plane.flapAngle/25, 0, 0, 0.421875, 0.453125, 0.921875, 0.953125, false);
    		ModelRenderHelper.endRender();
    	}
    }
    
	private static void drawParkingBrake(EntityParent parent, int centerX, int centerY, boolean hud){
    	ModelRenderHelper.startRender();
    	textureManager.bindTexture(instruments);
    	
    	if(!hud){
			GL11.glPushMatrix();
    		GL11.glDisable(GL11.GL_LIGHTING);
    		ModelRenderHelper.renderSquareUV(centerX-5.25, centerX+5.25, centerY+5.25, centerY-5.25, 0, 0, 0.75, 0.875, 0.875, 1, false);
        	
    		if(parent.parkingBrakeOn){        		
        		ModelRenderHelper.renderQuadUV(centerX-1.75, centerX-1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY+1.75, centerY+1.75, -20, 0, 0, -20, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		ModelRenderHelper.renderQuadUV(centerX+1.75, centerX+1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY-1.75, centerY-1.75, -20, 0, 0, -20, 0.640625, 0.734375, 0.890625, 0.984375, false);
            	ModelRenderHelper.renderQuadUV(centerX+1.75, centerX+1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY-1.75, centerY-1.75, -20, 0, 0, -20, 0.640625, 0.734375, 0.890625, 0.984375, false);
            	ModelRenderHelper.renderQuadUV(centerX-1.75, centerX-1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY+1.75, centerY+1.75, -20, 0, 0, -20, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		
        		ModelRenderHelper.renderQuadUV(centerX-5, centerX+5, centerX+5, centerX-5, centerY+25, centerY+25, centerY-10, centerY-10, -20, -20, -20, -20, 0.2578125, 0.3671875, 0.921875, 0.953125, false);
    	        ModelRenderHelper.endRender();
    	        GL11.glTranslatef(0, 0, -20.01F);
    	        
    	        GL11.glPushMatrix();
    	    	rotationHelper(centerX, centerY, -90);
    	    	drawScaledString("BRAKE", centerX*2-30, centerY*2-4, 0.5F);
    	    	GL11.glPopMatrix();
        	}else{
        		ModelRenderHelper.renderQuadUV(centerX-1.75, centerX-1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY+1.75, centerY+1.75, -2, 0, 0, -2, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		ModelRenderHelper.renderQuadUV(centerX+1.75, centerX+1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY-1.75, centerY-1.75, -2, 0, 0, -2, 0.640625, 0.734375, 0.890625, 0.984375, false);
            	ModelRenderHelper.renderQuadUV(centerX+1.75, centerX+1.75, centerX-1.75, centerX-1.75, centerY-1.75, centerY-1.75, centerY-1.75, centerY-1.75, -2, 0, 0, -2, 0.640625, 0.734375, 0.890625, 0.984375, false);
            	ModelRenderHelper.renderQuadUV(centerX-1.75, centerX-1.75, centerX+1.75, centerX+1.75, centerY+1.75, centerY+1.75, centerY+1.75, centerY+1.75, -2, 0, 0, -2, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		
    	        ModelRenderHelper.renderSquareUV(centerX-22.5, centerX+12.5, centerY+5, centerY-5, -2, -2, 0.2578125, 0.3671875, 0.921875, 0.953125, false);    		
    	        ModelRenderHelper.endRender();
    	        GL11.glTranslatef(0, 0, -2.01F);
    	        drawScaledString("BRAKE", centerX*2-25, centerY*2-4, 0.5F);
        	}
    		GL11.glEnable(GL11.GL_LIGHTING);
    		GL11.glPopMatrix();
    	}else{
    		ModelRenderHelper.renderSquareUV(centerX-5.25, centerX+5.25, centerY+0.175, centerY-10.5, 0, 0, 0.75, 0.875, 0.875, 1, true);
        	if(parent.parkingBrakeOn){
        		ModelRenderHelper.renderSquareUV(centerX-1.75, centerX+1.75, centerY+15, centerY-7, 0, 0, 0.640625, 0.734375, 0.890625, 0.984375, false);
        		ModelRenderHelper.renderQuadUV(centerX-5, centerX+5, centerX+5, centerX-5, centerY+35, centerY+35, centerY, centerY, 0, 0, 0, 0, 0.2578125, 0.3671875, 0.921875, 0.953125, false);
    	        ModelRenderHelper.endRender();
    	        
    	        GL11.glPushMatrix();
    	    	rotationHelper(centerX, centerY, -90);
    	        drawScaledString("BRAKE", centerX*2-50, centerY*2-4, 0.5F);
    	    	GL11.glPopMatrix();
        	}else{
        		ModelRenderHelper.renderSquareUV(centerX-1.75, centerX+1.75, centerY+5, centerY-7, 0, 0, 0.640625, 0.734375, 0.890625, 0.984375, false);
    	        ModelRenderHelper.renderSquareUV(centerX-22.5, centerX+12.5, centerY+5, centerY-5, 0, 0, 0.2578125, 0.3671875, 0.921875, 0.953125, false);    		
    	        ModelRenderHelper.endRender();
    	        drawScaledString("BRAKE", centerX*2-25, centerY*2-4, 0.5F);
        	}
    	}
    }
    
	private static void drawAttitudeIndicator(EntityParent parent, int centerX, int centerY, boolean hud){
		GL11.glPushMatrix();
		if(!hud){GL11.glDisable(GL11.GL_LIGHTING);}
		textureManager.bindTexture(instruments);
		
		//0.00390625 is 1 degree of pitch
		rotationHelper(centerX, centerY, -parent.rotationRoll);
		ModelRenderHelper.startRender();
		if(parent.rotationPitch >= 24){
			ModelRenderHelper.renderQuadUV(centerX-20, centerX+20, centerX+20, centerX-20, centerY+20, centerY+20, centerY-20, centerY-20, 0, 0, 0, 0, 0.25, 0.5625, 0.53125, 0.84375, false);
		}else if(parent.rotationPitch <= -24){
			ModelRenderHelper.renderQuadUV(centerX-20, centerX+20, centerX+20, centerX-20, centerY+20, centerY+20, centerY-20, centerY-20, 0, 0, 0, 0, 0.4375, 0.75, 0.53125, 0.84375, false);
		}else{
			ModelRenderHelper.renderQuadUV(centerX-20, centerX+20, centerX+20, centerX-20, centerY+20, centerY+20, centerY-20, centerY-20, 0, 0, 0, 0, 0.34375 - parent.rotationPitch*0.00390625, 0.65625 - 0.00390625*parent.rotationPitch, 0.53125,  0.84375, false);
		}
		ModelRenderHelper.endRender();
    	
		if(!hud){GL11.glTranslatef(0, 0, -0.1F);}
    	ModelRenderHelper.startRender();
    	ModelRenderHelper.renderSquareUV(centerX-30, centerX+30, centerY+30, centerY-30, 0, 0, 0.25, 0.5, 0, 0.25, false);
    	ModelRenderHelper.endRender();
    	
    	if(!hud){GL11.glTranslatef(0, 0, -0.1F);}
    	rotationHelper(centerX, centerY, parent.rotationRoll);
		ModelRenderHelper.startRender();
    	ModelRenderHelper.renderSquareUV(centerX-30, centerX+30, centerY+30, centerY-30, 0, 0, 0.5, 0.75, 0, 0.25, false);
    	ModelRenderHelper.endRender();
    	
    	if(!hud){GL11.glEnable(GL11.GL_LIGHTING);}
		GL11.glPopMatrix();
	}
	
	private static void drawAltimeter(EntityParent parent, int centerX, int centerY, boolean hud){
    	drawGaugeBase(centerX, centerY, 30);
    	if(!hud){
			GL11.glPushMatrix();
    		GL11.glDisable(GL11.GL_LIGHTING);
    		GL11.glTranslatef(0, 0, -0.1F);
    	}
    	
    	drawScaledString("ALTITUDE", centerX*2-20, centerY*2+14, 0.5F);
        drawDialIncrements(centerX, centerY, -180, 180, 25, 2, 51);
        drawDialIncrements(centerX, centerY, -180, 180, 25, 5, 11);
        drawDialNumbers(centerX, centerY, 0, 320,  17, 0, 1, 9, 0.7F);
        if(!hud){GL11.glTranslatef(0, 0, -0.1F);}
        drawShortPointer(centerX, centerY, (float) (.36*parent.posY), 20, 6);
        if(!hud){GL11.glTranslatef(0, 0, -0.1F);}
        drawLongPointer(centerX, centerY, (float) (3.6*parent.posY), 35, 3);
        if(!hud){
        	GL11.glEnable(GL11.GL_LIGHTING);
        	GL11.glPopMatrix();
        }
    }
    
	private static void drawHeadingIndicator(EntityParent parent, int centerX, int centerY, boolean hud){
    	drawGaugeBase(centerX, centerY, 30);
    	if(!hud){
			GL11.glPushMatrix();
    		GL11.glDisable(GL11.GL_LIGHTING);
    		GL11.glTranslatef(0, 0, -0.1F);
    	}
    	
    	ModelRenderHelper.startRender();
    	textureManager.bindTexture(instruments);
    	ModelRenderHelper.renderSquareUV(centerX-20, centerX+20, centerY+20, centerY-20, 0, 0, 0.75, 1, 0.25, 0.5, false);
    	ModelRenderHelper.endRender();
    	
    	drawScaledString("HEADING", centerX*2-18, centerY*2+14, 0.5F);
    	
        GL11.glPushMatrix();
        rotationHelper(centerX, centerY, -parent.rotationYaw);
        drawDialIncrements(centerX, centerY, 0, 360, 25, 5, 25);
        drawDialIncrements(centerX, centerY, 7.5F, 367.5F, 25, 3, 25);
        GL11.glScalef(0.60F, 0.60F, 0.60F);
        centerX=Math.round(((float)centerX)*(1/0.60F));
        centerY=Math.round(((float)centerY)*(1/0.60F));
        fontRenderer.drawString("S", centerX-3, centerY-32, Color.WHITE.getRGB());
        rotationHelper(centerX, centerY, 30);
        fontRenderer.drawString("3", centerX-3, centerY-32, Color.WHITE.getRGB());
        rotationHelper(centerX, centerY, 30);
        fontRenderer.drawString("6", centerX-3, centerY-32, Color.WHITE.getRGB());
        rotationHelper(centerX, centerY, 30);
        fontRenderer.drawString("W", centerX-2, centerY-32, Color.WHITE.getRGB());
        rotationHelper(centerX, centerY, 30);
        fontRenderer.drawString("12", centerX-4, centerY-32, Color.WHITE.getRGB());
        rotationHelper(centerX, centerY, 30);
        fontRenderer.drawString("15", centerX-4, centerY-32, Color.WHITE.getRGB());
        rotationHelper(centerX, centerY, 30);
        fontRenderer.drawString("N", centerX-2, centerY-32, Color.WHITE.getRGB());
        rotationHelper(centerX, centerY, 30);
        fontRenderer.drawString("21", centerX-4, centerY-32, Color.WHITE.getRGB());
        rotationHelper(centerX, centerY, 30);
        fontRenderer.drawString("24", centerX-4, centerY-32, Color.WHITE.getRGB());
        rotationHelper(centerX, centerY, 30);
        fontRenderer.drawString("E", centerX-3, centerY-32, Color.WHITE.getRGB());
        rotationHelper(centerX, centerY, 30);
        fontRenderer.drawString("30", centerX-5, centerY-32, Color.WHITE.getRGB());
        rotationHelper(centerX, centerY, 30);
        fontRenderer.drawString("33", centerX-5, centerY-32, Color.WHITE.getRGB());
        rotationHelper(centerX, centerY, 30);
        GL11.glPopMatrix();
        if(!hud){
        	GL11.glEnable(GL11.GL_LIGHTING);
        	GL11.glPopMatrix();
        }
    }
    
	private static void drawAirspeedIndicator(EntityParent parent, int centerX, int centerY, boolean hud){
    	drawGaugeBase(centerX, centerY, 30);
		if(!hud){
			GL11.glPushMatrix();
    		GL11.glDisable(GL11.GL_LIGHTING);
    		GL11.glTranslatef(0, 0, -0.1F);
    	}
    	
    	drawScaledString("BLK/S", centerX*2-15, centerY*2+14, 0.5F);
    	drawDialColoring(centerX, centerY, 292.5F, 330, 25, 3, new float[] {1, 0, 0});
    	drawDialColoring(centerX, centerY, 217.5F, 292.5F, 25, 3, new float[] {1, 1, 0});
    	drawDialColoring(centerX, centerY, 105F, 217.5F, 25, 3, new float[] {0, 1, 0});
    	if(!hud){GL11.glTranslatef(0, 0, -0.1F);}
    	drawDialIncrements(centerX, centerY, 30, 330, 25, 8, 9);
    	drawDialIncrements(centerX, centerY, 30, 330, 25, 3, 41);
    	drawDialNumbers(centerX, centerY, 30, 330, 15, 0, 10, 4, 0.6F);
    	drawLongPointer(centerX, centerY, (float) (30+7.5*parent.velocity*MFS.planeSpeedFactor*20), 35, 2);
    	
    	if(!hud){
    		GL11.glEnable(GL11.GL_LIGHTING);
    		GL11.glPopMatrix();
    	}
    }
    
	private static void drawTurnCoordinator(EntityParent parent, int centerX, int centerY, boolean hud){
		textureManager.bindTexture(instruments);
		drawGaugeBase(centerX, centerY, 30);
		GL11.glPushMatrix();
    	if(!hud){
    		GL11.glDisable(GL11.GL_LIGHTING);
    		GL11.glTranslatef(0, 0, -0.1F);
    	}

    	drawDialIncrements(centerX, centerY, -90, 90, 20, 5, 2);
    	drawDialIncrements(centerX, centerY, -75, 75, 20, 5, 2);
    	if(!hud){GL11.glTranslatef(0, 0, -0.2F);}
    	
    	ModelRenderHelper.startRender();
    	rotationHelper(centerX, centerY, ((parent.rotationRoll - parent.prevRotationRoll)/10 + parent.rotationYaw - parent.prevRotationYaw)/0.15F*15F);
    	ModelRenderHelper.renderSquareUV(centerX-25, centerX+25, centerY+25, centerY-25, 0, 0, 0.75, 1, 0.5, 0.625, false);
    	ModelRenderHelper.endRender();
    	if(!hud){GL11.glTranslatef(0, 0, 0.1F);}

    	drawScaledString("M.J.", centerX*2-6, centerY*2-46, 0.5F);
    	drawScaledString("ELEC", centerX*2-10, centerY*2-36, 0.5F);
    	drawScaledString("2 MIN", centerX*2-12, centerY*2+14, 0.5F);

    	
    	if(!hud){GL11.glEnable(GL11.GL_LIGHTING);}
    	GL11.glPopMatrix();
	}
	
	private static void drawTachometer(EntityParent parent, int centerX, int centerY, boolean hud){
    	drawGaugeBase(centerX, centerY, 30);
    	if(!hud){
			GL11.glPushMatrix();
    		GL11.glDisable(GL11.GL_LIGHTING);
    		GL11.glTranslatef(0, 0, -0.1F);
    	}
    	
    	drawScaledString("RPM", centerX*2-10, centerY*2+14, 0.5F);
    	drawDialColoring(centerX, centerY, 135, 165, 25, 4, new float[] {1, 0, 0});
    	if(!hud){GL11.glTranslatef(0, 0, -0.1F);}
        drawDialIncrements(centerX, centerY, -135, 165, 25, 5, 56);
        drawDialIncrements(centerX, centerY, -135, 165, 25, 9, 12);
        drawDialNumbers(centerX, centerY, -135, 135, 13, 0, 5, 5, 0.6F);
        for(double speed : parent.getEngineSpeeds()){
        	drawLongPointer(centerX, centerY, (float) (-135+speed/10), 30, 3);
        	if(!hud){GL11.glTranslatef(0, 0, -0.1F);}
        }
        if(!hud){
        	GL11.glEnable(GL11.GL_LIGHTING);
        	GL11.glPopMatrix();
        }
    }
    
	private static void drawFuelGauge(EntityParent parent, int centerX, int centerY, boolean hud){
    	drawGaugeBase(centerX, centerY, 30);
    	if(!hud){
			GL11.glPushMatrix();
    		GL11.glDisable(GL11.GL_LIGHTING);
    		GL11.glTranslatef(0, 0, -0.1F);
    	}
    	
    	drawScaledString("BUCKETS", centerX*2-20, centerY*2+14, 0.5F);
    	drawScaledString("FUEL", centerX*2-10, centerY*2+24, 0.5F);
    	drawScaledString("0", centerX*2-40, centerY*2-10, 0.5F);
    	drawScaledString(String.valueOf(parent.maxFuel/1000/2F), centerX*2-7, centerY*2-45, 0.5F);
    	drawScaledString(String.valueOf(parent.maxFuel/1000), centerX*2+35, centerY*2-10, 0.5F);
        drawDialIncrements(centerX, centerY+8, -50, 50, 25, 7, 5);
        drawDialColoring(centerX, centerY+8, -50, 50, 18, 2, new float[] {1, 1, 1});
        if(!hud){GL11.glTranslatef(0, 0, -0.1F);}
    	drawDialColoring(centerX, centerY+8, -50, -45, 25, 9, new float[] {1, 0, 0});
    	if(!hud){GL11.glTranslatef(0, 0, -0.1F);}
        drawLongPointer(centerX, centerY+8, (float) (-50+parent.fuel/parent.maxFuel*100F), 35, 3);
        if(!hud){
        	GL11.glEnable(GL11.GL_LIGHTING);
        	GL11.glPopMatrix();
        }
    }
	
	private static void drawFuelFlowGauge(EntityParent parent, int centerX, int centerY, boolean hud){
    	drawGaugeBase(centerX, centerY, 30);
    	if(!hud){
			GL11.glPushMatrix();
    		GL11.glDisable(GL11.GL_LIGHTING);
    		GL11.glTranslatef(0, 0, -0.1F);
    	}
    	
    	drawScaledString("FUEL", centerX*2-10, centerY*2+14, 0.5F);
    	drawScaledString("FLOW", centerX*2-12, centerY*2+24, 0.5F);
    	drawScaledString("BKT/M", centerX*2-14, centerY*2-20, 0.5F);
    	
        drawDialIncrements(centerX, centerY, -135, 135, 25, 3, 41);
        drawDialIncrements(centerX, centerY, -135, 135, 25, 5, 9);
        drawDialNumbers(centerX, centerY, -135, 135, 16, 0, 1, 4, 0.6F);
        //TODO figure out why this is not working.
        //System.out.println((parent.prevFuel - parent.fuel)*20*60*60/1000);
    	drawLongPointer(centerX, centerY, (float) (-135 + (parent.prevFuel - parent.fuel)*20*60*60/1000), 30, 3);
        if(!hud){
        	GL11.glEnable(GL11.GL_LIGHTING);
        	GL11.glPopMatrix();
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
        	fontRenderer.drawString(String.valueOf(Math.round(currentNumber)), Math.round(-3*scale), Math.round(-3*scale), Color.WHITE.getRGB());
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
    	ModelRenderHelper.startRender();
    	textureManager.bindTexture(instruments);
    	ModelRenderHelper.renderSquareUV(centerX-width/2, centerX+width/2, centerY+length/2, centerY-length/2, 0, 0, 0.09375, 0.15625, 0.5, 1, false);
        rotationHelper(centerX, centerY, angle);
        GL11.glTranslatef(0, -length*0.25F, 0);
        ModelRenderHelper.endRender();
        GL11.glPopMatrix();
    }
    
    /**
     * Draws a short pointer with the given rotation.
     */
    private static void drawShortPointer(int centerX, int centerY, float angle, int length, int width){
        GL11.glPushMatrix();
        GL11.glColor3f(1,1,1);
        ModelRenderHelper.startRender();
        textureManager.bindTexture(instruments);
        ModelRenderHelper.renderSquareUV(centerX-width/2, centerX+width/2, centerY+length/2, centerY-length/2, 0, 0, 0.03125, 0.21875, 0, 0.5, true);
        rotationHelper(centerX, centerY, angle);
        GL11.glTranslatef(0, -length*0.0625F, 0);
        ModelRenderHelper.endRender();
        GL11.glPopMatrix();
    }
    
    /**
     * Draws a scaled string with the bottom-left at x, y.
     */
    private static void drawScaledString(String string, int x, int y, float scale){
    	GL11.glPushMatrix();
    	GL11.glScalef(scale, scale, scale);
    	fontRenderer.drawString(string, x, y, Color.WHITE.getRGB());
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

