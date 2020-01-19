package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.jsondefs.PackVehicleObject.PackPart;
import minecrafttransportsimulator.packets.parts.PacketPartSeatRiderChange;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartBarrel;
import minecrafttransportsimulator.vehicles.parts.PartCrate;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**This is the next class level above the base vehicle.
 * At this level we add methods for the vehicle's existence in the world.
 * Variables for position are defined here, but no methods for MOVING
 * this vehicle are present until later sub-classes.  Also not present
 * are variables that define how this vehicle COULD move (motions, states
 * of brakes/throttles, collision boxes, etc.)  This is where the pack information comes in
 * as this is where we start needing it.  This is also where we handle how this
 * vehicle reacts with events like clicking and crashing with players inside.
 * 
 * @author don_bruce
 */
@Mod.EventBusSubscriber
public abstract class EntityVehicleB_Existing extends EntityVehicleA_Base{
	public boolean locked;
	public float rotationRoll;
	public float prevRotationRoll;
	public double airDensity;
	public double currentMass;
	public String ownerName="";
	public String displayText="";
	public Vec3d headingVec = Vec3d.ZERO;
	
	/**Cached map that links entity IDs to the seats riding them.  Used for mounting/dismounting functions.*/
	private final BiMap<Integer, PartSeat> riderSeats = HashBiMap.create();
	
	/**List for storage of rider linkages to seats.  Populated during NBT load and used to populate the riderSeats map after riders load.*/
	private List<Double[]> riderSeatPositions = new ArrayList<Double[]>();
	
	/**Names for reflection to get the entity any entity is riding.**/
	private static final String[] ridingEntityNames = { "ridingEntity", "field_73141_v", "field_184239_as"};
	
	/**ID of the current rider that wants to dismount this tick.  This gets set when the vehicle detects that an entity wants to dismount.**/
	private int riderIDToDismountThisTick = -1;
	
	/**Boolean paired with above to determine if dismounting code needs to be inhibited or if we are just changing seats.**/
	private boolean didRiderClickSeat;
			
	public EntityVehicleB_Existing(World world){
		super(world);
	}
	
