package minecrafttransportsimulator.wrappers;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

/**Wrapper for the world class.  This wrapper contains many common methods that 
 * MC has seen fit to change over multiple versions (such as lighting) and as such
 * provides a single point of entry to the world to interface with it.  This class
 * should be used whenever possible to replace the normal world object reference
 * with methods that re-direct to this wrapper.  This wrapper is normally created
 * from an instance of an {@link World} object passed-in to the constructor, so this
 * means you'll need something to get an instance of the MC world beforehand.
 * Note that other wrappers may access the world variable directly for things
 * that are specific to their classes (such as blocks getting states).
 *
 * @author don_bruce
 */
public class WrapperWorld{
	
	final World world;

	public WrapperWorld(World world){
		this.world = world;
	}
	
	/**
	 *  Returns true if this is a client world, false if we're on the server.
	 */
	public boolean isClient(){
		return world.isRemote;
	}
	
	/**
	 *  Returns the ID of the current dimension.
	 *  0 for overworld.
	 *  1 for the End.
	 *  -1 for the Nether.
	 *  Mods may add other values for their dims, so this list is not inclusive.
	 */
	public int getDimensionID(){
		return world.provider.getDimension();
	}
	
	/**
	 *  Returns the current world time, in ticks.  Useful when you need to sync
	 *  operations.  For animations, just use the system time.
	 */
	public long getTime(){
		return world.getTotalWorldTime();
	}
	
	/**
	 *  Returns the entity that has the passed-in ID.
	 */
	public Entity getEntity(int id){
		return world.getEntityByID(id);
	}
	
	/**
	 *  Returns the player with the passed-in ID.
	 */
	public WrapperPlayer getPlayer(int id){
		return new WrapperPlayer((EntityPlayer) world.getEntityByID(id));
	}
	
	/**
	 *  Returns the vehicle that has the passed-in ID.
	 */
	public EntityVehicleE_Powered getVehicle(int id){
		return (EntityVehicleE_Powered) world.getEntityByID(id);
	}
	
	/**
	 *  Returns a list of all vehicles within specified bounds.
	 */
	public List<EntityVehicleE_Powered> getVehiclesWithin(BoundingBox bounds){
		List<EntityVehicleE_Powered> vehicles = new ArrayList<EntityVehicleE_Powered>();
		for(Entity entity : world.loadedEntityList){
			if(entity instanceof EntityVehicleE_Powered){
				if(bounds.isPointInside(new Point3d(entity.posX, entity.posY, entity.posZ))){
					vehicles.add((EntityVehicleE_Powered) entity);
				}
			}
		}
		return vehicles;
	}
	
	/**
	 *  Returns true if the block at the passed-in location is solid.  Solid means
	 *  that said block can be collided with, is a cube, and is generally able to have
	 *  things placed or connected to it.
	 */
	public boolean isBlockSolid(Point3i point){
		IBlockState offsetMCState = world.getBlockState(new BlockPos(point.x, point.y, point.z));
		Block offsetMCBlock = offsetMCState.getBlock();
        return offsetMCBlock != null ? !offsetMCBlock.equals(Blocks.BARRIER) && offsetMCState.getMaterial().isOpaque() && offsetMCState.isFullCube() && offsetMCState.getMaterial() != Material.GOURD : false;
	}
	
	/**
	 *  Returns the block at the passed-in location, or null if it doesn't exist in the world.
	 *  Only valid for blocks of type {@link ABlockBase} others will return null.
	 */
	public ABlockBase getBlock(Point3i point){
		Block block = world.getBlockState(new BlockPos(point.x, point.y, point.z)).getBlock();
		return block instanceof WrapperBlock ? ((WrapperBlock) block).block : null;
	}
	
	/**
	 *  Returns the current redstone power at the passed-in position.
	 */
	public int getRedstonePower(Point3i point){
		return world.getStrongPower(new BlockPos(point.x, point.y, point.z));
	}
    
