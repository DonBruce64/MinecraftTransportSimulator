package minecrafttransportsimulator.mcinterface;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.Point3D;

/**IWrapper for interfacing with NBT data.  This pares down a few of the method to ones
 * more suited to what we use normally.  Of special importance is the ability to save
 * complex data objects like Point3d and lists.  Note that if a stack without a tag is 
 * passed-in to the constructor, one is created.  This is to prevent excess null checks.
 *
 * @author don_bruce
 */
public interface IWrapperNBT {

    public boolean getBoolean(String name);

    public void setBoolean(String name, boolean value);

    public int getInteger(String name);

    public void setInteger(String name, int value);

    public double getDouble(String name);

    public void setDouble(String name, double value);

    public String getString(String name);

    public void setString(String name, String value);

    public List<String> getStrings(String name);

    public List<String> getStrings(String name, int count);

    public void setStrings(String name, Collection<String> values);

    public UUID getUUID(String name);

    public void setUUID(String name, UUID value);

    public List<IWrapperItemStack> getStacks(int count);

    public void setStacks(List<IWrapperItemStack> stacks);

    public Point3D getPoint3d(String name);

    public void setPoint3d(String name, Point3D value);

    public List<Point3D> getPoint3ds(String name);

    public void setPoint3ds(String name, Collection<Point3D> values);

    /**Casts-down doubles to ints for compact storage.**/
    public Point3D getPoint3dCompact(String name);

    public void setPoint3dCompact(String name, Point3D value);

    public List<Point3D> getPoint3dsCompact(String name);

    public void setPoint3dsCompact(String name, Collection<Point3D> values);

    /**Returns the data, or null if it doesn't exist.**/
    public IWrapperNBT getData(String name);

    /**Returns the data, or a new data structure if it doesn't exist.**/
    public IWrapperNBT getDataOrNew(String name);

    public void setData(String name, IWrapperNBT value);

    public void deleteData(String name);

    /**Returns all key-tag names in this tag.**/
    public Set<String> getAllNames();
}
