package minecrafttransportsimulator.wrappers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**Wrapper for the world class.  This wrapper contains many common methods that 
 * MC has seen fit to change over multiple versions (such as lighting) and as such
 * provides a single point of entry to the world to interface with it.  This class
 * should be used whenever possible to replace the normal world object reference
 * with methods that re-direct to this wrapper.  This wrapper is normally created
 * from an instance of an {@link World} object passed-in to the constructor, so this
 * means you'll need something to get an instance of the MC world beforehand.
 *
 * @author don_bruce
 */
public class WrapperWorld{
	
	private final World world;

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
	 *  Returns the entity that has the passed-in ID.
	 */
	public Entity getEntity(int id){
		return world.getEntityByID(id);
	}
	
	/**
	 *  Returns the player with the passed-in ID.
	 *  Note that this auto-performs a cast from {@link Entity} to {@link EntityPlayer},
	 *  so make sure that the ID you pass in is for a player or you will crash!
	 */
	public EntityPlayer getPlayer(int id){
		return (EntityPlayer) world.getEntityByID(id);
	}
	
	/**
	 *  Returns the tile entity at the passed-in location, or null if it doesn't exist in the world.
	 */
	public TileEntity getTileEntity(int x, int y, int z){
		return world.getTileEntity(new BlockPos(x, y, z));
	}
}