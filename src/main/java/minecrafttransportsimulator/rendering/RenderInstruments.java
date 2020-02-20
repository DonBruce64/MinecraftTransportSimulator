package minecrafttransportsimulator.rendering;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.items.packs.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONInstrument.Component;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Plane;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import minecrafttransportsimulator.vehicles.parts.PartEngineCar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

public final class RenderInstruments{
	protected static final TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
	
	/**Map for texture sheets.  Keyed by packID.**/
	private static Map<String, ResourceLocation> instrumentTextureSheets = new HashMap<String, ResourceLocation>();
	
	public static void drawInstrument(EntityVehicleE_Powered vehicle, ItemInstrument instrument, boolean hud, byte engineNumber){
		//First get the appropriate texture file for this instrument combination.
		if(!instrumentTextureSheets.containsKey(instrument.definition.packID)){
			instrumentTextureSheets.put(instrument.definition.packID, new ResourceLocation(instrument.definition.packID, "textures/instruments.png"));
		}
		textureManager.bindTexture(instrumentTextureSheets.get(instrument.definition.packID));
		
		//If we are in the HUD, invert the rendering to get correct orientation.
		if(hud){
			GL11.glScalef(-1, -1, 0);
		}
		
		//Check if the lights are on.  If so, disable the lightmap.
		boolean lightsOn = isPanelIlluminated(vehicle);
		
		//Subtract 1 from the current engine number (if greater than 0) to account for zero-indexed engine mappings.
		if(engineNumber > 0){
			--engineNumber;
		}
		
		//Finally, render the instrument based on the JSON definitions.
		for(byte i=0; i<instrument.definition.components.size(); ++i){
			Component section = instrument.definition.components.get(i);
			GL11.glPushMatrix();
			//Translate to the component, but slightly away from the instrument location to prevent clipping.
			GL11.glTranslatef(section.xCenter, section.yCenter, -i*0.1F);
			
			//If the vehicle lights are on, disable the lightmap.
			if(lightsOn){
				Minecraft.getMinecraft().entityRenderer.disableLightmap();
			}else{
				Minecraft.getMinecraft().entityRenderer.enableLightmap();
			}
			
			//Init variables.
			float layerUStart;
			float layerUEnd;
			float layerVStart;
			float layerVEnd;
			
			//Depending on what variables are set we do different rendering operations.
			//If we are rotating the window, but not the texture we should initialize the texture points to that rotated point.
			//Otherwise, set the points to their normal location.
			if(section.rotationVariable != null && section.rotateWindow){
				double rotation = section.rotationOffset + getVariableValue(vehicle, section.rotationVariable, engineNumber)*section.rotationFactor;
				double sin = Math.sin(Math.toRadians(rotation));
				double cos = Math.sin(Math.toRadians(rotation));
				layerUStart = (float) ((-section.textureWidth/2F)*cos - (-section.textureHeight/2F)*sin);
				layerVStart = (float) ((-section.textureWidth/2F)*sin + (-section.textureHeight/2F)*cos);
				layerUEnd = (float) ((section.textureWidth/2F)*cos - (section.textureHeight/2F)*sin);
				layerVEnd = (float) ((section.textureWidth/2F)*sin + (section.textureHeight/2F)*cos);
			}else{
				layerUStart = section.textureXCenter - section.textureWidth/2F;
				layerUEnd = layerUStart + section.textureWidth;
				layerVStart = section.textureYCenter - section.textureHeight/2F;
				layerVEnd = layerVStart + section.textureHeight;
			}
			
			//If we are translating, offset the coords based on the translated amount.
			//Adjust the window to either move or scale depending on settings.
			if(section.translationVariable != null){
				float translation = (float) (getVariableValue(vehicle, section.translationVariable, engineNumber)*section.translationFactor);
				if(section.extendWindow){
					//We need to add to the edge of the window in this case rather than move the entire window.
					if(section.translateHorizontal){
						layerUEnd += translation;
					}else{
						layerVEnd += translation;
					}
				}else{
					//Translate the window to the appropriate section of the texture sheet.
					if(section.translateHorizontal){
						layerUStart += translation;
						layerUEnd = layerUStart + section.textureWidth;
					}else{
						layerVStart += translation;
						layerVEnd = layerVStart + section.textureHeight;
					}
				}
			}
			
			//If we are rotating the texture, and not the window, apply the rotation here after the translation.
			if(section.rotationVariable != null && !section.rotateWindow){
				float rotation = (float) (section.rotationOffset + getVariableValue(vehicle, section.rotationVariable, engineNumber)*section.rotationFactor);
				GL11.glRotatef(rotation, 0, 0, 1);
			}
			
			//Now that all transforms are done, render the instrument shape.
			if(!section.lightOverlay){
				renderSquareUV(section.textureWidth, section.textureHeight, 0, layerUStart/1024F, layerUEnd/1024F, layerVStart/1024F, layerVEnd/1024F);
			}else if(lightsOn){
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			    renderSquareUV(section.textureWidth, section.textureHeight, 0, layerUStart/1024F, layerUEnd/1024F, layerVStart/1024F, layerVEnd/1024F);
			}
			GL11.glPopMatrix();
		}
		
		//Reset blend functions changed in light operations.
		if(lightsOn){
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}
	}
	
