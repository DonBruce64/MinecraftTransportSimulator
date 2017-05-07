package minecrafttransportsimulator.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockTrackFake extends Block{
	public static final PropertyInteger height = PropertyInteger.create("height", 0, 15);
	private static final AxisAlignedBB heightBoxes[] = initHeightBoxes();
	
	public static boolean overrideBreakingBlocks = false;
	private static List<BlockPos> blockCheckPos = new ArrayList<BlockPos>();

	public BlockTrackFake(){
		super(Material.IRON);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
		this.setDefaultState(this.blockState.getBaseState().withProperty(height, 15));
	}
	
	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state){
		if(!overrideBreakingBlocks){
			//Add current block to list.
			blockCheckPos.add(pos);
			for(EnumFacing searchOffset : EnumFacing.VALUES){
				boolean skipCheck = false;
				if(blockCheckPos.contains(pos.offset(searchOffset))){
					//Block already checked.
					skipCheck = true;
					break;
				}else{
					for(BlockPos testPos : blockCheckPos){
						if(Math.sqrt(pos.offset(searchOffset).distanceSq(testPos)) > 150){
							//Block is too far to be a possible match.
							skipCheck = true;
							break;
						}
					}
					
				}
				if(!skipCheck){
					if(world.getTileEntity(pos.offset(searchOffset)) instanceof TileEntityTrack){
						//Found a track TE.  See if it's the parent for this fake track block.
						if(((TileEntityTrack) world.getTileEntity(pos.offset(searchOffset))).getFakeTracks().contains(pos)){
							//Track TE contains this fake track.  Set master track block to air and hand off breaking.
							overrideBreakingBlocks = true;
							world.setBlockToAir(pos.offset(searchOffset));
							blockCheckPos.clear();
							return;
						}
					}else if(world.getBlockState(pos.offset(searchOffset)).getBlock() instanceof BlockTrackFake){
						//Found another fake track.  Call it's break code to pass on the message.
						this.breakBlock(world, pos.offset(searchOffset), state);
					}
				}
			}
		}
		super.breakBlock(world, pos, state);
	}

	@Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune){
        return null;
    }
	
	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player){
		 return new ItemStack(MTSRegistry.track);
    }
	
	@Override
	protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, height);
    }
	
	@Override
	public int getMetaFromState(IBlockState state){
		return state.getValue(height);
	}
	
	//Depreciate, but correct so say master modders.
    @Deprecated
    public IBlockState getStateFromMeta(int meta){
        return this.getDefaultState().withProperty(height, meta);
    }

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos){
		return heightBoxes[state.getValue(height)];
	}
	
	private static AxisAlignedBB[] initHeightBoxes(){
		AxisAlignedBB[] heightBoxes = new AxisAlignedBB[16];
		for(byte i=0; i<16; ++i){
			heightBoxes[i] = new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, i/16F, 1.0F);
		}
		return heightBoxes;
	}
}
