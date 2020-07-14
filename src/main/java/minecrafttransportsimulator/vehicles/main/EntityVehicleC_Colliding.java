package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import mcinterface.BuilderEntity;
import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBBCollective;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.core.ItemWrench;
import minecrafttransportsimulator.items.packs.parts.AItemPart;
import minecrafttransportsimulator.items.packs.parts.ItemPartCustom;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartBarrel;
import minecrafttransportsimulator.vehicles.parts.PartCrate;
import minecrafttransportsimulator.vehicles.parts.PartGun;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**Now that we have an existing vehicle its time to add the ability to collide with it,
 * and for it to do collision with other entities in the world.  This is where collision
 * bounds are added, as well as the mass of the entity is calculated, as that's required
 * for collision physics forces.  We also add vectors here for the vehicle's orientation,
 * as those are required for us to know how the vehicle collided in the first place.
 * 
 * @author don_bruce
 */

@Mod.EventBusSubscriber
abstract class EntityVehicleC_Colliding extends EntityVehicleB_Rideable{
	//Internal collision frames.
	private VehicleAxisAlignedBBCollective collisionFrame;
	private VehicleAxisAlignedBBCollective interactionFrame;
	
	//Boxes used for collision and interaction with this vehicle.
	public final List<VehicleAxisAlignedBB> collisionBoxes = new ArrayList<VehicleAxisAlignedBB>();
	public final List<VehicleAxisAlignedBB> partBoxes = new ArrayList<VehicleAxisAlignedBB>();
	public final List<VehicleAxisAlignedBB> openPartSpotBoxes = new ArrayList<VehicleAxisAlignedBB>();
	public final List<VehicleAxisAlignedBB> interactionBoxes = new ArrayList<VehicleAxisAlignedBB>();
	
	
	
	//Last saved explosion position (used for damage calcs).
	private static Vec3d lastExplosionPosition;

	/**Cached config value for speedFactor.  Saves us from having to use the long form all over.  Not like it'll change in-game...*/
	public final double SPEED_FACTOR = ConfigSystem.configObject.general.speedFactor.value;
	

	//External state control.
	public boolean brakeOn;
	public boolean parkingBrakeOn;
	public boolean locked;
	public String ownerName = "";
	public String displayText = "";
	
	
	//Internal states.
	public byte prevParkingBrakeAngle;
	public byte parkingBrakeAngle;
	public double airDensity;
	public double currentMass;
	public double velocity;
	public double prevVelocity;
	public double normalizedGroundVelocity;
	public Point3d headingVector = new Point3d(0, 0, 0);
	public Point3d normalizedVelocity = new Point3d(0, 0, 0);
	public Point3d verticalVector = new Point3d(0, 0, 0);
	public Point3d sideVector = new Point3d(0, 0, 0);
	
	
	private float hardnessHitThisTick = 0;
	
	public EntityVehicleC_Colliding(BuilderEntity builder, WrapperWorld world, WrapperNBT data){
		super(builder, world, data);
		this.locked = data.getBoolean("locked");
		this.parkingBrakeOn = data.getBoolean("parkingBrakeOn");
		this.brakeOn = data.getBoolean("brakeOn");
		this.ownerName = data.getString("ownerName");
		this.displayText = data.getString("displayText");
		if(displayText.isEmpty()){
			displayText = definition.rendering.defaultDisplayText;
		}
	}
	
