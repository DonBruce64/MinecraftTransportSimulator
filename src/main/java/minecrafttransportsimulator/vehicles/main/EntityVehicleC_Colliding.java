package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mcinterface.InterfaceGame;
import mcinterface.WrapperBlock;
import mcinterface.WrapperEntity;
import mcinterface.WrapperItemStack;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.items.instances.ItemWrench;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleDoor;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartSeat;

/**Now that we have an existing vehicle its time to add the ability to collide with it,
 * and for it to do collision with other entities in the world.  This is where collision
 * bounds are added, as well as the mass of the entity is calculated, as that's required
 * for collision physics forces.  We also add vectors here for the vehicle's orientation,
 * as those are required for us to know how the vehicle collided in the first place.
 * 
 * @author don_bruce
 */


abstract class EntityVehicleC_Colliding extends EntityVehicleB_Rideable{
	
	//Internal states.
	private float hardnessHitThisTick = 0;
	public double airDensity;
	public double currentMass;
	public double velocity;
	public double axialVelocity;
	public final Point3d headingVector = new Point3d(0, 0, 0);
	public final Point3d verticalVector = new Point3d(0, 0, 0);
	public final Point3d sideVector = new Point3d(0, 0, 0);
	public final Point3d normalizedVelocityVector = new Point3d(0, 0, 0);
	public final Set<String> doorsOpen = new HashSet<String>();
	
	//Constants
	private final float PART_SLOT_HITBOX_WIDTH = 0.75F;
	private final float PART_SLOT_HITBOX_HEIGHT = 2.25F;
	
	//Boxes used for collision and interaction with this vehicle.
	public final List<BoundingBox> vehicleCollisionBoxes = new ArrayList<BoundingBox>();
	public final Map<APart, List<BoundingBox>> partCollisionBoxes = new HashMap<APart, List<BoundingBox>>();
	public final List<BoundingBox> blockCollisionBoxes = new ArrayList<BoundingBox>();
	public final List<BoundingBox> partInteractionBoxes = new ArrayList<BoundingBox>();
	public final Map<BoundingBox, VehiclePart> partSlotBoxes = new HashMap<BoundingBox, VehiclePart>();
	public final Map<BoundingBox, VehiclePart> activePartSlotBoxes = new HashMap<BoundingBox, VehiclePart>();
	public final Map<BoundingBox, VehicleDoor> doorBoxes = new HashMap<BoundingBox, VehicleDoor>();
	
	
	public EntityVehicleC_Colliding(WrapperWorld world, WrapperNBT data){
		super(world, data);
		
		//Create the initial part slots.
		recalculatePartSlots();
		
		//Create initial collision boxes.  Needed to test spawn logic.
		for(int i=0; i<definition.collision.size(); ++i){
			VehicleCollisionBox boxDefinition = definition.collision.get(i);
			BoundingBox newBox = new BoundingBox(boxDefinition.pos, boxDefinition.pos.copy(), boxDefinition.width/2D, boxDefinition.height/2D, boxDefinition.width/2D, boxDefinition.collidesWithLiquids, boxDefinition.isInterior);
			vehicleCollisionBoxes.add(newBox);
			collisionBoxes.add(newBox);
			if(!newBox.isInterior && !ConfigSystem.configObject.general.noclipVehicles.value){
				blockCollisionBoxes.add(newBox);
			}
		}
		
		//Create door boxes, and set states based on saved data.
		if(definition.doors != null){
			doorsOpen.clear();
			for(VehicleDoor door : definition.doors){
				BoundingBox box = new BoundingBox(door.closedPos, door.closedPos.copy(), door.width/2D, door.height/2D, door.width/2D, false, true);
				doorBoxes.put(box, door);
				collisionBoxes.add(box);
				if(data.getBoolean("doorsOpen_" + door.name)){
					doorsOpen.add(door.name);
				}
			}
		}
	}
	
