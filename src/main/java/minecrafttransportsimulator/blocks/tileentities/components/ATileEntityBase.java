package minecrafttransportsimulator.blocks.tileentities.components;

import java.util.ArrayList;
import java.util.List;

import mcinterface.BuilderTileEntity;
import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.rendering.instances.ARenderTileEntityBase;

/**Base Tile Entity class.  This type is used in the constructor of {@link BuilderTileEntity} to allow us to use
 * completely custom code that is not associated with MC's standard Tile Entity code.  Allows us to only
 * update the wrapper rather than the whole Tile Entity. In essence, this class holds the data and state of the
 * Tile Entity, while the wrapper ensures that the state gets saved to disk and the appropriate render gets
 * called.  Note that all TileEntities are used for making pack-based blocks, so they have JSON parameters
 * attached to them.  
 * <br><br>
 * Note that this constructor is called on the server when first creating the TE or loading it from disk,
 * but on the client this is called after the server sends over the saved data, not when the player first clicks.
 * Because of this, there there may be a slight delay in the TE showing up from when the block is first clicked.
 *
 * @author don_bruce
 */
public abstract class ATileEntityBase<JSONDefinition extends AJSONItem<? extends AJSONItem<?>.General>>{
	/**Current world for this TileEntity.**/
	public final WrapperWorld world;
	/**Current position of this TileEntity.**/
	public final Point3i position;
	/**JSON definition for this tileEntity.**/
	public final JSONDefinition definition;
	/**Current light level of the block for this TileEntity.  Defaults to 0, or no light.**/
	public float lightLevel;
	
	@SuppressWarnings("unchecked")
	public ATileEntityBase(WrapperWorld world, Point3i position, WrapperNBT data){
		this.world = world;
		this.position = position;
		this.definition = (JSONDefinition) MTSRegistry.packItemMap.get(data.getString("packID")).get(data.getString("systemName")).definition;
		this.lightLevel = (float) data.getDouble("lightLevel");
	}
	
	/**
	 *  Gets the block that's associated with this TileEntity.
	 */
	public ABlockBase getBlock(){
		return world.getBlock(position);
	}
	
	/**
	 *  Returns all items that make up this TE.  Used to spawn
	 *  items when broken.  Note that such items do NOT save
	 *  their NBT state.  Middle-clicking changes this.
	 */
	public List<AItemPack<? extends AJSONItem<? extends AJSONItem<?>.General>>> getDrops(){
		List<AItemPack<? extends AJSONItem<? extends AJSONItem<?>.General>>> drops = new ArrayList<AItemPack<? extends AJSONItem<? extends AJSONItem<?>.General>>>();
		if(definition != null){
			drops.add(MTSRegistry.packItemMap.get(definition.packID).get(definition.systemName));
		}
		return drops;
	}
	
	/**
	 *  Called when this TileEntity is removed from the world.
	 */
	public void remove(){}
	
	/**
	 *  Called when the TileEntity needs to be saved to disk.  The passed-in wrapper
	 *  should be written to at this point with any data needing to be saved.
	 */
	public void save(WrapperNBT data){
		data.setString("packID", definition.packID);
		data.setString("systemName", definition.systemName);
		data.setDouble("lightLevel", lightLevel);
	}

	
	/**
	 *  Called to get a render for this TE.  Only called on the client.
	 */
	public abstract ARenderTileEntityBase<? extends ATileEntityBase<JSONDefinition>> getRenderer();
}
