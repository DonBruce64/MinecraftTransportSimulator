package minecrafttransportsimulator.sound;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;

/**Interface for the sound system.  This is responsible for playing sound from vehicles/interactions.
 * As well as from the internal radio.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceSound{
	/**Flag for game paused state.  Gets set when the game is paused.**/
	private static boolean isSystemPaused;
	
	/**Map of String-based file-names to Integer pointers to buffer locations.  Used for loading sounds into
	 * memory to prevent the need to load them every time they are played.**/
	private static final Map<String, Integer> dataSourceBuffers = new HashMap<String, Integer>();
	
	/**List of sounds currently playing.  Queued for updates every tick.**/
	private static final Set<SoundInstance> playingSounds = new HashSet<SoundInstance>();
	
	/**List of playing {@link RadioStation} objects.**/
	private static final List<RadioStation> playingStations = new ArrayList<RadioStation>();
	
	/**List of sounds to start playing next update.  Split from playing sounds to avoid CMEs and odd states.**/
	private static volatile List<SoundInstance> queuedSounds = new ArrayList<SoundInstance>();
	
	/**This gets incremented whenever we try to get a source and fail.  If we get to 10, the sound system
	 * will stop attempting to play sounds.  Used for when mods take all the sources.**/
	private static byte sourceGetFailures = 0;
	
	/**
	 *  Main update loop.  Call every tick to update playing sounds,
	 *  as well as queue up sounds that aren't playing yet but need to.
	 */
	public static void update(){
		if(!InterfaceClient.isSoundSystemReady()){
			//Don't go any further if OpenAL isn't ready.
			return;
		}
		
		//Handle pause state logic.
		if(InterfaceClient.isGamePaused()){
			if(!isSystemPaused){
				for(SoundInstance sound : playingSounds){
					AL10.alSourcePause(sound.sourceIndex);
				}
				isSystemPaused = true;
			}
			return;
		}else if(isSystemPaused){
			for(SoundInstance sound : playingSounds){
				AL10.alSourcePlay(sound.sourceIndex);
			}
			isSystemPaused = false;
		}
		
		//Get the player for further calculations.
		WrapperPlayer player = InterfaceClient.getClientPlayer();
		
		//If the client world is null, or we don't have a player we need to stop all sounds.
		if(InterfaceClient.getClientWorld() == null || player == null){
			queuedSounds.clear();
			for(SoundInstance sound : playingSounds){
				sound.stopSound = true;
			}
		}
		
		//Start playing all queued sounds.
		if(!queuedSounds.isEmpty()){
			for(SoundInstance sound : queuedSounds){
				AL10.alSourcePlay(sound.sourceIndex);
				playingSounds.add(sound);
			}
			queuedSounds.clear();
		}
		
		//Update playing sounds.
		boolean soundSystemReset = false;
		Iterator<SoundInstance> iterator = playingSounds.iterator();
		while(iterator.hasNext()){
			SoundInstance sound = iterator.next();
			AL10.alGetError();
			int state = AL10.alGetSourcei(sound.sourceIndex, AL10.AL_SOURCE_STATE);
			//If we are an invalid name, it means the sound system was reset.
			if(AL10.alGetError() == AL10.AL_INVALID_NAME){
				soundSystemReset = true;
				break;
			}
			
			if(state == AL10.AL_PLAYING){
				if(sound.stopSound){
					AL10.alSourceStop(sound.sourceIndex);
				}else{
					//Update position and volume.
					AL10.alSource3f(sound.sourceIndex, AL10.AL_POSITION, (float) sound.position.x, (float) sound.position.y, (float) sound.position.z);
					AL10.alSourcef(sound.sourceIndex, AL10.AL_GAIN, sound.volume);
					
					//If the sound is looping, and the player isn't riding the source, calculate doppler pitch effect.
					//Otherwise, set pitch as normal.
					if(sound.looping && !sound.entity.equals(player.getEntityRiding())){
						Point3d playerVelocity = player.getVelocity();
						playerVelocity.y = 0;
						double initalDelta = player.getPosition().subtract(sound.entity.position).length();
						double finalDelta = player.getPosition().add(playerVelocity).subtract(sound.entity.position).add(-sound.entity.motion.x, 0D, -sound.entity.motion.z).length();
						float dopplerFactor = (float) (initalDelta > finalDelta ? 1 + 0.25*(initalDelta - finalDelta)/initalDelta : 1 - 0.25*(finalDelta - initalDelta)/finalDelta);
						AL10.alSourcef(sound.sourceIndex, AL10.AL_PITCH, sound.pitch*dopplerFactor);
					}else{
						AL10.alSourcef(sound.sourceIndex, AL10.AL_PITCH, sound.pitch);
					}

					//Update rolloff distance, which is based on pitch.
					AL10.alSourcef(sound.sourceIndex, AL10.AL_ROLLOFF_FACTOR, 1F/(0.25F + 3*sound.pitch));
				}
			}else{
				//We are a stopped sound.  Un-bind and delete any sources and buffers we are using.
				if(sound.radio == null){
					//Normal sound. Un-bind buffer and make sure we're flagged as stopped.
					//We could have just reached the end of the sound.
					AL10.alSourcei(sound.sourceIndex, AL10.AL_BUFFER, AL10.AL_NONE);
					sound.stopSound = true;
				}else if(sound.stopSound){
					//Radio with stop command.  Un-bind all radio buffers.
					int boundBuffers = AL10.alGetSourcei(sound.sourceIndex, AL10.AL_BUFFERS_PROCESSED);
					if(boundBuffers > 0){
						IntBuffer buffers = BufferUtils.createIntBuffer(boundBuffers);
						AL10.alSourceUnqueueBuffers(sound.sourceIndex, buffers);
					}
				}
				if(sound.stopSound){
					//Sound was commanded to be stopped.  Delete sound source to free up slot.
					IntBuffer sourceBuffer = (IntBuffer) BufferUtils.createIntBuffer(1).put(sound.sourceIndex).flip();
					AL10.alDeleteSources(sourceBuffer);
					
					//Delete from playing list, and entity that has this sound.
					iterator.remove();
					sound.entity.sounds.remove(sound);
				}
			}
		}
		
		//Now update radio stations.
		for(RadioStation station : playingStations){
			station.update();
		}
		
		//If the sound system was reset, blow out all saved data points.
		if(soundSystemReset){
			InterfaceCore.logError("Had an invalid sound name.  Was the sound system reset?  Clearing all sounds, playing or not!");
			dataSourceBuffers.clear();
			for(SoundInstance sound : playingSounds){
				sound.entity.sounds.remove(sound);
    		}
			playingSounds.clear();
			sourceGetFailures = 0;
		}
	}
	
	/**
	 *  Plays a sound file located in a jar without buffering.
	 *  Useful for quick sounds like gunshots or button presses.
	 *  If the sound is able to be played, it is added to its provider's sound list,
	 *  though it may not be playing yet due to update cycles.
	 */
	public static void playQuickSound(SoundInstance sound){
		if(InterfaceClient.isSoundSystemReady() && sourceGetFailures < 10){
			//First get the IntBuffer pointer to where this sound data is stored.
			Integer dataBufferPointer = loadOGGJarSound(sound.soundName);
			if(dataBufferPointer != null){
				//Set the sound's source buffer index.
				IntBuffer sourceBuffer = BufferUtils.createIntBuffer(1);
				AL10.alGetError();
				AL10.alGenSources(sourceBuffer);
				if(AL10.alGetError() != AL10.AL_NO_ERROR){
					++sourceGetFailures;
					AL10.alDeleteBuffers(dataBufferPointer);
					InterfaceClient.getClientPlayer().displayChatMessage("IMMERSIVE VEHICLES ERROR: Tried to play a sound, but was told no sound slots were available.  Some mod is taking up all the slots.  Probabaly Immersive Railroading or Dynamic Surroundings.  If you have those installed, complain to the mod author or check the mod configs.  Sound will not play.");
					return;
				}
				sound.sourceIndex = sourceBuffer.get(0);
				
				//Set properties and bind data buffer to source.
				AL10.alGetError();
				AL10.alSourcei(sound.sourceIndex, AL10.AL_LOOPING, sound.looping ? AL10.AL_TRUE : AL10.AL_FALSE);
				AL10.alSource3f(sound.sourceIndex, AL10.AL_POSITION, (float) sound.entity.position.x, (float) sound.entity.position.y, (float) sound.entity.position.z);
	    	    AL10.alSourcei(sound.sourceIndex, AL10.AL_BUFFER, dataBufferPointer);
	    	    
				//Done setting up buffer.  Queue sound to start playing.
				queuedSounds.add(sound);
				sound.entity.sounds.add(sound);
			}
		}
	}
    
	/**
	 *  Adds a station to be queued for updates.  This should only be done once upon station construction.
	 */
    public static void addRadioStation(RadioStation station){
    	playingStations.add(station);
    }
    
    /**
	 *  Adds a new radio sound source, and queues it up with the buffer indexes passed-in.
	 *  Unlike the quick sound, this does not queue the sound added.  This means that the
	 *  method must be called from the main update loop somewhere at the parent call in the
	 *  stack to avoid a CME.
	 */
	public static void addRadioSound(SoundInstance sound, List<Integer> buffers){
		if(InterfaceClient.isSoundSystemReady() && sourceGetFailures < 10){
    		//Set the sound's source buffer index.
			IntBuffer sourceBuffer = BufferUtils.createIntBuffer(1);
			AL10.alGetError();
			AL10.alGenSources(sourceBuffer);
			if(AL10.alGetError() != AL10.AL_NO_ERROR){
				++sourceGetFailures;
				InterfaceClient.getClientPlayer().displayChatMessage("IMMERSIVE VEHICLES ERROR: Tried to play a sound, but was told no sound slots were available.  Some mod is taking up all the slots.  Probabaly Immersive Railroading or Dynamic Surroundings.  If you have those installed, complain to the mod author or check the mod configs.  Sound will not play.");
				return;
			}
			sound.sourceIndex = sourceBuffer.get(0);
			
			//Queue up the buffer sources to the source itself.
			for(int bufferIndex : buffers){
				bindBuffer(sound, bufferIndex);
			}
			queuedSounds.add(sound);
    	}
	}

	/**
	 *  Buffers a ByteBuffer's worth of data from a streaming decoder.
	 *  Returns the index of the integer to where this buffer is stored.
	 */
	public static int createBuffer(ByteBuffer buffer, IStreamDecoder decoder){
		if(decoder.isStereo()){
			buffer = stereoToMono(buffer);
		}
		IntBuffer newDataBuffer = BufferUtils.createIntBuffer(1);
		AL10.alGenBuffers(newDataBuffer);
		AL10.alBufferData(newDataBuffer.get(0),  AL10.AL_FORMAT_MONO16, buffer, decoder.getSampleRate());
		return newDataBuffer.get(0);
	}
	
	/**
	 *  Deletes a buffer of station data.  Used when all radios are done playing the buffer,
	 *  or if the station switches buffers out.
	 */
	public static void deleteBuffer(int bufferIndex){
		AL10.alDeleteBuffers(bufferIndex);
	}
	
	/**
	 *  Binds the passed-in buffer to the passed-in source index.
	 */
	public static void bindBuffer(SoundInstance sound, int bufferIndex){
		AL10.alSourceQueueBuffers(sound.sourceIndex, bufferIndex);
	}
	
	/**
	 *  Checks if the passed-in radios all have a free buffer.  If so,
	 *  then the free buffer index is returned, and the buffer is-unbound
	 *  from all the sounds for all the radios.  If the radios are invalid
	 *  or not synced, then they are turned off for safety.
	 */
	public static int getFreeStationBuffer(Set<Radio> playingRadios){
		boolean freeBuffer = true;
		Radio badRadio = null;
		AL10.alGetError();
		Iterator<Radio> iterator = playingRadios.iterator();
		while(iterator.hasNext()){
			Radio radio = iterator.next();
			SoundInstance sound = radio.getPlayingSound();
			if(AL10.alGetSourcei(sound.sourceIndex, AL10.AL_BUFFERS_PROCESSED) == 0){
				freeBuffer = false;
				break;
			}
			if(AL10.alGetError() == AL10.AL_INVALID_NAME){
				badRadio = radio;
			}
		}
		if(badRadio != null){
			badRadio.stop();
			return 0;
		}else if(freeBuffer){
			//First get the old buffer index.
			int freeBufferIndex = 0;
			IntBuffer oldDataBuffer = BufferUtils.createIntBuffer(1);
			for(Radio radio : playingRadios){
				SoundInstance sound = radio.getPlayingSound();
				AL10.alSourceUnqueueBuffers(sound.sourceIndex, oldDataBuffer);
				if(freeBufferIndex == 0){
					freeBufferIndex = oldDataBuffer.get(0);
				}else if(freeBufferIndex != oldDataBuffer.get(0)){
					badRadio = radio;
					break;
				}
			}
			if(badRadio != null){
				badRadio.stop();
				return 0;
			}else{
				return freeBufferIndex;
			}
		}else{
			return 0;
		}
	}
	
	/**
	 *  Combines a stereo-sampled ByteBufer into a mono-sampled one.
	 *  This allows us to use mono-only sounds that support attenuation.
	 */
	private static ByteBuffer stereoToMono(ByteBuffer stereoBuffer){
		ByteBuffer monoBuffer = ByteBuffer.allocateDirect(stereoBuffer.limit()/2);
		while(stereoBuffer.hasRemaining()){
			//Combine samples using little-endian ordering.
			byte[] sampleSet = new byte[4];
			stereoBuffer.get(sampleSet);
			int leftSample = (sampleSet[1] << 8) | (sampleSet[0] & 0xFF);
			int rightSample = (sampleSet[3] << 8) | (sampleSet[2] & 0xFF);
			int combinedSample = (leftSample + rightSample)/2;
			monoBuffer.put((byte) (combinedSample & 0xFF));
			monoBuffer.put((byte) (combinedSample >> 8));
		}
		return (ByteBuffer) monoBuffer.flip();
	}
	
	/**
	 *  Loads an OGG file in its entirety using the {@link InterfaceOGGDecoder}. 
	 *  The sound is then stored in a dataBuffer keyed by soundName located in {@link #dataSourceBuffers}.
	 *  The pointer to the dataBuffer is returned for convenience as it allows for transparent sound caching.
	 *  If a sound with the same name is passed-in at a later time, it is assumed to be the same and rather
	 *  than re-parse the sound the system will simply return the same pointer index to be bound.
	 */
	private static Integer loadOGGJarSound(String soundName){
		if(dataSourceBuffers.containsKey(soundName)){
			//Already parsed the data.  Return the buffer.
			return dataSourceBuffers.get(soundName);
		}else{
			//Need to parse the data.  Do so now.
			DecodedFile decoderOutput = OGGDecoder.parseInternalFile(soundName);
			if(decoderOutput != null){
				//Generate an IntBuffer to store a pointer to the data buffer.
				IntBuffer dataBufferPointers = BufferUtils.createIntBuffer(1);
		    	AL10.alGenBuffers(dataBufferPointers);
		    	
		    	//Bind the decoder output buffer to the data buffer pointer.
		    	//If we are stereo, convert the data before binding.
		    	ByteBuffer decoderData = decoderOutput.isStereo ? stereoToMono(decoderOutput.decodedData) : decoderOutput.decodedData;
		    	AL10.alBufferData(dataBufferPointers.get(0), AL10.AL_FORMAT_MONO16, decoderData, decoderOutput.sampleRate);
				
		    	//Done parsing.  Map the dataBuffer(s) to the soundName and return the index.
		    	dataSourceBuffers.put(soundName, dataBufferPointers.get(0));
		    	return dataSourceBuffers.get(soundName);
			}else{
				return null;
			}
		}
	}
	
	/**
     * Update all sounds every client tick.
     */
    @SubscribeEvent
    public static void on(TickEvent.ClientTickEvent event){
    	//Only do updates at the end of a phase to prevent double-updates.
        if(event.phase.equals(Phase.END)){
			//We put this into a try block as sound system reloads can cause the thread to get stopped mid-execution.
			try{
				update();
			}catch(Exception e){
				e.printStackTrace();
				//Do nothing.  We only get exceptions here if OpenAL isn't ready.
			}
        }
    }
	
	/**
     * Stop all sounds when the world is unloaded.
     */
    @SubscribeEvent
    public static void on(WorldEvent.Unload event){
    	if(event.getWorld().isRemote){
    		Iterator<SoundInstance> iterator = queuedSounds.iterator();
    		while(iterator.hasNext()){
    			if(event.getWorld().equals(iterator.next().entity.world.world)){
    				iterator.remove();
    			}
    		}
    		for(SoundInstance sound : playingSounds){
    			if(event.getWorld().equals(sound.entity.world.world)){
    				if(sound.radio != null){
    					sound.radio.stop();
    				}else{
    					sound.stopSound = true;
    				}
    			}
    		}
    		
    		//Mark world as un-paused and update sounds to stop the ones that were just removed.
            isSystemPaused = false;
            update();
    	}
    }
}