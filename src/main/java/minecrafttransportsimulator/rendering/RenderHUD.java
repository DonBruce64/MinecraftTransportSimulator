package minecrafttransportsimulator.rendering;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.jsondefs.JSONVehicle.PackInstrument;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
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
	
	public static void drawMainHUD(EntityVehicleE_Powered vehicle, boolean inGUI){		
		GL11.glPushMatrix();
		GL11.glColor4f(1, 1, 1, 1);
		Minecraft.getMinecraft().getTextureManager().bindTexture(RenderVehicle.getTextureForVehicle(vehicle));
		
		if(!inGUI){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}

		if(CameraSystem.hudMode == 3 || inGUI){
			drawLowerPanel(vehicle.definition.rendering.hudBackplaneTexturePercentages);
		}else{
			GL11.glTranslatef(0, screenDefaultY/4, 0);
		}
		if(CameraSystem.hudMode == 2 || CameraSystem.hudMode == 3 || inGUI){
			drawUpperPanel(vehicle.definition.rendering.hudBackplaneTexturePercentages, vehicle.definition.rendering.hudMouldingTexturePercentages);
		}
		if(CameraSystem.hudMode > 0 || inGUI){
			drawInstruments(vehicle, 
					CameraSystem.hudMode == 1 && !inGUI ? 25 : 0,
					CameraSystem.hudMode == 1 && !inGUI  ? 75 : 100,
					CameraSystem.hudMode != 3 && !inGUI ? 75 : 100,
					true);
		}

		if(!inGUI){
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}
		GL11.glPopMatrix();
	}
	
	public static void drawAuxiliaryHUD(EntityVehicleE_Powered vehicle, int width, int height, boolean inGUI){		
		GL11.glPushMatrix();
		GL11.glColor4f(1, 1, 1, 1);
		Minecraft.getMinecraft().getTextureManager().bindTexture(RenderVehicle.getTextureForVehicle(vehicle));
		if(!inGUI){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}
		drawAuxiliaryPanel(vehicle.definition.rendering.hudBackplaneTexturePercentages, vehicle.definition.rendering.hudMouldingTexturePercentages);
		drawInstruments(vehicle, 0, 100, 100, false);
		if(!inGUI){
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}
		GL11.glPopMatrix();
	}
	
	private static void drawInstruments(EntityVehicleE_Powered vehicle, int minX, int maxX, int maxY, boolean main){
		for(byte i=0; i<vehicle.definition.motorized.instruments.size(); ++i){
			PackInstrument packInstrument = vehicle.definition.motorized.instruments.get(i);
			//Render the instruments in the correct panel.
			if((packInstrument.optionalEngineNumber == 0 && main) || (packInstrument.optionalEngineNumber != 0 && !main)){
				if(packInstrument.hudpos != null){
					if(packInstrument.hudpos[0] >= minX && packInstrument.hudpos[0] <= maxX && packInstrument.hudpos[1] <= maxY){
						GL11.glPushMatrix();
						GL11.glTranslated(packInstrument.hudpos[0]*screenDefaultX/100D, packInstrument.hudpos[1]*screenDefaultY/100D, 0);
						GL11.glScalef(packInstrument.hudScale, packInstrument.hudScale, packInstrument.hudScale);
						if(vehicle.instruments.containsKey(i)){
							RenderInstruments.drawInstrument(vehicle, vehicle.instruments.get(i), true, packInstrument.optionalEngineNumber);
						}
						GL11.glPopMatrix();
					}
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
