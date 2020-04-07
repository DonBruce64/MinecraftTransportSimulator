package minecrafttransportsimulator.wrappers;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.block.Block;
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
	 *  Has the player place the passed-in block at the point specified.
	 *  Returns true if the block was placed, false if not.
	 */
    public boolean setBlock(ABlockBase block, Point3i point, WrapperPlayer player, Axis axis){
    	if(!world.isRemote){
	    	WrapperBlock wrapper = WrapperBlock.blockWrapperMap.get(block);
	    	ItemStack itemstack = player.getHeldStack();
	    	BlockPos pos = new BlockPos(point.x, point.y, point.z);
	    	EnumFacing facing = EnumFacing.valueOf(axis.name());
	    	if(!world.getBlockState(pos).getBlock().isReplaceable(world, pos)){
	            pos = pos.offset(facing);
	        }
	    	if(!itemstack.isEmpty() && player.player.canPlayerEdit(pos, facing, itemstack) && world.mayPlace(wrapper, pos, false, facing, null)){
	            IBlockState newState = wrapper.getStateForPlacement(world, pos, facing, 0, 0, 0, 0, player.player, EnumHand.MAIN_HAND);
	            if(world.setBlockState(pos, newState, 11)){
	                itemstack.shrink(1);
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
	 *  Gets the brightness at this point, as a value between 0-1. Calculated from the
	 *  sun brightness and the brightness of neighboring blocks.
	 */
	public float getLightBrightness(Point3i point){
		BlockPos polePos = new BlockPos(point.x, point.y, point.z);
		float sunLight = world.getSunBrightness(0)*world.getLightBrightness(polePos);
		float blockLight = world.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, polePos)/15F;
		return Math.min((1 - Math.max(sunLight, blockLight)), 1);
	}
}