package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.packloading.PackResourceLoader.ItemClassification;
import minecrafttransportsimulator.packloading.PackResourceLoader.PackStructure;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Base JSON class for all pack-loaded JSONs.  All pack-loaded JSONs MUST extend this class.
 * This class will be loaded by {@link PackParserSystem} to create objects that contain the properties
 * of the pack items that the sub-classes of this class define.
 * 
 * @author don_bruce
 */
public abstract class AJSONItem<GeneralConfig extends AJSONItem<GeneralConfig>.General>{
	/**The structure for this pack item.  This determines the relative layout of files in
	 * the pack and tells systems how to load said files.*/
	public PackStructure structure;
	/**A String containing any prefix folders for this item.  Used by some packloading systems where the location
	 * of the item's resources is relative to the item's JSON definition file path.*/
	public String prefixFolders;
	/**The classification of this item.  Set after JSON is parsed into an object and used to tell which 
	 * sub-folder to look for assets in.  Note that multiple types of items may fall under a single classification.*/
	public ItemClassification classification;
	/**ID of the pack that this JSON was pulled from.  Set after JSON is parsed into an object and used
	 * to tell MTS what jar to load assets from.*/
	public String packID;
	/**Unique name (per-pack) that is used to identify this item in the MTS system.  Set after JSON is 
	 * parsed into an object and used for lookup operations or for maps when we need to keep track of
	 * a pack component by name rather than an object instance.  Used heavily in save/load operations.*/
	public String systemName;
	/**Generic variable used to represent the general properties of this pack item.  Extend this and
	 * reference it in the generic for all sub-classes.*/
	public GeneralConfig general;

    public class General{    	
    	public String name;
    	public String description;
    	public String[] materials;
    }
}