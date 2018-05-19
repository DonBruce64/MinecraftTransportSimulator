package minecrafttransportsimulator.items.parts;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class AItemPartEngine extends AItemPart{
	
	public AItemPartEngine(String partName){
		super(partName);
		this.hasSubtypes = true;
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems){
		super.getSubItems(item, tab, subItems);
		ItemStack engineStack = new ItemStack(item);
		NBTTagCompound stackTag = new NBTTagCompound();
		stackTag.setBoolean("isCreative", true);
		engineStack.setTagCompound(stackTag);
		subItems.add(engineStack);
    }
}
