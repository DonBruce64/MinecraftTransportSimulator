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
import minecrafttransportsimulator.dataclasses.PackMultipartObject;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackFileDefinitions;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Car;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;
import net.minecraftforge.common.MinecraftForge;

/**
 * Class responsible for parsing content pack data.  Gets properties from the text files that other parts
 * of the code can use.
 *
 * @author don_bruce
 */

public final class PackParserSystem{
    private static Map<String, PackMultipartObject> packMap = new HashMap<String, PackMultipartObject>();
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
        	log.add("Checking base mod dir for new content located at: " + modDir.getAbsolutePath());
        	parseModDirectory(modDir, assetDir);
        }
    	//Also check version-specific sub-directory for content.
        modDir = new File(System.getProperty("user.dir") + File.separator + "mods" + File.separator + MinecraftForge.MC_VERSION);
        if(modDir.exists()){
        	log.add("Checking version-specific mod dir for new content located at: " + modDir.getAbsolutePath());
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
    			byte packDefsAdded = 0;
    			try{
    				ZipFile jarFile = new ZipFile(modFile);
    				
    				//Check to make sure this isn't the main MTS mod jar before parsing.
    				boolean isValidFile = true;
    				Enumeration<? extends ZipEntry> jarEnum = jarFile.entries();
    				while(jarEnum.hasMoreElements()){
    					String jarEnumName = jarEnum.nextElement().getName();
    					if(jarEnumName.contains(".class")){
    						//This has class files.  Unless the only class file is MTSPackLoader, we don't want it!
    						if(!jarEnumName.contains("MTSPackLoader")){
        						isValidFile = false;
        						break;
    						}
    					}
    				}
    				
    				if(isValidFile){
    	    			log.add("Checking the following jar file for pack data: " + modFile.getAbsolutePath());
    					jarEnum = jarFile.entries();
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
	    						File outputfile = new File(assetDir + File.separator + jarEntry.getName().substring("assets/mts/".length()));
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
	    			}
    			}catch(Exception e){
    				e.printStackTrace();
    				return false;
    			}
    			if(packDefsAdded > 0){
    				log.add("Found " + packDefsAdded + " pack definitions inside: " + modFile.getAbsolutePath());
    			}
    		}
    	}
    	return foundPack;
    }

    private static void parseJSONFile(File file){
    	PackMultipartObject pack = null;
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
            pack = new Gson().fromJson(new FileReader(file), PackMultipartObject.class);
    	}catch(Exception e){
        	log.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + file.getName());
        	log.add(e.getMessage());
        	return;
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

    public static PackMultipartObject getPack(String name){
        return packMap.get(name);
    }
    
    public static PackPartObject getPartData(String name){
    	//TODO add return here.
        return ;
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
    	PackMultipartObject pack = getPack(packName);
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
    	PLANE(EntityMultipartF_Plane.class),
    	CAR(EntityMultipartF_Car.class);
    	
    	public final Class<? extends EntityMultipartE_Vehicle> multipartClass;
    	
    	private MultipartTypes(Class<? extends EntityMultipartE_Vehicle> multipartClass){
    		this.multipartClass = multipartClass;
    	}
    }
}
