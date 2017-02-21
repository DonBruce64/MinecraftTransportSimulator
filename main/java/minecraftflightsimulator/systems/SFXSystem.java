package minecraftflightsimulator.systems;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.particle.EntityDropParticleFX;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public final class SFXSystem{
	private static SoundHandler soundHandler;

	/**
	 * Plays a single sound in the same way as World.playSound.
	 * Placed here for ease of version updates.
	 */
	public static void playSound(Entity noisyEntity, String soundName, float volume, float pitch){
		if(noisyEntity.worldObj.isRemote){
			double soundDistance = Minecraft.getMinecraft().thePlayer.getDistance(noisyEntity.posX, noisyEntity.posY, noisyEntity.posZ);
	        PositionedSoundRecord sound = new PositionedSoundRecord(new ResourceLocation(soundName), volume, pitch, (float)noisyEntity.posX, (float)noisyEntity.posY, (float)noisyEntity.posZ);
	        if(soundDistance > 10.0D){
	        	Minecraft.getMinecraft().getSoundHandler().playDelayedSound(sound, (int)(soundDistance/2));
	        }else{
	        	Minecraft.getMinecraft().getSoundHandler().playSound(sound);
	        }
		}
	}
	
	/**
	 * Does SFX for entities of the appropriate type.
	 */
	public static void doSFX(SFXEntity entity, World world){
		if(world.isRemote){
			soundHandler = Minecraft.getMinecraft().getSoundHandler();
			if(entity.shouldSoundBePlaying() && entity.getCurrentSound() == null){
				entity.setCurrentSound(entity.getNewSound());
				soundHandler.playSound(entity.getCurrentSound());
			}
			entity.spawnParticles();
		}
	}
	
	public static class OilDropParticleFX extends EntityDropParticleFX{
		public OilDropParticleFX(World world, double posX, double posY, double posZ){
			super(world, posX, posY, posZ, Material.lava);
		}
		
		@Override
		public void onUpdate(){
			super.onUpdate();
			this.setRBGColorF(0, 0, 0);
		}
	}
	
	public static class FuelDropParticleFX extends EntityDropParticleFX{
		public FuelDropParticleFX(World world, double posX, double posY, double posZ){
			super(world, posX, posY, posZ, Material.lava);
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
