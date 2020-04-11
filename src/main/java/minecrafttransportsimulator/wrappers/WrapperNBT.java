package minecrafttransportsimulator.wrappers;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3i;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**Wrapper for interfacing with NBT data.  This pares down a few of the method to ones
 * more suited to what we use normally.  Of special importance is the ability to save
 * lists of data, which isn't normally allowed with NBT without loops.
 *
 * @author don_bruce
 */
public class WrapperNBT{
	protected final NBTTagCompound tag;
	
	public WrapperNBT(NBTTagCompound tag){
		this.tag = tag;
	}
	
	public WrapperNBT(ItemStack stack){
		if(stack.hasTagCompound()){
			this.tag = stack.getTagCompound();
		}else{
			this.tag = new NBTTagCompound();
			stack.setTagCompound(tag);
		}
	}
	
	//Booleans
	public boolean getBoolean(String name){
		return tag.getBoolean(name);
	}
	
	public void setBoolean(String name, boolean value){
		tag.setBoolean(name, value);
	}
	
	//Integers
	public int getInteger(String name){
		return tag.getInteger(name);
	}
	
	public void setInteger(String name, int value){
		tag.setInteger(name, value);
	}
	
	//Floating-point doubles.
	public double getDouble(String name){
		return tag.getDouble(name);
	}
	
	public void setDouble(String name, double value){
		tag.setDouble(name, value);
	}
	
	//Strings.
	public String getString(String name){
		return tag.getString(name);
	}
	
	public void setString(String name, String value){
		tag.setString(name, value);
	}
	
	
	//String array.
	public List<String> getStrings(String name, int qty){
		List<String> values = new ArrayList<String>();
		for(int i=0; i<qty; ++i){
			values.add(getString(name + i));
        }
		return values;
	}
	
	public void setStrings(String name, List<String> values){
		for(int i=0; i<values.size(); ++i){
			setString(name + i, values.get(i));
		}
	}
	
	//Point array.
	public List<Point3i> getPoints(String name){
		List<Point3i> values = new ArrayList<Point3i>();
		int count = getInteger(name + "count");
		for(int i=0; i<count; ++i){
			Point3i point = new Point3i(getInteger(name + i + "x"), getInteger(name + i + "y"), getInteger(name + i + "z"));
			if(!point.equals(Point3i.ZERO)){
				values.add(point);
			}
        }
		return values;
	}
	
	public void setPoints(String name, List<Point3i> values){
		setInteger(name + "count", values.size());
		for(int i=0; i<values.size(); ++i){
			setInteger(name + i + "x", values.get(i).x);
			setInteger(name + i + "y", values.get(i).y);
			setInteger(name + i + "z", values.get(i).z);
		}
	}
}
