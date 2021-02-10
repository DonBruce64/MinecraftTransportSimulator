package minecrafttransportsimulator.blocks.tileentities.components;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.AEntityC_Definable;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Base Tile Entity class.  In essence, this class holds the data and state of a Tile Entity in the world.
 * All TileEntities are used for making pack-based blocks, so they have JSON parameters
 * attached to them.  
 * <br><br>
 * Note that this constructor is called on the server when first creating the TE or loading it from disk,
 * but on the client this is called after the server sends over the saved data, not when the player first clicks.
 * Because of this, there there may be a slight delay in the TE showing up from when the block is first clicked.
 * Also note that the position of the TE is set by the constructor.  This is because TEs have their positions
 * set when they are created by the setting of a block.
 *
 * @author don_bruce
 */
public abstract class ATileEntityBase<JSONDefinition extends AJSONMultiModelProvider> extends AEntityC_Definable<JSONDefinition>{
	/**Current light level of the block for this TileEntity.  Defaults to 0, or no light.**/
	public float lightLevel;
	
	public ATileEntityBase(WrapperWorld world, Point3d position, WrapperNBT data){
		super(world, null, data);
		this.position.setTo(position);
		this.lightLevel = (float) data.getDouble("lightLevel");
		
		//TODO remove when packs have converted, as we previously used these fields on TEs.
		if(subName.isEmpty()){
			subName = data.getString("currentSubName");
		}
		if(rotation.y == 0){
			rotation.y = data.getDouble("rotation");
		}
	}
	
	/**
	 *  Returns all items that make up this TE.  Used to spawn
	 *  items when broken.  Note that such items do NOT save
	 *  their NBT state.  Middle-clicking changes this.
	 */
	public List<AItemPack<JSONDefinition>> getDrops(){
		List<AItemPack<JSONDefinition>> drops = new ArrayList<AItemPack<JSONDefinition>>();
		drops.add(getItem());
		return drops;
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		data.setDouble("lightLevel", lightLevel);
	}
}