	private static double getVariableValue(EntityVehicleE_Powered vehicle, String variable, byte engineNumber){
		switch(variable){
			case("yaw"): return -vehicle.rotationYaw;
			case("pitch"): return Math.max(Math.min(vehicle.rotationPitch, 25), -25);
			case("roll"): return vehicle.rotationRoll;
			case("altitude"): return vehicle.posY - (ConfigSystem.configObject.client.seaLevelOffset.value ? vehicle.world.provider.getAverageGroundLevel() : 0);
			case("speed"): return Math.abs(vehicle.velocity*vehicle.speedFactor*20);
			case("turn_coordinator"): return Math.max(Math.min(((vehicle.rotationRoll - vehicle.prevRotationRoll)/10 + vehicle.rotationYaw - vehicle.prevRotationYaw)/0.15F*25F, 50), -50);
			case("turn_indicator"): return Math.max(Math.min((vehicle.rotationYaw - vehicle.prevRotationYaw)/0.15F*25F, 50), -50);
			case("slip"): return 75*((EntityVehicleF_Air) vehicle).sideVec.dotProduct(vehicle.velocityVec);
			case("vertical_speed"): return vehicle.motionY*20;
			case("lift_reserve"): return Math.max(Math.min(((EntityVehicleF_Air) vehicle).trackAngle*3 + 20, 35), -35);
			case("trim_rudder"): return ((EntityVehicleF_Air) vehicle).rudderTrim/10F;
			case("trim_elevator"): return ((EntityVehicleF_Air) vehicle).elevatorTrim/10F;
			case("trim_aileron"): return ((EntityVehicleF_Air) vehicle).aileronTrim/10F;
			case("flaps_setpoint"): return ((EntityVehicleG_Plane) vehicle).flapDesiredAngle/10F;
			case("flaps_actual"): return ((EntityVehicleG_Plane) vehicle).flapCurrentAngle/10F;
			case("electric_power"): return vehicle.electricPower;
			case("electric_usage"): return Math.min(vehicle.electricFlow*20, 1);
			case("fuel"): return vehicle.fuel/vehicle.definition.motorized.fuelCapacity*100F;
			case("rpm"): return vehicle.engines.containsKey(engineNumber) ? (vehicle.engines.get(engineNumber).definition.engine.maxRPM < 15000 ? vehicle.engines.get(engineNumber).RPM : vehicle.engines.get(engineNumber).RPM/10D) : 0;
			case("rpm_max"): return vehicle.engines.containsKey(engineNumber) ? (vehicle.engines.get(engineNumber).definition.engine.maxRPM < 15000 ? APartEngine.getSafeRPMFromMax(vehicle.engines.get(engineNumber).definition.engine.maxRPM) : APartEngine.getSafeRPMFromMax(vehicle.engines.get(engineNumber).definition.engine.maxRPM)/10D) : 0;
			case("fuel_flow"): return vehicle.engines.containsKey(engineNumber) ? vehicle.engines.get(engineNumber).fuelFlow*20F*60F/1000F : 0;
			case("temp"): return vehicle.engines.containsKey(engineNumber) ? vehicle.engines.get(engineNumber).temp : 0;
			case("oil"): return vehicle.engines.containsKey(engineNumber) ? vehicle.engines.get(engineNumber).oilPressure : 0;
			case("gear"): return vehicle.engines.containsKey(engineNumber) ? ((PartEngineCar) vehicle.engines.get(engineNumber)).currentGear : 0;
			default: return 0;
		}
	}
	
    /**
     * Checks if lights are on for this vehicle and instruments need to be lit up.
     */
	public static boolean isPanelIlluminated(EntityVehicleE_Powered vehicle){
		return (vehicle.isLightOn(LightType.NAVIGATIONLIGHT) || vehicle.isLightOn(LightType.RUNNINGLIGHT)) && vehicle.electricPower > 3;
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
     * Draws a scaled string with the bottom-left at x, y.
     */
	protected static void drawScaledString(String string, int x, int y, float scale){
    	GL11.glPushMatrix();
    	GL11.glScalef(scale, scale, scale);
    	Minecraft.getMinecraft().fontRenderer.drawString(string, x, y, Color.WHITE.getRGB());
    	GL11.glPopMatrix();
    }
}

