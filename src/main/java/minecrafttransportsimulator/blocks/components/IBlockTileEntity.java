package minecrafttransportsimulator.blocks.components;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**Interface for blocks that have tile entities.  Such tile entities
 * are pack-based, so are linked to a specific pack definition.
 *
 * @author don_bruce
 */
public interface IBlockTileEntity<TileEntityType extends ATileEntityBase<? extends AJSONItem<?>>>{
	
	/**
	 *  Gets a new Tile Entity for this block.
	 */
	public TileEntityType createTileEntity(IWrapperWorld world, Point3i position, WrapperNBT data);
	
	/**
	 *  Gets the class that this Tile Entity is made from.
	 *  This is for registration, not construction.  For construction,
	 *  use {@link #createTileEntity(IWrapperWorld, Point3i, WrapperNBT)}
	 */
	public Class<TileEntityType> getTileEntityClass();
}