	@Override
	public void update(){
		super.update();
		//Set vectors to current velocity and orientation.
		headingVector.set(0D, 0D, 1D).rotateFine(angles);
		verticalVector.set(0D, 1D, 0D).rotateFine(angles);
		sideVector.setTo(verticalVector.crossProduct(headingVector));
		normalizedVelocityVector.setTo(motion).normalize();
		velocity = motion.length();
		axialVelocity = Math.abs(motion.dotProduct(headingVector));
		
		//Update mass.
		if(definition != null){
			currentMass = getCurrentMass();
			airDensity = 1.225*Math.pow(2, -position.y/(500D*world.getMaxHeight()/256D));
		}
		
		//Update vehicle collision boxes.
		for(int i=0; i<definition.collision.size(); ++i){
			vehicleCollisionBoxes.get(i).updateToEntity(this);
		}
		
		//Update part collision boxes.
		for(APart part : partCollisionBoxes.keySet()){
			for(BoundingBox box : partCollisionBoxes.get(part)){
				box.updateToEntity(this);
			}
		}
		
		//Update door collision boxes.
		for(Entry<BoundingBox, VehicleDoor> doorEntry : doorBoxes.entrySet()){
			if(doorsOpen.contains(doorEntry.getValue().name)){
				doorEntry.getKey().globalCenter.setTo(doorEntry.getValue().openPos).rotateFine(angles).add(position);
			}else{
				doorEntry.getKey().globalCenter.setTo(doorEntry.getValue().closedPos).rotateFine(angles).add(position);
			}
		}
		
		//Update part slot box positions.
		for(BoundingBox box : partSlotBoxes.keySet()){
			box.updateToEntity(this);
		}
		
		//Clear out interaction and slot boxes, as some boxes may not be added this tick depending on various factors.
		interactionBoxes.clear();
		partInteractionBoxes.clear();
		
		//Add active part slots to slot boxes.
		//Only do this on clients; servers always have all boxes active to handle clicks.
		//Boxes added on clients depend on what the player is holding.
		//We add these before part boxes so the player can click them before clicking a part.
		if(world.isClient()){
			activePartSlotBoxes.clear();
			WrapperPlayer player = InterfaceGame.getClientPlayer();
			AItemBase heldItem = player.getHeldItem();
			if(heldItem instanceof ItemPart){
				for(Entry<BoundingBox, VehiclePart> partSlotBoxEntry : partSlotBoxes.entrySet()){
					ItemPart heldPart = (ItemPart) heldItem;
					//Does the part held match this packPart?
					if(partSlotBoxEntry.getValue().types.contains(heldPart.definition.general.type)){
						//Does a door not exist, or is at least open?  Or are we riding this vehicle?
						if(partSlotBoxEntry.getValue().linkedDoor == null || doorsOpen.contains(partSlotBoxEntry.getValue().linkedDoor) || this.equals(player.getEntityRiding())){
							//Part matches.  Add the box.  Set the box bounds to the generic box, or the
							//special bounds of the custom part if we're holding one.
							BoundingBox box = partSlotBoxEntry.getKey();
							if(heldPart.definition.general.type.equals("custom")){
								box.widthRadius = heldPart.definition.custom.width/2D;
								box.heightRadius = heldPart.definition.custom.height/2D;
								box.depthRadius = heldPart.definition.custom.width/2D;
							}else{
								box.widthRadius = PART_SLOT_HITBOX_WIDTH/2D;
								box.heightRadius = PART_SLOT_HITBOX_HEIGHT/2D;
								box.depthRadius = PART_SLOT_HITBOX_WIDTH/2D;
							}
							activePartSlotBoxes.put(partSlotBoxEntry.getKey(), partSlotBoxEntry.getValue());
						}
					}
				}
			}
		}
		
		//Add all the active open slot boxes to the interaction frame.
		interactionBoxes.addAll(activePartSlotBoxes.keySet());
		
		//Part interaction boxes are linked to the part's bounding box, so we don't need to update those.
		//Rather, the part will update them on it's own update call.
		//However, we do need to decide which interaction boxes we add to the interaction list.
		//While we add all the boxes on the server, we only add some on the clients.
		//This is dependent on what the current player entity is holding.
		for(APart part : parts){
			if(world.isClient()){
				WrapperPlayer clientPlayer = InterfaceGame.getClientPlayer();
				//If the part is fake, don't add it.
				if(part.isFake()){
					continue;
				}
				
				//If the part is a seat, and we are riding it, don't add it.
				//This keeps us from clicking our own seat when we want to click other things.
				if(part instanceof PartSeat){
					if(part.placementOffset.equals(locationRiderMap.inverse().get(clientPlayer))){
						continue;
					}
				}
				//If the player is holding a wrench, and the part has children, don't add it.
				//Player shouldn't be able to wrench parts with children.
				if(clientPlayer.getHeldItem() instanceof ItemWrench && !part.childParts.isEmpty()){
					continue;
				}
				
				//If the part is linked to a door, and that door isn't open, and the player isn't in the vehicle, don't add it.
				//This prevents the player from interacting with things from outside the vehicle when the door is shut, but lets
				//them move around inside the vehicle.
				if(part.vehicleDefinition.linkedDoor != null && !doorsOpen.contains(part.vehicleDefinition.linkedDoor) && !this.equals(clientPlayer.getEntityRiding())){
					continue;
				}
			}
			
			//Conditions to add have been met, do so.
			interactionBoxes.add(part.boundingBox);
			partInteractionBoxes.add(part.boundingBox);
		}
		
		//Now add the collision boxes.  These go last as we want to avoid clicking on them and they should be checked last.
		//We do need to add these, however, as the player can interact with collision boxes to open inventories or wrench
		//the vehicle.  In general, the interaction layer is everything the player can click, which includes what they can touch.
		interactionBoxes.addAll(collisionBoxes);
		
		//Set hardness hit this tick to 0 to reset collision force calculations.
		hardnessHitThisTick = 0;
	}
	
