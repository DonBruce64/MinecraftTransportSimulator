package minecrafttransportsimulator.wrappers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;

import minecrafttransportsimulator.sound.MP3Decoder;
import minecrafttransportsimulator.sound.SoundInstance;
import paulscode.sound.SoundBuffer;
import paulscode.sound.codecs.CodecJOrbis;

/**Wrapper for the audio system.  This is responsible for playing sound from vehicles/interactions.
 * As well as from the internal radio.  Note that this is mostly the same between versions, with
 * the exception of some of the internal OpenAL calls having different names.
 *
 * @author don_bruce
 */
public class WrapperAudio{
	/**Flag for game paused state.  Gets set when the game is paused.**/
	private static boolean isSystemPaused;
	
	/**Pool of sound source buffers.  These are recycled as sources stop using them.**/
	private static final IntBuffer soundSourceIndexes;
	
	/**Map of String-based file-names to dataBuffers.  Used for loading sounds into
	 * memory to prevent the need to load them every time they are played.**/
	private static final Map<String, IntBuffer> dataSourceBuffers = new HashMap<String, IntBuffer>();
	
	/**List of playing {@link SoundInstance} objects.  Is a CLQ to allow for add operations on the main
	 * thread and remove operations on the audio thread.**/
	private static final ConcurrentLinkedQueue<SoundInstance> playingSounds = new ConcurrentLinkedQueue<SoundInstance>();
	
	/**Map of sources as an Integer value from #soundSourceIndexes to MP3Parser objects.  Used to feed
	 * sound sources from their data source parser objects during update loops.**/
	private static final Map<Integer, MP3Decoder> mp3SourceParsers = new HashMap<Integer, MP3Decoder>();
	
	private static final WrapperPlayer player;
	private static final FloatBuffer playerPosition = BufferUtils.createFloatBuffer(3);
	private static final FloatBuffer playerVelocity = BufferUtils.createFloatBuffer(3);
	private static final FloatBuffer playerOrientation = BufferUtils.createFloatBuffer(6);
	
	/**
	 *  Inits this class.  Static as we only need to do this this the first time we play a sound.
	 */
	static{
		try{
    		//Create the OpenAL system if we haven't already.
			if(!AL.isCreated()){
    			AL.create();
    			AL10.alGetError();
    		}
	    }catch(LWJGLException e){
	    	e.printStackTrace();
	    	//This should NEVER happen.  OpenAl is in all MC as it comes with lwjgl.
	    }
		
		//Get 32 sources (playing bits) for sounds.  We might need to share with MC in later versions...
		//As such, check the source indexes to make sure they all exist.  If not, set the limit to that number.
		soundSourceIndexes = BufferUtils.createIntBuffer(32);
		AL10.alGenSources(soundSourceIndexes);
		while(soundSourceIndexes.hasRemaining()){
			if(!AL10.alIsSource(soundSourceIndexes.get())){
	    		soundSourceIndexes.flip();
	    		break;
	    	}
		}
		if(!soundSourceIndexes.hasRemaining()){
			soundSourceIndexes.flip();
		}
		
		//Get a reference to the player.  This will be stored to be updated every
		//audio tick to allow us to update sound accordingly.
		player = WrapperGame.getClientPlayer();
		player.putPosition(playerPosition);
		player.putVelocity(playerVelocity);
		player.putOrientation(playerOrientation);
		
		//Set listener initial parameters.
		AL10.alListener(AL10.AL_POSITION,    playerPosition);
	    AL10.alListener(AL10.AL_VELOCITY,    playerVelocity);
		AL10.alListener(AL10.AL_ORIENTATION, playerOrientation);
		
		//Start the updater thread.
		new AudioUpdateThread().start();
	}
	
