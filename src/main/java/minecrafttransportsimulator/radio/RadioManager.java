package minecrafttransportsimulator.radio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.MTS;
import net.minecraft.client.Minecraft;

/**Manager class for all radio operations.  This class is responsible for
 * handling all requests from the radio GUI for file operations, and holds
 * references to all {@link RadioContainer} objects to allow them to be
 * queried by the GUI for status or for events to update their playing. 
 *
 * @author don_bruce
 */
public class RadioManager{
	private static boolean ready = false;
	private static File musicDir;
	private static File radioStationsFile;
	
	private static Map<RadioContainer, Radio> radios = new HashMap<RadioContainer, Radio>();
	
	/**Called to init the manager.  Should be called before any GUI operations are performed.**/
	public static void init(){
		if(!ready){
			musicDir = new File(MTS.minecraftDir.getAbsolutePath() + File.separator + "mts_music");
			musicDir.mkdir();
			radioStationsFile = new File(musicDir.getAbsolutePath() + File.separator + "radio_stations.txt");
			if(!radioStationsFile.exists()){
				try{
					radioStationsFile.createNewFile();
				}catch(IOException e){
					MTS.MTSLog.error("ERROR: UNABLE TO CREATE RADIO STATION SAVE FILE.  THINGS MAY GO BADLY!");
					MTS.MTSLog.error(e.getMessage());
					e.printStackTrace();
				}
			}
			ready = true;
		}
	}
	
	/**Called to update all radios.  Should be called at the end of the tick on the client-side.**/
	public static void updateRadios(){
		Iterator<Radio> radioIterator = radios.values().iterator();
		while(radioIterator.hasNext()){
			Radio radio = radioIterator.next();
			if(!radio.update(Minecraft.getMinecraft().isGamePaused())){
				radioIterator.remove();
			}
		}
	}
	
	/**Gets the directories from the mts_music folder.  Used to tell the radio which directory
	 * it will need to get music from.  Actual music files are gotten once the directory is selected.
	 **/
	public static List<String> getMusicDirectories(){
		List<String> musicDirectories = new ArrayList<String>();
		for(File file : musicDir.listFiles()){
			if(file.isDirectory()){
				musicDirectories.add(file.getName());
			}
		}
		Collections.sort(musicDirectories);
		//Pad out to 6 entries for the radio selection.
		while(musicDirectories.size() < 6){
			musicDirectories.add("");
		}
		return musicDirectories;
	}
	
	/**Gets the files from the specified directory in the mts_music folder. **/
	public static List<File> getMusicFiles(String directoryName, boolean sorted){
		List<File> musicFiles = new ArrayList<File>();
		for(File musicFile : new File(musicDir.getAbsolutePath() + File.separator + directoryName).listFiles()){
			if(!musicFile.isDirectory()){
				musicFiles.add(musicFile);
			}
		}
		if(sorted){
			Collections.sort(musicFiles);
		}else{
			Collections.shuffle(musicFiles);
		}
		return musicFiles;
	}
	
	/**Gets the list of radio stations present in the radio_stations.txt file in the mts_music directory.
	 * This should be done every time the GUI is opened to allow new stations to propagate.
	**/
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
			MTS.MTSLog.error("ERROR: UNABLE TO PARSE RADIO STATION FILE.");
			MTS.MTSLog.error(e.getMessage());
			e.printStackTrace();
		}
		//Don't sort the stations, as we want the order the user put them in.
		return stations;
	}
	
	/**Sets the radio station to the passed-in value and saves it to the radio_stations.txt file.*/
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
			MTS.MTSLog.error("ERROR: UNABLE TO SAVE RADIO STATION FILE.");
			MTS.MTSLog.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**Called to get the radio for a container.  If the radio is not found, one is created, cached, and returned.*/
	public static Radio getRadio(RadioContainer container){
		if(!radios.containsKey(container)){
			radios.put(container, new Radio(container));
		}
		return radios.get(container);
	}
}
