package minecrafttransportsimulator.sound;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javazoom.jl.decoder.Equalizer;
import mcinterface.InterfaceAudio;
import mcinterface.InterfaceOGGDecoder;
import minecrafttransportsimulator.MTS;

/**Base class for radios.  Used to provide a common set of tools for all radio implementations.
 * This class keeps track of the sound source (local files, Internet, server), as well as the
 * current volume for the source and equalization settings.
*
* @author don_bruce
*/
public class Radio{
	private static File musicDir;
	private static File radioStationsFile;
	
	//Public variables for modifying state.
	public byte presetIndex;
	public byte volume = 10;
	public boolean sorted;
	public byte queuedBuffers;
	public String displayText;
	public RadioSources source;
	public final Equalizer equalizer;
	
	//Private runtime variables.
	private final ISoundProvider provider;
	private final List<File> musicFiles = new ArrayList<File>();
	private SoundInstance currentSound;
	private volatile IStreamDecoder decoder;
	
	
	/**
	 * Need to set up global radio variables before we can create an instance of a radio.
	 */
	static{
		musicDir = new File(MTS.minecraftDir, "mts_music");
		musicDir.mkdir();
		radioStationsFile = new File(musicDir.getAbsolutePath() + File.separator + "radio_stations.txt");
		if(!radioStationsFile.exists()){
			try{
				radioStationsFile.createNewFile();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	
	public Radio(ISoundProvider provider){
		this.provider = provider;
		this.equalizer = new Equalizer();
		//Set initial source to local.
		changeSource(RadioSources.LOCAL);
	}
	
	/**
	 * Returns true if the radio is playing a song.
	 */
	public boolean playing(){
		return currentSound != null;
	}
	
	/**
	 * Stops all playback, closing any and all streams.
	 * If we aren't playing anything back, we just reset the preset.
	 * This happens when we reach the end of our list of playback files.
	 */
	public void stop(){
		if(currentSound != null){
			currentSound.stop();
			currentSound = null;
			musicFiles.clear();
			decoder.abort();
			displayText = "Radio turned off.";
		}
		presetIndex = -1;
	}
	
	/**
	 * Changes the radio's source.
	 */
	public void changeSource(RadioSources source){
		stop();
		this.source = source;
		switch(source){
			case LOCAL : displayText = "Ready to play from files on your PC.\nPress a station number to start."; break;
			case SERVER : displayText = "Ready to play from files on the server.\nPress a station number to start."; break;
			case INTERNET : displayText = "Ready to play from internet streams.\nPress a station number to start."; break;
		}
	}
	
	/**
	 * Changes the volume of this radio, and sets the currentSounds volume to that volume.
	 */
	public void changeVolume(byte volume){
		this.volume = volume;
		if(currentSound != null){
			currentSound.volume = volume/10F;
		}
	}
	
	/**
	 * Attempts to play music based on the current source and preset.
	 */
	public void pressPreset(byte index){
		//First stop the decoders and delete the sound references.
		stop();
		
		//Now set the preset and playback source.
		this.presetIndex = index;
		switch(source){
			case LOCAL : {
				if(parseLocalDirectory()){
					if(!queueNext()){
						return;
					}
				}
				break;
			}
			case SERVER : {
				displayText = "This method of playback is not supported .... yet!";
				return;
			}
			case INTERNET : {
				if(!playFromInternet()){
					return;
				}
				break;
			}
		}
	}
	
	/**
	 * Gets a ByteBuffer's worth of PCM samples from this radio.  Location of samples and
	 * size of the buffer is dependent on what is playing and from what source.  Note that
	 * the reference to the returned ByteBuffer may change during subsequent calls.  Copying/queuing
	 * is HIGHLY recommended. 
	 */
	public ByteBuffer getSampleBuffer(){
		ByteBuffer buffer = decoder.readBlock();
		return buffer != null ? (decoder.isStereo() ? InterfaceAudio.stereoToMono(buffer) : buffer) : null;
	}
	
	/**
	 * Gets the current sample rate of the source playing on this radio. 
	 */
	public int getSampleRate(){
		return decoder.getSampleRate();
	}
	
	/**
	 * Tells the radio to update the number of buffers it has displayed. 
	 */
	public void updateBufferCounts(byte bufferCount){
		if(bufferCount != queuedBuffers){
			queuedBuffers = bufferCount;
			displayText = displayText.substring(0, displayText.indexOf("Buffers:") + "Buffers:".length());
			for(byte i=0; i<bufferCount; ++i){
				displayText += "█";
			}
		}
	}
	
	/**
	 * Queues up songs from the preset directory and processes them for playing.
	 * Returns true if the directory was found, false otherwise.
	 */
	private boolean parseLocalDirectory(){
		musicFiles.clear();
		List<File> musicDirectories = new ArrayList<File>();
		for(File file : musicDir.listFiles()){
			if(file.isDirectory()){
				musicDirectories.add(file);
			}
		}
		Collections.sort(musicDirectories);
		
		//If we have the directory of the preset, load all the files in it.
		if(musicDirectories.size() > presetIndex){
			for(File musicFile : musicDirectories.get(presetIndex).listFiles()){
				if(!musicFile.isDirectory()){
					musicFiles.add(musicFile);
				}
			}
			if(sorted){
				Collections.sort(musicFiles);
			}else{
				Collections.shuffle(musicFiles);
			}
			return true;
		}else{
			displayText = "Fewer than " + (presetIndex + 1) + " folders in mts_music.\nGo add some!";
			return false;
		}
	}
	
	/**
	 * Queues the next source.  This is either the list of files to play on the local machine,
	 * ore the next song in the internet stream (cause some streams only send 1 song at a time).
	 * If there is nothing else to play, false is returned.
	 */
	public boolean queueNext(){
		if(source.equals(RadioSources.LOCAL)){
			//Get the next MP3 file for playback.
			//Use an iterator to keep non-MP3 files from blocking.
			Iterator<File> iterator = musicFiles.iterator();
			while(iterator.hasNext()){
				try{
					File musicFile = iterator.next();
					if(!musicFile.getName().toLowerCase().endsWith(".mp3")){
						iterator.remove();
					}else{
						decoder = new MP3Decoder(new FileInputStream(musicFiles.get(0)), equalizer);
						currentSound = new SoundInstance(provider, musicFiles.get(0).getParentFile().getName() + "\nNow Playing: " + musicFiles.get(0).getName(), false, this);
						currentSound.volume = volume/10F;
						InterfaceAudio.playStreamedSound(currentSound);
						displayText = "Station: " + currentSound.soundName;
						displayText += "\nBuffers:";
						queuedBuffers = 0;
						iterator.remove();
						return true;
					}
				}catch(Exception e){
					e.printStackTrace();
					iterator.remove();
				}
			}
			//No more files.  Perform exit logic for files.
			currentSound = null;
			displayText = "Finished playing.";
			stop();
			return false;
		}else if(source.equals(RadioSources.INTERNET)){
			//Internet stream is done.  This usually means it was done with sending the OGG file.
			//Try re-starting the stream to get new data.
			return playFromInternet();
		}
		return false;
	}
	
	/**
	 * Plays the station of the current index from the Internet.
	 * Returns true if the stream was able to be opened.
	 * Most of this is done via a thread to keep the main loop from blocking
	 * when parsing the initial buffer for playback.
	 */
	private boolean playFromInternet(){
		String station = getRadioStations().get(presetIndex);
		if(station.isEmpty()){
			displayText =  "Press SET to teach a station.";
		}else{
			try{
				//Create a URL and open a connection.
				final URL url = new URL(station);
				URLConnection connection = url.openConnection();
				
				//Verify stream is actually an HTTP stream.
				String contentType = connection.getHeaderField("Content-Type");
				if(contentType == null){
					displayText = "ERROR: No Content-Type header found.  Contact the mod author for more information.";
					return false;
				}
				
				//Check to make sure stream isn't an invalid type.
				switch(contentType){
					case("audio/mpeg") : break;
					case("application/ogg") : break;
					case("audio/x-wav") : displayText = "ERROR: WAV file format not supported...yet.  Contact the mod author."; return false;
					case("audio/flac") : displayText = "ERROR: Who the heck streams in FLAC?  Contact the mod author."; return false;
					default : {
						if(contentType.startsWith("audio")){
							displayText = "ERROR: Unsupported audio format of " + contentType + ".  Contact the mod author.";
						}else{
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
				queuedBuffers = 0;
				
				//Create a thread to start up the sound once the parsing is done.
				//This keeps us from blocking the main thread.
				final Radio thisRadio = this;
				Thread decoderInitThread = new Thread(){
					@Override
					public void run(){
						//Act based on our stream type.
						try{
							switch(contentType){
								case("audio/mpeg") : decoder = new MP3Decoder(url.openStream(), equalizer); break;
								case("application/ogg") : decoder = new InterfaceOGGDecoder(url); break;
							}
						}catch(Exception e){
							e.printStackTrace();
						}

						//Start this sound playing.
						currentSound = new SoundInstance(provider, station, false, thisRadio);
						currentSound.volume = volume/10F;
						InterfaceAudio.playStreamedSound(currentSound);
					}
				};
				decoderInitThread.start();
				return true;
			}catch(Exception e){
				e.printStackTrace();
				displayText = "ERROR: Unable to open URL.  Have you tried playing it in another application first?";
			}
		}
		return false;
	}
	
	/**
	 * Gets the list of radio stations present in the radio_stations.txt file in the mts_music directory.
	*/
	public static List<String> getRadioStations(){
		List<String> stations = new ArrayList<String>();
		try{
			BufferedReader radioStationFileReader = new BufferedReader(new FileReader(radioStationsFile));
			while(radioStationFileReader.ready()){
				stations.add(radioStationFileReader.readLine());
			}
			radioStationFileReader.close();
			//If we have no stations, make 6 blanks ones to avoid crashes.
			if(stations.size() == 0){
				for(byte i=0; i<6; ++i){
					stations.add("");
				}
			}
		}catch(IOException e){
			System.err.println("ERROR: Unable to parse radio_stations.txt file.  Is it in use?");
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		//Don't sort the stations, as we want the order the user put them in.
		return stations;
	}
	
	/**
	 * Sets the radio station to the passed-in value and saves it to the radio_stations.txt file.
	 */
	public static void setRadioStation(String station, int presetPressed){
		try{
			List<String> stations = getRadioStations();
			stations.set(presetPressed, station);
			BufferedWriter radioStationFileWriter = new BufferedWriter(new FileWriter(radioStationsFile));
			for(String stationToWrite : stations){
				radioStationFileWriter.write(stationToWrite + "\n");
			}
			radioStationFileWriter.close();
		}catch(IOException e){
			System.err.println("ERROR: Unable to save radio_stations.txt file.  Is it in use?");
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public enum RadioSources{
		LOCAL,
		SERVER,
		INTERNET;
	}
}