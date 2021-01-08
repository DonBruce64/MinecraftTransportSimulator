package minecrafttransportsimulator.sound;

import java.nio.ByteBuffer;

/**Class to hold output format for decoded sound files.
 * Note that this class is only used with wholly-parsed files.
 * Streaming is handled by the decoder class itself.
 *
 * @author don_bruce
 */
public class DecodedFile{	
	public final boolean isStereo;
	public final int sampleRate;
	public final ByteBuffer decodedData;
	
	public DecodedFile(boolean isStereo, int sampleRate, ByteBuffer decodedData){
		this.isStereo = isStereo;
		this.sampleRate = sampleRate;
		this.decodedData = decodedData;
	}
}