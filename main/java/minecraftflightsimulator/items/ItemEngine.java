package minecraftflightsimulator.items;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.dataclasses.MTSRegistry;
import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntityEngine.EngineTypes;
import minecraftflightsimulator.entities.parts.EntityEngine.EngineTypes.EngineProperties;
import minecraftflightsimulator.minecrafthelpers.ItemStackHelper;
import minecraftflightsimulator.minecrafthelpers.PlayerHelper;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;

public class ItemEngine extends Item{
	private IIcon[] icons = new IIcon[EngineTypes.values().length];
	
	public ItemEngine(){
		this.hasSubtypes=true;
		this.setMaxStackSize(1);
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack){
		return "item." + this.getClass().getSimpleName().substring(4).toLowerCase() + ItemStackHelper.getItemDamage(stack);
	}
	
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean p_77624_4_){
		NBTTagCompound stackTag = ItemStackHelper.getStackNBT(stack);
		if(stackTag.getFloat("fuelConsumption") == 0){
			list.add(EnumChatFormatting.DARK_PURPLE + PlayerHelper.getTranslatedText("info.item.engine.creative"));
		}
		list.add(PlayerHelper.getTranslatedText("info.item.engine.maxrpm") + stackTag.getInteger("maxRPM"));
		list.add(PlayerHelper.getTranslatedText("info.item.engine.maxsaferpm") + stackTag.getInteger("maxSafeRPM"));
		list.add(PlayerHelper.getTranslatedText("info.item.engine.fuelconsumption") + stackTag.getFloat("fuelConsumption"));
		list.add(PlayerHelper.getTranslatedText("info.item.engine.hours") + Math.round(stackTag.getDouble("hours")*100D)/100D);
		if(stackTag.getBoolean("oilLeak")){
			list.add(EnumChatFormatting.RED + PlayerHelper.getTranslatedText("info.item.engine.oilleak"));
		}
		if(stackTag.getBoolean("fuelLeak")){
			list.add(EnumChatFormatting.RED + PlayerHelper.getTranslatedText("info.item.engine.fuelleak"));
		}
		if(stackTag.getBoolean("brokenStarter")){
			list.add(EnumChatFormatting.RED + PlayerHelper.getTranslatedText("info.item.engine.brokenstarter"));
		}
	}
	
	public static ItemStack getItemStackForProperties(EngineTypes type, EngineProperties properties){
		ItemStack engineStack = new ItemStack(MTSRegistry.engine, 1, type.ordinal());
		NBTTagCompound stackTag = new NBTTagCompound();
		stackTag.setInteger("maxRPM", properties.maxRPM);
		stackTag.setInteger("maxSafeRPM", EntityEngine.getMaxSafeRPM(properties.maxRPM));
		stackTag.setFloat("fuelConsumption", properties.fuelConsumption);
		stackTag.setDouble("hours", 0);
		ItemStackHelper.setStackNBT(engineStack, stackTag);
		return engineStack;
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
		for(EngineTypes type : EngineTypes.values()){
			EngineProperties properties = null;
			for(byte i=0; i<type.defaultSubtypes.length; ++i){
				properties = type.defaultSubtypes[i];
				itemList.add(getItemStackForProperties(type, properties));
			}
			itemList.add(getItemStackForProperties(type, new EngineProperties(properties.maxRPM, 0.0F)));
		}
    }
	//DEL180START
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register){
    	for(int i=0; i<EngineTypes.values().length; ++i){
    		icons[i] = register.registerIcon(MFS.MODID + ":" + this.getClass().getSimpleName().substring(4).toLowerCase() + i);
    	}
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage){
        return this.icons[damage >= EngineTypes.values().length ? 0 : damage];
    }
    //DEL180END
}
