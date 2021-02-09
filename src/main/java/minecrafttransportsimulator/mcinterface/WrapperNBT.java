package minecrafttransportsimulator.mcinterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**Wrapper for interfacing with NBT data.  This pares down a few of the method to ones
 * more suited to what we use normally.  Of special importance is the ability to save
 * complex data objects like Point3d and lists.  Note that if a stack without a tag is 
 * passed-in to the constructor, one is created.  This is to prevent excess null checks.
 *
 * @author don_bruce
 */
public class WrapperNBT{
	public final NBTTagCompound tag;
	
	public WrapperNBT(){
		this.tag = new NBTTagCompound();
	}
	
	public WrapperNBT(NBTTagCompound tag){
		this.tag = tag;
	}
	
	public WrapperNBT(ItemStack stack){
		this.tag = stack.getTagCompound() != null ? stack.getTagCompound() : new NBTTagCompound();
	}
	
	public boolean getBoolean(String name){
		return tag.getBoolean(name);
	}
	
	public void setBoolean(String name, boolean value){
		tag.setBoolean(name, value);
	}
	
	public int getInteger(String name){
		return tag.getInteger(name);
	}
	
	public void setInteger(String name, int value){
		tag.setInteger(name, value);
	}
	
	public double getDouble(String name){
		return tag.getDouble(name);
	}
	
	public void setDouble(String name, double value){
		tag.setDouble(name, value);
	}
	
	public String getString(String name){
		return tag.getString(name);
	}
	
	public void setString(String name, String value){
		tag.setString(name, value);
	}
	
	public List<String> getStrings(String name){
		return getStrings(name, getInteger(name + "count"));
	}
	
	public List<String> getStrings(String name, int count){
		List<String> values = new ArrayList<String>();
		for(int i=0; i<count; ++i){
			values.add(getString(name + i));
        }
		return values;
	}
	
	public void setStrings(String name, Collection<String> values){
		setInteger(name + "count", values.size());
		int index = 0;
		for(String value : values){
			setString(name + index++, value);
		}
	}
	
	
	//Point3d
	public Point3d getPoint3d(String name){
		return new Point3d(getDouble(name + "x"), getDouble(name + "y"), getDouble(name + "z"));
	}
	
	public void setPoint3d(String name, Point3d value){
		setDouble(name + "x", value.x);
		setDouble(name + "y", value.y);
		setDouble(name + "z", value.z);
	}
	
	public List<Point3d> getPoint3ds(String name){
		List<Point3d> values = new ArrayList<Point3d>();
		int count = getInteger(name + "count");
		for(int i=0; i<count; ++i){
			Point3d point = getPoint3d(name + i);
			if(!point.isZero()){
				values.add(point);
			}
        }
		return values;
	}
	
	public void setPoint3ds(String name, Collection<Point3d> values){
		setInteger(name + "count", values.size());
		int index = 0;
		for(Point3d value : values){
			setPoint3d(name + index, value);
		}
	}
	
	
	//Point3i
	public Point3i getPoint3i(String name){
		return new Point3i(getInteger(name + "x"), getInteger(name + "y"), getInteger(name + "z"));
	}
	
	public void setPoint3i(String name, Point3i value){
		setInteger(name + "x", value.x);
		setInteger(name + "y", value.y);
		setInteger(name + "z", value.z);
	}
	
	public List<Point3i> getPoint3is(String name){
		List<Point3i> values = new ArrayList<Point3i>();
		int count = getInteger(name + "count");
		for(int i=0; i<count; ++i){
			Point3i point = getPoint3i(name + i);
			if(!point.isZero()){
				values.add(point);
			}
        }
		return values;
	}
	
	public void setPoint3is(String name, Collection<Point3i> values){
		setInteger(name + "count", values.size());
		int index = 0;
		for(Point3i value : values){
			setPoint3i(name + index, value);
		}
	}
	
	public WrapperNBT getData(String name){
		return tag.hasKey(name) ? new WrapperNBT(tag.getCompoundTag(name)) : null;
	}
	
	public void setData(String name, WrapperNBT value){
		tag.setTag(name, value.tag);
	}
	
	public void deleteData(String name){
		tag.removeTag(name);
	}
}
