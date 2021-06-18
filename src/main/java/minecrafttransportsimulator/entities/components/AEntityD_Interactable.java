package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.TrailerConnection;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.AJSONInteractableEntity;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONConnection;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;
import minecrafttransportsimulator.jsondefs.JSONDoor;
import minecrafttransportsimulator.jsondefs.JSONInstrumentDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityRiderChange;
import minecrafttransportsimulator.packets.instances.PacketEntityTrailerChange;
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
public abstract class AEntityD_Interactable<JSONDefinition extends AJSONInteractableEntity> extends AEntityC_Definable<JSONDefinition>{
	
	/**List of bounding boxes that should be used for collision of other entities with this entity.
	 * This includes {@link #blockCollisionBoxes}, but may include others.**/
	public final List<BoundingBox> collisionBoxes = new ArrayList<BoundingBox>();
	
	/**List of bounding boxes that should be used to check collision of this entity with blocks.**/
	public final List<BoundingBox> blockCollisionBoxes = new ArrayList<BoundingBox>();
	
	/**List of bounding boxes that should be used for interaction of other entities with this entity.
	 * This includes all {@link #collisionBoxes} and {@link #doorBoxes} by default, but may include others.**/
	public final List<BoundingBox> interactionBoxes = new ArrayList<BoundingBox>();
	
	/**Map of door boxes.  Key is the box, value is the JSON entry that it was created from.**/
	public final Map<BoundingBox, JSONDoor> doorBoxes = new HashMap<BoundingBox, JSONDoor>();
	
	/**Set of entities that this entity collided with this tick.  Any entity that is in this set 
	 * should NOT do collision checks with this entity, or infinite loops will occur.
	 * This set should be cleared after all collisions have been checked.**/
	public final Set<AEntityD_Interactable<?>> collidedEntities = new HashSet<AEntityD_Interactable<?>>();
	
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
	
	/**Maps instrument animations to their respective clocks.  Each animation has its own clock, which
	 * will be used when the instrument is rendered and animated.  Clocks should be created and added to
	 * this map as needed.  The map will only be modified when the JSON definition for this entity is
	 * reloaded, in which case all clocks will be reset as the JSON may have changed animations.
	 **/
	public final Map<JSONAnimationDefinition, DurationDelayClock> instrumentAnimationClocks = new HashMap<JSONAnimationDefinition, DurationDelayClock>();
	
	/**Locked state.  Locked entities should not be able to be interacted with except by entities riding them,
	 * their owners, or OP players (server admins).
	 **/
	public boolean locked;
	
	/**The ID of the owner of this entity. If this string is empty, it can be assumed that there is no owner.
	 * UUIDs are set at creation time of an entity, and will never change, even on world re-loads.
	 **/
	public String ownerUUID;
	
	/**Internal flag to prevent this entity from updating until the entity that is towing it has.  If we don't
	 * do this, then there may be a 1-tick de-sync between towing and towed entities if the towed entity gets
	 * updated before the one towing it.
	 **/
	private boolean overrideTowingChecks;
	
	//Connection data.
	public TrailerConnection towedByConnection;
	protected final Set<TrailerConnection> towingConnections = new HashSet<TrailerConnection>();
	private TrailerConnection savedTowedByConnection;
	private final Set<TrailerConnection> savedTowingConnections = new HashSet<TrailerConnection>();
	
