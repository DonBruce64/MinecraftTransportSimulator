package minecrafttransportsimulator.systems;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/**This class handles all radio operations.  It is not activated until the
 * user opens the radio panel on a vehicle, so if there's a problem, we won't
 * know until that happens!  The only thing that should be called every tick
 * is the update() method.  This will set the volume to the correct value
 * for the listening player, but won't touch any radio-specific bits if the
 * radio isn't running.  Be aware that the BasicPlayer class is old as
 * dirt, so it's quite possible things will break on other systems... 
 *
 * @author don_bruce
 */
public class RadioSystem{
	private static boolean ready = false;
	private static BasicPlayer player;
	private static File musicDir;
	private static File radioStationsFile;
	private static List<File> musicFilesToPlay = new ArrayList<File>();
	private static EntityVehicleE_Powered vehicleWithRadio;
	
	/**Called to init the system.  Should be called from the GUI when opened.**/
	public static void init(){
		if(!ready){
			player = new BasicPlayer();
			musicDir = new File(MTS.minecraftDir.getAbsolutePath() + File.separator + "mts_music");
			musicDir.mkdir();
			radioStationsFile = new File(musicDir.getAbsolutePath() + File.separator + "radio_stations.txt");
			if(!radioStationsFile.exists()){
				try{
					radioStationsFile.createNewFile();
				}catch(IOException e){
					MTS.MTSLog.error("ERROR: UNABLE TO CREATE RADIO STATION SAVE FILE.  THINGS MAY GO BADLY!");
					MTS.MTSLog.error(e.getMessage());
				}
			}
			ready = true;
		}
	}
	
	/**Called every tick to update the volume and play/pause status**/
	public static void update(){
		try{
			if(ready){
				if(player.getStatus() == BasicPlayer.PLAYING){
					//If we are playing, and the game is paused, pause the radio.
					//Otherwise, set the volume to the player distance.
					if(Minecraft.getMinecraft().isGamePaused()){
						player.pause();
					}else{
						if(vehicleWithRadio.equals(Minecraft.getMinecraft().player.getRidingEntity())){
							setVolume(1.0F);
						}else{
							setVolume(1/Minecraft.getMinecraft().player.getDistance(vehicleWithRadio));
						}
					}
				}else if(player.getStatus() == BasicPlayer.PAUSED){
					//If we are paused, but the game isn't, un-pause us.
					if(!Minecraft.getMinecraft().isGamePaused()){
						player.resume();
					}
				}else if(player.getStatus() == BasicPlayer.STOPPED || player.getStatus() == BasicPlayer.UNKNOWN){
					//If we are stopped, and we are have music files to play, go to the next song.
					if(!musicFilesToPlay.isEmpty()){
						player.open(musicFilesToPlay.get(0));
						player.play();
						musicFilesToPlay.remove(0);
					}
				}
			}
		}catch(Exception e){
			MTS.MTSLog.error("ERROR: BASICPLAYER INTERNAL UPDATED CODE HAS FAULTED.");
			MTS.MTSLog.error(e.getMessage());
			if(!musicFilesToPlay.isEmpty()){
				musicFilesToPlay.clear();
			}
		}
	}
	
	/**Plays a sound file from the Java classpath.  May be MC sounds or music disks.**/
	public static boolean play(ResourceLocation location, EntityVehicleE_Powered vehicle){
		try{
			player.open(Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream());
			player.play();
			vehicleWithRadio = vehicle;
			return true;
		}catch(IOException e){
			//We know why this happens.  No need to save the stacktrace.
			return false;
		}catch(BasicPlayerException e){
			MTS.MTSLog.error("ERROR: BASICPLAYER INTERNAL PLAY CODE HAS FAULTED.");
			MTS.MTSLog.error(e.getMessage());
			return false;
		}
		
	}
	
	/**Sets the player to play sound files from a directory in the mts_music folder.
	 * Actual play code is done during the update loop.**/
	public static void play(String directoryName, EntityVehicleE_Powered vehicle, boolean sorted){
		musicFilesToPlay.clear();
		for(File musicFile : new File(musicDir.getAbsolutePath() + File.separator + directoryName).listFiles()){
			if(musicFile.isFile()){
				musicFilesToPlay.add(musicFile);
			}
		}
		if(sorted){
			Collections.sort(musicFilesToPlay);
		}else{
			Collections.shuffle(musicFilesToPlay);
		}
		vehicleWithRadio = vehicle;
	}
	
	/**Plays a sound file or stream from the web.**/
	public static boolean play(URL url, EntityVehicleE_Powered vehicle){
		//Clear out other files as we shouldn't be playing those.
		musicFilesToPlay.clear();
		try{
			player.open(url);
			player.play();
			vehicleWithRadio = vehicle;
			return true;
		}catch(Exception e){
			MTS.MTSLog.error("ERROR: BASICPLAYER URL PLAY CODE HAS FAULTED.");
			MTS.MTSLog.error(e.getMessage());
			return false;
		}
	}
	
	/**Stops all playing music.**/
	public static void stop(){
		try{
			player.stop();
			vehicleWithRadio = null;
		}catch(Exception e){
			MTS.MTSLog.error("ERROR: BASICPLAYER STOP CODE HAS FAULTED.");
			MTS.MTSLog.error(e.getMessage());
		}
	}
	
	/**Gets the current volume as a normalized value.**/
	public static float getVolume(){
		return (player.getMaximumGain() - player.getMinimumGain())/player.getGainValue();
	}
	
	/**Sets the player volume.  Parameter should be from 0.0-1.0, but can be greater and will be clamped.**/
	public static void setVolume(float volume){
		try{
			player.setGain(volume > 1 ? 1 : volume);
		}catch(BasicPlayerException e){
			MTS.MTSLog.error("ERROR: BASICPLAYER VOLUME CODE HAS FAULTED.");
			MTS.MTSLog.error(e.getMessage());
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
	public static List<String> getMusicFiles(String directoryName, boolean sort){
		List<String> musicFiles = new ArrayList<String>();
		for(File musicFile : new File(musicDir.getAbsolutePath() + File.separator + directoryName).listFiles()){
			if(!musicFile.isDirectory()){
				musicFiles.add(musicFile.getName());
			}
		}
		if(sort){
			Collections.sort(musicFiles);
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
		}
		//Don't sort the stations, as we want the order the user put them in.
		return stations;
	}
	
	/**Sets the radio stations to the passed-in value and saves them to the radio_stations.txt file.
	**/
	public static void setRadioStations(List<String> stations){
		try{
			BufferedWriter radioStationFileWriter = new BufferedWriter(new FileWriter(radioStationsFile));
			for(String station : stations){
				radioStationFileWriter.write(station + "\n");
			}
			radioStationFileWriter.close();
		}catch(IOException e){
			MTS.MTSLog.error("ERROR: UNABLE TO SAVE RADIO STATION FILE.");
			MTS.MTSLog.error(e.getMessage());
		}
	}
}
