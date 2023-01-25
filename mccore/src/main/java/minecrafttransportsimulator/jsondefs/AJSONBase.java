package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.packloading.PackResourceLoader.ItemClassification;

/**
 * Base JSON class for all pack-loaded content.  All pack-loaded JSONs MUST extend this class.
 * This class will be loaded by {@link PackParser} to create objects that contain the properties
 * of the pack items that the sub-classes of this class define.
 *
 * @author don_bruce
 */
public abstract class AJSONBase {
    /**
     * ID of the pack that this JSON was pulled from.  Set after JSON is parsed into an object and used
     * to tell MTS what jar to load assets from.
     */
    public transient String packID;
    /**
     * Unique name (per-pack) that is used to identify this entry in the MTS system.  Set after JSON is
     * parsed into an object and used for lookup operations or for maps when we need to keep track of
     * a pack component by name rather than an object instance.  Used heavily in save/load operations.
     */
    public transient String systemName;
    /**
     * A String containing any prefix folders for this object.  Used by the packloading system to tell
     * where resources are relative to the object's JSON definition file path.  This contains all folders
     * "between" the object's classification directory and the object's actual file.
     */
    public transient String prefixFolders;
    /**
     * Classification for this object.  This is used to determine what folders this JSON definition,
     * and it's associated resources, are found in.
     */
    public transient ItemClassification classification;
}
