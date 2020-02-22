package minecrafttransportsimulator.systems;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.VehicleSound;
import minecrafttransportsimulator.baseclasses.VehicleSound.SoundTypes;
import minecrafttransportsimulator.vehicles.main.EntityVehicleD_Moving;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
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
 * All methods call the PaulsCode SoundSystem class directly.  This is done to avoid having to make
 * sounds.json files for packs, as well as allowing us to bypass the stupid SoundEvent/ISound
 * crud MC thinks is so good.  If PaulsCode uses Strings for IDs, why can't MC?!
 *
 * @author don_bruce
 */
@Mod.EventBusSubscriber(Side.CLIENT)
@SideOnly(Side.CLIENT)
public final class VehicleSoundSystem{	
	//Reflection variables.
	private static final String[] soundSystemNames = { "sndSystem", "field_148620_e" };
	
	//Stream handlers.
	private static final URLStreamHandler fileStreamHandler = new FileStreamHandler();
	private static final URLStreamHandler resourceStreamHandler = new ResourceStreamHandler();
	
	private static final List<String> playingSounds = new ArrayList<String>();
	private static SoundManager mcSoundManager;
	private static SoundSystem mcSoundSystem;
	private static byte soundSystemStartupDelay = 0;

	
	//--------------------START OF EVENT HOOKS--------------------//
	
	/**
	 * This runs right when the MC Sound Manager starts up.  We use this to get a reference to the manager
	 * and add any custom codecs to it for playing other file formats.
	 */
	@SubscribeEvent
	public static void on(SoundSetupEvent event){
		mcSoundManager = event.getManager();
	}
	
