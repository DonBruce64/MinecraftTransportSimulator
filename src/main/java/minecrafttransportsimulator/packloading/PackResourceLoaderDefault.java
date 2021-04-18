package minecrafttransportsimulator.packloading;

import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.systems.PackParserSystem;

/**
 * Default implementation of the resource loader.
 *
 * @author don_bruce
 */
public class PackResourceLoaderDefault extends APackResourceLoader{
	public static final PackResourceLoaderDefault INSTANCE = new PackResourceLoaderDefault();
	
	@Override
	public String getPackResource(AJSONItem definition, ResourceType type, String name){
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
}
