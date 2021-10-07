package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.components.ARenderEntity;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;
import minecrafttransportsimulator.sound.InterfaceSound;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.item.ItemStack;

/**Base class for entities that are defined via JSON definitions and can be modeled in 3D.
 * This level adds various method for said definitions, which include rendering functions. 
 * 
 * @author don_bruce
 */
public abstract class AEntityC_Definable<JSONDefinition extends AJSONMultiModelProvider> extends AEntityB_Existing{
	/**Map of created entities that can be rendered in the world, including those without {@link #lookupID}s.**/
	private static final Map<WrapperWorld, LinkedHashSet<AEntityC_Definable<?>>> renderableEntities = new HashMap<WrapperWorld, LinkedHashSet<AEntityC_Definable<?>>>();
	
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
	
	/**Set of variables that are "on" for this entity.  Used for animations.**/
	public final Set<String> variablesOn = new HashSet<String>();
	
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
	public AEntityC_Definable(WrapperWorld world, WrapperNBT data){
		super(world, data);
		this.subName = data.getString("subName");
		AItemSubTyped<JSONDefinition> item = PackParserSystem.getItem(data.getString("packID"), data.getString("systemName"), subName);
		this.definition = item != null ? item.definition : generateDefaultDefinition();
		
		//Load text.
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(int i=0; i<definition.rendering.textObjects.size(); ++i){
				text.put(definition.rendering.textObjects.get(i), data.getString("textLine" + i));
			}
		}
		
		//Load variables.
		this.variablesOn.addAll(data.getStrings("variablesOn"));
		
