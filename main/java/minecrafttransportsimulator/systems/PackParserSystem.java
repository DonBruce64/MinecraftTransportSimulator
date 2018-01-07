package minecrafttransportsimulator.systems;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSCreativeTabs;
import minecrafttransportsimulator.dataclasses.MTSExternalResourcePack;
import minecrafttransportsimulator.dataclasses.MTSPackObject;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackFileDefinitions;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.main.EntityPlane;
import net.minecraft.creativetab.CreativeTabs;

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
    	MTSExternalResourcePack.init();
        File assetDir = new File(MTS.assetDir);
        File jsonDir = new File(assetDir.getAbsolutePath() + File.separator + "jsondefs");
        log.clear();
        log.add("Initilizing pack parsing system.");
        if(!assetDir.exists() || !jsonDir.exists()){
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
			}catch (IOException e){
				e.printStackTrace();
			}
        }else{
            parseDirectory(jsonDir);
        }
        if(MTS.MTSLog != null){
        	writeLogOutput();
        }
    }

    private static void parseDirectory(File jsonDir){
    	log.add("Parsing directory: " + jsonDir.getAbsolutePath());
        for(File file : jsonDir.listFiles()){
            if(file.isDirectory()){
                parseDirectory(file);
            }else{
                if(file.getName().endsWith(".json")){
                	log.add("Parsing file: " + file.getName());
                    parseFile(file);
                }
            }
        }
    }

    private static void parseFile(File file){
    	MTSPackObject pack = null;
    	try{
        	//Check to make sure we are trying to parse a vaild file.
        	BufferedReader buffer = new BufferedReader(new FileReader(file));
        	boolean validFile = false;
        	while(buffer.ready()){
        		if(buffer.readLine().contains("definitions")){
        			validFile = true;
        			break;
        		}
        	}
        	buffer.close();
        	if(!validFile){
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
    	PLANE(EntityPlane.class, MTSCreativeTabs.tabMTSPlanes);
    	
    	public final Class<? extends EntityMultipartMoving> multipartClass;
    	public final CreativeTabs tabToDisplayOn;
    	
    	private MultipartTypes(Class<? extends EntityMultipartMoving> multipartClass, CreativeTabs tabToDisplayOn){
    		this.multipartClass = multipartClass;
    		this.tabToDisplayOn = tabToDisplayOn;
    	}
    }
}
