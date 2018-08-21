package minecrafttransportsimulator.rendering;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.dataclasses.MTSControls.Controls;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackControl;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackInstrument;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle.VehicleInstrument;
import minecrafttransportsimulator.systems.CameraSystem;
import net.minecraft.client.Minecraft;

/**Main render class for the HUD.
 * When calling this class it is assumed the screen resolution is the standard 
 * windowed value of 854x480.  If this is not true, the resolution needs to be scaled
 * to fit prior to calling methods in this class, otherwise the HUD will render incorrectly.
 *
 * @author don_bruce
 */
public final class RenderHUD{
	public static final int screenDefaultX = 854;
	public static final int screenDefaultY = 480;
	
	public static void drawMainHUD(EntityMultipartE_Vehicle vehicle, boolean inGUI){		
		GL11.glPushMatrix();
		GL11.glColor4f(1, 1, 1, 1);
		Minecraft.getMinecraft().getTextureManager().bindTexture(RenderMultipart.getTextureForMultipart(vehicle));
		
		if(!inGUI){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}

		if(CameraSystem.hudMode == 3 || inGUI){
			drawLowerPanel(vehicle.pack.rendering.hudBackplaneTexturePercentages);
		}else{
			GL11.glTranslatef(0, screenDefaultY/4, 0);
		}
		if(CameraSystem.hudMode == 2 || CameraSystem.hudMode == 3 || inGUI){
			drawUpperPanel(vehicle.pack.rendering.hudBackplaneTexturePercentages, vehicle.pack.rendering.hudMouldingTexturePercentages);
		}
		if(CameraSystem.hudMode > 0 || inGUI){
			drawInstruments(vehicle, 
					CameraSystem.hudMode == 1 && !inGUI ? 25 : 0,
					CameraSystem.hudMode == 1 && !inGUI  ? 75 : 100,
					CameraSystem.hudMode != 3 && !inGUI ? 75 : 100,
					true);
		}
		if(CameraSystem.hudMode > 1 && !inGUI){
			drawControls(vehicle, inGUI);
		}

		if(!inGUI){
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}
		GL11.glPopMatrix();
	}
	
	public static void drawAuxiliaryHUD(EntityMultipartE_Vehicle vehicle, int width, int height, boolean inGUI){		
		GL11.glPushMatrix();
		GL11.glColor4f(1, 1, 1, 1);
		Minecraft.getMinecraft().getTextureManager().bindTexture(RenderMultipart.getTextureForMultipart(vehicle));
		if(!inGUI){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}
		drawAuxiliaryPanel(vehicle.pack.rendering.hudBackplaneTexturePercentages, vehicle.pack.rendering.hudMouldingTexturePercentages);
		drawInstruments(vehicle, 0, 100, 100, false);
		if(!inGUI){
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}
		GL11.glPopMatrix();
	}
	
	private static void drawInstruments(EntityMultipartE_Vehicle vehicle, int minX, int maxX, int maxY, boolean main){
		for(byte i=0; i<vehicle.pack.motorized.instruments.size(); ++i){
			PackInstrument packInstrument = vehicle.pack.motorized.instruments.get(i);
			//Render the instruments in the correct panel.
			if((packInstrument.optionalEngineNumber == 0 && main) || (packInstrument.optionalEngineNumber != 0 && !main)){
				if(packInstrument.hudpos != null){
					if(packInstrument.hudpos[0] >= minX && packInstrument.hudpos[0] <= maxX && packInstrument.hudpos[1] <= maxY){
						GL11.glPushMatrix();
						GL11.glTranslated(packInstrument.hudpos[0]*screenDefaultX/100D, packInstrument.hudpos[1]*screenDefaultY/100D, 0);
						GL11.glScalef(packInstrument.hudScale, packInstrument.hudScale, packInstrument.hudScale);
						VehicleInstrument instrument = vehicle.getInstrumentInfoInSlot(i);
						if(instrument != null){
							RenderInstruments.drawInstrument(vehicle, instrument, true, packInstrument.optionalEngineNumber);
						}
						GL11.glPopMatrix();
					}
				}
			}
		}
	}
	
	private static void drawControls(EntityMultipartE_Vehicle vehicle, boolean inGUI){
		for(byte i=0; i<vehicle.pack.motorized.controls.size(); ++i){
			PackControl packControl = vehicle.pack.motorized.controls.get(i);
			for(Controls control : Controls.values()){
				if(control.name().toLowerCase().equals(packControl.controlName)){
					GL11.glPushMatrix();
					GL11.glTranslated(packControl.hudpos[0]*screenDefaultX/100, packControl.hudpos[1]*screenDefaultY/100, 0);
					RenderControls.drawControl(vehicle, control, !inGUI);
					GL11.glPopMatrix();
				}
			}
		}
	}
	
