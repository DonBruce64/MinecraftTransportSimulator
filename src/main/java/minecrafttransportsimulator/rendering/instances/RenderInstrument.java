package minecrafttransportsimulator.rendering.instances;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONInstrument.Component;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;
import minecrafttransportsimulator.systems.ConfigSystem;

/**Main render class for instruments.  This class contains a main method that takes an instance of {@link ItemInstrument},
 * as well as the engine associated with that instrument and the vehicle the instrument is on.  This allows for an
 * instrument to be rendered a vehicle, GUI, or HUD.}.
 *
 * @author don_bruce
 */
public final class RenderInstrument{
	private static final Point3d bottomLeft = new Point3d();
	private static final Point3d topLeft = new Point3d();
	private static final Point3d topRight = new Point3d();
	private static final Point3d bottomRight = new Point3d();
	private static final Point3d rotation = new Point3d();
	private static final float[][] points = new float[6][8];
	
	/**
     * Renders the passed-in instrument using the entity's current state.  Note that this method does NOT take any 
     * entity JSON parameters into account as it does not know which instrument is being rendered.  This means that 
     * any transformations that need to be applied for translation or scaling should be applied prior to calling this
     * method.  Such transformations will, of course, differ between applications, so care should be taken to ensure
     * OpenGL states are not left out-of-whack after rendering is complete.
     */
	public static void drawInstrument(ItemInstrument instrument, int partNumber, AEntityD_Interactable<?> entity, float scale, boolean blendingEnabled, float partialTicks){
		//First bind the texture file for this insturment's pack.
		InterfaceRender.setTexture("/assets/" + instrument.definition.packID + "/textures/instruments.png");
		
		//Check if the lights are on.  If so, render the overlays.
		boolean lightsOn = entity.renderTextLit();
		
		//Finally, render the instrument based on the JSON instrument.definitions.
		for(byte i=0; i<instrument.definition.components.size(); ++i){
			Component component = instrument.definition.components.get(i);
			if(component.overlayTexture ? blendingEnabled : !blendingEnabled){
				//If we have text, do a text render.  Otherwise, do a normal instrument render.
				//Also translate slightly away from the instrument location to prevent clipping.
				float componentScale = component.scale != 0 ? component.scale*scale : scale;
				GL11.glPushMatrix();
				GL11.glTranslatef(0.0F, 0.0F, i*0.0001F);
				GL11.glScalef(componentScale, componentScale, componentScale);
				if(component.textObject != null){
					int variablePartNumber = AEntityC_Definable.getVariableNumber(component.textObject.fieldName);
					final boolean addSuffix = variablePartNumber == -1 && ((component.textObject.fieldName.startsWith("engine_") || component.textObject.fieldName.startsWith("propeller_") || component.textObject.fieldName.startsWith("gun_") || component.textObject.fieldName.startsWith("seat_")));
					double textNumeric = entity.getRawVariableValue(addSuffix ? component.textObject.fieldName + "_" + partNumber : component.textObject.fieldName, 0)*component.textFactor;;
					if(Double.isNaN(textNumeric)){
						textNumeric = 0;
					}
					
					String text = String.format("%0" + component.textObject.maxLength + "d", (int) textNumeric);
					if(component.lightUpTexture && lightsOn && ConfigSystem.configObject.clientRendering.brightLights.value){
						InterfaceRender.setLightingState(false);
						RenderText.draw3DText(text, entity, component.textObject, componentScale, true);
						InterfaceRender.setLightingState(true);
					}else{
						RenderText.draw3DText(text, entity, component.textObject, componentScale, true);
					}
				}else{
					//Init variables.
					bottomLeft.set(-component.textureWidth/2D, -component.textureHeight/2D, 0);
					topLeft.set(-component.textureWidth/2D, component.textureHeight/2D, 0);
					topRight.set(component.textureWidth/2D, component.textureHeight/2D, 0);
					bottomRight.set(component.textureWidth/2D, -component.textureHeight/2D, 0);
					boolean skipRender = false;
					boolean skipFurtherTransforms = false;
					if(component.animations != null){
						for(JSONAnimationDefinition animation : component.animations){
							//If the partNumber is non-zero, we need to check if we are applying a part-based animation.
							//If so, we need to let the animation system know by adding a suffix to the variable.
							//Otherwise, as we don't pass-in the part, it will assume it's an entity variable.
							//We also need to set the partNumber to 1 if we have a part number of 0 and we're
							//doing a part-specific animation.
							//Skip adding a suffix if one already exists.
							int variablePartNumber = AEntityC_Definable.getVariableNumber(animation.variable);
							final boolean addSuffix = variablePartNumber == -1 && !(entity instanceof APart) && (animation.variable.startsWith("engine_") || animation.variable.startsWith("propeller_") || animation.variable.startsWith("gun_") || animation.variable.startsWith("seat_"));
							if(partNumber == 0 && addSuffix){
								partNumber = 1;
							}
							if(addSuffix){
								animation.variable += "_" + partNumber;
							}
							
							DurationDelayClock animationClock = entity.animationClocks.get(animation);
							if(animationClock == null){
								animationClock = new DurationDelayClock(animation);
								entity.animationClocks.put(animation, animationClock);
							}
							
							switch(animation.animationType){
								case ROTATION :{
									double variableValue = -entity.getAnimatedVariableValue(animationClock, animation.axis.z, partialTicks);
									//Depending on what variables are set we do different rendering operations.
									//If we are rotating the window, but not the texture we should offset the texture points to that rotated point.
									//Otherwise, we apply an OpenGL rotation operation.
									if(component.rotateWindow){
										//Add rotation offset to the points.
										bottomLeft.add(animation.centerPoint);
										topLeft.add(animation.centerPoint);
										topRight.add(animation.centerPoint);
										bottomRight.add(animation.centerPoint);
										
										//Rotate the points by the rotation.
										rotation.set(0, 0, variableValue);
										bottomLeft.rotateFine(rotation);
										topLeft.rotateFine(rotation);
										topRight.rotateFine(rotation);
										bottomRight.rotateFine(rotation);
										
										//Remove the rotation offsets.
										bottomLeft.subtract(animation.centerPoint);
										topLeft.subtract(animation.centerPoint);
										topRight.subtract(animation.centerPoint);
										bottomRight.subtract(animation.centerPoint);
									}else{
										GL11.glTranslated(component.xCenter + animation.centerPoint.x, component.yCenter + animation.centerPoint.y, 0.0F);
										GL11.glRotated(variableValue, 0, 0, 1);
										GL11.glTranslated(-component.xCenter - animation.centerPoint.x, -component.yCenter - animation.centerPoint.y, 0.0F);
									}
									break;
								}
								case TRANSLATION :{
									//Offset the coords based on the translated amount.
									//Adjust the window to either move or scale depending on settings.
									double xTranslation = entity.getAnimatedVariableValue(animationClock, animation.axis.x, partialTicks);
									double yTranslation = entity.getAnimatedVariableValue(animationClock, animation.axis.y, partialTicks);
									if(component.extendWindow){
										//We need to add to the edge of the window in this case rather than move the entire window.
										if(animation.axis.x < 0){
											bottomLeft.x += xTranslation;
											topLeft.x += xTranslation;
										}else if(animation.axis.x > 0){
											topRight.x += xTranslation;
											bottomRight.x += xTranslation;
										}
										if(animation.axis.y < 0){
											bottomLeft.y += yTranslation;
											bottomRight.y += yTranslation;
										}else if(animation.axis.y > 0){
											topLeft.y += yTranslation;
											topRight.y += yTranslation;
										}
									}else if(component.moveComponent){
										//Translate the rather than adjust the window coords.
										GL11.glTranslated(xTranslation, yTranslation, 0);
									}else{
										//Offset the window coords to the appropriate section of the texture sheet.
										//We don't want to do an OpenGL translation here as that would move the texture's
										//rendered position on the instrument rather than change what texture is rendered.
										if(animation.axis.x != 0){
											bottomLeft.x += xTranslation;
											topLeft.x += xTranslation;
											topRight.x += xTranslation;
											bottomRight.x += xTranslation;
										}
										if(animation.axis.y != 0){
											bottomLeft.y += yTranslation;
											topLeft.y += yTranslation;
											topRight.y += yTranslation;
											bottomRight.y += yTranslation;
										}
									}
									break;
								}
								case SCALING :{
									//Do nothing.  Translation does scaling here.
									break;
								}
								case VISIBILITY:{
									//Skip rendering this component if this is false.
									double variableValue = entity.getAnimatedVariableValue(animationClock, partialTicks);
									skipRender = variableValue < animation.clampMin || variableValue > animation.clampMax;
									break;
								}
								case INHIBITOR:{
									//Skip further operations if this is true.
									if(!skipFurtherTransforms){
										double variableValue = entity.getAnimatedVariableValue(animationClock, partialTicks);
										skipFurtherTransforms = variableValue >= animation.clampMin && variableValue <= animation.clampMax;
									}
									break;
								}
								case ACTIVATOR:{
									//Prevent skipping  further operations if this is true.
									if(skipFurtherTransforms){
										double variableValue = entity.getAnimatedVariableValue(animationClock, partialTicks);
										skipFurtherTransforms = variableValue >= animation.clampMin && variableValue <= animation.clampMax;
									}
									break;
								}
							}
							
							//Put suffix back to normal.
							if(addSuffix){
								animation.variable = animation.variable.substring(0, animation.variable.length() - ("_" + partNumber).length());
							}
							
							//Don't do any more transforms if we shouldn't render.
							if(skipRender){
								break;
							}
						}
					}
					
					//Now that all transforms are done, render the instrument if enabled.
					if(!skipRender){
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
						GL11.glTranslatef(component.xCenter, component.yCenter, 0.0F);
						
						//Set points to the variables here and render them.
						//If the shape is lit, disable lighting for blending.
						if(component.lightUpTexture && lightsOn && ConfigSystem.configObject.clientRendering.brightLights.value){
							InterfaceRender.setLightingState(false);
							renderSquareUV(component, componentScale);
							InterfaceRender.setLightingState(true);
						}else{
							renderSquareUV(component, componentScale);
						}
					}
				}
				
				//Done rendering.  Pop matrix.
				GL11.glPopMatrix();
			}
		}
	}
	
