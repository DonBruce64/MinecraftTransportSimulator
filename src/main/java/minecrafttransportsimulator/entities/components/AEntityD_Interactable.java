package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
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
import minecrafttransportsimulator.jsondefs.AJSONInteractableEntity;
import minecrafttransportsimulator.jsondefs.JSONCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONDoor;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityRiderChange;
import minecrafttransportsimulator.systems.ConfigSystem;

/**Base entity class containing riders and their positions on this entity.  Used for
 * entities that need to keep track of riders and their locations.  This also contains
 * various collision box lists for collision, as riders cannot interact and start riding
 * entities without collision boxes to click.
 * 
 * @author don_bruce
 */
public abstract class AEntityD_Interactable<JSONDefinition extends AJSONInteractableEntity> extends AEntityC_Definable<JSONDefinition>{
	
	/**List of bounding boxes that should be used for collision of other entities with this entity.
	 * This includes {@link #collisionBoxes}, and {@link #blockCollisionBoxes}, but may include others.**/
	public final List<BoundingBox> collisionBoxes = new ArrayList<BoundingBox>();
	
	/**List of bounding boxes that should be used to check collision of this entity with blocks.
	 * This only includes {@link #blockCollisionBoxes}, as it's only for blocks.**/
	public final List<BoundingBox> blockCollisionBoxes = new ArrayList<BoundingBox>();
	
	/**List of bounding boxes that should be used for interaction of other entities with this entity.
	 * This includes all {@link #collisionBoxes} and {@link #doorBoxes} by default, but may include others.**/
	public final List<BoundingBox> interactionBoxes = new ArrayList<BoundingBox>();
	
	/**Map of door boxes.  Key is the box, value is the JSON entry that it was created from.**/
	public final Map<BoundingBox, JSONDoor> doorBoxes = new HashMap<BoundingBox, JSONDoor>();
	
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
	
	/**Locked state.  Locked entities should not be able to be interacted with except by entities riding them,
	 * their owners, or OP players (server admins).
	 **/
	public boolean locked;
	
	/**The ID of the owner of this entity. If this string is empty, it can be assumed that there is no owner.
	 * UUIDs are set at creation time of an entity, and will never change, even on world re-loads.
	 **/
	public String ownerUUID;
	
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
	}
	
	@Override
	public void update(){
		super.update();
		
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
	 *  Called when the entity needs to be saved to disk.  The passed-in wrapper
	 *  should be written to at this point with any data needing to be saved.
	 */
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		data.setPoint3ds("savedRiderLocations", locationRiderMap.keySet());
		data.setBoolean("locked", locked);
		data.setString("ownerUUID", ownerUUID);
	}
}
