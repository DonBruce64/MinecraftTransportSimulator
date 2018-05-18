package minecrafttransportsimulator.systems;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.CreativeTabPack;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackMultipartObject;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackFileDefinitions;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Car;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;

/**
 * Class responsible for parsing content pack data.  Gets properties from the text files that other parts
 * of the code can use.
 *
 * @author don_bruce
 */
public final class PackParserSystem{
	/**Map that keys the unique name of a multipart to it's pack.  Used for associating packs with saved multiparts.**/
    private static Map<String, PackMultipartObject> multipartPackMap = new HashMap<String, PackMultipartObject>();
   
    /**Map that keys the unique name of a multipart to it's definition.  Used for definition-specific lookups.**/
    private static Map<String, PackFileDefinitions> multipartDefinitionMap = new HashMap<String, PackFileDefinitions>();
   
    /**Same function as the multipart map, just for parts.**/
    private static Map<String, PackPartObject> partPackMap = new HashMap<String, PackPartObject>();
  
    /**Listing of log messages.  Stored here on bootstrap and outputted once the logging system comes online.**/
    private static List<String> logList = new ArrayList<String>();
    
    
    
    /**Packs should call this upon load to add their multiparts to the mod.  This should be the location of the JSON file.**/
    public static void addMultipartDefinition(File jsonFile, String modID){
    	try{
    		PackMultipartObject pack = new Gson().fromJson(new FileReader(jsonFile), PackMultipartObject.class);
    		for(PackFileDefinitions definition : pack.definitions){
    			if(definition != null){
    				multipartPackMap.put(modID + ":" + definition.uniqueName, pack);
    				multipartDefinitionMap.put(modID + ":" + definition.uniqueName, definition);
    				if(!MTSRegistry.packTabs.containsKey(modID)){
    					MTSRegistry.packTabs.put(modID, new CreativeTabPack(modID));
    				}
    			}
    		}  
    	}catch(Exception e){
    		logList.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + modID + ":" + jsonFile.getName());
    		logList.add(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their parts to the mod.  This should be the location of the JSON file.**/
    public static void addPartDefinition(File jsonFile, String modID){
    	try{
	    	PackPartObject pack =  new Gson().fromJson(new FileReader(jsonFile), PackPartObject.class);
	    	partPackMap.put(modID + ":" + jsonFile.getName().substring(0, jsonFile.getName().length() - ".json".length()), pack);
    	}catch(Exception e){
    		logList.add("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + modID + ":" + jsonFile.getName());
    		logList.add(e.getMessage());
    	}
    }
    
    /**Removes all pack and part definitions and sends out a packet to cause packs to re-send their data.
     * Used if dev mode is enabled to allow for quicker testing of new packs.**/
    public static void resetPackMaps(){
    	multipartPackMap.clear();
    	multipartDefinitionMap.clear();
    	partPackMap.clear();
    	//TODO add packet to send to pack systems for a reload.
    }

    public static void outputLog(){
    	for(String logLine : logList){
    		MTS.MTSLog.error(logLine);
    	}
    	logList.clear();
    }

    public static PackMultipartObject getMultipartPack(String name){
        return multipartPackMap.get(name);
    }
    
    public static Set<String> getAllMultipartPackNames(){
        return multipartPackMap.keySet();
    }
    
    public static PackFileDefinitions getDefinitionForMultipart(String name){
    	return multipartDefinitionMap.get(name);
    }
    
    public static PackPartObject getPartPack(String name){
        return partPackMap.get(name);
    }
    
    public static Set<String> getAllPartPackNames(){
        return partPackMap.keySet();
    }
    
    public static MultipartTypes getMultipartType(String packName){
    	PackMultipartObject pack = getMultipartPack(packName);
    	for(MultipartTypes type : MultipartTypes.values()){
    		if(pack.general.type.equals(type.name().toLowerCase())){
    			return type;
    		}
    	}
    	return null;
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
