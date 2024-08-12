package minecrafttransportsimulator.sound;

import java.nio.ByteBuffer;

import minecrafttransportsimulator.entities.instances.EntityRadio;

/**
 * Decoder interface.  All decoders that play streaming music via {@link EntityRadio}s
 * needs to implement this interface.
 *
 * @author don_bruce
 */
public interface IStreamDecoder {
    /**
     * The max number of samples (shorts) that should be read in any given call to {@link #readBlock()}.
     * This is the amount read from the file, NOT sent up to the audio system: stereo files will return half
     * as may samples in their buffers due to them parsing twice as much data a combining it into one mono stream.
     **/
    int MAX_READ_SIZE = 96 * 1024 / 2;
    /**
     * The max size of the buffer (in bytes (2 bytes per sample)) to be returned in any given call to {@link #readBlock()}.
     **/
    int BUFFER_SIZE = 128 * 1024;

    /**
     * Reads a block of data and returns it as a ByteBuffer.
     * Note that this buffer is re-used, so do NOT make multiple
     * calls to this method without storing the data somewhere in
     * between them.  Once no more blocks are available this method
     * will return null.
     */
    ByteBuffer readBlock();

    /**
     * Stops the decoding process.  This ensures all I/O
     * references like streams are safely closed, allowing for
     * this decoder to be stopped prior to the end of the stream.
     */
    void stop();

    /**
     * Combines a stereo-sampled ByteBufer into a mono-sampled one.
     * This allows us to use mono-only sounds that support attenuation.
     * This should be done prior to sending the finalized buffer returned in
     * {@link #readBlock()}, if the source sound file is non-mono as all systems
     * expect a mono sound stream.
     */
    static ByteBuffer stereoToMono(ByteBuffer stereoBuffer) {
        ByteBuffer monoBuffer = ByteBuffer.allocateDirect(stereoBuffer.limit() / 2);
        while (stereoBuffer.hasRemaining()) {
            //Combine samples using little-endian ordering.
            byte[] sampleSet = new byte[4];
            stereoBuffer.get(sampleSet);
            int leftSample = (sampleSet[1] << 8) | (sampleSet[0] & 0xFF);
            int rightSample = (sampleSet[3] << 8) | (sampleSet[2] & 0xFF);
            int combinedSample = (leftSample + rightSample) / 2;
            monoBuffer.put((byte) (combinedSample & 0xFF));
            monoBuffer.put((byte) (combinedSample >> 8));
        }
        monoBuffer.flip();
        return monoBuffer;
    }

    int getSampleRate();
}