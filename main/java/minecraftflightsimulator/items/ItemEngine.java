package minecraftflightsimulator.items;

import java.util.List;

import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntityEngine.EngineTypes;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemEngine extends Item implements IItemNBT{
	public final EngineTypes type; 
	
	public ItemEngine(EngineTypes type){
		this.hasSubtypes=true;
		this.setMaxStackSize(1);
		this.type = type;
	}
	
	private static Item getStaticItemForType(EngineTypes type){
		switch (type){
			case PLANE_SMALL: return MFSRegistry.engineSmall;
			case PLANE_LARGE: return MFSRegistry.engineLarge;
		}
		System.err.println("AN ENGINE WITHOUT A TYPE IS BEING ACCESSED.  THINGS MAY GO BADLY.");
		return null;
	}
	
	public ItemStack createStackFromEntity(EntityChild entity){
		EntityEngine engine = (EntityEngine) entity;
		ItemStack engineStack = new ItemStack(getStaticItemForType(engine.type), 1, engine.propertyCode);
		NBTTagCompound tag = new NBTTagCompound();
		tag.setDouble("hours", engine.hours);
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
