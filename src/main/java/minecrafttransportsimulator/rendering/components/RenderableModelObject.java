package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.jsondefs.JSONLight.JSONLightBlendableComponent;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.rendering.instances.RenderText;
import minecrafttransportsimulator.systems.ConfigSystem;

/**This class represents an object that can be rendered from a model.  This object is a set of
 * faces that are rendered during the main rendering routine.  Various transforms may be performed on
 * this object via the various rendering classes.  These transforms are applied to the mesh prior
 * to rendering, either manipulating the mesh directly, or manipulating the OpenGL state.
 *
 * @author don_bruce
 */
public class RenderableModelObject<AnimationEntity extends AEntityC_Definable<?>>{
	public final String modelLocation;
	public final String objectName;
	private final List<RenderableModelObject<AnimationEntity>> allObjects;
	private final boolean isTranslucent;
	private final boolean isWindow;
	private final boolean isOnlineTexture;
	private final int cachedVertexIndex;
	private final float[][] exteriorWindowObject;
	private final float[][] interiorWindowObject;
	private float[][] colorObject;
	private float[][] coverObject;
	private final Map<JSONLight, float[][]> flareObjects = new HashMap<JSONLight, float[][]>();
	private final Map<JSONLight, float[][]> beamObjects = new HashMap<JSONLight, float[][]>();
	
	/**Map of tread points, keyed by the model the tread is pathing about, then the spacing of the tread.
	 * This can be shared for two different treads of the same spacing as they render the same.**/
	private static final Map<String, Map<Float, List<Double[]>>> treadPoints = new HashMap<String, Map<Float, List<Double[]>>>();
	private static final float COLOR_OFFSET = 0.0001F;
	private static final float FLARE_OFFSET = 0.0002F;
	private static final float COVER_OFFSET = 0.0003F;
	private static final float BEAM_OFFSET = -0.15F;
	private static final int BEAM_SEGMENTS = 40;
	
	public RenderableModelObject(String modelLocation, String objectName, List<RenderableModelObject<AnimationEntity>> allObjects, float[][] vertices){
		super();
		this.modelLocation = modelLocation;
		this.objectName = objectName;
		this.allObjects = allObjects;
		this.isTranslucent = objectName.toLowerCase().contains(AModelParser.TRANSLUCENT_OBJECT_NAME);
		this.isWindow = objectName.toLowerCase().contains(AModelParser.WINDOW_OBJECT_NAME);
		this.isOnlineTexture = objectName.toLowerCase().startsWith(AModelParser.ONLINE_TEXTURE_OBJECT_NAME) || objectName.toLowerCase().endsWith(AModelParser.ONLINE_TEXTURE_OBJECT_NAME);
		
		//Cache the displayList if we aren't a window.  If we are, just do raw rendering.
		//If we are a window, adjust our UV mapping points to map to a single texture.
		//Set these adjusted points, and then invert them for the interior component.
		if(isWindow){
			this.cachedVertexIndex = -1;
			this.exteriorWindowObject = vertices;
			normalizeUVs(exteriorWindowObject);
			
			this.interiorWindowObject = new float[exteriorWindowObject.length][8];
			for(int i=0, j=exteriorWindowObject.length-1; i<exteriorWindowObject.length; ++i, --j){
				interiorWindowObject[j] = exteriorWindowObject[i];
			}
		}else{
			this.cachedVertexIndex = InterfaceRender.cacheVertices(vertices);
			this.exteriorWindowObject = null;
			this.interiorWindowObject = null;
		}
		
		//If we are a light object, create color and cover points.
		//We may not use these, but it saves on processing later as we don't need to re-parse the model.
		if(objectName.startsWith("&")){
			colorObject = generateColors(vertices);
			coverObject = generateCovers(vertices);
		}
	}
	
