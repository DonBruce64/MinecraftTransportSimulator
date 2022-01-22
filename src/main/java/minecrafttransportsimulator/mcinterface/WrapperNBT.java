package minecrafttransportsimulator.mcinterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;


import net.minecraft.inventory.ItemStackHelper;
import minecrafttransportsimulator.baseclasses.Point3dPlus;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;

/**Wrapper for interfacing with NBT data.  This pares down a few of the method to ones
 * more suited to what we use normally.  Of special importance is the ability to save
 * complex data objects like Point3d and lists.  Note that if a stack without a tag is 
 * passed-in to the constructor, one is created.  This is to prevent excess null checks.
 *
 * @author don_bruce
 */
public class WrapperNBT{
	protected final NBTTagCompound tag;
	
	//TODO this needs to be protected and get out of the main code.
	public WrapperNBT(){
		this.tag = new NBTTagCompound();
	}
	
	protected WrapperNBT(NBTTagCompound tag){
		this.tag = tag;
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
	
	
	//UUID
	public UUID getUUID(String name){
		return tag.hasKey(name) ? UUID.fromString(tag.getString(name)) : null;
	}
	
	public void setUUID(String name, UUID value){
		tag.setString(name, value.toString());
	}
	
	
	//ItemStacks
	public List<WrapperItemStack> getStacks(int count){
		List<WrapperItemStack> stacks = new ArrayList<WrapperItemStack>();
		NonNullList<ItemStack> mcList = NonNullList.<ItemStack>withSize(count, ItemStack.EMPTY);
		ItemStackHelper.loadAllItems(tag, mcList);
		for(ItemStack stack : mcList){
			stacks.add(new WrapperItemStack(stack));
		}
		return stacks;
	}
	
	public void setStacks(List<WrapperItemStack> stacks){
		NonNullList<ItemStack> mcList = NonNullList.create();
		for(WrapperItemStack stack : stacks){
			mcList.add(stack.stack);
		}
		ItemStackHelper.saveAllItems(tag, mcList);
	}
	
	
	//Point3d
	public Point3dPlus getPoint3d(String name){
		return new Point3dPlus(getDouble(name + "x"), getDouble(name + "y"), getDouble(name + "z"));
	}
	
	public void setPoint3d(String name, Point3dPlus value){
		setDouble(name + "x", value.x);
		setDouble(name + "y", value.y);
		setDouble(name + "z", value.z);
	}
	
	public List<Point3dPlus> getPoint3ds(String name){
		List<Point3dPlus> values = new ArrayList<Point3dPlus>();
		int count = getInteger(name + "count");
		for(int i=0; i<count; ++i){
			Point3dPlus point = getPoint3d(name + i);
			if(!point.isZero()){
				values.add(point);
			}
        }
		return values;
	}
	
	public void setPoint3ds(String name, Collection<Point3dPlus> values){
		setInteger(name + "count", values.size());
		int index = 0;
		for(Point3dPlus value : values){
			setPoint3d(name + index++, value);
		}
	}
	
	
	//Point3dcompact.  Casts-down doubles to ints for compact storage.
	public Point3dPlus getPoint3dCompact(String name){
		return new Point3dPlus(getInteger(name + "x"), getInteger(name + "y"), getInteger(name + "z"));
	}
	
	public void setPoint3dCompact(String name, Point3dPlus value){
		setInteger(name + "x", (int) Math.floor(value.x));
		setInteger(name + "y", (int) Math.floor(value.y));
		setInteger(name + "z", (int) Math.floor(value.z));
	}
	
	public List<Point3dPlus> getPoint3dsCompact(String name){
		List<Point3dPlus> values = new ArrayList<Point3dPlus>();
		int count = getInteger(name + "count");
		for(int i=0; i<count; ++i){
			Point3dPlus point = getPoint3dCompact(name + i);
			if(!point.isZero()){
				values.add(point);
			}
        }
		return values;
	}
	
	public void setPoint3dsCompact(String name, Collection<Point3dPlus> values){
		setInteger(name + "count", values.size());
		int index = 0;
		for(Point3dPlus value : values){
			setPoint3dCompact(name + index++, value);
		}
	}
	
	public WrapperNBT getData(String name){
		return tag.hasKey(name) ? new WrapperNBT(tag.getCompoundTag(name)) : null;
	}
	
	public WrapperNBT getDataOrNew(String name){
		return tag.hasKey(name) ? new WrapperNBT(tag.getCompoundTag(name)) : new WrapperNBT();
	}
	
	public void setData(String name, WrapperNBT value){
		tag.setTag(name, value.tag);
	}
	
	public void deleteData(String name){
		tag.removeTag(name);
	}
	
	public Set<String> getAllNames(){
		return tag.getKeySet();
	}
}
