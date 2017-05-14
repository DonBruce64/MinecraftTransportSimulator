package minecrafttransportsimulator.systems;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.particle.ParticleDrip;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class SFXSystem{
	private static SoundHandler soundHandler;
	private static final Map<String, SoundEvent> cachedSoundEvents = new HashMap<String, SoundEvent>();

	/**
	 * Plays a single sound in the same way as World.playSound.
	 * Placed here for ease of version updates.
	 */
	public static void playSound(Entity noisyEntity, String soundName, float volume, float pitch){
		if(noisyEntity.worldObj.isRemote){
			volume = isPlayerInsideVehicle() ? volume*0.5F : volume;
			double soundDistance = Minecraft.getMinecraft().thePlayer.getDistance(noisyEntity.posX, noisyEntity.posY, noisyEntity.posZ);
	        PositionedSoundRecord sound = new PositionedSoundRecord(getSoundEventFromName(soundName), SoundCategory.MASTER, volume, pitch, (float)noisyEntity.posX, (float)noisyEntity.posY, (float)noisyEntity.posZ);
	        if(soundDistance > 10.0D){
	        	Minecraft.getMinecraft().getSoundHandler().playDelayedSound(sound, (int)(soundDistance/2));
	        }else{
	        	Minecraft.getMinecraft().getSoundHandler().playSound(sound);
	        }
		}
	}
	
	public static SoundEvent getSoundEventFromName(String name){
		if(!cachedSoundEvents.containsKey(name)){
			SoundEvent newEvent = new SoundEvent(new ResourceLocation(name));
			cachedSoundEvents.put(name, newEvent);
		}
		return cachedSoundEvents.get(name);
	}
	
	/**
	 * Does SFX for entities of the appropriate type.
	 */
	public static void doSFX(SFXEntity entity, World world){
		if(world.isRemote){
			soundHandler = Minecraft.getMinecraft().getSoundHandler();
			if(entity.shouldSoundBePlaying() && (entity.getCurrentSound() == null || !soundHandler.isSoundPlaying(entity.getCurrentSound()))){
				entity.setCurrentSound(entity.getNewSound());
				soundHandler.playSound(entity.getCurrentSound());
			}
			entity.spawnParticles();
		}
	}
	
	public static boolean isPlayerInsideVehicle(){
		if(ClientEventSystem.playerLastSeat != null){
			if(ClientEventSystem.playerLastSeat.parent != null){
				if(!((EntityMultipartMoving) ClientEventSystem.playerLastSeat.parent).pack.general.openTop && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0){
					return true;
				}
			}
		}
		return false;
	}
	
	public static class OilDropParticleFX extends ParticleDrip{
		public OilDropParticleFX(World world, double posX, double posY, double posZ){
			super(world, posX, posY, posZ, Material.LAVA);
		}
		
		@Override
		public void onUpdate(){
			super.onUpdate();
			this.setRBGColorF(0, 0, 0);
		}
	}
	
	public static class FuelDropParticleFX extends ParticleDrip{
		public FuelDropParticleFX(World world, double posX, double posY, double posZ){
			super(world, posX, posY, posZ, Material.LAVA);
		}
	}
	
	public static interface SFXEntity{		
		@SideOnly(Side.CLIENT)
		public MovingSound getNewSound();
		
		@SideOnly(Side.CLIENT)
		public MovingSound getCurrentSound();
		
		@SideOnly(Side.CLIENT)
		public void setCurrentSound(MovingSound sound);
		
		@SideOnly(Side.CLIENT)
		public abstract boolean shouldSoundBePlaying();
		
		@SideOnly(Side.CLIENT)
		public abstract void spawnParticles();
	}
}
