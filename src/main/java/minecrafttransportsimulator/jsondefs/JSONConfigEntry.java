package minecrafttransportsimulator.jsondefs;

/**
 * Config entry.  This is generic and used for most config entries in the JSON.
 * is called.  Default values are assigned in the field declaration, while
 * comments are simply fields of their own.  Note that sub-classes MUST
 * be static to use their default values!
 *
 * @author don_bruce
 */
public class JSONConfigEntry<ConfigType> {
    public ConfigType value;
    public String comment;

    public JSONConfigEntry(ConfigType defaultValue, String comment) {
        this.value = defaultValue;
        this.comment = comment;
    }
}
