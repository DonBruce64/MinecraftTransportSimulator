package minecrafttransportsimulator.items;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.minecrafthelpers.ItemStackHelper;
import minecrafttransportsimulator.minecrafthelpers.PlayerHelper;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPropeller extends Item{
	private IIcon[] icons = new IIcon[3];

	public ItemPropeller(){
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
		list.add(PlayerHelper.getTranslatedText("info.item.propeller.numberBlades") + stackTag.getInteger("numberBlades"));
		list.add(PlayerHelper.getTranslatedText("info.item.propeller.pitch") + stackTag.getInteger("pitch"));
		list.add(PlayerHelper.getTranslatedText("info.item.propeller.diameter") + stackTag.getInteger("diameter"));
		list.add(PlayerHelper.getTranslatedText("info.item.propeller.maxrpm") + Math.round(60*340.29/(0.0254*Math.PI*stackTag.getInteger("diameter"))));
		list.add(PlayerHelper.getTranslatedText("info.item.propeller.health") + stackTag.getFloat("health"));
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
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
			ItemStackHelper.setStackNBT(propellerStack, stackTag);
			itemList.add(propellerStack);
		}
    }
	//DEL180START
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register){
    	for(int i=0; i<3; ++i){
    		icons[i] = register.registerIcon(MTS.MODID + ":" + this.getClass().getSimpleName().substring(4).toLowerCase() + i);
    	}
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage){
        return this.icons[damage%10 > 2 ? 0 : damage%10];
    }
    //DEL180END
}
