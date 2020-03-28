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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javazoom.jl.decoder.Equalizer;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.wrappers.WrapperAudio;
import minecrafttransportsimulator.wrappers.WrapperOGGDecoder;

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
	public byte presetIndex = -1;
	public byte volume = 10;
	public boolean sorted;
	public String displayText = "";
	public RadioSources source = RadioSources.LOCAL;
	public IStreamDecoder decoder;
	public final Equalizer equalizer;
	
	//Private runtime variables.
	private final ISoundProvider provider;
	private final List<File> musicFiles = new ArrayList<File>();
	private SoundInstance currentSound;
	
	
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
	}
	
	/**
	 * Returns true if the radio is playing a song.
	 */
	public boolean playing(){
		return currentSound != null;
	}
	
	/**
	 * Returns the curent radio source.
	 */
	public RadioSources getSource(){
		return source;
	}
	
	/**
	 * Stops all playback, closing any and all streams.
	 */
	public void stop(){
		System.out.println("RADIO STOP COMMAND RECEIVED");
		if(currentSound != null){
			System.out.println("RADIO RESETTING");
			currentSound.stop();
			currentSound = null;
			displayText = "";
		}
		presetIndex = -1;
	}
	
	/**
	 * Changes the radio's source.
	 */
	public void changeSource(RadioSources source){
		stop();
		this.source = source;
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
					if(!queueNextFile()){
						return;
					}
				}
			}
			case SERVER : {
				displayText = "This method of playback is not supported .... yet!";
				return;
			}
			case INTERNET : {
				if(!playFromInternet()){
					return;
				}
			}
		}
		
		//FIXME fire off radio packet here to have the radio update on the server and other clients.
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
	 * Queues the next file in the list of files to play on the local machine.
	 * If there are no more files, nothing is done, and false is returned.
	 */
	public boolean queueNextFile(){
		if(!musicFiles.isEmpty()){
			try{
				decoder = new MP3Decoder(new FileInputStream(musicFiles.get(0)), equalizer);
				SoundInstance oldSound = currentSound;
				currentSound = new SoundInstance(provider, musicFiles.get(0).getParentFile().getName() + "\nNow Playing: " + musicFiles.get(0).getName(), false, this);
				currentSound.volume = volume/10F;
				WrapperAudio.playStreamedSound(currentSound, oldSound);
				displayText = "Station: " + currentSound.soundName;
				return true;
			}catch(Exception e){
				e.printStackTrace();
			}
			musicFiles.remove(0);
		}else{
			System.out.println("END OF FILE LIST");
			stop();
		}
		return false;
	}
	
	/**
	 * Plays the station of the current index from the Internet.
	 * Returns true if the stream was able to be opened.
	 */
	private boolean playFromInternet(){
		String station = getRadioStations().get(presetIndex);
		if(station.isEmpty()){
			displayText =  "Press SET to teach a station.";
		}else{
			try{
				//Create a URL and open a connection.
				URL url = new URL(station);
				URLConnection connection = url.openConnection();
				
				//Check the headers content to see if we are a MP3 or OGG format.
				String contentType = connection.getHeaderField("Content-Type");
				if(contentType == null){
					displayText = "ERROR: No Content-Type header found.  Contact the mod author for more information.";
					return false;
				}
				
				//Act based on our stream type.
				switch(contentType){
					case("audio/mpeg") : decoder = new MP3Decoder(url.openStream(), equalizer); break;
					case("application/ogg") : decoder = new WrapperOGGDecoder(url); break;
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
				
				//We have a valid media type/decoder.  Get station info before we start parsing.
				displayText = "Name: " + (connection.getHeaderField("icy-name") != null ? connection.getHeaderField("icy-name") : "");
				displayText += "\nDesc: " + (connection.getHeaderField("icy-description") != null ? connection.getHeaderField("icy-description") : "");
				displayText += "\nGenre: " + (connection.getHeaderField("icy-genre") != null ? connection.getHeaderField("icy-genre") : "");
				
				//Done parsing data.  Create and start playing sound.
				currentSound = new SoundInstance(provider, station, false, this);
				currentSound.volume = volume/10F;
				WrapperAudio.playStreamedSound(currentSound, null);
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