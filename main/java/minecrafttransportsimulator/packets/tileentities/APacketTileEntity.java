package minecrafttransportsimulator.packets.tileentities;

import minecrafttransportsimulator.baseclasses.Location;
import minecrafttransportsimulator.mcinterface.MTSNetwork.MTSPacket;
import minecrafttransportsimulator.mcinterface.MTSTileEntity;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import net.minecraft.nbt.NBTTagCompound;


/**Base packet for tile entity interaction.  Contains the location of the
 * tile entity, as well as a helper method for getting it from the world.
 * 
 * @author don_bruce
 */
public abstract class APacketTileEntity extends MTSPacket{
	private Location location;

	public APacketTileEntity(){}
	
	public APacketTileEntity(MTSTileEntity tile){
		this.location = tile.getLocation();
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		location = new Location(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		tag.setInteger("x", location.x);
		tag.setInteger("y", location.y);
		tag.setInteger("z", location.z);
	}
	
	protected MTSTileEntity getTileEntity(MTSWorldInterface world){
		return world.getTileEntity(location);
	}
}
