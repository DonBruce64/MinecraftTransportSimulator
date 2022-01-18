package minecrafttransportsimulator.rendering.instances;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3dPlus;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONInstrument.JSONInstrumentComponent;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;
import minecrafttransportsimulator.rendering.components.RenderableObject;
import minecrafttransportsimulator.systems.ConfigSystem;

/**Main render class for instruments.  This class contains a main method that takes an instance of {@link ItemInstrument},
 * as well as the engine associated with that instrument and the vehicle the instrument is on.  This allows for an
 * instrument to be rendered a vehicle, GUI, or HUD.}.
 *
 * @author don_bruce
 */
public final class RenderInstrument{
	private static float globalScale = 0;
	private static int partNumber = 0;
	private static final Point3dPlus bottomLeft = new Point3dPlus();
	private static final Point3dPlus topLeft = new Point3dPlus();
	private static final Point3dPlus topRight = new Point3dPlus();
	private static final Point3dPlus bottomRight = new Point3dPlus();
	private static final Point3dPlus helperRotation = new Point3dPlus();
	private static final float[][] instrumentSingleComponentPoints = new float[6][8];
	private static final RenderableObject renderObject = new RenderableObject("instrument", null, new ColorRGB(), FloatBuffer.allocate(6*8), false);
	
	/**
     * Renders the passed-in instrument using the entity's current state.  Note that this method does NOT take any 
     * entity JSON parameters into account as it does not know which instrument is being rendered.  This means that 
     * any transformations that need to be applied for translation should be applied prior to calling this method.
     * Also note that the parameters in the JSON here are in png-texture space, so y is inverted.  Hence the various
     * negations in translation transforms.
     */
	public static void drawInstrument(ItemInstrument instrument, int partNumberIn, AEntityE_Interactable<?> entity, float scale, boolean blendingEnabled, float partialTicks){
		//Check if the lights are on.  If so, render the overlays and the text lit if requested.
		boolean lightsOn = entity.renderTextLit();
		
		//Get scale of the instrument, before component scaling.
		globalScale = entity.scale*scale;
		
		//Set the part number for switchbox reference.
		partNumber = partNumberIn;
		
		//Finally, render the instrument based on the JSON instrument.definitions.
		//We cache up all the draw calls for this blend pass, and then render them all at once.
		//This is more efficient than rendering each one individually.
		for(int i=0; i<instrument.definition.components.size(); ++i){
			JSONInstrumentComponent component = instrument.definition.components.get(i);
			if(component.overlayTexture ? blendingEnabled : !blendingEnabled){
				//If we have text, do a text render.  Otherwise, do a normal instrument render.
				if(component.textObject != null){
					//Also translate slightly away from the instrument location to prevent clipping.
					GL11.glPushMatrix();
					GL11.glTranslatef(0.0F, 0.0F, i*0.0001F);
					int variablePartNumber = AEntityD_Definable.getVariableNumber(component.textObject.variableName);
					final boolean addSuffix = variablePartNumber == -1 && ((component.textObject.variableName.startsWith("engine_") || component.textObject.variableName.startsWith("propeller_") || component.textObject.variableName.startsWith("gun_") || component.textObject.variableName.startsWith("seat_")));
					if(addSuffix){
						String oldName = component.textObject.variableName; 
						component.textObject.variableName += "_" + partNumber;
						RenderText.draw3DText(entity.getAnimatedTextVariableValue(component.textObject, partialTicks), entity, component.textObject, globalScale*component.scale, true);
						component.textObject.variableName = oldName;
					}else{
						RenderText.draw3DText(entity.getAnimatedTextVariableValue(component.textObject, partialTicks), entity, component.textObject, globalScale*component.scale, true);
					}
					GL11.glPopMatrix();
				}else{
					//Init variables.
					renderObject.texture = "/assets/" + instrument.definition.packID + "/textures/" + instrument.definition.textureName;
					renderObject.transform.resetTransforms();
					renderObject.transform.translate(0.0, 0.0, i*0.0001);
					renderObject.transform.scale(globalScale);
					bottomLeft.set(-component.textureWidth/2D, component.textureHeight/2D, 0);
					topLeft.set(-component.textureWidth/2D, -component.textureHeight/2D, 0);
					topRight.set(component.textureWidth/2D, -component.textureHeight/2D, 0);
					bottomRight.set(component.textureWidth/2D, component.textureHeight/2D, 0);
					
					//Render if we don't have transforms, or of those transforms said we were good.
					InstrumentSwitchbox switchbox = entity.instrumentComponentSwitchboxes.get(component);
					if(switchbox == null || switchbox.runSwitchbox(partialTicks)){
						//Add the instrument UV-map offsets.
						//These don't get added to the initial points to allow for rotation.
						bottomLeft.add(component.textureXCenter, component.textureYCenter, 0);
						topLeft.add(component.textureXCenter, component.textureYCenter, 0);
						topRight.add(component.textureXCenter, component.textureYCenter, 0);
						bottomRight.add(component.textureXCenter, component.textureYCenter, 0);
						
						//Divide the Points by 1024.  This converts the points from pixels to the 0-1 UV values.
						bottomLeft.multiply(1D/1024D);
						topLeft.multiply(1D/1024D);
						topRight.multiply(1D/1024D);
						bottomRight.multiply(1D/1024D);
						
						//Translate to the component.
						renderObject.transform.translate(component.xCenter, -component.yCenter, 0);
						
						//Set points to the variables here and render them.
						//If the shape is lit, disable lighting for blending.
						renderObject.disableLighting = component.lightUpTexture && lightsOn && ConfigSystem.configObject.clientRendering.brightLights.value;
						renderSquareUV(component);
					}
				}
			}
		}
	}
	
