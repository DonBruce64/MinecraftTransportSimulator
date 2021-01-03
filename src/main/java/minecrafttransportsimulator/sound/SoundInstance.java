package minecrafttransportsimulator.sound;

import minecrafttransportsimulator.mcinterface.IInterfaceAudio;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Class that holds sound information.  One class is created for each sound that's playing
 * in the {@link IInterfaceAudio} system.  This class holds data such as the current
 * source the sound is playing from, whether the sound is supposed to be looping or not, etc.  
 * Setting {@link #stopSound} will stop this sound immediately, while {@link #streaming} tells
 * the audio system that this sound needs to be read in via chunks rather than all at once.
 *
 * @author don_bruce
 */
public class SoundInstance{
	public final ISoundProviderSimple provider;
	public final String soundName;
	public final boolean looping;
	public final Radio radio;
	
	//Runtime variables.
	public int sourceIndex;
	public float volume = 1.0F;
	public float pitch = 1.0F;
	public boolean stopSound = false;
	
	public SoundInstance(ISoundProviderSimple provider, String soundName){
		this.provider = provider;
		this.soundName = soundName;
		this.looping = false;
		this.radio = null;
	}
	
	public SoundInstance(ISoundProviderComplex provider, String soundName, boolean looping){
		this.provider = provider;
		this.soundName = soundName;
		this.looping = looping;
		this.radio = null;
	}

	public SoundInstance(IRadioProvider provider, String soundName, boolean looping, Radio radio){
		this.provider = provider;
		this.soundName = soundName;
		this.looping = looping;
		this.radio = radio;
	}
	
	/**
	 *  Flags the sound as stopped.  This will cause the audio system to stop
	 *  playing it the next update call.
	 */
	public void stop(){
		this.stopSound = true;
	}
	
	/**
	 *  Returns true if the sound should be dampened.
	 *  Used if we are in an enclosed vehicle and in first-person mode.
	 *  If the sound is streaming, and the vehicle is the provider, it is
	 *  assumed the sound is the vehicle radio, so it should NOT be dampened.
	 */
	public boolean shouldBeDampened(){
		AEntityBase entityRiding = MasterLoader.gameInterface.getClientPlayer().getEntityRiding();
		return entityRiding instanceof EntityVehicleF_Physics && !((EntityVehicleF_Physics) entityRiding).definition.general.openTop && MasterLoader.gameInterface.inFirstPerson() && (radio == null || !entityRiding.equals(provider));
	}
}