	@Override
	public void update(){
		super.update();
		//Set vectors to current velocity and orientation.
		headingVector.set(0D, 0D, 1D).rotateFine(angles);
		normalizedVelocity.set(motion.x, 0D, motion.z);
		normalizedGroundVelocity = normalizedVelocity.dotProduct(headingVector);
		normalizedVelocity.y = motion.y;
		prevVelocity = velocity;
		velocity = Math.abs(normalizedVelocity.dotProduct(headingVector));
		normalizedVelocity.normalize();
		verticalVector = new Point3d(0D, 1D, 0D).rotateFine(rotation);
		sideVector = headingVector.crossProduct(verticalVector);
		
		//Update mass.
		if(definition != null){
			currentMass = getCurrentMass();
			airDensity = 1.225*Math.pow(2, -position.y/(500D*world.getMaxHeight()/256D));
		}
		
		//Update parking brake angle.
		//FIXME remove this with the duration/delay code.
		prevParkingBrakeAngle = parkingBrakeAngle;
		if(parkingBrakeOn && !locked && velocity < 0.25){
			if(parkingBrakeAngle < 30){
				prevParkingBrakeAngle = parkingBrakeAngle;
				++parkingBrakeAngle;
			}
		}else{
			if(parkingBrakeAngle > 0){
				prevParkingBrakeAngle = parkingBrakeAngle;
				--parkingBrakeAngle;
			}
		}
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(definition != null){
			//Make sure the collision bounds for MC are big enough to collide with this entity.
			if(World.MAX_ENTITY_RADIUS < 64){
				World.MAX_ENTITY_RADIUS = 64;
			}
			
			//Update the box lists.
			updateCollisionBoxes();
			hardnessHitThisTick = 0;
		}
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource source, float damage){
		//This is called if we attack the vehicle with something that isn't an interactable item.
		//This attack can come from a player with a hand-held item, or a projectile such as an arrow.
		//In any case, we check to see if the attacker is in the collision box of a part and forward the
		//attack to that part of so.  Note that multiple parts may be hit by the attacker if their speed is
		//fast enough, and sometimes the attack may hit nothing at all.  This is particularly true if the
		//attack is from a player standing still and smacking the vehicle with a weapon.
		if(!world.isRemote){
			if(source.getImmediateSource() != null){
				Entity attacker = source.getImmediateSource();
				for(VehicleAxisAlignedBB partBox : partBoxes){
					//Expand this box by the speed of the projectile just in case the projectile is custom and
					//calls its attack code before it actually gets inside the collision box.
					if(partBox.grow(Math.abs(attacker.motionX), Math.abs(attacker.motionY), Math.abs(attacker.motionZ)).contains(attacker.getPositionVector())){
						APart part = getPartAtLocation(partBox.rel.x, partBox.rel.y, partBox.rel.z);
						if(part != null){
							part.attackPart(source, damage);
						}
					}
				}
			}else{
				//Check if we encountered an explosion.  These don't have entities linked to them, despite TNT being a TE.
				if(lastExplosionPosition != null && source.isExplosion()){
					//Expand the box by the explosion size and attack any parts in it.
					for(VehicleAxisAlignedBB partBox : partBoxes){
						if(partBox.grow(damage).contains(lastExplosionPosition)){
							APart part = getPartAtLocation(partBox.rel.x, partBox.rel.y, partBox.rel.z);
							if(part != null){
								part.attackPart(source, damage);
							}
						}
					}
					lastExplosionPosition = null;
				}
			}
		}
		return true;
	}
	
	/**
	 * This code allows for this vehicle to be "attacked" without an entity present.
	 * Normally the attack code will check the attacking entity to figure out what was
	 * attacked on the vehicle, but for some situations this may not be desirable.
	 * In particular, this is done for the particle-based bullet system as there are
	 * no projectile entities to allow the attack system to automatically calculate
	 * what was attacked on the vehicle.
	 */
	public void attackManuallyAtPosition(double x, double y, double z, DamageSource source, float damage){
		for(VehicleAxisAlignedBB partBox : partBoxes){
			if(partBox.contains(new Vec3d(x, y, z))){
				APart part = getPartAtLocation(partBox.rel.x, partBox.rel.y, partBox.rel.z);
				if(part != null){
					part.attackPart(source, damage);
				}
			}
		}
	}
	
