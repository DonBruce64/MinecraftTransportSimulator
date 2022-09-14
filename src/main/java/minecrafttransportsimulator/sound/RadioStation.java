package minecrafttransportsimulator.sound;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javazoom.jl.decoder.Equalizer;
import minecrafttransportsimulator.entities.instances.EntityRadio;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.sound.RadioManager.RadioSources;

/**
 * Radio stations are sources that radios can hook into to provide sound.  All radios share the
 * same common set of stations, which means that should two radios start playing the same station, they
 * will both play in-sync with one another.
 *
 * @author don_bruce
 */
public class RadioStation {
    //Created variables.
    private final RadioSources source;
    private final int index;
    private final boolean randomOrder;
    private final String url;
    private final List<File> musicFiles;
    private final Set<EntityRadio> queuedRadios = new HashSet<>();
    private final Set<EntityRadio> playingRadios = new HashSet<>();

    //Runtime variables.
    //Due to how the mp3 parser works, we can only have one equalizer per station.
    public String displayText = "";
    public final Equalizer equalizer;
    private final List<Integer> activeBuffers = new ArrayList<>();
    private volatile IStreamDecoder decoder;
    private volatile DecoderThread decoderThread;

    public RadioStation(int index, boolean randomOrder) {
        this.source = RadioSources.LOCAL;
        this.equalizer = new Equalizer();
        this.index = index;
        this.randomOrder = randomOrder;
        this.url = null;
        musicFiles = RadioManager.parseLocalDirectory(index, randomOrder);
        if (musicFiles.isEmpty()) {
            displayText = "Fewer than " + (index + 1) + " folders in mts_music.\nGo add some!";
        }
        InterfaceManager.soundInterface.addRadioStation(this);
    }

    public RadioStation(String url) {
        this.source = RadioSources.INTERNET;
        this.equalizer = new Equalizer();
        this.index = 0;
        this.randomOrder = false;
        this.url = url;
        if (url.isEmpty()) {
            displayText = "No station set for this preset.  Press SET to teach a station.";
        }
        musicFiles = new ArrayList<>();
        InterfaceManager.soundInterface.addRadioStation(this);
    }

    /**
     * Generates a new buffer for this station from the current decoder and
     * stores it in the list of active buffers.  Also updates the displayText
     * to reflect the buffer count.  Returns the index of the newly-created
     * buffer, or 0 if the buffer wasn't able to be created.
     */
    private int generateBufferIndex() {
        ByteBuffer buffer = decoder.readBlock();
        if (buffer != null) {
            //Get new buffer index from the audio system and add it to our radios.
            int bufferIndex = InterfaceManager.soundInterface.createBuffer(buffer, decoder);
            activeBuffers.add(bufferIndex);

            //Update station buffer counts and return buffer index.
            displayText = displayText.substring(0, displayText.indexOf("Buffers:") + "Buffers:".length());
            for (byte i = 0; i < activeBuffers.size(); ++i) {
                displayText += "X";
            }

            return bufferIndex;
        }
        return 0;
    }

    /**
     * Adds a radio to this station for playback.  If the station isn't playing to any radios, then
     * the station is started and the radio will start playing as soon as its ready.  If the station
     * is playing, then the radio is queued to start on the next buffer call.  This allows for syncing
     * of radios in the world.
     */
    public void addRadio(EntityRadio radio) {
        queuedRadios.add(radio);
    }

    /**
     * Removes a radio to this station for playback.
     */
    public void removeRadio(EntityRadio radio) {
        playingRadios.remove(radio);
        queuedRadios.remove(radio);
    }

    /**
     * Updates the station.  Responsible for managing buffers, encoder calls,
     * starting new queued radios, and the like.  This will be called from
     * the audio thread, so watch out for CMEs!
     */
    public void update() {
        if (!playingRadios.isEmpty() || !queuedRadios.isEmpty()) {
            if (decoderThread == null && decoder == null) {
                //Need to start the first decoder thread.
                startPlayback();
            } else if (decoderThread == null) {
                int freeBufferIndex = 0;

                //If we have any playing radios, do buffer logic.
                if (!playingRadios.isEmpty()) {
                    //First check if we have any buffers that are done playing that we can re-claim.
                    freeBufferIndex = InterfaceManager.soundInterface.getFreeStationBuffer(playingRadios);
                    if (freeBufferIndex != 0) {
                        activeBuffers.remove((Integer) freeBufferIndex);
                        InterfaceManager.soundInterface.deleteBuffer(freeBufferIndex);
                    }
                }

                //If we removed a buffer, or if we don't have any playing radios, start our radios.
                //This syncs new radios if we are playing one, and starts new radios if we aren't.
                if ((freeBufferIndex != 0 || playingRadios.isEmpty()) && !queuedRadios.isEmpty()) {
                    for (EntityRadio radio : queuedRadios) {
                        radio.start();
                        InterfaceManager.soundInterface.addRadioSound(radio.getPlayingSound(), activeBuffers);
                        playingRadios.add(radio);
                    }
                    queuedRadios.clear();
                }

                //If we have less than 5 buffers, try to get another one.
                if (activeBuffers.size() < 5) {
                    int newIndex = generateBufferIndex();
                    if (newIndex != 0) {
                        for (EntityRadio radio : playingRadios) {
                            InterfaceManager.soundInterface.bindBuffer(radio.getPlayingSound(), newIndex);
                        }
                    }
                }

                //If we have 0 buffers, clear out the decoder and start the station again.
                //This happens if we reach an EOF, or the stream cuts out.
                if (activeBuffers.isEmpty()) {
                    startPlayback();
                }
            }
        } else {
            //If we are an internet stream, and we aren't hooked to anything, abort us.
            //This is because internet streams are constant feeds and can't be cached.
            if (!source.equals(RadioSources.LOCAL) && decoder != null) {
                decoder.stop();
                decoder = null;
            }
        }
    }

