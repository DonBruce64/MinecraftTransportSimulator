package minecrafttransportsimulator.vehicles.parts;

import mcinterface.InterfaceAudio;
import mcinterface.InterfaceNetwork;
import mcinterface.InterfaceRender;
import mcinterface.WrapperBlock;
import mcinterface.WrapperNBT;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartGroundDevice;
import minecrafttransportsimulator.rendering.components.IVehiclePartFXProvider;
import minecrafttransportsimulator.rendering.instances.ParticleSmoke;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.item.Item;

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
public class PartGroundDevice extends APart implements IVehiclePartFXProvider{
	public static final Point3d groundDetectionOffset = new Point3d(0, -0.05F, 0);
	
	//External states for animations.
	public boolean skipAngularCalcs = false;
	public double angularPosition;
	public double angularVelocity;
	
	//Internal states for control and physics.
	private boolean isFlat;
	private boolean contactThisTick = false;
	private int ticksCalcsSkipped = 0;
	private double prevAngularVelocity;
	private final PartGroundDeviceFake fakePart;
	
	public PartGroundDevice(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, WrapperNBT data){
		super(vehicle, packVehicleDef, definition, data);
		this.isFlat = data.getBoolean("isFlat");
		
		//If we are a long ground device, add a fake ground device at the offset to make us
		//have a better contact area.  If we are a fake part calling this as a super constructor,
		//we will be invalid.  Check that to prevent loops.  Also set some parameters manually
		//as fake parts have a few special properties.
		if(!isFake() && getLongPartOffset() != 0){
			packVehicleDef.pos[2] += getLongPartOffset();
			fakePart = new PartGroundDeviceFake(this, packVehicleDef, definition, data);
			packVehicleDef.pos[2] -= getLongPartOffset();
			//Only check collision if we are not adding this part from saved NBT data.
			//If we are adding from NBT, then we should have a tag saying that.
			vehicle.addPart(fakePart, data.getBoolean("isExisting"));
		}else{
			fakePart = null;
		}
	}
	
	@Override
	public void attack(Damage damage){
		if(definition.ground.isWheel && !isFlat && ConfigSystem.configObject.damage.wheelBreakage.value){
			if(damage.isExplosion || Math.random() < 0.1){
				if(!vehicle.world.isClient()){
					setFlat();
					InterfaceNetwork.sendToClientsTracking(new PacketVehiclePartGroundDevice(this), vehicle);
				}
			}
		}
	}
	
	@Override
	public void update(){
		super.update();
		if(isOnGround()){
			//If we aren't skipping angular calcs, change our velocity accordingly.
			if(!skipAngularCalcs){
				prevAngularVelocity = angularVelocity;
				angularVelocity = getDesiredAngularVelocity();
			}
			
			//Set contact for wheel skidding effects.
			if(definition.ground.isWheel){
				if(prevAngularVelocity/((vehicle.groundVelocity)/(this.getHeight()*Math.PI)) < 0.25 && vehicle.velocity > 0.3){
					//Sudden angular velocity increase.  Mark for skidding effects if the block below us is hard.
					WrapperBlock blockBelow = vehicle.world.getWrapperBlock(new Point3i((int) worldPos.x, (int) worldPos.y - 1, (int) worldPos.z));
					if(blockBelow != null && blockBelow.getHardness() >= 1.25){
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
						if(!vehicle.world.isClient()){
							setFlat();
							InterfaceNetwork.sendToClientsTracking(new PacketVehiclePartGroundDevice(this), vehicle);
						}
					}
				}
			}
			
			//Check for colliding entities and damage them.
			if(!vehicle.world.isClient() && vehicle.velocity >= ConfigSystem.configObject.damage.wheelDamageMinimumVelocity.value){
				boundingBox.widthRadius += 0.25;
				boundingBox.depthRadius += 0.25;
				final double wheelDamageAmount;
				if(!ConfigSystem.configObject.damage.wheelDamageIgnoreVelocity.value){
					wheelDamageAmount = ConfigSystem.configObject.damage.wheelDamageFactor.value*vehicle.velocity*vehicle.currentMass/1000F;
				}else{
					wheelDamageAmount = ConfigSystem.configObject.damage.wheelDamageFactor.value*vehicle.currentMass/1000F;
				}
				Damage wheelDamage = new Damage("wheel", wheelDamageAmount, boundingBox, vehicle.getController());
				vehicle.world.attackEntities(wheelDamage, vehicle);
				boundingBox.widthRadius -= 0.25;
				boundingBox.depthRadius -= 0.25;
			}
		}else if(vehicle.definition.car == null){
			if(vehicle.brakeOn || vehicle.parkingBrakeOn){
				angularVelocity = 0;
			}else if(angularVelocity>0){
				angularVelocity = (float) Math.max(angularVelocity - 0.05, 0);
			}
		}
		angularPosition += angularVelocity;
	}
	
	@Override
	public void remove(){
		super.remove();
		if(fakePart != null){
			fakePart.isValid = false;
		}
	}
	
	@Override
	public WrapperNBT getData(){
		WrapperNBT data = new WrapperNBT();
		data.setBoolean("isExisting", true);
		data.setBoolean("isFlat", isFlat);
		return data;
	}
	
	@Override
	public Item getItem(){
		return isFlat ? null : super.getItem();
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
		//Update box height to new height.
		boundingBox.heightRadius = getHeight()/2D;
		if(vehicle.world.isClient()){
			InterfaceAudio.playQuickSound(new SoundInstance(this, MTS.MODID + ":wheel_blowout"));
		}
	}
	
	public float getFrictionLoss(){
		Point3i groundPosition = new Point3i((int) worldPos.x, (int) worldPos.y - 1, (int) worldPos.z);
		WrapperBlock groundBlock = vehicle.world.getWrapperBlock(groundPosition);		
		if(groundBlock != null){
			//0.6 is default slipperiness for blocks.  Anything extra should reduce friction, anything less should increase it.
			return 0.6F - groundBlock.getSlipperiness() + (groundBlock.isRaining() ? 0.25F : 0);
		}else{
			return 0;
		}
	}
	
	public double getDesiredAngularVelocity(){
		return getLongPartOffset() == 0 ? vehicle.groundVelocity/(getHeight()*Math.PI) : vehicle.groundVelocity;
	}
	
	public boolean isOnGround(){
		return boundingBox.updateCollidingBlocks(vehicle.world, groundDetectionOffset);
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
	public void spawnParticles(){
		if(contactThisTick){
			for(byte i=0; i<4; ++i){
				InterfaceRender.spawnParticle(new ParticleSmoke(vehicle.world, worldPos, new Point3d(Math.random()*0.10 - 0.05, 0.15, Math.random()*0.10 - 0.05), 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));
			}
			InterfaceAudio.playQuickSound(new SoundInstance(this, MTS.MODID + ":" + "wheel_striking"));
			contactThisTick = false;
		}
		if(skipAngularCalcs && this.isOnGround()){
			for(byte i=0; i<4; ++i){
				InterfaceRender.spawnParticle(new ParticleSmoke(vehicle.world, worldPos, new Point3d(Math.random()*0.10 - 0.05, 0.15, Math.random()*0.10 - 0.05), 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));
			}
		}
	}
}
