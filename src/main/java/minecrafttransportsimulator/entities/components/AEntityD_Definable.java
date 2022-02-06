package minecrafttransportsimulator.entities.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityParticle;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONCameraObject;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.jsondefs.JSONParticle;
import minecrafttransportsimulator.jsondefs.JSONSound;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceSound;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Base class for entities that are defined via JSON definitions and can be modeled in 3D.
 * This level adds various method for said definitions, which include rendering functions. 
 * 
 * @author don_bruce
 */
public abstract class AEntityD_Definable<JSONDefinition extends AJSONMultiModelProvider> extends AEntityC_Renderable{	
	/**The pack definition for this entity.  May contain extra sections if the super-classes
	 * have them in their respective JSONs.
	 */
	public final JSONDefinition definition;

	/**The current subName for this entity.  Used to select which definition represents this entity.*/
	public String subName;
	
	/**Variable for saving animation initialized state.  Is set true on the first tick, but may be set false afterwards to re-initialize animations.*/
	public boolean animationsInitialized;
	
	/**Map containing text lines for saved text provided by this entity.**/
	public final LinkedHashMap<JSONText, String> text = new LinkedHashMap<JSONText, String>();
	
	/**Map of variables.  These are generic and can be interfaced with in the JSON.  Some names are hard-coded to specific variables.Used for animations/physics.**/
	protected final Map<String, Double> variables = new HashMap<String, Double>();
	
	private final List<JSONSound> allSoundDefs = new ArrayList<JSONSound>();
	private final Map<JSONSound, List<DurationDelayClock>> soundActiveClocks = new HashMap<JSONSound, List<DurationDelayClock>>();
	private final Map<JSONSound, List<DurationDelayClock>> soundVolumeClocks = new HashMap<JSONSound, List<DurationDelayClock>>();
	private final Map<JSONSound, List<DurationDelayClock>> soundPitchClocks = new HashMap<JSONSound, List<DurationDelayClock>>();
	private final Map<JSONLight, List<DurationDelayClock>> lightBrightnessClocks = new HashMap<JSONLight, List<DurationDelayClock>>();
	private final Map<JSONParticle, List<DurationDelayClock>> particleActiveClocks = new HashMap<JSONParticle, List<DurationDelayClock>>();
	private final Map<JSONParticle, Long> lastTickParticleSpawned = new HashMap<JSONParticle, Long>();
	
	/**Maps animations to their respective clocks.  Used for anything that has an animation block.**/
	public final Map<JSONAnimationDefinition, DurationDelayClock> animationClocks = new HashMap<JSONAnimationDefinition, DurationDelayClock>();
	
	/**Maps animated (model) object names to their definitions.  This is created from the JSON definition to prevent the need to do loops.**/
	public final Map<String, JSONAnimatedObject> animatedObjectDefinitions = new HashMap<String, JSONAnimatedObject>();
	
	/**Maps light definitions to their current brightness.  This is updated every frame prior to rendering.**/
	public final Map<JSONLight, Float> lightBrightnessValues = new HashMap<JSONLight, Float>();
	
	/**Maps light definitions to their current color.  This is updated every frame prior to rendering.**/
	public final Map<JSONLight, ColorRGB> lightColorValues = new HashMap<JSONLight, ColorRGB>();
	
	/**Maps light (model) object names to their definitions.  This is created from the JSON definition to prevent the need to do loops.**/
	public final Map<String, JSONLight> lightObjectDefinitions = new HashMap<String, JSONLight>();
	
	/**Constructor for synced entities**/
	public AEntityD_Definable(WrapperWorld world, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, placingPlayer, data);
		this.subName = data.getString("subName");
		AItemSubTyped<JSONDefinition> item = PackParserSystem.getItem(data.getString("packID"), data.getString("systemName"), subName);
		this.definition = item != null ? item.definition : generateDefaultDefinition();
		
