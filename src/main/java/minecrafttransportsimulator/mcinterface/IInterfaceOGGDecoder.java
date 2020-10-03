package minecrafttransportsimulator.mcinterface;

import java.net.URL;

import minecrafttransportsimulator.sound.IStreamDecoder;
import minecrafttransportsimulator.sound.OGGDecoderOutput;

/**Interface for loading OGG files.  While open-source libraries exist for loading OGG files,
 * they all are horridly complex and painful to use.  Since MC at least knows how to load
 * OGG files, we piggyback off of that code.  Uses PaulsCode codecs in 1.12, and OpenAL
 * libraries in later versions.
 *
 * @author don_bruce
 */
public interface IInterfaceOGGDecoder{
    
	/**
	 *  Creates a new decoder from the passed-in URL.
	 */
    public IStreamDecoder createFrom(URL soundURL);
	
	/**
	 *  Parses a sound file from the classpath, returning it completely parsed.
	 *  This should only be done for small sounds that are played frequently, not large music files.
	 */
	public OGGDecoderOutput parseWholeOGGFile(String soundName);
}