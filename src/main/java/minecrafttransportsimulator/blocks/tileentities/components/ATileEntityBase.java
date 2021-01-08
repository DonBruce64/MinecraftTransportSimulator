package minecrafttransportsimulator.blocks.tileentities.components;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.rendering.instances.ARenderTileEntityBase;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Base Tile Entity class.  In essence, this class holds the data and state of a Tile Entity in the world.
 * All TileEntities are used for making pack-based blocks, so they have JSON parameters
 * attached to them.  
 * <br><br>
 * Note that this constructor is called on the server when first creating the TE or loading it from disk,
 * but on the client this is called after the server sends over the saved data, not when the player first clicks.
 * Because of this, there there may be a slight delay in the TE showing up from when the block is first clicked.
 *
 * @author don_bruce
 */
public abstract class ATileEntityBase<JSONDefinition extends AJSONItem<?>>{
	/**Current world for this TileEntity.**/
	public final IWrapperWorld world;
	/**Current position of this TileEntity.**/
	public final Point3i position;
	/**Current position of this TileEntity in Point3d form.**/
	public final Point3d doublePosition;
	/**Y-axis rotation of this TileEntity when it was placed.**/
	public final double rotation;
	/**Current subName for this TileEntity.**/
	public String currentSubName;
	/**Item that that was used to spawn this TileEntity.**/
	public final AItemPack<JSONDefinition> item;
	/**JSON definition for this TileEntity.**/
	public final JSONDefinition definition;
	/**Current light level of the block for this TileEntity.  Defaults to 0, or no light.**/
	public float lightLevel;
	
	public ATileEntityBase(IWrapperWorld world, Point3i position, WrapperNBT data){
		this.world = world;
		this.position = position;
		this.doublePosition = new Point3d(position);
		this.rotation = data.getDouble("rotation");
		this.currentSubName = data.getString("currentSubName");
		this.item = PackParserSystem.getItem(data.getString("packID"), data.getString("systemName"), currentSubName);
		this.definition = item.definition;
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
	public List<AItemPack<JSONDefinition>> getDrops(){
		List<AItemPack<JSONDefinition>> drops = new ArrayList<AItemPack<JSONDefinition>>();
		drops.add(item);
		return drops;
	}
	
	/**
	 *  Called when this TileEntity is removed from the world.
	 */
	public void remove(){}
	
	/**
	 *  Called to get a render for this TE.  Only called on the client.
	 */
	public abstract ARenderTileEntityBase<? extends ATileEntityBase<JSONDefinition>> getRenderer();
	
	/**
	 *  Called when the TileEntity needs to be saved to disk.  The passed-in wrapper
	 *  should be written to at this point with any data needing to be saved.
	 */
	public void save(WrapperNBT data){
		data.setDouble("rotation", rotation);
		data.setString("packID", definition.packID);
		data.setString("systemName", definition.systemName);
		data.setString("currentSubName", currentSubName);
		data.setDouble("lightLevel", lightLevel);
	}
}