		//Load text.
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(int i=0; i<definition.rendering.textObjects.size(); ++i){
				JSONText textDef = definition.rendering.textObjects.get(i);
				text.put(textDef, newlyCreated ? textDef.defaultText : data.getString("textLine" + i));
			}
		}
		
		//Load variables.
		for(String variableName : data.getStrings("variables")){
			variables.put(variableName, data.getDouble(variableName));
		}
		if(newlyCreated && definition.rendering != null && definition.rendering.initialVariables != null){
			for(String variable : definition.rendering.initialVariables){
				variables.put(variable, 1D);
			}
		}
		if(definition.rendering != null && definition.rendering.constants != null){
			for(String variable : definition.rendering.constants){
				variables.put(variable, 1D);
			}
		}
	}
	
	/**Constructor for un-synced entities.  Allows for specification of position/motion/angles.**/
	public AEntityD_Definable(WrapperWorld world, Point3d position, Point3d motion, Point3d angles, AItemSubTyped<JSONDefinition> creatingItem){
		super(world, position, motion, angles);
		this.subName = creatingItem.subName;
		this.definition = creatingItem.definition;
		
		//Add constants.
		if(definition.rendering != null && definition.rendering.constants != null){
			for(String variable : definition.rendering.constants){
				variables.put(variable, 1D);
			}
		}
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			world.beginProfiling("EntityD_Level", true);
			if(!animationsInitialized){
				initializeDefinition();
				animationsInitialized = true;
			}
			
			//Update value-based text.  Only do this on clients as servers won't render this text.
			if(world.isClient() && !text.isEmpty()){
				for(Entry<JSONText, String> textEntry : text.entrySet()){
					JSONText textDef = textEntry.getKey();
					if(textDef.variableName != null){
						textEntry.setValue(getAnimatedTextVariableValue(textDef, 0));
					}
				}
			}
			world.endProfiling();
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 *  Called the first update tick after this entity is first constructed, and when the definition on it is reset via hotloading.
	 *  This should create (and reset) all JSON clocks and other static objects that depend on the definition. 
	 */
	protected void initializeDefinition(){
		allSoundDefs.clear();
		soundActiveClocks.clear();
		soundVolumeClocks.clear();
		soundPitchClocks.clear();
		if(definition.rendering != null && definition.rendering.sounds != null){
			for(JSONSound soundDef : definition.rendering.sounds){
				allSoundDefs.add(soundDef);
				
				List<DurationDelayClock> activeClocks = new ArrayList<DurationDelayClock>();
				if(soundDef.activeAnimations !=  null){
					for(JSONAnimationDefinition animation : soundDef.activeAnimations){
						activeClocks.add(new DurationDelayClock(animation));
					}
				}
				soundActiveClocks.put(soundDef, activeClocks);
				
				List<DurationDelayClock> volumeClocks = new ArrayList<DurationDelayClock>();
				if(soundDef.volumeAnimations !=  null){
					for(JSONAnimationDefinition animation : soundDef.volumeAnimations){
						volumeClocks.add(new DurationDelayClock(animation));
					}
				}
				soundVolumeClocks.put(soundDef, volumeClocks);
				
				List<DurationDelayClock> pitchClocks = new ArrayList<DurationDelayClock>();
				if(soundDef.pitchAnimations != null){
					for(JSONAnimationDefinition animation : soundDef.pitchAnimations){
						pitchClocks.add(new DurationDelayClock(animation));
					}
				}
				soundPitchClocks.put(soundDef, pitchClocks);
			}
		}
		
		lightBrightnessClocks.clear();
		lightBrightnessValues.clear();
		lightColorValues.clear();
		lightObjectDefinitions.clear();
		if(definition.rendering != null && definition.rendering.lightObjects != null){
			for(JSONLight lightDef : definition.rendering.lightObjects){
				lightObjectDefinitions.put(lightDef.objectName, lightDef);
				List<DurationDelayClock> lightClocks = new ArrayList<DurationDelayClock>();
				if(lightDef.brightnessAnimations !=  null){
					for(JSONAnimationDefinition animation : lightDef.brightnessAnimations){
						lightClocks.add(new DurationDelayClock(animation));
					}
				}
				lightBrightnessClocks.put(lightDef, lightClocks);
				lightBrightnessValues.put(lightDef, 0F);
				lightColorValues.put(lightDef, new ColorRGB());
			}
		}
		
		particleActiveClocks.clear();
		if(definition.rendering != null && definition.rendering.particles != null){
			for(JSONParticle particleDef : definition.rendering.particles){
				List<DurationDelayClock> activeClocks = new ArrayList<DurationDelayClock>();
				if(particleDef.activeAnimations !=  null){
					for(JSONAnimationDefinition animation : particleDef.activeAnimations){
						activeClocks.add(new DurationDelayClock(animation));
					}
				}
				particleActiveClocks.put(particleDef, activeClocks);
				lastTickParticleSpawned.put(particleDef, ticksExisted);
			}
		}
		
		animationClocks.clear();
		animatedObjectDefinitions.clear();
		if(definition.rendering != null){
			if(definition.rendering.animatedObjects != null){
				for(JSONAnimatedObject animatedDef : definition.rendering.animatedObjects){
					animatedObjectDefinitions.put(animatedDef.objectName, animatedDef);
					if(animatedDef.animations != null){
						for(JSONAnimationDefinition animation : animatedDef.animations){
							animationClocks.put(animation, new DurationDelayClock(animation));
						}
					}
				}
			}
			if(definition.rendering.cameraObjects != null){
				for(JSONCameraObject cameraDef : definition.rendering.cameraObjects){
					if(cameraDef.animations != null){
						for(JSONAnimationDefinition animation : cameraDef.animations){
							animationClocks.put(animation, new DurationDelayClock(animation));
						}
					}
				}
			}
		}
		
		//Store text data if we have it, then reset it.
		List<String> oldTextValues = new ArrayList<String>();
		oldTextValues.addAll(text.values());
		text.clear();
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(int i=0; i<definition.rendering.textObjects.size(); ++i){
				if(i < oldTextValues.size()){
					text.put(definition.rendering.textObjects.get(i), oldTextValues.get(i));
				}else{
					text.put(definition.rendering.textObjects.get(i), definition.rendering.textObjects.get(i).defaultText);
				}
			}
		}
	}
	
	/**
	 *  Returns the current item for this entity.
	 */
	public <ItemInstance extends AItemPack<JSONDefinition>> ItemInstance getItem(){
		return PackParserSystem.getItem(definition.packID, definition.systemName, subName);
	}
	
	/**
	 *  Populates the passed-in list with item stacks that will drop when this entity is broken.
	 *  This is different than what is used for middle-clicking, as that will
	 *  return a stack that can re-create this entity, whereas drops may or may not allow for this.
	 *  An example is a vehicle that is broken in a crash versus picked up via a wrench.
	 */
	public void addDropsToList(List<WrapperItemStack> drops){
		AItemPack<JSONDefinition> packItem = getItem();
		if(packItem != null){
			drops.add(packItem.getNewStack(save(new WrapperNBT())));
		}
	}
	
	/**
	 *  Generates the default definition for this entity. Used if the item can't be found.
	 *  This allows for internally-definable entities.
	 */
	public JSONDefinition generateDefaultDefinition(){
		throw new IllegalArgumentException("Was asked to auto-generate a definition on an entity with one not defined.  This is NOT allowed.  The entity must be missing its item.  Perhaps a pack was removed with this entity still in the world?");
	}
	
	/**
	 *  Returns the texture that should be bound to this entity for the passed-in object from the model.
	 *  This may change between render passes, but only ONE texture may be used for any given object render
	 *  operation!  By default this returns the JSON-defined texture, though the model parser may override this.
	 */
	public String getTexture(){
		return definition.getTextureLocation(subName);
	}
	
    /**
   	 *  Returns true if this entity is lit up, and text should be rendered lit.
   	 *  Note that what text is lit is dependent on the text's definition, so just
   	 *  because text could be lit, does not mean it will be lit if the pack
   	 *  author doesn't want it to be.
   	 */
    public boolean renderTextLit(){
    	return ConfigSystem.configObject.clientRendering.brightLights.value;
    }
    
    /**
   	 *  Returns the color for the text on this entity.  This takes into account the passed-in index.
   	 *  If a color exists at the index, it is returned.  If not, then the passed-in color is returned.
   	 */
    public ColorRGB getTextColor(int index, ColorRGB defaultColor){
    	if(index != 0){
	    	for(JSONSubDefinition subDefinition : definition.definitions){
				if(subDefinition.subName.equals(subName)){
					if(subDefinition.secondaryTextColors != null && subDefinition.secondaryTextColors.size() >= index){
						return subDefinition.secondaryTextColors.get(index-1);
					}else{
						return defaultColor;
					}
				}
			}
	    	throw new IllegalArgumentException("Tried to get the definition for an object of subName:" + subName + ".  But that isn't a valid subName for the object:" + definition.packID + ":" + definition.systemName + ".  Report this to the pack author as this is a missing JSON component!");
    	}else{
    		return defaultColor;
    	}
    }
    
    /**
	 *  Called to update the text on this entity.  Normally just sets the text to the passed-in values,
	 *  but may do supplemental logic if desired.
	 */
    public void updateText(List<String> textLines){
    	int linesChecked = 0;
		for(Entry<JSONText, String> textEntry : text.entrySet()){
			textEntry.setValue(textLines.get(linesChecked++));
		}
    }
    
    /**
   	 *  Spawns particles for this entity.  This is called after every render frame, so
   	 *  watch your methods to prevent spam.  Note that this method is not called if the
   	 *  game is paused, as particles are assumed to only be spawned during normal entity
   	 *  updates.
   	 */
    public void spawnParticles(float partialTicks){
    	//Check all particle defs and update the existing particles accordingly.
    	for(Entry<JSONParticle, List<DurationDelayClock>> particleEntry : particleActiveClocks.entrySet()){
    		JSONParticle particleDef = particleEntry.getKey();
    		//Check if the particle should be spawned this tick.
    		boolean shouldParticleSpawn = true;
			boolean anyClockMovedThisUpdate = false;
			if(particleDef.activeAnimations != null){
				boolean inhibitAnimations = false;
				for(DurationDelayClock clock : particleEntry.getValue()){
					switch(clock.animation.animationType){
						case VISIBILITY :{
							//We use the clock here to check if the state of the variable changed, not
							//to clamp the value used in the testing.
							if(!inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, partialTicks);
								if(!anyClockMovedThisUpdate){
									anyClockMovedThisUpdate = clock.movedThisUpdate;
								}
								if(variableValue < clock.animation.clampMin || variableValue > clock.animation.clampMax){
									shouldParticleSpawn = false;
								}
							}
							break;
						}
						case INHIBITOR :{
							if(!inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, partialTicks);
								if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
									inhibitAnimations = true;
								}
							}
							break;
						}
						case ACTIVATOR :{
							if(inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, partialTicks);
								if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
									inhibitAnimations = false;
								}
							}
							break;
						}
						case TRANSLATION :{
							//Do nothing.
							break;
						}
						case ROTATION :{
							//Do nothing.
							break;
						}
						case SCALING :{
							//Do nothing.
							break;
						}
					}
					
					if(!shouldParticleSpawn){
						//Don't need to process any further as we can't spawn.
						break;
					}
				}
			}
			
			//Make the particle spawn if able.
			if(shouldParticleSpawn && (anyClockMovedThisUpdate || (particleDef.spawnEveryTick && ticksExisted > lastTickParticleSpawned.get(particleDef)))){
				lastTickParticleSpawned.put(particleDef, ticksExisted);
				if(particleDef.quantity > 0){
					for(int i=0; i<particleDef.quantity; ++i){
						world.addEntity(new EntityParticle(this, particleDef));
					}
				}else{
					world.addEntity(new EntityParticle(this, particleDef));
				}
			}
    	}
    }
    
    /**
   	 *  Updates the light brightness values contained in {@link #lightBrightnessValues}.  This is done
   	 *  every frame for all light definitions to prevent excess calculations caused by multiple
   	 *  lighting components for the light re-calculating the same value multiple times a frame.
   	 *  An example of this is a light with a bean and flare component. 
   	 */
    public void updateLightBrightness(float partialTicks){
		for(JSONLight lightDef : lightBrightnessClocks.keySet()){
			boolean definedBrightness = false;
			float lightLevel = 0.0F;
			boolean inhibitAnimations = false;
			boolean inhibitLight = false;
			ColorRGB customColor = null;
			
			for(DurationDelayClock clock : lightBrightnessClocks.get(lightDef)){
				switch(clock.animation.animationType){
					case VISIBILITY :{
						if(!inhibitAnimations){
							double variableValue = getAnimatedVariableValue(clock, partialTicks);
							if(variableValue < clock.animation.clampMin || variableValue > clock.animation.clampMax){
								inhibitLight = true;
							}
						}
						break;
					}
					case INHIBITOR :{
						if(!inhibitAnimations){
							double variableValue = getAnimatedVariableValue(clock, partialTicks);
							if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
								inhibitAnimations = true;
							}
						}
						break;
					}
					case ACTIVATOR :{
						if(inhibitAnimations){
							double variableValue = getAnimatedVariableValue(clock, partialTicks);
							if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
								inhibitAnimations = false;
							}
						}
						break;
					}
					case TRANSLATION :{
						if(!inhibitAnimations){
							definedBrightness = true;
							if(clock.animation.axis.x != 0){
								lightLevel *= getAnimatedVariableValue(clock, clock.animation.axis.x, partialTicks);
							}else if(clock.animation.axis.y != 0){
								lightLevel += getAnimatedVariableValue(clock, clock.animation.axis.y, partialTicks);
							}else{
								lightLevel = (float) (getAnimatedVariableValue(clock, clock.animation.axis.z, partialTicks));
							}
						}
						break;
					}
					case ROTATION :{
						if(!inhibitAnimations){
							double colorFactor = getAnimatedVariableValue(clock, 1.0, -clock.animation.offset, partialTicks);
							if(customColor == null){
								customColor = new ColorRGB((float) Math.min(clock.animation.axis.x*colorFactor + clock.animation.offset, 1.0), (float) Math.min(clock.animation.axis.y*colorFactor + clock.animation.offset, 1.0), (float) Math.min(clock.animation.axis.z*colorFactor + clock.animation.offset, 1.0), false);
							}else{
								customColor = new ColorRGB((float) Math.min(clock.animation.axis.x*colorFactor + clock.animation.offset + customColor.red, 1.0), (float) Math.min(clock.animation.axis.y*colorFactor + clock.animation.offset + customColor.green, 1.0), (float) Math.min(clock.animation.axis.z*colorFactor + clock.animation.offset + customColor.blue, 1.0), false);
							}
						}
						break;
					}
					case SCALING :{
						//Do nothing.
						break;
					}
				}
				if(inhibitLight){
					//No need to process further.
					break;
				}
			}
			
			//Set light level.
			if(inhibitLight || lightLevel < 0){
				lightLevel = 0;
			}else if(!definedBrightness || lightLevel > 1){
				lightLevel = 1;
			}
			lightBrightnessValues.put(lightDef, lightLevel);
			
			//Set color level.
			ColorRGB lightColor = lightColorValues.get(lightDef);
			if(customColor != null){
				lightColor.setTo(customColor);
			}else if(lightDef.color != null){
				lightColor.setTo(lightDef.color);
			}else{
				lightColor.setTo(ColorRGB.WHITE);
			}
			lightColorValues.put(lightDef, lightColor);
		}
    }
	
	/**
	 *  Returns the raw value for the passed-in variable.  If the variable is not present, NaN
	 *  should be returned (calling functions need to account for this!).
	 *  This should be extended on all sub-classes for them to provide their own variables.
	 *  For all cases of this, the sub-classed variables should be checked first.  If none are
	 *  found, then the super() method should be called to return those as a default.
	 */
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("tick"): return ticksExisted + partialTicks;
			case("tick_sin"): return Math.sin(Math.toRadians(ticksExisted + partialTicks));
			case("tick_cos"): return Math.cos(Math.toRadians(ticksExisted + partialTicks));
			case("time"): return world.getTime();
			case("rain_strength"): return (int) world.getRainStrength(position);
			case("rain_sin"): {
				int rainStrength = (int) world.getRainStrength(position); 
				return rainStrength > 0 ? Math.sin(rainStrength*Math.toRadians(360*(ticksExisted + partialTicks)/20))/2D + 0.5: 0;
			}
			case("rain_cos"): {
				int rainStrength = (int) world.getRainStrength(position); 
				return rainStrength > 0 ? Math.cos(rainStrength*Math.toRadians(360*(ticksExisted + partialTicks)/20))/2D + 0.5 : 0;
			}	
			case("light_sunlight"): return world.getLightBrightness(position, false);
			case("light_total"): return world.getLightBrightness(position, true);
			case("ground_distance"): return world.getHeight(position);
		}
		
		//Check if this is a cycle variable.
		if(variable.endsWith("_cycle")){
			String[] parsedVariable = variable.split("_");
			int offTime = Integer.valueOf(parsedVariable[0]);
			int onTime = Integer.valueOf(parsedVariable[1]);
			int totalTime = offTime + onTime + Integer.valueOf(parsedVariable[2]);
			long timeInCycle = ticksExisted%totalTime;
			return timeInCycle > offTime && timeInCycle - offTime < onTime ? 1 : 0;
		}
		
		//Check if this is a generic variable.  This contains lights in most cases.
		Double variableValue = variables.get(variable);
		if(variableValue != null){
			return variableValue;
		}
		
		//Didn't find a variable.  Return NaN.
		return Double.NaN;
	}
	
	/**
	 *  Similar to {@link #getRawVariableValue(String, float)}, but returns
	 *  a String for text-based parameters rather than a double.  If no match
	 *  is found, return null.  Otherwise, return the string.
	 */
	public String getRawTextVariableValue(JSONText textDef, float partialTicks){
		return null;
	}
	
	/**
	 *  Returns the value for the passed-in variable, subject to the clamping, and duration/delay requested in the 
	 *  animation definition.  The passed-in offset is used to allow for stacking animations, and should be 0 if 
	 *  this functionality is not required.  Note that the animation offset is applied AFTER the scaling performed by
	 *  the scale parameter as only the variable value should be scaled, not the offset..
	 */
	public final double getAnimatedVariableValue(DurationDelayClock clock, double scaleFactor, double offset, float partialTicks){
		double value;
		if(clock.animation.variable.startsWith("!")){
			value = getRawVariableValue(clock.animation.variable.substring(1), partialTicks);
			value = (value == 0 || Double.isNaN(value)) ? 1 : 0;
		}else{
			value = getRawVariableValue(clock.animation.variable, partialTicks);
			if(Double.isNaN(value)){
				value = 0;
			}
		}
		if(!clock.isUseful){
			return clampAndScale(value, clock.animation, scaleFactor, offset);
		}else{
			return clampAndScale(clock.getFactoredState(this, value, partialTicks), clock.animation, scaleFactor, offset);
		}
	}
	
	/**
	 *  Short-hand version of {@link #getAnimatedVariableValue(DurationDelayClock, double, double, float)}
	 *  with an offset of 0.0.
	 */
	public final double getAnimatedVariableValue(DurationDelayClock clock, double scaleFactor, float partialTicks){
		return getAnimatedVariableValue(clock, scaleFactor, 0.0, partialTicks);
	}
	
	/**
	 *  Short-hand version of {@link #getAnimatedVariableValue(DurationDelayClock, double, double, float)}
	 *  with a scale of 1.0 and offset of 0.0.
	 */
	public final double getAnimatedVariableValue(DurationDelayClock clock, float partialTicks){
		return getAnimatedVariableValue(clock, 1.0, 0.0, partialTicks);
	}
	
	/**
	 *  Helper method to clamp and scale the passed-in variable value based on the passed-in animation, 
	 *  returning it in the proper form.
	 */
	private static double clampAndScale(double value, JSONAnimationDefinition animation, double scaleFactor, double offset){
		if(animation.axis != null){
			value = (animation.absolute ? Math.abs(value) : value)*scaleFactor + animation.offset + offset;
			if(animation.clampMin != 0 && value < animation.clampMin){
				value = animation.clampMin;
			}else if(animation.clampMax != 0 && value > animation.clampMax){
				value = animation.clampMax;
			}
			return value;
		}else{
			return (animation.absolute ? Math.abs(value) : value)*scaleFactor + animation.offset;
		}
	}
	
	/**
	 *  Returns the value for the passed-in variable, subject to the formatting and factoring in the 
	 *  text definition.
	 */
	public final String getAnimatedTextVariableValue(JSONText textDef, float partialTicks){
		//Check text values first, then anmiated values.
		String value = getRawTextVariableValue(textDef, 0);
		if(value == null){
			double numberValue = getRawVariableValue(textDef.variableName, 0);
			if(Double.isNaN(numberValue)){
				numberValue = 0;
			}
			return String.format(textDef.variableFormat, numberValue*textDef.variableFactor);
		}else{
			return String.format(textDef.variableFormat, value);
		}
	}
	
	/**
	 *  Helper method to get the index of the passed-in variable.  Indexes are defined by
	 *  variable names ending in _xx, where xx is a number.  The defined number is assumed
	 *  to be 1-indexed, but the returned number will be 0-indexed.  If the variable doesn't
	 *  define a number, then -1 is returned.
	 */
	public static int getVariableNumber(String variable){
		if(variable.matches("^.*_[0-9]+$")){
			return Integer.parseInt(variable.substring(variable.lastIndexOf('_') + 1)) - 1;
		}else{
			return -1;
		}
	}
	
	/**
	 *  Helper method to toggle a variable for this entity.
	 */
	public void toggleVariable(String variable){
		//Try to remove the variable,this requires only one key-search operation, unlike a containsKey followed by a remove.
		if(variables.remove(variable) == null){
			//No key was in this map prior, so this variable was off, set it on.
			variables.put(variable, 1D);
		}
	}
	
	/**
	 *  Helper method to set a variable for this entity.
	 */
	public void setVariable(String variable, double value){
		if(value == 0){
			//Remove variable from the map so we don't have as many to deal with.
			variables.remove(variable);
		}else{
			variables.put(variable, value);
		}
	}
	
	/**
	 *  Helper method to get get a variable for this entity.
	 */
	public double getVariable(String variable){
		Double value = variables.get(variable);
		if(value == null){
			//Don't add the variable to the map, just return 0 here.
			return 0;
		}else{
			return value;
		}
	}
	
	/**
	 *  Helper method to check if a variable is non-zero.
	 *  This is a bit quicker than getting the value due to auto-boxing off the map.
	 */
	public boolean isVariableActive(String variable){
		return variables.containsKey(variable);
	}
    
    @Override
    public void updateSounds(float partialTicks){
    	super.updateSounds(partialTicks);
    	//Check all sound defs and update the existing sounds accordingly.
    	for(JSONSound soundDef : allSoundDefs){
    		if(soundDef.canPlayOnPartialTicks ^ partialTicks == 0){
	    		//Check if the sound should be playing before we try to update state.
	    		AEntityE_Interactable<?> entityRiding = InterfaceClient.getClientPlayer().getEntityRiding();
	    		boolean playerRidingEntity = this.equals(entityRiding) || (this instanceof APart && ((APart) this).entityOn.equals(entityRiding));
	    		boolean shouldSoundPlay = playerRidingEntity && InterfaceClient.inFirstPerson() && !CameraSystem.runningCustomCameras ? !soundDef.isExterior : !soundDef.isInterior;
				boolean anyClockMovedThisUpdate = false;
				boolean inhibitAnimations = false;
				if(shouldSoundPlay){
					for(DurationDelayClock clock : soundActiveClocks.get(soundDef)){
						switch(clock.animation.animationType){
							case VISIBILITY :{
								//We use the clock here to check if the state of the variable changed, not
								//to clamp the value used in the testing.
								if(!inhibitAnimations){
									double variableValue = getAnimatedVariableValue(clock, partialTicks);
									if(!anyClockMovedThisUpdate){
										anyClockMovedThisUpdate = clock.movedThisUpdate;
									}
									if(variableValue < clock.animation.clampMin || variableValue > clock.animation.clampMax){
										shouldSoundPlay = false;
									}
								}
								break;
							}
							case INHIBITOR :{
								if(!inhibitAnimations){
									double variableValue = getAnimatedVariableValue(clock, partialTicks);
									if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
										inhibitAnimations = true;
									}
								}
								break;
							}
							case ACTIVATOR :{
								if(inhibitAnimations){
									double variableValue = getAnimatedVariableValue(clock, partialTicks);
									if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
										inhibitAnimations = false;
									}
								}
								break;
							}
							case TRANSLATION :{
								//Do nothing.
								break;
							}
							case ROTATION :{
								//Do nothing.
								break;
							}
							case SCALING :{
								//Do nothing.
								break;
							}
						}
						
						if(!shouldSoundPlay){
							//Don't need to process any further as we can't play.
							break;
						}
					}
				}
				
				//If we aren't a looping or repeating sound, check if we had a clock-movement to trigger us.
				//If we didn't, then we shouldn't play, even if all states are true.
				if(!soundDef.looping && !soundDef.forceSound && !anyClockMovedThisUpdate){
					shouldSoundPlay = false;
				}
				
				if(shouldSoundPlay){
					//Sound should play.  If it's not playing, start it.
					boolean isSoundPlaying = false;
					if(!soundDef.forceSound){
						for(SoundInstance sound : sounds){
							if(sound.soundName.equals(soundDef.name)){
								isSoundPlaying = true;
								break;
							}
						}
					}
					if(!isSoundPlaying){
						InterfaceSound.playQuickSound(new SoundInstance(this, soundDef));
					}
				}else{
					if(soundDef.looping){
						//If sound is playing, stop it.
						for(SoundInstance sound : sounds){
							if(sound.soundName.equals(soundDef.name)){
								sound.stopSound = true;
								break;
							}
						}
					}
					
					//Go to the next soundDef.  No need to change properties on sounds that shouldn't play.
					continue;
				}
				
				//Sound should be playing.  If it's part of the sound list, update properties.
				//Sounds may not be in the list if they have just been queued and haven't started yet.
				for(SoundInstance sound : sounds){
					if(sound.soundName.equals(soundDef.name)){
						if(sound != null){
							//Adjust volume.
							boolean definedVolume = false;
							inhibitAnimations = false;
							sound.volume = 0;
							for(DurationDelayClock clock : soundVolumeClocks.get(soundDef)){
								switch(clock.animation.animationType){
									case TRANSLATION :{
										if(!inhibitAnimations){
											definedVolume = true;
											sound.volume += getAnimatedVariableValue(clock, clock.animation.axis.y, partialTicks);
										}
										break;
									}
									case ROTATION :{
										if(!inhibitAnimations){
											definedVolume = true;
											//Parobola is defined with parameter A being x, and H being z.
											double parabolaValue = getAnimatedVariableValue(clock, clock.animation.axis.y, -clock.animation.offset, partialTicks);
											sound.volume += clock.animation.axis.x*Math.pow(parabolaValue - clock.animation.axis.z, 2) + clock.animation.offset;
										}
										break;
									}
									case INHIBITOR :{
										if(!inhibitAnimations){
											double variableValue = getAnimatedVariableValue(clock, partialTicks);
											if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
												inhibitAnimations = true;
											}
										}
										break;
									}
									case ACTIVATOR :{
										if(inhibitAnimations){
											double variableValue = getAnimatedVariableValue(clock, partialTicks);
											if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
												inhibitAnimations = false;
											}
										}
										break;
									}
									case SCALING :{
										//Do nothing.
										break;
									}
									case VISIBILITY :{
										//Do nothing.
										break;
									}
								}
							}
							if(!definedVolume){
								sound.volume = 1;
							}else if(sound.volume < 0){
								sound.volume = 0;
							}
							
							//If the player is in a closed-top vehicle that isn't this one, dampen the sound
							//Unless it's a radio, in which case don't do so.
							if(!playerRidingEntity && sound.radio == null && entityRiding instanceof EntityVehicleF_Physics && !((EntityVehicleF_Physics) entityRiding).definition.motorized.hasOpenTop && InterfaceClient.inFirstPerson() && !CameraSystem.runningCustomCameras){
								sound.volume *= 0.5F;
							}
							
							//Adjust pitch.
							boolean definedPitch = false;
							inhibitAnimations = false;
							sound.pitch = 0;
							for(DurationDelayClock clock : soundPitchClocks.get(soundDef)){
								switch(clock.animation.animationType){
									case TRANSLATION :{
										if(!inhibitAnimations){
											definedPitch = true;
											sound.pitch += getAnimatedVariableValue(clock, clock.animation.axis.y, partialTicks);
										}
										break;
									}
									case ROTATION :{
										if(!inhibitAnimations){
											definedPitch = true;
											//Parobola is defined with parameter A being x, and H being z.
											double parabolaValue = getAnimatedVariableValue(clock, clock.animation.axis.y, -clock.animation.offset, partialTicks);
											sound.pitch += clock.animation.axis.x*Math.pow(parabolaValue - clock.animation.axis.z, 2) + clock.animation.offset;
										}
										break;
									}
									case INHIBITOR :{
										if(!inhibitAnimations){
											double variableValue = getAnimatedVariableValue(clock, partialTicks);
											if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
												inhibitAnimations = true;
											}
										}
										break;
									}
									case ACTIVATOR :{
										if(inhibitAnimations){
											double variableValue = getAnimatedVariableValue(clock, partialTicks);
											if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
												inhibitAnimations = false;
											}
										}
										break;
									}
									case SCALING :{
										//Do nothing.
										break;
									}
									case VISIBILITY :{
										//Do nothing.
										break;
									}
								}
							}
							if(!definedPitch){
								sound.pitch = 1;
							}else if(sound.pitch < 0){
								sound.pitch = 0;
							}
						}						
					}
				}
    		}
    	}
    }
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setString("packID", definition.packID);
		data.setString("systemName", definition.systemName);
		data.setString("subName", subName);
		int lineNumber = 0;
		for(String textLine : text.values()){
			data.setString("textLine" + lineNumber++, textLine);
		}
		data.setStrings("variables", variables.keySet());
		for(String variableName : variables.keySet()){
			data.setDouble(variableName, variables.get(variableName));
		}
		return data;
	}
	
	/**
	 * Indicates that this field is a derived value from
	 * one of the variables in {@link AEntityD_Definable#variables}.
	 * Variables that are derived are parsed from the map every update.
	 * To modify them you will need to update their values in the respective
	 * variable set via 
	 * {@link PacketEntityVariableToggle},
	 * {@link PacketEntityVariableSet},
	 * {@link PacketEntityVariableIncrement} 
	 */
	@Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD})
	public static @interface DerivedValue{}
	
	/**
	 * Indicates that this field is able to be modified via variable modification
	 * by the code in {@link AEntityE_Interactable#updateVariableModifiers()},
	 * This annotation is only for variables that are NOT derived from states
	 * and annotated with {@link DerivedValue}, as those variables can inherently
	 * be modified as they are derived from the variable states. 
	 */
	@Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD})
	public static @interface ModifiableValue{}
	
	/**
	 * Indicates that this field is a modified version of a field annotated with
	 * {@link ModifiableValue}.  This is done to prevent modifying the parsed
	 * definition entry that contains the value, which is why it's stored
	 * in a new variable that gets aligned every tick before updates. 
	 */
	@Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD})
	public static @interface ModifiedValue{}
}
