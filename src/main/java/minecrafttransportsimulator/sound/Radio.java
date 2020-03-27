package minecrafttransportsimulator.radio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javazoom.jlgui.basicplayer.BasicPlayer;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.radio.RadioManager;

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
	public String displayText;
	
	//Private runtime variables.
	private RadioSources source = RadioSources.LOCAL;
	private SoundInstance currentSound;
	private SoundInstance nextSound;
	private List<InputStream> nextSounds = new ArrayList<InputStream>();
	
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
	
	public Radio(){
		
	}
	
	/**
	 * Attempts to play music based on the current source and preset.
	 */
	public void pressPreset(){
		//Stop the decoders and delete the sound references.
		if(currentSound != null){
			currentSound.decoder.abort();
			currentSound = null;
		}
		if(nextSound != null){
			nextSound.decoder.abort();
			nextSound = null;
		}
		
		//Close all InputStreams we have open before we try and play anything else.
		for(InputStream stream : nextSounds){
			stream.close();
		}
		
		
		switch(source){
			case LOCAL : {
				if(!playFromDirectory()){
					displayText = "Fewer than " + presetIndex + " folders in mts_music.  Go add some!";
				}
				break;
			}
			case SERVER : {
				displayText = "This method of playback is not supported .... yet!";
				break;
			}
			case INTERNET : {
				displayText = "This method of playback is not supported .... yet!";
				break;
			}
		}
	}
	
	/**
	 * Queues up songs from the preset directory and processes them for playing.
	 * Returns true if the directory was found, false otherwise.
	 */
	private boolean playFromDirectory(){
		List<File> musicDirectories = new ArrayList<File>();
		for(File file : musicDir.listFiles()){
			if(file.isDirectory()){
				musicDirectories.add(file);
			}
		}
		Collections.sort(musicDirectories);
		
		//If we have the directory of the preset, load a song from it.
		if(musicDirectories.size() >= presetIndex){
			List<File> musicFiles = new ArrayList<File>();
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
			
			//Wrap files in an InputStream and exit.
			for(File file : musicFiles){
				nextSounds.add(new FileInputStream(file));
			}
			return true;
		}else{
			return false;
		}
	}
	
	
	private void 
	
	/**Sets the player to play sound files from a directory in the mts_music folder.
	 * Actual play code is done during the update loop as it's sequential.**/
	public void playLocal(String directoryName, boolean sorted){
		if(currentSound != null){
			currentSound.stopSound = true;
		}
		
		songsToPlay = RadioManager.getMusicFiles(directoryName, sorted);
		selectedPreset = presetPressed;
		selectedSource = directoryName;
	}
	
	/**Plays a sound file or stream from the web.  Returns true if the URL is able to be played.**/
	public boolean playInternet(URL url, int presetPressed){
		songsToPlay.clear();
		try{
			player.open(url);
			player.play();
			selectedPreset = presetPressed;
			selectedSource = url.toString();
			return true;
		}catch(Exception e){
			System.err.println("ERROR: BASICPLAYER URL PLAY CODE HAS FAULTED.");
			System.err.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	/**Plays a sound file from an InputStream.  Is a generic method for use with un-specified objects.
	 * Could be used for MC sounds in a jar, or music disks from other mods.**/
	public boolean playGeneric(InputStream stream){
		try{
			player.open(stream);
			player.play();
			selectedSource = "Streaming";
			return true;
		}catch(Exception e){
			System.err.println("ERROR: BASICPLAYER INTERNAL PLAY CODE HAS FAULTED.");
			System.err.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	/**Get the current play state of the radio.
	 * Returns -1 for not playing anything.
	 * Returns 1-10 for playing from the internet.
	 * Returns 11-20 for playing from a folder.**/
	public int getPlayState(){
		if(player.getStatus() == BasicPlayer.PLAYING){
			return songsToPlay.isEmpty() ? selectedPreset + 10 : selectedPreset;
		}
		return -1;
	}
	
	/**Gets the current preset pressed for this radio.**/
	public int getPresetSelected(){
		return selectedPreset;
	}
	
	/**Gets the current source for this radio.  If we are playing from the internet, it will
	 * be the URL.  If we are playing from a directory, it will be the directory name with
	 * the file name of the current song after a newline.**/
	public String getSource(){
		return selectedSource;
	}
	
	/**Stops all playing music.  Should be called when the class containing this RadioContainer is destroyed.
	 * If it's not, then the radio won't stop even if the thing is gone!**/
	public void stopPlaying(){
		songsToPlay.clear();
		selectedPreset = -1;
		selectedSource = "";
		try{
			player.stop();
		}catch(Exception e){
			System.err.println("ERROR: BASICPLAYER STOP CODE HAS FAULTED.");
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**Should be called every tick to update the volume and play/pause status.
	 * Return false if we need to remove this radio because it's invalid.
	 * Passed-in coords are where the listener is located.**/
	public boolean update(double x, double y, double z, boolean enablePlayback){
		try{
			if(container.isValid()){
				if(player.getStatus() == BasicPlayer.PLAYING){
					if(enablePlayback){
						//Set the volume to the player distance.
						double dist = container.getDistanceTo(x, y, z);
						if(dist > 0){
							player.setGain(Math.min(2F*(volume/10F), 1.0F)/dist);
						}else{
							player.setGain(volume/10F);
						}
					}else{
						player.pause();
					}
				}else if(player.getStatus() == BasicPlayer.STOPPED || player.getStatus() == BasicPlayer.UNKNOWN){
					//If we are stopped, and we are have music files to play, go to the next song.
					//Otherwise, clear out our variables.
					if(!songsToPlay.isEmpty()){
						player.open(songsToPlay.get(0));
						player.play();
						selectedSource = "Station:  " + songsToPlay.get(0).getParentFile().getName() + "\nPlaying:  " + songsToPlay.get(0).getName() + "\nUp Next: " + (songsToPlay.size() > 1 ? songsToPlay.get(1).getName() : ""); 
						songsToPlay.remove(0);
					}else{
						if(selectedPreset != -1){
							songsToPlay.clear();
							selectedPreset = -1;
							selectedSource = "";
						}
					}
				}else if(enablePlayback && player.getStatus() == BasicPlayer.PAUSED){
					player.resume();
				}
			}else{
				stopPlaying();
				return false;
			}
		}catch(Exception e){
			System.err.println("ERROR: BASICPLAYER INTERNAL UPDATED CODE HAS FAULTED.");
			System.err.println(e.getMessage());
			e.printStackTrace();
			stopPlaying();
		}
		return true;
	}
	
	/**Sets the player volume.  Parameter should be from 0-10, but can be greater and will be clamped.**/
	public void setVolume(byte newVolume){
		volume = newVolume > 10 ? 10 : newVolume;
	}
	
	/**Gets the current volume as a normalized value.**/
	public byte getVolume(){
		return volume;
	}
	
	public enum RadioSources{
		LOCAL,
		SERVER,
		INTERNET;
	}
}