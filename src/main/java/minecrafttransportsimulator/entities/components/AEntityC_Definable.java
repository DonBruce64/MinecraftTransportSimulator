package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityParticle;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
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
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.sound.InterfaceSound;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Base class for entities that are defined via JSON definitions and can be modeled in 3D.
 * This level adds various method for said definitions, which include rendering functions. 
 * 
 * @author don_bruce
 */
public abstract class AEntityC_Definable<JSONDefinition extends AJSONMultiModelProvider> extends AEntityB_Existing{
	/**Map of created entities that can be rendered in the world, including those without {@link #lookupID}s.**/
	private static final Map<WrapperWorld, Set<AEntityC_Definable<?>>> renderableEntities = new HashMap<WrapperWorld, Set<AEntityC_Definable<?>>>();
	
	/**The pack definition for this entity.  May contain extra sections if the super-classes
	 * have them in their respective JSONs.
	 */
	public final JSONDefinition definition;

	/**The current subName for this entity.  Used to select which definition represents this entity.*/
	public String subName;
	
	/**Map containing text lines for saved text provided by this entity.**/
	public final LinkedHashMap<JSONText, String> text = new LinkedHashMap<JSONText, String>();
	
	/**Set of variables that are "on" for this entity.  Used for animations.**/
	public final Set<String> variablesOn = new HashSet<String>();
	
	private final List<JSONSound> allSoundDefs = new ArrayList<JSONSound>();
	private final LinkedHashMap<JSONSound, LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>> soundActiveClocks = new LinkedHashMap<JSONSound, LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>>();
	private final LinkedHashMap<JSONSound, LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>> soundVolumeClocks = new LinkedHashMap<JSONSound, LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>>();
	private final LinkedHashMap<JSONSound, LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>> soundPitchClocks = new LinkedHashMap<JSONSound, LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>>();
	private final LinkedHashMap<JSONParticle, LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>> particleActiveClocks = new LinkedHashMap<JSONParticle, LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>>();
	private final LinkedHashMap<JSONParticle, Long> lastTickParticleSpawned = new LinkedHashMap<JSONParticle, Long>();
	
	/**Constructor for synced entities**/
	public AEntityC_Definable(WrapperWorld world, WrapperNBT data){
		super(world, data);
		//Set definition and current subName.
		//TODO remove when packs have converted, as we previously used these fields on TEs.
		this.subName = data.getString("subName");
		if(subName.isEmpty()){
			subName = data.getString("currentSubName");
		}
		AItemSubTyped<JSONDefinition> item = PackParserSystem.getItem(data.getString("packID"), data.getString("systemName"), subName);
		if(item != null){
			this.definition = item.definition;
			//this.subName = item.subName;
		}else{
			this.definition = generateDefaultDefinition();
			//this.subName = "";
		}
		
		//Load text.
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(int i=0; i<definition.rendering.textObjects.size(); ++i){
				text.put(definition.rendering.textObjects.get(i), data.getString("textLine" + i));
			}
		}
		
		//Load variables.
		this.variablesOn.addAll(data.getStrings("variablesOn"));
		
		//Make sure the generic light is in the variable set.
		this.variablesOn.add(LightType.GENERICLIGHT.lowercaseName);
		
