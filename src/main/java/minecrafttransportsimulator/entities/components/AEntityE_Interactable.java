package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.TrailerConnection;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartGeneric;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.AJSONInteractableEntity;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup;
import minecrafttransportsimulator.jsondefs.JSONConnection;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;
import minecrafttransportsimulator.jsondefs.JSONInstrumentDefinition;
import minecrafttransportsimulator.jsondefs.JSONVariableModifier;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketEntityRiderChange;
import minecrafttransportsimulator.packets.instances.PacketEntityTrailerChange;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Base entity class containing riders and their positions on this entity.  Used for
 * entities that need to keep track of riders and their locations.  This also contains
 * various collision box lists for collision, as riders cannot interact and start riding
 * entities without collision boxes to click.
 * 
 * @author don_bruce
 */
public abstract class AEntityE_Interactable<JSONDefinition extends AJSONInteractableEntity> extends AEntityD_Definable<JSONDefinition>{
	
	/**List of boxes generated from JSON.  These are stored here as objects since they may not be
	 * added to their respective maps if they aren't active.**/
	public final Map<JSONCollisionGroup, Set<BoundingBox>> definitionCollisionBoxes = new HashMap<JSONCollisionGroup, Set<BoundingBox>>();
	private final Map<JSONCollisionGroup, List<DurationDelayClock>> collisionClocks = new HashMap<JSONCollisionGroup, List<DurationDelayClock>>();
	
	/**List of bounding boxes that should be used to check collision of this entity with blocks.**/
	public final Set<BoundingBox> blockCollisionBoxes = new HashSet<BoundingBox>();
	
	/**List of bounding boxes that should be used for collision of other entities with this entity.
	 * This includes {@link #blockCollisionBoxes}, but may include others.**/
	public final Set<BoundingBox> entityCollisionBoxes = new HashSet<BoundingBox>();
	
	/**List of bounding boxes that should be used for interaction of other entities with this entity.
	 * This includes all {@link #entityCollisionBoxes}, but may include others, most likely being the
	 * core {@link #boundingBox} for this entity.**/
	public final Set<BoundingBox> interactionBoxes = new HashSet<BoundingBox>();
	
	/**Box that encompasses all boxes on this entity.  This can be used as a pre-check for collision operations
	 * to check a single large box rather than multiple small ones to save processing power.**/
	public final BoundingBox encompassingBox = new BoundingBox(new Point3d(), new Point3d(), 0, 0, 0, false);
	
	/**Set of entities that this entity collided with this tick.  Any entity that is in this set 
	 * should NOT do collision checks with this entity, or infinite loops will occur.
	 * This set should be cleared after all collisions have been checked.**/
	public final Set<AEntityE_Interactable<?>> collidedEntities = new HashSet<AEntityE_Interactable<?>>();
	
	/**List of all possible locations for riders on this entity.  For the actual riders in these positions,
	 * see the map.  This list is only used to allow for querying of valid locations for placing riders.
	 * This should be populated prior to trying to load riders, so ideally this will be populated during construction.
	 * Note that these values are shared as keys in the rider map, so if you change them, you will no longer have
	 * hash equality in the keys.  If you need to interface with the map with a new Point3d object, you should do equality
	 * checks on this list to find the "same" point and use that in map operations to ensure hash-matching of the map.
	 **/
	public final Set<Point3d> ridableLocations = new HashSet<Point3d>();
	
	/**List of locations where rider were last save.  This is used to re-populate riders on reloads.
	 * It can be assumed that riders will be re-added in the same order the location list was saved.
	 **/
	public final List<Point3d> savedRiderLocations = new ArrayList<Point3d>();
	
	/**Maps relative position locations to riders riding at those positions.  Only one rider
	 * may be present per position.  Positions should be modified via mutable modification to
	 * avoid modifying this map.  The only modifications should be done when a rider is 
	 * mounting/dismounting this entity and we don't want to track them anymore.
	 * While you are free to read this map, all modifications should be through the method calls in this class.
	 **/
	public final BiMap<Point3d, WrapperEntity> locationRiderMap = HashBiMap.create();
	
	/**Maps instruments to their place in the JSON.  This is done instead of a list as there may
	 * be instruments not present, so we'd need to have empty slots in a list for this, and maps
	 * work better for this from a code standpoint than lists anyways.
	 **/
	public final Map<Integer, ItemInstrument> instruments = new HashMap<Integer, ItemInstrument>();
	
	/**Locked state.  Locked entities should not be able to be interacted with except by entities riding them,
	 * their owners, or OP players (server admins).
	 **/
	public boolean locked;
	
	/**The ID of the owner of this entity. If this is null, it can be assumed that there is no owner.
	 * UUIDs are set at creation time of an entity, and will never change, even on world re-loads.
	 **/
	public final UUID ownerUUID;
	
	/**The amount of damage on this entity.  This value is not necessarily used on all entities, but is put here
	 * as damage is something that a good number of entities will have and that the base entity should track.
	 **/
	@DerivedValue
	public double damageAmount;
	public static final String DAMAGE_VARIABLE = "damage";
	
	/**Internal flag to prevent this entity from updating until the entity that is towing it has.  If we don't
	 * do this, then there may be a 1-tick de-sync between towing and towed entities if the towed entity gets
	 * updated before the one towing it.
	 **/
	protected boolean overrideTowingChecks;
	
	//Connection data.
	public TrailerConnection towedByConnection;
	protected final Set<TrailerConnection> towingConnections = new HashSet<TrailerConnection>();
	private TrailerConnection savedTowedByConnection;
	private final Set<TrailerConnection> savedTowingConnections = new HashSet<TrailerConnection>();
	public static final String TRAILER_CONNECTION_REQUEST_VARIABLE = "connection_requested";
	
