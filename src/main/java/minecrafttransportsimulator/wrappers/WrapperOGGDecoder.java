package minecrafttransportsimulator.wrappers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.ByteBuffer;

import minecrafttransportsimulator.sound.IStreamDecoder;
import minecrafttransportsimulator.sound.OGGDecoderOutput;
import paulscode.sound.SoundBuffer;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.codecs.CodecJOrbis;

/**Wrapper for loading OGG files.  While open-source libraries exist for loading OGG files,
 * they all are horridly complex and painful to use.  Since MC at least knows how to load
 * OGG files, we piggyback off of that code.  Uses PaulsCode codecs in 1.12, and OpenAL
 * libraries in later versions.
 *
 * @author don_bruce
 */
public class WrapperOGGDecoder implements IStreamDecoder{
    /**Decoder that decodes the file.**/
    private final CodecJOrbis decoder;
    /**Buffer used to store decoded data that can be sent to OpenAL.**/
    private final ByteBuffer decodedDataBuffer;
    /**Stereo or not.  Needed to tell OpenAL how to parse the bytes.**/
    private final boolean isStereo;
    /**Sample rate.  Required for correct playback speed.**/
    private final int sampleRate;
    
    private boolean firstRead = true;

    public WrapperOGGDecoder(URL soundURL){
    	//Create a new decoder.
		decoder = new CodecJOrbis();
		decoder.initialize(soundURL);
		SoundBuffer decoderOutputBuffer = decoder.read();
		//Need to allocate double the buffer space.  Cause PaulsCode lies about their max size.
		//They stop the loop AFTER the size is exceeded!
		decodedDataBuffer = ByteBuffer.allocateDirect(SoundSystemConfig.getStreamingBufferSize()*2);
		decodedDataBuffer.put(decoderOutputBuffer.audioData).flip();
    	
		//Get the audio format data.
		this.isStereo = decoderOutputBuffer.audioFormat.getChannels() == 2;
		this.sampleRate = (int) decoderOutputBuffer.audioFormat.getSampleRate();
    }

    @Override
    public ByteBuffer readBlock(){
    	//If this is the first read, just return the already-decoded block.
    	//If the EOS has been reached, return null.
    	if(firstRead){
    		firstRead = false;
    		return decodedDataBuffer;
    	}else if(decoder.endOfStream()){
    		return null;
    	}else{
    		//Not EOS or first read.  Parse some data and return it.
    		SoundBuffer outputBuffer = decoder.read();
    		decodedDataBuffer.clear();
    		decodedDataBuffer.put(outputBuffer.audioData);
    		decodedDataBuffer.flip();
    		return decodedDataBuffer;
    	}
    }
    
    @Override
    public void abort(){
    	decoder.cleanup();
    }

    @Override
    public boolean isStereo(){
        return isStereo;
    }

    @Override
    public int getSampleRate(){
        return sampleRate;
    }
	
	
	/**
	 *  Parses a sound file from the classpath, returning it completely parsed.
	 *  This should only be done for small sounds that are played frequently, not large music files.
	 */
	public static OGGDecoderOutput parseWholeOGGFile(String soundName){
		try{
			//Get the whole OGG data from the decoder.
			CodecJOrbis decoder = new CodecJOrbis();
			URL soundURL = new URL(null, "mtssounds:" + soundName + ".ogg", resourceStreamHandler);
			decoder.initialize(soundURL);
			SoundBuffer decoderOutput = decoder.readAll();
			ByteBuffer decoderData = (ByteBuffer) ByteBuffer.allocateDirect(decoderOutput.audioData.length).put(decoderOutput.audioData).flip();
			return new OGGDecoderOutput(decoderOutput.audioFormat.getChannels() == 2, (int) decoderOutput.audioFormat.getSampleRate(), decoderData);
		}catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}
	
	private static final URLStreamHandler resourceStreamHandler = new ResourceStreamHandler();
	/**
	 *  StreamHandler for OGG files.  Used for interfacing with PaulsCode OGG codec.
	 */
	private static class ResourceStreamHandler extends URLStreamHandler{
		public ResourceStreamHandler(){}
		
        protected URLConnection openConnection(URL connection){
            return new URLConnection(connection){
                public void connect() throws IOException{}
                
                public InputStream getInputStream() throws IOException{
                	String soundName = connection.getFile();
                	String soundDomain = soundName.substring(0, soundName.indexOf(':'));
                	soundName = soundName.substring(soundDomain.length() + 1);
                	return WrapperOGGDecoder.class.getResourceAsStream("/assets/" +  soundDomain + "/sounds/" + soundName);
                }
            };
        }
    };
}