package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet used to increment variable states.  Sent from clients to servers to
 * tell them to change the custom state of an entity variable, and then sent
 * back to all clients to have them update those states.  May also be sent directly
 * from a server to all clients if the server is the one that changed the state.
 * 
 * @author don_bruce
 */
public class PacketEntityVariableIncrement extends APacketEntity<AEntityC_Definable<?>>{
	private final String variableName;
	private final double variableValue;
	private final double minValue;
	private final double maxValue;
	
	public PacketEntityVariableIncrement(AEntityC_Definable<?> entity, String variableName, double variableValue, double minValue, double maxValue){
		super(entity);
		this.variableName = variableName;
		this.variableValue = variableValue;
		this.minValue = minValue;
		this.maxValue = maxValue;
	}
	
	public PacketEntityVariableIncrement(AEntityC_Definable<?> entity, String variableName, double variableValue){
		this(entity, variableName, variableValue, -Double.MAX_VALUE, Double.MAX_VALUE);
	}
	
	public PacketEntityVariableIncrement(ByteBuf buf){
		super(buf);
		this.variableName = readStringFromBuffer(buf);
		this.variableValue = buf.readDouble();
		this.minValue = buf.readDouble();
		this.maxValue = buf.readDouble();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(variableName, buf);
		buf.writeDouble(variableValue);
		buf.writeDouble(minValue);
		buf.writeDouble(maxValue);
	}
	
	@Override
	public boolean handle(WrapperWorld world, AEntityC_Definable<?> entity){
		double newValue = entity.getVariable(variableName) + variableValue;
		if(newValue >= minValue & newValue <= maxValue){
			entity.setVariable(variableName, newValue);
			return true;
		}else{
			return false;
		}
	}
}
