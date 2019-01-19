package minecrafttransportsimulator.items.parts;

import java.util.List;

import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.systems.PackParserSystem;
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
	public boolean isPartValidForPackDef(PackPart packPart){
		float fuelConsumption = PackParserSystem.getPartPack(partName).engine.fuelConsumption;
		return packPart.minValue <= fuelConsumption && packPart.maxValue >= fuelConsumption ? super.isPartValidForPackDef(packPart) : false;
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems){
		if(this.getCreativeTab().equals(tab)){
			subItems.add(new ItemStack(this));
			
			ItemStack engineStackCreative = new ItemStack(this);
			NBTTagCompound stackTag = new NBTTagCompound();
			stackTag.setBoolean("isCreative", true);
			engineStackCreative.setTagCompound(stackTag);
			subItems.add(engineStackCreative);
		}
    }
}
