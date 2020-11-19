package minecrafttransportsimulator.rendering.instances;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.guis.components.AGUIBase.TextPosition;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONInstrument.Component;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimationDefinition;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.VehicleAnimationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Main render class for instruments.  This class contains a main method that takes an instance of {@link ItemInstrument},
 * as well as the engine associated with that instrument and the vehicle the instrument is on.  This allows for an
 * instrument to be rendered a vehicle, GUI, or HUD.}.
 *
 * @author don_bruce
 */
public final class RenderInstrument{
	private static final Map<EntityVehicleF_Physics, Map<ItemInstrument, Map<Integer, DurationDelayClock>>> clocks = new HashMap<EntityVehicleF_Physics, Map<ItemInstrument, Map<Integer, DurationDelayClock>>>();
	private static final Point3d p1 = new Point3d(0, 0, 0);
	private static final Point3d p2 = new Point3d(0, 0, 0);
	private static final Point3d p3 = new Point3d(0, 0, 0);
	private static final Point3d p4 = new Point3d(0, 0, 0);
	private static final Point3d r = new Point3d(0, 0, 0);
	
	/**
     * Renders the passed-in instrument using the vehicle's current state.  Note that this method does NOT take any 
     * vehicle JSON parameters into account as it does not know which instrument is being rendered.  This means that 
     * any transformations that need to be applied for translation or scaling should be applied prior to calling this
     * method.  Such transformations will, of course, differ between applications, so care should be taken to ensure
     * OpenGL states are not left out-of-whack after rendering is complete.
     */
	public static void drawInstrument(ItemInstrument instrument, int partNumber, EntityVehicleF_Physics vehicle){
		//First bind the texture file for this insturment's pack.
		MasterLoader.renderInterface.setTexture("/assets/" + instrument.definition.packID + "/textures/instruments.png");
		
		//Check if the lights are on.  If so, render the overlays.
		boolean lightsOn = vehicle.areInteriorLightsOn();
		
		//Finally, render the instrument based on the JSON instrument.definitions.
		for(byte i=0; i<instrument.definition.components.size(); ++i){
			Component component = instrument.definition.components.get(i);
			//Only render regular sections on pass 0 or -1, and overlays on pass 1 or -1.
			if(component.lightOverlay ? MasterLoader.renderInterface.getRenderPass() != 0 : MasterLoader.renderInterface.getRenderPass() != 1){
				//If we have text, do a text render.  Otherwise, do a normal instrument render.
				//Also translate slightly away from the instrument location to prevent clipping.
				GL11.glPushMatrix();
				GL11.glTranslatef(0.0F, 0.0F, i*0.1F);
				if(component.textObject != null){
					final boolean addSuffix = !component.textObject.fieldName.substring(component.textObject.fieldName.length() - 1).matches("[0-9]+") && ((component.textObject.fieldName.startsWith("engine_") || component.textObject.fieldName.startsWith("propeller_") || component.textObject.fieldName.startsWith("gun_") || component.textObject.fieldName.startsWith("seat_")));
					double textNumeric = VehicleAnimationSystem.getVariableValue(addSuffix ? component.textObject.fieldName + "_" + partNumber : component.textObject.fieldName, component.textFactor, 0, 0, 0, false, 0, vehicle, null);
					String text = String.format("%0" + component.textObject.maxLength + "d", (int) textNumeric);
					MasterLoader.guiInterface.drawScaledText(text, (int) component.textObject.pos.x, (int) component.textObject.pos.y, Color.decode(component.textObject.color), TextPosition.values()[component.textObject.renderPosition], component.textObject.wrapWidth, component.textObject.scale, component.textObject.autoScale);
					MasterLoader.renderInterface.setColorState(1.0F, 1.0F, 1.0F, 1.0F);
					MasterLoader.renderInterface.recallTexture();
				}else{
					//Init variables.
					p1.set(-component.textureWidth/2D, -component.textureHeight/2D, 0);
					p2.set(-component.textureWidth/2D, component.textureHeight/2D, 0);
					p3.set(component.textureWidth/2D, component.textureHeight/2D, 0);
					p4.set(component.textureWidth/2D, -component.textureHeight/2D, 0);
					boolean skipRender = false;
					boolean skipFurtherTransforms = false;
					if(component.animations != null){
						for(VehicleAnimationDefinition animation : component.animations){
							//If the partNumber is non-zero, we need to check if we are applying a part-based animation.
							//If so, we need to let the animation system know by adding a suffix to the variable.
							//Otherwise, as we don't pass-in the part, it will assume it's a vehicle variable.
							//We also need to set the partNumber to 1 if we have a part number of 0 and we're
							//doing a part-specific animation.
							//Skip adding a suffix if one already exists.
							final boolean addSuffix = !animation.variable.substring(animation.variable.length() - 1).matches("[0-9]+") && (animation.variable.startsWith("engine_") || animation.variable.startsWith("propeller_") || animation.variable.startsWith("gun_") || animation.variable.startsWith("seat_"));
							if(partNumber == 0 && addSuffix){
								partNumber = 1;
							}
							String variable = addSuffix ? animation.variable + "_" + partNumber : animation.variable;
							int clockAnimationMapIndex = (((int) partNumber) << Byte.SIZE*2) | ((int) i << Byte.SIZE*1) | (component.animations.indexOf(animation));
							
							switch(animation.animationType){
								case("rotation"):{
									//Depending on what variables are set we do different rendering operations.
									//If we are rotating the window, but not the texture we should offset the texture points to that rotated point.
									//Otherwise, we apply an OpenGL rotation operation.
									double rotation = getClock(vehicle, instrument, clockAnimationMapIndex).getFactoredState(vehicle, VehicleAnimationSystem.getVariableValue(variable, 0, vehicle, null));
									rotation = VehicleAnimationSystem.clampAndScale(rotation, animation.axis.length(), animation.offset, animation.clampMin, animation.clampMax, animation.absolute);
									if(component.rotateWindow){
										//Add rotation offset to the points.
										p1.add(animation.centerPoint);
										p2.add(animation.centerPoint);
										p3.add(animation.centerPoint);
										p4.add(animation.centerPoint);
										
										//Rotate the points by the rotation.
										r.set(0, 0, rotation);
										p1.rotateFine(r);
										p2.rotateFine(r);
										p3.rotateFine(r);
										p4.rotateFine(r);
										
										//Remove the rotation offsets.
										p1.subtract(animation.centerPoint);
										p2.subtract(animation.centerPoint);
										p3.subtract(animation.centerPoint);
										p4.subtract(animation.centerPoint);
									}else{
										GL11.glTranslated(component.xCenter + animation.centerPoint.x, component.yCenter + animation.centerPoint.y, 0.0F);
										GL11.glRotated(rotation, 0, 0, 1);
										GL11.glTranslated(-component.xCenter - animation.centerPoint.x, -component.yCenter - animation.centerPoint.y, 0.0F);
									}
									break;
								}
								case("translation"):{
									//Offset the coords based on the translated amount.
									//Adjust the window to either move or scale depending on settings.
									double axisLength = animation.axis.length();
									double translation = getClock(vehicle, instrument, clockAnimationMapIndex).getFactoredState(vehicle, VehicleAnimationSystem.getVariableValue(variable,  0, vehicle, null));
									translation = VehicleAnimationSystem.clampAndScale(translation, axisLength, animation.offset, animation.clampMin, animation.clampMax, animation.absolute);
									double xTranslation = translation*animation.axis.x/axisLength;
									double yTranslation = translation*animation.axis.y/axisLength;
									if(component.extendWindow){
										//We need to add to the edge of the window in this case rather than move the entire window.
										if(animation.axis.x < 0){
											p1.x += xTranslation;
											p2.x += xTranslation;
										}else if(animation.axis.x > 0){
											p3.x += xTranslation;
											p4.x += xTranslation;
										}
										if(animation.axis.y < 0){
											p1.y += yTranslation;
											p4.y += yTranslation;
										}else if(animation.axis.y > 0){
											p2.y += yTranslation;
											p3.y += yTranslation;
										}
									}else{
										//Offset the window coords to the appropriate section of the texture sheet.
										//We don't want to do an OpenGL translation here as that would move the texture's
										//rendered position on the instrument rather than change what texture is rendered.
										if(animation.axis.x != 0){
											p1.x += xTranslation;
											p2.x += xTranslation;
											p3.x += xTranslation;
											p4.x += xTranslation;
										}
										if(animation.axis.y != 0){
											p1.y += yTranslation;
											p2.y += yTranslation;
											p3.y += yTranslation;
											p4.y += yTranslation;
										}
									}
									break;
								}
								case("visibility"):{
									//Skip rendering this component if this is false.
									double value = animation.offset + getClock(vehicle, instrument, clockAnimationMapIndex).getFactoredState(vehicle, VehicleAnimationSystem.getVariableValue(variable, 0, vehicle, null));
									skipRender = value < animation.clampMin || value > animation.clampMax;
									skipFurtherTransforms = skipRender;
									break;
								}
								case("inhibitor"):{
									//Skip further operations if this is false.
									double value = animation.offset + getClock(vehicle, instrument, clockAnimationMapIndex).getFactoredState(vehicle, VehicleAnimationSystem.getVariableValue(variable, 0, vehicle, null));
									skipFurtherTransforms = value >= animation.clampMin && value <= animation.clampMax;
									break;
								}
							}
							
							if(skipFurtherTransforms){
								break;
							}
						}
					}
					
					//Now that all transforms are done, render the instrument if enabled.
					if(!skipRender){
						//Add the instrument UV-map offsets.
						//These don't get added to the initial points to allow for rotation.
						p1.add(component.textureXCenter, component.textureYCenter, 0);
						p2.add(component.textureXCenter, component.textureYCenter, 0);
						p3.add(component.textureXCenter, component.textureYCenter, 0);
						p4.add(component.textureXCenter, component.textureYCenter, 0);
						
						//Divide the Points by 1024.  This converts the points from pixels to the 0-1 UV values.
						p1.multiply(1D/1024D);
						p2.multiply(1D/1024D);
						p3.multiply(1D/1024D);
						p4.multiply(1D/1024D);
						
						//Translate to the component.
						GL11.glTranslatef(component.xCenter, component.yCenter, 0.0F);
						
						//If the shape is lit, do blending.  If not, just render normally.
						if(!component.lightOverlay){
							if(component.lightUpTexture && lightsOn){
								MasterLoader.renderInterface.setLightingState(false);
								renderSquareUV(component.textureWidth, component.textureHeight, p1, p2, p3, p4);
								MasterLoader.renderInterface.setLightingState(true);
							}else{
								renderSquareUV(component.textureWidth, component.textureHeight, p1, p2, p3, p4);
							}
						}else if(lightsOn && ConfigSystem.configObject.clientRendering.instBlending.value){
							GL11.glEnable(GL11.GL_BLEND);
							MasterLoader.renderInterface.setLightingState(false);
						    renderSquareUV(component.textureWidth, component.textureHeight, p1, p2, p3, p4);
						    MasterLoader.renderInterface.setLightingState(true);
							if(MasterLoader.renderInterface.getRenderPass() != 1){
								GL11.glDisable(GL11.GL_BLEND);
							}
						}
					}
				}
				
				//Done rendering.  Pop matrix.
				GL11.glPopMatrix();
			}
		}
	}
	