	/**
	 *  Renders this object, applying any transforms that need to happen.  This method also
	 *  renders any objects that depend on this object's transforms after rendering.
	 */
	public void render(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		JSONLight lightDef = entity.lightObjectDefinitions.get(objectName);
		float lightLevel = lightDef != null ? entity.lightBrightnessValues.get(lightDef) : 0;
		if(shouldRender(entity, lightDef, blendingEnabled)){
			//Do pre-render checks based on the object we are rendering.
			//This may block rendering if there are false visibility transforms.
			GL11.glPushMatrix();
			JSONAnimatedObject definition = entity.animatedObjectDefinitions.get(objectName);
			if(doPreRenderTransforms(entity, definition != null ? definition.animations : null, blendingEnabled, partialTicks)){
				//If we are a window or online texture, bind that one rather than our own.
				if(isWindow){
					InterfaceRender.bindTexture("mts:textures/rendering/glass.png");
				}else if(isOnlineTexture){
					//Get the texture from the text objects of the entity.
					//If we don't have anything set, we just use the existing texture.
					for(Entry<JSONText, String> textEntry : entity.text.entrySet()){
						JSONText textDef = textEntry.getKey();
						if(textDef.fieldName.equals(objectName)){
							String textValue = entity.text.get(textDef);
							if(!textValue.isEmpty() && !textValue.contains(" ")){
								String errorString = InterfaceRender.bindURLTexture(textValue);
								if(errorString != null){
									textEntry.setValue(errorString);
								}
							}
							break;
						}
					}
				}
				
				//If we are a light, get the actual light level as calculated.
				//We do this here as there's no reason to calculate this if we're not gonna render.
				if(lightDef != null){
					lightLevel = entity.lightBrightnessValues.get(lightDef);
					if(lightDef.isElectric && entity instanceof EntityVehicleF_Physics){
						//Light start dimming at 10V, then go dark at 3V.
						double electricPower = ((EntityVehicleF_Physics) entity).electricPower;
						if(electricPower < 3){
							lightLevel = 0;
						}else if(electricPower < 10){
							lightLevel *= (electricPower - 3)/7D; 
						}
					}
				}
				
				//Render us based on the current pass and our states.
				if(blendingEnabled && lightDef != null && lightLevel > 0 && lightDef.isBeam && entity.shouldRenderBeams()){
					//Model that's actually a beam, render it with beam lighting/blending. 
					if(ConfigSystem.configObject.clientRendering.brightLights.value){
						InterfaceRender.setLightingState(false);
						if(ConfigSystem.configObject.clientRendering.blendedLights.value){
							InterfaceRender.setBlendBright(true);
							InterfaceRender.renderVertices(cachedVertexIndex);
							InterfaceRender.setBlendBright(false);
						}else{
							InterfaceRender.renderVertices(cachedVertexIndex);
						}
						InterfaceRender.setLightingState(true);
					}else{
						InterfaceRender.renderVertices(cachedVertexIndex);
					}
				}else if(!blendingEnabled && entity instanceof PartGroundDevice && ((PartGroundDevice) entity).definition.ground.isTread && !((PartGroundDevice) entity).placementDefinition.isSpare){
					//Active tread.  Do tread-path rendering.					
					doTreadRendering((PartGroundDevice) entity, partialTicks);
				}else if(!(blendingEnabled ^ isTranslucent)){
					//Either solid texture on solid pass, or translucent texture on blended pass.
					//If we are a window, render inner and outer manually.
					//Otherwise, render cached vertices.
					if(isWindow){
						InterfaceRender.renderVertices(exteriorWindowObject);
						if(ConfigSystem.configObject.clientRendering.innerWindows.value){
							InterfaceRender.renderVertices(interiorWindowObject);
						}
					}else{
						//Need to disable lighting if we are a light-up texture.
						if(ConfigSystem.configObject.clientRendering.brightLights.value && lightDef != null && lightLevel > 0 && !lightDef.emissive && !lightDef.isBeam){
							InterfaceRender.setLightingState(false);
							InterfaceRender.renderVertices(cachedVertexIndex);
							InterfaceRender.setLightingState(true);
						}else{
							InterfaceRender.renderVertices(cachedVertexIndex);
						}
					}
				}
				
				//Check if we are a light that's not a beam, do light-specific rendering.
				if(lightDef != null && !lightDef.isBeam){
					doLightRendering(entity, lightDef, lightLevel, entity.lightColorValues.get(lightDef), blendingEnabled);
				}
				
				//Pop the texture back to the one we last bound in case we changed it.
				InterfaceRender.recallTexture();
				
				//Render text on this object.  Only do this on the solid pass.
				if(!blendingEnabled){
					for(JSONText textDef : entity.text.keySet()){
						if(objectName.equals(textDef.attachedTo)){
							//TODO this is technically wrong, but are people ever going to scale parts with text on them?
							RenderText.draw3DText(entity.text.get(textDef), entity, textDef, 1.0F, false);
						}
					}
				}
				
				//Render any objects that depend on us before we pop our state.
				for(RenderableModelObject<AnimationEntity> modelObject : allObjects){
					JSONAnimatedObject animation = entity.animatedObjectDefinitions.get(modelObject.objectName);
					if(animation != null && objectName.equals(animation.applyAfter)){
						modelObject.render(entity, blendingEnabled, partialTicks);
					}
				}
			}
			
			//Pop state.
			GL11.glPopMatrix();
		}
	}
	
