package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.EntityConnection;
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
			for(EntityConnection connection : entity.towingConnections){
				if(connection.groupIndex == connectionGroupIndex){
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
					if(testEntity instanceof AEntityD_Interactable && shouldConnect(entity, (AEntityD_Interactable<?>) testEntity)){
						switch(((AEntityD_Interactable<?>) testEntity).checkIfCanConnect(entity, -1, connectionGroupIndex)){
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
						switch(entity.checkIfCanConnect((AEntityD_Interactable<?>) testEntity, connectionGroupIndex, -1)){
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
				entity.towedByConnection.otherEntity.disconnectTrailer(entity.towedByConnection.getInverse());
			}else{
				for(EntityConnection connection : entity.towingConnections){
					if(connection.groupIndex == connectionGroupIndex){
						entity.disconnectTrailer(connection);
						break;
					}
				}
			}
			player.sendPacket(new PacketPlayerChatMessage(player, "interact.trailer.disconnect"));
		}
		return false;
	}
	
	private static boolean shouldConnect(AEntityD_Interactable<?> entity1, AEntityD_Interactable<?> entity2){
		if(entity1.equals(entity2)){
			return false; //Entity is the same.
		}else if(entity1 instanceof AEntityE_Multipart && ((AEntityE_Multipart<?>) entity1).parts.contains(entity2)){
			return false; //Entity2 is a part on entity1.
		}else if(entity2 instanceof AEntityE_Multipart && ((AEntityE_Multipart<?>) entity2).parts.contains(entity1)){
			return false; //Entity1 is a part on entity2.
		}else{
			return true;
		} 
	}
}
