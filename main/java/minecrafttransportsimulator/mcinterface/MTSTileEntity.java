package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.baseclasses.Location;
import minecrafttransportsimulator.baseclasses.Point;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**Class that is used by MTS for all TileEntity operations.  This provides a standard set of
 * methods for interacting with tile entities, so all tile entities in MTS should extend this
 * class to ensure they allow use of those methods.
 * 
 * @author don_bruce
 */
public abstract class MTSTileEntity extends TileEntity{

	//---------------START OF FORWARDED METHODS---------------//
	@Override
	public void readFromNBT(NBTTagCompound tag){
		super.readFromNBT(tag);
		handleLoad(tag);
	}
	/**Called when the entity is being told to load itself from NBT.*/
	protected abstract void handleLoad(NBTTagCompound tag);
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag){
		return handleSave(super.writeToNBT(tag));
	}
	/**Called when the entity is being told to save itself to NBT.*/
	protected abstract NBTTagCompound handleSave(NBTTagCompound tag);
	
	
	
	//---------------START OF CUSTOM METHODS---------------//
	/**Gets the point this TE is at.*/
	public Location getLocation(){
		return new Location(this.pos.getX(), this.pos.getY(), this.pos.getZ());
	}
}
