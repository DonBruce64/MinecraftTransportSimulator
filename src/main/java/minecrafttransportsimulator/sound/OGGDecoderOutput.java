package minecrafttransportsimulator.sound;

import java.nio.ByteBuffer;

import mcinterface.InterfaceOGGDecoder;

/**Class to hold output format for OGG files.  This isn't an actual decoder, as
 * the decoder we use changes depending on the MC version.  See {@link InterfaceOGGDecoder}.
 * Note that this class is only used with wholly-parsed files.  Streaming is handled
 * by the wrapper directly like the MP3Decoder class.
 *
 * @author don_bruce
 */
public class OGGDecoderOutput{	
	public final boolean isStereo;
	public final int sampleRate;
	public final ByteBuffer decodedData;
	
	public OGGDecoderOutput(boolean isStereo, int sampleRate, ByteBuffer decodedData){
		this.isStereo = isStereo;
		this.sampleRate = sampleRate;
		this.decodedData = decodedData;
	}
}