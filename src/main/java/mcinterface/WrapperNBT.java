package mcinterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

/**Wrapper for interfacing with NBT data.  This pares down a few of the method to ones
 * more suited to what we use normally.  Of special importance is the ability to save
 * lists of data, which isn't normally allowed with NBT without loops.  Note that the
 * constructors that take MC classes are package-private.  This is to avoid accidental
 * use in other code sections.
 *
 * @author don_bruce
 */
public class WrapperNBT{
	final NBTTagCompound tag;
	
	public WrapperNBT(){
		this.tag = new NBTTagCompound();
	}
	
	public WrapperNBT(ByteBuf buf){
        PacketBuffer pb = new PacketBuffer(buf);
        try{
        	this.tag = pb.readCompoundTag();
        }catch (IOException e){
            // Unpossible? --- Says Forge comments, so who knows?
            throw new RuntimeException(e);
        }
    }

	WrapperNBT(NBTTagCompound tag){
		this.tag = tag;
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
	
	//Points.
	public Point3i getPoint3i(String name){
		return new Point3i(getInteger(name + "x"), getInteger(name + "y"), getInteger(name + "z"));
	}
	
	public void setPoint3i(String name, Point3i value){
		setInteger(name + "x", value.x);
		setInteger(name + "y", value.y);
		setInteger(name + "z", value.z);
	}
	
	public Point3d getPoint3d(String name){
		return new Point3d(getDouble(name + "x"), getDouble(name + "y"), getDouble(name + "z"));
	}
	
	public void setPoint3d(String name, Point3d value){
		setDouble(name + "x", value.x);
		setDouble(name + "y", value.y);
		setDouble(name + "z", value.z);
	}
	
	//Point array.
	public List<Point3i> getPoints(String name){
		List<Point3i> values = new ArrayList<Point3i>();
		int count = getInteger(name + "count");
		for(int i=0; i<count; ++i){
			Point3i point = new Point3i(getInteger(name + i + "x"), getInteger(name + i + "y"), getInteger(name + i + "z"));
			if(!point.isZero()){
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
	
	//NBT.
	public WrapperNBT getData(String name){
		return tag.hasKey(name) ? new WrapperNBT(tag.getCompoundTag(name)) : null;
	}
	
	public void setData(String name, WrapperNBT value){
		tag.setTag(name, value.tag);
	}

	//Packet handling.
	public void writeToBuffer(ByteBuf to){
        PacketBuffer pb = new PacketBuffer(to);
        pb.writeCompoundTag(tag);
    }
}