	/**
	 *  Thread to update the audio system.
	 *  More specifically, it updates the player position, and thus the audio 
	 *  profile of all sounds.  Additionally, all playing sounds are queried to
	 * 	update their pitch if required.
	 */
	public static class AudioUpdateThread extends Thread{
		@Override
		public void run(){
			//Keep running until the main game instance dies.
			while(true){
				//Handle pause state logic.
				if(WrapperGame.isGamePaused()){
					if(!isSystemPaused){
						for(SoundInstance sound : playingSounds){
							for(int i=0; i<sound.sourceIndexes.capacity(); ++i){
								AL10.alSourcePause(sound.sourceIndexes.get(i));
							}
						}
						isSystemPaused = true;
					}
				}else if(isSystemPaused){
					for(SoundInstance sound : playingSounds){
						for(int i=0; i<sound.sourceIndexes.capacity(); ++i){
							AL10.alSourcePlay(sound.sourceIndexes.get(i));
						}
					}
					isSystemPaused = false;
				}
				
				if(!isSystemPaused){
					//Update playing sounds.
					Iterator<SoundInstance> iterator = playingSounds.iterator();
					while(iterator.hasNext()){
						SoundInstance sound = iterator.next();
						boolean soundHasPlayingSources = false;
						for(int i=0; i<sound.sourceIndexes.capacity(); ++i){
							int currentSourceIndex = sound.sourceIndexes.get(i);
							int state = AL10.alGetSourcei(currentSourceIndex, AL10.AL_SOURCE_STATE);
							
							if(!sound.streaming){
								if(state != AL10.AL_STOPPED){
									sound.provider.updateProviderSound(sound);
									if(sound.stopSound){
										//If the sound has a following sound, play that.
										//Otherwise, stop the source.
										//First remove all bufferes queued, then stop the source and remove one buffer.
										if(sound.nextSound != null){
											AL10.alSourceUnqueueBuffers(currentSourceIndex, BufferUtils.createIntBuffer(AL10.alGetSourcei(currentSourceIndex, AL10.AL_BUFFERS_PROCESSED)));
											AL10.alSourceStop(currentSourceIndex);
											//AL10.alSourceUnqueueBuffers(currentSourceIndex, BufferUtils.createIntBuffer(AL10.alGetSourcei(currentSourceIndex, 1)));
											//AL10.alSourcePlay(currentSourceIndex);
										}else{
											AL10.alSourceStop(currentSourceIndex);
										}
									}else{
										//If the player is inside an enclosed vehicle, half the sound volume.
										if(WrapperGame.shouldSoundBeDampened()){
											AL10.alSourcef(currentSourceIndex, AL10.AL_GAIN, 0.5F);
										}else{
											AL10.alSourcef(currentSourceIndex, AL10.AL_GAIN, 1.0F);
										}
										
										//Update sound source properties.
										AL10.alSource(currentSourceIndex, AL10.AL_POSITION, sound.provider.getProviderPosition());
							    	    AL10.alSource(currentSourceIndex, AL10.AL_VELOCITY, sound.provider.getProviderVelocity());
							    	    AL10.alSourcei(currentSourceIndex, AL10.AL_LOOPING, sound.looping ? AL10.AL_TRUE : AL10.AL_FALSE);
							    	    AL10.alSourcef(currentSourceIndex, AL10.AL_PITCH, sound.pitch);
										AL10.alSourcef(currentSourceIndex, AL10.AL_ROLLOFF_FACTOR, 1F/(0.25F + 3*sound.pitch));
							    	    soundHasPlayingSources = true;
									}
								}
							}
						}
						if(!soundHasPlayingSources){
							iterator.remove();
						}
					}
					
					//Update player position velocity, and orientation.
					//Don't need to re-bind these as they are buffers.
					//Note that PaulsCode does this for us in 1.12.2, so we don't need to do that.
					//However, it does foul up velocity, so wee need to re-do that one.
					//We also need to check if the doppler has been set to 0.
					//MC does that somewhere in the the code as it's 0 during the update cycle.
					//player.putPosition(playerPosition);
					player.putVelocity(playerVelocity);
					//player.putOrientation(playerOrientation);
				    AL10.alListener(AL10.AL_VELOCITY, playerVelocity);
					if(AL10.alGetFloat(AL10.AL_DOPPLER_FACTOR) == 0){
						//Sound normally is at a speed of 17.15m/tick.
						//But the doppler equations assume m/second.
						//We need to set the factor of 1/20 here to fix that.
						//We the divide it by 2 again to lower the speed down a little further to account for MC's slow movement.
						AL10.alDopplerVelocity(1F/20F/2F);
						AL10.alDopplerFactor(1.0F);
					}
				}
				
				//Sleep for a tick.
				try{
					System.out.println("SLEEP");
					Thread.sleep(1000/20);
				}catch(InterruptedException e){}
			}
		}
	}
	
