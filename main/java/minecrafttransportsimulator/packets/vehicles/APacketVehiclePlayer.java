package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import net.minecraft.nbt.NBTTagCompound;

/**Base packet used for vehicle-player interaction.  This packet
 * gets sent with the vehicle packet as a super to contain the vehicle
 * id.  If part interaction is needed, use APackerVehiclePartPlayer.
 * 
 * @author don_bruce
 */
public abstract class APacketVehiclePlayer extends APacketVehicle{
	private int playerID;

	public APacketVehiclePlayer(){}
	
	public APacketVehiclePlayer(EntityVehicleA_Base vehicle, int playerID){
		super(vehicle);
		this.playerID = playerID;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		playerID = tag.getInteger("playerID");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setInteger("playerID", playerID);
	}
	
	protected MTSPlayerInterface getPlayer(MTSWorldInterface world){
		return new MTSPlayerInterface(world.getEntity(playerID));
	}
}