	public EntityVehicleB_Existing(World world, float posX, float posY, float posZ, float playerRotation, String vehicleName){
		super(world, vehicleName);
		//Set position to the spot that was clicked by the player.
		//Add a -90 rotation offset so the vehicle is facing perpendicular.
		//Makes placement easier and is less likely for players to get stuck.
		this.setPositionAndRotation(posX, posY, posZ, playerRotation-90, 0);
		
		//This only gets done at the beginning when the entity is first spawned.
		this.displayText = pack.rendering.defaultDisplayText;
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(pack != null){
			currentMass = getCurrentMass();
			airDensity = 1.225*Math.pow(2, -posY/(500D*world.getHeight()/256D));
			getBasicProperties();
		}
		
		if(riderIDToDismountThisTick != -1){
			Entity riderToDismount = world.getEntityByID(riderIDToDismountThisTick);
			if(riderToDismount != null){
				PartSeat seat = this.getSeatForRider(riderToDismount);
				if(seat != null){
					removeRiderFromSeat(riderToDismount, seat);
					riderIDToDismountThisTick = -1;
				}
			}
			
			if(riderIDToDismountThisTick != -1){
				//We couldn't dismount this rider.
				//Likely a Sponge issue, so find the missing rider and remove him.
				Integer missingRiderID = -1;
				for(Integer entityID : riderSeats.keySet()){
					boolean passengerIsValid = false;
					for(Entity passenger : getPassengers()){
						if(passenger.getEntityId() == entityID){
							passengerIsValid = true;
							break;
						}
					}
					if(!passengerIsValid){
						missingRiderID = entityID;
					}
				}
				riderSeats.remove(missingRiderID);
			}
			riderIDToDismountThisTick = -1;
		}
		didRiderClickSeat = false;
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource source, float damage){
		if(!world.isRemote){
			if(source.getImmediateSource() != null && !source.getImmediateSource().equals(source.getTrueSource())){
				//This is a projectile of some sort.  If this projectile is inside a part
				//make it hit the part rather than hit the vehicle.
				Entity projectile = source.getImmediateSource();
				for(APart part : this.getVehicleParts()){
					//Expand this box by the speed of the projectile just in case the projectile is custom and
					//calls its attack code before it actually gets inside the collision box.
					if(part.getAABBWithOffset(Vec3d.ZERO).expand(Math.abs(projectile.motionX), Math.abs(projectile.motionY), Math.abs(projectile.motionZ)).contains(projectile.getPositionVector())){
						part.attackPart(source, damage);
						return true;
					}
				}
			}else{
				//This is not a projectile, and therefore must be some sort of entity.
				//Check to see where this entity is looking and if it has hit a
				//part attack that part.
				Entity attacker = source.getTrueSource();
				if(attacker != null){
					APart hitPart = this.getHitPart(attacker);
					if(hitPart != null){
						hitPart.attackPart(source, damage);
						return true;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * Prevent dismounting from this vehicle naturally as MC sucks at finding good spots to dismount.
	 * Instead, chose a better spot manually to prevent the player from getting stuck inside things.
	 * This event is used to populate a list of riders to dismount the next tick.  This list is cleared when the operation
	 * is done successfully.  If the operation fails, the rider will still be in the list so in that case the
	 * event should proceed as normal to allow regular MC dismounting.
	 */
	@SubscribeEvent
	public static void on(EntityMountEvent event){
		if(event.getEntityBeingMounted() instanceof EntityVehicleB_Existing  && event.isDismounting() && event.getEntityMounting() != null && !event.getWorldObj().isRemote && !event.getEntityBeingMounted().isDead){
			EntityVehicleB_Existing vehicle = (EntityVehicleB_Existing) event.getEntityBeingMounted();
			if(!vehicle.didRiderClickSeat){
				if(vehicle.riderIDToDismountThisTick == -1){
					vehicle.riderIDToDismountThisTick = event.getEntityMounting().getEntityId();
					event.setCanceled(true);
				}else{
					vehicle.riderIDToDismountThisTick = -1;
					event.setCanceled(false);
				}
			}
			vehicle.didRiderClickSeat = false;
		}
	 }
	
	@Override
	public void updatePassenger(Entity passenger){
		PartSeat seat = this.getSeatForRider(passenger);
		if(seat != null){
			Vec3d playerOffsetVec = seat.partPos.add(RotationSystem.getRotatedPoint(new Vec3d(0, -seat.getHeight()/2F + passenger.getYOffset() + passenger.height, 0), this.rotationPitch, this.rotationYaw, this.rotationRoll));
			passenger.setPosition(playerOffsetVec.x, playerOffsetVec.y - passenger.height, playerOffsetVec.z);
			passenger.motionX = this.motionX;
			passenger.motionY = this.motionY;
			passenger.motionZ = this.motionZ;
		}else if(pack != null && !this.riderSeatPositions.isEmpty()){
			Double[] seatLocation = this.riderSeatPositions.get(this.getPassengers().indexOf(passenger));
			APart part = getPartAtLocation(seatLocation[0], seatLocation[1], seatLocation[2]);
			if(part instanceof PartSeat){
				riderSeats.put(passenger.getEntityId(), (PartSeat) part);
			}else{
				MTS.MTSLog.error("ERROR: NO SEAT FOUND WHEN LINKING RIDER TO SEAT IN VEHICLE!");
				if(!world.isRemote){
					passenger.dismountRidingEntity();
				}
				return;
			}
		}
	}
	
	@Override
	public void addPart(APart part, boolean ignoreCollision){
		if(!ignoreCollision){
			//Check if we are colliding and adjust roll before letting part addition continue.
			//This is needed as the vehicle system doesn't know about roll.
			if(part.isPartCollidingWithBlocks(Vec3d.ZERO)){
				this.rotationRoll = 0;
			}
		}
		super.addPart(part, ignoreCollision);
	}
	
    @Override
    public boolean shouldRenderInPass(int pass){
        //Need to render in pass 1 to render transparent things in the world like light beams.
    	return true;
    }
    
	@Override
	public boolean canBeCollidedWith(){
		//This gets overridden to allow players to interact with this vehicle.
		return true;
	}
	
    /**
     * Checks to see if the entity passed in could have hit a part.
     * Is determined by the rotation of the entity and distance from parts.
     * If a part is found to be hit-able, it is returned.  Else null is returned.
     */
	public APart getHitPart(Entity entity){
		Vec3d lookVec = entity.getLook(1.0F);
		Vec3d hitVec = entity.getPositionVector().addVector(0, entity.getEyeHeight(), 0);
		for(float f=1.0F; f<4.0F; f += 0.1F){
			for(APart part : this.getVehicleParts()){
				if(part.getAABBWithOffset(Vec3d.ZERO).contains(hitVec)){
					return part;
				}
			}
			hitVec = hitVec.addVector(lookVec.x*0.1F, lookVec.y*0.1F, lookVec.z*0.1F);
		}
		return null;
	}
	
    /**
     * Adds a rider to this vehicle and sets their seat.
     * All riders MUST be added through this method.
     */
	public void setRiderInSeat(Entity rider, PartSeat seat){
		this.didRiderClickSeat = true;
		riderSeats.put(rider.getEntityId(), seat);
		rider.startRiding(this, true);
		//Set the player's yaw to the same yaw as the vehicle to ensure we don't have 360+ rotations to deal with.
		rider.rotationYaw =  (float) (this.rotationYaw + seat.partRotation.y);
		if(!world.isRemote){
			MTS.MTSNet.sendToAll(new PacketPartSeatRiderChange(seat, rider, true));
		}
	}
	
	/**
     * Removes the rider safely from this vehicle.  Returns true if removal was good, false if it failed.
     */
	public void removeRiderFromSeat(Entity rider, PartSeat seat){
		try{
			ObfuscationReflectionHelper.setPrivateValue(Entity.class, rider, null, ridingEntityNames);
		}catch (Exception e){
			MTS.MTSLog.fatal("ERROR IN VEHICLE RIDER REFLECTION!");
			return;
   	    }
		
		riderSeats.remove(rider.getEntityId());
		this.removePassenger(rider);
		rider.setSneaking(false);
		if(!world.isRemote){
			Vec3d placePosition;
			PackPart packPart = this.getPackDefForLocation(seat.offset.x, seat.offset.y, seat.offset.z);
			if(packPart.dismountPos != null){
				placePosition = RotationSystem.getRotatedPoint(new Vec3d(packPart.dismountPos[0], packPart.dismountPos[1], packPart.dismountPos[2]), this.rotationPitch, this.rotationYaw, this.rotationRoll).add(this.getPositionVector());
			}else{
				placePosition = RotationSystem.getRotatedPoint(seat.offset.addVector(seat.offset.x > 0 ? 2 : -2, 0, 0), this.rotationPitch, this.rotationYaw, this.rotationRoll).add(this.getPositionVector());	
			}
			AxisAlignedBB collisionDetectionBox = new AxisAlignedBB(new BlockPos(placePosition));
			if(!world.collidesWithAnyBlock(collisionDetectionBox)){
				rider.setPositionAndRotation(placePosition.x, collisionDetectionBox.minY, placePosition.z, rider.rotationYaw, rider.rotationPitch);
			}else if(rider instanceof EntityLivingBase){
				((EntityLivingBase) rider).dismountEntity(this);
			}
			MTS.MTSNet.sendToAll(new PacketPartSeatRiderChange(seat, rider, false));
		}
	}
	
	public Entity getRiderForSeat(PartSeat seat){
		return riderSeats.inverse().containsKey(seat) ? world.getEntityByID(riderSeats.inverse().get(seat)) : null;
	}
	
	public PartSeat getSeatForRider(Entity rider){
		return riderSeats.get(rider.getEntityId());
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
				NBTTagCompound stackTag = part.getPartNBTTag();
				if(stackTag != null){
					partStack.setTagCompound(stackTag);
				}
				world.spawnEntity(new EntityItem(world, part.partPos.x, part.partPos.y, part.partPos.z, partStack));
			}
		}
		
		//Also drop some crafting ingredients as items.
		double crashItemDropPercentage = ConfigSystem.getDoubleConfig("CrashItemDropPercentage");
		for(ItemStack craftingStack : PackParserSystem.getMaterials(this.vehicleName)){
			for(byte i=0; i<craftingStack.getCount(); ++i){
				if(this.rand.nextDouble() < crashItemDropPercentage){
					world.spawnEntity(new EntityItem(world, this.posX, this.posY, this.posZ, new ItemStack(craftingStack.getItem(), 1, craftingStack.getMetadata())));
				}
			}
		}
	}
	
	/**
	 * This code is allow for this vehicle to be "attacked" without an entity present.
	 * Normally the attack code will check the attacking entity to figure out what was
	 * attacked on the vehicle, but for some situations this may not be desireable.
	 * In particular, this is done for the particle-based bullet system as there are
	 * no projectile entities to allow the attack system to automatically calculate
	 * what was attacked on the vehicle.
	 */
	public void attackManuallyAtPosition(double x, double y, double z, DamageSource source, float damage){
		for(APart part : this.getVehicleParts()){
			if(part.getAABBWithOffset(Vec3d.ZERO).contains(new Vec3d(x, y, z))){
				part.attackPart(source, damage);
				return;
			}
		}
	}
	
	
	
	protected float getCurrentMass(){
		int currentMass = pack.general.emptyMass;
		for(APart part : this.getVehicleParts()){
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
	
	/**Calculates the weight of the inventory passed in.  Used for physics calculations.
	 */
	private static float calculateInventoryWeight(IInventory inventory){
		float weight = 0;
		for(int i=0; i<inventory.getSizeInventory(); ++i){
			ItemStack stack = inventory.getStackInSlot(i);
			if(stack != null){
				weight += 1.2F*stack.getCount()/stack.getMaxStackSize()*(ConfigSystem.getStringConfig("HeavyItems").contains(stack.getItem().getUnlocalizedName().substring(5)) ? 2 : 1);
			}
		}
		return weight;
	}
	
	protected void updateHeadingVec(){
        double f1 = Math.cos(-this.rotationYaw * 0.017453292F - (float)Math.PI);
        double f2 = Math.sin(-this.rotationYaw * 0.017453292F - (float)Math.PI);
        double f3 = -Math.cos(-this.rotationPitch * 0.017453292F);
        double f4 = Math.sin(-this.rotationPitch * 0.017453292F);
        headingVec = new Vec3d((f2 * f3), f4, (f1 * f3));
   	}
	
	/**
	 * Method block for basic properties like weight and vectors.
	 * This should be used by all vehicles to define all properties before
	 * calculating anything.
	 */
	protected abstract void getBasicProperties();
	
	/**
	 * Returns whatever the steering angle is.
	 * Used for rendering and possibly other things.
	 */
	public abstract float getSteerAngle();
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.locked=tagCompound.getBoolean("locked");
		this.rotationRoll=tagCompound.getFloat("rotationRoll");
		this.ownerName=tagCompound.getString("ownerName");
		this.displayText=tagCompound.getString("displayText");
		
		this.riderSeatPositions.clear();
		while(tagCompound.hasKey("Seat" + String.valueOf(riderSeatPositions.size()) + "0")){
			Double[] seatPosition = new Double[3];
			seatPosition[0] = tagCompound.getDouble("Seat" + String.valueOf(riderSeatPositions.size()) + "0");
			seatPosition[1] = tagCompound.getDouble("Seat" + String.valueOf(riderSeatPositions.size()) + "1");
			seatPosition[2] = tagCompound.getDouble("Seat" + String.valueOf(riderSeatPositions.size()) + "2");
			riderSeatPositions.add(seatPosition);
		}
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("locked", this.locked);
		tagCompound.setFloat("rotationRoll", this.rotationRoll);
		tagCompound.setString("ownerName", this.ownerName);
		tagCompound.setString("displayText", this.displayText);
		
		//Correlate the order of passengers in the rider list with their location to save it to NBT.
		//That way riders don't get moved to other seats on world save/load.
		for(byte i=0; i<this.getPassengers().size(); ++i){
			Entity rider = this.getPassengers().get(i);
			PartSeat seat = this.getSeatForRider(rider);
			if(seat != null){
				tagCompound.setDouble("Seat" + String.valueOf(i) + "0", seat.offset.x);
				tagCompound.setDouble("Seat" + String.valueOf(i) + "1", seat.offset.y);
				tagCompound.setDouble("Seat" + String.valueOf(i) + "2", seat.offset.z);
			}
		}
		return tagCompound;
	}
}
