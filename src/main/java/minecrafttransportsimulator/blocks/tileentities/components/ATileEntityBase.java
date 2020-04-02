package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.rendering.blocks.ARenderTileEntityBase;
import minecrafttransportsimulator.wrappers.WrapperNBT;
import minecrafttransportsimulator.wrappers.WrapperTileEntity;
import minecrafttransportsimulator.wrappers.WrapperWorld;

/**Base Tile Entity class.  This type is used in the constructor of {@link WrapperTileEntity} to allow us to use
 * completely custom code that is not associated with MC's standard Tile Entity code.  Allows us to only
 * update the wrapper rather than the whole Tile Entity. In essence, this class holds the data and state of the
 * Tile Entity, while the wrapper ensures that the state gets saved to disk and the appropriate render gets
 * called.
 *
 * @author don_bruce
 */
public abstract class ATileEntityBase{
	/**Current world for this TileEntity.  May be null in some cases right after construction.**/
	public WrapperWorld world;
	/**Current position of this TileEntity.  Set both manually and during loading from world.**/
	public Point3i position;
	
	/**
	 *  Gets the block that's associated with this TileEntity.
	 */
	public ABlockBase getBlock(){
		return world.getBlock(position);
	}

	/**
	 *  Called when the TileEntity is loaded from saved data.  This method is called
	 *  on the server when loading from disk, but it's not called on the client until
	 *  the client gets the supplemental data packet with the rest of the data.
	 *  Because of this, things may be null on client-sided construction time!
	 */
	public abstract void load(WrapperNBT data);
	
	/**
	 *  Called when the TileEntity needs to be saved to disk.  The passed-in wrapper
	 *  should be written to at this point with any data needing to be saved.
	 */
	public abstract void save(WrapperNBT data);
	
	/**
	 *  Called to get a render for this TE.  Only called on the client.
	 */
	public abstract ARenderTileEntityBase<? extends ATileEntityBase, ? extends WrapperTileEntity.IProvider> getRenderer();
}