	/**
	 * We need to use explosion events here as we don't know where explosions occur in the world.
	 * This results in them being position-less, so we can't forward them to parts for damage calcs.
	 * Whenever we have an explosion detonated in the world, save it's position.  We can then use it
	 * in {@link #attackEntityFrom(DamageSource, float)} to tell the system which part to attack.
	 */
	@SubscribeEvent
	public static void on(ExplosionEvent.Detonate event){
		if(!event.getWorld().isRemote){
			lastExplosionPosition = event.getExplosion().getPosition();
		}
	}
    
	@Override
	public boolean canBeCollidedWith(){
		//This gets overridden to allow players to interact with this vehicle.
		return true;
	}
	
	@Override
	public boolean canRiderInteract(){
		//Return true here to allow clicks while seated.
        return true;
    }
	
	@Override
	public VehicleAxisAlignedBBCollective getEntityBoundingBox(){
		//Override this to make interaction checks work with the multiple collision points.
		//We return the collision and interaction boxes here as we need a bounding box large enough to encompass both.
		return this.interactionFrame != null ? this.interactionFrame : new VehicleAxisAlignedBBCollective((EntityVehicleF_Physics) this, 1, 1, interactionBoxes);
	}
	
	@Override
    @Nullable
    public VehicleAxisAlignedBBCollective getCollisionBoundingBox(){
		//Override this to make collision checks work with the multiple collision points.
		//We only return collision boxes here as we don't want the player to collide with interaction boxes.
		return this.collisionFrame != null ? this.collisionFrame : new VehicleAxisAlignedBBCollective((EntityVehicleF_Physics) this, 1, 1, collisionBoxes);
    }
    
