package minecrafttransportsimulator.sound;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

public class OGGDecoder implements IStreamDecoder {
    private final static int OGG_BUFFER_SIZE = 4096;

    /**
     * Raw input stream for data.
     **/
    private final InputStream dataSourceStream;
    /**
     * Buffer used to store decoded data that can be sent to OpenAL.
     **/
    private final ByteBuffer decodedDataBuffer;
    /**
     * Internal flag set to true when we are done processing data.  Some data may still be left in buffers for return at this point.
     **/
    private boolean doneProcessing;

    /**
     * The current sync state.  Used to sync page read operations out of the bitstreamn
     * and must be notified for every stream read and every page request.  Does not handle packets,
     * that is the job of the stream.
     **/
    private final SyncState syncState = new SyncState();
    /**
     * The current streamn state.  Used for processing packets out of the synced stream.
     * Note that to use pages, they must first be passed from the syncState to the streamState.
     **/
    private final StreamState streamState = new StreamState();
    /**
     * The current page in use.  This is a header, and a bunch of packets together.
     **/
    private final Page page = new Page();
    /**
     * The current packet in use.  This is the raw audio data.
     **/
    private final Packet packet = new Packet();
    /**
     * The current comment.  This contains audiao sub-data like track name and whatnot.
     **/
    private final Comment comment = new Comment();
    /**
     * The current stream info.  This is for stuff like stero/mono, bitrate, etc.
     **/
    private final Info info = new Info();

    /**
     * State of the sound signal processor.  Used for decoding samples to PCM.
     **/
    private final DspState dspState = new DspState();
    /**
     * Data blocks send to DSP for processing.  Can be re-used.
     **/
    private final Block block = new Block(dspState);
    /**
     * The total samples processed for the current read operation.  Reset every call..
     **/
    private int totalSamplesProcessed;
    /**
     * True if we were processing samples and filled the buffer before we returned.
     **/
    private boolean bufferFilledLastDecodeCall;

    /**
     * A three-dimensional an array with PCM information.
     **/
    private final float[][][] pcmInfo;
    /**
     * The index for the PCM information.
     **/
    private final int[] pcmIndex;

