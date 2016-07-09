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
	
	public ItemEngine(EngineTypes type){
		this.hasSubtypes=true;
		this.setMaxStackSize(1);
		this.type = (byte) type.ordinal();
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
		for(int subtype : EngineTypes.values()[((ItemEngine) item).type].getSubtypes()){
			itemList.add(new ItemStack(item, 1, subtype));
		}
		return;
    }
	
	public enum EngineTypes{
		SMALL((short) 2805, (short) 3007), 
		LARGE((short) 2907, (short) 3210);
		
		private short[] subtypes;
		private EngineTypes(short... subtypes){
			this.subtypes = subtypes;
		}
		
		public short[] getSubtypes(){
			return subtypes;
		}
	}
}
