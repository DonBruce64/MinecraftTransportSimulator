package minecrafttransportsimulator.sound;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    //Runtime variables.
    //Due to how the mp3 parser works, we can only have one equalizer per station.
    public String displayText = "";
    public String infoText = "";
    public final Equalizer equalizer;
    private final ConcurrentLinkedQueue<EntityRadio> queuedRadios = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<EntityRadio> playingRadios = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Integer> activeBuffers = new ConcurrentLinkedQueue<>();
    private volatile LinkingThread linkingThread;
    private volatile DecoderThread decoderThread;
    private volatile int faultedDecodes;

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
        //If we are an internet stream, and we killed the last radio, abort us.
        //This is because internet streams are constant feeds and can't be cached.
        if (playingRadios.isEmpty() && queuedRadios.isEmpty() && source != RadioSources.LOCAL && decoderThread != null) {
            decoderThread.kill();
            decoderThread = null;
        }
    }

    /**
     * Updates the station.  Responsible for managing buffers, encoder calls,
     * starting new queued radios, and the like.  This will be called from
     * the audio thread, so watch out for CMEs!
     */
    public void update() {
        if (!playingRadios.isEmpty() || !queuedRadios.isEmpty()) {
            if (linkingThread == null && decoderThread == null) {
                //Need to start trying to do playback since we don't have any threads.
                if (faultedDecodes < 5) {
                    startPlayback();
                }
            } else if (decoderThread != null && decoderThread.initDone) {
                //Have an active and ready decoder, start decoding.
                int freeBufferIndex = 0;

                //If we have any playing radios, do buffer logic.
                if (!playingRadios.isEmpty()) {
                    //First check if we have any buffers that are done playing that we can re-claim.
                    freeBufferIndex = InterfaceManager.soundInterface.getFreeStationBuffer(playingRadios);
                    if (freeBufferIndex != 0) {
                        activeBuffers.remove(freeBufferIndex);
                        InterfaceManager.soundInterface.deleteBuffer(freeBufferIndex);
                    }
                }

                //If we removed a buffer, or if we don't have any playing radios, start our radios.
                //This syncs new radios if we are playing one, and starts new radios if we aren't.
                if ((freeBufferIndex != 0 || playingRadios.isEmpty()) && !queuedRadios.isEmpty()) {
                    Iterator<EntityRadio> iterator = queuedRadios.iterator();
                    while (iterator.hasNext()) {
                        EntityRadio radio = iterator.next();
                        //Only start radios in range.
                        if (radio.position.isDistanceToCloserThan(InterfaceManager.clientInterface.getClientPlayer().getPosition(), SoundInstance.DEFAULT_MAX_DISTANCE)) {
                            radio.start();
                            InterfaceManager.soundInterface.addRadioSound(radio.getPlayingSound(), activeBuffers);
                            playingRadios.add(radio);
                            iterator.remove();
                        }
                    }
                }

                //Check if the radio is out of player audio range and stop playing it here if so.
                Iterator<EntityRadio> iterator = playingRadios.iterator();
                while (iterator.hasNext()) {
                    EntityRadio radio = iterator.next();
                    //If the radio isn't in rage, stop playing it.
                    //Just kill the sound, since the stop command is for the stop button and it won't restart.
                    if (!radio.position.isDistanceToCloserThan(InterfaceManager.clientInterface.getClientPlayer().getPosition(), SoundInstance.DEFAULT_MAX_DISTANCE)) {
                        radio.getPlayingSound().stopSound = true;
                        queuedRadios.add(radio);
                        iterator.remove();
                    }
                }

                //If we have 0 buffers, clear out the decoder and start the station again.
                //This happens if we reach an EOF, or the stream cuts out.
                if (activeBuffers.isEmpty()) {
                    if (decoderThread != null) {
                        decoderThread.kill();
                    }
                    startPlayback();
                }
            }
        }
    }

    /**
     * Starts playback of this station.  This is called when we first add a radio,
     * or when the radio stops playing and we auto-restart.  This creates a new decoder for 
     * parsing data and populates the buffers via a thread.  Radios will be started in the update
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
                playFromInternet();
            }
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
                if (!musicFile.getName().toLowerCase(Locale.ROOT).endsWith(".mp3")) {
                    iterator.remove();
                } else {
                    infoText = "Station: " + musicFiles.get(0).getParentFile().getName() + "\nNow Playing: " + musicFiles.get(0).getName();
                    infoText += "\nBuffers:";
                    decoderThread = new DecoderThread(this, musicFiles.get(0));
                    decoderThread.start();
                    iterator.remove();
                    return;
                }
            } catch (Exception e) {
                //e.printStackTrace();
                iterator.remove();
            }
        }
    }

    /**
     * Starts playing the Internet stream for this station.
     */
    private void playFromInternet() {
        displayText = "CONNECTING";
        decoderThread = null;
        linkingThread = new LinkingThread(this);
        linkingThread.start();
    }
    
    /**
     * Custom thread class to prevent blocking of the main thread when querying radio stations for audio.
     * This thread finds the audio source and kills itself when it does, or when the source can't be found.
     *
     * @author don_bruce
     */
    private static class LinkingThread extends Thread {
        private final RadioStation station;

        private LinkingThread(RadioStation station) {
            this.station = station;
        }

        @Override
        public void run() {
            if (!initDecoderThread()) {
                //Something is wrong with the radio station, abort all radio playback.
                station.queuedRadios.clear();
            }
            station.linkingThread = null;
        }

        private boolean initDecoderThread() {
            //Try to open the radio URL.
            int tryCount = 0;
            String errorString = null;
            do {
                try {
                    //Create a URL and open a connection.
                    URL urlObj = new URL(station.url);
                    URLConnection connection = urlObj.openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                    //Verify stream is actually an HTTP stream.
                    connection.connect();
                    station.displayText = "CONNECTING - TRY #" + tryCount;
                    String contentType = connection.getContentType();
                    if (contentType == null) {
                        errorString = "ERROR: No content-type header found.  Contact the mod author for more information.";
                    } else {
                        //Check to make sure stream isn't an invalid type.
                        switch (contentType) {
                            case ("audio/mpeg"):
                            case ("application/ogg"):
                                break;
                            case ("audio/x-wav"): {
                                station.displayText = "ERROR: WAV file format not supported...yet.  Contact the mod author.";
                                return false;
                            }
                            case ("audio/flac"): {
                                station.displayText = "ERROR: Who the heck streams in FLAC?  Contact the mod author.";
                                return false;
                            }
                            default: {
                                if (contentType.startsWith("audio")) {
                                    station.displayText = "ERROR: Unsupported audio format of " + contentType + ".  Contact the mod author.";
                                    return false;
                                } else {
                                    errorString = "ERROR: Format " + contentType + " is NOT an audio format.  Is this really a music URL?";
                                    continue; //Could be a bad packet with text or something.
                                }
                            }
                        }

                        //Parse out information from header.
                        station.infoText = "Name: " + (connection.getHeaderField("icy-name") != null ? connection.getHeaderField("icy-name") : "");
                        station.infoText += "\nDesc: " + (connection.getHeaderField("icy-description") != null ? connection.getHeaderField("icy-description") : "");
                        station.infoText += "\nGenre: " + (connection.getHeaderField("icy-genre") != null ? connection.getHeaderField("icy-genre") : "");
                        station.infoText += "\nBuffers:";

                        //Create a thread to start up the sound once the parsing is done.
                        //This keeps us from blocking the main thread.
                        station.decoderThread = new DecoderThread(station, contentType, connection);
                        station.decoderThread.start();
                        return true;
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                    station.displayText = "ERROR: Unable to open URL.  Have you tried playing it in another application first?";
                    return false;
                }
            } while (++tryCount < 5 && errorString != null);

            //We must have failed too many times, so set value to last text.
            station.displayText = errorString;
            return false;
        }
    }

    /**
     * Custom thread class to prevent blocking of the main thread when playing audio.
     * This thread parses out the audio from the source, and keeps the decoder inside of it.
     *
     * @author don_bruce
     */
    private static class DecoderThread extends Thread {
        private final RadioStation station;
        private final String contentType;
        private final URLConnection contentConnection;
        private final File contentFile;
        private IStreamDecoder decoder;
        private boolean stopDecoding;
        public boolean initDone;

        public DecoderThread(RadioStation station, String contentType, URLConnection contentConnection) {
            this.station = station;
            this.contentType = contentType;
            this.contentConnection = contentConnection;
            this.contentFile = null;
        }

        public DecoderThread(RadioStation station, File contentFile) {
            this.station = station;
            this.contentType = null;
            this.contentConnection = null;
            this.contentFile = contentFile;
        }

        @Override
        public void run() {
            //Act based on our stream type.
            int tryCount = 0;
            do {
                try {
                    station.displayText = "BUFFERING - TRY #" + tryCount;
                    if (contentConnection != null) {
                        switch (contentType) {
                            case ("audio/mpeg"):
                                decoder = new MP3Decoder(contentConnection.getInputStream(), station.equalizer);
                                break;
                            case ("application/ogg"):
                                decoder = new OGGDecoder(contentConnection.getInputStream());
                                break;
                        }
                    } else {
                        decoder = new MP3Decoder(Files.newInputStream(contentFile.toPath()), station.equalizer);
                    }
                    //Prime the buffers before setting the thread to null.
                    //This prevents the buffers from running out from starting too quickly.
                    //Because this is in a thread, it also saves on processing power.
                    for (byte i = 0; i < 5; ++i) {
                        generateBufferIndex(false);
                    }
                    initDone = true;

                    //Done starting decoding, update text and go into main parsing loop.
                    station.displayText = station.infoText;
                    while (!stopDecoding) {
                        if (station.activeBuffers.size() < 5) {
                            int newIndex = generateBufferIndex(true);
                            if (newIndex != 0) {
                                station.playingRadios.forEach(radio -> InterfaceManager.soundInterface.bindBuffer(radio.getPlayingSound(), newIndex));
                            }
                        }
                    }
                    return;
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            } while (++tryCount < 5);
            station.displayText = "ERROR: Was able to connect to URL but not open stream.  Try again later?";
            //Something is wrong with the radio station, abort all radio playback.
            station.queuedRadios.clear();
        }

        public void kill() {
            stopDecoding = true;
            if (decoder != null) {
                decoder.stop();
            }
        }

        /**
         * Generates a new buffer for this station from the current decoder and
         * stores it in the list of active buffers.  Also updates the displayText
         * to reflect the buffer count.  Returns the index of the newly-created
         * buffer, or 0 if the buffer wasn't able to be created.
         */
        private int generateBufferIndex(boolean updateDisplay) {
            ByteBuffer buffer = decoder.readBlock();
            if (buffer != null) {
                //Get new buffer index from the audio system and add it to our radios.
                int bufferIndex = InterfaceManager.soundInterface.createBuffer(buffer, decoder);
                station.activeBuffers.add(bufferIndex);
                if (updateDisplay) {
                    //Update station buffer counts and return buffer index.
                    int bufferTextIndex = station.displayText.indexOf("Buffers:");
                    if (bufferTextIndex != -1) {
                        station.displayText = station.displayText.substring(0, bufferTextIndex + "Buffers:".length());
                        for (byte i = 0; i < station.activeBuffers.size(); ++i) {
                            station.displayText += "X";
                        }
                    } else {
                        station.displayText = "DISPLAY MALFUNCTION!\nTURN RADIO OFF AND ON TO RESET!";
                    }
                }
                return bufferIndex;
            }
            return 0;
        }
    }
}