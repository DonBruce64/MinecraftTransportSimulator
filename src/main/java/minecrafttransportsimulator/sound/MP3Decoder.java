package minecrafttransportsimulator.sound;

import java.io.InputStream;
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Equalizer;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

/**Decoder class for MP3 files.  Reads data into ShortBuffers when called.  Also allows for
 * equalization operations, though they aren't studio-quality....
 *
 * @author don_bruce
 */
public class MP3Decoder{
	
	private final static int MAX_READ_SIZE = 96 * 1024;
    private final static int MP3_BUFFER_SIZE = 128 * 1024;

    /**Raw input stream for data.**/
    private final InputStream dataSourceStream;
    /**Bitstream for the internal parser.**/
    private final Bitstream bitstream;
    /**Decoder that decodes the Bitstream.**/
    private final Decoder decoder;
    /**Equalizer to use during the decoding process.**/
    private final Equalizer equalizer;
    /**SampleBuffer to store decoded data into prior to sending to a ShortBuffer.**/
    private final SampleBuffer decoderOutputBuffer;
    /**ShortBuffer used to store decoded data that can be sent to OpenAL.**/
    private final ShortBuffer decodedDataBuffer;
    /**Number of channels (usually 2 for Stereo).**/
    private final int channels;
    /**Sample rate.  Required for correct playback speed.**/
    private final int sampleRate;
    /**Current frame header for the next frame to be parsed.**/
    private Header currentFrameHeader;

    public MP3Decoder(InputStream dataSourceStream){
        this.dataSourceStream = dataSourceStream;
        this.bitstream = new Bitstream(dataSourceStream);
        this.decoder = new Decoder();
        this.equalizer = new Equalizer();
        
        //Get the first header for the frame to get the channel, sample rate, and setup the
        //outputbuffer for handling the decoded data.
        try{
			currentFrameHeader = bitstream.readFrame();
		}catch(Exception e){
			//Ain't gonna happen.
		}
		this.channels = (currentFrameHeader.mode() == Header.SINGLE_CHANNEL) ? 1 : 2;
		this.sampleRate = currentFrameHeader.frequency();
		this.decoderOutputBuffer = new SampleBuffer(sampleRate, channels);
		this.decodedDataBuffer = BufferUtils.createShortBuffer(MP3_BUFFER_SIZE);
		this.decoder.setOutputBuffer(decoderOutputBuffer);
    }

    /**
	 *  Reads a block of data and returns it as a ShortBuffer.
	 *  Note that this buffer is re-used, so do NOT make multiple
	 *  calls to this method without storing the data somewhere in
	 *  between them.  Once no more blocks are available this method
	 *  will return null.  Subsequent calls WILL cause Bad Stuff. 
	 */
    public ShortBuffer readBlock(){
        //Read a number of bytes from frames to fill the buffer.
    	//First reset the total bytes read and update the equalizer
    	//The setter doesn't actually set the equalizer and keep a reference,
    	//rather it sets the values for the decoder to those of the passed-in equalizer.
        int totalBytesRead = 0;
        decoder.setEqualizer(equalizer);
        decodedDataBuffer.rewind();
        while(totalBytesRead < MAX_READ_SIZE){
            try{
            	//We will already have a header at this point, so start parsing.
            	//Decode a frame.  Frame will be saved to the decodedDataBuffer.
                //We then close the frame right after as we're done with it.
                decoder.decodeFrame(currentFrameHeader, bitstream);
                bitstream.closeFrame();
                
                //Get number of bytes read, and append the data in the
                //decodedDataBuffer to the passed-in ShortBuffer.
                int bytesRead = decoderOutputBuffer.getBufferLength();
                decodedDataBuffer.put(decoderOutputBuffer.getBuffer(), 0, bytesRead);
                totalBytesRead += bytesRead;
            	
            	//Read the next frame header.
                //If it's null, we've reached the end of the file.
            	
                currentFrameHeader = bitstream.readFrame();
                if(currentFrameHeader == null){
                	bitstream.close();
                	dataSourceStream.close();
                	return null;
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        //Flip the buffer to set the position to the correct spot and return.
        decodedDataBuffer.flip();
        return decodedDataBuffer;
    }

    public int getChannels(){
        return channels;
    }

    public int getSampleRate(){
        return sampleRate;
    }

    public Equalizer getEqualizer(){
        return equalizer;
    }
}