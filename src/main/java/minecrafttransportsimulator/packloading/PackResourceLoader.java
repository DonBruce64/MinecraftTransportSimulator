package minecrafttransportsimulator.packloading;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.systems.PackParserSystem;

/**
 * Class responsible for loading pack resource files from pack archives.  This happens both during load
 * and during gameplay as different references are requested.  One loader is created for every pack
 *
 * @author don_bruce
 */
public final class PackResourceLoader{
	
    public static String getPackResource(AJSONItem<?> definition, ResourceType type, String name){
    	switch(PackStructure.values()[PackParserSystem.getPackConfiguration(definition.packID).fileStructure]){
    		case DEFAULT : {
    			 return "/assets/" + definition.packID + "/" + type.prefixFolder + definition.classification.toDirectory() + name + type.normalSuffix;
    		}
    		case LAYERED : {
    			return "/assets/" + definition.packID + "/" + type.prefixFolder + definition.classification.toDirectory() + definition.prefixFolders + name + type.normalSuffix;
    		}
    		case MODULAR : {
    			return "/assets/" + definition.packID + "/" + definition.classification.toDirectory() + definition.prefixFolders + name + type.modularSuffix;
    		}
    	}
    	return null;
    }
    
    public enum PackStructure{
    	DEFAULT,
    	LAYERED,
    	MODULAR;
    }
    
    public enum ResourceType{
    	OBJ("objmodels/", ".obj", ".obj"),
    	PNG("textures/", ".png", ".png"),
    	ITEM_PNG("textures/items/", ".png", "_item.png"),
    	ITEM_JSON("models/item/", ".json", "_item.json");
    	
    	private final String prefixFolder;
    	private final String normalSuffix;
    	private final String modularSuffix;
    	
    	private ResourceType(String prefixFolder, String normalSuffix, String modularSuffix){
    		this.prefixFolder = prefixFolder;
    		this.normalSuffix = normalSuffix;
    		this.modularSuffix = modularSuffix;
    	}
    }
    
    public enum ItemClassification{
    	VEHICLE,
    	PART,
    	INSTRUMENT,
    	POLE,
    	ROAD,
    	DECOR,
    	ITEM,
    	SKIN;
    	
    	public static List<String> getAllTypesAsStrings(){
        	List<String> assetTypes = new ArrayList<String>();
        	for(ItemClassification classification : ItemClassification.values()){
        		assetTypes.add(classification.name().toLowerCase());
        	}
        	return assetTypes;
    	}
    	
    	public String toDirectory(){
    		return this.name().toLowerCase() + "s/";
    	}
    	
    	public static ItemClassification fromDirectory(String directory){
    		try{
    			return ItemClassification.valueOf(directory.substring(0, directory.length() - "s/".length()).toUpperCase());
    		}catch(Exception e){
    			throw new IllegalArgumentException("Was told to get classification for directory: " + directory + " but none exists.  Contact the mod author!");
    		}
    	}
    }
}
