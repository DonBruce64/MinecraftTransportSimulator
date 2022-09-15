package minecrafttransportsimulator.mcinterface;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.Point3D;

/**
 * IWrapper for interfacing with NBT data.  This pares down a few of the method to ones
 * more suited to what we use normally.  Of special importance is the ability to save
 * complex data objects like Point3d and lists.  Note that if a stack without a tag is
 * passed-in to the constructor, one is created.  This is to prevent excess null checks.
 *
 * @author don_bruce
 */
public interface IWrapperNBT {

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

    /**
     * Returns the data, or a new data structure if it doesn't exist.
     **/
    IWrapperNBT getDataOrNew(String name);

    void setData(String name, IWrapperNBT value);

    void deleteData(String name);

    /**
     * Returns all key-tag names in this tag.
     **/
    Set<String> getAllNames();
}
