package minecrafttransportsimulator.systems;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.Gson;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSPackObject;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackFileDefinitions;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.main.EntityCar;
import minecrafttransportsimulator.entities.main.EntityPlane;
import net.minecraftforge.common.MinecraftForge;

/**
 * Class responsible for parsing content pack data.  Gets properties from the text files that other parts
 * of the code can use.
 *
 * @author don_bruce
 */

public final class PackParserSystem{
    private static Map<String, MTSPackObject> packMap = new HashMap<String, MTSPackObject>();
    /**List of log file generated during parsing.
     * Stored here as we can't output these during boot on 1.10-1.11 due to registerItems running before the log is set up.
     */
    private static final List<String> log = new ArrayList<String>();

    /**Parses the entire pack directory and loads it into the registers.  Also creates the custom
     * ResourceLocation for item JSON if needed.
     */
    public static void init(){
    	log.clear();
        log.add("Initilizing pack parsing system.");
        
    	File assetDir = new File(MTS.assetDir);
        File jsonDir = new File(assetDir.getAbsolutePath() + File.separator + "jsondefs");
        File modelDir = new File(assetDir.getAbsolutePath() + File.separator + "models");
        
        //If we don't have the folders for pack info, make it now.
        if(!assetDir.exists() || !jsonDir.exists() || !modelDir.exists()){
        	try{
	        	assetDir.mkdirs();
	            new File(assetDir.getAbsolutePath() + File.separator + "models").mkdirs();
	            new File(assetDir.getAbsolutePath() + File.separator + "models" + File.separator + "item").mkdirs();
	            new File(assetDir.getAbsolutePath() + File.separator + "models" + File.separator + "item" + File.separator + "AUTO_GENERATED_FILES").createNewFile();
	                        
	            new File(assetDir.getAbsolutePath() + File.separator + "textures").mkdirs();
	            new File(assetDir.getAbsolutePath() + File.separator + "textures" + File.separator + "items").mkdirs();
	            new File(assetDir.getAbsolutePath() + File.separator + "textures" + File.separator + "items" + File.separator + "PUT_ITEM_TEXTURES_IN_THIS_FOLDER").createNewFile();
	            new File(assetDir.getAbsolutePath() + File.separator + "textures" + File.separator + "models").mkdirs();
	            new File(assetDir.getAbsolutePath() + File.separator + "textures" + File.separator + "models" + File.separator + "PUT_MODEL_TEXTURES_IN_THIS_FOLDER").createNewFile();
	            new File(assetDir.getAbsolutePath() + File.separator + "textures" + File.separator + "hud").mkdirs();
	            new File(assetDir.getAbsolutePath() + File.separator + "textures" + File.separator + "hud" + File.separator + "PUT_HUD_TEXTURES_IN_THIS_FOLDER").createNewFile();
	            
	            new File(assetDir.getAbsolutePath() + File.separator + "objmodels").mkdirs();
	            new File(assetDir.getAbsolutePath() + File.separator + "objmodels" + File.separator + "PUT_OBJ_MODELS_IN_THIS_FOLDER").createNewFile();
	            
	            new File(assetDir.getAbsolutePath() + File.separator + "jsondefs").mkdirs();
	            new File(assetDir.getAbsolutePath() + File.separator + "jsondefs" + File.separator + "PUT_MASTER_JSON_FILES_IN_THIS_FOLDER").createNewFile();
			}catch(IOException e){
				e.printStackTrace();
				return;
			}
        }
        
        //Now that we are assured we have the directories, check the mods folder for new pack content.
        File modDir = new File(System.getProperty("user.dir") + File.separator + "mods");
        if(modDir.exists()){
        	parseModDirectory(modDir, assetDir);
        }
    	//Also check version-specific sub-directory for content.
        modDir = new File(System.getProperty("user.dir") + File.separator + "mods" + File.separator + MinecraftForge.MC_VERSION);
        if(modDir.exists()){
        	parseModDirectory(modDir, assetDir);
        }
        
    	//Finally, parse the pack info.
    	log.add("Parsing pack directory: " + jsonDir.getAbsolutePath());
        for(File file : jsonDir.listFiles()){
            if(file.getName().endsWith(".json")){
            	log.add("Parsing file: " + file.getName());
                parseJSONFile(file);
            }
        }
        if(MTS.MTSLog != null){
        	writeLogOutput();
        }
    }

