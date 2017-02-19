package minecraftflightsimulator.items;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.minecrafthelpers.ItemStackHelper;
import minecraftflightsimulator.minecrafthelpers.PlayerHelper;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;

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
		list.add(PlayerHelper.getTranslatedText("info.item.propeller.model") + stackTag.getInteger("model"));
		list.add(PlayerHelper.getTranslatedText("info.item.propeller.numberBlades") + stackTag.getInteger("numberBlades"));
		list.add(PlayerHelper.getTranslatedText("info.item.propeller.pitch") + stackTag.getInteger("pitch"));
		list.add(PlayerHelper.getTranslatedText("info.item.propeller.diameter") + stackTag.getInteger("diameter"));
		list.add(PlayerHelper.getTranslatedText("info.item.propeller.health") + stackTag.getFloat("health"));
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
		int[] modelList = new int[]{1520, 1530, 1540, 1521, 1531, 1541, 9521, 1522, 1532, 1542, 9522, 9532, 9542};
		for(int model : modelList){
			ItemStack propellerStack = new ItemStack(MFSRegistry.propeller, 1, model%10);
			NBTTagCompound stackTag = new NBTTagCompound();
			stackTag.setInteger("model", model);
			stackTag.setInteger("numberBlades", model%100/10);
			stackTag.setInteger("pitch", 55+3*(model%1000/100));
			stackTag.setInteger("diameter", 70+5*(model/1000));
			if(model%10==1){
				stackTag.setFloat("health", 500);
			}else if(model%10==2){
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
    		icons[i] = register.registerIcon(MFS.MODID + ":" + this.getClass().getSimpleName().substring(4).toLowerCase() + i);
    	}
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage){
        return this.icons[damage%10 > 2 ? 0 : damage%10];
    }
    //DEL180END
}
