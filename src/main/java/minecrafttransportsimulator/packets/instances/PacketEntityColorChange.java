package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Packet sent to entities to update their their subName (color).  This gets sent from
 * a client when they change the color in the paint gun GUI.  Change is applied to the
 * entity and all parts (if applicable).
 * 
 * @author don_bruce
 */
public class PacketEntityColorChange extends APacketEntityInteract<AEntityC_Definable<?>, WrapperPlayer>{
	private final AItemSubTyped<?> newItem;
	
	public PacketEntityColorChange(AEntityC_Definable<?> entity, WrapperPlayer player, AItemSubTyped<?> newItem){
		super(entity, player);
		this.newItem = newItem;
	}
	
	public PacketEntityColorChange(ByteBuf buf){
		super(buf);
		this.newItem = (AItemSubTyped<?>) PackParserSystem.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf), readStringFromBuffer(buf));
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(newItem.definition.packID, buf);
		writeStringToBuffer(newItem.definition.systemName, buf);
		writeStringToBuffer(newItem.subName, buf);
	}
	
	@Override
	public boolean handle(WrapperWorld world, AEntityC_Definable<?> entity, WrapperPlayer player){
		WrapperInventory inventory = player.getInventory();
		if(player.isCreative() || inventory.hasMaterials(newItem, false, true, false)){
			//Remove livery materials (if required) and set new subName.
			if(!player.isCreative()){
				inventory.removeMaterials(newItem, false, true, false);
			}
			entity.subName = newItem.subName;
			
			//If we have parts, and have a second tone, change parts to match if possible.
			if(entity instanceof AEntityE_Multipart){
				for(APart part : ((AEntityE_Multipart<?>) entity).parts){
					((AEntityE_Multipart<?>) entity).updatePartTone(part);
				}
			}
			return true;
		}
		return false;
	}
}