	public AEntityD_Interactable(WrapperWorld world, WrapperNBT data){
		super(world, data);
		//Load saved rider positions.  We don't have riders here yet (as those get created later), 
		//so just make the locations for the moment so they are ready when riders are created.
		this.savedRiderLocations.addAll(data.getPoint3ds("savedRiderLocations"));
		this.locked = data.getBoolean("locked");
		this.ownerUUID = data.getString("ownerUUID");
		
		//Create collision boxes.
		if(definition.collision != null){
			for(JSONCollisionBox boxDef : definition.collision){
				BoundingBox newBox = new BoundingBox(boxDef.pos, boxDef.pos.copy(), boxDef.width/2D, boxDef.height/2D, boxDef.width/2D, boxDef.collidesWithLiquids, boxDef.isInterior, true, boxDef.armorThickness);
				collisionBoxes.add(newBox);
				if(!newBox.isInterior && !ConfigSystem.configObject.general.noclipVehicles.value){
					blockCollisionBoxes.add(newBox);
				}
			}
		}
		
		//Create door boxes.
		if(definition.doors != null){
			for(JSONDoor doorDef : definition.doors){
				BoundingBox box = new BoundingBox(doorDef.closedPos, doorDef.closedPos.copy(), doorDef.width/2D, doorDef.height/2D, doorDef.width/2D, false, true, false, 0);
				doorBoxes.put(box, doorDef);
				collisionBoxes.add(box);
			}
		}
		
		//Add collision and door boxes to interaction list.
		interactionBoxes.addAll(collisionBoxes);
		interactionBoxes.addAll(doorBoxes.keySet());
		

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
		
		//Load instruments.
		if(definition.instruments != null){
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
	
	@Override
	public boolean update(){
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
			
			//Update collision boxes.
			for(BoundingBox box : collisionBoxes){
				box.updateToEntity(this, null);
			}
			
			//Update door boxes.
			for(Entry<BoundingBox, JSONDoor> doorEntry : doorBoxes.entrySet()){
				if(variablesOn.contains(doorEntry.getValue().name)){
					doorEntry.getKey().globalCenter.setTo(doorEntry.getValue().openPos).rotateFine(angles).add(position);
				}else{
					doorEntry.getKey().globalCenter.setTo(doorEntry.getValue().closedPos).rotateFine(angles).add(position);
				}
			}
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
	public void onDefinitionReset(){
		super.onDefinitionReset();
		instrumentAnimationClocks.clear();
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
				if(foundConnection != null && !towingConnections.isEmpty()){
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
		}
		
		//Not a towing variable, check others.
		return super.getRawVariableValue(variable, partialTicks);
	}
	
	/**
	 * Called to perform supplemental update logic on this entity.  This should be called after all movement on the
	 * entity has been performed, and is used to do updates that require the new positional logic to be ready.
	 * Calling this before the entity finishes moving will lead to things "lagging" behind the entity.
	 */
	public void updatePostMovement(){
		//If we are towing entities, update them now.
		if(!towingConnections.isEmpty()){
			for(TrailerConnection connection : towingConnections){
				connection.hookupBaseEntity.overrideTowingChecks = true;
				connection.hookupBaseEntity.update();
				connection.hookupBaseEntity.overrideTowingChecks = false;
			}
		}
	}
	
	/**
   	 *  Returns a collection of BoundingBoxes that make up this entity's collision bounds.
   	 */
    public Collection<BoundingBox> getCollisionBoxes(){
    	return collisionBoxes;
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
			rider.setPosition(locationRiderMap.inverse().get(rider));
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
	 *  Called when the entity is attacked.
	 *  This should ONLY be called on the server; clients will sync via packets.
	 */
	public void attack(Damage damage){}
	
	/**
	 * Helper method to add all default instruments to this entity.  These instruments
	 * should be added when the entity is first placed into the world.
	 */
	public void addDefaultInstruments(){
		if(definition.instruments != null){
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
							throw new IllegalArgumentException("Attempted to add defaultInstrument: " + instrumentPackID + ":" + instrumentSystemName + " to: " + definition.packID + ":" + definition.systemName + " but that instrument doesn't exist in the pack item registry.");
						}
					}catch(IndexOutOfBoundsException e){
						throw new IllegalArgumentException("Could not parse defaultInstrument definition: " + packInstrument.defaultInstrument + ".  Format should be \"packId:instrumentName\"");
					}
				}
			}
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
	public EntityConnectionResult checkIfTrailerCanConnect(AEntityD_Interactable<?> hookupEntity, int hitchGroupIndex, int hookupGroupIndex){
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
									Point3d hitchPos = hitchConnection.pos.copy().rotateCoarse(angles).add(position);
									double maxDistance = hitchConnection.distance > 0 ? hitchConnection.distance : 2;
									for(JSONConnection hookupConnection : hookupConnectionGroup.connections){
										Point3d hookupPos = hookupConnection.pos.copy().rotateCoarse(hookupEntity.angles).add(hookupEntity.position);
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
				trailerConnection.hookupBaseEntity.updateAnglesToTowed();
			}
		}
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setPoint3ds("savedRiderLocations", locationRiderMap.keySet());
		data.setBoolean("locked", locked);
		data.setString("ownerUUID", ownerUUID);
		
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
	 * Emum for easier functions for trailer connections.
	 */
	public static enum EntityConnectionResult{
		NO_TRAILER_NEARBY,
		TRAILER_TOO_FAR,
		TRAILER_WRONG_HITCH,
		TRAILER_CONNECTED;
	}
}
