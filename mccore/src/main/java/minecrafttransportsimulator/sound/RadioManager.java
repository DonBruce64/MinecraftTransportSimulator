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

import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Class that manages all radios and stations.  Responsible for creating new stations and storing them,
 * as well as giving said stations to radios when they request them.  This class also interfaces with
 * the files on the local machine and keeps track of the order they are played in.
 *
 * @author don_bruce
 */
public class RadioManager {
    private static final File musicDir;
    private static final File radioStationsFile;
    private static final Map<Integer, RadioStation> localSourcesMap = new HashMap<>();
    private static final Map<String, RadioStation> internetSourcesMap = new HashMap<>();

    /**
     * Need to set up global radio variables before we can create an instance of a radio.
     */
    static {
        musicDir = new File(InterfaceManager.gameDirectory, "mts_music");
        musicDir.mkdir();
        radioStationsFile = new File(musicDir.getAbsolutePath() + File.separator + "radio_stations.txt");
        if (!radioStationsFile.exists()) {
            try {
                radioStationsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets the local station for the specific preset.
     * Creates one it if it hasn't been created yet.
     */
    public static RadioStation getLocalStation(int index, boolean randomOrder) {
        //No clue why we have to use Integer.valueOf, but whatever...
        Integer mapKey = index;
        if (!localSourcesMap.containsKey(mapKey)) {
            localSourcesMap.put(mapKey, new RadioStation(index, randomOrder));
        }
        return localSourcesMap.get(mapKey);
    }

    /**
     * Gets the internet station for the specific URL.
     * Creates one it if it hasn't been created yet.
     */
    public static RadioStation getInternetStation(String url) {
        if (!internetSourcesMap.containsKey(url)) {
            internetSourcesMap.put(url, new RadioStation(url));
        }
        return internetSourcesMap.get(url);
    }

    /**
     * Queues up songs from the preset directory for playing.
     * Returns the files in the directory if they were found, or an empty list otherwise.
     */
    public static List<File> parseLocalDirectory(int index, boolean randomOrder) {
        List<File> musicDirectories = new ArrayList<>();
        List<File> musicFiles = new ArrayList<>();
        for (File file : musicDir.listFiles()) {
            if (file.isDirectory()) {
                musicDirectories.add(file);
            }
        }
        Collections.sort(musicDirectories);

        //If we have the directory of the preset, load all the files in it.
        if (musicDirectories.size() > index) {
            for (File musicFile : musicDirectories.get(index).listFiles()) {
                if (!musicFile.isDirectory()) {
                    musicFiles.add(musicFile);
                }
            }
            if (randomOrder) {
                Collections.shuffle(musicFiles);
            } else {
                Collections.sort(musicFiles);
            }
        }
        return musicFiles;
    }

    /**
     * Gets the radio URL for the specified index in the radio_stations.txt file in the mts_music directory.
     */
    public static String getLocalStationURL(int index) {
        try {
            List<String> stations = new ArrayList<>();
            BufferedReader radioStationFileReader = new BufferedReader(new FileReader(radioStationsFile));
            while (radioStationFileReader.ready()) {
                stations.add(radioStationFileReader.readLine());
            }
            radioStationFileReader.close();

            //Subtract one off the index as presets are 1-indexed.
            --index;
            if (stations.size() > index) {
                return stations.get(index);
            } else {
                return "";
            }
        } catch (IOException e) {
            InterfaceManager.coreInterface.logError("Unable to parse radio_stations.txt file.  Is it in use?");
            InterfaceManager.coreInterface.logError(e.getMessage());
            return "";
        }
    }

    /**
     * Sets the radio URL to the passed-in value and saves it to the radio_stations.txt file.
     */
    public static void setLocalStationURL(String stationURL, int index) {
        try {
            List<String> stations = new ArrayList<>();
            BufferedReader radioStationFileReader = new BufferedReader(new FileReader(radioStationsFile));
            while (radioStationFileReader.ready()) {
                stations.add(radioStationFileReader.readLine());
            }
            radioStationFileReader.close();

            //If we have less than 6 stations, make 6 blanks ones to avoid crashes.
            if (stations.size() < 6) {
                for (int i = stations.size(); i < 6; ++i) {
                    stations.add("");
                }
            }
            stations.set(index, stationURL);
            BufferedWriter radioStationFileWriter = new BufferedWriter(new FileWriter(radioStationsFile));
            for (String stationToWrite : stations) {
                radioStationFileWriter.write(stationToWrite + "\n");
            }
            radioStationFileWriter.close();
        } catch (IOException e) {
            InterfaceManager.coreInterface.logError("Unable to save radio_stations.txt file.  Is it in use?");
            InterfaceManager.coreInterface.logError(e.getMessage());
        }
    }

    public enum RadioSources {
        LOCAL,
        SERVER,
        INTERNET
    }
}