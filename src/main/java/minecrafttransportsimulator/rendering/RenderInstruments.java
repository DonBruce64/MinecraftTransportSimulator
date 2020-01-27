package minecrafttransportsimulator.rendering;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.items.packs.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONInstrument.InstrumentComponent;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightTypes;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Plane;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import minecrafttransportsimulator.vehicles.parts.PartEngineCar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

public final class RenderInstruments{
	protected static final TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
	
	/**Map for texture sheets.  First keyed by vehicle, then keyed by the gauge itself.**/
	private static Map<String, Map<String, ResourceLocation>> instrumentTextureSheets = new HashMap<String, Map<String, ResourceLocation>>();
	
	public static void drawInstrument(EntityVehicleE_Powered vehicle, ItemInstrument instrument, boolean hud, byte engineNumber){
		//First get the appropriate texture file for this vehicle/instrument combination.
		if(!instrumentTextureSheets.containsKey(vehicle.definition.general.type)){
			instrumentTextureSheets.put(vehicle.definition.general.type, new HashMap<String, ResourceLocation>());
		}
		if(!instrumentTextureSheets.get(vehicle.definition.general.type).containsKey(instrument.definition.systemName)){			
			instrumentTextureSheets.get(vehicle.definition.general.type).put(instrument.definition.systemName, new ResourceLocation(instrument.definition.packID, "textures/instruments/" + vehicle.definition.general.type + ".png"));
		}
		textureManager.bindTexture(instrumentTextureSheets.get(vehicle.definition.general.type).get(instrument.definition.systemName));
		
		//Next get the appropriate starting sector for this instrument.
		//This is based on a 1024x1024 texture sheet divided into 8 - 128x128 sectors.
		float textureUStart = (instrument.definition.general.textureXSectorStart - 1)/8F;
		float textureVStart = (instrument.definition.general.textureYSectorStart - 1)/8F;
		
		//If we are in the HUD, invert the rendering to get correct orientation.
		if(hud){
			GL11.glScalef(-1, -1, 0);
		}
		
		//Check if the lights are on.  If so, disable the lightmap.
		boolean lightsOn = lightsOn(vehicle);
		
		//Subtract 1 from the current engine number (if greater than 0) to account for zero-indexed engine mappings.
		if(engineNumber > 0){
			--engineNumber;
		}
		
		//Finally, render the instrument based on the JSON definitions.
		byte currentLayer = 0;
		for(InstrumentComponent component : instrument.definition.components){
			GL11.glPushMatrix();
			//Translate slightly away from the instrument location to prevent clipping.
			GL11.glTranslatef(0, 0, -currentLayer*0.1F);
			
			//If the vehicle lights are on, disable the lightmap.
			if(lightsOn){
				Minecraft.getMinecraft().entityRenderer.disableLightmap();
			}else{
				Minecraft.getMinecraft().entityRenderer.enableLightmap();
			}
			
			//Set the UV location for the render.
			float layerHeight = 128;
			float layerUStart = textureUStart;
			float layerUEnd = textureUStart + 0.125F;
			float layerVStart = textureVStart + currentLayer/8F;
			float layerVEnd = textureVStart + (1 + currentLayer)/8F;
			
			//If we use a rotation variable, rotate now.
			//Otherwise, just render normally.
			if(component.rotationVariable != null && !component.rotationVariable.isEmpty()){
				double rotation = component.rotationOffset + getVariableValue(vehicle, component.rotationVariable, engineNumber)*component.rotationFactor;
				GL11.glTranslatef(-component.xRotationPositionOffset, component.yRotationPositionOffset, 0);
				GL11.glRotated(rotation, 0, 0, 1);
				GL11.glTranslatef(component.xRotationPositionOffset, -component.yRotationPositionOffset, 0);
			}
			
			//If we have a visibility variable, adjust the UV mapping.
			if(component.visibilityVariable != null && !component.visibilityVariable.isEmpty()){
				double height = getVariableValue(vehicle, component.visibilityVariable, engineNumber)*component.visibilityFactor;
				//If the height is locked, move the UV map to the variable.
				//If not, increase the UV map height with the variable.
				if(!component.dynamicVisibility){
					layerHeight = component.visibleSectionHeight;
					layerVStart += (float) ((64F + component.visibilityOffset + height - component.visibleSectionHeight/2F)/1024F);
					layerVEnd = layerVStart + component.visibleSectionHeight/1024F;
				}else{
					layerHeight = (float) (component.visibilityOffset + height);
					layerVStart += (component.visibilityOffset)/1024F;
					layerVEnd = (float) (layerVStart + layerHeight/1024F);
					GL11.glTranslatef(0, layerHeight/2F, 0);
				}
			}
			
			//Finally, render the instrument shape.
			if(!component.lightOverlay){
				renderSquareUV(128, layerHeight, 0, layerUStart, layerUEnd, layerVStart, layerVEnd);
			}else if(lightsOn){
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			    renderSquareUV(128, layerHeight, 0, layerUStart, layerUEnd, layerVStart, layerVEnd);
			}
			++currentLayer;
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
			case("rpm"): return vehicle.getEngineByNumber(engineNumber) != null ? (vehicle.getEngineByNumber(engineNumber).definition.engine.maxRPM < 15000 ? vehicle.getEngineByNumber(engineNumber).RPM : vehicle.getEngineByNumber(engineNumber).RPM/10D) : 0;
			case("rpm_max"): return vehicle.getEngineByNumber(engineNumber) != null ? (vehicle.getEngineByNumber(engineNumber).definition.engine.maxRPM < 15000 ? APartEngine.getSafeRPMFromMax(vehicle.getEngineByNumber(engineNumber).definition.engine.maxRPM) : APartEngine.getSafeRPMFromMax(vehicle.getEngineByNumber(engineNumber).definition.engine.maxRPM)/10D) : 0;
			case("fuel_flow"): return vehicle.getEngineByNumber(engineNumber) != null ? vehicle.getEngineByNumber(engineNumber).fuelFlow*20F*60F/1000F : 0;
			case("temp"): return vehicle.getEngineByNumber(engineNumber) != null ? vehicle.getEngineByNumber(engineNumber).temp : 0;
			case("oil"): return vehicle.getEngineByNumber(engineNumber) != null ? vehicle.getEngineByNumber(engineNumber).oilPressure : 0;
			case("gear"): return vehicle.getEngineByNumber(engineNumber) != null ? ((PartEngineCar) vehicle.getEngineByNumber(engineNumber)).currentGear : 0;
			default: return 0;
		}
	}
	
    /**
     * Checks if lights are on for this vehicle and instruments need to be lit up.
     */
	public static boolean lightsOn(EntityVehicleE_Powered vehicle){
		return (vehicle.isLightOn(LightTypes.NAVIGATIONLIGHT) || vehicle.isLightOn(LightTypes.RUNNINGLIGHT)) && vehicle.electricPower > 3;
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

