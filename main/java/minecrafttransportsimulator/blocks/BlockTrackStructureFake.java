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

public class BlockTrackStructureFake extends Block{
	public static final PropertyInteger height = PropertyInteger.create("height", 0, 15);
	private static final AxisAlignedBB heightBoxes[] = initHeightBoxes();

	private static boolean shouldTryToBreakTrackWhenBroken = true;
	private static int breakageRecusionDepth = 0;
	private static BlockPos firstBrokenBlockPos;
	private static List<BlockPos> blockCheckPos = new ArrayList<BlockPos>();

	public BlockTrackStructureFake(){
		super(Material.IRON);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
		this.setDefaultState(this.blockState.getBaseState().withProperty(height, 15));
		this.fullBlock = false;
	}
	
	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state){
		if(shouldTryToBreakTrackWhenBroken){
			if(breakageRecusionDepth == 0){
				firstBrokenBlockPos = pos;
			}
			++breakageRecusionDepth;
			//Add current block to list.
			blockCheckPos.add(pos);
			for(EnumFacing searchOffset : EnumFacing.VALUES){
				boolean skipCheck = false;
				if(blockCheckPos.contains(pos.offset(searchOffset))){
					//Block already checked.
					skipCheck = true;
					continue;
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
							//Just because the TE is the parent for a fake block, doesn't me it's the parent for the FIRST broken block.
							//We could have searched the wrong way and missed the real TE.
							if(((TileEntityTrack) world.getTileEntity(pos.offset(searchOffset))).getFakeTracks().contains(firstBrokenBlockPos)){
								//Track TE contains the first broken fake track.  Set master track block to air and hand off breaking.
								world.setBlockToAir(pos.offset(searchOffset));
								blockCheckPos.clear();
								firstBrokenBlockPos = null;
								break;
							}
						}
					}else if(world.getBlockState(pos.offset(searchOffset)).getBlock() instanceof BlockTrackStructureFake){
						//Found another fake track.  Call it's break code to pass on the message.
						this.breakBlock(world, pos.offset(searchOffset), state);
					}
				}
			}
			--breakageRecusionDepth;
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
	
	//Depreciated, but correct so say master modders.
	@Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta){
        return this.getDefaultState().withProperty(height, meta);
    }
	
    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state){
        return false;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state){
        return false;
    }

	@Override
	@SuppressWarnings("deprecation")
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos){
		return heightBoxes[state.getValue(height)];
	}
	
	public static void enableMainTrackBreakage(){
		shouldTryToBreakTrackWhenBroken = true;
	}
	
	public static void disableMainTrackBreakage(){
		shouldTryToBreakTrackWhenBroken = false;
	}
	
	public static BlockPos getLastHitFakeTrack(){
		return firstBrokenBlockPos;
	}
	
	private static AxisAlignedBB[] initHeightBoxes(){
		AxisAlignedBB[] heightBoxes = new AxisAlignedBB[16];
		for(byte i=0; i<16; ++i){
			heightBoxes[i] = new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, (i+1)/16F, 1.0F);
		}
		return heightBoxes;
	}
}