		//Create all clocks.
		populateMaps();
	}
	
	/**Constructor for un-synced entities.  Allows for specification of position/motion/angles.**/
	public AEntityC_Definable(WrapperWorld world, Point3d position, Point3d motion, Point3d angles, AItemSubTyped<JSONDefinition> creatingItem){
		super(world, position, motion, angles);
		this.subName = creatingItem.subName;
		this.definition = creatingItem.definition;
		
		//Make sure the generic light is in the variable set.
		this.variablesOn.add(LightType.GENERICLIGHT.lowercaseName);
		
		//Create all clocks.
		populateMaps();
	}
	
	/**
	 * Call to get all renderable entities from the world.  This includes
	 * both tracked and un-tracked entities.  This list may be null on the
	 * first frame before any entities have been spawned, and entities
	 * may be removed from this list at any time, so watch out for CMEs!
	 * 
	 */
	public static Collection<AEntityC_Definable<?>> getRenderableEntities(WrapperWorld world){
		return renderableEntities.get(world);
	}
	
	/**
	 * Call this if you need to remove all entities from the world.  Used mainly when
	 * a world is un-loaded because no players are in it anymore.
	 */
	public static void removaAllEntities(WrapperWorld world){
		Collection<AEntityC_Definable<?>> existingEntities = renderableEntities.get(world);
		if(existingEntities != null){
			//Need to copy the entities so we don't CME the map keys.
			Set<AEntityA_Base> entities = new HashSet<AEntityA_Base>();
			entities.addAll(existingEntities);
			for(AEntityA_Base entity : entities){
				entity.remove();
			}
			renderableEntities.remove(world);
		}
	}
	
	/**
	 *  Helper method for populating rendering, sound, and particle maps.
	 */
	private void populateMaps(){
		//Add us to the entity rendering list..
		Set<AEntityC_Definable<?>> worldEntities = renderableEntities.get(world);
		if(worldEntities == null){
			worldEntities = new HashSet<AEntityC_Definable<?>>();
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
				
				LinkedHashMap<JSONAnimationDefinition, DurationDelayClock> activeClocks = new LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>();
				if(soundDef.activeAnimations !=  null){
					for(JSONAnimationDefinition animation : soundDef.activeAnimations){
						activeClocks.put(animation, new DurationDelayClock(animation));
					}
				}
				soundActiveClocks.put(soundDef, activeClocks);
				
				LinkedHashMap<JSONAnimationDefinition, DurationDelayClock> volumeClocks = new LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>();
				if(soundDef.volumeAnimations !=  null){
					for(JSONAnimationDefinition animation : soundDef.volumeAnimations){
						volumeClocks.put(animation, new DurationDelayClock(animation));
					}
				}
				soundVolumeClocks.put(soundDef, volumeClocks);
				
				LinkedHashMap<JSONAnimationDefinition, DurationDelayClock> pitchClocks = new LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>();
				if(soundDef.pitchAnimations != null){
					for(JSONAnimationDefinition animation : soundDef.pitchAnimations){
						pitchClocks.put(animation, new DurationDelayClock(animation));
					}
				}
				soundPitchClocks.put(soundDef, pitchClocks);
			}
		}
		particleActiveClocks.clear();
		if(definition.rendering != null && definition.rendering.particles != null){
			for(JSONParticle particleDef : definition.rendering.particles){
				LinkedHashMap<JSONAnimationDefinition, DurationDelayClock> activeClocks = new LinkedHashMap<JSONAnimationDefinition, DurationDelayClock>();
				if(particleDef.activeAnimations !=  null){
					for(JSONAnimationDefinition animation : particleDef.activeAnimations){
						activeClocks.put(animation, new DurationDelayClock(animation));
					}
				}
				particleActiveClocks.put(particleDef, activeClocks);
				lastTickParticleSpawned.put(particleDef, ticksExisted);
			}
		}
	}
	
	@Override
	public void remove(){
		super.remove();
		renderableEntities.get(world).remove(this);
	}
	
	/**
	 *  Returns the current item for this entity.
	 */
	public <ItemInstance extends AItemPack<JSONDefinition>> ItemInstance getItem(){
		return PackParserSystem.getItem(definition.packID, definition.systemName, subName);
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
   	 *  Returns a string that represents this entity's secondary text color.  If this color is set,
   	 *  and text is told to render from this provider, and that text is told to use this color, then it will.
   	 *  Otherwise, the text will use its default color.
   	 */
    public String getSecondaryTextColor(){
    	for(JSONSubDefinition subDefinition : definition.definitions){
			if(subDefinition.subName.equals(subName)){
				return subDefinition.secondColor;
			}
		}
		throw new IllegalArgumentException("Tried to get the definition for an object of subName:" + subName + ".  But that isn't a valid subName for the object:" + definition.packID + ":" + definition.systemName + ".  Report this to the pack author as this is a missing JSON component!");
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
	 *  Called to reset the definition for this entity.  At this point, the definition will
	 *  already updated, so this is more for updating cached variables.
	 */
    public void onDefinitionReset(){
    	populateMaps();
    }
    
    /**
   	 *  Spawns particles for this entity.  This is called after every render frame, so
   	 *  watch your methods to prevent spam.  Note that this method is not called if the
   	 *  game is paused, as particles are assumed to only be spawned during normal entity
   	 *  updates.
   	 */
    public void spawnParticles(float partialTicks){
    	//Check all particle defs and update the existing particles accordingly.
    	for(JSONParticle particleDef : particleActiveClocks.keySet()){
    		//Check if the particle should be spawned this tick.
    		boolean shouldParticleSpawn = true;
			boolean anyClockMovedThisUpdate = false;
			if(particleDef.activeAnimations != null){
				boolean inhibitAnimations = false;
				for(JSONAnimationDefinition animation : particleDef.activeAnimations){
					switch(animation.animationType){
						case VISIBILITY :{
							//We use the clock here to check if the state of the variable changed, not
							//to clamp the value used in the testing.
							if(!inhibitAnimations){
								DurationDelayClock clock = particleActiveClocks.get(particleDef).get(animation);
								double variableValue = animation.offset + getAnimatedVariableValue(animation, 0, clock, partialTicks);
								if(!anyClockMovedThisUpdate){
									anyClockMovedThisUpdate = clock.movedThisUpdate;
								}
								if(variableValue < animation.clampMin || variableValue > animation.clampMax){
									shouldParticleSpawn = false;
								}
							}
							break;
						}
						case INHIBITOR :{
							if(!inhibitAnimations){
								double variableValue = getAnimatedVariableValue(animation, 0, particleActiveClocks.get(particleDef).get(animation), partialTicks);
								if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
									inhibitAnimations = true;
								}
							}
							break;
						}
						case ACTIVATOR :{
							if(inhibitAnimations){
								double variableValue = getAnimatedVariableValue(animation, 0, particleActiveClocks.get(particleDef).get(animation), partialTicks);
								if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
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
	 *  Returns the raw value for the passed-in variable.  If the variable is not present, NaM
	 *  should be returned (calling functions need to account for this!).
	 *  This should be extended on all sub-classes for them to provide their own variables.
	 *  For all cases of this, the sub-classed variables should be checked first.  If none are
	 *  found, then the super() method should be called to return those as a default.
	 */
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("tick"): return world.getTick();
			case("tick_sin"): return Math.sin(Math.toRadians(world.getTick()));
			case("tick_cos"): return Math.cos(Math.toRadians(world.getTick()));
			case("time"): return world.getTime();
			case("rain_strength"): return (int) world.getRainStrength(position);
			case("rain_sin"): {
				int rainStrength = (int) world.getRainStrength(position); 
				return rainStrength > 0 ? Math.sin(rainStrength*Math.toRadians(360*System.currentTimeMillis()/1000))/2D + 0.5: 0;
			}
			case("rain_cos"): {
				int rainStrength = (int) world.getRainStrength(position); 
				return rainStrength > 0 ? Math.cos(rainStrength*Math.toRadians(360*System.currentTimeMillis()/1000))/2D + 0.5 : 0;
			}	
			case("light_sunlight"): return world.getLightBrightness(position, false);
			case("light_total"): return world.getLightBrightness(position, true);
			case("ground_distance"): return world.getHeight(position);
		}
		
		//Check if this is a cycle variable.
		if(variable.startsWith("cycle")){
			int ticksCycle = Integer.valueOf(variable.substring(variable.indexOf('_') + 1, variable.lastIndexOf('_')));
			int startTick = Integer.valueOf(variable.substring(variable.lastIndexOf('_') + 1));
			return world.getTick()%ticksCycle >= startTick ? 1 : 0;
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
	 *  this functionality is not required.  The passed-in clock may be null to prevent duration/delay functionality.
	 *  Returns the value of the variable, or 0 if the variable is not valid.
	 */
	public final double getAnimatedVariableValue(JSONAnimationDefinition animation, double offset, DurationDelayClock clock, float partialTicks){
		double value = getRawVariableValue(animation.variable, partialTicks);
		if(Double.isNaN(value)){
			value = 0;
		}
		if(clock == null || !clock.isUseful){
			return clampAndScale(value, animation, offset);
		}else{
			return clampAndScale(clock.getFactoredState(this, value), animation, offset);
		}
	}
	
	/**
	 *  Helper method to clamp and scale the passed-in variable value based on the passed-in animation, 
	 *  returning it in the proper form.
	 */
	private static double clampAndScale(double value, JSONAnimationDefinition animation, double offset){
		if(animation.axis != null){
			value = animation.axis.length()*(animation.absolute ? Math.abs(value) : value) + animation.offset + offset;
			if(animation.clampMin != 0 && value < animation.clampMin){
				value = animation.clampMin;
			}else if(animation.clampMax != 0 && value > animation.clampMax){
				value = animation.clampMax;
			}
		}
		return animation.absolute ? Math.abs(value) : value;
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
    public void updateSounds(){
    	super.updateSounds();
    	//Check all sound defs and update the existing sounds accordingly.
    	for(JSONSound soundDef : allSoundDefs){
    		//Check if the sound should be playing before we try to update state.
    		AEntityD_Interactable<?> entityRiding = InterfaceClient.getClientPlayer().getEntityRiding();
    		boolean playerRidingEntity = this.equals(entityRiding) || (this instanceof APart && ((APart) this).entityOn.equals(entityRiding));
    		boolean shouldSoundPlay = playerRidingEntity && InterfaceClient.inFirstPerson() ? !soundDef.isExterior : !soundDef.isInterior;
			boolean anyClockMovedThisUpdate = false;
			if(shouldSoundPlay && soundDef.activeAnimations != null){
				boolean inhibitAnimations = false;
				for(JSONAnimationDefinition animation : soundDef.activeAnimations){
					switch(animation.animationType){
						case VISIBILITY :{
							//We use the clock here to check if the state of the variable changed, not
							//to clamp the value used in the testing.
							if(!inhibitAnimations){
								DurationDelayClock clock = soundActiveClocks.get(soundDef).get(animation);
								double variableValue = animation.offset + getAnimatedVariableValue(animation, 0, clock, 0);
								if(!anyClockMovedThisUpdate){
									anyClockMovedThisUpdate = clock.movedThisUpdate;
								}
								if(variableValue < animation.clampMin || variableValue > animation.clampMax){
									shouldSoundPlay = false;
								}
							}
							break;
						}
						case INHIBITOR :{
							if(!inhibitAnimations){
								double variableValue = getAnimatedVariableValue(animation, 0, soundActiveClocks.get(soundDef).get(animation), 0);
								if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
									inhibitAnimations = true;
								}
							}
							break;
						}
						case ACTIVATOR :{
							if(inhibitAnimations){
								double variableValue = getAnimatedVariableValue(animation, 0, soundActiveClocks.get(soundDef).get(animation), 0);
								if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
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
					InterfaceSound.playQuickSound(new SoundInstance(this, soundDef.name, soundDef.looping));
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
			SoundInstance sound = null;
			for(SoundInstance activeSound : sounds){
				if(activeSound.soundName.equals(soundDef.name)){
					sound = activeSound;
					break;
				}
			}
			
			if(sound != null){
				//Adjust volume.
				sound.volume = 1;
				if(soundDef.volumeAnimations != null && !soundDef.volumeAnimations.isEmpty()){
					boolean inhibitAnimations = false;
					boolean definedVolume = false;
					inhibitAnimations = false;
					sound.volume = 0;
					for(JSONAnimationDefinition animation : soundDef.volumeAnimations){
						switch(animation.animationType){
							case TRANSLATION :{
								if(!inhibitAnimations){
									definedVolume = true;
									sound.volume += Math.signum(animation.axis.y)*getAnimatedVariableValue(animation, -animation.offset, soundVolumeClocks.get(soundDef).get(animation), 0) + animation.offset;
								}
								break;
							}
							case ROTATION :{
								if(!inhibitAnimations){
									definedVolume = true;
									//Need to parse out parabola params here to not upset the axis calcs.
									double parabolaParamA = animation.axis.x;
									animation.axis.x = 0;
									double parabolaParamH = animation.axis.z;
									animation.axis.z = 0;
									double parabolaValue = Math.signum(animation.axis.y)*getAnimatedVariableValue(animation, -animation.offset, soundVolumeClocks.get(soundDef).get(animation), 0);
									sound.volume += parabolaParamA*Math.pow(parabolaValue - parabolaParamH, 2) + animation.offset;
									
									animation.axis.x = parabolaParamA;
									animation.axis.z = parabolaParamH;
								}
								break;
							}
							case INHIBITOR :{
								if(!inhibitAnimations){
									double variableValue = getAnimatedVariableValue(animation, 0, soundVolumeClocks.get(soundDef).get(animation), 0);
									if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
										inhibitAnimations = true;
									}
								}
								break;
							}
							case ACTIVATOR :{
								if(inhibitAnimations){
									double variableValue = getAnimatedVariableValue(animation, 0, soundVolumeClocks.get(soundDef).get(animation), 0);
									if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
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
				}
				
				//If the player is in a closed-top vehicle that isn't this one, dampen the sound
				//Unless it's a radio, in which case don't do so.
				if(!playerRidingEntity && sound.radio == null && entityRiding instanceof EntityVehicleF_Physics && !((EntityVehicleF_Physics) entityRiding).definition.motorized.hasOpenTop && InterfaceClient.inFirstPerson()){
					sound.volume *= 0.5F;
				}
				
				//Adjust pitch.
				if(soundDef.pitchAnimations != null && !soundDef.pitchAnimations.isEmpty()){
					boolean inhibitAnimations = false;
					sound.pitch = 0;
					for(JSONAnimationDefinition animation : soundDef.pitchAnimations){
						switch(animation.animationType){
							case TRANSLATION :{
								if(!inhibitAnimations){
									sound.pitch += Math.signum(animation.axis.y)*getAnimatedVariableValue(animation, -animation.offset, soundPitchClocks.get(soundDef).get(animation), 0) + animation.offset;
								}
								break;
							}
							case ROTATION :{
								if(!inhibitAnimations){
									//Need to parse out parabola params here to not upset the axis calcs.
									double parabolaParamA = animation.axis.x;
									animation.axis.x = 0;
									double parabolaParamH = animation.axis.z;
									animation.axis.z = 0;
									double parabolaValue = Math.signum(animation.axis.y)*getAnimatedVariableValue(animation, -animation.offset, soundPitchClocks.get(soundDef).get(animation), 0);
									sound.pitch += parabolaParamA*Math.pow(parabolaValue - parabolaParamH, 2) + animation.offset;
									
									animation.axis.x = parabolaParamA;
									animation.axis.z = parabolaParamH;
								}
								break;
							}
							case INHIBITOR :{
								if(!inhibitAnimations){
									double variableValue = getAnimatedVariableValue(animation, 0, soundPitchClocks.get(soundDef).get(animation), 0);
									if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
										inhibitAnimations = true;
									}
								}
								break;
							}
							case ACTIVATOR :{
								if(inhibitAnimations){
									double variableValue = getAnimatedVariableValue(animation, 0, soundPitchClocks.get(soundDef).get(animation), 0);
									if(variableValue >= animation.clampMin && variableValue <= animation.clampMax){
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
					if(sound.pitch < 0){
						sound.pitch = 0;
					}
				}
			}
    	}
    }
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		data.setString("packID", definition.packID);
		data.setString("systemName", definition.systemName);
		data.setString("subName", subName);
		int lineNumber = 0;
		for(String textLine : text.values()){
			data.setString("textLine" + lineNumber++, textLine);
		}
		data.setStrings("variablesOn", variablesOn);
	}
}
