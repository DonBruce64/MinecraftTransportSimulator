package minecrafttransportsimulator.sound;

import java.nio.IntBuffer;

import minecrafttransportsimulator.wrappers.WrapperAudio;

/**Class that holds sound information.  One class is created for each sound that's playing
 * in the {@link WrapperAudio} system.  This class holds data such as the current
 * source the sound is playing from, the sound to play after this one (if so), whether
 * the sound is supposed to be looping or not, etc.  Setting {@link #stopSound} will 
 * stop this sound immediately, and will cause {@link #nextSoundName} not to be played.
 *
 * @author don_bruce
 */
public class SoundInstance{
	public final ISoundProvider provider;
	public final String soundName;
	public final SoundInstance nextSound;
	public final boolean streaming;
	public SoundInstance priorSound;
	
	//Runtime variables.
	public boolean stopSound = false;
	public boolean looping = false;
	public float volume = 1.0F;
	public float pitch = 1.0F;
	public IntBuffer sourceIndexes;
	
	public SoundInstance(ISoundProvider provider, String soundName){
		this(provider, soundName, false);
	}
	
	public SoundInstance(ISoundProvider provider, String soundName, boolean looping){
		this(provider, soundName, looping, null);
	}
	
	public SoundInstance(ISoundProvider provider, String soundName, boolean looping, SoundInstance nextSound){
		this.provider = provider;
		this.soundName = soundName;
		this.looping = looping;
		this.nextSound = nextSound;
		if(nextSound != null){
			this.nextSound.priorSound = this;
		}
		this.streaming = false;
	}
}
