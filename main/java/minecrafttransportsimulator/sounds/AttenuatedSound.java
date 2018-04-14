package minecrafttransportsimulator.sounds;

import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.systems.SFXSystem.SFXEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;

public final class AttenuatedSound<SoundEntity extends Entity & SFXEntity> extends MovingSound{
	private final SoundEntity soundSource;
	private final EntityPlayer player;
		
	public AttenuatedSound(String soundName, SoundEntity soundSource){
		super(SFXSystem.getSoundEventFromName(soundName), SoundCategory.MASTER);
		this.volume=1;
		this.repeat=true;
		this.xPosF = (float) soundSource.posX;
		this.yPosF = (float) soundSource.posY;
		this.zPosF = (float) soundSource.posZ;
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
			Vec3d sourcePos = new Vec3d(soundSource.posX, soundSource.posY, soundSource.posZ);
			
			if(soundSource != null && !soundSource.isDead){
				if(player.getRidingEntity() instanceof EntitySeat){
					EntityMultipartParent playerParent = ((EntitySeat) player.getRidingEntity()).parent;
					EntityMultipartParent soundParent = null;
					if(soundSource instanceof EntityMultipartParent){
						soundParent = (EntityMultipartParent) soundSource;
					}else if(soundSource instanceof EntityMultipartChild){
						soundParent = ((EntityMultipartChild) soundSource).parent;
					}
					if(playerParent != null && soundParent != null && playerParent.equals(soundParent)){
						this.pitch = soundSource.getPitch();
						if(SFXSystem.isPlayerInsideEnclosedVehicle()){
							this.volume = 0.5F;
						}else{
							this.volume = 1.0F;
						}
						return;
					}
				}
				double soundVelocity = playerPos.distanceTo(sourcePos) - playerPos.addVector(player.motionX, player.motionY, player.motionZ).distanceTo(sourcePos.addVector(soundSource.motionX, soundSource.motionY, soundSource.motionZ));
				this.pitch = (float) (soundSource.getPitch()*(1+soundVelocity/10F));
				this.volume = (float) (soundSource.getVolume()/playerPos.distanceTo(sourcePos));
				if(SFXSystem.isPlayerInsideEnclosedVehicle()){
					this.volume *= 0.5F;
				}
			}
		}else{
			this.donePlaying=true;
			soundSource.setCurrentSound(null);
		}
	}
}