    /**
	 *  Has the player place the passed-in block at the point specified.
	 *  Returns true if the block was placed, false if not.
	 */
    @SuppressWarnings("unchecked")
	public <JSONDefinition extends AJSONItem<? extends AJSONItem<?>.General>> boolean setBlock(ABlockBase block, Point3i location, WrapperPlayer player, Axis axis){
    	if(!world.isRemote){
	    	WrapperBlock wrapper = WrapperBlock.blockWrapperMap.get(block);
	    	ItemStack stack = player.getHeldStack();
	    	BlockPos pos = new BlockPos(location.x, location.y, location.z);
	    	EnumFacing facing = EnumFacing.valueOf(axis.name());
	    	if(!world.getBlockState(pos).getBlock().isReplaceable(world, pos)){
	            pos = pos.offset(facing);
	            location.offset(facing.getFrontOffsetX(), facing.getFrontOffsetY(), facing.getFrontOffsetZ());
	        }
	    	if(!stack.isEmpty() && player.player.canPlayerEdit(pos, facing, stack) && world.mayPlace(wrapper, pos, false, facing, null)){
	            IBlockState newState = wrapper.getStateForPlacement(world, pos, facing, 0, 0, 0, 0, player.player, EnumHand.MAIN_HAND);
	            if(world.setBlockState(pos, newState, 11)){
	            	//Block is set.  See if we need to set TE data.
	            	if(block instanceof IBlockTileEntity){
	            		ATileEntityBase<JSONDefinition> tile = (ATileEntityBase<JSONDefinition>) getTileEntity(location);
	            		if(stack.hasTagCompound()){
	            			tile.load(new WrapperNBT(stack.getTagCompound()));
	            		}else{
	            			tile.setDefinition(((AItemPack<JSONDefinition>) stack.getItem()).definition);
	            		}
	            	}
	            	//Send place event to block class, and also send initial update cheeck.
	            	block.onPlaced(this, location, player);
	                stack.shrink(1);
	            }
	            return true;
	        }
    	}
    	return false;
    }
	
	/**
	 *  Returns the tile entity at the passed-in location, or null if it doesn't exist in the world.
	 *  Only valid for TEs of type {@link ATileEntityBase} others will return null.
	 */
	public ATileEntityBase<?> getTileEntity(Point3i point){
		TileEntity tile = world.getTileEntity(new BlockPos(point.x, point.y, point.z));
		return tile instanceof WrapperTileEntity ? ((WrapperTileEntity<?>) tile).tileEntity : null;
	}
	
	/**
	 *  Flags the tile entity at the passed-in point for saving.  This means the TE's
	 *  NBT data will be saved to disk when the chunk unloads so it will maintain its state.
	 */
	public void markTileEntityChanged(Point3i point){
		world.getTileEntity(new BlockPos(point.x, point.y, point.z)).markDirty();
	}
	
	/**
	 *  Gets the brightness at this point, as a value between 0.0-1.0. Calculated from the
	 *  sun brightness, and possibly the block brightness if calculateBlock is true.
	 */
	public float getLightBrightness(Point3i point, boolean calculateBlock){
		BlockPos pos = new BlockPos(point.x, point.y, point.z);
		float sunLight = world.getSunBrightness(0)*(world.getLightFor(EnumSkyBlock.SKY, pos) - world.getSkylightSubtracted())/15F;
		float blockLight = calculateBlock ? world.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, pos)/15F : 0.0F;
		return Math.max(sunLight, blockLight);
	}
	
	/**
	 *  Updates the brightness of the block at this point.  Only works if the block
	 *  is a dynamic-brightness block that implements {@link ITileEntityProvider}. 
	 */
	public void updateLightBrightness(Point3i point){
		ATileEntityBase<?> tile = getTileEntity(point);
		if(tile != null){
			BlockPos pos = new BlockPos(point.x, point.y, point.z);
			//This needs to get fired manually as even if we update the blockstate the light value won't change
			//as the actual state of the block doesn't change, so MC doesn't think it needs to do any lighting checks.
			world.checkLight(pos);
		}
	}
	
	/**
	 *  Sets a fake light block at the passed-in position.
	 *  Only sets the fake light if the block at the passed-in position is air.
	 *  Make sure you track this position and remove the light when it's not in-use! 
	 */
	public void setFakeLight(Point3i point){
		BlockPos pos = new BlockPos(point.x, point.y, point.z);
		if(world.isAirBlock(pos)){
			world.setBlockState(pos, WrapperBlockFakeLight.instance.getDefaultState());
		}
	}
	
	/**
	 *  Sets the block at the passed-in position to air. 
	 *  This does no sanity checks, so make sure you're
	 *  actually allowed to do such a thing before calling.
	 */
	public void setToAir(Point3i point){
		world.setBlockToAir(new BlockPos(point.x, point.y, point.z));
	}
}