	/**
	 * Called to populate the collision lists for this entity.
	 * Do NOT call more than once a tick as this operation is complex and
	 * CPU and RAM intensive!
	 */
	private void updateCollisionBoxes(){
		if(this.definition != null){
			//Get all collision boxes and set the bounding collective to encompass all of them.
			collisionBoxes.clear();
			double furthestWidth = 0;
			double furthestHeight = 0;
			
			//First get collision from the vehicle.
			for(VehicleCollisionBox box : definition.collision){
				Point3d boxOffset = new Point3d(box.pos[0], box.pos[1], box.pos[2]);
				Point3d offset = RotationSystem.getRotatedPoint(boxOffset, rotationPitch, rotationYaw, rotationRoll);
				VehicleAxisAlignedBB newBox = new VehicleAxisAlignedBB(offset.add(positionVector), boxOffset, box.width, box.height, box.isInterior, box.collidesWithLiquids);
				collisionBoxes.add(newBox);
				furthestWidth = (float) Math.max(furthestWidth, Math.abs(newBox.rel.x) + box.width/2F);
				furthestHeight = (float) Math.max(furthestHeight, Math.abs(newBox.rel.y) + box.height/2F);
				furthestWidth = (float) Math.max(furthestWidth, Math.abs(newBox.rel.z) + box.width/2F);
			}
			
			//Now get any part collision.
			for(APart part : getVehicleParts()){
				if(part.definition.collision != null){
					for(VehicleCollisionBox box : part.definition.collision){
						Point3d boxOffset = new Point3d(box.pos[0], box.pos[1], box.pos[2]).add(part.totalOffset);
						Point3d offset = RotationSystem.getRotatedPoint(boxOffset, rotationPitch, rotationYaw, rotationRoll);
						VehicleAxisAlignedBB newBox = new VehicleAxisAlignedBB(offset.add(positionVector), boxOffset, box.width, box.height, box.isInterior, box.collidesWithLiquids);
						collisionBoxes.add(newBox);
						furthestWidth = (float) Math.max(furthestWidth, Math.abs(newBox.rel.x) + box.width/2F);
						furthestHeight = (float) Math.max(furthestHeight, Math.abs(newBox.rel.y) + box.height/2F);
						furthestWidth = (float) Math.max(furthestWidth, Math.abs(newBox.rel.z) + box.width/2F);
					}
				}
			}
			
			//Finally, set the collision frame.
			this.collisionFrame = new VehicleAxisAlignedBBCollective((EntityVehicleF_Physics) this, (float) furthestWidth*2F+0.5F, (float) furthestHeight*2F+0.5F, collisionBoxes);
			
			//Add all part boxes to the part box list.
			//The server always adds all boxes, the client omits some.
			//This allows for smoother interaction on clients.
			partBoxes.clear();
			for(APart part : this.getVehicleParts()){
				if(world.isRemote){
					//If the part is a seat, and we are riding it, don't add it.
					//This keeps us from clicking our own seat when we want to click other things.
					if(part instanceof PartSeat){
						if(Minecraft.getMinecraft().player.equals(getRiderForSeat((PartSeat) part))){
							continue;
						}
					}
					//If the player is holding a wrench, and the part has children, don't add it.
					//Player shouldn't be able to wrench parts with children.
					if(Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() instanceof ItemWrench && !part.childParts.isEmpty()){
						continue;
					}
					//If the player is holding a part, and the part isn't a seat, don't add it.
					//This prevents us from clicking on parts when we're trying to place one.
					//Seats are left in because it'd be a pain to switch items.
					//Guns are also left in as the player may be clicking them with a bullet part to load them.
					if(Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() instanceof AItemPart && !(part instanceof PartSeat) && !(part instanceof PartGun)){
						continue;
					}
				}
				partBoxes.add(part.boundingBox);
			}
			
			//Finally, add all possible part boxes that don't have parts to the list.
			//If we are on a server, we add all boxes with a size of 0 to prevent them from being attacked.
			//If we are on a client, we add only the ones that correspond with the item the player is holding.
			openPartSpotBoxes.clear();
			final double PART_SLOT_HITBOX_OFFSET = 0.5D;
			final float PART_SLOT_HITBOX_WIDTH = 0.75F;
			final float PART_SLOT_HITBOX_HEIGHT = 1.75F;
			for(Entry<Point3d, VehiclePart> packPartEntry : getAllPossiblePackParts().entrySet()){
				if(getPartAtLocation(packPartEntry.getKey().x, packPartEntry.getKey().y, packPartEntry.getKey().z) == null){
					if(world.isRemote){
						ItemStack heldStack = Minecraft.getMinecraft().player.getHeldItemMainhand();
						if(heldStack.getItem() instanceof AItemPart){
							AItemPart heldPart = (AItemPart) heldStack.getItem();
							//Does the part held match this packPart?
							if(packPartEntry.getValue().types.contains(heldPart.definition.general.type)){
								//Part matches.  Add the box.  If we are holding a custom part, add that box
								//instead of the generic box.
								if(heldPart instanceof ItemPartCustom){
									Point3d offset = RotationSystem.getRotatedPoint(packPartEntry.getKey(), rotationPitch, rotationYaw, rotationRoll);
									openPartSpotBoxes.add(new VehicleAxisAlignedBB(offset.add(positionVector), packPartEntry.getKey(), heldPart.definition.custom.width, heldPart.definition.custom.height, false, false));
								}else{
									packPartEntry.getKey().add(0D, PART_SLOT_HITBOX_OFFSET, 0D);
									Point3d offset = RotationSystem.getRotatedPoint(packPartEntry.getKey(), rotationPitch, rotationYaw, rotationRoll);
									openPartSpotBoxes.add(new VehicleAxisAlignedBB(offset.add(positionVector), packPartEntry.getKey(), PART_SLOT_HITBOX_WIDTH, PART_SLOT_HITBOX_HEIGHT, false, false));
								}
							}
						}else{
							//We aren't holding a part, do don't even bother adding hitboxes.
							break;
						}
					}else{
						//We are on the server.  Set width and height to 0 to prevent clicking.
						Point3d offset = RotationSystem.getRotatedPoint(packPartEntry.getKey(), rotationPitch, rotationYaw, rotationRoll);
						openPartSpotBoxes.add(new VehicleAxisAlignedBB(offset.add(positionVector), packPartEntry.getKey(), 0, 0, false, false));
					}
				}
			}
			
			//Add all the boxes together and put in the interaction frame.
			//We arrange them in order of importance to influence which ones get checked first.
			interactionBoxes.clear();
			interactionBoxes.addAll(partBoxes);
			interactionBoxes.addAll(openPartSpotBoxes);
			interactionBoxes.addAll(collisionBoxes);
			this.interactionFrame = new VehicleAxisAlignedBBCollective((EntityVehicleF_Physics) this, (float) furthestWidth*2F+0.5F, (float) furthestHeight*2F+0.5F, interactionBoxes);
		}
	}
	
