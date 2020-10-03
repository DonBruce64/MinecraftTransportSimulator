package minecrafttransportsimulator.sound;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.mcinterface.MasterLoader;

/**Class that manages all radios and stations.  Responsible for creating new stations and storing them,
 * as well as giving said stations to radios when they request them.  This class also interfaces with
 * the files on the local machine and keeps track of the order they are played in.
*
* @author don_bruce
*/
public class RadioManager{
	private static File musicDir;
	private static File radioStationsFile;
	private static Map<RadioSources, Map<Integer, RadioStation>> sourceMap = new HashMap<RadioSources, Map<Integer, RadioStation>>();
	
	/**
	 * Need to set up global radio variables before we can create an instance of a radio.
	 */
	static{
		musicDir = new File(MasterLoader.gameDirectory, "mts_music");
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
	
	/**
	 * Gets the station for the specific source and preset.
	 * Creates one it if it hasn't been created yet.
	 */
	public static RadioStation getStation(RadioSources source, int index){
		//No clue why we have to use Integer.valueOf, but whatever...
		Integer mapKey = Integer.valueOf(index);
		if(!sourceMap.containsKey(source)){
			sourceMap.put(source, new HashMap<Integer, RadioStation>());
		}
		if(!sourceMap.get(source).containsKey(mapKey)){
			sourceMap.get(source).put(mapKey, new RadioStation(source, index));
		}
		return sourceMap.get(source).get(mapKey);
	}
	
	/**
	 * Queues up songs from the preset directory for playing.
	 * Returns the files in the directory if they were found, or an empty list otherwise.
	 */
	public static List<File> parseLocalDirectory(int index){
		List<File> musicDirectories = new ArrayList<File>();
		List<File> musicFiles = new ArrayList<File>();
		for(File file : musicDir.listFiles()){
			if(file.isDirectory()){
				musicDirectories.add(file);
			}
		}
		Collections.sort(musicDirectories);
		
		//If we have the directory of the preset, load all the files in it.
		if(musicDirectories.size() > index){
			for(File musicFile : musicDirectories.get(index).listFiles()){
				if(!musicFile.isDirectory()){
					musicFiles.add(musicFile);
				}
			}
			Collections.sort(musicFiles);
		}
		return musicFiles;
	}
	
	/**
	 * Gets the radio URL for the specified index in the radio_stations.txt file in the mts_music directory.
	*/
	public static String getLocalStationURL(int index){
		try{
			List<String> stations = new ArrayList<String>();
			BufferedReader radioStationFileReader = new BufferedReader(new FileReader(radioStationsFile));
			while(radioStationFileReader.ready()){
				stations.add(radioStationFileReader.readLine());
			}
			radioStationFileReader.close();
			if(stations.size() > index){
				return stations.get(index);
			}else{
				return "";
			}
		}catch(IOException e){
			System.err.println("ERROR: Unable to parse radio_stations.txt file.  Is it in use?");
			System.err.println(e.getMessage());
			return "";
		}
	}
	
	/**
	 * Sets the radio URL to the passed-in value and saves it to the radio_stations.txt file.
	 */
	public static void setLocalStationURL(String stationURL, int index){
		try{
			List<String> stations = new ArrayList<String>();
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
			stations.set(index, stationURL);
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