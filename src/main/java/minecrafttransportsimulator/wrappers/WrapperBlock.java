package minecrafttransportsimulator.wrappers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.instances.BlockDecor;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityFluidTank;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityTickable;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.core.IItemBlock;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONDecor;
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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**Wrapper for the MC Block class.  This class assumes the block will not be a solid
 * block (so no culling) and may have alpha channels in the texture (like glass).
 * It also assumes the block can be rotated, and saves the rotation with whatever
 * version-specific rotation scheme the current MC version uses.
 *
 * @author don_bruce
 */
@EventBusSubscriber
public class WrapperBlock extends Block{
	protected static final PropertyDirection FACING = BlockHorizontal.FACING;
	final ABlockBase block;
	
	static final Map<ABlockBase, WrapperBlock> blockWrapperMap = new HashMap<ABlockBase, WrapperBlock>();
	static final Map<String, IBlockTileEntity> tileEntityMap = new HashMap<String, IBlockTileEntity>();
	
    public WrapperBlock(ABlockBase block){
		super(Material.WOOD);
		this.block = block;
		setHardness(block.hardness);
		setResistance(block.blastResistance);
		fullBlock = false;
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.SOUTH));
	}
    
    /**
	 *  Returns the rotation of the block at the passed-in location.
	 *  Block-insensitive, but assumes block is instance of this wrapper.
	 */
    public static float getRotation(WrapperWorld world, Point3i point){
    	return world.world.getBlockState(new BlockPos(point.x, point.y, point.z)).getValue(FACING).getHorizontalAngle();
    }
    
    @Override
    public boolean hasTileEntity(IBlockState state){
    	//If our block implements the interface to be a TE, we return true.
        return block instanceof IBlockTileEntity;
    }
    
    @Nullable
    public TileEntity createTileEntity(World world, IBlockState state){
    	//Need to return a wrapper class here, not the actual TE.
        ATileEntityBase tile = ((IBlockTileEntity) block).createTileEntity();
        if(tile instanceof ATileEntityFluidTank){
        	return new WrapperFluidTank((ATileEntityFluidTank) tile);
        }else if(tile instanceof ATileEntityTickable){
        	return new WrapperTileEntityTickable((ATileEntityTickable) tile);
        }else{
        	return new WrapperTileEntity(tile);
        }
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
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player){
		//Returns the ItemStack that gets put in the player's inventory when they middle-click this block.
    	//This calls down into getItem, which then uses the Item class's block<->item mapping to get a block.
    	//By overriding here, we intercept those calls and return our own.  This also allows us to put NBT
    	//data on the stack based on the TE state.
    	
    	//Note that this method is only used for middle-clicking and nothing else.  Failure to return valid results
    	//here will result in air being grabbed, and no WAILA support.
    	if(block instanceof BlockDecor){
    		JSONDecor definition = ((BlockDecor) block).definition;
    		ItemStack stack = new ItemStack(MTSRegistry.packItemMap.get(definition.packID).get(definition.systemName));
    		WrapperNBT data = new WrapperNBT(new NBTTagCompound());
    		((WrapperTileEntity) world.getTileEntity(pos)).tileEntity.save(data);
    		stack.setTagCompound(data.tag);
        	return stack;
    	}else{
    		ItemStack stack = super.getPickBlock(state, target, world, pos, player);
        	if(block instanceof IBlockTileEntity){
        		WrapperNBT data = new WrapperNBT(new NBTTagCompound());
        		((WrapperTileEntity) world.getTileEntity(pos)).tileEntity.save(data);
        		stack.setTagCompound(data.tag);
        	}
        	return stack;
    	}
    }
    
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune){
    	//This gets called to drop the block as an item.  We override it here to return the correct item.
    	//This is called from dropBlockAsItemWithChance, which then forwards the call to this method to get the actual drops.
    	//dropBlockAsItemWithChance is called from harvestBlock to actually harvest the block.
    	//Harvesting is done by the server when the player (who is not in creative) breaks (harvests) this block.
    	//Said drops are then spawned in the world.  We return those drops here with TE data if applicable.
    	if(block instanceof BlockDecor){
    		JSONDecor definition = ((BlockDecor) block).definition;
    		ItemStack stack = new ItemStack(MTSRegistry.packItemMap.get(definition.packID).get(definition.systemName));
    		WrapperNBT data = new WrapperNBT(new NBTTagCompound());
    		((WrapperTileEntity) world.getTileEntity(pos)).tileEntity.save(data);
    		stack.setTagCompound(data.tag);
    		drops.add(stack);
    	}else{
    		super.getDrops(drops, world, pos, state, fortune);
        	if(block instanceof IBlockTileEntity){
        		WrapperNBT data = new WrapperNBT(new NBTTagCompound());
        		((WrapperTileEntity) world.getTileEntity(pos)).tileEntity.save(data);
        		//Will only be one drop for our block.
        		drops.get(0).setTagCompound(data.tag);
        	}
    	}
    }
    
    @Override
	@SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean p_185477_7_){
    	//Gets the collision boxes. We forward this call to the block to handle.
    	//We add-on 0.5D to offset the box to the correct location.
    	List<BoundingBox> collisionBoxes = new ArrayList<BoundingBox>();
    	block.addCollisionBoxes(new WrapperWorld(world), new Point3i(pos.getX(), pos.getY(), pos.getZ()), collisionBoxes);
    	for(BoundingBox box : collisionBoxes){
    		AxisAlignedBB mcBox = new AxisAlignedBB(
				pos.getX() + 0.5D + box.x - box.widthRadius, 
				pos.getY() + 0.5D + box.y - box.heightRadius, 
				pos.getZ() + 0.5D + box.z - box.depthRadius, 
				pos.getX() + 0.5D + box.x + box.widthRadius, 
				pos.getY() + 0.5D + box.y + box.heightRadius, 
				pos.getZ() + 0.5D + box.z + box.depthRadius);
			if(mcBox.intersects(entityBox)){
				collidingBoxes.add(mcBox);
			}
    	}
    }
    
	@Override
	public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand){
		//Sets the blockstate to the correct state to save rotation data.
		return super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer, hand).withProperty(FACING, placer.getHorizontalFacing().getOpposite());
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
        return block instanceof IBlockTileEntity && !((IBlockTileEntity) block).renderBlockModel() ? EnumBlockRenderType.ENTITYBLOCK_ANIMATED : EnumBlockRenderType.MODEL;
    }
    
  	@Override
  	public BlockRenderLayer getBlockLayer(){
  		//Gets the block layer.  Needs to be CUTOUT so textures with alpha in them render.
  		return BlockRenderLayer.CUTOUT;
  	}
  	
  	/**
	 * Registers all blocks in the core mod, as well as any decors in packs.
	 * Also adds the respective TileEntity if the block has one.
	 */
	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event){
		//First check the static entries.
		for(Field field : MTSRegistry.class.getFields()){
			if(field.getType().equals(WrapperBlock.class)){
				try{
					//Get the name of the wrapper and register it.
					WrapperBlock block = (WrapperBlock) field.get(null);
					String name = field.getName().toLowerCase();
					event.getRegistry().register(block.setRegistryName(name).setUnlocalizedName(name));
					if(block.block instanceof IBlockTileEntity){
						tileEntityMap.put(((IBlockTileEntity) block.block).createTileEntity().getClass().getSimpleName(), (IBlockTileEntity) block.block);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		//Register the TE wrappers.
		GameRegistry.registerTileEntity(WrapperTileEntity.class, new ResourceLocation(MTS.MODID, WrapperTileEntity.class.getSimpleName()));
		GameRegistry.registerTileEntity(WrapperTileEntityTickable.class, new ResourceLocation(MTS.MODID, WrapperTileEntityTickable.class.getSimpleName()));
		GameRegistry.registerTileEntity(WrapperFluidTank.class, new ResourceLocation(MTS.MODID, WrapperFluidTank.class.getSimpleName()));
		
		//Now check for any IItemBlock items we need to register blocks for.
		for(String packID : MTSRegistry.packItemMap.keySet()){
			for(AItemPack<?> item : MTSRegistry.packItemMap.get(packID).values()){
				if(item instanceof IItemBlock){
					ABlockBase block = ((IItemBlock) item).getBlock();
					WrapperBlock wrapper = new WrapperBlock(block);
					String name = item.definition.packID + "." + item.definition.systemName;
					event.getRegistry().register(wrapper.setRegistryName(name).setUnlocalizedName(name));
					blockWrapperMap.put(block, wrapper);
					if(block instanceof IBlockTileEntity){
						tileEntityMap.put(((IBlockTileEntity) block).createTileEntity().getClass().getSimpleName(), (IBlockTileEntity) block);
					}
				}
			}
		}
	}
}
