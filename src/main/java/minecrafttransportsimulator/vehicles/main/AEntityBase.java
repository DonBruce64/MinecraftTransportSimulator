package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
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
	private static int idCounter = 1;
	/**List of created entities.  This is a list as it's possible for clients to have multiple identical entities.
	 * This happens when mods like Optifine or The One Probe do their janky hacks.**/
	public static List<AEntityBase> createdClientEntities = new ArrayList<AEntityBase>();
	/**Like {@link #createdClientEntities}, but on the server.**/
	public static List<AEntityBase> createdServerEntities = new ArrayList<AEntityBase>();
	
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
	public final IWrapperWorld world;
	
	/**Wrapper reference for interfacing with wrapper systems.**/
	public final IWrapperEntity wrapper;
	
	/**True as long as this entity is part of the world and being ticked.**/
	public boolean isValid = true;
	
	/**Counter for how many ticks this entity has existed in the world.  Realistically, it's the number of updates.**/
	public long ticksExisted;
	
	
	/**List of bounding boxes that should be used for collision of other entities with this entity.**/
	public List<BoundingBox> collisionBoxes = new ArrayList<BoundingBox>();
	
	/**List of bounding boxes that should be used for interaction of other entities with this entity.**/
	public List<BoundingBox> interactionBoxes = new ArrayList<BoundingBox>();
	
	/**List of all possible locations for riders on this entity.  For the actual riders in these positions,
	 * see the map.  This list is only used to allow for querying of valid locations for placing riders.
	 * This should be populated prior to trying to load riders, so ideally this will be populated during construction.
	 * Note that these values are shared as keys in the rider map, so if you change them, you will no longer have
	 * hash equality in the keys.  If you need to interface with the map with a new Point3d object, you should do equality
	 * checks on this list to find the "same" point and use that in map operations to ensure hash-matching of the map.
	 **/
	public Set<Point3d> ridableLocations = new HashSet<Point3d>();
	
	/**List of locations where rider were last save.  This is used to re-populate riders on reloads.
	 * It can be assumed that riders will be re-added in the same order the location list was saved.
	 **/
	public List<Point3d> savedRiderLocations = new ArrayList<Point3d>();
	
	/**Maps relative position locations to riders riding at those positions.  Only one rider
	 * may be present per position.  Positions should be modified via mutable modification to
	 * avoid modifying this map.  The only modifications should be done when a rider is 
	 * mounting/dismounting this entity and we don't want to track them anymore.
	 * While you are free to read this map, all modifications should be through the method calls in this class.
	 **/
	public BiMap<Point3d, IWrapperEntity> locationRiderMap = HashBiMap.create();
	
	public AEntityBase(IWrapperWorld world, IWrapperEntity wrapper, IWrapperNBT data){
		this.lookupID = world.isClient() ? data.getInteger("lookupID") : idCounter++;
		this.uniqueUUID = data.getString("uniqueUUID").isEmpty() ? UUID.randomUUID().toString() : data.getString("uniqueUUID"); 
		this.world = world;
		this.wrapper = wrapper;
		this.position = data.getPoint3d("position");
		this.prevPosition = position.copy();
		this.motion = data.getPoint3d("motion");
		this.prevMotion = motion.copy();
		this.angles = data.getPoint3d("angles");
		this.prevAngles = angles.copy();
		this.rotation = data.getPoint3d("rotation");
		this.prevRotation = rotation.copy();
		
		//Load saved rider positions.  We don't have riders here yet, so just make the locations.
		//Riders come from the Builder class after construction as that class saves them.
		for(int riderIndex=0; riderIndex<data.getInteger("totalSavedRiderLocations"); ++riderIndex){
			savedRiderLocations.add(data.getPoint3d("savedRiderLocation" + riderIndex));
		}
		
		if(world.isClient()){
			createdClientEntities.add(this);
		}else{
			createdServerEntities.add(this);
		}
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
	 *  Called to update the passed-in rider.  This gets called after the update loop,
	 *  as the entity needs to move to its new position before we can know where the
	 *  riders of said entity will be.
	 */
	public void updateRider(IWrapperEntity rider, Iterator<IWrapperEntity> iterator){
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
	public boolean addRider(IWrapperEntity rider, Point3d riderLocation){
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
				MasterLoader.networkInterface.sendToAllClients(new PacketEntityRiderChange(this, rider, riderLocation));
			}
			return true;
		}
	}
	
	/**
	 *  Called to remove the passed-in rider from this entity.
	 *  Passed-in iterator is optional, but MUST be included if this is called inside a loop
	 *  that's iterating over {@link #ridersToLocations} or you will get a CME!
	 */
	public void removeRider(IWrapperEntity rider, Iterator<IWrapperEntity> iterator){
		if(locationRiderMap.containsValue(rider)){
			if(iterator != null){
				iterator.remove();
			}else{
				locationRiderMap.inverse().remove(rider);
			}
			if(!world.isClient()){
				rider.setRiding(null);
				MasterLoader.networkInterface.sendToAllClients(new PacketEntityRiderChange(this, rider, null));
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
	public void save(IWrapperNBT data){
		data.setInteger("lookupID", lookupID);
		data.setString("uniqueUUID", uniqueUUID);
		data.setPoint3d("position", position);
		data.setPoint3d("motion", motion);
		data.setPoint3d("angles", angles);
		data.setPoint3d("rotation", rotation);
		
		//Save rider positions.  That way the entity knows where the rider is on world re-load.
		int riderIndex = 0;
		for(Point3d riderLocation : locationRiderMap.keySet()){
			data.setPoint3d("savedRiderLocation" + riderIndex++, riderLocation);
		}
		data.setInteger("totalSavedRiderLocations", riderIndex);
	}
}
