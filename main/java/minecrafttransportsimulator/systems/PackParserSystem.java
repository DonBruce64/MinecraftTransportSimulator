package minecrafttransportsimulator.systems;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSCreativeTabs;
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

    public static void init(){
        File assetDir = new File(MTS.assetDir);
        File jsonDir = new File(assetDir.getAbsolutePath() + File.separator + "jsondefs");
        MTS.MTSLog.info("Initilizing pack parsing system.");
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
    }

    private static void parseDirectory(File jsonDir){
    	MTS.MTSLog.info("Parsing directory: " + jsonDir.getAbsolutePath());
        for(File file : jsonDir.listFiles()){
            if(file.isDirectory()){
                parseDirectory(file);
            }else{
                if(file.getName().endsWith(".json")){
                	MTS.MTSLog.info("Parsing file: " + file.getName());
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
        	MTS.MTSLog.error("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + file.getName());
            e.printStackTrace();
        }
    	
    	//Create pack data
        for(PackFileDefinitions definition : pack.definitions){
        	if(definition != null){
	        	MTS.MTSLog.info("Adding pack definition: " + definition.uniqueName);
	        	packMap.put(definition.uniqueName, pack);
        	}else{
        		MTS.MTSLog.warn("An extra comma was detcted when parsing a json file.  This could get ugly.");
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
