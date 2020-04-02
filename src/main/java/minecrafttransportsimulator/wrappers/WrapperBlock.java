package minecrafttransportsimulator.wrappers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**Wrapper for the MC Block class.  This class assumes the block will not be a solid
 * block (so no culling) and may have alpha channels in the texture (like glass).
 * It also assumes the block can be rotated, and saves the rotation with whatever
 * version-specific rotation scheme the current MC version uses.
 *
 * @author don_bruce
 */
public class WrapperBlock extends Block{
	protected static final PropertyDirection FACING = BlockHorizontal.FACING;
	private static final List<BoundingBox> collisionBoxes = new ArrayList<BoundingBox>();
	final ABlockBase block;
	
    public WrapperBlock(ABlockBase block){
		super(Material.WOOD);
		this.block = block;
		setHardness(block.hardness);
		setResistance(block.blastResistance);
		fullBlock = false;
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.SOUTH));
		//FIXME add some way to do creative tabs assignments.
	}
    
    /**
	 *  Returns the rotation of the block at the passed-in location.
	 */
    public float getRotation(WrapperWorld world, Point3i point){
    	return world.world.getBlockState(new BlockPos(point.x, point.y, point.z)).getValue(FACING).getHorizontalAngle();
    }
    
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack stack){
    	//Forward place event to the block if a player placed this block.
    	if(entity instanceof EntityPlayer){
    		block.onPlaced(new WrapperWorld(world), new Point3i(pos.getX(), pos.getY(), pos.getZ()), new WrapperPlayer((EntityPlayer) entity));
    	}
    }
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ){
		//Forward this click to the block.
		return block.onClicked(new WrapperWorld(world), new Point3i(pos.getX(), pos.getY(), pos.getZ()), new WrapperPlayer(player));
	}
    
    @Override
	@SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean p_185477_7_){
    	//Gets the collision boxes. We forward this call to the block to handle.
    	//We add-on 0.5D to offset the box to the correct location.
    	block.addCollisionBoxes(new WrapperWorld(world), new Point3i(pos.getX(), pos.getY(), pos.getZ()), collisionBoxes);
    	for(BoundingBox box : collisionBoxes){
    		collidingBoxes.add(new AxisAlignedBB(
				pos.getX() + 0.5D + box.x - box.widthRadius, 
				pos.getY() + 0.5D + box.y - box.heightRadius, 
				pos.getZ() + 0.5D + box.z - box.depthRadius, 
				pos.getX() + 0.5D + box.x + box.widthRadius, 
				pos.getY() + 0.5D + box.y + box.heightRadius, 
				pos.getZ() + 0.5D + box.z + box.depthRadius)
			);
    	}
    }
    
    @Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player){
		//Returns the ItemStack that gets put in the player's inventory when they middle-click this block.
    	return block.getStack(new WrapperWorld(world), new Point3i(pos.getX(), pos.getY(), pos.getZ()));
    }
    
    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack){
    	super.harvestBlock(worldIn, player, pos, state, te, stack);
    	//FIXME see if we need this for the dropping of signs with their NBT attached.
    	//TileEntityDecor decor = (TileEntityDecor) te;
    	//spawnAsEntity(worldIn, pos, new ItemStack(MTSRegistry.packItemMap.get(decor.definition.packID).get(decor.definition.systemName)));
    }
    
	@Override
	@SuppressWarnings("deprecation")
	public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer){
		//Sets the blockstate to the correct state to save rotation data.
		return super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer).withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }
	
	@Override
	@SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta){
		//Restores the sate from meta.
        return this.getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state){
    	//Saves the state as metadata.
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rot){
    	//Returns the state with the applied rotation.
        return state.getBlock() != this ? state : state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockStateContainer createBlockState(){
    	//Creates a new, default, blockstate holder.
        return new BlockStateContainer(this, new IProperty[] {FACING});
    }
	
    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state){
    	//If this is opaque, we block light.
        return false;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state){
    	//If this is a full cube, we do culling on faces.
        return false;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face){
    	//If this is SOLID, we can attach things to this block (e.g. torches)
        return BlockFaceShape.UNDEFINED;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public EnumBlockRenderType getRenderType(IBlockState state){
    	//Handles if we render a block model or not.
        return block.renderBlockModel() ? EnumBlockRenderType.MODEL : EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }
    
  	@Override
  	public BlockRenderLayer getBlockLayer(){
  		//Gets the block layer.  Needs to be CUTOUT so textures with alpha in them render.
  		return BlockRenderLayer.CUTOUT;
  	}
}