	private static void drawUpperPanel(float[] backplanePercentages, float[] mouldingPercentages){
		GL11.glBegin(GL11.GL_QUAD_STRIP);
		//Left backplane (left side)
		GL11.glTexCoord2f(backplanePercentages[0], backplanePercentages[2] + 0.5F*(backplanePercentages[3] - backplanePercentages[2]));
		GL11.glVertex2d(0, 5*screenDefaultY/8);
		GL11.glTexCoord2f(backplanePercentages[0], backplanePercentages[3]);
		GL11.glVertex2d(0.0F, 3*screenDefaultY/4);
		
		//Left backplane (right side)
		GL11.glTexCoord2f(backplanePercentages[0] + 0.25F*(backplanePercentages[1] - backplanePercentages[0]), backplanePercentages[2]);
		GL11.glVertex2d(screenDefaultX/4, screenDefaultY/2);
		GL11.glTexCoord2f(backplanePercentages[0] + 0.25F*(backplanePercentages[1] - backplanePercentages[0]), backplanePercentages[3]);
		GL11.glVertex2d(screenDefaultX/4, 3*screenDefaultY/4);
		
		//Center backlplane (right side)
		GL11.glTexCoord2f(backplanePercentages[0] + 0.75F*(backplanePercentages[1] - backplanePercentages[0]), backplanePercentages[2]);
		GL11.glVertex2d(3*screenDefaultX/4, screenDefaultY/2);
		GL11.glTexCoord2f(backplanePercentages[0] + 0.75F*(backplanePercentages[1] - backplanePercentages[0]), backplanePercentages[3]);
		GL11.glVertex2d(3*screenDefaultX/4, 3*screenDefaultY/4);
		
		//Right backplane (right side)
		GL11.glTexCoord2f(backplanePercentages[1], backplanePercentages[2] + 0.5F*(backplanePercentages[3] - backplanePercentages[2]));
		GL11.glVertex2d(screenDefaultX, 5*screenDefaultY/8);
		GL11.glTexCoord2f(backplanePercentages[1], backplanePercentages[3]);
		GL11.glVertex2d(screenDefaultX, 3*screenDefaultY/4);		
		GL11.glEnd();
    	
		GL11.glBegin(GL11.GL_QUAD_STRIP);
		//Left moulding (left side)
		GL11.glTexCoord2f(mouldingPercentages[0], mouldingPercentages[2]);
		GL11.glVertex2d(0, 5*screenDefaultY/8 - 16);
		GL11.glTexCoord2f(mouldingPercentages[0], mouldingPercentages[3]);
		GL11.glVertex2d(0, 5*screenDefaultY/8);
		
		//Left moulding (right side)
		GL11.glTexCoord2f(mouldingPercentages[0] + 0.25F*(mouldingPercentages[1] - mouldingPercentages[0]), mouldingPercentages[2]);
		GL11.glVertex2d(screenDefaultX/4, screenDefaultY/2 - 16);
		GL11.glTexCoord2f(mouldingPercentages[0] + 0.25F*(mouldingPercentages[1] - mouldingPercentages[0]), mouldingPercentages[3]);
		GL11.glVertex2d(screenDefaultX/4, screenDefaultY/2);
		
		//Center moulding (right side)
		GL11.glTexCoord2f(mouldingPercentages[0] + 0.75F*(mouldingPercentages[1] - mouldingPercentages[0]), mouldingPercentages[2]);
		GL11.glVertex2d(3*screenDefaultX/4, screenDefaultY/2 - 16);
		GL11.glTexCoord2f(mouldingPercentages[0] + 0.75F*(mouldingPercentages[1] - mouldingPercentages[0]), mouldingPercentages[3]);
		GL11.glVertex2d(3*screenDefaultX/4, screenDefaultY/2);
		
		//Right moulding (right side)
		GL11.glTexCoord2f(mouldingPercentages[1], mouldingPercentages[2]);
		GL11.glVertex2d(screenDefaultX, 5*screenDefaultY/8 - 16);
		GL11.glTexCoord2f(mouldingPercentages[1], mouldingPercentages[3]);
		GL11.glVertex2d(screenDefaultX, 5*screenDefaultY/8);
		GL11.glEnd();
    }
    
	private static void drawLowerPanel(float[] backplanePercentages){
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(backplanePercentages[0], backplanePercentages[2]);
		GL11.glVertex2d(0, 3*screenDefaultY/4);
		GL11.glTexCoord2f(backplanePercentages[0], backplanePercentages[3]);
		GL11.glVertex2d(0, screenDefaultY);
		GL11.glTexCoord2f(backplanePercentages[1], backplanePercentages[3]);
		GL11.glVertex2d(screenDefaultX, screenDefaultY);
		GL11.glTexCoord2f(backplanePercentages[1], backplanePercentages[2]);
		GL11.glVertex2d(screenDefaultX, 3*screenDefaultY/4);
		GL11.glEnd();
    }

	private static void drawAuxiliaryPanel(float[] backplanePercentages, float[] mouldingPercentages){
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(backplanePercentages[0], backplanePercentages[2]);
		GL11.glVertex2d(0, screenDefaultY/2+16);
		GL11.glTexCoord2f(backplanePercentages[0], backplanePercentages[3]);
		GL11.glVertex2d(0, screenDefaultY);
		GL11.glTexCoord2f(backplanePercentages[1], backplanePercentages[3]);
		GL11.glVertex2d(screenDefaultX, screenDefaultY);
		GL11.glTexCoord2f(backplanePercentages[1], backplanePercentages[2]);
		GL11.glVertex2d(screenDefaultX, screenDefaultY/2+16);
		GL11.glEnd();
    	
		GL11.glBegin(GL11.GL_QUADS);
    	GL11.glTexCoord2f(mouldingPercentages[0], mouldingPercentages[2]);
    	GL11.glVertex2d(0, screenDefaultY/2);
		GL11.glTexCoord2f(mouldingPercentages[0], mouldingPercentages[3]);
		GL11.glVertex2d(0, screenDefaultY/2 + 16);
		GL11.glTexCoord2f(mouldingPercentages[1], mouldingPercentages[3]);
		GL11.glVertex2d(screenDefaultX, screenDefaultY/2 + 16);
		GL11.glTexCoord2f(mouldingPercentages[1], mouldingPercentages[2]);
		GL11.glVertex2d(screenDefaultX, screenDefaultY/2);
		GL11.glEnd();
    }
}
