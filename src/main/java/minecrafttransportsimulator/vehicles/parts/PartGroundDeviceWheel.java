package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.parts.PacketPartGroundDeviceWheelFlat;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.VehicleEffectsSystem;
import minecrafttransportsimulator.systems.VehicleEffectsSystem.FXPart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.wrappers.WrapperAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class PartGroundDeviceWheel extends APartGroundDevice implements FXPart{
	private boolean isFlat;
	private boolean contactThisTick = false;
	private int ticksCalcsSkipped = 0;
	private float prevAngularVelocity;
	
	public PartGroundDeviceWheel(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
		this.isFlat = dataTag.getBoolean("isFlat");
	}
	
	@Override
	public void attackPart(DamageSource source, float damage){
		if(!this.isFlat && ConfigSystem.configObject.damage.wheelBreakage.value){
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
		prevAngularVelocity = angularVelocity;
		super.updatePart();
		if(this.isOnGround()){
			//Set contact for wheel skid.
			if(prevAngularVelocity/(vehicle.velocity/(this.getHeight()*Math.PI)) < 0.25 && vehicle.velocity > 0.3){
				BlockPos blockBelow = new BlockPos(worldPos).down();
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
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound dataTag = new NBTTagCompound();
		dataTag.setBoolean("isFlat", this.isFlat);
		return dataTag;
	}
	
	@Override
	public float getWidth(){
		return this.definition.wheel.diameter/2F;
	}
	
	@Override
	public float getHeight(){
		return this.isFlat ? this.definition.wheel.diameter/2F : this.definition.wheel.diameter;
	}
	
	@Override
	public Item getItemForPart(){
		return this.isFlat ? null : super.getItemForPart();
	}
	
	@Override
	public String getModelLocation(){
		if(this.isFlat){
			return "objmodels/parts/" + (definition.general.modelName != null ? definition.general.modelName : definition.systemName) + "_flat.obj";
		}else{
			return super.getModelLocation();
		}
	}
	
	@Override
	public Vec3d getActionRotation(float partialTicks){
		return new Vec3d(vehicle.SPEED_FACTOR*(this.angularPosition + this.angularVelocity*partialTicks)*360D, vehicleDefinition.turnsWithSteer ? vehicle.getSteerAngle() : 0, 0);
	}
	
	@Override
	public float getMotiveFriction(){
		return !this.isFlat ? this.definition.wheel.motiveFriction : this.definition.wheel.motiveFriction/10F;
	}
	
	@Override
	public float getLateralFriction(){
		return !this.isFlat ? this.definition.wheel.lateralFriction : this.definition.wheel.lateralFriction/10F;
	}
	
	@Override
	public float getLongPartOffset(){
		return 0;
	}
	
	@Override
	public boolean canBeDrivenByEngine(){
		return true;
	}
	
	public void setFlat(){
		this.isFlat = true;
		if(vehicle.world.isRemote){
			WrapperAudio.playQuickSound(new SoundInstance(this, MTS.MODID + ":wheel_blowout"));
		}
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