	/**
	 *  Plays a sound file located in a jar without buffering.
	 *  Useful for quick sounds like gunshots or button presses.
	 */
	public static void playQuickSound(SoundInstance sound){
		//First get the IntBuffer pointer to where this sound data is stored.
		IntBuffer dataBuffer = loadJarSound(sound.soundName);
		if(dataBuffer != null){
			//Set up the IntBuffer for the sound to hold sources.  May be more than one.
			//If we have a prior sound, use that one instead.
			if(sound.priorSound != null){
				sound.sourceIndexes = sound.priorSound.sourceIndexes;
			}else{
				sound.sourceIndexes = BufferUtils.createIntBuffer(dataBuffer.capacity());
			}
			
			//Loop through the dataBuffer components of the sound.  Depends on how many channels it has.
			for(int i=0; i<dataBuffer.capacity(); ++i){
				//If we don't have a prior sound, we need to get a new source.
				if(sound.priorSound == null){
					sound.sourceIndexes.put(i, getFreeSource());
				}
				
				//If we have a source, bind a buffer of data to it.
				int currentSourceIndex = sound.sourceIndexes.get(i);
				if(currentSourceIndex != -1){
		    	    AL10.alSource(currentSourceIndex, AL10.AL_POSITION, sound.provider.getProviderPosition());
		    	    AL10.alSource(currentSourceIndex, AL10.AL_VELOCITY, sound.provider.getProviderVelocity());
		    	    AL10.alSourcei(currentSourceIndex, AL10.AL_LOOPING, sound.looping ? AL10.AL_TRUE : AL10.AL_FALSE);
		    	    
		    	    //If this SoundInstance is part of a chain, enqueue it to the source.
		    	    //We know if this is true if it has a prior or next sound.   
		    	    if(sound.priorSound != null || sound.nextSound != null){
		    	    	AL10.alSourceQueueBuffers(currentSourceIndex, dataBuffer.get(i));
		    	    	//Set the source to playing to keep the free source checker from grabbing it again.
		    	    }else{
		    	    	AL10.alSourcei(currentSourceIndex, AL10.AL_BUFFER,	dataBuffer.get(i));
		    	    }
		    	    
		    	    //Start the source playing.  This keeps us from grabbing it as a free buffer in subsequent loops.
		    	    AL10.alSourcePlay(currentSourceIndex);
				}
			}
			
			//Add the sound to the playing sounds list.
			//Only add the first sound.  The next sound plays after the first is done.
			if(sound.priorSound == null){
				playingSounds.add(sound);
			}
			
			//If the sound has a nextSound, parse that one too.
			if(sound.nextSound != null){
				playQuickSound(sound.nextSound);
			}
		}
	}

	/**
	 *  Loads a sound file (MP3 or OGG) by parsing it from the classpath. 
	 *  The sound is then stored in a dataBuffer keyed by soundName located in {@link #dataSourceBuffers}.
	 *  The pointer to the dataBuffer is returned for convenience as it allows for transparent sound caching.
	 *  If a sound with the same name is passed-in at a later time, it is assumed to be the same and rather
	 *  than re-parse the sound the system will simply return the same pointer index to be bound.
	 *  Note that this loading routine will load the whole file into memory, so no  4-hour music files unless you want your RAM gone.
	 *  Also note that the InputStream may be anything, be it a file, resource in a jar, or something else.
	 */
	private static IntBuffer loadJarSound(String soundName){
		if(dataSourceBuffers.containsKey(soundName)){
			//Already parsed the data.  Return the buffer.
			return dataSourceBuffers.get(soundName);
		}
		//Haven't parsed the data, do so now.
		//Get the sound data stream from the jar.
    	String soundDomain = soundName.substring(0, soundName.indexOf(':'));
    	String soundLocation = soundName.substring(soundDomain.length() + 1);
    	InputStream stream = WrapperAudio.class.getResourceAsStream("/assets/" + soundDomain + "/sounds/" + soundLocation + ".ogg");
    	if(stream == null){
    		return null;
    	}
    	
    	
    	//Parse the stream into a buffer, and buffer that data to a dataBuffer.
		try{
			CodecJOrbis decoder = new CodecJOrbis();
			URL soundURL = new URL(null, "mtssounds:" + soundName + ".ogg", resourceStreamHandler);
			decoder.initialize(soundURL);
			SoundBuffer decoderOuput = decoder.readAll();
			ByteBuffer decoderData = ByteBuffer.allocateDirect(decoderOuput.audioData.length);
			decoderData.put(decoderOuput.audioData).flip();
			
			//Generate IntBuffers to store the data pointer.
			//If we are stereo, we need two.
			IntBuffer dataBuffer = BufferUtils.createIntBuffer(decoderOuput.audioFormat.getChannels());
	    	AL10.alGenBuffers(dataBuffer);
			
			//If we are a stereo source, make us mono.  We need this for attenuation.
			//We do this by simply splitting the audio and making a second sound source.
			//We'd use mono averaging here, but it creates artifacts on most sounds.
			if(decoderOuput.audioFormat.getChannels() == 2){
				ByteBuffer leftChannel = ByteBuffer.allocateDirect(decoderOuput.audioData.length/2);
				ByteBuffer rightChannel = ByteBuffer.allocateDirect(decoderOuput.audioData.length/2);
				while(decoderData.hasRemaining()){
					leftChannel.putShort(decoderData.getShort());
					rightChannel.putShort(decoderData.getShort());
				}
				leftChannel.flip();
				rightChannel.flip();
				
				AL10.alBufferData(dataBuffer.get(0),  AL10.AL_FORMAT_MONO16, leftChannel, (int) decoderOuput.audioFormat.getSampleRate());
				AL10.alBufferData(dataBuffer.get(1),  AL10.AL_FORMAT_MONO16, rightChannel, (int) decoderOuput.audioFormat.getSampleRate());
			}else{
				AL10.alBufferData(dataBuffer.get(0),  AL10.AL_FORMAT_MONO16, decoderData, (int) decoderOuput.audioFormat.getSampleRate());
			}
			stream.close();
	    	
	    	//Done parsing.  Map the dataBuffer to the soundName and return the index.
	    	dataSourceBuffers.put(soundName, dataBuffer);
	    	return dataSourceBuffers.get(soundName);
		}catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}
	
	
	private static final URLStreamHandler resourceStreamHandler = new ResourceStreamHandler();
	/**
	 *  StreamHandler for OGG files.  Used for interfacing with PaulsCode OGG codec.
	 */
	private static class ResourceStreamHandler extends URLStreamHandler{
		public ResourceStreamHandler(){}
		