	/**
	 *  Does all the transforms for this object.  If the object should render, return true. 
	 *  If the object should not render due to a transform, return false.
	 *  This is static as it's common to all renderable objects, including those not in this class.
	 */
	public static boolean doPreRenderTransforms(AEntityC_Definable<?> entity, List<JSONAnimationDefinition> animations, boolean blendingEnabled, float partialTicks){
		if(animations != null){
			double variableValue = 0;
			double priorOffset = 0;
			boolean inhibitAnimations = false;
			for(JSONAnimationDefinition animation : animations){
				//If the animation is a cumulative offset, we need to add the prior value to our variable. 
				priorOffset = animation.addPriorOffset ? variableValue : 0;
				variableValue = 0;
				
				switch(animation.animationType){
					case VISIBILITY :{
						if(!inhibitAnimations){
							variableValue = entity.getAnimatedVariableValue(entity.animationClocks.get(animation), partialTicks);
							if(variableValue < animation.clampMin || variableValue > animation.clampMax){
								return false;
							}
						}
						break;
					}
					case INHIBITOR :{
						if(!inhibitAnimations){
							variableValue =  entity.getAnimatedVariableValue(entity.animationClocks.get(animation), partialTicks);
							if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
								inhibitAnimations = true;
							}
						}
						break;
					}
					case ACTIVATOR :{
						if(inhibitAnimations){
							variableValue = entity.getAnimatedVariableValue(entity.animationClocks.get(animation), partialTicks);
							if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
								inhibitAnimations = false;
							}
						}
						break;
					}
					case TRANSLATION :{
						if(!inhibitAnimations){
							double magnitude = animation.axis.length();
							variableValue = entity.getAnimatedVariableValue(entity.animationClocks.get(animation), magnitude, priorOffset, partialTicks);
							//Do the actual translation, if we aren't 0.
							if(animation.addPriorOffset){
								GL11.glTranslated((variableValue - priorOffset)*animation.axis.x/magnitude, (variableValue - priorOffset)*animation.axis.y/magnitude, (variableValue - priorOffset)*animation.axis.z/magnitude);
							}else if(variableValue != 0){
								GL11.glTranslated(variableValue*animation.axis.x/magnitude, variableValue*animation.axis.y/magnitude, variableValue*animation.axis.z/magnitude);
							}
						}
						break;
					}
					case ROTATION :{
						if(!inhibitAnimations){
							double magnitude = animation.axis.length();
							variableValue = entity.getAnimatedVariableValue(entity.animationClocks.get(animation), magnitude, priorOffset, partialTicks);
							//Do rotation.
							if(animation.addPriorOffset){
								GL11.glTranslated(animation.centerPoint.x, animation.centerPoint.y, animation.centerPoint.z);
								GL11.glRotated((variableValue - priorOffset), animation.axis.x/magnitude, animation.axis.y/magnitude, animation.axis.z/magnitude);
								GL11.glTranslated(-animation.centerPoint.x, -animation.centerPoint.y, -animation.centerPoint.z);
							}else if(variableValue != 0){
								GL11.glTranslated(animation.centerPoint.x, animation.centerPoint.y, animation.centerPoint.z);
								GL11.glRotated(variableValue, animation.axis.x/magnitude, animation.axis.y/magnitude, animation.axis.z/magnitude);
								GL11.glTranslated(-animation.centerPoint.x, -animation.centerPoint.y, -animation.centerPoint.z);
							}
						}
						break;
					}
					case SCALING :{
						if(!inhibitAnimations){
							double magnitude = animation.axis.length();
							variableValue = entity.getAnimatedVariableValue(entity.animationClocks.get(animation), magnitude, priorOffset, partialTicks);
							//Do the actual scaling.
							GL11.glTranslated(animation.centerPoint.x, animation.centerPoint.y, animation.centerPoint.z);
							GL11.glScaled(animation.axis.x == 0 ? 1.0 : variableValue*animation.axis.x/magnitude, animation.axis.y == 0 ? 1.0 : variableValue*animation.axis.y/magnitude, animation.axis.z == 0 ? 1.0 : variableValue*animation.axis.z/magnitude);
							GL11.glTranslated(-animation.centerPoint.x, -animation.centerPoint.y, -animation.centerPoint.z);
						}
						break;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 *  Call to destory this renderable object.  This should be done prior to re-parsing the model
	 *  as it allows for the freeing of OpenGL resources.
	 */
	public void destroy(){
		InterfaceRender.deleteVertices(cachedVertexIndex);
		treadPoints.remove(modelLocation);
		flareObjects.remove(objectName);
		beamObjects.remove(objectName);
	}
	
	private boolean shouldRender(AnimationEntity entity, JSONLight lightDef, boolean blendingEnabled){
		if(isTranslucent && !blendingEnabled){
			return false;
		}
		if(isWindow && !ConfigSystem.configObject.clientRendering.renderWindows.value){
			return false;
		}
		if(isOnlineTexture){
			//Make sure the entity has a texture for us.
			for(JSONText textDef : entity.text.keySet()){
				if(textDef.fieldName.equals(objectName)){
					if(entity.text.get(textDef).isEmpty()){
						return false;
					}
					break;
				}
			}
		}
		if(lightDef != null){
			//If the light only has solid components, and we aren't translucent, don't render on the blending pass.
			if(blendingEnabled && !isTranslucent && !lightDef.emissive && !lightDef.isBeam && (lightDef.blendableComponents == null || lightDef.blendableComponents.isEmpty())){
				return false;
			}
		}
		return true;
	}
	
	private void doTreadRendering(PartGroundDevice tread, float partialTicks){
		AEntityC_Definable<?> entityTreadAttachedTo = tread.placementDefinition.isSubPart ? tread.parentPart : tread.entityOn;
		String treadPathModel = entityTreadAttachedTo.definition.getModelLocation(entityTreadAttachedTo.subName); 
		Map<Float, List<Double[]>> treadPointsMap = treadPoints.get(treadPathModel);
		if(treadPointsMap == null){
			treadPointsMap = new HashMap<Float, List<Double[]>>();
		}
		List<Double[]> points = treadPointsMap.get(tread.definition.ground.spacing);
		
		if(points == null){
			points = generateTreads(entityTreadAttachedTo, treadPathModel, treadPointsMap, tread);
			treadPointsMap.put(tread.definition.ground.spacing, points);
			treadPoints.put(treadPathModel, treadPointsMap);
		}
				
		//Render the treads along their points.
		//We manually set point 0 here due to the fact it's a joint between two differing angles.
		//We also need to translate to that point to start rendering as we're currently at 0,0,0.
		//For each remaining point, we only translate the delta of the point.
		float treadLinearPosition = (float) ((Math.abs(tread.angularPosition) + tread.angularVelocity*partialTicks)*EntityVehicleF_Physics.SPEED_FACTOR);
		float treadMovementPercentage = treadLinearPosition%tread.definition.ground.spacing/tread.definition.ground.spacing;
		if(tread.angularPosition < 0){
			treadMovementPercentage = 1 - treadMovementPercentage;
		}
		Double[] point;
		Double[] nextPoint;
		double yDelta;
		double zDelta;
		double angleDelta;
		
		//Tread rendering is done via the thing the tread is on, which will assume the part is centered at 0, 0, 0.
		//We need to undo the offset of the tread part for this routine.
		if(!(entityTreadAttachedTo instanceof APart)){
			GL11.glTranslated(0, -tread.localOffset.y, -tread.localOffset.z);	
		}
		
		for(int i=0; i<points.size() - 1; ++i){
			//Update variables.
			//If we're at the last point, set the next point to the first point.
			point = points.get(i);
			if(i == points.size() - 1){
				nextPoint = points.get(0);
			}else{
				nextPoint = points.get(i + 1);
			}
			yDelta = nextPoint[0] - point[0];
			zDelta = nextPoint[1] - point[1];
			angleDelta = nextPoint[2] - point[2];
			
			//If we're at the first point, do initial translation.
			if(i == 0){
				GL11.glTranslated(0, point[0], point[1]);
			}
			
			//If our angle delta is greater than 180, we can assume that we're inverted.
			//This happens when we cross the 360 degree rotation barrier.
			if(angleDelta > 180){
				angleDelta -= 360;
			}else if(angleDelta < -180){
				angleDelta += 360;
			}
			
			//If there's no rotation to the point, and no delta between points, don't do rotation.
			//That's an expensive operation due to sin and cos operations.
			//Do note that the model needs to be flipped 180 on the X-axis due to all our points
			//assuming a YZ coordinate system with 0 degrees rotation being in +Y.
			//This is why 180 is added to all points cached in the operations above.
			if(point[2] != 0 || angleDelta != 0){
				//We can't use a running rotation here as we'll end up translating in the rotated
				//coordinate system.  To combat this, we translate like normal, but then push a
				//stack and rotate prior to rendering.  This keeps us from having to do another
				//rotation to get the old coordinate system back.
				GL11.glPushMatrix();
				GL11.glTranslated(0, yDelta*treadMovementPercentage, zDelta*treadMovementPercentage);
				GL11.glRotated(point[2] + angleDelta*treadMovementPercentage, 1, 0, 0);
				InterfaceRender.renderVertices(cachedVertexIndex);
				GL11.glPopMatrix();
				GL11.glTranslated(0, yDelta, zDelta);
			}else{
				//Translate to the current position of the tread based on the percent it has moved.
				//This is determined by partial ticks and actual tread position.
				//Once there, render the tread.  Then translate the remainder of the way to prepare
				//to render the next tread.
				GL11.glTranslated(0, yDelta*treadMovementPercentage, zDelta*treadMovementPercentage);
				InterfaceRender.renderVertices(cachedVertexIndex);
				GL11.glTranslated(0, yDelta*(1 - treadMovementPercentage), zDelta*(1 - treadMovementPercentage));
			}
		}
	}
		
	private void doLightRendering(AnimationEntity entity, JSONLight lightDef, float lightLevel, ColorRGB color, boolean blendingEnabled){
		boolean lightingDisabled = false;
		boolean brightBlendEnabled = false;
		boolean colorChanged = false;
		if(blendingEnabled && lightLevel > 0 && lightDef.emissive){
			//Light color detected on blended render pass.
			if(colorObject == null){
				colorObject = generateColors(AModelParser.parseModel(modelLocation).get(objectName));
			}
			
			InterfaceRender.bindTexture("mts:textures/rendering/light.png");
			if(ConfigSystem.configObject.clientRendering.brightLights.value){
				InterfaceRender.setLightingState(false);
				lightingDisabled = true;
			}
			InterfaceRender.setColorState(color, lightLevel);
			colorChanged = true;
			InterfaceRender.renderVertices(colorObject);
			
		}
		if(blendingEnabled && lightLevel > 0 && lightDef.blendableComponents != null && !lightDef.blendableComponents.isEmpty()){
			//Light flares or beams detected on blended render pass.
			//First render all flares, then render all beams.
			float blendableBrightness = Math.min((1 - entity.world.getLightBrightness(entity.position, false))*lightLevel, 1);
			if(blendableBrightness > 0){
				float[][] flareObject = flareObjects.get(lightDef);
				float[][] beamObject = beamObjects.get(lightDef);
				if(flareObject == null && beamObject == null){
					List<JSONLightBlendableComponent> flareDefs = new ArrayList<JSONLightBlendableComponent>();
					List<JSONLightBlendableComponent> beamDefs = new ArrayList<JSONLightBlendableComponent>();
					for(JSONLightBlendableComponent component : lightDef.blendableComponents){
						if(component.flareHeight > 0){
							flareDefs.add(component);
						}
						if(component.beamDiameter > 0){
							beamDefs.add(component);
						}
					}
					if(!flareDefs.isEmpty()){
						flareObjects.put(lightDef, flareObject = generateFlares(flareDefs));
					}
					if(!beamDefs.isEmpty()){
						beamObjects.put(lightDef, beamObject = generateBeams(beamDefs));
					}
				}
				
				
				//Render all flares.
				if(flareObject != null){
					InterfaceRender.bindTexture("mts:textures/rendering/lensflare.png");
					if(ConfigSystem.configObject.clientRendering.brightLights.value){
						InterfaceRender.setLightingState(false);
						lightingDisabled = true;
					}
					InterfaceRender.setColorState(color, blendableBrightness);
					colorChanged = true;
					InterfaceRender.renderVertices(flareObject);
					
				}
				
				//Render all beams.
				if(beamObject != null && entity.shouldRenderBeams()){
					InterfaceRender.bindTexture("mts:textures/rendering/lightbeam.png");
					//Don't set lighting if flares already did so.
					if(!lightingDisabled && ConfigSystem.configObject.clientRendering.brightLights.value){
						InterfaceRender.setLightingState(false);
						lightingDisabled = true;
					}
					if(ConfigSystem.configObject.clientRendering.blendedLights.value){
						InterfaceRender.setBlendBright(true);
						brightBlendEnabled = true;
					}else if(brightBlendEnabled){
						//Bright blend was turned on for flares, turn off for beams.
						InterfaceRender.setBlendBright(false);
						brightBlendEnabled = false;
					}
					if(flareObject == null){
						InterfaceRender.setColorState(color, blendableBrightness);
						colorChanged = true;
					}
					InterfaceRender.renderVertices(beamObject);
				}
			}
		}
		if(!blendingEnabled && lightDef.covered){
			//Light cover detected on solid render pass.
			if(coverObject == null){
				coverObject = generateCovers(AModelParser.parseModel(modelLocation).get(objectName));
			}
			
			InterfaceRender.bindTexture("minecraft:textures/blocks/glass.png");
			if(lightLevel > 0){
				if(ConfigSystem.configObject.clientRendering.brightLights.value){
					InterfaceRender.setLightingState(false);
					lightingDisabled = true;
				}
			}
			InterfaceRender.renderVertices(coverObject);
			
		}
		
		//Set lighting and color back to normal if it was changed.
		if(lightingDisabled){
			InterfaceRender.setLightingState(true);
		}
		if(brightBlendEnabled){
			InterfaceRender.setBlendBright(false);
		}
		if(colorChanged){
			InterfaceRender.setColorState(ColorRGB.WHITE, 1.0F);
		}
	}
	
	private static float[][] generateColors(float[][] parsedObject){
		//Make a duplicate set of vertices with an offset for the color rendering.
		float[][] offsetObject = new float[parsedObject.length][8];
		for(int i=0; i<parsedObject.length; ++i){
			offsetObject[i][0] = parsedObject[i][0] + parsedObject[i][5]*COLOR_OFFSET;
			offsetObject[i][1] = parsedObject[i][1] + parsedObject[i][6]*COLOR_OFFSET;
			offsetObject[i][2] = parsedObject[i][2] + parsedObject[i][7]*COLOR_OFFSET;
			offsetObject[i][5] = parsedObject[i][5];
			offsetObject[i][6] = parsedObject[i][6];
			offsetObject[i][7] = parsedObject[i][7];
		}
		normalizeUVs(offsetObject);
		return offsetObject;
	}
	
	private static float[][] generateCovers(float[][] parsedObject){
		//Make a duplicate set of vertices with an offset for the cover rendering.
		float[][] offsetObject = new float[parsedObject.length][8];
		for(int i=0; i<parsedObject.length; ++i){
			offsetObject[i][0] = parsedObject[i][0] + parsedObject[i][5]*COVER_OFFSET;
			offsetObject[i][1] = parsedObject[i][1] + parsedObject[i][6]*COVER_OFFSET;
			offsetObject[i][2] = parsedObject[i][2] + parsedObject[i][7]*COVER_OFFSET;
			offsetObject[i][5] = parsedObject[i][5];
			offsetObject[i][6] = parsedObject[i][6];
			offsetObject[i][7] = parsedObject[i][7];
		}
		normalizeUVs(offsetObject);
		return offsetObject;
	}
	
	private static float[][] generateFlares(List<JSONLightBlendableComponent> flareDefs){
		//6 vertices per flare due to triangle rendering.
		float[][] flareObject = new float[flareDefs.size()*6][8];
		for(int i=0; i<flareDefs.size(); ++i){
			JSONLightBlendableComponent flareDef = flareDefs.get(i);
			//Get the angle that is needed to rotate points to the normalized vector.
			Point3d rotation = flareDef.axis.copy().getAngles(false);
			Point3d vertexOffset = new Point3d();
			Point3d centerOffset = flareDef.axis.copy().multiply(FLARE_OFFSET).add(flareDef.pos);
			for(int j=0; j<6; ++j){
				float[] newVertex = new float[8];
				//Get the current UV points.
				switch(j){
					case(0): newVertex[3] = 0.0F; newVertex[4] = 0.0F; break;
					case(1): newVertex[3] = 0.0F; newVertex[4] = 1.0F; break;
					case(2): newVertex[3] = 1.0F; newVertex[4] = 1.0F; break;
					case(3): newVertex[3] = 0.0F; newVertex[4] = 0.0F; break;
					case(4): newVertex[3] = 1.0F; newVertex[4] = 1.0F; break;
					case(5): newVertex[3] = 1.0F; newVertex[4] = 0.0F; break;
				}
				
				//Based on the UVs and the axis for the flare, calculate the vertices.
				vertexOffset.x = newVertex[3] == 0.0 ? -flareDef.flareWidth/2D : flareDef.flareWidth/2D;
				vertexOffset.y = newVertex[4] == 0.0 ? flareDef.flareHeight/2D : -flareDef.flareHeight/2D;
				vertexOffset.z = 0;
				vertexOffset.rotateFine(rotation).add(centerOffset);
				newVertex[0] = (float) vertexOffset.x;
				newVertex[1] = (float) vertexOffset.y;
				newVertex[2] = (float) vertexOffset.z;
				
				//Set normals to the normal axis in the JSON.
				newVertex[5] = (float) flareDef.axis.x;
				newVertex[6] = (float) flareDef.axis.y;
				newVertex[7] = (float) flareDef.axis.z;
				
				//Set the actual vertex.
				flareObject[i*6 + j] = newVertex;
			}
		}
		return flareObject;
	}
	
	private static float[][] generateBeams(List<JSONLightBlendableComponent> beamDefs){
		//3 vertices per cone-face, each share the same center point.
		//Number of cone faces is equal to the number of segments for beams.
		//We render two beams.  One inner and one outer.
		float[][] beamObject = new float[beamDefs.size()*2*BEAM_SEGMENTS*3][8];
		for(int i=0; i<beamDefs.size(); ++i){
			JSONLightBlendableComponent beamDef = beamDefs.get(i);
			//Get the angle that is needed to rotate points to the normalized vector.
			Point3d rotation = beamDef.axis.copy().getAngles(false);
			Point3d vertexOffset = new Point3d();
			Point3d centerOffset = beamDef.axis.copy().multiply(BEAM_OFFSET).add(beamDef.pos);
			//Go from negative to positive to render both beam-faces in the same loop.
			for(int j=-BEAM_SEGMENTS; j<BEAM_SEGMENTS; ++j){
				for(int k=0; k<3; ++k){
					float[] newVertex = new float[8];
					//Get the current UV points.
					//Point 0 is always the center of the beam, 1 and 2 are the outer points.
					switch(k%3){
						case(0): newVertex[3] = 0.0F; newVertex[4] = 0.0F; break;
						case(1): newVertex[3] = 0.0F; newVertex[4] = 1.0F; break;
						case(2): newVertex[3] = 1.0F; newVertex[4] = 1.0F; break;
					}
					
					//Based on the UVs and the axis for the beam, calculate the vertices.
					double currentAngleRad;
					if(j<0){
						currentAngleRad = newVertex[3] == 0.0F ? 2D*Math.PI*((j+1)/(double)BEAM_SEGMENTS) : 2D*Math.PI*(j/(double)BEAM_SEGMENTS);
					}else{
						currentAngleRad = newVertex[3] == 0.0F ? 2D*Math.PI*(j/(double)BEAM_SEGMENTS) : 2D*Math.PI*((j+1)/(double)BEAM_SEGMENTS);
					}
					if(newVertex[4] == 0.0){
						vertexOffset.set(0, 0, 0);
					}else{
						vertexOffset.x = beamDef.beamDiameter/2F*Math.cos(currentAngleRad);
						vertexOffset.y = beamDef.beamDiameter/2F*Math.sin(currentAngleRad);
						vertexOffset.z = beamDef.beamLength;
					}
					vertexOffset.rotateFine(rotation).add(centerOffset);
					newVertex[0] = (float) vertexOffset.x;
					newVertex[1] = (float) vertexOffset.y;
					newVertex[2] = (float) vertexOffset.z;
					
					//Don't care about normals for beam rendering as it's a blending face, so we just set them to 0.
					newVertex[5] = 0F;
					newVertex[6] = 0F;
					newVertex[7] = 0F;
					
					//Set the actual vertex.
					beamObject[i*2*BEAM_SEGMENTS*3 + (j + BEAM_SEGMENTS)*3 + k] = newVertex;
				}
			}
		}
		return beamObject;
	}
	
	private static <TreadEntity extends AEntityC_Definable<?>> List<Double[]> generateTreads(TreadEntity entityTreadAttachedTo, String treadPathModel, Map<Float, List<Double[]>> treadPointsMap, PartGroundDevice tread){
		//If we don't have the deltas, calculate them based on the points of the rollers on the model.			
		//Search through rotatable parts on the model and grab the rollers.
		Map<Integer, RenderableTreadRoller<TreadEntity>> parsedRollers = new HashMap<Integer, RenderableTreadRoller<TreadEntity>>();
		for(RenderableModelObject<?> modelObject : entityTreadAttachedTo.getRenderer().objectLists.get(treadPathModel)){
			if(modelObject instanceof RenderableTreadRoller){
				@SuppressWarnings("unchecked")
				RenderableTreadRoller<TreadEntity> treadObject = (RenderableTreadRoller<TreadEntity>) modelObject;
				if(!treadObject.isLeft){
					parsedRollers.put(treadObject.rollerNumber, treadObject);
				}
			}
		}
		
		//Now that we have all the rollers, we can start calculating points.
		//First calculate the endpoints on the rollers by calling the calculation method.
		//We also transfer the rollers to an ordered array for convenience later.
		List<RenderableTreadRoller<TreadEntity>> rollers = new ArrayList<RenderableTreadRoller<TreadEntity>>();
		for(int i=0; i<parsedRollers.size(); ++ i){
			if(!parsedRollers.containsKey(i)){
				throw new IndexOutOfBoundsException("Attempted to render roller_" + i + " on " + entityTreadAttachedTo.definition.packID + ":" + entityTreadAttachedTo.definition.systemName + ", but it was not found.  Did you not make it in the OBJ model?");
			}
			if(i < parsedRollers.size() - 1){
				parsedRollers.get(i).calculateEndpoints(parsedRollers.get(i + 1));
			}else{
				parsedRollers.get(i).calculateEndpoints(parsedRollers.get(0));
			}
			rollers.add(parsedRollers.get(i));
		}
		
		//We need to ensure the endpoints are all angle-aligned.
		//It's possible to have a start angle of -181 and end angle of
		//181, which is really just 2 degress of angle (179-181).
		//To do this, we set the end angle of roller 0 and start
		//angle of roller 1 to be around 180, or downward-facing.
		//From there, we add angles to align things.
		//At the end, we should have an end angle of 540, or 180 + 360.
		rollers.get(0).endAngle = 180;
		for(int i=1; i<rollers.size(); ++i){
			RenderableTreadRoller<?> roller = rollers.get(i);
			roller.startAngle = rollers.get(i - 1).endAngle;
			//End angle should be 0-360 greater than start angle, or within
			//30 degrees less, as is the case for concave rollers. 
			while(roller.endAngle < roller.startAngle - 30){
				roller.endAngle += 360;
			}
			while(roller.endAngle > roller.startAngle + 360){
				roller.endAngle += 360;
			}
		}
		//Set the end angle of the last roller, or start angle of the first roller, manually.
		//Need to get it between the value of 360 + 0-180 as that's where we will connect.
		while(rollers.get(0).startAngle < 0){
			rollers.get(0).startAngle += 360;
		}
		if(rollers.get(0).startAngle > 180){
			rollers.get(0).startAngle -= 360;
		}
		rollers.get(0).startAngle += 360;
		rollers.get(rollers.size() - 1).endAngle = rollers.get(0).startAngle;
		
		
		//Now that the endpoints are set, we can calculate the path.
		//Do this by following the start and end points at small increments.
		//First calculate the total path length, and determine the optimum spacing.
		//This is the closest value to the definition's tread spacing.
		double totalPathLength = 0;
		for(int i=0; i<rollers.size(); ++i){
			//Get roller and add roller path contribution.
			RenderableTreadRoller<?> roller = rollers.get(i);
			totalPathLength += 2*Math.PI*roller.radius*Math.abs(roller.endAngle - (i == 0 ? roller.startAngle - 360 : roller.startAngle))/360D;
			
			//Get next roller and add distance path contribution.
			//For points that start and end at an angle of around 0 (top of rollers) we add droop.
			//This is a hyperbolic function, so we need to calculate the integral value to account for the path.
			RenderableTreadRoller<?> nextRoller = i == rollers.size() - 1 ? rollers.get(0) : rollers.get(i + 1);
			double straightPathLength = Math.hypot(nextRoller.startY - roller.endY, nextRoller.startZ - roller.endZ);
			if(tread.placementDefinition.treadDroopConstant > 0 && (roller.endAngle%360 < 10 || roller.endAngle%360 > 350) && (nextRoller.startAngle%360 < 10 || nextRoller.startAngle%360 > 350)){
				totalPathLength += 2D*tread.placementDefinition.treadDroopConstant*Math.sinh(straightPathLength/2D/tread.placementDefinition.treadDroopConstant);
			}else{
				totalPathLength += straightPathLength;
			}
		}
		
		double deltaDist = tread.definition.ground.spacing + (totalPathLength%tread.definition.ground.spacing)/(totalPathLength/tread.definition.ground.spacing);
		double leftoverPathLength = 0;
		double yPoint = 0;
		double zPoint = 0; 
		List<Double[]> points = new ArrayList<Double[]>();
		for(int i=0; i<rollers.size(); ++i){
			RenderableTreadRoller<?> roller = rollers.get(i);
			//Follow the curve of the roller from the start and end point.
			//Do this until we don't have enough roller path left to make a point.
			//If we have any remaining path from a prior operation, we
			//need to offset our first point on the roller path to account for it.
			//It can very well be that this remainder will be more than the path length
			//of the roller.  If so, we just skip the roller entirely.
			//For the first roller we need to do some special math, as the angles will be inverted
			//For start and end due to the tread making a full 360 path.				
			double rollerPathLength = 2*Math.PI*roller.radius*Math.abs(roller.endAngle - (i == 0 ? roller.startAngle - 360 : roller.startAngle))/360D;
			double currentAngle = roller.startAngle;
			
			//Add the first point here, and add more as we follow the path.
			if(i == 0){
				yPoint = roller.centerPoint.y + roller.radius*Math.cos(Math.toRadians(currentAngle));
				zPoint = roller.centerPoint.z + roller.radius*Math.sin(Math.toRadians(currentAngle));
				points.add(new Double[]{yPoint, zPoint, currentAngle + 180});
			}
			
			//If we have any leftover straight path, account for it here to keep spacing consistent.
			//We will need to interpolate the point that the straight path would have gone to, but
			//take our rotation angle into account.  Only do this if we have enough of a path to do so.
			//If not, we should just skip this roller as we can't put any points on it.
			if(deltaDist - leftoverPathLength < rollerPathLength){
				if(leftoverPathLength > 0){
					//Make a new point that's along a line from the last point and the start of this roller.
					//Then increment currentAngle to account for the new point made.
					//Add an angle relative to the point on the roller.
					Double[] lastPoint = points.get(points.size() - 1);
					yPoint = roller.centerPoint.y + roller.radius*Math.cos(Math.toRadians(currentAngle));
					zPoint = roller.centerPoint.z + roller.radius*Math.sin(Math.toRadians(currentAngle));
					double pointDist = Math.hypot(yPoint - lastPoint[0], zPoint - lastPoint[1]);
					double normalizedY = (yPoint - lastPoint[0])/pointDist;
					double normalizedZ = (zPoint - lastPoint[1])/pointDist;
					double rollerAngleSpan = 360D*((deltaDist - leftoverPathLength)/roller.circumference);
					
					points.add(new Double[]{lastPoint[0] + deltaDist*normalizedY, lastPoint[1] + deltaDist*normalizedZ, lastPoint[2] + rollerAngleSpan});
					currentAngle += rollerAngleSpan;
					rollerPathLength -= (deltaDist - leftoverPathLength);
					leftoverPathLength = 0;
				}
				
				while(rollerPathLength > deltaDist){
					//Go to and add the next point on the roller path.
					rollerPathLength -= deltaDist;
					currentAngle += 360D*(deltaDist/roller.circumference);
					yPoint = roller.centerPoint.y + roller.radius*Math.cos(Math.toRadians(currentAngle));
					zPoint = roller.centerPoint.z + roller.radius*Math.sin(Math.toRadians(currentAngle));
					points.add(new Double[]{yPoint, zPoint, currentAngle + 180});
				}
				
				//Done following roller.  Set angle to end angle.
				currentAngle = roller.endAngle;
			}
			
			//If we have any leftover roller path, account for it here to keep spacing consistent.
			//We may also have leftover straight path length if we didn't do anything on a roller.
			//If we have roller length, make sure to offset it to account for the curvature of the roller.
			//If we don't do this, the line won't start at the end of the prior roller.
			//If we are on the last roller, we need to get the first roller to complete the loop.
			//For points that start and end at an angle of around 0 (top of rollers) we add droop.
			//This is a hyperbolic function, so we need to calculate the integral value to account for the path,
			//as well as model the function for the actual points.  This requires formula-driven points rather than normalization.
			RenderableTreadRoller<?> nextRoller = i == rollers.size() - 1 ? rollers.get(0) : rollers.get(i + 1);
			double straightPathLength = Math.hypot(nextRoller.startY - roller.endY, nextRoller.startZ - roller.endZ);
			double extraPathLength = rollerPathLength + leftoverPathLength;
			double normalizedY = (nextRoller.startY - roller.endY)/straightPathLength;
			double normalizedZ = (nextRoller.startZ - roller.endZ)/straightPathLength;
			if(tread.placementDefinition.treadDroopConstant > 0 && (roller.endAngle%360 < 10 || roller.endAngle%360 > 350) && (nextRoller.startAngle%360 < 10 || nextRoller.startAngle%360 > 350)){
				double hyperbolicPathLength = 2D*tread.placementDefinition.treadDroopConstant*Math.sinh(straightPathLength/2D/tread.placementDefinition.treadDroopConstant);
				double hyperbolicFunctionStep = deltaDist*straightPathLength/hyperbolicPathLength;
				double hyperbolicPathMaxY = tread.placementDefinition.treadDroopConstant*Math.cosh((-straightPathLength/2D)/tread.placementDefinition.treadDroopConstant);
				double hyperbolicFunctionCurrent = 0;
				while(straightPathLength + extraPathLength - hyperbolicFunctionCurrent > hyperbolicFunctionStep){
					//Go to and add the next point on the hyperbolic path.
					if(extraPathLength > 0){
						hyperbolicFunctionCurrent += extraPathLength*hyperbolicFunctionStep;
						extraPathLength = 0;
					}else{
						hyperbolicFunctionCurrent += hyperbolicFunctionStep;
					}
					yPoint = roller.endY + normalizedY*hyperbolicFunctionCurrent + tread.placementDefinition.treadDroopConstant*Math.cosh((hyperbolicFunctionCurrent - straightPathLength/2D)/tread.placementDefinition.treadDroopConstant) - hyperbolicPathMaxY;
					zPoint = roller.endZ + normalizedZ*hyperbolicFunctionCurrent;
					points.add(new Double[]{yPoint, zPoint, roller.endAngle + 180 - Math.toDegrees(Math.asin((hyperbolicFunctionCurrent - straightPathLength/2D)/tread.placementDefinition.treadDroopConstant))});
				}
				leftoverPathLength = (straightPathLength - hyperbolicFunctionCurrent)/(straightPathLength/hyperbolicPathLength);
			}else{
				while(straightPathLength + extraPathLength > deltaDist){
					//Go to and add the next point on the straight path.
					if(extraPathLength > 0){
						yPoint = roller.endY + normalizedY*(deltaDist - extraPathLength);
						zPoint = roller.endZ + normalizedZ*(deltaDist - extraPathLength);
						straightPathLength -= (deltaDist - extraPathLength);
						extraPathLength = 0;
					}else{
						yPoint += normalizedY*deltaDist;
						zPoint += normalizedZ*deltaDist;
						straightPathLength -= deltaDist;
					}
					points.add(new Double[]{yPoint, zPoint, roller.endAngle + 180});
				}
				leftoverPathLength = straightPathLength;
			}
		}
		return points;
	}
	
	private static void normalizeUVs(float[][] parsedObject){
		for(int i=0; i<parsedObject.length; ++i){
			if(parsedObject.length > 3 && i%6 >= 3){
				//Second-half of a quad.
				switch(i%6){
					case(3): parsedObject[i][3] = 0.0F; parsedObject[i][4] = 0.0F; break;
					case(4): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 1.0F; break;
					case(5): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 0.0F; break;
				}
			}else{
				//Normal tri or first half of quad using tri mapping.
				switch(i%6){
					case(0): parsedObject[i][3] = 0.0F; parsedObject[i][4] = 0.0F; break;
					case(1): parsedObject[i][3] = 0.0F; parsedObject[i][4] = 1.0F; break;
					case(2): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 1.0F; break;
					
					case(3): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 1.0F; break;
					case(4): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 0.0F; break;
					case(5): parsedObject[i][3] = 0.0F; parsedObject[i][4] = 0.0F; break;
				}
			}
		}
	}
}
