package minecrafttransportsimulator.systems;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.VehicleSound;
import minecrafttransportsimulator.baseclasses.VehicleSound.SoundTypes;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.multipart.parts.APartEngine;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
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

	/**
	 * Plays a single sound in the same way as World.playSound.
	 * Placed here for ease of version updates and to allow custom volumes.
	 */
	public static void playSound(Vec3d soundPosition, String soundName, float volume, float pitch){
		if(Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().theWorld.isRemote){
			volume = isPlayerInsideEnclosedVehicle() ? volume*0.5F : volume;
			double soundDistance = Minecraft.getMinecraft().thePlayer.getPositionVector().distanceTo(soundPosition);
	        PositionedSoundRecord sound = new PositionedSoundRecord(getSoundEventFromName(soundName), SoundCategory.MASTER, volume, pitch, (float)soundPosition.xCoord, (float)soundPosition.yCoord, (float)soundPosition.zCoord);
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
	
	public static void doSound(EntityMultipartE_Vehicle vehicle, World world){
		if(world.isRemote){
			soundHandler = Minecraft.getMinecraft().getSoundHandler();
			//If we are a new vehicle without sounds, init them.
			//If we are old, we can assume to not have any sounds right now.
			if(vehicle.soundsNeedInit){
				vehicle.initSounds();
				vehicle.soundsNeedInit = false;
			}
			for(VehicleSound sound : vehicle.getSounds()){
				if(!sound.isDonePlaying() && !soundHandler.isSoundPlaying(sound) && sound.isSoundActive()){
					soundHandler.playSound(sound);
				}
			}
		}
	}
	
	public static void addVehicleEngineSound(EntityMultipartE_Vehicle vehicle, APartEngine engine){
		if(vehicle.worldObj.isRemote){
			vehicle.addSound(SoundTypes.ENGINE, engine);
		}
	}
	
	public static void doFX(FXPart part, World world){
		if(world.isRemote){
			part.spawnParticles();
		}
	}
	
	public static boolean isPlayerInsideEnclosedVehicle(){
		if(Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().thePlayer.getRidingEntity() instanceof EntityMultipartD_Moving){
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
	
	public static interface FXPart{
		@SideOnly(Side.CLIENT)
		public abstract void spawnParticles();
	}
}