        protected URLConnection openConnection(URL connection){
            return new URLConnection(connection){
                public void connect() throws IOException{}
                
                public InputStream getInputStream() throws IOException{
                	String soundName = connection.getFile();
                	String soundDomain = soundName.substring(0, soundName.indexOf(':'));
                	soundName = soundName.substring(soundDomain.length() + 1);
                	return WrapperAudio.class.getResourceAsStream("/assets/" +  soundDomain + "/sounds/" + soundName);
                }
            };
        }
    };
	
	/**
	 *  Plays a MP3 file from the passed-in InputStream.  This is read in via chunks, and may be sent over the
	 *  network via buffers if so desired as long as the InputSream implementation supports it.
	 */
	public static void playMP3File(InputStream stream, float x, float y, float z){
		int sourceIndex = getFreeSource();
		if(sourceIndex != -1){
			//We have a free buffer, setup a MP3 parser.
			MP3Decoder parser = new MP3Decoder(stream);
			
			//Generate 8 buffers (will be 1mb once parsed) for use in the parser.
			IntBuffer dataBuffer = BufferUtils.createIntBuffer(8);
			AL10.alGenBuffers(dataBuffer);
			
			//Prime the source with the 10 buffers worth of data.
			for(byte i=0; i<dataBuffer.capacity();++i){
    	    	AL10.alBufferData(dataBuffer.get(i), AL10.AL_FORMAT_STEREO16, parser.readBlock(), parser.getSampleRate());
    	    }
    	    AL10.alSourceQueueBuffers(sourceIndex, dataBuffer);
		}
		
		/*
		 *     	MP3Parser parser = new MP3Parser(stream);
    	ShortBuffer parsedBuffer = parser.readBlock();
    	while(parsedBuffer != null){
    		AL10.alBufferData(dataBuffer.get(0), AL10.AL_FORMAT_STEREO16, parsedBuffer, parser.getSampleRate());
    		parsedBuffer = parser.readBlock();
    	}
		 */
	}
	
	/**
	 *  Returns a free source from the pool of sources.  Used when we need to start playing a sound.
	 *  If no source is free, -1 is returned.
	 */
	private static int getFreeSource(){
		int sourceState;
		while(soundSourceIndexes.hasRemaining()){
			int sourceIndex = soundSourceIndexes.get();
			sourceState = AL10.alGetSourcei(sourceIndex, AL10.AL_SOURCE_STATE);
			if(sourceState == AL10.AL_INITIAL || sourceState == AL10.AL_STOPPED){
				//Found a valid source.  Rewind the buffer and return.
				soundSourceIndexes.rewind();
				return sourceIndex;
			}
		}
		soundSourceIndexes.rewind();
		return -1;
	}
}