    private static boolean parseModDirectory(File modDir, File assetDir){
    	boolean foundPack = false;
    	for(File modFile : modDir.listFiles()){
    		if(modFile.getName().endsWith(".jar")){
    			log.add("Checking the following jar file for pack data: " + modFile.getAbsolutePath());
    			byte packDefsAdded = 0;
    			try{
    				ZipFile jarFile = new ZipFile(modFile);
    				Enumeration<? extends ZipEntry> jarEnum = jarFile.entries();
    				while(jarEnum.hasMoreElements()){
    					ZipEntry jarEntry = jarEnum.nextElement();
    					if(jarEntry.getName().contains("mts/")){
    						//Check to see if this file is a directory.  If so, ignore it and go on.
    						if(jarEntry.isDirectory()){
    							continue;
    						}else if(jarEntry.getName().contains("jsondefs")){
    							++packDefsAdded;
    						}
    						
    						//If the file exists, replace it to update it.
    						File outputfile = new File(assetDir + File.separator + jarEntry.getName().substring("mts/".length()));
    						if(outputfile.exists()){
    							outputfile.delete();
    						}
    						
    						//Now copy over the file.
    						InputStream inputStream = jarFile.getInputStream(jarEntry);
    						FileOutputStream outputStream = new FileOutputStream(outputfile);
    						
    						byte[] bytes = new byte[1024];
    						int length = inputStream.read(bytes);
    						while(length >= 0){
    							outputStream.write(bytes, 0, length);
    							length = inputStream.read(bytes);
    						}
    						inputStream.close();
    						outputStream.close();
    						foundPack = true;
    					}
    				}
    				jarFile.close();
    			}catch(IOException e){
    				e.printStackTrace();
    				return false;
    			}
    			if(packDefsAdded >= 0){
    				log.add("Found " + packDefsAdded + " pack definitions inside: " + modFile.getAbsolutePath());
    			}
    		}
    	}
    	return foundPack;
    }

    private static void parseJSONFile(File file){
    	MTSPackObject pack = null;
    	try{
        	//Check to make sure we are trying to parse a vaild file.
        	BufferedReader buffer = new BufferedReader(new FileReader(file));
        	boolean validFile = false;
        	while(buffer.ready()){
        		String line = buffer.readLine();
        		if(line.contains("packVersion")){
        			byte packVersionNumber = Byte.valueOf(line.substring(line.indexOf(':') + 1, line.indexOf(',')).trim());
        			if(packVersionNumber == MTS.packJSONVersionNumber){
            			validFile = true;
        			}else{
        				log.add("ERROR!  Found JSON with version " + packVersionNumber +". We are using " + MTS.packJSONVersionNumber + ". This file will NOT be loaded!");
        			}
        			break;
        		}
        	}
        	buffer.close();
        	if(!validFile){
        		log.add("ERROR!  Invalid JSON file detected.  This file will NOT be loaded!");
        		return;
        	}
            pack = new Gson().fromJson(new FileReader(file), MTSPackObject.class);
        }catch(Exception e){
        	log.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + file.getName());
            e.printStackTrace();
        }
    	
    	//Create pack data
        for(PackFileDefinitions definition : pack.definitions){
        	if(definition != null){
        		log.add("Adding pack definition: " + definition.uniqueName);
	        	packMap.put(definition.uniqueName, pack);
        	}else{
        		log.add("An extra comma was detcted when parsing a json file.  This could get ugly.");
        	}
        }  
    }

    public static MTSPackObject getPack(String name){
        return packMap.get(name);
    }
    
    public static Set<String> getRegisteredNames(){
        return packMap.keySet();
    }
    
    
    public static PackFileDefinitions getDefinitionForPack(String name){
		for(PackFileDefinitions definition : PackParserSystem.getPack(name).definitions){
			if(definition.uniqueName.equals(name)){
				return definition;
			}
		}
		return null;
    }
    
    public static MultipartTypes getMultipartType(String packName){
    	MTSPackObject pack = getPack(packName);
    	for(MultipartTypes type : MultipartTypes.values()){
    		if(pack.general.type.equals(type.name().toLowerCase())){
    			return type;
    		}
    	}
    	return null;
    }
    
    public static void writeLogOutput(){
    	for(String logLine : log){
    		MTS.MTSLog.info(logLine);
    	}
    }
    
    public enum MultipartTypes{
    	PLANE(EntityPlane.class),
    	CAR(EntityCar.class);
    	
    	public final Class<? extends EntityMultipartMoving> multipartClass;
    	
    	private MultipartTypes(Class<? extends EntityMultipartMoving> multipartClass){
    		this.multipartClass = multipartClass;
    	}
    }
}