	/**
	 *  Custom instrument switchbox class.
	 */
	public static class InstrumentSwitchbox extends AnimationSwitchbox{
		private final JSONInstrumentComponent component;

		public InstrumentSwitchbox(AEntityD_Definable<?> entity, JSONInstrumentComponent component){
			super(entity, component.animations);
			this.component = component;
		}
		
		private String convertAnimationPartNumber(DurationDelayClock clock){
			//If the partNumber is non-zero, we need to check if we are applying a part-based animation.
			//If so, we need to let the animation system know by adding a suffix to the variable.
			//Otherwise, as we don't pass-in the part, it will assume it's an entity variable.
			//We also need to set the partNumber to 1 if we have a part number of 0 and we're
			//doing a part-specific animation.
			//Skip adding a suffix if one already exists.
			int variablePartNumber = AEntityD_Definable.getVariableNumber(clock.animation.variable);
			final boolean addSuffix = variablePartNumber == -1 && !(entity instanceof APart) && (clock.animation.variable.startsWith("engine_") || clock.animation.variable.startsWith("propeller_") || clock.animation.variable.startsWith("gun_") || clock.animation.variable.startsWith("seat_"));
			if(partNumber == 0 && addSuffix){
				partNumber = 1;
			}
			String oldVariable = clock.animation.variable;
			if(addSuffix){
				clock.animation.variable += "_" + partNumber;
			}
			return oldVariable;
		}
		
		@Override
		public void runTranslation(DurationDelayClock clock, float partialTicks){
			//Offset the coords based on the translated amount.
			//Adjust the window to either move or scale depending on settings.
			String oldVariable = convertAnimationPartNumber(clock);
			double xTranslation = entity.getAnimatedVariableValue(clock, clock.animation.axis.x, partialTicks);
			double yTranslation = entity.getAnimatedVariableValue(clock, clock.animation.axis.y, partialTicks);
			clock.animation.variable = oldVariable;
			
			if(component.extendWindow){
				//We need to add to the edge of the window in this case rather than move the entire window.
				if(clock.animation.axis.x < 0){
					bottomLeft.x += xTranslation;
					topLeft.x += xTranslation;
				}else if(clock.animation.axis.x > 0){
					topRight.x += xTranslation;
					bottomRight.x += xTranslation;
				}
				if(clock.animation.axis.y < 0){
					bottomLeft.y += yTranslation;
					bottomRight.y += yTranslation;
				}else if(clock.animation.axis.y > 0){
					topLeft.y += yTranslation;
					topRight.y += yTranslation;
				}
			}else if(component.moveComponent){
				//Translate the rather than adjust the window coords.
				renderObject.transform.translate(xTranslation, yTranslation, 0);
			}else{
				//Offset the window coords to the appropriate section of the texture sheet.
				//We don't want to do an OpenGL translation here as that would move the texture's
				//rendered position on the instrument rather than change what texture is rendered.
				if(clock.animation.axis.x != 0){
					bottomLeft.x += xTranslation;
					topLeft.x += xTranslation;
					topRight.x += xTranslation;
					bottomRight.x += xTranslation;
				}
				if(clock.animation.axis.y != 0){
					bottomLeft.y += yTranslation;
					topLeft.y += yTranslation;
					topRight.y += yTranslation;
					bottomRight.y += yTranslation;
				}
			}
		}
		
