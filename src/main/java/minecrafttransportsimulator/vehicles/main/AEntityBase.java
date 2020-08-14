package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mcinterface.InterfaceNetwork;
import mcinterface.WrapperEntity;
import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packets.instances.PacketEntityRiderChange;

/**Base entity class.  This class contains the most basic code for entities,
 * as well as some basic variables and methods for movement and save/load operations.
 * The constructor in this class is called both on servers and clients.  At this time,
 * the data to create this entity will be present, unlike MC where NBT is held from clients.
 * Because of this, no checks are required in the code to ensure server-client syncing has
 * occurred, as this entity class will not be constructed until such handshakes are performed.
 * Do note that this means that for a brief period on the client-side, this class will not exist,
 * despite there being an active builder created.
 * <br><br>
 * Also note that this entity is never "loads" saved data.  Rather, it is created from it.
 * This means that there is no method call to load properties from data. Instead, data required
 * for loading will be passed-in to the constructor.  This data should be used to create the entity
 * in its loaded state.  For saving, which can happen multiple times in the entity's lifetime,
 * {@link #save(WrapperNBT)} is called.  All data required in the constructor should be saved here.
 * 
 * 
 * @author don_bruce
 */
public abstract class AEntityBase{
	/**Internal counter for entity IDs.  Increments each time an entity is created**/
	private static int idCounter = 0;
	/**Map of created entities.  Keyed by their ID.  Note: invalid entities are NOT removed from this map as IDs don't get re-used.**/
	public static Map<Integer, AEntityBase> createdEntities = new HashMap<Integer, AEntityBase>();
	/**A general ID for this entity.  This is set when this entity is loaded, and changes between games.  Used for client/server syncing.**/
	public final int lookupID;
	/**A unique ID for this entity.  This is only set when this entity is first spawned, and never changes, even on save/load operations.**/
	public final String uniqueUUID;
	
	public final Point3d position;
	public final Point3d prevPosition;
	public final Point3d motion;
	public final Point3d prevMotion;
	public final Point3d angles;
	public final Point3d prevAngles;
	public final Point3d rotation;
	public final Point3d prevRotation;
	public final WrapperWorld world;
	
	/**True as long as this entity is part of the world and being ticked.**/
	public boolean isValid = true;
	
	/**Counter for how many ticks this entity has existed in the world.  Realistically, it's the number of updates.**/
	public long ticksExisted;
	
	
	/**List of bounding boxes that should be used for collision of other entities with this entity.**/
	public List<BoundingBox> collisionBoxes = new ArrayList<BoundingBox>();
	
	/**List of bounding boxes that should be used for interaction of other entities with this entity.**/
	public List<BoundingBox> interactionBoxes = new ArrayList<BoundingBox>();
	
	/**Maps relative position locations to riders riding at those positions.  Only one rider
	 * may be present per position.  Positions should be modified via mutable modification to
	 * avoid modifying this map.  The only modifications should be done when a rider is 
	 * mounting/dismounting this entity and we don't want to track them anymore.  Note that
	 * this map contains all possible places riders can sit.  And not all keys will have non-null values.
	 * While you are free to read this map, all modifications should be through the method calls in this class.
	 **/
	public LinkedHashMap<Point3d, WrapperEntity> locationsToRiders = new LinkedHashMap<Point3d, WrapperEntity>();
	
	/**Maps riders to their relative locations.  This is an inverse of {@link #locationsToRiders},
	 * in that the key is the rider, not the position.  Note that all riders riding this entity
	 * will be in this map, but not all possible riding spots will have a rider.  This is the key
	 * difference between this map and the location map, and why we don't use a BiMap here.
	 * While you are free to read this map, all modifications should be through the method calls in this class.
	 **/
	public LinkedHashMap<WrapperEntity, Point3d> ridersToLocations = new LinkedHashMap<WrapperEntity, Point3d>();
	
	public AEntityBase(WrapperWorld world, WrapperNBT data){
		this.lookupID = world.isClient() ? data.getInteger("lookupID") : idCounter++;
		this.uniqueUUID = data.getString("uniqueUUID").isEmpty() ? UUID.randomUUID().toString() : data.getString("uniqueUUID"); 
		this.world = world;
		this.position = data.getPoint3d("position");
		this.prevPosition = position.copy();
		this.motion = data.getPoint3d("motion");
		this.prevMotion = motion.copy();
		this.angles = data.getPoint3d("angles");
		this.prevAngles = angles.copy();
		this.rotation = data.getPoint3d("rotation");
		this.prevRotation = rotation.copy();
		
		//Load rider positions.  We don't have riders here yet, so just make the locations.
		//Riders come from the Builder class after construction as that class saves them.
		for(int riderIndex=0; riderIndex<data.getInteger("totalRiderLocations"); ++riderIndex){
			locationsToRiders.put(data.getPoint3d("riderLocation" + riderIndex), null);
		}
		
		createdEntities.put(lookupID, this);
	}
	
