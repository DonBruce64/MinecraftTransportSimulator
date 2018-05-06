package minecrafttransportsimulator.systems;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.particle.ParticleDrip;
import net.minecraft.client.particle.ParticleSmokeNormal;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class SFXSystem{
	private static SoundHandler soundHandler;
	private static final Map<String, SoundEvent> cachedSoundEvents = new HashMap<String, SoundEvent>();
	private static final Map<SoundPart, ISound> soundMap = new HashMap<SoundPart, ISound>();

	/**
	 * Plays a single sound in the same way as World.playSound.
	 * Placed here for ease of version updates and to allow custom volumes.
	 */
	public static void playSound(Vec3d soundPosition, String soundName, float volume, float pitch){
		volume = isPlayerInsideEnclosedVehicle() ? volume*0.5F : volume;
		double soundDistance = Minecraft.getMinecraft().thePlayer.getPositionVector().distanceTo(soundPosition);
        PositionedSoundRecord sound = new PositionedSoundRecord(getSoundEventFromName(soundName), SoundCategory.MASTER, volume, pitch, (float)soundPosition.xCoord, (float)soundPosition.xCoord, (float)soundPosition.xCoord);
        if(soundDistance > 10.0D){
        	Minecraft.getMinecraft().getSoundHandler().playDelayedSound(sound, (int)(soundDistance/2));
        }else{
        	Minecraft.getMinecraft().getSoundHandler().playSound(sound);
        }
	}
	
	public static SoundEvent getSoundEventFromName(String name){
		if(!cachedSoundEvents.containsKey(name)){
			SoundEvent newEvent = new SoundEvent(new ResourceLocation(name));
			cachedSoundEvents.put(name, newEvent);
		}
		return cachedSoundEvents.get(name);
	}
	
	public static void doSound(SoundPart part, World world){
		if(world.isRemote){
			soundHandler = Minecraft.getMinecraft().getSoundHandler();
			if(part.shouldSoundBePlaying()){
				if(!soundMap.containsKey(part)){
					soundMap.put(part, part.getNewSound());
				}
				if(!soundHandler.isSoundPlaying(soundMap.get(part))){
					soundHandler.playSound(soundMap.get(part));
				}
			}else if(soundMap.containsKey(part)){
				soundMap.remove(part);
			}
		}
	}
	
	public static void doFX(FXPart part, World world){
		if(world.isRemote){
			part.spawnParticles();
		}
	}
	
	public static boolean isPlayerInsideEnclosedVehicle(){
		if(Minecraft.getMinecraft().thePlayer.getRidingEntity() instanceof EntityMultipartD_Moving){
			return !((EntityMultipartD_Moving) Minecraft.getMinecraft().thePlayer.getRidingEntity()).pack.general.openTop && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
		}else{
			return false;
		}
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
	
	public static class WhiteSmokeFX extends ParticleSmokeNormal{
		public WhiteSmokeFX(World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ){
			super(world, posX, posY, posZ, motionX, motionY, motionZ, 1.0F);
		}
		
		@Override
		public void onUpdate(){
			super.onUpdate();
			this.setRBGColorF(1, 1, 1);
		}
	}
	
	public static interface SoundPart{		
		@SideOnly(Side.CLIENT)
		public MovingSound getNewSound();
		
		@SideOnly(Side.CLIENT)
		public abstract boolean shouldSoundBePlaying();
		
		@SideOnly(Side.CLIENT)
		public abstract Vec3d getSoundPosition();
		
		@SideOnly(Side.CLIENT)
		public abstract Vec3d getSoundMotion();
		
		@SideOnly(Side.CLIENT)
		public abstract float getSoundVolume();
		
		@SideOnly(Side.CLIENT)
		public abstract float getSoundPitch();
	}
	
	public static interface FXPart{
		@SideOnly(Side.CLIENT)
		public abstract void spawnParticles();
	}
}
