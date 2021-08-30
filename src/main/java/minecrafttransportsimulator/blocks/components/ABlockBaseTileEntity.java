package minecrafttransportsimulator.blocks.components;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Abstract class for blocks that have tile entities.  Such tile entities
 * are pack-based, so are linked to a specific pack definition.
 *
 * @author don_bruce
 */
public abstract class ABlockBaseTileEntity extends ABlockBase{
	
	public ABlockBaseTileEntity(float hardness, float blastResistance){
		super(hardness, blastResistance);
	}
	
	@Override
	public void onBroken(WrapperWorld world, Point3d position){
		ATileEntityBase<?> tile = world.getTileEntity(position);
		if(tile != null){
			tile.destroy(tile.boundingBox);
		}
	}
	
	/**
	 *  Gets a new Tile Entity for this block.
	 *  The placingPlayer may be null if this is being loaded from NBT.
	 */
	public abstract ATileEntityBase<?> createTileEntity(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data);
}