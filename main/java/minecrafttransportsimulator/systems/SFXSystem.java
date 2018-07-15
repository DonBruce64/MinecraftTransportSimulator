package minecrafttransportsimulator.systems;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.VehicleSound;
import minecrafttransportsimulator.baseclasses.VehicleSound.SoundTypes;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.multipart.parts.APartEngine;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.particle.ParticleDrip;
import net.minecraft.client.particle.ParticleSmokeNormal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;

/**This class handles all sounds for MTS.  Single sounds call playSound in a fashion similar
 * to default MC, while looping sounds are checked when doSound is called with a vehicle.
 * All methods call the paulscode SoundSystem class directly.  This is done to avoid having to make
 * sounds.json files for packs, as well as allowing us to bypass the stupid SoundEvent crud MC 
 * thinks is so good.  If paulscode uses Strings for IDs, why can't MC?!
 *
 * @author don_bruce
 */
@Mod.EventBusSubscriber(Side.CLIENT)
@SideOnly(Side.CLIENT)
public final class SFXSystem{	
	private static final String[] soundManagerNames = { "sndManager", "field_147694_f" };
	private static final String[] soundSystemNames = { "sndSystem", "field_148620_e" };
	private static final String[] soundSystemURLNames = { "getURLForSoundResource", "func_148612_a" };
	private static SoundSystem mcSoundSystem;
	private static Method getURLMethod;

	
	/**
	 * Populates the static soundsystem fields when called.  Used when either the regular or
	 * looping sound systems first try to play a sound and notice they are not populated yet.
	 */
	private static void initSoundSystemHooks(){
		try{
			//First get the SoundManager from the SoundHandler.
			SoundHandler soundHandler = Minecraft.getMinecraft().getSoundHandler();
			Field soundManagerField = ReflectionHelper.findField(SoundHandler.class, soundManagerNames);
			SoundManager mcSoundManager = (SoundManager) soundManagerField.get(soundHandler);
			
			//Now get the SoundSystem from the SoundManager. 
			Field soundSystemField = ReflectionHelper.findField(SoundManager.class, soundSystemNames);
			mcSoundSystem = (SoundSystem) soundSystemField.get(mcSoundManager);
			
			//Also get the helper URL method for adding sounds from resource locations.
			getURLMethod = ReflectionHelper.findMethod(SoundManager.class, mcSoundManager, soundSystemURLNames, ResourceLocation.class);
		}catch (Exception e){
			MTS.MTSLog.fatal("ERROR IN SOUND SYSTEM REFLECTION!");
			throw new RuntimeException(e);
   	    }
	}
	
	/**
	 * Runs right after SoundSystem start.
	 * If we have the MC sound system saved, discard it as
	 * it has been reset and is no longer valid.
	 */
	@SubscribeEvent
	public static void on(SoundLoadEvent event){
		mcSoundSystem = null;
	}
	
	/**
	 * Make sure we stop any of the sounds that are running when the world closes.
	 */
	@SubscribeEvent
	public static void on(WorldEvent.Unload event){
		if(event.getWorld().isRemote){
			for(Entity entity : event.getWorld().loadedEntityList){
				if(entity instanceof EntityMultipartE_Vehicle){
					for(VehicleSound sound : ((EntityMultipartE_Vehicle) entity).getSounds()){
						String soundID = sound.getSoundUniqueName();
						if(mcSoundSystem.playing(soundID)){
							mcSoundSystem.stop(soundID);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Returns true if a player is determined to be inside a vehicle.
	 * This is used to determine the volume of MTS sounds.
	 */
	public static boolean isPlayerInsideEnclosedVehicle(){
		if(Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().thePlayer.getRidingEntity() instanceof EntityMultipartD_Moving){
			return !((EntityMultipartD_Moving) Minecraft.getMinecraft().thePlayer.getRidingEntity()).pack.general.openTop && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
		}else{
			return false;
		}
	}
	
	/**
	 * Plays a single sound.
	 */
	public static void playSound(Vec3d soundPosition, String soundName, float volume, float pitch){
		if(Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().theWorld.isRemote){
			//If we don't have the running instance of the SoundSystem, get it now.
			if(mcSoundSystem == null){
				initSoundSystemHooks();
			}
			
			volume = isPlayerInsideEnclosedVehicle() ? volume*0.5F : volume;
			double soundDistance = Minecraft.getMinecraft().thePlayer.getPositionVector().distanceTo(soundPosition);
			
			try{
				ResourceLocation soundFileLocation = new ResourceLocation(soundName);
				soundFileLocation = new ResourceLocation(soundFileLocation.getResourceDomain(), "sounds/" + soundFileLocation.getResourcePath() + ".ogg");
				URL soundURL = (URL) getURLMethod.invoke(null, soundFileLocation);
				mcSoundSystem.quickPlay(false, soundURL, soundFileLocation.toString(), false, (float) soundPosition.xCoord, (float) soundPosition.yCoord, (float) soundPosition.zCoord, SoundSystemConfig.ATTENUATION_LINEAR, 16.0F);
			}catch(Exception e){
				MTS.MTSLog.error("COULD NOT PLAY VEHICLE SOUND:" + soundName);
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Does sound updates for the multipart vehicle sounds.
	 */
	public static void doSound(EntityMultipartE_Vehicle vehicle, World world){
		if(world.isRemote){
			//If we don't have the running instance of the SoundSystem, get it now.
			if(mcSoundSystem == null){
				initSoundSystemHooks();
			}
			
			//If we are a new vehicle without sounds, init them.
			//If we are old, we can assume to not have any sounds right now.
			if(vehicle.soundsNeedInit){
				vehicle.initSounds();
				vehicle.soundsNeedInit = false;
			}
			
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			for(VehicleSound sound : vehicle.getSounds()){
				String soundID = sound.getSoundUniqueName();
				if(sound.isSoundSourceActive() && sound.isSoundActive()){
					if(!mcSoundSystem.playing(soundID)){
						try{
							ResourceLocation soundFileLocation = new ResourceLocation(sound.getSoundName());
							soundFileLocation = new ResourceLocation(soundFileLocation.getResourceDomain(), "sounds/" + soundFileLocation.getResourcePath() + ".ogg");
							URL soundURL = (URL) getURLMethod.invoke(null, soundFileLocation);
							mcSoundSystem.newSource(false, soundID, soundURL, soundFileLocation.toString(), true, sound.getPosX(), sound.getPosY(), sound.getPosZ(), SoundSystemConfig.ATTENUATION_LINEAR, 16.0F);
							mcSoundSystem.play(soundID);
						}catch(Exception e){
							MTS.MTSLog.error("COULD NOT PLAY LOOPING VEHICLE SOUND:" + sound.getSoundName());
							throw new RuntimeException(e);
						}
					}
					mcSoundSystem.setVolume(soundID, sound.getVolume());
					mcSoundSystem.setPitch(soundID, sound.getPitch());
					mcSoundSystem.setPosition(soundID, sound.getPosX(), sound.getPosY(), sound.getPosZ());
				}else if(mcSoundSystem.playing(soundID)){
					mcSoundSystem.stop(soundID);
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
