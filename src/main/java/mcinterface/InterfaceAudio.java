package mcinterface;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.sound.OGGDecoderOutput;
import minecrafttransportsimulator.sound.SoundInstance;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;

/**Interface for the audio system.  This is responsible for playing sound from vehicles/interactions.
 * As well as from the internal radio.  Note that this is mostly the same between versions, with
 * the exception of some of the internal OpenAL calls having different names.
 *
 * @author don_bruce
 */
@Mod.EventBusSubscriber(Side.CLIENT)
public class InterfaceAudio{
	/**Flag for game paused state.  Gets set when the game is paused.**/
	private static boolean isSystemPaused;
	
	/**Map of String-based file-names to Integer pointers to buffer locations.  Used for loading sounds into
	 * memory to prevent the need to load them every time they are played.**/
	private static final Map<String, Integer> dataSourceBuffers = new HashMap<String, Integer>();
	
	/**List of playing {@link SoundInstance} objects.**/
	private static final List<SoundInstance> playingSounds = new ArrayList<SoundInstance>();
	
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
		if(!AL.isCreated()){
			//Don't go any further if OpenAL isn't ready.
			return;
		}
		
		//Handle pause state logic.
		if(InterfaceGame.isGamePaused()){
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
		
		//If the world is null, we need to stop all sounds as we're on the main screen.
		if(InterfaceGame.getClientWorld() == null){
			Iterator<SoundInstance> iterator = queuedSounds.iterator();
    		while(iterator.hasNext()){
    			iterator.remove();
    		}
    		for(SoundInstance playingSound : playingSounds){
    			playingSound.stop();
    		}
		}
		
		//Add all queued sounds to playing sounds and start them.
		for(SoundInstance sound : queuedSounds){
			AL10.alSourcePlay(sound.sourceIndex);
			playingSounds.add(sound);
		}
		queuedSounds.clear();
		
		//Get the player for further calculations.
		WrapperPlayer player = InterfaceGame.getClientPlayer();
		
		//Update playing sounds.
		boolean soundSystemReset = false;
		Iterator<SoundInstance> iterator = playingSounds.iterator();
		while(iterator.hasNext()){
			SoundInstance sound = iterator.next();
			AL10.alGetError();
			int state = AL10.alGetSourcei(sound.sourceIndex, AL10.AL_SOURCE_STATE);

			//If we are an invalid name, it means the sound system was reset.
			//Blow out all buffers and restart all sounds.
			if(AL10.alGetError() == AL10.AL_INVALID_NAME){
				soundSystemReset = true;
				break;
			}
			
			if(state != AL10.AL_STOPPED){
				sound.provider.updateProviderSound(sound);
				if(sound.stopSound){
					AL10.alSourceStop(sound.sourceIndex);
				}else{
					//Update position.
					FloatBuffer providerPosbuffer = sound.provider.getProviderPosition();
					AL10.alSource(sound.sourceIndex, AL10.AL_POSITION, providerPosbuffer);
					
					//If the player is inside an enclosed vehicle, half the sound volume.
					if(InterfaceGame.shouldSoundBeDampened(sound)){
						AL10.alSourcef(sound.sourceIndex, AL10.AL_GAIN, sound.volume/2F);
					}else{
						AL10.alSourcef(sound.sourceIndex, AL10.AL_GAIN, sound.volume);
					}
					
					//If the sound is looping, and the player isn't riding the source, calculate doppler pitch effect.
					//Otherwise, set pitch as normal.
					if(sound.looping && !sound.provider.equals(player.getEntityRiding())){
						Point3d providerVelocity = sound.provider.getProviderVelocity();
						Point3d playerVelocity = player.getVelocity();
						playerVelocity.y = 0;
						double initalDelta = player.getPosition().add((double)-providerPosbuffer.get(0), (double)-providerPosbuffer.get(1), (double)-providerPosbuffer.get(2)).length();
						double finalDelta = player.getPosition().add(playerVelocity).add((double)-providerPosbuffer.get(0), (double)-providerPosbuffer.get(1), (double)-providerPosbuffer.get(2)).add(-providerVelocity.x, 0D, -providerVelocity.z).length();
						float dopplerFactor = (float) (initalDelta > finalDelta ? 1 + (initalDelta - finalDelta)/initalDelta : 1 - (finalDelta - initalDelta)/finalDelta);
						AL10.alSourcef(sound.sourceIndex, AL10.AL_PITCH, sound.pitch*dopplerFactor);
					}else{
						AL10.alSourcef(sound.sourceIndex, AL10.AL_PITCH, sound.pitch);
					}

					//Update rolloff distance, which is based on pitch.
					AL10.alSourcef(sound.sourceIndex, AL10.AL_ROLLOFF_FACTOR, 1F/(0.25F + 3*sound.pitch));

					//If we are a radio, check for more data.  Also set the number of buffers we have.
					if(sound.radio != null){									
						//Do we have any processed buffers?  If so, use one to store more data.
						if(AL10.alGetSourcei(sound.sourceIndex, AL10.AL_BUFFERS_PROCESSED) > 0){										
							//We have free buffers.  See if we have data to fill them with before we un-bind them.										
							ByteBuffer decoderData = sound.radio.getSampleBuffer();
		            		if(decoderData != null){
		            			//Have data and buffers.  Remove buffer from queue and put it back in.
								IntBuffer doneBuffer = BufferUtils.createIntBuffer(1);
								AL10.alSourceUnqueueBuffers(sound.sourceIndex, doneBuffer);
		            			AL10.alBufferData(doneBuffer.get(0),  AL10.AL_FORMAT_MONO16, decoderData, sound.radio.getSampleRate());
		            			AL10.alSourceQueueBuffers(sound.sourceIndex, doneBuffer);
		            		}
						}
						sound.radio.updateBufferCounts((byte) (AL10.alGetSourcei(sound.sourceIndex, AL10.AL_BUFFERS_QUEUED) - AL10.alGetSourcei(sound.sourceIndex, AL10.AL_BUFFERS_PROCESSED)));
					}
				}
			}else{
				//We are a stopped sound.  Un-bind and delete any sources and buffers we are using.
				if(sound.radio != null){
					IntBuffer usedBuffers = BufferUtils.createIntBuffer(AL10.alGetSourcei(sound.sourceIndex, AL10.AL_BUFFERS_PROCESSED));
					AL10.alSourceUnqueueBuffers(sound.sourceIndex, usedBuffers);
					AL10.alDeleteBuffers(usedBuffers);
					
					//Attempt to queue up the next song as this one is done.
					//Only do this if the radio wasn't stopped manually.
					if(!sound.stopSound){
						sound.radio.queueNext();
					}
				}
				
				//Detach all buffers from the source, delete it, and remove the sound.
				AL10.alSourcei(sound.sourceIndex, AL10.AL_BUFFER, AL10.AL_NONE);
				IntBuffer sourceBuffer = (IntBuffer) BufferUtils.createIntBuffer(1).put(sound.sourceIndex).flip();
				AL10.alDeleteSources(sourceBuffer);
				iterator.remove();
			}
		}
		
		//If the sound system was reset, blow out all saved data points.
		if(soundSystemReset){
			dataSourceBuffers.clear();
			for(SoundInstance sound : playingSounds){
				sound.provider.restartSound(sound);
			}
			playingSounds.clear();
			sourceGetFailures = 0;
		}
	}
	
