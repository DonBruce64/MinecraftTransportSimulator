package minecrafttransportsimulator.mcinterface;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.entities.instances.EntityRadio;
import minecrafttransportsimulator.sound.IStreamDecoder;
import minecrafttransportsimulator.sound.RadioStation;
import minecrafttransportsimulator.sound.SoundInstance;

/**
 * Interface for the sound system.  This is responsible for playing sound from vehicles/interactions.
 * As well as from the internal radio.
 *
 * @author don_bruce
 */
public interface IInterfaceSound {

    /**
     * Plays a sound file located in a jar without buffering.
     * Useful for quick sounds like gunshots or button presses.
     * If the sound is able to be played, it is added to its provider's sound list,
     * though it may not be playing yet due to update cycles.  Returns true, unless
     * the sound instance couldn't be started due to an audio issue (missing file,
     * lack of buffers, audio system not ready, etc.)
     */
    boolean playQuickSound(SoundInstance sound);

    /**
     * Adds a station to be queued for updates.  This should only be done once upon station construction.
     */
    void addRadioStation(RadioStation station);

    /**
     * Adds a new radio sound source, and queues it up with the buffer indexes passed-in.
     * Unlike the quick sound, this does not queue the sound added.  This means that the
     * method must be called from the main update loop somewhere at the parent call in the
     * stack to avoid a CME.
     */
    void addRadioSound(SoundInstance sound, List<Integer> buffers);

    /**
     * Buffers a ByteBuffer's worth of data from a streaming decoder.
     * Returns the index of the integer to where this buffer is stored.
     */
    int createBuffer(ByteBuffer buffer, IStreamDecoder decoder);

    /**
     * Deletes a buffer of station data.  Used when all radios are done playing the buffer,
     * or if the station switches buffers out.
     */
    void deleteBuffer(int bufferIndex);

    /**
     * Binds the passed-in buffer to the passed-in source index.
     */
    void bindBuffer(SoundInstance sound, int bufferIndex);

    /**
     * Checks if the passed-in radios all have a free buffer.  If so,
     * then the free buffer index is returned, and the buffer is-unbound
     * from all the sounds for all the radios.  If the radios are invalid
     * or not synced, then they are turned off for safety.
     */
    int getFreeStationBuffer(Set<EntityRadio> playingRadios);
}