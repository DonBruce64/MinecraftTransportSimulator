package minecrafttransportsimulator.items;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.dataclasses.MTSCreativeTabs;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.parts.EntityPropeller;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPropeller extends ItemPart{
	
	public ItemPropeller(){
		super(EntityPropeller.class);
		this.hasSubtypes=true;
		this.setMaxStackSize(1);
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack){
		switch (stack.getItemDamage()){
			case 0: return this.getUnlocalizedName() + "_wood";
			case 1: return this.getUnlocalizedName() + "_iron";
			case 2: return this.getUnlocalizedName() + "_obsidian";
			default: return this.getUnlocalizedName() + "_invalid";
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_){
		NBTTagCompound stackTag = stack.getTagCompound();
		tooltipLines.add(I18n.format("info.item.propeller.numberBlades") + stackTag.getInteger("numberBlades"));
		tooltipLines.add(I18n.format("info.item.propeller.pitch") + stackTag.getInteger("pitch"));
		tooltipLines.add(I18n.format("info.item.propeller.diameter") + stackTag.getInteger("diameter"));
		tooltipLines.add(I18n.format("info.item.propeller.maxrpm") + Math.round(60*340.29/(0.0254*Math.PI*stackTag.getInteger("diameter"))));
		tooltipLines.add(I18n.format("info.item.propeller.health") + stackTag.getFloat("health"));
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems){
		if(MTSCreativeTabs.tabMTS.equals(tab)){
			List<Byte[]> propellerList = new ArrayList<Byte[]>();
			propellerList.add(new Byte[]{0, 2, 70, 75});
			propellerList.add(new Byte[]{0, 3, 70, 75});
			propellerList.add(new Byte[]{0, 4, 70, 75});
			propellerList.add(new Byte[]{1, 2, 70, 75});
			propellerList.add(new Byte[]{1, 3, 70, 75});
			propellerList.add(new Byte[]{1, 4, 70, 75});
			propellerList.add(new Byte[]{1, 2, 70, 115});
			propellerList.add(new Byte[]{2, 2, 70, 75});
			propellerList.add(new Byte[]{2, 3, 70, 75});
			propellerList.add(new Byte[]{2, 4, 70, 75});
			propellerList.add(new Byte[]{2, 2, 70, 115});
			propellerList.add(new Byte[]{2, 3, 70, 115});
			propellerList.add(new Byte[]{2, 4, 70, 115});
			
			for(Byte[] propellerProperties : propellerList){
				ItemStack propellerStack = new ItemStack(MTSRegistry.propeller, 1, propellerProperties[0]);
				NBTTagCompound stackTag = new NBTTagCompound();
				stackTag.setByte("type", propellerProperties[0]);
				stackTag.setInteger("numberBlades", propellerProperties[1]);
				stackTag.setInteger("pitch", propellerProperties[2]);
				stackTag.setInteger("diameter", propellerProperties[3]);
				if(propellerProperties[0]==1){
					stackTag.setFloat("health", 500);
				}else if(propellerProperties[0]==2){
					stackTag.setFloat("health", 1000);
				}else{
					stackTag.setFloat("health", 100);
				}
				propellerStack.setTagCompound(stackTag);
				subItems.add(propellerStack);
			}
		}
    }
}
