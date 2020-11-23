package mcinterface1122;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.IFluidTankProvider;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemBlock;
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

/**Builder for a basic MC Block class.  This builder assumes the block will not be a solid
 * block (so no culling) and may have alpha channels in the texture (like glass).
 * It also assumes the block can be rotated, and saves the rotation with whatever
 * version-specific rotation scheme the current MC version uses.
 *
 * @author don_bruce
 */
@EventBusSubscriber
class BuilderBlock extends Block{
	/**Map of created blocks linked to their builder instances.  Used for interface operations.**/
	static final Map<ABlockBase, BuilderBlock> blockMap = new HashMap<ABlockBase, BuilderBlock>();
	/**Maps TE class names to instances of the IBlockTileEntity class that creates them.**/
	static final Map<String, IBlockTileEntity<?>> tileEntityMap = new HashMap<String, IBlockTileEntity<?>>();
	
	/**Current block we are built around.**/
	final ABlockBase block;
	/**Holding map for block drops.  MC calls breakage code after the TE is removed, so we need to store drops 
	created during the drop checks here to ensure they actually drop when the block is broken. **/
	private static final Map<BlockPos, List<ItemStack>> dropsAtPositions = new HashMap<BlockPos, List<ItemStack>>();
	/**Internal property for rotation of this block.**/
	protected static final PropertyDirection FACING = BlockHorizontal.FACING;
	
