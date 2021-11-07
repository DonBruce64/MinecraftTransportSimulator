package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet used to toggle variable states.  Sent from clients to servers to
 * tell them to change the custom state of an entity variable, and then sent
 * back to all clients to have them update those states.
 * 
 * @author don_bruce
 */
public class PacketEntityVariableToggle extends APacketEntity<AEntityC_Definable<?>>{
	private final String variableName;
	
	public PacketEntityVariableToggle(AEntityC_Definable<?> entity, String variableName){
		super(entity);
		this.variableName = variableName;
	}
	
	public PacketEntityVariableToggle(ByteBuf buf){
		super(buf);
		this.variableName = readStringFromBuffer(buf);
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(variableName, buf);
	}
	
	@Override
	public boolean handle(WrapperWorld world, AEntityC_Definable<?> entity){
		entity.toggleVariable(variableName);
		return true;
	}
}
