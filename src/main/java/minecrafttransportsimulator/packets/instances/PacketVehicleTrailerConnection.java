package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**Packet used to send signals to vehicles to attempt to connect/disconnect a trailer.  If connection
 * change is valid, the {@link PacketVehicleTrailerChange} is called.
 * 
 * @author don_bruce
 */
public class PacketVehicleTrailerConnection extends APacketEntityInteract<EntityVehicleF_Physics, WrapperPlayer>{
	
	public PacketVehicleTrailerConnection(EntityVehicleF_Physics vehicle, WrapperPlayer player){
		super(vehicle, player);
	}
	
	public PacketVehicleTrailerConnection(ByteBuf buf){
		super(buf);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, EntityVehicleF_Physics vehicle, WrapperPlayer player){
		if(vehicle.towedVehicle != null){
			vehicle.changeTrailer(null, null, null, null, null);
			player.sendPacket(new PacketPlayerChatMessage(player, "interact.trailer.disconnect"));
		}else{
			boolean matchingConnection = false;
			boolean trailerInRange = false;
			for(AEntityA_Base entity : AEntityA_Base.getEntities(world)){
				if(entity instanceof EntityVehicleF_Physics && !entity.equals(vehicle)){
					switch(vehicle.tryToConnect((EntityVehicleF_Physics) entity)){
						case TRAILER_CONNECTED : player.sendPacket(new PacketPlayerChatMessage(player, "interact.trailer.connect")); return false;
						case TRAILER_TOO_FAR : matchingConnection = true; break;
						case TRAILER_WRONG_HITCH : trailerInRange = true; break;
						case NO_TRAILER_NEARBY : break;
					}
				}
			}
			
			//Send packet based on what we found.
			if(!matchingConnection && !trailerInRange){
				player.sendPacket(new PacketPlayerChatMessage(player, "interact.trailer.notfound"));
			}else if(matchingConnection && !trailerInRange){
				player.sendPacket(new PacketPlayerChatMessage(player, "interact.trailer.toofar"));
			}else if(!matchingConnection && trailerInRange){
				player.sendPacket(new PacketPlayerChatMessage(player, "interact.trailer.wronghitch"));
			}else{
				player.sendPacket(new PacketPlayerChatMessage(player, "interact.trailer.wrongplacement"));
			}					
		}
		return false;
	}
}
