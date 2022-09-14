package minecrafttransportsimulator.sound;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Equalizer;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

/**
 * Decoder class for MP3 files.  Reads data into ShortBuffers when called.  Also allows for
 * equalization operations, though they aren't studio-quality....
 *
 * @author don_bruce
 */
public class MP3Decoder implements IStreamDecoder {
    /**
     * Raw input stream for data.
     **/
    private final InputStream dataSourceStream;
    /**
     * Bitstream for the internal parser.
     **/
    private final Bitstream bitstream;
    /**
     * Decoder that decodes the Bitstream.
     **/
    private final Decoder decoder;
    /**
     * Equalizer to use during the decoding process.
     **/
    private final Equalizer equalizer;
    /**
     * SampleBuffer to store decoded data into prior to sending to the decodedDataBuffer.
     **/
    private final SampleBuffer decoderOutputBuffer;
    /**
     * Buffer used to store decoded data that can be sent to OpenAL.
     **/
    private final ByteBuffer decodedDataBuffer;
    /**
     * Stereo or not.  Needed to tell OpenAL how to parse the bytes.
     **/
    private final boolean isStereo;
    /**
     * Sample rate.  Required for correct playback speed.
     **/
    private final int sampleRate;
    /**
     * Current frame header for the next frame to be parsed.
     **/
    private Header currentFrameHeader;

    public MP3Decoder(InputStream dataSourceStream, Equalizer equalizer) {
        this.dataSourceStream = dataSourceStream;
        this.bitstream = new Bitstream(dataSourceStream);
        this.decoder = new Decoder();
        this.equalizer = equalizer;

        //Get the first header for the frame to get the channel, sample rate, and setup the
        //outputbuffer for handling the decoded data.
        try {
            currentFrameHeader = bitstream.readFrame();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.isStereo = currentFrameHeader.mode() != Header.SINGLE_CHANNEL;
        this.sampleRate = currentFrameHeader.frequency();
        this.decoderOutputBuffer = new SampleBuffer(sampleRate, isStereo ? 2 : 1);
        this.decodedDataBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(ByteOrder.nativeOrder());
        this.decoder.setOutputBuffer(decoderOutputBuffer);
    }

    @Override
    public ByteBuffer readBlock() {
        //If we have closed the stream from the last read, return null.
        if (currentFrameHeader == null) {
            return null;
        }

        //Read a number of bytes from frames to fill the buffer.
        //First reset the total bytes read and update the equalizer
        //The setter doesn't actually set the equalizer and keep a reference,
        //rather it sets the values for the decoder to those of the passed-in equalizer.
        int totalSamplesRead = 0;
        decoder.setEqualizer(equalizer);
        decodedDataBuffer.clear();

        //Create a ShortBuffer view to put short arrays into.
        ShortBuffer sampleBuffer = decodedDataBuffer.asShortBuffer();
        while (totalSamplesRead < MAX_READ_SIZE) {
            try {
                //We will already have a header at this point, so start parsing.
                //Decode a frame.  Frame will be saved to the decodedDataBuffer.
                //We then close the frame right after as we're done with it.
                decoder.decodeFrame(currentFrameHeader, bitstream);
                bitstream.closeFrame();

                //Get number of bytes read, and append the data in the
                //decodedDataBuffer to the ByteBuffer.  Note that the length
                //of the outputBuffer is in shorts, so need to multiply that by 2 for bytes.
                int samplesRead = decoderOutputBuffer.getBufferLength();
                sampleBuffer.put(decoderOutputBuffer.getBuffer(), 0, samplesRead);
                totalSamplesRead += samplesRead;

                //Read the next frame header.
                //If it's null, we've reached the end of the stream and should just return what we have.
                currentFrameHeader = bitstream.readFrame();
                if (currentFrameHeader == null) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Rewind the decoded data buffer, set the limit based on the samples read, and return.
        decodedDataBuffer.rewind();
        decodedDataBuffer.limit(totalSamplesRead * 2);
        return isStereo ? IStreamDecoder.stereoToMono(decodedDataBuffer) : decodedDataBuffer;
    }

    @Override
    public void stop() {
        try {
            bitstream.close();
            dataSourceStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }
}