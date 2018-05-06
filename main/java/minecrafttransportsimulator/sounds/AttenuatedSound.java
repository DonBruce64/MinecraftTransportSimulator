package minecrafttransportsimulator.sounds;

import minecrafttransportsimulator.multipart.parts.AMultipartPart;
import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.systems.SFXSystem.SoundPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;

public final class AttenuatedSound extends MovingSound{
	private final SoundPart soundSource;
	private final EntityPlayer player;
		
	public AttenuatedSound(String soundName, SoundPart soundSource){
		super(SFXSystem.getSoundEventFromName(soundName), SoundCategory.MASTER);
		this.volume=1;
		this.repeat=true;
		Vec3d sourcePos = soundSource.getSoundPosition(); 
		this.xPosF = (float) sourcePos.xCoord;
		this.yPosF = (float) sourcePos.yCoord;
		this.zPosF = (float) sourcePos.zCoord;
		this.soundSource = soundSource;
		this.player = Minecraft.getMinecraft().thePlayer;
	}
	
	@Override
	public void update(){
		if(soundSource.shouldSoundBePlaying()){
			this.xPosF = (float) player.posX;
			this.yPosF = (float) player.posY;
			this.zPosF = (float) player.posZ;
			Vec3d playerPos = new Vec3d(player.posX, player.posY, player.posZ);
			Vec3d sourcePos = soundSource.getSoundPosition();
			
			if(soundSource != null){
				if(soundSource.equals(player.getRidingEntity()) || (soundSource instanceof AMultipartPart && ((AMultipartPart) soundSource).multipart.equals(player.getRidingEntity()))){
					this.pitch = soundSource.getSoundPitch();
					if(SFXSystem.isPlayerInsideEnclosedVehicle()){
						this.volume = 0.5F;
					}else{
						this.volume = 1.0F;
					}
					return;
				}
				double soundVelocity = playerPos.distanceTo(sourcePos) - playerPos.addVector(player.motionX, player.motionY, player.motionZ).distanceTo(sourcePos.add(soundSource.getSoundMotion()));
				this.pitch = (float) (soundSource.getSoundPitch()*(1+soundVelocity/10F));
				this.volume = (float) (soundSource.getSoundVolume()/playerPos.distanceTo(sourcePos));
				if(SFXSystem.isPlayerInsideEnclosedVehicle()){
					this.volume *= 0.5F;
				}
			}
		}else{
			this.donePlaying = true;
		}
	}
}