    public OGGDecoder(InputStream dataSourceStream) {
        this.dataSourceStream = dataSourceStream;

        //Initialize objects and info and comment objects.
        syncState.init();
        info.init();
        comment.init();
        syncState.buffer(OGG_BUFFER_SIZE);

        //Read stream header to set up properties for the audio.
        //This is three packets, all of which go to the info and comment objects.
        int packetCount = 0;
        while (packetCount < 3) {
            //Loop as long as we have packets, and haven't processed 3.
            //The Vorbis header is in three packets; the initial small packet in
            //the first page that identifies basic parameters, a second packet
            //with bitstream comments and a third packet that holds the codebook.
            do {
                int pageStatus = syncState.pageout(page);
                if (pageStatus == 1) {
                    //Found page, process data.
                    //Init streamState to the page type if required on first page.
                    //For all pages, feed page into streamState for processing,
                    //then get packet out to be used by info and comment objects.
                    if (++packetCount == 1) {
                        streamState.init(page.serialno());
                    }
                    streamState.pagein(page);
                    streamState.packetout(packet);
                    info.synthesis_headerin(comment, packet);
                } else if (pageStatus == 0) {
                    //Need more data.
                    try {
                        int offset = syncState.buffer(OGG_BUFFER_SIZE);
                        int bytesRead = dataSourceStream.read(syncState.data, offset, OGG_BUFFER_SIZE);
                        syncState.wrote(bytesRead);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    throw new IllegalStateException("ERROR: Corrupt OGG file detected!");
                }
            } while (packetCount < 3);
        }

        //Init DSP and associated bits with parsed into.
        dspState.synthesis_init(info);
        pcmInfo = new float[1][][];
        pcmIndex = new int[info.channels];
        this.decodedDataBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(ByteOrder.nativeOrder());
    }

    @Override
    public ByteBuffer readBlock() {
        //If we have closed the stream from the last read, return null.
        if (doneProcessing) {
            return null;
        }

        totalSamplesProcessed = 0;
        int totalBytesRead = 0;
        decodedDataBuffer.clear();
        if (bufferFilledLastDecodeCall) {
            decodeSamples();
        }

        //Go until we read too much, or until we filled our buffer, or until we read everything.
        while (totalBytesRead < MAX_READ_SIZE && !bufferFilledLastDecodeCall && !doneProcessing) {
            //Get packets from data.  It appears a page can have multiple packets, but no example code checks for this?
            //Just flat-call for a new page.  If we don't have a packet, it means we need a new one anyways.
            //Though we do need to make sure we don't clobber the stream state with a null page.
            if (syncState.pageout(page) == 1) {
                streamState.pagein(page);
            }

            //Process packets in page.  Do so until we return 0, which means no more packets.
            //-1 in this case is a packet data hole, and we can skip those here without issue.
            while (!bufferFilledLastDecodeCall && streamState.packetout(packet) == 1) {
                if (block.synthesis(packet) == 0) {
                    dspState.synthesis_blockin(block);
                    decodeSamples();
                }
            }

            //We didn't fill our buffer up, see if we were either needing more data, or hit the end of stream.
            if (!bufferFilledLastDecodeCall) {
                try {
                    //Read some data.
                    int offset = syncState.buffer(OGG_BUFFER_SIZE);
                    int bytesRead = dataSourceStream.read(syncState.data, offset, OGG_BUFFER_SIZE);
                    if (bytesRead == -1) {
                        doneProcessing = true;
                    } else {
                        syncState.wrote(bytesRead);
                        totalBytesRead += bytesRead;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    doneProcessing = true;
                }
            }
        }

        //Rewind the decoded data buffer, set the limit based on the samples read, and return.
        decodedDataBuffer.rewind();
        decodedDataBuffer.limit(totalSamplesProcessed * 2 * info.channels);
        return info.channels == 2 ? IStreamDecoder.stereoToMono(decodedDataBuffer) : decodedDataBuffer;
    }

    private void decodeSamples() {
        //Create an ShortBuffer view to put short arrays into, and get the number of samples we can fit into it.
        ShortBuffer sampleBuffer = decodedDataBuffer.asShortBuffer();
        int samplesLeftInBuffer = BUFFER_SIZE / (2 * info.channels) - totalSamplesProcessed;
        int samplesAbleToProcess = dspState.synthesis_pcmout(pcmInfo, pcmIndex);
        if (samplesAbleToProcess > samplesLeftInBuffer) {
            samplesAbleToProcess = samplesLeftInBuffer;
            bufferFilledLastDecodeCall = true;
        } else {
            bufferFilledLastDecodeCall = false;
        }

        for (int i = 0; i < samplesAbleToProcess; i++) {
            for (int j = 0; j < info.channels; j++) {
                float[] channelSamples = pcmInfo[0][j];
                //Get value as an int from the array.
                //Not sure why this is a float array, but since it is we also need to do data checks.
                int value = (int) (channelSamples[pcmIndex[j] + i] * 32767);

                //Prevent bounds clipping before making a short value.
                //If we didn't do this and went right to a short, we would loose context and could end up with a wrong state.
                if (value > Short.MAX_VALUE)
                    value = Short.MAX_VALUE;
                if (value < Short.MIN_VALUE)
                    value = Short.MIN_VALUE;
                sampleBuffer.put((short) value);
            }
        }

        //Notify DSP that we processed these samples, increment process count, and exit.
        dspState.synthesis_read(samplesAbleToProcess);
        totalSamplesProcessed += samplesAbleToProcess;
        decodedDataBuffer.position(decodedDataBuffer.position() + sampleBuffer.position() * 2);
    }

    @Override
    public void stop() {
        try {
            dataSourceStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getSampleRate() {
        return info.rate;
    }
}