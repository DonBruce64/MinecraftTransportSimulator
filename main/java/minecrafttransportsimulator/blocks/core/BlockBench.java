package minecrafttransportsimulator.blocks.core;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.items.blocks.ItemBlockRotatable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockBench extends BlockRotatable{
	public final List<String> partTypes;
	
	public BlockBench(EnumFacing orientation, ItemBlockRotatable item, String... partTypes){
		super(orientation, item);
		this.partTypes = Arrays.asList(partTypes);
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ){
		if(Math.sqrt(player.getDistanceSq(pos)) < 5){
			if(world.isRemote){
				MTS.proxy.openGUI(this, player);
			}
		}
		return true;
	}
}
