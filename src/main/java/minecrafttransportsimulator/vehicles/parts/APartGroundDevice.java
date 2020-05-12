package minecrafttransportsimulator.vehicles.parts;

import java.util.List;

import minecrafttransportsimulator.dataclasses.DamageSources.DamageSourceWheel;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Car;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**Any child that touches the ground should extend this class.
 * It's used to perform ground physics and rendering of wheels, pontoons, and whatever
 * else people may want their vehicles to touch the ground with.
 * 
 * @author don_bruce
 */
public abstract class APartGroundDevice extends APart{
	public static final Vec3d groundDetectionOffset = new Vec3d(0, -0.05F, 0);
	private final PartGroundDeviceFake fakePart;
	
	public boolean skipAngularCalcs = false;
	public float angularPosition;
	public float angularVelocity;
	
	public APartGroundDevice(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
		
		//If we are a long ground device, add a fake ground device at the offset to make us
		//have a better contact area.  If we are a fake part calling this as a super constructor,
		//we will be invalid.  Check that to prevent loops.  Also set some parameters manually
		//as fake parts have a few special properties.
		if(this.isValid() && this.getLongPartOffset() != 0){
			packVehicleDef.pos[2] += this.getLongPartOffset();
			packVehicleDef.turnsWithSteer = packVehicleDef.pos[2] > 0;
			fakePart = new PartGroundDeviceFake(this, packVehicleDef, definition, dataTag);
			//Only check collision if we are not adding this part from saved NBT data.
			//If we check all the time, clients get wonky.
			//To do this, we only check if the vehicle has existed for over 40 ticks.
			//At this point we shouldn't be loading an NBT, so this part will have been
			//added by the player and should do collision checks.
			vehicle.addPart(fakePart, vehicle.ticksExisted < 40);
			packVehicleDef.pos[2] -= this.getLongPartOffset();
			packVehicleDef.turnsWithSteer = false;
		}else{
			fakePart = null;
		}
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		if(this.isOnGround()){
			//If we aren't skipping angular calcs, change our velocity accordingly.
			//Long parts use linear propulsion, not rotary, so don't take height into account.
			if(!skipAngularCalcs){
				if(getLongPartOffset() == 0){
					angularVelocity = (float) (vehicle.velocity/(this.getHeight()*Math.PI));
				}else{
					angularVelocity = (float) vehicle.velocity;
				}
			}
			
			//Check for colliding entities and damage them.
			if(!vehicle.world.isRemote && vehicle.velocity > 0.2F){
				List<EntityLivingBase> collidedEntites = vehicle.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getAABBWithOffset(Vec3d.ZERO).expand(0.25F, 0, 0.25F));
				if(!collidedEntites.isEmpty()){
					Entity attacker = null;
					for(Entity passenger : vehicle.getPassengers()){
						PartSeat seat = vehicle.getSeatForRider(passenger);
						if(seat.isController){
							attacker = passenger;
							break;
						}
					}
					for(int i=0; i < collidedEntites.size(); ++i){
						if(!this.vehicle.isPassenger(collidedEntites.get(i))){
							if(!ConfigSystem.configObject.damage.wheelDamageIgnoreVelocity.value){
								collidedEntites.get(i).attackEntityFrom(new DamageSourceWheel(attacker), (float) (ConfigSystem.configObject.damage.wheelDamageFactor.value*vehicle.velocity*vehicle.currentMass/1000F));
							}else{
								collidedEntites.get(i).attackEntityFrom(new DamageSourceWheel(attacker), (float) (ConfigSystem.configObject.damage.wheelDamageFactor.value*vehicle.currentMass/1000F));
							}
						}
					}
				}
			}
		}else if(!(vehicle instanceof EntityVehicleG_Car)){
			if(vehicle.brakeOn || vehicle.parkingBrakeOn){
				angularVelocity = 0;
			}else if(angularVelocity>0){
				angularVelocity = (float) Math.max(angularVelocity - 0.05, 0);
			}
		}
		angularPosition += angularVelocity;
	}
	
	@Override
	public void removePart(){
		super.removePart();
		if(fakePart != null){
			vehicle.removePart(fakePart, false);
		}
	}
	
	public float getFrictionLoss(){
		//0.6 is default slipperiness for blocks.  Anything extra should reduce friction, anything less should increase it.
		BlockPos pos = new BlockPos(partPos.addVector(0, -1, 0));
		return 0.6F - vehicle.world.getBlockState(pos).getBlock().getSlipperiness(vehicle.world.getBlockState(pos), vehicle.world, pos, null) + (vehicle.world.isRainingAt(pos.up()) ? 0.25F : 0);
	}
	
	public boolean isOnGround(){
		return isPartCollidingWithBlocks(groundDetectionOffset);
	}
	
	public abstract float getMotiveFriction();
	
	public abstract float getLateralFriction();
	
	public abstract float getLongPartOffset();
	
	public abstract boolean canBeDrivenByEngine();
}
