package minecrafttransportsimulator.mcinterface;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.packloading.PackParser;

/**
 * IWrapper for interfacing with NBT data.  This pares down a few of the method to ones
 * more suited to what we use normally.  Of special importance is the ability to save
 * complex data objects like Point3d and lists.  Note that if a stack without a tag is
 * passed-in to the constructor, one is created.  This is to prevent excess null checks.
 *
 * @author don_bruce
 */
public interface IWrapperNBT {

    /**Helper method to compare data which allows null objects.  Prevents excess code for null checks.**/
    public static boolean isDataEqual(IWrapperNBT data1, IWrapperNBT data2) {
        if (data1 == null) {
            return data2 == null;
        } else if (data2 == null) {
            return data1 == null;
        } else {
            return data1.equals(data2);
        }
    }

    /**
     * Helper method to remove all UUID tags from NBT.
     * Required when saving things as items because UUID data is only for things in the world.
     */
    public default void deleteAllUUIDTags() {
        deleteEntry(AEntityA_Base.UNIQUE_UUID_TAG_NAME);
        getAllNames().forEach(name -> {
            IWrapperNBT data2 = getData(name);
            if (data2 != null) {
                data2.deleteAllUUIDTags();
            }
        });
    }

    boolean getBoolean(String name);

    void setBoolean(String name, boolean value);

    int getInteger(String name);

    void setInteger(String name, int value);

    double getDouble(String name);

    void setDouble(String name, double value);

    String getString(String name);

    void setString(String name, String value);

    List<String> getStrings(String name);

    List<String> getStrings(String name, int count);

    void setStrings(String name, Collection<String> values);

    UUID getUUID(String name);

    void setUUID(String name, UUID value);

    @SuppressWarnings("unchecked")
    default <ItemPackActual extends AItemPack<?>> ItemPackActual getPackItem() {
        return (ItemPackActual) PackParser.getItem(getString("packID"), getString("systemName"), getString("subName"));
    }

    default void setPackItem(AJSONItem definition, String subName) {
        setString("packID", definition.packID);
        setString("systemName", definition.systemName);
        setString("subName", subName);
    }

    List<IWrapperItemStack> getStacks(int count);

    void setStacks(List<IWrapperItemStack> stacks);

    Point3D getPoint3d(String name);

    void setPoint3d(String name, Point3D value);

    List<Point3D> getPoint3ds(String name);

    void setPoint3ds(String name, Collection<Point3D> values);

    /**
     * Casts-down doubles to ints for compact storage.
     **/
    Point3D getPoint3dCompact(String name);

    void setPoint3dCompact(String name, Point3D value);

    List<Point3D> getPoint3dsCompact(String name);

    void setPoint3dsCompact(String name, Collection<Point3D> values);

    /**
     * Returns the data, or null if it doesn't exist.
     **/
    IWrapperNBT getData(String name);

    void setData(String name, IWrapperNBT value);

    boolean hasKey(String name);

    void deleteEntry(String name);

    /**
     * Returns all key-tag names in this tag.
     **/
    Set<String> getAllNames();
}
