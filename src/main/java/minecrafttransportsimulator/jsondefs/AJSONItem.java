package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.PackResourceLoader.ItemClassification;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Base JSON class for all pack-loaded JSONs.  All pack-loaded JSONs MUST extend this class.
 * This class will be loaded by {@link PackParserSystem} to create objects that contain the properties
 * of the pack items that the sub-classes of this class define.
 * 
 * @author don_bruce
 */
public abstract class AJSONItem<GeneralConfig extends AJSONItem<GeneralConfig>.General>{
	/**A String containing any prefix folders for this item.  Used by the packloading system to tell
	 * where resources are relative to the item's JSON definition file path.  This contains all folders
	 * "between" the item's classification directory and the item's actual file.*/
	public String prefixFolders;
	/**ID of the pack that this JSON was pulled from.  Set after JSON is parsed into an object and used
	 * to tell MTS what jar to load assets from.*/
	public String packID;
	/**Unique name (per-pack) that is used to identify this item in the MTS system.  Set after JSON is 
	 * parsed into an object and used for lookup operations or for maps when we need to keep track of
	 * a pack component by name rather than an object instance.  Used heavily in save/load operations.*/
	public String systemName;
	/**Classification for this item.  This is used to determine what folders this JSON definition,
	 * and it's associated resources, are found in.*/
	public ItemClassification classification;
	/**Generic variable used to represent the general properties of this pack item.  Extend this and
	 * reference it in the generic for all sub-classes.*/
	public GeneralConfig general;

    public class General{    	
    	public String name;
    	public String description;
    	public List<String> materials;
    }
}