	/**
	 *  Returns the current animation clock for the passed-in vehicle/instrument/part/animation set.
	 *  Clocks are not shared between these three components to allow each to have their own clock.
	 *  If a clock is not present, one is created.  The index parameter should be a combined 3-byte
	 *  integer, where the first byte is the part number for the vehicle, the second is the section
	 *  of the instrument JSON to render, and the third is the animation index.
	 */
	private static DurationDelayClock getClock(EntityVehicleF_Physics vehicle, ItemInstrument instrument, int clockAnimationMapIndex){
		if(!clocks.containsKey(vehicle)){
			clocks.put(vehicle, new HashMap<ItemInstrument, Map<Integer, DurationDelayClock>>());
		}
		Map<ItemInstrument, Map<Integer,DurationDelayClock>> vehicleClocks = clocks.get(vehicle);
		if(!vehicleClocks.containsKey(instrument)){
			vehicleClocks.put(instrument, new HashMap<Integer, DurationDelayClock>());
		}
		Map<Integer, DurationDelayClock> instrumentClocks = vehicleClocks.get(instrument);
		if(!instrumentClocks.containsKey(clockAnimationMapIndex)){
			int instrumentSection = (byte) (clockAnimationMapIndex >> Byte.SIZE);
			int animationSection = (byte) (clockAnimationMapIndex);
			instrumentClocks.put(clockAnimationMapIndex, new DurationDelayClock(instrument.definition.components.get(instrumentSection).animations.get(animationSection)));
		}
		return instrumentClocks.get(clockAnimationMapIndex);
	}
	
    /**
     * Renders a textured quad from the current bound texture of a specific width and height.
     * Used for rendering instrument textures off their texture sheets.
     */
	private static void renderSquareUV(float width, float height, Point3d p1, Point3d p2, Point3d p3, Point3d p4){
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2d(p1.x, p1.y);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3f(-width/2, -height/2, 0);
		GL11.glTexCoord2d(p2.x, p2.y);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3f(-width/2, height/2, 0);
		GL11.glTexCoord2d(p3.x, p3.y);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3f(width/2, height/2, 0);
		GL11.glTexCoord2d(p4.x, p4.y);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3f(width/2, -height/2, 0);
		GL11.glEnd();
	}
}

