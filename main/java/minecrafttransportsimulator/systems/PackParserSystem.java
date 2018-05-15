package minecrafttransportsimulator.systems;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackMultipartObject;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackFileDefinitions;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Car;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;
import net.minecraft.util.ResourceLocation;

/**
 * Class responsible for parsing content pack data.  Gets properties from the text files that other parts
 * of the code can use.
 *
 * @author don_bruce
 */

public final class PackParserSystem{
	/**Pack map that keys the unique name of a multipart to it's pack.  Used for associating packs with saved multiparts.**/
    private static Map<String, PackMultipartObject> multipartPackMap = new HashMap<String, PackMultipartObject>();
    /**Same function as the multipart map, just for parts.**/
    private static Map<String, PackPartObject> partPackMap = new HashMap<String, PackPartObject>();
    
    /**Packs should call this upon load to add their multiparts to the mod.**/
    public static void addMultipartDefinition(ResourceLocation location){
    	
    }
    
    /**Packs should call this upon load to add their parts to the mod.**/
    public static void addPartDefinition(ResourceLocation location){
    	
    }
    
    /**Removes all pack and part definitions and sends out a packet to cause packs to re-send their data.
     * Used if dev mode is enabled to allow for quicker testing of new packs.**/
    public static void resetPackMaps(){
    	multipartPackMap.clear();
    	partPackMap.clear();
    	//TODO add packet to send to pack systems for a reload.
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

    public static PackMultipartObject getMultipartPack(String name){
        return multipartPackMap.get(name);
    }
    
    public static PackPartObject getPartPack(String name){
        return partPackMap.get(name);
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
    
    public enum MultipartTypes{
    	PLANE(EntityMultipartF_Plane.class),
    	CAR(EntityMultipartF_Car.class);
    	
    	public final Class<? extends EntityMultipartE_Vehicle> multipartClass;
    	
    	private MultipartTypes(Class<? extends EntityMultipartE_Vehicle> multipartClass){
    		this.multipartClass = multipartClass;
    	}
    }
}
