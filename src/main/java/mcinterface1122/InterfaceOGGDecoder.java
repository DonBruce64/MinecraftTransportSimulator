package mcinterface1122;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import minecrafttransportsimulator.mcinterface.IInterfaceOGGDecoder;
import minecrafttransportsimulator.sound.IStreamDecoder;
import minecrafttransportsimulator.sound.OGGDecoderOutput;
import paulscode.sound.SoundBuffer;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.codecs.CodecJOrbis;

class InterfaceOGGDecoder implements IInterfaceOGGDecoder{
	
	@Override
	public IStreamDecoder createFrom(URL soundURL){
		return new OGGDecoder(soundURL);
	}
	
	@Override
	public OGGDecoderOutput parseWholeOGGFile(String soundName){
		try{
			//Get the whole OGG data from the decoder.
			CodecJOrbis decoder = new CodecJOrbis();
			URL soundURL = new URL(null, "mtssounds:" + soundName + ".ogg", resourceStreamHandler);
			decoder.initialize(soundURL);
			SoundBuffer decoderOutput = decoder.readAll();
			ByteBuffer decoderData = (ByteBuffer) ByteBuffer.allocateDirect(decoderOutput.audioData.length).put(decoderOutput.audioData).flip();
			return new OGGDecoderOutput(decoderOutput.audioFormat.getChannels() == 2, (int) decoderOutput.audioFormat.getSampleRate(), decoderData);
		}catch(Exception e){
			return null;
		}
	}
	
	/**
	 *  Decoder for OGG files.  Used for interfacing with PaulsCode OGG codec.
	 */
	private static class OGGDecoder implements IStreamDecoder{
	    /**Decoder that decodes the file.**/
	    private final CodecJOrbis decoder;
		/**Thread to decode the file.**/
		private final DecoderThread decoderThread;
		/**Buffers created on the decoding thread are stored here after parsing.**/
		private final ConcurrentLinkedQueue<SoundBuffer> decoderOutputBuffers = new ConcurrentLinkedQueue<SoundBuffer>();
	    /**Buffer used to store decoded data that can be sent to OpenAL.**/
	    private final ByteBuffer decodedDataBuffer;
	    /**Stereo or not.  Needed to tell OpenAL how to parse the bytes.**/
	    private final boolean isStereo;
	    /**Sample rate.  Required for correct playback speed.**/
	    private final int sampleRate;
	    
	    public OGGDecoder(URL soundURL){
	    	//Create a new decoder.
			decoder = new CodecJOrbis();
			decoder.initialize(soundURL);
			//Need to allocate double the buffer space.  Cause PaulsCode lies about their max size.
			//They stop the loop AFTER the size is exceeded!
			decodedDataBuffer = ByteBuffer.allocateDirect(SoundSystemConfig.getStreamingBufferSize()*2);
			
			//Start the decoder thread and wait for it to prime the decodedDataBuffer.
			decoderThread = new DecoderThread();
			decoderThread.start();
			synchronized(decoderThread){
	            try{
	                decoderThread.wait();
	            }catch(InterruptedException e){}
	        }
			
			//Get the first buffer and audio data information.
			this.isStereo = decoderOutputBuffers.peek().audioFormat.getChannels() == 2;
			this.sampleRate = (int) decoderOutputBuffers.peek().audioFormat.getSampleRate();
	    }
	
	    @Override
	    public ByteBuffer readBlock(){
	    	//See if we have a buffer in the stack.
			SoundBuffer decodedOutputBuffer = decoderOutputBuffers.poll();
			decodedDataBuffer.clear();
			if(decodedOutputBuffer != null){
				decodedDataBuffer.clear();
				return (ByteBuffer) decodedDataBuffer.put(decodedOutputBuffer.audioData).flip();
			}else{
				//No buffers.  Stream is likely slow.  Just return null here and try later.
				return null;
			}
	    }
	    
	    @Override
	    public void stop(){
	    	decoderThread.radioPlaying = false;
	    	//Decoder thread handles cleanup to prevent object states being invalid.
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
		 *  Helper class for parsing OGG streams.  PaulsCode locks up when parsing them.
		 *  Really don't want to use threads....
		 */
	    private class DecoderThread extends Thread{
	    	private boolean radioPlaying = true;
	    	
	    	@Override
	    	public void run(){
	    		//Run until the decoder is out of data, or until the radio stops.
	    		while(radioPlaying && !decoder.endOfStream()){
	    			//Try to always keep 5 buffers of data.
	    			//This allows for times where the machine gets loaded-down.
	        		while(decoderOutputBuffers.size() < 5){
	        			SoundBuffer buffer = decoder.read();
	        			if(buffer != null){
	        				decoderOutputBuffers.add(buffer);
	        			}else{
	        				//Buffer was null.  Break out of loop and go to sleep to prevent lock-ups.
	        				break;
	        			}
	        		}
	        		
	        		//Let the main thread know we're done running a loop check and go to sleep.
	        		//We'll wake up in 1 second to parse more data when it arrives.
	        		synchronized (decoderThread){
	        			decoderThread.notify();
	            		try{
	            			sleep(1000);
	        			}catch(InterruptedException e){}
	        		}
	    		}
	    		
	    		//Done playing.  Cleanup and close decoder.
	        	decoder.cleanup();
	    	}
	    }
	}
	
	private static final URLStreamHandler resourceStreamHandler = new ResourceStreamHandler();
	/**
	 *  StreamHandler for OGG files.  Used for interfacing with PaulsCode OGG codec.
	 */
	private static class ResourceStreamHandler extends URLStreamHandler{
		public ResourceStreamHandler(){}
		
		@Override
        protected URLConnection openConnection(URL connection){
            return new URLConnection(connection){
            	@Override
                public void connect() throws IOException{}
                
            	@Override
                public InputStream getInputStream() throws IOException{
                	String soundName = connection.getFile();
                	String soundDomain = soundName.substring(0, soundName.indexOf(':'));
                	soundName = soundName.substring(soundDomain.length() + 1);
                	return InterfaceOGGDecoder.class.getResourceAsStream("/assets/" +  soundDomain + "/sounds/" + soundName);
                }
            };
        }
    };
}