	/**
	 *  Plays a sound file located in a jar without buffering.
	 *  Useful for quick sounds like gunshots or button presses.
	 */
	public static void playQuickSound(SoundInstance sound){
		if(AL.isCreated() && sourceGetFailures < 10){
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
					InterfaceNetwork.sendToAllClients(new PacketPlayerChatMessage("IMMERSIVE VEHICLES ERROR: Tried to play a sound, but was told no sound slots were available.  Some mod is taking up all the sound slots.  Probabaly Immersive Railroading.  Sound will not play."));
					return;
				}
				sound.sourceIndex = sourceBuffer.get(0);
				
				//Set properties and bind data buffer to source.
				AL10.alGetError();
				AL10.alSourcei(sound.sourceIndex, AL10.AL_LOOPING, sound.looping ? AL10.AL_TRUE : AL10.AL_FALSE);
				AL10.alSource(sound.sourceIndex, AL10.AL_POSITION, sound.provider.getProviderPosition());
	    	    AL10.alSourcei(sound.sourceIndex, AL10.AL_BUFFER, dataBufferPointer);
	    	    
				//Done setting up buffer.  Queue sound to start playing.
				queuedSounds.add(sound);
			}
		}
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
			OGGDecoderOutput decoderOutput = InterfaceOGGDecoder.parseWholeOGGFile(soundName);
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
	 *  Plays a streaming sound (one with a Radio attached).  This uses a buffer-based system for loading
	 *  as it puts chunks (frames?) of the data into a buffer queue rather than loading it all into one buffer and
	 *  binding it to the source.  As the buffer queue is used up, the audio system will release the buffers and
	 *  parse more data.  When the source runs out of buffers, the radio will be queried for another source.
	 */
    public static void playStreamedSound(SoundInstance sound){
    	if(AL.isCreated() && sourceGetFailures < 10){
	    	//Create 5 buffers to be used as rolling storage for the stream.
			IntBuffer dataBuffers = BufferUtils.createIntBuffer(5);
			AL10.alGenBuffers(dataBuffers);
			
			//Now decode data for each dataBuffer.
			while(dataBuffers.hasRemaining()){
				//Get the raw decoder output and bind it to the data buffers.
				//Note that we may not obtain this data if we haven't decoded it yet.
				ByteBuffer decoderData = sound.radio.getSampleBuffer();
				if(decoderData != null){
					AL10.alBufferData(dataBuffers.get(), AL10.AL_FORMAT_MONO16, decoderData, sound.radio.getSampleRate());
				}
			}
			//Flip the data buffers to prepare them for reading.
			dataBuffers.flip();
			
			//Data has been buffered.  Now get source index.
			//If we have an old source, use that index instead to allow smooth streaming.
			IntBuffer sourceBuffer = BufferUtils.createIntBuffer(1);
			AL10.alGetError();
			AL10.alGenSources(sourceBuffer);
			if(AL10.alGetError() != AL10.AL_NO_ERROR){
				++sourceGetFailures;
				AL10.alDeleteBuffers(dataBuffers);
				InterfaceNetwork.sendToAllClients(new PacketPlayerChatMessage("IMMERSIVE VEHICLES ERROR: Tried to play a sound, but was told no sound slots were available.  Some mod is taking up all the slots.  Probabaly Immersive Railroading.  Sound will not play."));
				return;
			}
			sound.sourceIndex = sourceBuffer.get(0);
			
			//Have source and data.  Queue sound to start playing.
			AL10.alSourceQueueBuffers(sound.sourceIndex, dataBuffers);
			queuedSounds.add(sound);
    	}
	}
	
	/**
	 *  Combines a stereo-sampled ByteBufer into a mono-sampled one.
	 *  This allows us to use mono-only sounds that support attenuation.
	 */
	public static ByteBuffer stereoToMono(ByteBuffer stereoBuffer){
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
     * Update all sounds every client tick.
     */
    @SubscribeEvent
    public static void on(TickEvent.ClientTickEvent event){
    	//Only do updates at the end of a phase to prevent double-updates.
        if(event.phase.equals(Phase.END)){
			//We put this into a try block as sound system reloads can cause the thread to get stopped mid-execution.
			try{
				InterfaceAudio.update();
			}catch(Exception e){
				//e.printStackTrace();
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
    			if(iterator.next().provider.getProviderDimension() == event.getWorld().provider.getDimension()){
    				iterator.remove();
    			}
    		}
    		for(SoundInstance playingSound : playingSounds){
    			if(playingSound.provider.getProviderDimension() == event.getWorld().provider.getDimension()){
    				playingSound.stop();
    			}
    		}
    		
    		//Mark world as un-paused and update sounds to stop the ones that were just removed.
            isSystemPaused = false;
            update();
    	}
    }
}