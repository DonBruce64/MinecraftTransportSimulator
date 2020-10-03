package minecrafttransportsimulator.mcinterface;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.sound.IStreamDecoder;
import minecrafttransportsimulator.sound.Radio;
import minecrafttransportsimulator.sound.RadioStation;
import minecrafttransportsimulator.sound.SoundInstance;

/**Interface for the audio system.  This is responsible for playing sound from vehicles/interactions.
 * As well as from the internal radio.  Note that this is mostly the same between versions, with
 * the exception of some of the internal OpenAL calls having different names.
 *
 * @author don_bruce
 */
public interface IInterfaceAudio{
	
	/**
	 *  Main update loop.  Call every tick to update playing sounds,
	 *  as well as queue up sounds that aren't playing yet but need to.
	 */
	public void update();
	
	/**
	 *  Plays a sound file located in a jar without buffering.
	 *  Useful for quick sounds like gunshots or button presses.
	 */
	public void playQuickSound(SoundInstance sound);
    
    /**
	 *  Adds a station to be queued for updates.  This should only be done once upon station construction.
	 */
    public void addRadioStation(RadioStation station);
    
    /**
	 *  Adds a new radio sound source, and queues it up with the buffer indexes passed-in.
	 *  Unlike the quick sound, this does not queue the sound added.  This means that the
	 *  method must be called from the main update loop somewhere at the parent call in the
	 *  stack to avoid a CME.
	 */
	public void addRadioSound(SoundInstance sound, List<Integer> buffers);

	/**
	 *  Buffers a ByteBuffer's worth of data from a streaming decoder.
	 *  Returns the index of the integer to where this buffer is stored.
	 */
	public int createBuffer(ByteBuffer buffer, IStreamDecoder decoder);
	
	/**
	 *  Deletes a buffer of station data.  Used when all radios are done playing the buffer,
	 *  or if the station switches buffers out.
	 */
	public void deleteBuffer(int bufferIndex);
	
	/**
	 *  Binds the passed-in buffer to the passed-in source index.
	 */
	public void bindBuffer(SoundInstance sound, int bufferIndex);
	
	/**
	 *  Checks if the passed-in radios all have a free buffer.  If so,
	 *  then the free buffer index is returned, and the buffer is-unbound
	 *  from all the sounds for all the radios.  If the radios are invalid
	 *  or not synced, then they are turned off for safety.
	 */
	public int getFreeStationBuffer(Set<Radio> playingRadios);
}