	/**
	 * This runs when the MC Sound System starts up.  It won't be ready yet, so we
	 * can't get it through reflection here.  We will have to discard our instance of
	 * it if we have one, as it will be invalid.  We also need to clear out the
	 * playing sounds list, as all those sounds will have been stopped.
	 * To ensure we don't get the wrong instance of the SoundSystem, we use
	 * a small delay to allow the system to come online before we grab it.
	 */
	@SubscribeEvent
	public static void on(SoundLoadEvent event){
		mcSoundSystem = null;
		soundSystemStartupDelay = 50;
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
	 * We do this by verifying there is an entity with the given ID still present,
	 * and if the entity is still alive.  The not being alive will happen if the
	 * vehicle is removed, but the change in ID should NEVER happen as that'll
	 * break all sorts of packet code that depends on the ID being the same
	 * on servers and clients.  Usually other mods messing with things.
	 */
	@SubscribeEvent
    public static void on(RenderWorldLastEvent event){
		Iterator<String> soundIterator = playingSounds.iterator();
		while(soundIterator.hasNext()){
			String soundID = soundIterator.next();
			int entityID = Integer.valueOf(soundID.substring(0, soundID.indexOf('_')));
			if(Minecraft.getMinecraft().world.getEntityByID(entityID) == null || Minecraft.getMinecraft().world.getEntityByID(entityID).isDead){
				mcSoundSystem.stop(soundID);
				soundIterator.remove();
			}
		}
    }
	
	
	//--------------------START OF CUSTOM METHODS--------------------//
	/**
	 * Plays a single sound.  Format of soundName should be modID:soundFileName.  If this sound
	 * came from a vehicle, pass it in as a parameter.  This lets the system do volume calculations
	 * if it finds the the player is riding the vehicle.
	 */
	public static void playSound(Vec3d soundPosition, String soundName, float volume, float pitch, EntityVehicleE_Powered optionalVehicle){
		if(Minecraft.getMinecraft().player != null && Minecraft.getMinecraft().world.isRemote){
			//If we don't have the running instance of the SoundSystem, get it now.
			if(mcSoundSystem == null){
				if(soundSystemStartupDelay > 0){
					--soundSystemStartupDelay;
					return;
				}
				initSoundSystemHooks();
			}
			try{
				//Need to add the mtssounds: prefix as the URL will trim off the first section, leading to a bad parse.
				//Also set the position to 5 blocks from the player in the direction of the sound.
				//Don't worry about motion as that's used in the sound itself for the pitch.
				//We should worry if the sound doesn't exist.  Playing things that don't exist will fault the SoundSystem. 
				URL soundURL = new URL(null, "mtssounds:" + soundName + ".ogg", resourceStreamHandler);
				if(soundURL.openStream() != null){
					EntityPlayer player = Minecraft.getMinecraft().player;
					Vec3d soundNormalizedPosition = new Vec3d(soundPosition.x - player.posX, soundPosition.y - player.posY, soundPosition.z - player.posZ).normalize().scale(5).add(player.getPositionVector());
					String soundTempName = mcSoundSystem.quickPlay(false, soundURL, soundURL.getFile(), false, (float) soundNormalizedPosition.x, (float) soundNormalizedPosition.y, (float) soundNormalizedPosition.z, SoundSystemConfig.ATTENUATION_LINEAR, 16.0F);
					
					//If the player is not riding this vehicle, or the sound didn't come from a vehicle, reduce the volume by the distance.
					if(optionalVehicle != null && !optionalVehicle.equals(player.getRidingEntity())){
						volume /= player.getPositionVector().distanceTo(soundPosition);
					}

					//If the player is inside an enclosed vehicle, half the volume.
					if(isPlayerInsideEnclosedVehicle()){
						volume *= 0.5;
					}
					
					
					mcSoundSystem.setVolume(soundTempName, (float) (volume));
					mcSoundSystem.setPitch(soundTempName, pitch);
				}
			}catch(Exception e){
				MTS.MTSLog.error("COULD NOT PLAY VEHICLE SOUND:" + soundName);
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Does sound updates for the vehicle sounds.
	 */
	public static void updateVehicleSounds(EntityVehicleE_Powered vehicle){
		//If we don't have the running instance of the SoundSystem, get it now.
		if(mcSoundSystem == null){
			if(soundSystemStartupDelay > 0){
				--soundSystemStartupDelay;
				return;
			}
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
						//Format of soundName should be modID:soundFileName.
						//Need to add the mtssounds: prefix as the URL will trim off the first section, leading to a bad parse.
						URL soundURL = new URL(null, "mtssounds:" + sound.getSoundName() + ".ogg", resourceStreamHandler);
						mcSoundSystem.newSource(false, soundID, soundURL, soundURL.getFile(), true, (float) sound.getPosX(), (float) sound.getPosY(), (float) sound.getPosZ(), SoundSystemConfig.ATTENUATION_LINEAR, 16.0F);
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
	}
	
	/**
	 * Returns true if a player is determined to be inside a vehicle.
	 * This is used to determine the volume of MTS sounds.
	 */
	public static boolean isPlayerInsideEnclosedVehicle(){
		if(Minecraft.getMinecraft().player != null && Minecraft.getMinecraft().player.getRidingEntity() instanceof EntityVehicleD_Moving){
			return !((EntityVehicleD_Moving) Minecraft.getMinecraft().player.getRidingEntity()).definition.general.openTop && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
		}else{
			return false;
		}
	}
	
	/**
	 * Helper wrapper to add vehicle engine sounds to vehicles.
	 */
	public static void addVehicleEngineSound(EntityVehicleE_Powered vehicle, APartEngine<? extends EntityVehicleE_Powered> engine){
		if(vehicle.world.isRemote){
			vehicle.addSound(SoundTypes.ENGINE, engine);
		}
	}
	
	
	
	//--------------------START OF STREAM HANDLERS--------------------//
	private static class FileStreamHandler extends URLStreamHandler{
		public FileStreamHandler(){}
		
        protected URLConnection openConnection(URL connection){
            return new URLConnection(connection){
                public void connect() throws IOException{}
                
                public InputStream getInputStream() throws IOException{
                    return new FileInputStream(new File(connection.getFile()));
                }
            };
        }
    };
    
    private static class ResourceStreamHandler extends URLStreamHandler{
		public ResourceStreamHandler(){}
		
        protected URLConnection openConnection(URL connection){
            return new URLConnection(connection){
                public void connect() throws IOException{}
                
                public InputStream getInputStream() throws IOException{
                	String soundName = connection.getFile();
                	String packID = soundName.substring(0, soundName.indexOf(':'));
                	soundName = soundName.substring(packID.length() + 1);
                	return VehicleSoundSystem.class.getResourceAsStream("/assets/" +  packID + "/sounds/" + soundName);
                }
            };
        }
    };
}
