package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.TrailerConnection;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**Packet used to send signals to entities to attempt to connect/disconnect a trailer.  If connection
 * change is valid, then the a {@link PacketEntityTrailerChange} is sent to all clients.  
 * In all cases a chat message is sent back to the player who sent the packet initially for status notifications.
 * 
 * @author don_bruce
 */
public class PacketEntityTrailerConnection extends APacketEntityInteract<AEntityD_Interactable<?>, WrapperPlayer>{
	private final int connectionGroupIndex;
	private final boolean fromTrailer;
	private final boolean connect;
	
	public PacketEntityTrailerConnection(AEntityD_Interactable<?> entity, WrapperPlayer player, int connectionGroupIndex){
		super(entity, player);
		this.connectionGroupIndex = connectionGroupIndex;
		JSONConnectionGroup requestedGroup = entity.definition.connectionGroups.get(connectionGroupIndex);
		
		this.fromTrailer = requestedGroup.hookup;
		if(fromTrailer){
			this.connect = entity.towedByConnection == null;
		}else{
			boolean foundConnection = false;
			for(TrailerConnection connection : entity.towingConnections){
				if(connection.hitchGroupIndex == connectionGroupIndex){
					foundConnection = true;
				}
			}
			this.connect = !foundConnection;
		}
	}
	
	public PacketEntityTrailerConnection(ByteBuf buf){
		super(buf);
		this.connectionGroupIndex = buf.readInt();
		this.connect = buf.readBoolean();
		this.fromTrailer = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(connectionGroupIndex);
		buf.writeBoolean(connect);
		buf.writeBoolean(fromTrailer);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, AEntityD_Interactable<?> entity, WrapperPlayer player){
		if(connect){
			boolean matchingConnection = false;
			boolean trailerInRange = false;
			if(fromTrailer){
				for(AEntityA_Base testEntity : AEntityA_Base.getEntities(world)){
					if(testEntity instanceof AEntityD_Interactable && shouldConnect((AEntityD_Interactable<?>) testEntity, entity)){
						switch(((AEntityD_Interactable<?>) testEntity).checkIfTrailerCanConnect(entity, -1, connectionGroupIndex)){
							case TRAILER_CONNECTED : player.sendPacket(new PacketPlayerChatMessage(player, "interact.trailer.connect")); return false;
							case TRAILER_TOO_FAR : matchingConnection = true; break;
							case TRAILER_WRONG_HITCH : trailerInRange = true; break;
							case NO_TRAILER_NEARBY : break;
						}
					}
				}
			}else{
				for(AEntityA_Base testEntity : AEntityA_Base.getEntities(world)){
					if(testEntity instanceof AEntityD_Interactable  && shouldConnect(entity, (AEntityD_Interactable<?>) testEntity)){
						switch(entity.checkIfTrailerCanConnect((AEntityD_Interactable<?>) testEntity, connectionGroupIndex, -1)){
							case TRAILER_CONNECTED : player.sendPacket(new PacketPlayerChatMessage(player, "interact.trailer.connect")); return false;
							case TRAILER_TOO_FAR : matchingConnection = true; break;
							case TRAILER_WRONG_HITCH : trailerInRange = true; break;
							case NO_TRAILER_NEARBY : break;
						}
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
		}else{
			if(fromTrailer){
				entity.towedByConnection.hitchEntity.disconnectTrailer(entity.towedByConnection);
			}else{
				for(TrailerConnection connection : entity.towingConnections){
					if(connection.hitchGroupIndex == connectionGroupIndex){
						entity.disconnectTrailer(connection);
						player.sendPacket(new PacketPlayerChatMessage(player, "interact.trailer.disconnect"));
						break;
					}
				}
			}
		}
		return false;
	}
	
	private static boolean shouldConnect(AEntityD_Interactable<?> hitchEntity, AEntityD_Interactable<?> hookupEntity){
		if(hookupEntity.towedByConnection != null){
			return false; //Entity is already hooked up.
		}else if(hookupEntity.equals(hitchEntity)){
			return false; //Entity is the same.
		}else if(hookupEntity instanceof AEntityE_Multipart && ((AEntityE_Multipart<?>) hookupEntity).parts.contains(hitchEntity)){
			return false; //Hitch is a part on hookup.
		}else if(hitchEntity instanceof AEntityE_Multipart && ((AEntityE_Multipart<?>) hitchEntity).parts.contains(hookupEntity)){
			return false; //Hookup is a part on hitch.
		}else{
			//Check to make sure the hookupEntity isn't towing the hitchEntity.
			for(TrailerConnection connection : hookupEntity.towingConnections){
				if(connection.hookupEntity.equals(hitchEntity) || connection.hookupBaseEntity.equals(hitchEntity)){
					return false;
				}
			}
			return true;
		} 
	}
}
