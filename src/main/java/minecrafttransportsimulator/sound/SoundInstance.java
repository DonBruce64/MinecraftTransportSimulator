package minecrafttransportsimulator.sound;

import mcinterface.InterfaceAudio;

/**Class that holds sound information.  One class is created for each sound that's playing
 * in the {@link InterfaceAudio} system.  This class holds data such as the current
 * source the sound is playing from, whether the sound is supposed to be looping or not, etc.  
 * Setting {@link #stopSound} will stop this sound immediately, while {@link #streaming} tells
 * the audio system that this sound needs to be read in via chunks rather than all at once.
 *
 * @author don_bruce
 */
public class SoundInstance{
	public final ISoundProvider provider;
	public final String soundName;
	public final boolean looping;
	public final Radio radio;
	
	//Runtime variables.
	public int sourceIndex;
	public float volume = 1.0F;
	public float pitch = 1.0F;
	public boolean stopSound = false;
	
	public SoundInstance(ISoundProvider provider, String soundName){
		this(provider, soundName, false);
	}
	
	public SoundInstance(ISoundProvider provider, String soundName, boolean looping){
		this(provider, soundName, looping, null);
	}

	public SoundInstance(ISoundProvider provider, String soundName, boolean looping, Radio radio){
		this.provider = provider;
		this.soundName = soundName;
		this.looping = looping;
		if(radio == null || (radio != null && provider instanceof IRadioProvider)){
			this.radio = radio;
		}else{
			throw new IllegalArgumentException("ERROR: A sound played from a radio was attempted to be added to an object that isn't an instance of " + IRadioProvider.class.getSimpleName());
		}
	}
	
	public void stop(){
		this.stopSound = true;
	}
}
