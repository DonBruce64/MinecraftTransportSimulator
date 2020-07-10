package mcinterface;

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
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
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
public class BuilderBlock extends Block{
	protected static final PropertyDirection FACING = BlockHorizontal.FACING;
	final ABlockBase block;
	
	static final Map<ABlockBase, BuilderBlock> blockWrapperMap = new HashMap<ABlockBase, BuilderBlock>();
	static final Map<String, IBlockTileEntity<?>> tileEntityMap = new HashMap<String, IBlockTileEntity<?>>();
	
	private static final Map<BlockPos, List<ItemStack>> dropsAtPositions = new HashMap<BlockPos, List<ItemStack>>();
	
    public BuilderBlock(ABlockBase block){
		super(Material.ROCK);
		this.block = block;
		fullBlock = false;
		setHardness(block.hardness);
		setResistance(block.blastResistance);
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
        return block instanceof IBlockTileEntity<?>;
    }
    
	@Nullable
    public TileEntity createTileEntity(World world, IBlockState state){
    	//Need to return a wrapper class here, not the actual TE.
		ATileEntityBase<?> tile = ((IBlockTileEntity<?>) block).createTileEntity();
		if(tile instanceof TileEntitySignalController){
			return new BuilderTileEntitySignalController((TileEntitySignalController) tile);
		}else{
			return getTileEntityGenericWrapper(tile);
		}
    }
	
