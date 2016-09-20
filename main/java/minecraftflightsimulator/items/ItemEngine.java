package minecraftflightsimulator.items;

import java.util.List;

import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.entities.parts.EntityEngine.EngineTypes;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemEngine extends Item{
	public final EngineTypes type; 
	
	public ItemEngine(EngineTypes type){
		this.hasSubtypes=true;
		this.setMaxStackSize(1);
		this.type = type;
	}
	
	private static Item getStaticItemForType(EngineTypes type){
		if(type.equals(EngineTypes.PLANE_SMALL)){
			return MFSRegistry.engineSmall; 
		}else if(type.equals(EngineTypes.PLANE_LARGE)){
			return MFSRegistry.engineLarge; 
		}else{
			return null;
		}
	}
	
	public static ItemStack createStack(EngineTypes type, int propertyCode, double hours){
		ItemStack engineStack = new ItemStack(getStaticItemForType(type), 1, propertyCode);
		NBTTagCompound tag = new NBTTagCompound();
		tag.setDouble("hours", hours);
		engineStack.setTagCompound(tag);
		return engineStack;
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack){
		return "item.engine_" + ((ItemEngine) stack.getItem()).type.name().toLowerCase();
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
		for(int subtype : type.getSubtypes()){
			itemList.add(new ItemStack(item, 1, subtype));
		}
		return;
    }
}
