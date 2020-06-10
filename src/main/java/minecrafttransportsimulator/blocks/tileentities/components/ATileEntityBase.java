package minecrafttransportsimulator.blocks.tileentities.components;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.rendering.instances.ARenderTileEntityBase;
import minecrafttransportsimulator.wrappers.WrapperNBT;
import minecrafttransportsimulator.wrappers.WrapperTileEntity;
import minecrafttransportsimulator.wrappers.WrapperWorld;

/**Base Tile Entity class.  This type is used in the constructor of {@link WrapperTileEntity} to allow us to use
 * completely custom code that is not associated with MC's standard Tile Entity code.  Allows us to only
 * update the wrapper rather than the whole Tile Entity. In essence, this class holds the data and state of the
 * Tile Entity, while the wrapper ensures that the state gets saved to disk and the appropriate render gets
 * called.  Note that all TileEntities are used for making pack-based blocks, so they have JSON parameters
 * attached to them.
 *
 * @author don_bruce
 */
public abstract class ATileEntityBase<JSONDefinition extends AJSONItem<? extends AJSONItem<?>.General>>{
	/**Current world for this TileEntity.  May be null in some cases right after construction.**/
	public WrapperWorld world;
	/**Current position of this TileEntity.  Set both manually and during loading from world.**/
	public Point3i position;
	/**Current light level of the block for this TileEntity.  Defaults to 0, or no light.**/
	public float lightLevel;
	/**JSON definition for this tileEntity.  Private to allow getter/setter post-definition assignment logic.**/
	private JSONDefinition definition;
	
	/**
	 *  Gets the block that's associated with this TileEntity.
	 */
	public ABlockBase getBlock(){
		return world.getBlock(position);
	}
	
	/**
	 *  Returns the definition for this TileEntity.
	 */
	public JSONDefinition getDefinition(){
		return definition;
	}
	
	/**
	 *  Sets the definition for this TileEntity.
	 */
	public void setDefinition(JSONDefinition definition){
		this.definition = definition;
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
	 *  Called when the TileEntity is loaded from saved data.  This method is called
	 *  on the server when loading from disk, but it's not called on the client until
	 *  the client gets the supplemental data packet with the rest of the data.
	 *  Because of this, things may be null on client-sided construction time!
	 */
	@SuppressWarnings("unchecked")
	public void load(WrapperNBT data){
		String packID = data.getString("packID");
		String systemName = data.getString("systemName");
		if(!packID.isEmpty()){
			setDefinition((JSONDefinition) MTSRegistry.packItemMap.get(packID).get(systemName).definition);
			lightLevel = (float) data.getDouble("lightLevel");
		}
	}
	
	/**
	 *  Called when the TileEntity needs to be saved to disk.  The passed-in wrapper
	 *  should be written to at this point with any data needing to be saved.
	 */
	public void save(WrapperNBT data){
		if(definition != null){
			data.setString("packID", definition.packID);
			data.setString("systemName", definition.systemName);
			data.setDouble("lightLevel", lightLevel);
		}
	}

	
	/**
	 *  Called to get a render for this TE.  Only called on the client.
	 */
	public abstract ARenderTileEntityBase<? extends ATileEntityBase<JSONDefinition>, ? extends IBlockTileEntity<JSONDefinition>> getRenderer();
}
