package minecrafttransportsimulator.mcinterface;

import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;

/**Wrapper for interfacing with NBT data.  This pares down a few of the method to ones
 * more suited to what we use normally.  Of special importance is the ability to save
 * lists of data, which isn't normally allowed with NBT without loops.
 *
 * @author don_bruce
 */
public interface IWrapperNBT{
	
	//Booleans
	public boolean getBoolean(String name);
	
	public void setBoolean(String name, boolean value);
	
	//Integers
	public int getInteger(String name);
	
	public void setInteger(String name, int value);
	
	//Floating-point doubles.
	public double getDouble(String name);
	
	public void setDouble(String name, double value);
	
	//Strings.
	public String getString(String name);
	
	public void setString(String name, String value);
	
	//String array.
	public List<String> getStrings(String name, int qty);
	
	public void setStrings(String name, List<String> values);
	
	//Points.
	public Point3i getPoint3i(String name);
	
	public void setPoint3i(String name, Point3i value);
	
	public Point3d getPoint3d(String name);
	
	public void setPoint3d(String name, Point3d value);
	
	//Point array.
	public List<Point3i> getPoints(String name);
	
	public void setPoints(String name, List<Point3i> values);
	
	//NBT.
	public IWrapperNBT getData(String name);
	
	public void setData(String name, IWrapperNBT value);

	//Packet handling.
	public void writeToBuffer(ByteBuf buf);
}