	 /**
	 *  Helper method for creating new Wrapper TEs for this block.
	 *  Far better than ? all over for generics in the createTileEntity method.
	 */
	private static <JSONDefinition extends AJSONItem<? extends AJSONItem<?>.General>> BuilderTileEntity<? extends ATileEntityBase<JSONDefinition>> getTileEntityGenericWrapper(ATileEntityBase<JSONDefinition> tile){
		if(tile instanceof ATileEntityFluidTank){
		   	if(tile instanceof ITileEntityTickable){
		   		return new BuilderTileEntityFluidTank.Tickable<ATileEntityFluidTank<JSONDefinition>>((ATileEntityFluidTank<JSONDefinition>) tile);	
		   	}else{
		   		return new BuilderTileEntityFluidTank<ATileEntityFluidTank<JSONDefinition>>((ATileEntityFluidTank<JSONDefinition>) tile);
		   	}
		}else{
	       	if(tile instanceof ITileEntityTickable){
	       		return new BuilderTileEntity.Tickable<ATileEntityBase<JSONDefinition>>(tile);	
	       	}else{
	       		return new BuilderTileEntity<ATileEntityBase<JSONDefinition>>(tile);
	       	}
		}
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ){
		//Forward this click to the block.  For left-clicks we'll need to use item attack calls.
		return block.onClicked(new WrapperWorld(world), new Point3i(pos.getX(), pos.getY(), pos.getZ()), Axis.valueOf(side.name()), new WrapperEntityPlayer(player));
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
    		TileEntity tile = world.getTileEntity(pos);
    		if(tile instanceof BuilderTileEntity){
    			if(((BuilderTileEntity<?>) tile).tileEntity != null){
    				AJSONItem<? extends AJSONItem<?>.General> definition = ((BuilderTileEntity<?>) tile).tileEntity.getDefinition();
    				if(definition != null){
    					ItemStack stack = new ItemStack(MTSRegistry.packItemMap.get(definition.packID).get(definition.systemName));
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
    	//spawned during the getDrops method.
    	if(block instanceof IBlockTileEntity<?>){
    		TileEntity tile = world.getTileEntity(pos);
    		if(tile instanceof BuilderTileEntity){
    			if(((BuilderTileEntity<?>) tile).tileEntity != null){
    				List<ItemStack> drops = new ArrayList<ItemStack>();
        			for(AItemPack<? extends AJSONItem<? extends AJSONItem<?>.General>> item : ((BuilderTileEntity<?>) tile).tileEntity.getDrops()){
        				drops.add(new ItemStack(item));
        			}
        			dropsAtPositions.put(pos, drops);
    			}
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
		//Sets the blockstate to the correct state to save rotation data.  Should be opposite facing as what the player placed.
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
  	public BlockRenderLayer getBlockLayer(){
  		//Gets the block layer.  Needs to be CUTOUT so textures with alpha in them render.
  		return BlockRenderLayer.CUTOUT;
  	}
  	
  	@Override
  	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos){
  		//Gets the light level.  We need to override this as light level can change.
  		if(block instanceof IBlockTileEntity){
  			TileEntity tile = world.getTileEntity(pos);
  			if(tile instanceof BuilderTileEntity){
  				return (int) (((BuilderTileEntity<?>) tile).tileEntity.lightLevel*15F);
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
		//Create the crafting bench wrappers and register them.
		//None of these will be TEs, so we don't have to check for that.
		for(Field field : MTSRegistry.class.getFields()){
			if(field.getType().equals(BuilderBlock.class)){
				try{
					//Get the name of the wrapper and register it.
					BuilderBlock block = (BuilderBlock) field.get(null);
					String name = field.getName().toLowerCase();
					event.getRegistry().register(block.setRegistryName(name).setUnlocalizedName(name));
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		//Register the TE wrappers.
		GameRegistry.registerTileEntity(BuilderTileEntity.class, new ResourceLocation(MTS.MODID, BuilderTileEntity.class.getSimpleName()));
		GameRegistry.registerTileEntity(BuilderTileEntity.Tickable.class, new ResourceLocation(MTS.MODID, BuilderTileEntity.class.getSimpleName() + BuilderTileEntity.Tickable.class.getSimpleName()));
		GameRegistry.registerTileEntity(BuilderTileEntitySignalController.class, new ResourceLocation(MTS.MODID, BuilderTileEntity.class.getSimpleName() + BuilderTileEntitySignalController.class.getSimpleName()));
		GameRegistry.registerTileEntity(BuilderTileEntityFluidTank.class, new ResourceLocation(MTS.MODID, BuilderTileEntityFluidTank.class.getSimpleName()));
		GameRegistry.registerTileEntity(BuilderTileEntityFluidTank.Tickable.class, new ResourceLocation(MTS.MODID, BuilderTileEntityFluidTank.class.getSimpleName() + BuilderTileEntityFluidTank.Tickable.class.getSimpleName()));
		
		//Register the pack-based blocks.  We cheat here and
		//iterate over all pack items and get the blocks they spawn.
		//Not only does this prevent us from having to manually set the blocks
		//we also pre-generate the block classes here.
		List<ABlockBase> blocksRegistred = new ArrayList<ABlockBase>();
		for(String packID : MTSRegistry.packItemMap.keySet()){
			for(AItemPack<?> item : MTSRegistry.packItemMap.get(packID).values()){
				if(item instanceof IItemBlock){
					ABlockBase itemBlockBlock = ((IItemBlock) item).getBlock();
					if(!blocksRegistred.contains(itemBlockBlock)){
						//New block class detected.  Register it and its instance.
						BuilderBlock wrapper = new BuilderBlock(itemBlockBlock);
						String name = itemBlockBlock.getClass().getSimpleName();
						name = MTS.MODID + ":" + name.substring("Block".length());
						event.getRegistry().register(wrapper.setRegistryName(name).setUnlocalizedName(name));
						blockWrapperMap.put(itemBlockBlock, wrapper);
						blocksRegistred.add(itemBlockBlock);
						if(itemBlockBlock instanceof IBlockTileEntity<?>){
							//Block makes a Tile Entity.  We need to link it to a wrapper.
							tileEntityMap.put(((IBlockTileEntity<?>) itemBlockBlock).createTileEntity().getClass().getSimpleName(), (IBlockTileEntity<?>) itemBlockBlock);
						}
					}
				}
			}
		}
		
		//Finally, register the fake light block.
		event.getRegistry().register(BuilderBlockFakeLight.instance.setRegistryName(MTS.MODID + ":fake_light"));
	}
}
