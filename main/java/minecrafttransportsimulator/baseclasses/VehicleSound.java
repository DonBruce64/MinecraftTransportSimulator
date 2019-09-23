package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;

public final class VehicleSound{
	private final EntityVehicleE_Powered vehicle;
	private final APart optionalPart;
	private final EntityPlayer player;
	private final SoundTypes soundType;
	
	private Vec3d playerPos;
	private Vec3d sourcePos;
	
	public VehicleSound(EntityVehicleE_Powered vehicle, APart optionalPart, SoundTypes soundType){
		this.vehicle = vehicle;
		this.optionalPart = optionalPart;
		this.player = Minecraft.getMinecraft().thePlayer;
		this.soundType = soundType;
		
		this.playerPos = new Vec3d(player.posX, player.posY, player.posZ);
		this.sourcePos = optionalPart != null ? optionalPart.partPos : vehicle.getPositionVector();
	}
	
	public double getPosX(){
		return optionalPart != null ? optionalPart.partPos.xCoord : vehicle.posX;
	}
	
	public double getPosY(){
		return optionalPart != null ? optionalPart.partPos.yCoord : vehicle.posY;
	}
	
	public double getPosZ(){
		return optionalPart != null ? optionalPart.partPos.zCoord : vehicle.posZ;
	}
	
	public float getVolume(){
		if(isSoundActive()){
			//If the player is riding the source, volume will either be 1.0 or 0.5.
			if(vehicle.equals(player.getRidingEntity())){
				return SFXSystem.isPlayerInsideEnclosedVehicle() ? 0.5F : 1.0F;
			}
			
			//Sound is not internal and player is not riding the source.  Volume is player distance.
			this.playerPos = new Vec3d(player.posX, player.posY, player.posZ);
			this.sourcePos = optionalPart != null ? optionalPart.partPos : vehicle.getPositionVector();
			return (float) (getCurrentVolume()/playerPos.distanceTo(sourcePos)*(SFXSystem.isPlayerInsideEnclosedVehicle() ? 0.5F : 1.0F));
		}else{
			return 0;
		}
    }
    
    public float getPitch(){
		//If the player is riding the sound source, don't apply a doppler effect.
		if(vehicle.equals(player.getRidingEntity())){
			return getCurrentPitch();
		}else{
			sourcePos = optionalPart != null ? optionalPart.partPos : vehicle.getPositionVector();
			playerPos = new Vec3d(player.posX, player.posY, player.posZ);
			double soundVelocity = playerPos.distanceTo(sourcePos) - playerPos.addVector(player.motionX, player.motionY, player.motionZ).distanceTo(sourcePos.addVector(vehicle.motionX, vehicle.motionY, vehicle.motionZ));
			return (float) (getCurrentPitch()*(1+soundVelocity/10F));
		}
    }
    
	
	public String getSoundName(){
		switch(soundType){
			case ENGINE: return optionalPart.partName + "_running";
			case HORN: return vehicle.pack.motorized.hornSound;
			case SIREN: return vehicle.pack.motorized.sirenSound;
			default: return "";
		}
	}
	
	public String getSoundUniqueName(){
		return vehicle.getEntityId() + "_" + (optionalPart != null ? getSoundName() + String.valueOf(optionalPart.offset.xCoord) + String.valueOf(optionalPart.offset.yCoord) + String.valueOf(optionalPart.offset.zCoord) : getSoundName());
	}
	
	public boolean isSoundSourceActive(){
		return vehicle.isDead ? false : (optionalPart != null ? optionalPart.isValid() : true);
	}
    
	public boolean isSoundActive(){
		switch(soundType){
			case ENGINE: return ((APartEngine) optionalPart).state.running || ((APartEngine) optionalPart).internalFuel > 0;
			case HORN: return vehicle.hornOn;
			case SIREN: return vehicle.sirenOn;
			default: return true;
		}
	}
	
	private float getCurrentVolume(){
		switch(soundType){
			case ENGINE: return (float) (30F*((APartEngine) optionalPart).RPM/((APartEngine) optionalPart).pack.engine.maxRPM);
			case HORN: return 5.0F;
			case SIREN: return 10.0F;
			default: return 1.0F;
		}
	}
	
	private float getCurrentPitch(){
		switch(soundType){
			case ENGINE: return (float) (((APartEngine) optionalPart).RPM/(optionalPart.pack.engine.maxRPM/2F));
			default: return 1.0F;
		}
	}
	
	public enum SoundTypes{
		ENGINE(),
		HORN(),
		SIREN();
	}
}