	 /**
	 * Called to update this entity.  Value of previous variables are set here by default, but
	 * extra functionality can and should be added in sub-classes.
	 */
	public void update(){
		++ticksExisted;
		prevPosition.setTo(position);
		prevMotion.setTo(motion);
		prevAngles.setTo(angles);
		prevRotation.setTo(rotation);
	}
	
	/**
	 *  Called to update the passed-in rider position.  This gets called after the update loop,
	 *  as the entity needs to move to its new position before we can know where the
	 *  riders of said entity will be.
	 */
	public void updateRiders(){
		//Update riding entities.
		Iterator<WrapperEntity> riderIterator = ridersToLocations.keySet().iterator();
		while(riderIterator.hasNext()){
			WrapperEntity rider = riderIterator.next();
			Point3d riderPosition = ridersToLocations.get(rider);
			if(rider.isValid()){
				rider.setPosition(riderPosition);
			}else{
				//Remove invalid rider.
				removeRider(rider, riderIterator);
			}
		}
	}
	
	/**
	 *  Called to add a rider to this entity.  Passed-in point is the point they
	 *  should try to ride.  If this isn't possible, return false.  Otherwise,
	 *  return true.  Call this ONLY on the server!  Packets are sent to clients
	 *  for syncing so calling this on clients will result in Bad Stuff.
	 *  Note that null should be passed-in when re-loading this entity from saved
	 *  data.  In this case, the rider will be placed in the next free slot.
	 */
	public boolean addRider(WrapperEntity rider, Point3d riderLocation){
		if(riderLocation != null){
			if(locationsToRiders.containsKey(riderLocation)){
				//We already have a rider in this location.
				return false;
			}else{
				//Update maps.  First check if the rider is already riding and change their location if so.
				if(ridersToLocations.containsKey(rider)){
					ridersToLocations.put(rider, riderLocation);
				}else{
					//This rider wasn't riding this entity before now.  Update their yaw to match
					//the yaw of this entity to prevent 360+ rotation offsets.
					rider.setYaw(angles.y);
				}
				if(!world.isClient()){
					InterfaceNetwork.sendToClientsTracking(new PacketEntityRiderChange(this, rider, riderLocation), this);
				}
				return true;
			}
		}else{
			for(Point3d location : locationsToRiders.keySet()){
				if(locationsToRiders.get(location) == null){
					riderLocation = location;
					break;
				}
			}
			if(riderLocation != null){
				return addRider(rider, riderLocation);
			}else{
				return false;
			}
		}
	}
	
	/**
	 *  Called to remove the passed-in rider from this entity.
	 *  Passed-in iterator is optional, but MUST be included if this is called inside a loop
	 *  that's iterating over {@link #ridersToLocations} or you will get a CME!
	 */
	public void removeRider(WrapperEntity rider, Iterator<WrapperEntity> iterator){
		if(ridersToLocations.containsKey(rider)){
			locationsToRiders.remove(ridersToLocations.get(rider));
			if(iterator != null){
				iterator.remove();
			}else{
				ridersToLocations.remove(rider);
			}
			if(!world.isClient()){
				InterfaceNetwork.sendToClientsTracking(new PacketEntityRiderChange(this, rider, null), this);
			}
		}
	}
	
	/**
	 *  Called when the entity is attacked.
	 *  This should ONLY be called on the server; clients will sync via packets.
	 */
	public void attack(Damage damage){}
	
	/**
	 *  This method returns true if this entity is lit up.  Used to send lighting status to various
	 *  systems for rendering.  Note that this does NOT imply that this entity is bright enough to make
	 *  its surroundings lit up.  Rather, this simply means there is a light on this entity somewhere.
	 */
	public abstract boolean isLitUp();
	
	/**
	 *  Called to render this entity.  No actual rendering should be done in this method, as doing so
	 *  could result in classed being imported during object instantiation on the server for graphics
	 *  libraries that do not exist.  Instead, all render calls should be forwarded to self-contained
	 *  classes that do rendering.
	 */
	public abstract void render(float partialTicks);
	
	/**
	 *  Called when the entity needs to be saved to disk.  The passed-in wrapper
	 *  should be written to at this point with any data needing to be saved.
	 */
	public void save(WrapperNBT data){
		data.setInteger("lookupID", lookupID);
		data.setString("uniqueUUID", uniqueUUID);
		data.setPoint3d("position", position);
		data.setPoint3d("motion", motion);
		data.setPoint3d("angles", angles);
		data.setPoint3d("rotation", rotation);
		
		//Save rider positions.  That way riders don't get moved to other locations on world save/load.
		int riderIndex = 0;
		for(Point3d riderLocation : locationsToRiders.keySet()){
			data.setPoint3d("riderLocation" + riderIndex++, riderLocation);
		}
		data.setInteger("totalRiderLocations", riderIndex);
	}
}