	@Override
	public void attack(Damage damage){
		//This is called if we attack the vehicle with something, rather than click it with an item.
		//This attack can come from a player with a hand-held item, or a projectile such as an arrow.
		//If the bounding box attacked corresponds to a part, forward the attack to that part for calculation.
		APart part = getPartAtLocation(damage.box.localCenter);
		if(part != null){
			part.attack(damage);
		}
	}
	
	@Override
	public void addPart(APart part, boolean ignoreCollision){
		super.addPart(part, ignoreCollision);
		//Add part to collision map if it has collision.
		if(part.definition.collision != null && part.definition.collision.size() > 0){
			partCollisionBoxes.put(part, new ArrayList<BoundingBox>());
			for(VehicleCollisionBox boxDefinition : part.definition.collision){
				BoundingBox newBox = new BoundingBox(boxDefinition.pos, boxDefinition.pos.copy().add(part.totalOffset).add(position), boxDefinition.width, boxDefinition.height, boxDefinition.width, boxDefinition.collidesWithLiquids, boxDefinition.isInterior);
				partCollisionBoxes.get(part).add(newBox);
				collisionBoxes.add(newBox);
				if(!newBox.isInterior){
					blockCollisionBoxes.add(newBox);
				}
			}
		}
		
		//Recalculate slots.
		recalculatePartSlots();
	}
	
	@Override
	public void removePart(APart part, Iterator<APart> iterator){
		super.removePart(part, iterator);
		//Remove collision boxes from maps.
		if(partCollisionBoxes.containsKey(part)){
			for(BoundingBox box : partCollisionBoxes.get(part)){
				collisionBoxes.remove(box);
				blockCollisionBoxes.remove(box);
			}
			partCollisionBoxes.remove(part);
		}
		
		//Recalculate slots.
		recalculatePartSlots();
	}
	
	/**
	 * Call to re-create the list of all valid part slot boxes.
	 * This should be called after part addition or part removal.
	 * Also must be called at construction time to create the initial slot set.
	 */
	private void recalculatePartSlots(){
		partSlotBoxes.clear();
		for(Entry<Point3d, VehiclePart> packPartEntry : getAllPossiblePackParts().entrySet()){
			if(getPartAtLocation(packPartEntry.getKey()) == null){
				BoundingBox newSlotBox = new BoundingBox(packPartEntry.getKey(), packPartEntry.getKey().copy().rotateCoarse(angles).add(position), 0, 0, 0, false, false); 
				partSlotBoxes.put(newSlotBox, packPartEntry.getValue());
				if(!world.isClient()){
					activePartSlotBoxes.put(newSlotBox, packPartEntry.getValue());
				}
			}
		}
	}
	
