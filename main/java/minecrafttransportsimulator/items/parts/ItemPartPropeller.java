package minecrafttransportsimulator.items.parts;

import java.util.List;

import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPartPropeller extends AItemPart{
	
	public ItemPartPropeller(String partName){
		super(partName);
	}
	
	@Override
	public float getPartValue(){
		return PackParserSystem.getPartPack(partName).propeller.diameter;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_){
		PackPartObject pack = PackParserSystem.getPartPack(((ItemPartPropeller) stack.getItem()).partName); 
		NBTTagCompound stackTag = stack.getTagCompound();
		tooltipLines.add(I18n.format("info.item.propeller.numberBlades") + pack.propeller.numberBlades);
		tooltipLines.add(I18n.format("info.item.propeller.pitch") + pack.propeller.pitch);
		tooltipLines.add(I18n.format("info.item.propeller.diameter") + pack.propeller.diameter);
		tooltipLines.add(I18n.format("info.item.propeller.numberBlades") + pack.propeller.numberBlades);
		tooltipLines.add(I18n.format("info.item.propeller.maxrpm") + Math.round(60*340.29/(0.0254*Math.PI*pack.propeller.diameter)));
		tooltipLines.add(I18n.format("info.item.propeller.health") + stackTag.getFloat("health"));
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems){
		ItemStack propellerStack = new ItemStack(item);
		NBTTagCompound stackTag = new NBTTagCompound();
		stackTag.setFloat("health", PackParserSystem.getPartPack(((ItemPartPropeller) item).partName).propeller.startingHealth);
		propellerStack.setTagCompound(stackTag);
		subItems.add(propellerStack);
    }
}
