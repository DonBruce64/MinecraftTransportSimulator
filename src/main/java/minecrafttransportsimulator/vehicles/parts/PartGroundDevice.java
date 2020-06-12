package minecrafttransportsimulator.vehicles.parts;

import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.dataclasses.DamageSources.DamageSourceWheel;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.parts.PacketPartGroundDeviceWheelFlat;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.VehicleEffectsSystem;
import minecrafttransportsimulator.systems.VehicleEffectsSystem.FXPart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Car;
import minecrafttransportsimulator.wrappers.WrapperAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**A ground device is simply a part of a vehicle that touches the ground.
 * This class is used to perform ground physics, which include steering, 
 * turning, and hill climbing.  Can be a wheel-based part that rolls and 
 * provides power from engines, a solid part that doesn't provide power but
 * still allows for movement, a longer part with multiple hitboxes, a 
 * floating part, etc.  Each property is set via the JSON definition, though
 * a few are vehicle-dependent. 
 * 
 * @author don_bruce
 */
public class PartGroundDevice extends APart implements FXPart{
	public static final Point3d groundDetectionOffset = new Point3d(0, -0.05F, 0);
	
	//External states for animations.
	public boolean skipAngularCalcs = false;
	public float angularPosition;
	public float angularVelocity;
	
	//Internal states for control and physics.
	private boolean isFlat;
	private boolean contactThisTick = false;
	private int ticksCalcsSkipped = 0;
	private float prevAngularVelocity;
	private final PartGroundDeviceFake fakePart;
	
	public PartGroundDevice(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
		this.isFlat = dataTag.getBoolean("isFlat");
		
		//If we are a long ground device, add a fake ground device at the offset to make us
		//have a better contact area.  If we are a fake part calling this as a super constructor,
		//we will be invalid.  Check that to prevent loops.  Also set some parameters manually
		//as fake parts have a few special properties.
		if(isValid() && getLongPartOffset() != 0){
			packVehicleDef.pos[2] += getLongPartOffset();
			fakePart = new PartGroundDeviceFake(this, packVehicleDef, definition, dataTag);
			//Only check collision if we are not adding this part from saved NBT data.
			//If we check all the time, clients get wonky.
			//To do this, we only check if the vehicle has existed for over 40 ticks.
			//At this point we shouldn't be loading an NBT, so this part will have been
			//added by the player and should do collision checks.
			vehicle.addPart(fakePart, vehicle.ticksExisted < 40);
			packVehicleDef.pos[2] -= getLongPartOffset();
		}else{
			fakePart = null;
		}
	}
	