    /**
     * Helper method for setting points for rendering.
     */
	private static void renderSquareUV(Component component, float componentScale){
		//Set X, Y, U, V, and normal Z.  All other values are 0.
		//Also invert V, as we're going off of pixel-coords here.
		for(int i=0; i<points.length; ++i){
			float[] charVertex = points[i];
			switch(i){
				case(0):{//Bottom-right
					charVertex[0] = component.textureWidth/2;
					charVertex[1] = -component.textureHeight/2;
					charVertex[3] = (float) bottomRight.x;
					charVertex[4] = (float) topRight.y;
					break;
				}
				case(1):{//Top-right
					charVertex[0] = component.textureWidth/2;
					charVertex[1] = component.textureHeight/2;
					charVertex[3] = (float) topRight.x;
					charVertex[4] = (float) bottomRight.y;
					break;
				}
				case(2):{//Top-left
					charVertex[0] = -component.textureWidth/2;
					charVertex[1] = component.textureHeight/2;
					charVertex[3] = (float) topLeft.x;
					charVertex[4] = (float) bottomLeft.y;
					break;
				}
				case(3):{//Bottom-right
					charVertex[0] = component.textureWidth/2;
					charVertex[1] = -component.textureHeight/2;
					charVertex[3] = (float) bottomRight.x;
					charVertex[4] = (float) topRight.y;
					break;
				}
				case(4):{//Top-left
					charVertex[0] = -component.textureWidth/2;
					charVertex[1] = component.textureHeight/2;
					charVertex[3] = (float) topLeft.x;
					charVertex[4] = (float) bottomLeft.y;
					break;
				}
				case(5):{//Bottom-left
					charVertex[0] = -component.textureWidth/2;
					charVertex[1] = -component.textureHeight/2;
					charVertex[3] = (float) bottomLeft.x;
					charVertex[4] = (float) topLeft.y;						
					break;
				}
			}
			charVertex[7] = componentScale;
		}
		InterfaceRender.renderVertices(points);
	}
}