    BuilderBlock(ABlockBase block){
		super(Material.ROCK);
		this.block = block;
		fullBlock = false;
		setHardness(block.hardness);
		setResistance(block.blastResistance);
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.SOUTH));
	}
    
    @Override
    public boolean hasTileEntity(IBlockState state){
    	//If our block implements the interface to be a TE, we return true.
        return block instanceof IBlockTileEntity<?>;
    }
    
	@Nullable
    public TileEntity createTileEntity(World world, IBlockState state){
    	//Need to return a wrapper class here, not the actual TE.
		Class<? extends ATileEntityBase<?>> teClass = ((IBlockTileEntity<?>) block).getTileEntityClass();
		if(IFluidTankProvider.class.isAssignableFrom(teClass)){
			return getTileEntityTankWrapper(block);
		}else{
			return getTileEntityGenericWrapper(block);
		}
    }
	
	 /**
	 *  Helper method for creating new Wrapper TEs for this block.
	 *  Far better than ? all over for generics in the createTileEntity method.
	 */
	@SuppressWarnings("unchecked")
	private static <TileEntityType extends ATileEntityBase<?>> BuilderTileEntity<TileEntityType> getTileEntityGenericWrapper(ABlockBase block){
		Class<TileEntityType> teClass = ((IBlockTileEntity<TileEntityType>) block).getTileEntityClass();
		if(ITileEntityTickable.class.isAssignableFrom(teClass)){
       		return new BuilderTileEntity.Tickable<TileEntityType>();	
       	}else{
       		return new BuilderTileEntity<TileEntityType>();
       	}
	}
	
	 /**
	 *  Helper method for creating new Wrapper TEs for this block.
	 *  Far better than ? all over for generics in the createTileEntity method.
	 */
	private static <TileEntityType extends ATileEntityBase<?> & IFluidTankProvider> BuilderTileEntity<TileEntityType> getTileEntityTankWrapper(ABlockBase block){
		return new BuilderTileEntityFluidTank<TileEntityType>();
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ){
		//Forward this click to the block.  For left-clicks we'll need to use item attack calls.
		return block.onClicked(WrapperWorld.getWrapperFor(world), new Point3i(pos.getX(), pos.getY(), pos.getZ()), Axis.valueOf(side.name()), WrapperWorld.getWrapperFor(world).getWrapperFor(player));
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
    		TileEntity tile = world.getTileEntity(pos);
    		if(tile instanceof BuilderTileEntity){
    			if(((BuilderTileEntity<?>) tile).tileEntity != null){
    				AItemPack<?> item = ((BuilderTileEntity<?>) tile).tileEntity.item;
    				if(item != null){
    					ItemStack stack = new ItemStack(BuilderItem.itemMap.get(item));
    	        		WrapperNBT data = new WrapperNBT(new NBTTagCompound());
    	        		((BuilderTileEntity<?>) tile).tileEntity.save(data);
    	        		stack.setTagCompound(data.tag);
    	            	return stack;
    				}
    			}
    		}
    	}
    	return super.getPickBlock(state, target, world, pos, player);
    }
    
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune){
    	//If this is a TE, drop TE drops.  Otherwise, drop normal drops.
    	if(block instanceof IBlockTileEntity<?>){
    		if(dropsAtPositions.containsKey(pos)){
    			drops.addAll(dropsAtPositions.get(pos));
    			dropsAtPositions.remove(pos);
    		}
    	}else{
    		super.getDrops(drops, world, pos, state, fortune);
    	}
    		
    }
    
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state){
    	//This gets called before the block is broken to do logic.  Save drops to static map to be
    	//spawned during the getDrops method.  Also notify the block that it's been broken in case
    	//it needs to do operations.
    	if(block instanceof IBlockTileEntity<?>){
    		TileEntity tile = world.getTileEntity(pos);
    		if(tile instanceof BuilderTileEntity){
    			if(((BuilderTileEntity<?>) tile).tileEntity != null){
    				List<ItemStack> drops = new ArrayList<ItemStack>();
        			for(AItemPack<? extends AJSONItem<? extends AJSONItem<?>.General>> item : ((BuilderTileEntity<?>) tile).tileEntity.getDrops()){
        				drops.add(new ItemStack(BuilderItem.itemMap.get(item)));
        			}
        			dropsAtPositions.put(pos, drops);
    			}
    		}
    	}
    	super.breakBlock(world, pos, state);
    }
    
    @Override
	@SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean p_185477_7_){
    	//Gets the collision boxes. We forward this call to the block to handle.
    	//We add-on 0.5D to offset the box to the correct location.
    	List<BoundingBox> collisionBoxes = new ArrayList<BoundingBox>();
    	block.addCollisionBoxes(WrapperWorld.getWrapperFor(world), new Point3i(pos.getX(), pos.getY(), pos.getZ()), collisionBoxes);
    	for(BoundingBox box : collisionBoxes){
    		AxisAlignedBB mcBox = new AxisAlignedBB(
				pos.getX() + 0.5D + box.globalCenter.x - box.widthRadius, 
				pos.getY() + 0.5D + box.globalCenter.y - box.heightRadius, 
				pos.getZ() + 0.5D + box.globalCenter.z - box.depthRadius, 
				pos.getX() + 0.5D + box.globalCenter.x + box.widthRadius, 
				pos.getY() + 0.5D + box.globalCenter.y + box.heightRadius, 
				pos.getZ() + 0.5D + box.globalCenter.z + box.depthRadius);
			if(mcBox.intersects(entityBox)){
				collidingBoxes.add(mcBox);
			}
    	}
    }
    
	@Override
	public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand){
		//Sets the blockstate to the correct state to save rotation data.  Should be opposite facing as what the player placed.
		return super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer, hand).withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }
	
	@Override
	@SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta){
		//Restores the sate from meta.
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state){
    	//Saves the state as metadata.
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rot){
    	//Returns the state with the applied rotation.  Rotate based on how we're facing.
        return state.getBlock() != this ? state : state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockStateContainer createBlockState(){
    	//Creates a new, default, blockstate holder.  Return the four facing directions here.
        return new BlockStateContainer(this, new IProperty[] {FACING});
    }
	
    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state){
    	//If this is opaque, we block light.  None of our blocks are opaque.
        return false;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state){
    	//If this is a full cube, we do culling on faces.  None of our blocks are full cubes.
        return false;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face){
    	//If this is SOLID, we can attach things to this block (e.g. torches).  We don't want that for any of our blocks.
        return BlockFaceShape.UNDEFINED;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public EnumBlockRenderType getRenderType(IBlockState state){
    	//Handles if we render a block model or not.
        return block instanceof IBlockTileEntity<?> ? EnumBlockRenderType.ENTITYBLOCK_ANIMATED : EnumBlockRenderType.MODEL;
    }
    
  	@Override
  	public BlockRenderLayer getRenderLayer(){
  		//Gets the block layer.  Needs to be CUTOUT so textures with alpha in them render.
  		return BlockRenderLayer.CUTOUT;
  	}
  	
  	@Override
  	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos){
  		//Gets the light level.  We need to override this as light level can change.
  		if(block instanceof IBlockTileEntity){
  			BuilderTileEntity<?> builder = (BuilderTileEntity<?>) world.getTileEntity(pos);
  			if(builder != null && builder.tileEntity != null){
  				return (int) (builder.tileEntity.lightLevel*15F);
  			}
  		}
        return super.getLightValue(state, world, pos);
    }
  	
  	/**
	 * Registers all blocks in the core mod, as well as any decors in packs.
	 * Also adds the respective TileEntity if the block has one.
	 */
	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event){
		//Register the TEs.
		GameRegistry.registerTileEntity(BuilderTileEntity.class, new ResourceLocation(MasterInterface.MODID, BuilderTileEntity.class.getSimpleName()));
		GameRegistry.registerTileEntity(BuilderTileEntity.Tickable.class, new ResourceLocation(MasterInterface.MODID, BuilderTileEntity.class.getSimpleName() + BuilderTileEntity.Tickable.class.getSimpleName()));
		GameRegistry.registerTileEntity(BuilderTileEntityFluidTank.class, new ResourceLocation(MasterInterface.MODID, BuilderTileEntityFluidTank.class.getSimpleName()));
		
		//Register the IItemBlock blocks.  We cheat here and
		//iterate over all items and get the blocks they spawn.
		//Not only does this prevent us from having to manually set the blocks
		//we also pre-generate the block classes here.
		List<ABlockBase> blocksRegistred = new ArrayList<ABlockBase>();
		for(AItemBase item : BuilderItem.itemMap.keySet()){
			if(item instanceof IItemBlock){
				ABlockBase itemBlockBlock = ((IItemBlock) item).getBlock();
				if(!blocksRegistred.contains(itemBlockBlock)){
					//New block class detected.  Register it and its instance.
					BuilderBlock wrapper = new BuilderBlock(itemBlockBlock);
					String name = itemBlockBlock.getClass().getSimpleName();
					name = MasterInterface.MODID + ":" + name.substring("Block".length());
					event.getRegistry().register(wrapper.setRegistryName(name).setTranslationKey(name));
					blockMap.put(itemBlockBlock, wrapper);
					blocksRegistred.add(itemBlockBlock);
					if(itemBlockBlock instanceof IBlockTileEntity<?>){
						//Block makes a Tile Entity.  We need to link it to a wrapper.
						tileEntityMap.put(((IBlockTileEntity<?>) itemBlockBlock).getTileEntityClass().getSimpleName(), (IBlockTileEntity<?>) itemBlockBlock);
					}
				}
			}
		}
		
		//Finally, register the fake light block.
		event.getRegistry().register(BuilderBlockFakeLight.instance.setRegistryName(MasterInterface.MODID + ":fake_light"));
	}
}
