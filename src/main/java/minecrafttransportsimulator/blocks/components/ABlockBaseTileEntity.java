package minecrafttransportsimulator.blocks.components;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Abstract class for blocks that have tile entities.  Such tile entities
 * are pack-based, so are linked to a specific pack definition.
 *
 * @author don_bruce
 */
public abstract class ABlockBaseTileEntity<TileEntityType extends ATileEntityBase<?>> extends ABlockBase{
	
	public ABlockBaseTileEntity(float hardness, float blastResistance){
		super(hardness, blastResistance);
	}
	
	@Override
	public void onBroken(WrapperWorld world, Point3d position){
		TileEntityType tile = world.getTileEntity(position);
		if(tile != null){
			tile.destroy(tile.boundingBox);
		}
	}
	
	/**
	 *  Gets a new Tile Entity for this block.
	 */
	public abstract TileEntityType createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data);
	
	/**
	 *  Gets the class that this Tile Entity is made from.
	 *  This is for registration, not construction.  For construction,
	 *  use {@link #createTileEntity(WrapperWorld, Point3d, WrapperNBT)}
	 */
	public abstract Class<TileEntityType> getTileEntityClass();
}