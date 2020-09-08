package minecrafttransportsimulator.rendering.instances;

import org.lwjgl.opengl.GL11;

import mcinterface.InterfaceRender;
import minecrafttransportsimulator.items.packs.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONInstrument.Component;
import minecrafttransportsimulator.systems.VehicleAnimationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Main render class for instruments.  This class contains a main method that takes an instance of {@link ItemInstrument},
 * as well as the engine associated with that instrument and the vehicle the instrument is on.  This allows for an
 * instrument to be rendered a vehicle, GUI, or HUD.}.
 *
 * @author don_bruce
 */
public final class RenderInstrument{	
	/**
     * Renders the passed-in instrument using the vehicle's current state.  Note that this method does NOT take any 
     * vehicle JSON parameters into account as it does not know which instrument is being rendered.  This means that 
     * any transformations that need to be applied for translation or scaling should be applied prior to calling this
     * method.  Such transformations will, of course, differ between applications, so care should be taken to ensure
     * OpenGL states are not left out-of-whack after rendering is complete.
     */
	public static void drawInstrument(ItemInstrument instrument, byte partNumber, EntityVehicleF_Physics vehicle){
		//First bind the texture file for this insturment's pack.
		InterfaceRender.bindTexture(instrument.definition.packID, "textures/instruments.png");
		
		//Check if the lights are on.  If so, disable the lightmap.
		boolean lightsOn = vehicle.areInteriorLightsOn();
		
		//Finally, render the instrument based on the JSON instrument.definitions.
		for(byte i=0; i<instrument.definition.components.size(); ++i){
			Component section = instrument.definition.components.get(i);
			
			//Only render regular sections on pass 0 or -1, and overlays on pass 1 or -1.
			if((!section.lightOverlay && InterfaceRender.getRenderPass() != 1) || (section.lightOverlay && InterfaceRender.getRenderPass() != 0)){
				GL11.glPushMatrix();
				//Translate to the component, but slightly away from the instrument location to prevent clipping.
				GL11.glTranslatef(section.xCenter, section.yCenter, i*0.1F);
				
				//If the vehicle lights are on, disable the lightmap.
				if(lightsOn){
					InterfaceRender.setInternalLightingState(false);
				}
				
				//If the partNumber is non-zero, we need to check if we are applying a part-based animation.
				//If so, we need to let the animation system know by adding a suffix to the variable.
				//Otherwise, as we don't pass-in the part, it will assume it's a vehicle variable.
				//We also need to set the partNumber to 1 if we have a part number of 0 and we're
				//doing a part-specific animation.
				final boolean addRotationSuffix = section.rotationVariable != null && (section.rotationVariable.startsWith("engine_") || section.rotationVariable.startsWith("propeller_") || section.rotationVariable.startsWith("gun_"));
				final boolean addTranslationSuffix = section.translationVariable != null && (section.translationVariable.startsWith("engine_") || section.translationVariable.startsWith("propeller_") || section.translationVariable.startsWith("gun_"));
				if(partNumber == 0 && (addRotationSuffix || addTranslationSuffix)){
					partNumber = 1;
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
					double rotation = VehicleAnimationSystem.getVariableValue(addRotationSuffix ? section.rotationVariable + "_" + partNumber : section.rotationVariable, section.rotationFactor, section.rotationOffset, section.rotationClampMin, section.rotationClampMax, section.rotationAbsoluteValue, 0, vehicle, null);
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
					double translation = VehicleAnimationSystem.getVariableValue(addTranslationSuffix ? section.translationVariable + "_" + partNumber : section.translationVariable, section.translationFactor, 0, section.translationClampMin, section.translationClampMax, section.translationAbsoluteValue, 0, vehicle, null);
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
					double rotation = VehicleAnimationSystem.getVariableValue(addRotationSuffix ? section.rotationVariable + "_" + partNumber : section.rotationVariable, section.rotationFactor, section.rotationOffset, section.rotationClampMin, section.rotationClampMax, section.rotationAbsoluteValue, 0, vehicle, null);
					GL11.glRotated(rotation, 0, 0, 1);
				}
				
				//Now that all transforms are done, render the instrument shape.
				//If the shape is lit, do blending.  If not, disable blending.
				//This is required as it might already be enabled.
				if(!section.lightOverlay){
					GL11.glDisable(GL11.GL_BLEND);
					renderSquareUV(section.textureWidth, section.textureHeight, layerUStart/1024F, layerUEnd/1024F, layerVStart/1024F, layerVEnd/1024F);
				}else if(lightsOn){
					GL11.glEnable(GL11.GL_BLEND);
					GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
				    renderSquareUV(section.textureWidth, section.textureHeight, layerUStart/1024F, layerUEnd/1024F, layerVStart/1024F, layerVEnd/1024F);
				}
				GL11.glPopMatrix();
			}
			
			//Reset lightmap if we had previously disabled it.
			if(lightsOn){
				InterfaceRender.setInternalLightingState(true);
			}
			
			//Reset blend state.
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			if(InterfaceRender.getRenderPass() != 1){
				GL11.glDisable(GL11.GL_BLEND);
			}
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

