package minecrafttransportsimulator.mcinterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.systems.PackParserSystem;
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
 * It also assumes the block can be rotated, and saves the rotation as a set of
 * FACING properties.  This MAY change in later versions to TE data though...
 *
 * @author don_bruce
 */
@EventBusSubscriber
public class BuilderBlock extends Block{
	/**Map of created blocks linked to their builder instances.  Used for interface operations.**/
	public static final Map<ABlockBase, BuilderBlock> blockMap = new HashMap<ABlockBase, BuilderBlock>();
	/**Maps TE class names to instances of the IBlockTileEntity class that creates them.**/
	public static final Map<String, IBlockTileEntity<?>> tileEntityMap = new HashMap<String, IBlockTileEntity<?>>();
	
	/**Current block we are built around.**/
	public final ABlockBase mcBlock;
	/**Holding map for block drops.  MC calls breakage code after the TE is removed, so we need to store drops 
	created during the drop checks here to ensure they actually drop when the block is broken. **/
	private static final Map<BlockPos, List<ItemStack>> dropsAtPositions = new HashMap<BlockPos, List<ItemStack>>();
	/**Map to hold AABB representations of BB states.  Used to prevent re-creating them every check.**/
	private static final Map<BoundingBox, AxisAlignedBB> boundingBoxMap = new HashMap<BoundingBox, AxisAlignedBB>();
	/**Internal property for rotation of this block.**/
	protected static final PropertyDirection FACING = BlockHorizontal.FACING;
	