	/**
	 * Checks collisions and returns the collision depth for a box.
	 * Returns -1 if collision was hard enough to destroy the vehicle.
	 * Otherwise, we return the collision depth in the specified axis.
	 */
	protected float getCollisionForAxis(VehicleAxisAlignedBB box, boolean xAxis, boolean yAxis, boolean zAxis){
		Vec3d motion = new Vec3d(this.motionX*SPEED_FACTOR, this.motionY*SPEED_FACTOR, this.motionZ*SPEED_FACTOR);
		box = box.offset(xAxis ? motion.x : 0, yAxis ? motion.y : 0, zAxis ? motion.z : 0);
		List<BlockPos> collidedBlockPos = new ArrayList<BlockPos>();
		List<AxisAlignedBB> collidingAABBList = box.getAABBCollisions(world, collidedBlockPos);
		
		float collisionDepth = 0;
		for(AxisAlignedBB box2 : collidingAABBList){
			if(xAxis){
				collisionDepth = (float) Math.max(collisionDepth, motion.x > 0 ? box.maxX - box2.minX : box2.maxX - box.minX);
			}
			if(yAxis){
				collisionDepth = (float) Math.max(collisionDepth, motion.y > 0 ? box.maxY - box2.minY : box2.maxY - box.minY);
			}
			if(zAxis){
				collisionDepth = (float) Math.max(collisionDepth, motion.z > 0 ? box.maxZ - box2.minZ : box2.maxZ - box.minZ);
			}
			if(collisionDepth > 0.3){
				//This could be a collision, but it could also be that we are really colliding in another axis.
				//Check the motion and bail if the collision depth is less than our movement.
				if((xAxis && (Math.abs(motion.x) < collisionDepth)) || (yAxis && (Math.abs(motion.y) < collisionDepth)) || (zAxis && (Math.abs(motion.z) < collisionDepth))){
					return 0;
				}
			}
		}
		
		if(collisionDepth > 0){
			//We collided, so check to see if we can break some blocks or if we need to explode.
			//Don't bother with this logic if it's impossible for us to break anything.
			double velocity = Math.hypot(motion.x, motion.z);
			if(velocity > 0 && !yAxis){
				byte blockPosIndex = 0;
				while(blockPosIndex < collidedBlockPos.size()){
					BlockPos pos = collidedBlockPos.get(blockPosIndex);
					float hardness = world.getBlockState(pos).getBlockHardness(world, pos);
					if(hardness <= velocity*currentMass/250F && hardness >= 0){
						if(ConfigSystem.configObject.damage.blockBreakage.value){
							hardnessHitThisTick += hardness;
							collidedBlockPos.remove(blockPosIndex);
							motionX *= Math.max(1.0F - hardness*0.5F/((1000F + currentMass)/1000F), 0.0F);
							motionY *= Math.max(1.0F - hardness*0.5F/((1000F + currentMass)/1000F), 0.0F);
							motionZ *= Math.max(1.0F - hardness*0.5F/((1000F + currentMass)/1000F), 0.0F);
							if(!world.isRemote){
								world.destroyBlock(pos, true);
							}
						}else{
							motionX = 0;
							motionY = 0;
							motionZ = 0;
							++blockPosIndex;
						}
					}else{
						++blockPosIndex;
					}
				}

				if(hardnessHitThisTick > currentMass/(0.75+velocity)/250F){
					if(!world.isRemote){
						this.destroyAtPosition(box.pos.x, box.pos.y, box.pos.z);
					}
					return -1;
				}else if(collidedBlockPos.isEmpty()){
					return 0;
				}
			}
			if(collisionDepth > 0.3){
				if(!world.isRemote){
					this.destroyAtPosition(box.pos.x, box.pos.y, box.pos.z);
				}
				return -1;	
			}
		}
		return collisionDepth;
	}
	