	//Mutable variables.
	private final Point3d collisionGroupAnimationResult = new Point3d();
	private final Point3d collisionGroupWorkingAngles = new Point3d();
	private final Point3d collisionGroupWorkingAngleOffset = new Point3d();
	
	public AEntityE_Interactable(WrapperWorld world, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, placingPlayer, data);
		//Load saved rider positions.  We don't have riders here yet (as those get created later), 
		//so just make the locations for the moment so they are ready when riders are created.
		this.savedRiderLocations.addAll(data.getPoint3ds("savedRiderLocations"));
		this.locked = data.getBoolean("locked");
		this.ownerUUID = placingPlayer != null ? placingPlayer.getID() : data.getUUID("ownerUUID");

		//Load towing data.
		WrapperNBT towData = data.getData("towedByConnection");
		if(towData != null){
			this.savedTowedByConnection = new TrailerConnection(towData);
		}
		
		int towingConnectionCount = data.getInteger("towingConnectionCount");
		for(int i=0; i<towingConnectionCount; ++i){
			towData = data.getData("towingConnection" + i);
			if(towData != null){
				this.savedTowingConnections.add(new TrailerConnection(towData));
			}
		}
		
		//Load instruments.  If we are new, create the default ones.
		if(definition.instruments != null){
			if(newlyCreated){
				for(JSONInstrumentDefinition packInstrument : definition.instruments){
					if(packInstrument.defaultInstrument != null){
						try{
							String instrumentPackID = packInstrument.defaultInstrument.substring(0, packInstrument.defaultInstrument.indexOf(':'));
							String instrumentSystemName = packInstrument.defaultInstrument.substring(packInstrument.defaultInstrument.indexOf(':') + 1);
							try{
								ItemInstrument instrument = PackParserSystem.getItem(instrumentPackID, instrumentSystemName);
								if(instrument != null){
									instruments.put(definition.instruments.indexOf(packInstrument), instrument);
									continue;
								}
							}catch(NullPointerException e){
								remove();
								throw new IllegalArgumentException("Attempted to add defaultInstrument: " + instrumentPackID + ":" + instrumentSystemName + " to: " + definition.packID + ":" + definition.systemName + " but that instrument doesn't exist in the pack item registry.");
							}
						}catch(IndexOutOfBoundsException e){
							remove();
							throw new IllegalArgumentException("Could not parse defaultInstrument definition: " + packInstrument.defaultInstrument + ".  Format should be \"packId:instrumentName\"");
						}
					}
				}
			}else{
				for(int i = 0; i<definition.instruments.size(); ++i){
					String instrumentPackID = data.getString("instrument" + i + "_packID");
					String instrumentSystemName = data.getString("instrument" + i + "_systemName");
					if(!instrumentPackID.isEmpty()){
						ItemInstrument instrument = PackParserSystem.getItem(instrumentPackID, instrumentSystemName);
						//Check to prevent loading of faulty instruments due to updates.
						if(instrument != null){
							instruments.put(i, instrument);
						}
					}
				}
			}
		}
	}
	
	@Override
	protected void initializeDefinition(){
		super.initializeDefinition();
		//Create collision boxes.
		definitionCollisionBoxes.clear();
		collisionClocks.clear();
		if(definition.collisionGroups != null){
			for(JSONCollisionGroup groupDef : definition.collisionGroups){
				Set<BoundingBox> boxes = new HashSet<BoundingBox>();
				for(JSONCollisionBox boxDef : groupDef.collisions){
					boxes.add(new BoundingBox(boxDef, groupDef));
				}
				definitionCollisionBoxes.put(groupDef, boxes);
				if(groupDef.animations != null){
					List<DurationDelayClock> animations = new ArrayList<DurationDelayClock>();
					for(JSONAnimationDefinition animation : groupDef.animations){
						animations.add(new DurationDelayClock(animation));
					}
					collisionClocks.put(groupDef, animations);
				}
			}
		}
		//Update collision boxes as they might have changed.
		updateCollisionBoxes();
				
		//Create instrument animation clocks.
		if(definition.instruments != null){
			for(int i=0; i<definition.instruments.size(); ++i){
				JSONInstrumentDefinition packInstrument = definition.instruments.get(i);
				if(packInstrument.animations != null){
					for(JSONAnimationDefinition animation : packInstrument.animations){
						animationClocks.put(animation, new DurationDelayClock(animation));
					}
				}
			}
		}
		
		//Add variable modifiers.
		if(definition.variableModifiers != null){
			for(JSONVariableModifier modifier : definition.variableModifiers){
				if(modifier.animations != null){
					for(JSONAnimationDefinition animation : modifier.animations){
						animationClocks.put(animation, new DurationDelayClock(animation));
					}
				}
			}
		}
	}
	
	@Override
	public boolean update(){
		//Need to do this before updating as these require knowledge of prior states.
		//If we call super, then it will overwrite the prior state.
		updateVariableModifiers();
				
		//Do validity checks for towing variables.  We could do this whenever we disconnect,
		//but there are tons of ways this could happen.  The trailer could blow up, the 
		//part-hitch could have been blown up, the trailer could have gotten wrenched, the
		//part hitch could have gotten wrenched, etc.  And that doesn't even count what the
		//thing towing us could have done! 
		if(towedByConnection != null){
			if(!towedByConnection.hitchEntity.isValid){
				towedByConnection = null;
			}
		}
		if(!towingConnections.isEmpty()){
			//First functional expression here in the whole codebase, history in the making!
			towingConnections.removeIf(connection -> !connection.hookupEntity.isValid);
		}
		
		//Now check if we can do the actual update based on our towing status.
		//We want to do the towing checks first, as we don't want to call super if we are blocked by being towed.
		if((towedByConnection == null || overrideTowingChecks) && super.update()){
			world.beginProfiling("EntityE_Level", true);
			
			//Update damage value
			damageAmount = getVariable(DAMAGE_VARIABLE);
			
			//See if we need to link connections.
			//We need to wait on this in case the entity didn't load at the same time.
			//That being said, it may be the vehicle we are loading is in another chunk.
			//As such we wait only some time, and if we caon't find all entities, we remove
			//them from the listing of entities to find.
			//Only do this once a second, and if we hit 5 seconds, bail.
			if(savedTowedByConnection != null){
				if(ticksExisted%20 == 0){
					if(ticksExisted <= 100){
						try{
							if(savedTowedByConnection.setConnection(world)){
								towedByConnection = savedTowedByConnection;
								savedTowedByConnection = null;
							}
						}catch(Exception e){
							savedTowedByConnection = null;
							InterfaceCore.logError("Could not hook-up trailer to entity towing it.  Did the JSON or pack change?");
						}
					}else{
						savedTowedByConnection = null;
						InterfaceCore.logError("Could not hook-up trailer to entity towing it.  Did the JSON or pack change?");
					}
				}
			}
			if(!savedTowingConnections.isEmpty()){
				if(ticksExisted%20 == 0){
					if(ticksExisted <= 100){
						Iterator<TrailerConnection> iterator = savedTowingConnections.iterator();
						while(iterator.hasNext()){
							TrailerConnection savedTowingConnection = iterator.next();
							try{
								if(savedTowingConnection.setConnection(world)){
									towingConnections.add(savedTowingConnection);
									iterator.remove();
								}
							}catch(Exception e){
								iterator.remove();
								InterfaceCore.logError("Could not connect trailer(s) to the entity towing them.  Did the JSON or pack change?");
							}
						}
					}else{
						savedTowingConnections.clear();
						InterfaceCore.logError("Could not connect trailer(s) to the entity towing them.  Did the JSON or pack change?");
					}
				}
			}
			
			//If we have a connection request, handle it now.
			int connectionRequestIndex = (int) getVariable(TRAILER_CONNECTION_REQUEST_VARIABLE);
			if(connectionRequestIndex != 0){
				if(!world.isClient()){
					//Don't handle requests on the client.  These get packets.
					handleConnectionRequest(connectionRequestIndex - 1);
				}
				setVariable(TRAILER_CONNECTION_REQUEST_VARIABLE, 0);
			}
			
			world.endProfiling();
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public double getMass(){
		return 100*locationRiderMap.values().size();
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		//Check if this is a hookup or hitch variable.
		if(variable.startsWith("connection")){
			//Format is (hitch/hookup)_groupIndex_connectionIndex_animationType.
			TrailerConnection foundConnection = null;
			String[] variableData = variable.split("_");
			if(variableData.length == 4){
				boolean isHookup = false;
				int groupIndex = Integer.valueOf(variableData[1]) - 1;
				int connectionIndex = Integer.valueOf(variableData[2]) - 1;
				if(towedByConnection != null){
					if(towedByConnection.hookupGroupIndex == groupIndex && towedByConnection.hookupConnectionIndex == connectionIndex){
						isHookup = true;
						foundConnection = towedByConnection;
					}
				}
				if(foundConnection == null && !towingConnections.isEmpty()){
					for(TrailerConnection towingConnection : towingConnections){
						if(towingConnection.hookupGroupIndex == groupIndex && towingConnection.hookupConnectionIndex == connectionIndex){
							foundConnection = towingConnection;
							break;
						}
					}
				}
				if(foundConnection != null){
					switch(variableData[3]){
						case("connected"): return 1;
						case("pitch"): return isHookup ? foundConnection.hookupEntity.angles.x - angles.x : foundConnection.hitchEntity.angles.x - angles.x;
						case("yaw"): return isHookup ? foundConnection.hookupEntity.angles.y - angles.y : foundConnection.hitchEntity.angles.y - angles.y;
						case("roll"): return isHookup ? foundConnection.hookupEntity.angles.z - angles.z : foundConnection.hitchEntity.angles.z - angles.z;
					}
				}
			}
		}else{
			switch(variable){
				case("damage"): return damageAmount;
				case("damage_percent"): return damageAmount/definition.general.health;
			}
		}
		
		//Not a towing variable, check others.
		return super.getRawVariableValue(variable, partialTicks);
	}
	
	/**
   	 *  Updates the position of all collision boxes, and sets them in their appropriate maps based on their
   	 *  properties, and animation state (if applicable). 
   	 */
    protected void updateCollisionBoxes(){
    	blockCollisionBoxes.clear();
    	entityCollisionBoxes.clear();
    	interactionBoxes.clear();
    	
    	if(definition.collisionGroups != null){
			for(JSONCollisionGroup groupDef : definition.collisionGroups){
				Set<BoundingBox> collisionBoxes = definitionCollisionBoxes.get(groupDef);
				if(collisionBoxes == null){
					//This can only happen if we hotloaded the definition due to devMode.
					//Flag us as needing a reset, and then bail to prevent further collision checks.
					animationsInitialized = false;
					return;
				}
				if(groupDef.health == 0 || getVariable("collision_" + (definition.collisionGroups.indexOf(groupDef) + 1) + "_damage") < groupDef.health){
					if(groupDef.animations != null){
						boolean inhibitAnimations = false;
						boolean inhibitCollision = false;
						//Reset working angles, but don't reset offset as it's not required.
						collisionGroupWorkingAngles.set(0, 0, 0);
						//Set box global center to local center.  This is used as a temp storage to do proper animation math.
						for(BoundingBox box : collisionBoxes){
							box.globalCenter.setTo(box.localCenter);
						}
						for(DurationDelayClock clock : collisionClocks.get(groupDef)){
							switch(clock.animation.animationType){
								case VISIBILITY :{
									if(!inhibitAnimations){
										double variableValue = getAnimatedVariableValue(clock, 0);
										if(variableValue < clock.animation.clampMin || variableValue > clock.animation.clampMax){
											inhibitCollision = true;
										}
									}
									break;
								}
								case INHIBITOR :{
									if(!inhibitAnimations){
										double variableValue = getAnimatedVariableValue(clock, 0);
										if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
											inhibitAnimations = true;
										}
									}
									break;
								}
								case ACTIVATOR :{
									if(inhibitAnimations){
										double variableValue = getAnimatedVariableValue(clock, 0);
										if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
											inhibitAnimations = false;
										}
									}
									break;
								}
								case TRANSLATION :{
									if(!inhibitAnimations){
										//Found translation.  This gets applied in the translation axis direction directly.
										double variableValue = getAnimatedVariableValue(clock, clock.animationAxisMagnitude, 0);
										collisionGroupAnimationResult.setTo(clock.animationAxisNormalized).multiply(variableValue).rotateFine(collisionGroupWorkingAngles);
										for(BoundingBox box : collisionBoxes){
											box.globalCenter.add(collisionGroupAnimationResult);
										}
									}
									break;
								}
								case ROTATION :{
									if(!inhibitAnimations){
										//Found rotation.  Get angles that needs to be applied.
										//We need to apply this to every box differently due to offsets.
										double variableValue = getAnimatedVariableValue(clock, clock.animationAxisMagnitude, 0);
										collisionGroupAnimationResult.setTo(clock.animationAxisNormalized).multiply(variableValue);
										
										for(BoundingBox box : collisionBoxes){
											//Use the center point as a vector we rotate to get the applied offset.
											//We need to take into account the current offset here, as we might have rotated on a prior call.
											collisionGroupWorkingAngleOffset.setTo(box.globalCenter).subtract(box.localCenter);
											box.globalCenter.subtract(clock.animation.centerPoint).subtract(collisionGroupWorkingAngleOffset).rotateFine(collisionGroupAnimationResult).add(clock.animation.centerPoint).add(collisionGroupWorkingAngleOffset);
										}
										
										//Apply rotation.  We need to do this after translation operations to ensure proper offsets.
										collisionGroupWorkingAngles.add(collisionGroupAnimationResult);
									}
									break;
								}
								case SCALING :{
									//Do nothing.
									break;
								}
							}
							if(inhibitCollision){
								//No need to process further.
								break;
							}
						}
						
						//Update collisions using temp offset.
						//Need to move it to temp variable to not get overwritten.
						if(!inhibitCollision){
							for(BoundingBox box : collisionBoxes){
								collisionGroupAnimationResult.setTo(box.globalCenter).subtract(box.localCenter);
								box.updateToEntity(this, collisionGroupAnimationResult);
							}
						}else{
							//Don't let these boxes get added to the list.
							continue;
						}
					}else{
						for(BoundingBox box : collisionBoxes){
							box.updateToEntity(this, null);
						}
					}
					entityCollisionBoxes.addAll(collisionBoxes);
					if(!groupDef.isInterior && !ConfigSystem.configObject.general.noclipVehicles.value){
						blockCollisionBoxes.addAll(collisionBoxes);
					}
				}
			}
    	}
    	interactionBoxes.addAll(entityCollisionBoxes);
    	
    	//Now get the encompassing box.
    	encompassingBox.widthRadius = 0;
    	encompassingBox.heightRadius = 0;
    	encompassingBox.depthRadius = 0;
    	for(BoundingBox box : interactionBoxes){
    		encompassingBox.widthRadius = (float) Math.max(encompassingBox.widthRadius, Math.abs(box.globalCenter.x - position.x + box.widthRadius));
    		encompassingBox.heightRadius = (float) Math.max(encompassingBox.heightRadius, Math.abs(box.globalCenter.y - position.y + box.heightRadius));
    		encompassingBox.depthRadius = (float) Math.max(encompassingBox.depthRadius, Math.abs(box.globalCenter.z - position.z + box.depthRadius));
    	}
    	encompassingBox.updateToEntity(this, null);
    }
    
    /**
	 * Called to update the variable modifiers for this entity.
	 * By default, this will get any variables that {@link #getVariable(String)}
	 * returns, but can be extended to do other variables specific to the entity.
	 */
	protected void updateVariableModifiers(){
		if(definition.variableModifiers != null){
			for(JSONVariableModifier modifier : definition.variableModifiers){
				setVariable(modifier.variable, adjustVariable(modifier, (float) getVariable(modifier.variable)));
			}
		}
	}
	
	 /**
	 * Helper method for variable modification.
	 */
	protected float adjustVariable(JSONVariableModifier modifier, float currentValue){
		float modifiedValue = modifier.setValue != 0 ? modifier.setValue : currentValue + modifier.addValue;
		boolean doModification = true;
		if(modifier.animations != null){
			boolean inhibitAnimations = false;
			for(JSONAnimationDefinition animation : modifier.animations){
				DurationDelayClock clock = animationClocks.get(animation);
				if(clock != null){
					switch(animation.animationType){
						case VISIBILITY :{
							if(!inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, 0);
								if(variableValue < clock.animation.clampMin || variableValue > clock.animation.clampMax){
									doModification = false;
								}
							}
							break;
						}
						case INHIBITOR :{
							if(!inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, 0);
								if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
									inhibitAnimations = true;
								}
							}
							break;
						}
						case ACTIVATOR :{
							if(inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, 0);
								if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
									inhibitAnimations = false;
								}
							}
							break;
						}
						case TRANSLATION :{
						    if(!inhibitAnimations){
						    	if(clock.animation.axis.x != 0){
									modifiedValue *= getAnimatedVariableValue(clock, clock.animation.axis.x, 0);
								}else if(clock.animation.axis.y != 0){
									modifiedValue += getAnimatedVariableValue(clock, clock.animation.axis.y, 0);
								}else{
									modifiedValue = (float) getAnimatedVariableValue(clock, clock.animation.axis.z, 0);
								} 
						    }
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
					if(!doModification){
						break;
					}
				}
			}
		}
		return doModification ? modifiedValue : currentValue;
	}
	
    /**
	 * Called to perform supplemental update logic on this entity.  This should be called after all movement on the
	 * entity has been performed, and is used to do updates that require the new positional logic to be ready.
	 * Calling this before the entity finishes moving will lead to things "lagging" behind the entity.
	 */
	public void updatePostMovement(){
		//Update collision boxes to new position.
		world.beginProfiling("CollisionBoxUpdates", true);
		updateCollisionBoxes();
		world.endProfiling();
		
		//If we are towing entities, update them now.
		if(!towingConnections.isEmpty()){
			world.beginProfiling("TowedEntities", true);
			for(TrailerConnection connection : towingConnections){
				connection.hookupVehicle.overrideTowingChecks = true;
				connection.hookupVehicle.update();
				connection.hookupVehicle.overrideTowingChecks = false;
			}
			world.endProfiling();
		}
		
		//Move all entities that are touching this entity.
		if(!entityCollisionBoxes.isEmpty()){
			world.beginProfiling("MoveAlongEntities", true);
			encompassingBox.heightRadius += 1.0;
			List<WrapperEntity> nearbyEntities = world.getEntitiesWithin(encompassingBox);
			encompassingBox.heightRadius -= 1.0;
    		for(WrapperEntity entity : nearbyEntities){
    			//Only move Vanilla entities not riding things.  We don't want to move other things as we handle our inter-entity movement in each class.
    			if(entity.getEntityRiding() == null && (!(entity instanceof WrapperPlayer) || !((WrapperPlayer) entity).isSpectator())){
    				//Check each box individually.  Need to do this to know which delta to apply.
    				BoundingBox entityBounds = entity.getBounds();
    				entityBounds.heightRadius += 0.25;
    				for(BoundingBox box : entityCollisionBoxes){
        				if(entityBounds.intersects(box)){
							//If the entity is within 0.5 units of the top of the box, we can move them.
							//If not, they are just colliding and not on top of the entity and we should leave them be.
							double entityBottomDelta = box.globalCenter.y + box.heightRadius - (entityBounds.globalCenter.y - entityBounds.heightRadius + 0.25F);
							if(entityBottomDelta >= -0.5 && entityBottomDelta <= 0.5){
								//Only move the entity if it's going slow or in the delta.  Don't move if it's going fast as they might have jumped.
								Point3d entityVelocity = entity.getVelocity();
								if(entityVelocity.y < 0 || entityVelocity.y < entityBottomDelta){
									//Get how much the entity moved the collision box the entity collided with so we know how much to move the entity.
									//This lets entities "move along" with entities when touching a collision box.
									Point3d entityPosition = entity.getPosition();
									Point3d linearMovement = position.copy().subtract(prevPosition);
									Point3d angularMovement = angles.copy().subtract(prevAngles);
									Point3d entityDeltaOffset = entityPosition.copy().subtract(prevPosition);
									Point3d vehicleBoxMovement = entityDeltaOffset.copy().rotateFine(angularMovement).subtract(entityDeltaOffset).add(linearMovement);
									
									//Apply motions to move entity.
									entityPosition.add(vehicleBoxMovement).add(0, entityBottomDelta, 0);
									entity.setPosition(entityPosition, true);
									entity.setYaw(entity.getYaw() + angularMovement.y);
									entity.setBodyYaw(entity.getBodyYaw() + angularMovement.y);
									break;
								}
							}
        				}
    				}
    			}
    		}
    		world.endProfiling();
		}
	}
	
	/**
   	 *  Returns a collection of BoundingBoxes that make up this entity's collision bounds.
   	 */
    public Collection<BoundingBox> getCollisionBoxes(){
    	return entityCollisionBoxes;
    }
    
    /**
   	 *  Returns a collection of BoundingBoxes that make up this entity's interaction bounds.
   	 */
    public Collection<BoundingBox> getInteractionBoxes(){
    	return interactionBoxes;
    }
	
	/**
	 *  Called to update the passed-in rider.  This gets called after the update loop,
	 *  as the entity needs to move to its new position before we can know where the
	 *  riders of said entity will be.
	 */
	public void updateRider(WrapperEntity rider, Iterator<WrapperEntity> iterator){
		//Update entity position and motion.
		if(rider.isValid()){
			rider.setPosition(locationRiderMap.inverse().get(rider), false);
			rider.setVelocity(motion);
		}else{
			//Remove invalid rider.
			removeRider(rider, iterator);
		}
	}
	
	/**
	 *  Called to add a rider to this entity.  Passed-in point is the point they
	 *  should try to ride.  If this isn't possible, return false.  Otherwise,
	 *  return true.  Call this ONLY on the server!  Packets are sent to clients
	 *  for syncing so calling this on clients will result in Bad Stuff.
	 *  If we are re-loading a rider from saved data, pass-in null as the position
	 *  
	 */
	public boolean addRider(WrapperEntity rider, Point3d riderLocation){
		if(riderLocation == null){
			if(savedRiderLocations.isEmpty()){
				return false;
			}else{
				riderLocation = savedRiderLocations.get(0);
			}
		}
		
		//Need to find the actual point reference for this to ensure hash equality.
		for(Point3d location : ridableLocations){
			if(riderLocation.equals(location)){
				riderLocation = location;
				break;
			}
		}
		
		//Remove the existing location, if we have one.
		savedRiderLocations.remove(riderLocation);
		if(locationRiderMap.containsKey(riderLocation)){
			//We already have a rider in this location.
			return false;
		}else{
			//If this rider wasn't riding this vehicle before, adjust their yaw.
			//This prevents bad math due to 360+ degree rotations.
			//If we are riding this vehicle, clear out the location before we change it.
			if(!locationRiderMap.containsValue(rider)){
				rider.setYaw(angles.y);
			}else{
				locationRiderMap.inverse().remove(rider);
			}
			
			//Add rider to map, and send out packet if required.
			locationRiderMap.put(riderLocation, rider);
			if(!world.isClient()){
				rider.setRiding(this);
				InterfacePacket.sendToAllClients(new PacketEntityRiderChange(this, rider, riderLocation));
			}
			return true;
		}
	}
	
	/**
	 *  Called to remove the passed-in rider from this entity.
	 *  Passed-in iterator is optional, but MUST be included if this is called inside a loop
	 *  that's iterating over {@link #ridersToLocations} or you will get a CME!
	 */
	public void removeRider(WrapperEntity rider, Iterator<WrapperEntity> iterator){
		if(locationRiderMap.containsValue(rider)){
			if(iterator != null){
				iterator.remove();
			}else{
				locationRiderMap.inverse().remove(rider);
			}
			if(!world.isClient()){
				rider.setRiding(null);
				InterfacePacket.sendToAllClients(new PacketEntityRiderChange(this, rider, null));
			}
		}
	}
	
	/**
	 *  Returns the owner state of the passed-in player, relative to this entity.
	 *  Takes into account player OP status and {@link #ownerUUID}, if set.
	 */
	public PlayerOwnerState getOwnerState(WrapperPlayer player){
		boolean canPlayerEdit = player.isOP() || ownerUUID == null || player.getID().equals(ownerUUID);
		return player.isOP() ? PlayerOwnerState.ADMIN : (canPlayerEdit ? PlayerOwnerState.OWNER : PlayerOwnerState.USER);
	}
	
	/**
	 *  Called when the entity is attacked.
	 *  This should ONLY be called on the server; clients will sync via packets.
	 *  If calling this method in a loop, make sure to check if this entity is valid.
	 *  as this function may be called multiple times in a single tick for multiple damage 
	 *  applications, which means one of those may have made this entity invalid.
	 */
	public void attack(Damage damage){
		if(!damage.isWater){ 
			if(definition.collisionGroups != null){
				for(JSONCollisionGroup groupDef : definition.collisionGroups){
					Set<BoundingBox> collisionBoxes = definitionCollisionBoxes.get(groupDef);
					if(collisionBoxes.contains(damage.box)){
						if(groupDef.health != 0){
							String variableName = "collision_" + (definition.collisionGroups.indexOf(groupDef) + 1) + "_damage";
							double currentDamage = getVariable(variableName) + damage.amount;
							if(currentDamage > groupDef.health){
								double amountActuallyNeeded = damage.amount - (currentDamage - groupDef.health);
								currentDamage = groupDef.health;
								InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, variableName, amountActuallyNeeded));
							}else{
								InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, variableName, damage.amount));
							}
							setVariable(variableName, currentDamage);
							return;
						}
					}
				}
			}
			
			//Didn't hit a collision box or found one with no health defined 
			damageAmount += damage.amount;
			//FIXME this goes away when we make fake guns go away.
			if(definition.general != null && damageAmount > definition.general.health){
				double amountActuallyNeeded = damage.amount - (damageAmount - definition.general.health);
				damageAmount = definition.general.health;
				InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, DAMAGE_VARIABLE, amountActuallyNeeded));
			}else{
				InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, DAMAGE_VARIABLE, damage.amount));
			}
			setVariable(DAMAGE_VARIABLE, damageAmount);
		}
	}
	
	/**
	 * Checks if the other entity can be connected to this entity.  The other entity may be a trailer we
	 * want to connect, or it may be a trailer that has requested to connect to us.  In either case, we
	 * are the main entity and will start towing the trailer if the connection is successful.
	 * For connection indexes, a -1 will allow for any index, while a value other than -1 will try to connect
	 * using only that connection group.
	 * 
	 */
	public EntityConnectionResult checkIfTrailerCanConnect(AEntityE_Interactable<?> hookupEntity, int hitchGroupIndex, int hookupGroupIndex){
		//Init variables.
		boolean matchingConnection = false;
		boolean trailerInRange = false;
		
		//First make sure the entity is in-range.  This is done by checking if the entity is even remotely close enough.
		double trailerDistance = position.distanceTo(hookupEntity.position);
		if(trailerDistance < 25){
			//Check all connection groups on the other entity to see if we can connect to them.
			//If we specified a index, skip all others.
			if(definition.connectionGroups != null && !definition.connectionGroups.isEmpty() && hookupEntity.definition.connectionGroups != null && !hookupEntity.definition.connectionGroups.isEmpty()){
				for(JSONConnectionGroup hitchConnectionGroup : definition.connectionGroups){
					if(!hitchConnectionGroup.hookup && (hitchGroupIndex == -1 || definition.connectionGroups.indexOf(hitchConnectionGroup) == hitchGroupIndex)){
						for(JSONConnectionGroup hookupConnectionGroup : hookupEntity.definition.connectionGroups){
							if(hookupConnectionGroup.hookup && (hookupGroupIndex == -1 || hookupEntity.definition.connectionGroups.indexOf(hookupConnectionGroup) == hookupGroupIndex)){
								//We can potentially connect these two entities.  See if we actually can.
								for(JSONConnection hitchConnection : hitchConnectionGroup.connections){
									Point3d hitchPos = hitchConnection.pos.copy().rotateFine(angles).add(position);
									double maxDistance = hitchConnection.distance > 0 ? hitchConnection.distance : 2;
									for(JSONConnection hookupConnection : hookupConnectionGroup.connections){
										Point3d hookupPos = hookupConnection.pos.copy().rotateFine(hookupEntity.angles).add(hookupEntity.position);
										if(hitchPos.distanceTo(hookupPos) < maxDistance + 10){
											boolean validType = hitchConnection.type.equals(hookupConnection.type);
											boolean validDistance = hitchPos.distanceTo(hookupPos) < maxDistance;
											if(validType && validDistance){
												connectTrailer(new TrailerConnection(this, definition.connectionGroups.indexOf(hitchConnectionGroup), hitchConnectionGroup.connections.indexOf(hitchConnection), hookupEntity, hookupEntity.definition.connectionGroups.indexOf(hookupConnectionGroup), hookupConnectionGroup.connections.indexOf(hookupConnection)));
												return EntityConnectionResult.TRAILER_CONNECTED;
											}else if(validType){
												matchingConnection = true;
											}else if(validDistance){
												trailerInRange = true;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		//Return results.
		if(matchingConnection && !trailerInRange){
			return EntityConnectionResult.TRAILER_TOO_FAR;
		}else if(!matchingConnection && trailerInRange){
			return EntityConnectionResult.TRAILER_WRONG_HITCH;
		}else{
			return EntityConnectionResult.NO_TRAILER_NEARBY;
		}
	}
	
	/**
	 * Method block for handling a trailer connection requsdt. a trailer to this entity.
	 */
	private void handleConnectionRequest(int connectionGroupIndex){
		JSONConnectionGroup requestedGroup = definition.connectionGroups.get(connectionGroupIndex);
		boolean requestIsToBecomeTrailer = requestedGroup.hookup;
		boolean connect;
		String packetMessage = null;
		
		//Check if this is a connect or disconnect request.
		if(requestIsToBecomeTrailer){
			//Can connect as a trailer if we're not being towed already.
			connect = towedByConnection == null;
		}else{
			//Can connect trailer if this connection group isn't being used.
			boolean foundConnection = false;
			for(TrailerConnection connection : getTowingConnections()){
				if(connection.hitchGroupIndex == connectionGroupIndex){
					foundConnection = true;
				}
			}
			connect = !foundConnection;
		}
		
		if(connect){
			boolean matchingConnection = false;
			boolean trailerInRange = false;
			List<AEntityE_Interactable<?>> entitiesToCheck = new ArrayList<AEntityE_Interactable<?>>();
			entitiesToCheck.addAll(world.getEntitiesOfType(EntityVehicleF_Physics.class));
			entitiesToCheck.addAll(world.getEntitiesOfType(PartGeneric.class));
			
			if(requestIsToBecomeTrailer){
				for(AEntityE_Interactable<?> testEntity : entitiesToCheck){
					if(shouldConnect(testEntity, this)){
						switch(((AEntityE_Interactable<?>) testEntity).checkIfTrailerCanConnect(this, -1, connectionGroupIndex)){
							case TRAILER_CONNECTED : packetMessage = "interact.trailer.connect"; break;
							case TRAILER_TOO_FAR : matchingConnection = true; break;
							case TRAILER_WRONG_HITCH : trailerInRange = true; break;
							case NO_TRAILER_NEARBY : break;
						}
					}
					if(packetMessage != null){
						break;
					}
				}
			}else{
				for(AEntityE_Interactable<?> testEntity : entitiesToCheck){
					if(shouldConnect(this, testEntity)){
						switch(this.checkIfTrailerCanConnect(testEntity, connectionGroupIndex, -1)){
							case TRAILER_CONNECTED : packetMessage = "interact.trailer.connect"; break;
							case TRAILER_TOO_FAR : matchingConnection = true; break;
							case TRAILER_WRONG_HITCH : trailerInRange = true; break;
							case NO_TRAILER_NEARBY : break;
						}
					}
					if(packetMessage != null){
						break;
					}
				}
			}
			
			//Get message based on what we found.
			if(packetMessage == null){
				if(!matchingConnection && !trailerInRange){
					packetMessage = "interact.trailer.notfound";
				}else if(matchingConnection && !trailerInRange){
					packetMessage = "interact.trailer.toofar";
				}else if(!matchingConnection && trailerInRange){
					packetMessage = "interact.trailer.wronghitch";
				}else{
					packetMessage = "interact.trailer.wrongplacement";
				}
			}
		}else{
			if(requestIsToBecomeTrailer){
				towedByConnection.hitchEntity.disconnectTrailer(towedByConnection);
				packetMessage = "interact.trailer.disconnect";
			}else{
				for(TrailerConnection connection : getTowingConnections()){
					if(connection.hitchGroupIndex == connectionGroupIndex){
						disconnectTrailer(connection);
						packetMessage = "interact.trailer.disconnect";
						break;
					}
				}
			}
		}
		if(packetMessage != null){
			for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))){
				if(entity instanceof WrapperPlayer){
					((WrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((WrapperPlayer) entity, packetMessage));
				}
			}
		}
	}
	
	private static boolean shouldConnect(AEntityE_Interactable<?> hitchEntity, AEntityE_Interactable<?> hookupEntity){
		if(hookupEntity.towedByConnection != null){
			return false; //Entity is already hooked up.
		}else if(hookupEntity.equals(hitchEntity)){
			return false; //Entity is the same.
		}else if(hookupEntity instanceof AEntityF_Multipart && ((AEntityF_Multipart<?>) hookupEntity).parts.contains(hitchEntity)){
			return false; //Hitch is a part on hookup.
		}else if(hitchEntity instanceof AEntityF_Multipart && ((AEntityF_Multipart<?>) hitchEntity).parts.contains(hookupEntity)){
			return false; //Hookup is a part on hitch.
		}else{
			//Check to make sure the hookupEntity isn't towing the hitchEntity.
			for(TrailerConnection connection : hookupEntity.getTowingConnections()){
				if(connection.hookupEntity.equals(hitchEntity) || connection.hookupVehicle.equals(hitchEntity)){
					return false;
				}
			}
			return true;
		} 
	}
	
	/**
	 * Method block for connecting a trailer to this entity.
	 */
	public void connectTrailer(TrailerConnection connection){
		towingConnections.add(connection);
		connection.hookupEntity.connectAsTrailer(connection);
		if(!world.isClient()){
			InterfacePacket.sendToAllClients(new PacketEntityTrailerChange(connection, true));
		}
	}
	
	/**
	 * Method block for disconnecting the trailer with the passed-in connection from this entity.
	 */
	public void disconnectTrailer(TrailerConnection connection){
		towingConnections.removeIf(otherConnection -> otherConnection.hookupEntity.equals(connection.hookupEntity));
		connection.hookupEntity.disconnectAsTrailer();
		if(!world.isClient()){
			InterfacePacket.sendToAllClients(new PacketEntityTrailerChange(connection, false));
		}
	}
	
	/**
	 * Method block for connecting this entity as a trailer to another entity.
	 * Do NOT call this to actually connect the trailer.  Instead, call {@link #connectTrailer(TrailerConnection)}
	 * on the towing entity to haul this trailer.  This method is purely for maintaining states.
	 */
	public void connectAsTrailer(TrailerConnection connection){
		towedByConnection = connection;
		updateAnglesToTowed();
	}
	
	/**
	 * Method block for disconnecting this entity trailer.
	 * Do NOT call this to actually connect the trailer.  Instead, call {@link #disconnectTrailer(TrailerConnection)}
	 * on the towing entity to disconnect this trailer.  This method is purely for maintaining states.
	 * No parameter for this block as there's only one hookup option.
	 */
	public void disconnectAsTrailer(){
		towedByConnection = null;
	}
	
	/**
	 * Returns the towing connections for this entity.  Normally just returns the connections,
	 * but can be overriden to return others.
	 */
	public Set<TrailerConnection> getTowingConnections(){
		return towingConnections;
	}
	
	/**
	 * Method-block for disconnecting all connections from this entity.  Used when this
	 * entity is removed from the world to prevent lingering connections.  Mainly done in item form,
	 * as during removal it will be marked invalid, so all entities connected to it will automatically
	 * disconnect; this just ensures it won't try to re-connect to those entities if re-spawned.
	 * As such, this method does NOT send packets to clients as it's assumed the entity will be gone
	 * on those clients by the time the packet arrives.
	 */
	public void disconnectAllConnections(){
		towingConnections.clear();
		towedByConnection = null;
	}
	
	/**
	 * Helper method for aligning trailer connections.  Used to prevent yaw mis-alignments.
	 */
	protected void updateAnglesToTowed(){
		//Need to set angles for mounted/restricted connections.
		if(towedByConnection.hitchConnection.mounted || towedByConnection.hitchConnection.restricted){
			angles.y = towedByConnection.hitchEntity.angles.y;
			if(towedByConnection.hitchConnection.mounted){
				angles.add(towedByConnection.hitchConnection.rot);
			}
			prevAngles.y = angles.y;
			
			//Also set trailer yaw.
			for(TrailerConnection trailerConnection : towingConnections){
				trailerConnection.hookupVehicle.updateAnglesToTowed();
			}
		}
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setPoint3ds("savedRiderLocations", locationRiderMap.keySet());
		data.setBoolean("locked", locked);
		if(ownerUUID != null){
			data.setUUID("ownerUUID", ownerUUID);
		}
		
		//Save towing data.
		if(towedByConnection != null){
			data.setData("towedByConnection", towedByConnection.getData());
		}
		
		int towingConnectionIndex = 0;
		for(TrailerConnection towingEntry : towingConnections){
			data.setData("towingConnection" + (towingConnectionIndex++), towingEntry.getData());
		}
		data.setInteger("towingConnectionCount", towingConnectionIndex);
		
		if(definition.instruments != null){
			String[] instrumentsInSlots = new String[definition.instruments.size()];
			for(int i=0; i<instrumentsInSlots.length; ++i){
				if(instruments.containsKey(i)){
					data.setString("instrument" + i + "_packID", instruments.get(i).definition.packID);
					data.setString("instrument" + i + "_systemName", instruments.get(i).definition.systemName);
				}
			}
		}
		return data;
	}
	
	/**
	 * Emum for easier functions for owner states.
	 */
	public static enum PlayerOwnerState{
		USER,
		OWNER,
		ADMIN;
	}
	
	/**
	 * Emum for easier functions for trailer connections.
	 */
	public static enum EntityConnectionResult{
		NO_TRAILER_NEARBY,
		TRAILER_TOO_FAR,
		TRAILER_WRONG_HITCH,
		TRAILER_CONNECTED;
	}
}
