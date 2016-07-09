package minecraftflightsimulator.items;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemEngine extends Item{
	private final byte type; 
	
	public ItemEngine(EngineType type){
		this.hasSubtypes=true;
		this.setMaxStackSize(1);
		this.type = type.getTypeID();
	}
	
	public static ItemStack createStack(Item engine, int propertyCode, double hours){
		ItemStack engineStack = new ItemStack(engine, 1, propertyCode);
		NBTTagCompound tag = new NBTTagCompound();
		tag.setDouble("hours", hours);
		engineStack.setTagCompound(tag);
		return engineStack;
	}
	
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean p_77624_4_){
		list.add("Model# " + stack.getItemDamage());
		int maxRPM = (stack.getItemDamage()/((int) 100))*100;
		list.add("Max possible RPM: " + maxRPM);
		list.add("Max safe RPM: " + (maxRPM - (maxRPM - 2500)/2));
		list.add("Fuel consumption: " + (stack.getItemDamage()%100)/10F);
		if(stack.hasTagCompound()){
			list.add("Hours: " + Math.round(stack.getTagCompound().getDouble("hours")*100D)/100D);
		}else{
			list.add("Hours: 0");
		}
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
		for(EngineType type : EngineType.values()){
			if(type.getTypeID() == ((ItemEngine) item).type){
				for(int subtype : type.getSubtypes()){
					itemList.add(new ItemStack(item, 1, subtype));
				}
				return;
			}
		}
    }
	
	public enum EngineType{
		SMALL((byte) 0, (short) 2805, (short) 3007), 
		LARGE((byte) 1, (short) 2907, (short) 3210);
		
		private byte type;
		private short[] subtypes;
		private EngineType(byte type, short... subtypes){
			this.type = type;
			this.subtypes = subtypes;
		}
		public byte getTypeID(){
			return this.type;
		}
		
		public short[] getSubtypes(){
			return subtypes;
		}
	}
}
