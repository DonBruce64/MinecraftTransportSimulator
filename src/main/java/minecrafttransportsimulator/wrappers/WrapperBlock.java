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
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityFluidTank;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.core.IItemBlock;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
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
import net.minecraft.entity.item.EntityItem;
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
	static final Map<String, IBlockTileEntity<?>> tileEntityMap = new HashMap<String, IBlockTileEntity<?>>();
	
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
    
	 /**
	 *  Helper method for creating new Wrapper TEs for this block.
	 *  Far better than ? all over for generics in the createTileEntity method.
	 */
	private static <JSONDefinition extends AJSONItem<? extends AJSONItem<?>.General>> WrapperTileEntity<? extends ATileEntityBase<JSONDefinition>> getTileEntityWrapper(IBlockTileEntity<JSONDefinition> block){
		ATileEntityBase<JSONDefinition> tile = block.createTileEntity();
		if(tile instanceof ATileEntityFluidTank){
		   	if(tile instanceof ITileEntityTickable){
		   		return new WrapperTileEntityFluidTank.Tickable<ATileEntityFluidTank<JSONDefinition>>((ATileEntityFluidTank<JSONDefinition>) tile);	
		   	}else{
		   		return new WrapperTileEntityFluidTank<ATileEntityFluidTank<JSONDefinition>>((ATileEntityFluidTank<JSONDefinition>) tile);
		   	}
		}else{
	       	if(tile instanceof ITileEntityTickable){
	       		return new WrapperTileEntity.Tickable<ATileEntityBase<JSONDefinition>>(tile);	
	       	}else{
	       		return new WrapperTileEntity<ATileEntityBase<JSONDefinition>>(tile);
	       	}
		}
	}
	
    
    @Override
    public boolean hasTileEntity(IBlockState state){
    	//If our block implements the interface to be a TE, we return true.
        return block instanceof IBlockTileEntity<?>;
    }
    
    
	@Nullable
    public TileEntity createTileEntity(World world, IBlockState state){
    	//Need to return a wrapper class here, not the actual TE.
		return getTileEntityWrapper((IBlockTileEntity<?>) block);
    }
    
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack stack){
    	WrapperWorld wWrapper = new WrapperWorld(world);
    	Point3i location = new Point3i(pos.getX(), pos.getY(), pos.getZ());
    	//Forward place event to the block if a player placed this block.
    	if(entity instanceof EntityPlayer){
    		block.onPlaced(wWrapper, location, new WrapperPlayer((EntityPlayer) entity));
    	}
    	//If the block was prior saved as NBT, load it back.
    	if(block instanceof IBlockTileEntity<?>){
    		wWrapper.getTileEntity(location).load(new WrapperNBT(stack.getTagCompound()));
    	}
    }
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ){
		//Forward this click to the block.  For left-clicks we'll need to use item attack calls.
		return block.onClicked(new WrapperWorld(world), new Point3i(pos.getX(), pos.getY(), pos.getZ()), Axis.valueOf(hand.name()), new WrapperPlayer(player), true);
	}
    
    @Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player){
		//Returns the ItemStack that gets put in the player's inventory when they middle-click this block.
    	//This calls down into getItem, which then uses the Item class's block<->item mapping to get a block.
    	//By overriding here, we intercept those calls and return our own.  This also allows us to put NBT
    	//data on the stack based on the TE state.
    	
    	//Note that this method is only used for middle-clicking and nothing else.  Failure to return valid results
    	//here will result in air being grabbed, and no WAILA support.
    	if(block instanceof IBlockTileEntity<?>){
    		//TODO move this into the interface when we have a wrapper itemstack.
    		WrapperWorld wWorld = new WrapperWorld(world);
    		Point3i location = new Point3i(pos.getX(), pos.getY(), pos.getZ());
    		ATileEntityBase<?> tile = wWorld.getTileEntity(location);
    		if(tile != null){
    			AJSONItem<? extends AJSONItem<?>.General> definition = tile.getDefinition();
        		ItemStack stack = new ItemStack(MTSRegistry.packItemMap.get(definition.packID).get(definition.systemName));
        		WrapperNBT data = new WrapperNBT(new NBTTagCompound());
        		stack.setTagCompound(data.tag);
            	return stack;
    		}
    	}
    	return super.getPickBlock(state, target, world, pos, player);
    }
    
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune){
    	//Don't drop this block as an item in the normal drops if we have a TE.
    	//TE drops happen in the breakBlock call.
    	if(!(block instanceof IBlockTileEntity<?>)){
    		super.getDrops(drops, world, pos, state, fortune);
    	}
    }
    
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state){
    	//This gets called before the block is broken to do logic.  Normally we'd use the getDrops method,
    	//by MC is stupid and deletes the TE before calling that method, meaning we can't save NBT.
    	//If the block has a TE, get it and save its NBT to the item.
    	if(block instanceof IBlockTileEntity<?>){
    		//TODO move this into the interface when we have a wrapper itemstack.
    		WrapperWorld wWorld = new WrapperWorld(world);
    		Point3i location = new Point3i(pos.getX(), pos.getY(), pos.getZ());
    		ATileEntityBase<?> tile = wWorld.getTileEntity(location);
    		if(tile != null){
    			AJSONItem<? extends AJSONItem<?>.General> definition = tile.getDefinition();
        		ItemStack stack = new ItemStack(MTSRegistry.packItemMap.get(definition.packID).get(definition.systemName));
        		WrapperNBT data = new WrapperNBT(new NBTTagCompound());
        		stack.setTagCompound(data.tag);
            	world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), stack));
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
        return block instanceof IBlockTileEntity<?> ? EnumBlockRenderType.ENTITYBLOCK_ANIMATED : EnumBlockRenderType.MODEL;
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
					if(block.block instanceof IBlockTileEntity<?>){
						tileEntityMap.put(((IBlockTileEntity<?>) block.block).createTileEntity().getClass().getSimpleName(), (IBlockTileEntity<?>) block.block);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		//Register the TE wrappers.
		GameRegistry.registerTileEntity(WrapperTileEntity.class, new ResourceLocation(MTS.MODID, WrapperTileEntity.class.getSimpleName()));
		GameRegistry.registerTileEntity(WrapperTileEntity.Tickable.class, new ResourceLocation(MTS.MODID, WrapperTileEntity.class.getSimpleName() + WrapperTileEntity.Tickable.class.getSimpleName()));
		GameRegistry.registerTileEntity(WrapperTileEntityFluidTank.class, new ResourceLocation(MTS.MODID, WrapperTileEntityFluidTank.class.getSimpleName()));
		GameRegistry.registerTileEntity(WrapperTileEntityFluidTank.Tickable.class, new ResourceLocation(MTS.MODID, WrapperTileEntityFluidTank.class.getSimpleName() + WrapperTileEntityFluidTank.Tickable.class.getSimpleName()));
		
		//Now check for any IItemBlock items we need to register blocks for.
		//Note that multiple IItemBlocks may share the same block, 
		//so we need to check and not duplicate registrations.
		List<ABlockBase> blocksRegistred = new ArrayList<ABlockBase>();
		for(String packID : MTSRegistry.packItemMap.keySet()){
			for(AItemPack<?> item : MTSRegistry.packItemMap.get(packID).values()){
				if(item instanceof IItemBlock){
					IItemBlock itemBlock = (IItemBlock) item;
					ABlockBase itemBlockBlock = itemBlock.getBlock();
					if(!blocksRegistred.contains(itemBlockBlock)){
						WrapperBlock wrapper = new WrapperBlock(itemBlockBlock);
						String name = item.definition.packID + "." + item.definition.systemName;
						event.getRegistry().register(wrapper.setRegistryName(name).setUnlocalizedName(name));
						blockWrapperMap.put(itemBlockBlock, wrapper);
					}
				}
			}
		}
	}
}