	@Override
	public void attackPart(DamageSource source, float damage){
		if(definition.ground.isWheel && !isFlat && ConfigSystem.configObject.damage.wheelBreakage.value){
			if(source.isExplosion() || Math.random() < 0.1){
				if(!vehicle.world.isRemote){
					this.setFlat();
					MTS.MTSNet.sendToAll(new PacketPartGroundDeviceWheelFlat(this));
				}
			}
		}
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		if(this.isOnGround()){
			//If we aren't skipping angular calcs, change our velocity accordingly.
			//Long parts use linear propulsion, not rotary, so don't take height into account.
			if(!skipAngularCalcs){
				prevAngularVelocity = angularVelocity;
				if(getLongPartOffset() == 0){
					angularVelocity = (float) (vehicle.groundVelocity/(getHeight()*Math.PI));
				}else{
					angularVelocity = (float) (vehicle.groundVelocity);
				}
			}
			
			//Set contact for wheel skidding effects.
			if(definition.ground.isWheel){
				if(prevAngularVelocity/((vehicle.groundVelocity)/(this.getHeight()*Math.PI)) < 0.25 && vehicle.velocity > 0.3){
					BlockPos blockBelow = new BlockPos(worldPos.x, worldPos.y - 1, worldPos.z);
					if(vehicle.world.getBlockState(blockBelow).getBlockHardness(vehicle.world, blockBelow) >= 1.25){
						contactThisTick = true;
					}
				}
				
				//If we have a slipping wheel, count down and possibly pop it.
				if(!skipAngularCalcs){
					if(ticksCalcsSkipped > 0 && !isFlat){
						--ticksCalcsSkipped;
					}
				}else if(!isFlat){
					++ticksCalcsSkipped;
					if(Math.random()*50000 < ticksCalcsSkipped && ConfigSystem.configObject.damage.wheelBreakage.value){
						if(!vehicle.world.isRemote){
							this.setFlat();
							MTS.MTSNet.sendToAll(new PacketPartGroundDeviceWheelFlat(this));
						}
					}
				}
			}
			
			//Check for colliding entities and damage them.
			if(!vehicle.world.isRemote && vehicle.velocity >= ConfigSystem.configObject.damage.wheelDamageMinimumVelocity.value){
				List<EntityLivingBase> collidedEntites = vehicle.world.getEntitiesWithinAABB(EntityLivingBase.class, boundingBox.expand(0.25F, 0, 0.25F));
				if(!collidedEntites.isEmpty()){
					Entity attacker = null;
					for(Entity passenger : vehicle.getPassengers()){
						PartSeat seat = vehicle.getSeatForRider(passenger);
						if(seat.vehicleDefinition.isController){
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
	public boolean wouldPartCollide(Point3d collisionOffset){
		if(super.wouldPartCollide(collisionOffset)){
			return true;
    	}else if(definition.ground.canFloat){
    		return isPartCollidingWithLiquids(collisionOffset);
    	}else{
    		return false;
    	}
    }
	
	@Override
	public void removePart(){
		super.removePart();
		if(fakePart != null){
			vehicle.removePart(fakePart, false);
		}
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound dataTag = new NBTTagCompound();
		dataTag.setBoolean("isFlat", isFlat);
		return dataTag;
	}
	
	@Override
	public Item getItemForPart(){
		return isFlat ? null : super.getItemForPart();
	}
	
	@Override
	public float getWidth(){
		return definition.ground.width;
	}
	
	@Override
	public float getHeight(){
		return isFlat ? definition.ground.height/2F : definition.ground.height;
	}
	
	@Override
	public String getModelLocation(){
		if(isFlat){
			return "objmodels/parts/" + (definition.general.modelName != null ? definition.general.modelName : definition.systemName) + "_flat.obj";
		}else{
			return super.getModelLocation();
		}
	}
	
	@Override
	public Point3d getActionRotation(float partialTicks){
		double xRotation = definition.ground.isWheel ? vehicle.SPEED_FACTOR*(angularPosition + angularVelocity*partialTicks)*360D : 0;
		double yRotation = vehicleDefinition.turnsWithSteer && definition.ground.extraCollisionBoxOffset == 0 ? -vehicle.getSteerAngle()*Math.signum(totalOffset.z) : 0;
		return new Point3d(xRotation, yRotation, 0D);
		
	}
	
	public void setFlat(){
		isFlat = true;
		if(vehicle.world.isRemote){
			WrapperAudio.playQuickSound(new SoundInstance(this, MTS.MODID + ":wheel_blowout"));
		}
	}
	
	public float getFrictionLoss(){
		//0.6 is default slipperiness for blocks.  Anything extra should reduce friction, anything less should increase it.
		BlockPos pos = new BlockPos(worldPos.x, worldPos.y - 1, worldPos.z);
		return 0.6F - vehicle.world.getBlockState(pos).getBlock().getSlipperiness(vehicle.world.getBlockState(pos), vehicle.world, pos, null) + (vehicle.world.isRainingAt(pos.up()) ? 0.25F : 0);
	}
	
	public boolean isOnGround(){
		return wouldPartCollide(groundDetectionOffset);
	}
	
	public float getMotiveFriction(){
		return !isFlat ? definition.ground.motiveFriction : definition.ground.motiveFriction/10F;
	}
	
	public float getLateralFriction(){
		return !isFlat ? definition.ground.lateralFriction : definition.ground.lateralFriction/10F;
	}
		
	public float getLongPartOffset(){
		return vehicleDefinition.extraCollisionBoxOffset != 0 ? vehicleDefinition.extraCollisionBoxOffset : definition.ground.extraCollisionBoxOffset;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void spawnParticles(){
		if(contactThisTick){
			for(byte i=0; i<4; ++i){
				Minecraft.getMinecraft().effectRenderer.addEffect(new VehicleEffectsSystem.ColoredSmokeFX(vehicle.world, worldPos.x, worldPos.y, worldPos.z, Math.random()*0.10 - 0.05, 0.15, Math.random()*0.10 - 0.05, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));
			}
			WrapperAudio.playQuickSound(new SoundInstance(this, MTS.MODID + ":" + "wheel_striking"));
			contactThisTick = false;
		}
		if(skipAngularCalcs && this.isOnGround()){
			for(byte i=0; i<4; ++i){
				Minecraft.getMinecraft().effectRenderer.addEffect(new VehicleEffectsSystem.ColoredSmokeFX(vehicle.world, worldPos.x, worldPos.y, worldPos.z, Math.random()*0.10 - 0.05, 0.15, Math.random()*0.10 - 0.05, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));
			}
		}
	}
}
