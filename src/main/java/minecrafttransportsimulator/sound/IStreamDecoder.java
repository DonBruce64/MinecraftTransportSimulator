package minecrafttransportsimulator.sound;

import java.nio.ByteBuffer;

/**Decoder interface.  All decoders that play streaming music via {@link Radio}s
 * needs to implement this interface.
 *
 * @author don_bruce
 */
public interface IStreamDecoder{
    
	/**
	 *  Reads a block of data and returns it as a ByteBuffer.
	 *  Note that this buffer is re-used, so do NOT make multiple
	 *  calls to this method without storing the data somewhere in
	 *  between them.  Once no more blocks are available this method
	 *  will return null.
	 */
    public ByteBuffer readBlock();
    
    /**
	 *  Stops the decoding process.  This ensures all I/O
	 *  references like streams are safely closed, allowing for
	 *  this decoder to be stopped prior to the end of the stream.
	 */
    public void stop();

    public boolean isStereo();

    public int getSampleRate();
}