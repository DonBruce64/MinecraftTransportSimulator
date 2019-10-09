package minecrafttransportsimulator.systems;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.VehicleSound;
import minecrafttransportsimulator.baseclasses.VehicleSound.SoundTypes;
import minecrafttransportsimulator.vehicles.main.EntityVehicleD_Moving;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.particle.ParticleDrip;
import net.minecraft.client.particle.ParticleSmokeNormal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.client.event.sound.SoundSetupEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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
	private static final String[] soundSystemNames = { "sndSystem", "field_148620_e" };
	private static final String[] soundSystemURLNames = { "getURLForSoundResource", "func_148612_a" };
	
	private static final List<String> playingSounds = new ArrayList<String>();
	private static SoundManager mcSoundManager;
	private static SoundSystem mcSoundSystem;
	private static Method getURLMethod;

	
	/**
	 * This runs right when the MC SoundSystem starts up.  We use this to get a reference to the system
	 * and add any custom codecs to it for playing other file formats.
	 */
	@SubscribeEvent
	public static void on(SoundSetupEvent event){
		mcSoundManager = event.getManager();
	}
	
	/**
	 * Populates the static soundsystem fields when called.  Used when either the regular or
	 * looping sound systems first try to play a sound and notice they are not populated yet.
	 */
	private static void initSoundSystemHooks(){
		Exception lastException = null;
		
		//Get the SoundSystem from the SoundManager.
		for(String soundSystemName : soundSystemNames){
			try{
				Field soundSystemField = SoundManager.class.getDeclaredField(soundSystemName);
				soundSystemField.setAccessible(true);
				mcSoundSystem = (SoundSystem) soundSystemField.get(mcSoundManager);
			}catch (Exception e){
				lastException = e;
				continue;
			}
		}
		if(mcSoundSystem == null){
			MTS.MTSLog.fatal("ERROR IN SOUND SYSTEM REFLECTION!  COULD NOT FIND SOUNDSYSTEM!");
			throw new RuntimeException(lastException);
		}
		
		//Also get the helper URL method for adding sounds from resource locations.
		for(String soundSystemURLName : soundSystemURLNames){
			try{
				getURLMethod = SoundManager.class.getDeclaredMethod(soundSystemURLName, ResourceLocation.class);
				getURLMethod.setAccessible(true);
			}catch (Exception e){
				lastException = e;
				continue;
			}
		}
		if(getURLMethod == null){
			MTS.MTSLog.fatal("ERROR IN SOUND SYSTEM REFLECTION!  COULD NOT FIND URLMETHOD!");
			throw new RuntimeException(lastException);
		}
	}
	
	/**
	 * Runs right after SoundSystem start.
	 * If we have the MC sound system saved, discard it as
	 * it has been reset and is no longer valid.
	 * Also clear out the playingSounds list as those sounds will have stopped.
	 */
	@SubscribeEvent
	public static void on(SoundLoadEvent event){
		mcSoundSystem = null;
		playingSounds.clear();
	}
	
	/**
	 * Make sure we stop any of the sounds that are running when the world closes.
	 */
	@SubscribeEvent
	public static void on(WorldEvent.Unload event){
		if(event.getWorld().isRemote){
			for(String soundID : playingSounds){
				if(mcSoundSystem.playing(soundID)){
					mcSoundSystem.stop(soundID);
				}
			}
			playingSounds.clear();
		}
	}
	
	/**
	 * Check for orphaned sounds, and delete them if they are present.
	 * We do this by verifying there is an entity with the given ID still present.
	 * This can happen if something changes the entityID of a vehicle mid-game.
	 * Usually another mod, but could be due to other reasons.
	 */
	@SubscribeEvent
    public static void on(RenderWorldLastEvent event){
		Iterator<String> soundIterator = playingSounds.iterator();
		while(soundIterator.hasNext()){
			String soundID = soundIterator.next();
			int entityID = Integer.valueOf(soundID.substring(0, soundID.indexOf('_')));
			if(Minecraft.getMinecraft().world.getEntityByID(entityID) == null){
				mcSoundSystem.stop(soundID);
				soundIterator.remove();
			}
		}
    }
	
	/**
	 * Returns true if a player is determined to be inside a vehicle.
	 * This is used to determine the volume of MTS sounds.
	 */
	public static boolean isPlayerInsideEnclosedVehicle(){
		if(Minecraft.getMinecraft().player != null && Minecraft.getMinecraft().player.getRidingEntity() instanceof EntityVehicleD_Moving){
			return !((EntityVehicleD_Moving) Minecraft.getMinecraft().player.getRidingEntity()).pack.general.openTop && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
		}else{
			return false;
		}
	}
	
	/**
	 * Plays a single sound.
	 */
	public static void playSound(Vec3d soundPosition, String soundName, float volume, float pitch){
		if(Minecraft.getMinecraft().player != null && Minecraft.getMinecraft().world.isRemote){
			//If we don't have the running instance of the SoundSystem, get it now.
			if(mcSoundSystem == null){
				initSoundSystemHooks();
			}
			
			volume = isPlayerInsideEnclosedVehicle() ? volume*0.5F : volume;
			double soundDistance = Minecraft.getMinecraft().player.getPositionVector().distanceTo(soundPosition);
			
			try{
				ResourceLocation soundFileLocation = new ResourceLocation(soundName);
				soundFileLocation = new ResourceLocation(soundFileLocation.getResourceDomain(), "sounds/" + soundFileLocation.getResourcePath() + ".ogg");
				URL soundURL = (URL) getURLMethod.invoke(null, soundFileLocation);
				String soundTempName = mcSoundSystem.quickPlay(false, soundURL, soundFileLocation.toString(), false, (float) soundPosition.x, (float) soundPosition.y, (float) soundPosition.z, SoundSystemConfig.ATTENUATION_LINEAR, 16.0F);
				mcSoundSystem.setVolume(soundTempName, volume);
				mcSoundSystem.setPitch(soundTempName, pitch);
			}catch(Exception e){
				MTS.MTSLog.error("COULD NOT PLAY VEHICLE SOUND:" + soundName);
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Does sound updates for the vehicle sounds.
	 */
	public static void updateVehicleSounds(EntityVehicleE_Powered vehicle, float partialTicks){
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
		
		EntityPlayer player = Minecraft.getMinecraft().player;
		for(VehicleSound sound : vehicle.getSounds()){
			String soundID = sound.getSoundUniqueName();
			
			//First check to see if this source is active.
			if(sound.isSoundSourceActive() && sound.isSoundActive()){
				//If we haven't created the sound, and we should be playing it, create it now.
				if(!playingSounds.contains(soundID) && !Minecraft.getMinecraft().isGamePaused()){
					try{
						ResourceLocation soundFileLocation = new ResourceLocation(sound.getSoundName());
						soundFileLocation = new ResourceLocation(soundFileLocation.getResourceDomain(), "sounds/" + soundFileLocation.getResourcePath() + ".ogg");
						URL soundURL = (URL) getURLMethod.invoke(null, soundFileLocation);
						mcSoundSystem.newSource(false, soundID, soundURL, soundFileLocation.toString(), true, (float) sound.getPosX(), (float) sound.getPosY(), (float) sound.getPosZ(), SoundSystemConfig.ATTENUATION_LINEAR, 16.0F);
						mcSoundSystem.play(soundID);
						playingSounds.add(soundID);
					}catch(Exception e){
						MTS.MTSLog.error("COULD NOT PLAY LOOPING VEHICLE SOUND:" + sound.getSoundName());
						throw new RuntimeException(e);
					}
				}
				
				//If the sound is created, update it.
				if(playingSounds.contains(soundID)){
					mcSoundSystem.setVolume(soundID, sound.getVolume());
					mcSoundSystem.setPitch(soundID, sound.getPitch());
					//Set the position to 5 blocks from the player in the direction of the sound.
					//Don't worry about motion as that's used in the sound itself for the pitch.
					Vec3d soundNormalizedPosition = new Vec3d(sound.getPosX() - player.posX, sound.getPosY() - player.posY, sound.getPosZ() - player.posZ).normalize().scale(5).add(player.getPositionVector());
					mcSoundSystem.setPosition(soundID, (float) soundNormalizedPosition.x, (float) soundNormalizedPosition.y, (float) soundNormalizedPosition.z);
					if(Minecraft.getMinecraft().isGamePaused()){
						mcSoundSystem.pause(soundID);
					}else{
						mcSoundSystem.play(soundID);
					}
				}
				
			}else if(mcSoundSystem.playing(soundID)){
				//If we aren't supposed to be playing this source, and it's still playing, delete it. 
				mcSoundSystem.stop(soundID);
				playingSounds.remove(soundID);
			}
		}
	}
	
	/**
	 * Stops all sounds for the vehicle.  Normally, this happens automatically when the vehicle is removed,
	 * however it may not happen sometimes due to oddities in the thread systems.  This method is called
	 * whenever a vehicle is set as dead and is responsible for ensuring the sounds have indeed stopped.
	 */
	public static void stopVehicleSounds(EntityVehicleE_Powered vehicle){
		//Make sure we are dead now, otherwise the sounds will just start again.
		vehicle.setDead();
		for(VehicleSound sound : vehicle.getSounds()){
			String soundID = sound.getSoundUniqueName();
			if(playingSounds.contains(soundID)){
				mcSoundSystem.stop(soundID);
				playingSounds.remove(soundID);
			}
		}
	}
	
	public static void addVehicleEngineSound(EntityVehicleE_Powered vehicle, APartEngine engine){
		if(vehicle.world.isRemote){
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
