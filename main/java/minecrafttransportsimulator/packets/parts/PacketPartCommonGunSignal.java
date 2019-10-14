package minecrafttransportsimulator.packets.parts;

import minecrafttransportsimulator.mcinterface.MTSNetwork;
import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.packets.vehicles.APacketVehiclePart;
import minecrafttransportsimulator.vehicles.parts.APartGun;
import net.minecraft.nbt.NBTTagCompound;


/**This packet is sent to servers when a player starts and stops
 * pulling the trigger on a gun.  Contains the playerID to let
 * the server know is pulling the trigger.  Once the server
 * processes this packet it will send it back to all clients
 * to let them adjust their states. 
 * 
 * @author don_bruce
 */
public class PacketPartCommonGunSignal extends APacketVehiclePart{
	private int playerControllerID;
	private boolean firing;

	public PacketPartCommonGunSignal(){}
	
	public PacketPartCommonGunSignal(APartGun gun, int playerControllerID, boolean firing){
		super(gun);
		this.playerControllerID = playerControllerID;
		this.firing = firing;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		playerControllerID = tag.getInteger("playerControllerID");
		firing = tag.getBoolean("firing");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setInteger("playerControllerID", playerControllerID);
		tag.setBoolean("firing", firing);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		APartGun gun = (APartGun) getPart(world);
		gun.playerControllerID = playerControllerID;
		gun.firing = firing;
		if(onServer){
			MTSNetwork.sendPacketToPlayersTracking(this, gun.vehicle);
		}
	}
}
