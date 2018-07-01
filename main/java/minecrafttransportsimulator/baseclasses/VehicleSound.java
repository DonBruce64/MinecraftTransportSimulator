package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle.LightTypes;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;
import minecrafttransportsimulator.multipart.parts.APart;
import minecrafttransportsimulator.multipart.parts.APartEngine;
import minecrafttransportsimulator.systems.SFXSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;

public final class VehicleSound extends PositionedSound implements ITickableSound{
	private final EntityMultipartE_Vehicle vehicle;
	private final APart optionalPart;
	private final EntityPlayer player;
	private final SoundTypes soundType;
	
	private Vec3d playerPos;
	private Vec3d sourcePos;
	
	public VehicleSound(EntityMultipartE_Vehicle vehicle, APart optionalPart, SoundTypes soundType){
		super(SFXSystem.getSoundEventFromName(getSoundName(vehicle, optionalPart, soundType)), SoundCategory.MASTER);
		this.vehicle = vehicle;
		this.optionalPart = optionalPart;
		this.player = Minecraft.getMinecraft().thePlayer;
		this.soundType = soundType;
		this.repeat = true;
		
		this.playerPos = new Vec3d(player.posX, player.posY, player.posZ);
		this.sourcePos = optionalPart != null ? optionalPart.partPos : vehicle.getPositionVector();
		this.xPosF = (float) sourcePos.xCoord;
		this.yPosF = (float) sourcePos.yCoord;
		this.zPosF = (float) sourcePos.zCoord;
	}
	
    @Override
	public float getVolume(){
		if(isSoundActive()){
			//If this source is internal, only make noise if we are in first-person.
			if(soundType.internal){
				return Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 ? 1 : 0;
			}
			
			//If the player is riding the source, volume will either be 1.0 or 0.5.
			if(vehicle.equals(player.getRidingEntity())){
				return SFXSystem.isPlayerInsideEnclosedVehicle() ? 0.5F : 1.0F;
			}
			
			//Sound is not internal and player is not riding the source.  Volume is player distance.
			this.xPosF = (float) player.posX;
			this.yPosF = (float) player.posY;
			this.zPosF = (float) player.posZ;
			this.playerPos = new Vec3d(player.posX, player.posY, player.posZ);
			this.sourcePos = optionalPart != null ? optionalPart.partPos : vehicle.getPositionVector();
			return (float) (getCurrentVolume()/playerPos.distanceTo(sourcePos)*(SFXSystem.isPlayerInsideEnclosedVehicle() ? 0.5F : 1.0F));
		}else{
			return 0;
		}
    }
    
    @Override
    public float getPitch(){
		this.xPosF = (float) player.posX;
		this.yPosF = (float) player.posY;
		this.zPosF = (float) player.posZ;
		sourcePos = optionalPart != null ? optionalPart.partPos : vehicle.getPositionVector();
		playerPos = new Vec3d(player.posX, player.posY, player.posZ);
		
		//If the player is riding the sound source, don't apply a doppler effect.
		if(vehicle.equals(player.getRidingEntity())){
			return getCurrentPitch();
		}
		
		//Only adjust pitch for doppler sounds.
		if(!soundType.internal && !vehicle.equals(player.getRidingEntity())){
			double soundVelocity = playerPos.distanceTo(sourcePos) - playerPos.addVector(player.motionX, player.motionY, player.motionZ).distanceTo(sourcePos.addVector(vehicle.motionX, vehicle.motionY, vehicle.motionZ));
			return (float) (getCurrentPitch()*(1+soundVelocity/10F));
		}else{
			return 1.0F;
		}
    }
	
	@Override
	public boolean isDonePlaying(){
		return vehicle.isDead || (optionalPart != null && !optionalPart.isValid());
	}
	
	@Override
	public void update(){}
    
	public boolean isSoundActive(){
		switch(soundType){
			case ENGINE: return ((APartEngine) optionalPart).state.running || ((APartEngine) optionalPart).internalFuel > 0;
			case HORN: return vehicle.hornOn;
			case SIREN: return vehicle.isLightOn(LightTypes.EMERGENCYLIGHT);
			case STALL_BUZZER: return ((EntityMultipartF_Plane) vehicle).trackAngle <= -17;
			default: return true;
		}
	}
	
	private float getCurrentVolume(){
		switch(soundType){
			case ENGINE: return (float) (30F*((APartEngine) optionalPart).RPM/2000F);
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
	
	private static String getSoundName(EntityMultipartE_Vehicle vehicle, APart optionalPart, SoundTypes soundType){
		switch(soundType){
			case ENGINE: return optionalPart.partName + "_running";
			case HORN: return vehicle.pack.motorized.hornSound;
			case SIREN: return vehicle.pack.motorized.sirenSound;
			case STALL_BUZZER: return MTS.MODID + ":stall_buzzer";
			default: return "";
		}
	}
	
	public enum SoundTypes{
		ENGINE(false),
		HORN(false),
		SIREN(false),
		STALL_BUZZER(true);
		
		private final boolean internal;
		
		private SoundTypes(boolean internal){
			this.internal = internal;
		}
	}
}