	/**
	 * Call this to remove this vehicle.  This should be called when the vehicle has crashed, as it
	 * ejects all parts and damages all players.  Explosions may not occur in crashes depending on config 
	 * settings or a lack of fuel or explodable cargo.  Call only on the SERVER as this is for item-spawning 
	 * code and player damage code.
	 */
	public void destroyAtPosition(double x, double y, double z){
		this.setDead();
		//Remove all parts from the vehicle and place them as items.
		for(APart part : getVehicleParts()){
			if(part.getItemForPart() != null){
				ItemStack partStack = new ItemStack(part.getItemForPart());
				NBTTagCompound stackTag = part.getData();
				if(stackTag != null){
					partStack.setTagCompound(stackTag);
				}
				world.spawnEntity(new EntityItem(world, part.worldPos.x, part.worldPos.y, part.worldPos.z, partStack));
			}
		}
		
		//Also drop some crafting ingredients as items.
		for(ItemStack craftingStack : MTSRegistry.getMaterials(MTSRegistry.packItemMap.get(definition.packID).get(definition.systemName))){
			for(int i=0; i<craftingStack.getCount(); ++i){
				if(this.rand.nextDouble() < ConfigSystem.configObject.damage.crashItemDropPercentage.value){
					world.spawnEntity(new EntityItem(world, this.posX, this.posY, this.posZ, new ItemStack(craftingStack.getItem(), 1, craftingStack.getMetadata())));
				}
			}
		}
	}	
	
	/**
	 * Calculates the current mass of the vehicle.
	 * Includes core mass, player weight and inventory, and cargo.
	 */
	protected float getCurrentMass(){
		int currentMass = definition.general.emptyMass;
		for(APart part : parts){
			if(part instanceof PartCrate){
				currentMass += calculateInventoryWeight(((PartCrate) part).crateInventory);
			}else if(part instanceof PartBarrel){
				currentMass += ((PartBarrel) part).getFluidAmount()/50;
			}
		}
		
		//Add passenger inventory mass as well.
		for(Entity passenger : this.getPassengers()){
			if(passenger instanceof EntityPlayer){
				currentMass += 100 + calculateInventoryWeight(((EntityPlayer) passenger).inventory);
			}else{
				currentMass += 100;
			}
		}
		return currentMass;
	}
	
	/**
	 * Calculates the weight of the inventory passed in.
	 */
	private static float calculateInventoryWeight(IInventory inventory){
		float weight = 0;
		for(int i=0; i<inventory.getSizeInventory(); ++i){
			ItemStack stack = inventory.getStackInSlot(i);
			if(stack != null){
				double weightMultiplier = 1.0;
				for(String heavyItemName : ConfigSystem.configObject.general.itemWeights.weights.keySet()){
					if(stack.getItem().getRegistryName().toString().contains(heavyItemName)){
						weightMultiplier = ConfigSystem.configObject.general.itemWeights.weights.get(heavyItemName);
						break;
					}
				}
				weight += 5F*stack.getCount()/stack.getMaxStackSize()*weightMultiplier;
			}
		}
		return weight;
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		data.setBoolean("locked", locked);
		data.setBoolean("brakeOn", brakeOn);
		data.setBoolean("parkingBrakeOn", parkingBrakeOn);
		data.setString("ownerName", ownerName);
		data.setString("displayText", displayText);
	}
}