		if(definition.rendering != null && definition.rendering.constants != null){
			variablesOn.addAll(definition.rendering.constants);
		}
	}
	
	/**Constructor for un-synced entities.  Allows for specification of position/motion/angles.**/
	public AEntityC_Definable(WrapperWorld world, Point3d position, Point3d motion, Point3d angles, AItemSubTyped<JSONDefinition> creatingItem){
		super(world, position, motion, angles);
		this.subName = creatingItem.subName;
		this.definition = creatingItem.definition;
		
		//Add constants.
		if(definition.rendering != null && definition.rendering.constants != null){
			variablesOn.addAll(definition.rendering.constants);
		}
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			world.beginProfiling("EntityC_Level", true);
			if(!animationsInitialized){
				initializeDefinition();
				animationsInitialized = true;
			}
			world.endProfiling();
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Call to get all renderable entities from the world.  This includes
	 * both tracked and un-tracked entities.  This list may be null on the
	 * first frame before any entities have been spawned, and entities
	 * may be removed from this list at any time, so watch out for CMEs!
	 * Note that this listing is a linked hash set, so iteration will be
	 * in the same order entities were added.
	 * 
	 */
	public static LinkedHashSet<AEntityC_Definable<?>> getRenderableEntities(WrapperWorld world){
		return renderableEntities.get(world);
	}
	
	/**
	 * Call this if you need to remove all entities from the world.  Used mainly when
	 * a world is un-loaded because no players are in it anymore.
	 */
	public static void removaAllEntities(WrapperWorld world){
		LinkedHashSet<AEntityC_Definable<?>> existingEntities = renderableEntities.get(world);
		if(existingEntities != null){
			//Need to copy the entities so we don't CME the map keys.
			LinkedHashSet<AEntityA_Base> entities = new LinkedHashSet<AEntityA_Base>();
			entities.addAll(existingEntities);
			for(AEntityA_Base entity : entities){
				entity.remove();
			}
			renderableEntities.remove(world);
		}
	}
	
	/**
	 *  Called the first update tick after this entity is first constructed, and when the definition on it is reset via hotloading.
	 *  This should create (and reset) all JSON clocks and other static objects that depend on the definition. 
	 */
	protected void initializeDefinition(){
		//Add us to the entity rendering list.
		LinkedHashSet<AEntityC_Definable<?>> worldEntities = renderableEntities.get(world);
		if(worldEntities == null){
			worldEntities = new LinkedHashSet<AEntityC_Definable<?>>();
			renderableEntities.put(world, worldEntities);
		}
		worldEntities.add(this);
		
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
	}
	
	@Override
	public void remove(){
		if(isValid){
			super.remove();
			//Need to check for null, as this key may not exist if we were an entity spawned in a world but never ticked.
			LinkedHashSet<AEntityC_Definable<?>> entities = renderableEntities.get(world);
			if(entities != null){
				renderableEntities.get(world).remove(this);
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
	public void addDropsToList(List<ItemStack> drops){
		AItemPack<JSONDefinition> packItem = getItem();
		if(packItem != null){
			ItemStack droppedStack = getItem().getNewStack();
			droppedStack.setTagCompound(save(new WrapperNBT()).tag);
			drops.add(droppedStack);
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
   	 *  Returns true if this entity is lit up, and text should be rendered lit.
   	 *  Note that what text is lit is dependent on the text's definition, so just
   	 *  because text could be lit, does not mean it will be lit if the pack
   	 *  author doesn't want it to be.
   	 */
    public boolean renderTextLit(){
    	return true;
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
			textEntry.setValue(textLines.get(linesChecked));
			++linesChecked;
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
						InterfaceRender.spawnParticle(new EntityParticle(this, particleDef));
					}
				}else{
					InterfaceRender.spawnParticle(new EntityParticle(this, particleDef));
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
	 *  Returns the raw value for the passed-in variable.  If the variable is not present, NaM
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
		if(variablesOn.contains(variable)){
			return 1;
		}
		
		//Didn't find a variable.  Return NaN.
		return Double.NaN;
	}
	
	/**
	 *  Returns the value for the passed-in variable, subject to the clamping, and duration/delay requested in the 
	 *  animation definition.  The passed-in offset is used to allow for stacking animations, and should be 0 if 
	 *  this functionality is not required.  Note that the animation offset is applied AFTER the scaling performed by
	 *  the scale parameter as only the variable value should be scaled, not the offset..
	 */
	public final double getAnimatedVariableValue(DurationDelayClock clock, double scale, double offset, float partialTicks){
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
			return clampAndScale(value, clock.animation, scale, offset);
		}else{
			return clampAndScale(clock.getFactoredState(this, value, partialTicks), clock.animation, scale, offset);
		}
	}
	
	/**
	 *  Short-hand version of {@link #getAnimatedVariableValue(DurationDelayClock, double, double, float)}
	 *  with an offset of 0.0.
	 */
	public final double getAnimatedVariableValue(DurationDelayClock clock, double scale, float partialTicks){
		return getAnimatedVariableValue(clock, scale, 0.0, partialTicks);
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
	private static double clampAndScale(double value, JSONAnimationDefinition animation, double scale, double offset){
		if(animation.axis != null){
			value = (animation.absolute ? Math.abs(value) : value)*scale + animation.offset + offset;
			if(animation.clampMin != 0 && value < animation.clampMin){
				value = animation.clampMin;
			}else if(animation.clampMax != 0 && value > animation.clampMax){
				value = animation.clampMax;
			}
			return value;
		}else{
			return (animation.absolute ? Math.abs(value) : value)*scale + animation.offset;
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
	 *  Gets the renderer for this entity.  No actual rendering should be done in this method, 
	 *  as doing so could result in classes being imported during object instantiation on the server 
	 *  for graphics libraries that do not exist.  Instead, generate a class that does this and call it.
	 *  This method is assured to be only called on clients, so you can just do the construction of the
	 *  renderer in this method and pass it back as the return.
	 */
	public abstract <RendererInstance extends ARenderEntity<AnimationEntity>, AnimationEntity extends AEntityC_Definable<?>> RendererInstance getRenderer();
    
    @Override
    public void updateSounds(float partialTicks){
    	super.updateSounds(partialTicks);
    	//Check all sound defs and update the existing sounds accordingly.
    	for(JSONSound soundDef : allSoundDefs){
    		if(soundDef.canPlayOnPartialTicks ^ partialTicks == 0){
	    		//Check if the sound should be playing before we try to update state.
	    		AEntityD_Interactable<?> entityRiding = InterfaceClient.getClientPlayer().getEntityRiding();
	    		boolean playerRidingEntity = this.equals(entityRiding) || (this instanceof APart && ((APart) this).entityOn.equals(entityRiding));
	    		boolean shouldSoundPlay = playerRidingEntity && InterfaceClient.inFirstPerson() && !CameraSystem.areCustomCamerasActive() ? !soundDef.isExterior : !soundDef.isInterior;
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
							if(!playerRidingEntity && sound.radio == null && entityRiding instanceof EntityVehicleF_Physics && !((EntityVehicleF_Physics) entityRiding).definition.motorized.hasOpenTop && InterfaceClient.inFirstPerson() && !CameraSystem.areCustomCamerasActive()){
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
		data.setStrings("variablesOn", variablesOn);
		return data;
	}
}
