package minecrafttransportsimulator.items.packs;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockDecor;
import minecrafttransportsimulator.items.core.IItemBlock;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemDecor extends AItemPack<JSONDecor> implements IItemBlock{
	
	public ItemDecor(JSONDecor definition){
		super(definition);
	}
	
	@Override
	public ABlockBase createBlock(){
		return new BlockDecor(definition);
	}
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			if(new WrapperWorld(world).setBlock(getBlock(), new Point3i(pos.getX(), pos.getY(), pos.getZ()), new WrapperPlayer(player))){
				return EnumActionResult.SUCCESS;
			}
		}
		return EnumActionResult.FAIL;
	}
}