		@Override
		public void runRotation(DurationDelayClock clock, float partialTicks){
			String oldVariable = convertAnimationPartNumber(clock);
			double variableValue = -entity.getAnimatedVariableValue(clock, clock.animation.axis.z, partialTicks);
			clock.animation.variable = oldVariable;
			
			//Depending on what variables are set we do different rendering operations.
			//If we are rotating the window, but not the texture we should offset the texture points to that rotated point.
			//Otherwise, we apply an OpenGL rotation operation.
			if(component.rotateWindow){
				//Add rotation offset to the points.
				bottomLeft.add(clock.animation.centerPoint);
				topLeft.add(clock.animation.centerPoint);
				topRight.add(clock.animation.centerPoint);
				bottomRight.add(clock.animation.centerPoint);
				
				//Rotate the points by the rotation.
				helperRotation.set(0, 0, variableValue);
				bottomLeft.rotateFine(helperRotation);
				topLeft.rotateFine(helperRotation);
				topRight.rotateFine(helperRotation);
				bottomRight.rotateFine(helperRotation);
				
				//Remove the rotation offsets.
				bottomLeft.subtract(clock.animation.centerPoint);
				topLeft.subtract(clock.animation.centerPoint);
				topRight.subtract(clock.animation.centerPoint);
				bottomRight.subtract(clock.animation.centerPoint);
			}else{
				renderObject.transform.translate((component.xCenter + clock.animation.centerPoint.x), -(component.yCenter + clock.animation.centerPoint.y), 0.0);
				renderObject.transform.rotate(variableValue, 0, 0, 1);
				renderObject.transform.translate(-(component.xCenter + clock.animation.centerPoint.x), (component.yCenter + clock.animation.centerPoint.y), 0.0);
			}
		}
	}
	
    /**
     * Helper method for setting points for rendering.
     */
	private static void renderSquareUV(JSONInstrumentComponent component){
		//Set X, Y, U, V, and normal Z.  All other values are 0.
		//Also invert V, as we're going off of pixel-coords here.
		for(int i=0; i<instrumentSingleComponentPoints.length; ++i){
			float[] vertex = instrumentSingleComponentPoints[i];
			switch(i){
				case(0):{//Bottom-right
					vertex[5] = component.textureWidth/2;
					vertex[6] = -component.textureHeight/2;
					vertex[3] = (float) bottomRight.x;
					vertex[4] = (float) bottomRight.y;
					break;
				}
				case(1):{//Top-right
					vertex[5] = component.textureWidth/2;
					vertex[6] = component.textureHeight/2;
					vertex[3] = (float) topRight.x;
					vertex[4] = (float) topRight.y;
					break;
				}
				case(2):{//Top-left
					vertex[5] = -component.textureWidth/2;
					vertex[6] = component.textureHeight/2;
					vertex[3] = (float) topLeft.x;
					vertex[4] = (float) topLeft.y;
					break;
				}
				case(3):{//Bottom-right
					vertex[5] = component.textureWidth/2;
					vertex[6] = -component.textureHeight/2;
					vertex[3] = (float) bottomRight.x;
					vertex[4] = (float) bottomRight.y;
					break;
				}
				case(4):{//Top-left
					vertex[5] = -component.textureWidth/2;
					vertex[6] = component.textureHeight/2;
					vertex[3] = (float) topLeft.x;
					vertex[4] = (float) topLeft.y;
					break;
				}
				case(5):{//Bottom-left
					vertex[5] = -component.textureWidth/2;
					vertex[6] = -component.textureHeight/2;
					vertex[3] = (float) bottomLeft.x;
					vertex[4] = (float) bottomLeft.y;						
					break;
				}
			}
			vertex[2] = 1.0F;
			renderObject.vertices.put(vertex);
		}
		renderObject.vertices.flip();
		renderObject.transform.scale(component.scale);
		renderObject.render();
	}
}