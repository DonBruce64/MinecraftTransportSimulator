package minecrafttransportsimulator.items.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class AItemPart extends Item{
	public final String partName;
	
	public AItemPart(String partName){
		super();
		this.partName = partName;
		this.setUnlocalizedName(partName.replace(":", "."));
		this.setCreativeTab(MTSRegistry.packTabs.get(partName.substring(0, partName.indexOf(':'))));
	}
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		return EnumActionResult.SUCCESS;
	}
	
	public boolean isPartValidForPackDef(PackPart packPart){
		PackPartObject itemPack = PackParserSystem.getPartPack(partName);
		if(packPart.customTypes == null){
			return itemPack.general.customType == null;
		}else if(itemPack.general.customType == null){
			return packPart.customTypes == null;
		}else{
			return packPart.customTypes.contains(itemPack.general.customType);
		}
	}
}
