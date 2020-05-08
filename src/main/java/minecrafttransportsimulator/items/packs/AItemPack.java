package minecrafttransportsimulator.items.packs;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.items.core.IItemBlock;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**Base item class for all pack-created items.  Stores information such as the
 * pack the item belongs to and the class that extends {@link AJSONItem} that
 * is the instance of the item's pack.  It will also attempt to place blocks
 * from this item should the pack item implement {@link IItemBlock}.
 * 
 * @author don_bruce
 */
public abstract class AItemPack<JSONDefinition extends AJSONItem<? extends AJSONItem<?>.General>> extends Item{
	public final JSONDefinition definition;
	
	public AItemPack(JSONDefinition definition){
		super();
		this.definition = definition;
	}
	
	@Override
	public String getItemStackDisplayName(ItemStack stack){
        return definition.general.name != null ? definition.general.name : definition.systemName;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		if(definition.general.description != null){
			for(String tooltipLine : definition.general.description.split("\n")){
				tooltipLines.add(tooltipLine);
			}
		}
	}
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		//If we are a type of pack item that can place blocks, try to do so now.
		if(this instanceof IItemBlock){
			return ((IItemBlock) this).placeBlock(new WrapperWorld(world), new WrapperPlayer(player), new Point3i(pos.getX(), pos.getY(), pos.getZ()), Axis.valueOf(facing.name())) ? EnumActionResult.SUCCESS : EnumActionResult.FAIL;
		}else{
			return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
		}
	}
	
	/**
	 *  Returns the location of the OBJ model for this item, or null if we use the default item rendering system.
	 */
	public abstract String getModelLocation();
	
	/**
	 *  Returns the location of the texture for this item, or null if we use the default item rendering system.
	 */
	public abstract String getTextureLocation();
}