	/**
	 * Checks collisions and returns the collision depth for a box.
	 * Returns -1 if collision was hard enough to destroy the vehicle.
	 * Otherwise, we return the collision depth in the specified axis.
	 */
	protected double getCollisionForAxis(BoundingBox box, boolean xAxis, boolean yAxis, boolean zAxis){
		//Get the motion the entity is trying to move, and add it to the passed-in box value.
		Point3d collisionMotion = motion.copy().multiply(SPEED_FACTOR);
		
		//If we collided, so check to see if we can break some blocks or if we need to explode.
		//Don't bother with this logic if it's impossible for us to break anything.
		if(box.updateCollidingBlocks(world, collisionMotion)){
			for(WrapperBlock block : box.collidingBlocks){
				if(!block.isLiquid() && block.getHardness() <= velocity*currentMass/250F && block.getHardness() >= 0){
					if(ConfigSystem.configObject.general.blockBreakage.value){
						hardnessHitThisTick += block.getHardness();
						motion.multiply(Math.max(1.0F - block.getHardness()*0.5F/((1000F + currentMass)/1000F), 0.0F));
						if(!world.isClient()){
							if(ticksExisted > 500){
								world.destroyBlock(block.getPosition());
							}else{
								motion.set(0D, 0D, 0D);
								return -1;
							}
						}
					}else{
						hardnessHitThisTick = 0;
						motion.set(0D, 0D, 0D);
					}
				}
			}
			
			if(hardnessHitThisTick > currentMass/(0.75 + velocity)/250F){
				if(!world.isClient()){
					destroyAtPosition(box.globalCenter);
				}
				return -1;
			}else if(xAxis){
				return box.currentCollisionDepth.x;
			}else if(yAxis){
				return box.currentCollisionDepth.y;
			}else if(zAxis){
				return box.currentCollisionDepth.z;
			}else{
				throw new IllegalArgumentException("ERROR: Collision requested but no axis was specified!");
			}
		}else{
			return 0;
		}
	}
	
	/**
	 * Call this to remove this vehicle.  This should be called when the vehicle has crashed, as it
	 * ejects all parts and damages all players.  Explosions may not occur in crashes depending on config 
	 * settings or a lack of fuel or explodable cargo.  Call only on the SERVER as this is for item-spawning 
	 * code and player damage code.
	 */
	public void destroyAtPosition(Point3d position){
		this.isValid = false;
		//Remove all parts from the vehicle and place them as items.
		for(APart part : parts){
			if(part.getItem() != null){
				world.spawnItem(part.getItem(), part.getData(), part.worldPos);
			}
		}
		
		//Also drop some crafting ingredients as items.
		for(WrapperItemStack craftingStack : WrapperItemStack.parseFromJSON(definition)){
			if(Math.random() < ConfigSystem.configObject.damage.crashItemDropPercentage.value){
				world.spawnItemStack(craftingStack, position);
			}
		}
		
		//Damage all riders, including the controller.
		WrapperPlayer controller = getController();
		Damage controllerCrashDamage = new Damage("crash", ConfigSystem.configObject.damage.crashDamageFactor.value*velocity*20, null, null);
		Damage passengerCrashDamage = new Damage("crash", ConfigSystem.configObject.damage.crashDamageFactor.value*velocity*20, null, controller);
		for(WrapperEntity rider : locationRiderMap.values()){
			if(rider.equals(controller)){
				rider.attack(controllerCrashDamage);
			}else{
				rider.attack(passengerCrashDamage);
			}
		}
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		//Save open doors.
		for(String doorName : doorsOpen){
			data.setBoolean("doorsOpen_" + doorName, true);
		}
	}
}
