package minecrafttransportsimulator.packloading;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.jsondefs.AJSONItem;

/**
 * Class responsible for loading pack resource files from pack archives.  This happens both during load
 * and during gameplay as different references are requested.  One loader is created for every pack
 *
 * @author don_bruce
 */
public final class PackResourceLoader{
	
    public static String getPackResource(AJSONItem<?> definition, ResourceType type, String name){
    	switch(definition.structure){
    		case DEFAULT : {
    			 return type.getMainFolder() + definition.classification.getClassificationFolder() + name + type.getFileSuffix();
    		}
    		case LAYERED : {
    			//TODO this can't happen until we load our own resources.
    		}
    		case MODULAR : {
    			//TODO this can't happen until we load our own resources.
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
    	OBJ("objmodels/"),
    	PNG("textures/"),
    	PNG_ITEM("textures/items/");
    	
    	private final String folder;
    	
    	private ResourceType(String folder){
    		this.folder = folder;
    	}
    	
    	public String getMainFolder(){
    		return folder;
    	}
    	
    	public String getFileSuffix(){
    		return "." + this.name().toLowerCase();
    	}
    }
    
    public enum ItemClassification{
    	VEHICLE,
    	PART,
    	INSTRUMENT,
    	POLE,
    	DECOR,
    	ITEM,
    	BOOKLET;
    	
    	public String getClassificationFolder(){
    		return this.name().toLowerCase() + "s/";
    	}
    	
    	public static List<String> getAllTypesAsStrings(){
        	List<String> assetTypes = new ArrayList<String>();
        	for(ItemClassification classification : ItemClassification.values()){
        		assetTypes.add(classification.name().toLowerCase());
        	}
        	return assetTypes;
    	}
    }
}
