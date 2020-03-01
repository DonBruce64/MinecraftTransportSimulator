package minecrafttransportsimulator.rendering.vehicles;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.items.packs.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONInstrument.Component;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Plane;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import minecrafttransportsimulator.vehicles.parts.PartEngineCar;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/**Main render class for instruments.  This class contains a main method that takes an instance of {@link ItemInstrument},
 * as well as the engine associated with that instrument and the vehicle the instrument is on.  This allows for an
 * instrument to be rendered a vehicle, GUI, or HUD.}.
 *
 * @author don_bruce
 */
public final class RenderInstrument{	
	private static Map<String, ResourceLocation> instrumentTextureSheets = new HashMap<String, ResourceLocation>();
	
    /**
     * Renders the passed-in instrument using the vehicle's current state.  Note that this method does NOT take any 
     * vehicle JSON parameters into account as it does not know which instrument is being rendered.  This means that 
     * any transformations that need to be applied for translation or scaling should be applied prior to calling this
     * method.  Such transformations will, of course, differ between applications, so care should be taken to ensure
     * OpenGL states are not left out-of-whack after rendering is complete.
     */
	public static void drawInstrument(ItemInstrument instrument, byte engineNumber, EntityVehicleE_Powered vehicle){
		//First get the appropriate texture file for this instrument combination.
		if(!instrumentTextureSheets.containsKey(instrument.definition.packID)){
			instrumentTextureSheets.put(instrument.definition.packID, new ResourceLocation(instrument.definition.packID, "textures/instruments.png"));
		}
		Minecraft.getMinecraft().getTextureManager().bindTexture(instrumentTextureSheets.get(instrument.definition.packID));
		
		//Check if the lights are on.  If so, disable the lightmap.
		boolean lightsOn = RenderVehicle.isVehicleIlluminated(vehicle);
		
		//Subtract 1 from the current engine number (if greater than 0) to account for zero-indexed engine mappings.
		if(engineNumber > 0){
			--engineNumber;
		}
		
		//Finally, render the instrument based on the JSON definitions.
		for(byte i=0; i<instrument.definition.components.size(); ++i){
			Component section = instrument.definition.components.get(i);
			GL11.glPushMatrix();
			//Translate to the component, but slightly away from the instrument location to prevent clipping.
			GL11.glTranslatef(section.xCenter, section.yCenter, i*0.1F);
			
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
			//If the shape is lit, do blending.  If not, disable blending to save GPU work.
			if(!section.lightOverlay){
				GL11.glDisable(GL11.GL_BLEND);
				renderSquareUV(section.textureWidth, section.textureHeight, layerUStart/1024F, layerUEnd/1024F, layerVStart/1024F, layerVEnd/1024F);
			}else if(lightsOn){
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			    renderSquareUV(section.textureWidth, section.textureHeight, layerUStart/1024F, layerUEnd/1024F, layerVStart/1024F, layerVEnd/1024F);
			    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			}
			GL11.glPopMatrix();
		}
		
		//Reset lightmap if we had previously disabled it.
		if(lightsOn){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}
	}
	
	public static double getVariableValue(EntityVehicleE_Powered vehicle, String variable, byte engineNumber){
		switch(variable){
			case("yaw"): return -vehicle.rotationYaw;
			case("pitch"): return Math.max(Math.min(vehicle.rotationPitch, 25), -25);
			case("roll"): return vehicle.rotationRoll;
			case("altitude"): return vehicle.posY - (ConfigSystem.configObject.client.seaLvlOffset.value ? vehicle.world.provider.getAverageGroundLevel() : 0);
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
     * Renders a textured quad from the current bound texture of a specific width and height.
     * Used for rendering instrument textures off their texture sheets.
     */
	private static void renderSquareUV(float width, float height, float u, float U, float v, float V){
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(u, v);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3f(-width/2, -height/2, 0);
		GL11.glTexCoord2f(u, V);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3f(-width/2, height/2, 0);
		GL11.glTexCoord2f(U, V);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3f(width/2, height/2, 0);
		GL11.glTexCoord2f(U, v);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3f(width/2, -height/2, 0);
		GL11.glEnd();
	}
}