    BuilderBlock(ABlockBase block){
		super(Material.ROCK);
		this.mcBlock = block;
		fullBlock = false;
		setHardness(block.hardness);
		setResistance(block.blastResistance);
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.SOUTH));
	}
    
    @Override
    public boolean hasTileEntity(IBlockState state){
    	//If our block implements the interface to be a TE, we return true.
        return mcBlock instanceof IBlockTileEntity<?>;
    }
    
	@Nullable
	@Override
    public TileEntity createTileEntity(World world, IBlockState state){
    	//Need to return a wrapper class here, not the actual TE.
		Class<? extends ATileEntityBase<?>> teClass = ((IBlockTileEntity<?>) mcBlock).getTileEntityClass();
		if(ITileEntityFluidTankProvider.class.isAssignableFrom(teClass)){
			return getTileEntityTankWrapper(mcBlock);
		}else{
			return getTileEntityGenericWrapper(mcBlock);
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
	private static <TileEntityType extends ATileEntityBase<?> & ITileEntityFluidTankProvider> BuilderTileEntity<TileEntityType> getTileEntityTankWrapper(ABlockBase block){
		return new BuilderTileEntityFluidTank<TileEntityType>();
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ){
		//Forward this click to the block.  For left-clicks we'll need to use item attack calls.
		if(mcBlock instanceof IBlockTileEntity<?>){
    		TileEntity tile = world.getTileEntity(pos);
    		if(tile instanceof BuilderTileEntity){
    			if(((BuilderTileEntity<?>) tile).tileEntity != null){
    				return mcBlock.onClicked(WrapperWorld.getWrapperFor(world), new Point3d(pos.getX(), pos.getY(), pos.getZ()), Axis.valueOf(side.name()), WrapperPlayer.getWrapperFor(player));
    			}
    		}
		}
		return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
	}
    
    @Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player){
		//Returns the ItemStack that gets put in the player's inventory when they middle-click this block.
    	//This calls down into getItem, which then uses the Item class's block<->item mapping to get a block.
    	//By overriding here, we intercept those calls and return our own.  This also allows us to put NBT
    	//data on the stack based on the TE state.
    	
    	//Note that this method is only used for middle-clicking and nothing else.  Failure to return valid results
    	//here will result in air being grabbed, and no WAILA support.
    	if(mcBlock instanceof IBlockTileEntity<?>){
    		TileEntity mcTile = world.getTileEntity(pos);
    		if(mcTile instanceof BuilderTileEntity){
    			ATileEntityBase<?> tile = ((BuilderTileEntity<?>) mcTile).tileEntity;
    			if(tile != null){
    				AItemPack<?> item = tile.getItem();
    				if(item != null){
    					ItemStack stack = item.getNewStack();
    	        		WrapperNBT data = new WrapperNBT(new NBTTagCompound());
    	        		((BuilderTileEntity<?>) mcTile).tileEntity.save(data);
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
    	if(mcBlock instanceof IBlockTileEntity<?>){
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
    	//Forward the breaking call to the block to allow for breaking logic.
    	mcBlock.onBroken(WrapperWorld.getWrapperFor(world), new Point3d(pos.getX(), pos.getY(), pos.getZ()));
    	//This gets called before the block is broken to do logic.  Save drops to static map to be
    	//spawned during the getDrops method.  Also notify the block that it's been broken in case
    	//it needs to do operations.
    	if(mcBlock instanceof IBlockTileEntity<?>){
    		TileEntity tile = world.getTileEntity(pos);
    		if(tile instanceof BuilderTileEntity){
    			if(((BuilderTileEntity<?>) tile).tileEntity != null){
    				List<ItemStack> drops = new ArrayList<ItemStack>();
    				((BuilderTileEntity<?>) tile).tileEntity.addDropsToList(drops);
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
    	mcBlock.addCollisionBoxes(WrapperWorld.getWrapperFor(world), new Point3d(pos.getX(), pos.getY(), pos.getZ()), collisionBoxes);
    	for(BoundingBox box : collisionBoxes){
    		AxisAlignedBB mcBox = box.convertWithOffset(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
			if(mcBox.intersects(entityBox)){
				collidingBoxes.add(mcBox);
			}
    	}
    }
    
    @Override
	@SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos){
        BoundingBox box = mcBlock.getCollisionBounds();
        if(!boundingBoxMap.containsKey(box)){
        	boundingBoxMap.put(box, box.convertWithOffset(0.5D, 0.5D, 0.5D));
        }
        return boundingBoxMap.get(box);
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
    	//If this is opaque, we block light.  None of our blocks are opaque and block light.
        return false;
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state){
    	//If this is a full cube, we do culling on faces and potentially connections.  None of our blocks are full cubes.
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
    	//Don't render this block.  We manually render via the TE.
        return EnumBlockRenderType.INVISIBLE;
    }
  	
  	/**
	 * Registers all blocks in the core mod, as well as any decors in packs.
	 * Also adds the respective TileEntity if the block has one.
	 */
	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event){
		//Create all pack items.  We need to do this here in the blocks because
		//block registration comes first, and we use the items registered to determine
		//which blocks we need to register.
		for(AItemPack<?> packItem : PackParserSystem.getAllPackItems()){
			new BuilderItem(packItem);
		}
		
		//Register the TEs.
		GameRegistry.registerTileEntity(BuilderTileEntity.class, new ResourceLocation(MasterLoader.MODID, BuilderTileEntity.class.getSimpleName()));
		GameRegistry.registerTileEntity(BuilderTileEntity.Tickable.class, new ResourceLocation(MasterLoader.MODID, BuilderTileEntity.class.getSimpleName() + BuilderTileEntity.Tickable.class.getSimpleName()));
		GameRegistry.registerTileEntity(BuilderTileEntityFluidTank.class, new ResourceLocation(MasterLoader.MODID, BuilderTileEntityFluidTank.class.getSimpleName()));
		
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
					name = MasterLoader.MODID + ":" + name.substring("Block".length());
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
		
		//Register the collision blocks.
		for(int i=0; i<BlockCollision.blockInstances.size(); ++i){
			BlockCollision collisionBlock = BlockCollision.blockInstances.get(i);
			BuilderBlock wrapper = new BuilderBlock(collisionBlock);
			String name = collisionBlock.getClass().getSimpleName();
			name = MasterLoader.MODID + ":" + name.substring("Block".length()) + i;
			event.getRegistry().register(wrapper.setRegistryName(name).setTranslationKey(name));
			blockMap.put(collisionBlock, wrapper);
		}
		
		//Register the fake light block.
		event.getRegistry().register(BuilderBlockFakeLight.instance.setRegistryName(MasterLoader.MODID + ":fake_light"));
	}
}