    /**
     * Starts playback of this station.  This is called when we first add a radio,
     * or when the radio stops playing.  This creates a new encoder for parsing data
     * and populates the buffers via a thread.  Radios will be started in the update
     * method when the buffer is full.
     */
    private void startPlayback() {
        //Delete any buffers we might still have.
        for (int buffer : activeBuffers) {
            InterfaceManager.soundInterface.deleteBuffer(buffer);
        }
        activeBuffers.clear();

        //Move any playing radios back into the queue.
        queuedRadios.addAll(playingRadios);
        playingRadios.clear();

        //Start decoder creation routines.
        if (source.equals(RadioSources.LOCAL)) {
            if (musicFiles.isEmpty()) {
                //Try to parse files again in case the user added some.
                musicFiles.addAll(RadioManager.parseLocalDirectory(index, randomOrder));
            }
            playFromLocalFiles();
        } else {
            if (!url.isEmpty()) {
                if (playFromInternet()) {
                    return;
                }
            }
            queuedRadios.clear();
        }
    }

    /**
     * Starts playing the local files on the local machine.
     */
    private void playFromLocalFiles() {
        //Get the next MP3 file for playback.
        //Use an iterator to keep non-MP3 files from blocking.
        Iterator<File> iterator = musicFiles.iterator();
        while (iterator.hasNext()) {
            try {
                File musicFile = iterator.next();
                if (!musicFile.getName().toLowerCase().endsWith(".mp3")) {
                    iterator.remove();
                } else {
                    displayText = "Station: " + musicFiles.get(0).getParentFile().getName() + "\nNow Playing: " + musicFiles.get(0).getName();
                    displayText += "\nBuffers:";
                    decoder = null;
                    decoderThread = new DecoderThread(this, musicFiles.get(0));
                    decoderThread.start();
                    iterator.remove();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                iterator.remove();
            }
        }
    }

    /**
     * Starts playing the Internet stream for this station.  Returns true if the stream started, false if there
     * was an error.
     */
    private boolean playFromInternet() {
        try {
            //Create a URL and open a connection.
            URL urlObj = new URL(url);
            URLConnection connection = urlObj.openConnection();

            //Verify stream is actually an HTTP stream.
            String contentType = connection.getHeaderField("Content-Type");
            if (contentType == null) {
                displayText = "ERROR: No Content-Type header found.  Contact the mod author for more information.";
                return false;
            }

            //Check to make sure stream isn't an invalid type.
            switch (contentType) {
                case ("audio/mpeg"):
                case ("application/ogg"):
                    break;
                case ("audio/x-wav"):
                    displayText = "ERROR: WAV file format not supported...yet.  Contact the mod author.";
                    return false;
                case ("audio/flac"):
                    displayText = "ERROR: Who the heck streams in FLAC?  Contact the mod author.";
                    return false;
                default: {
                    if (contentType.startsWith("audio")) {
                        displayText = "ERROR: Unsupported audio format of " + contentType + ".  Contact the mod author.";
                    } else {
                        displayText = "ERROR: Format " + contentType + " is NOT an audio format.  Is this really a music URL?";
                    }
                    return false;
                }
            }

            //Parse out information from header.
            displayText = "Name: " + (connection.getHeaderField("icy-name") != null ? connection.getHeaderField("icy-name") : "");
            displayText += "\nDesc: " + (connection.getHeaderField("icy-description") != null ? connection.getHeaderField("icy-description") : "");
            displayText += "\nGenre: " + (connection.getHeaderField("icy-genre") != null ? connection.getHeaderField("icy-genre") : "");
            displayText += "\nBuffers:";

            //Create a thread to start up the sound once the parsing is done.
            //This keeps us from blocking the main thread.
            decoder = null;
            decoderThread = new DecoderThread(this, contentType, urlObj);
            decoderThread.start();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            displayText = "ERROR: Unable to open URL.  Have you tried playing it in another application first?";
            return false;
        }
    }

    /**
     * Custom thread class to prevent blocking of the main thread when playing audio.
     * This thread parses out the audio from the source, and keeps the decoder inside of it.
     *
     * @author don_bruce
     */
    public static class DecoderThread extends Thread {
        private final RadioStation station;
        private final String contentType;
        private final URL contentURL;
        private final File contentFile;

        public DecoderThread(RadioStation station, String contentType, URL contentURL) {
            this.station = station;
            this.contentType = contentType;
            this.contentURL = contentURL;
            this.contentFile = null;
        }

        public DecoderThread(RadioStation station, File contentFile) {
            this.station = station;
            this.contentType = null;
            this.contentURL = null;
            this.contentFile = contentFile;
        }

        @Override
        public void run() {
            //Act based on our stream type.
            try {
                if (contentURL != null) {
                    switch (contentType) {
                        case ("audio/mpeg"):
                            station.decoder = new MP3Decoder(contentURL.openStream(), station.equalizer);
                            break;
                        case ("application/ogg"):
                            station.decoder = new OGGDecoder(contentURL.openStream());
                            break;
                    }
                } else {
                    station.decoder = new MP3Decoder(Files.newInputStream(contentFile.toPath()), station.equalizer);
                }
                //Prime the buffers before setting the thread to null.
                //This prevents the buffers from running out from starting too quickly.
                //Because this is in a thread, it also saves on processing power.
                for (byte i = 0; i < 5; ++i) {
                    station.generateBufferIndex();
                }
                station.decoderThread = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}