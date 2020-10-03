package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

/**Packet sent to vehicles to update their text.  This is sent from clients when they
 * click confirm in the text GUI.
 * 
 * @author don_bruce
 */
public class PacketVehicleTextChange extends APacketVehicle{
	private final List<String> textLines;
	
	public PacketVehicleTextChange(EntityVehicleF_Physics vehicle, List<String> textLines){
		super(vehicle);
		this.textLines = textLines;
	}
	
	public PacketVehicleTextChange(ByteBuf buf){
		super(buf);
		byte textLineCount = buf.readByte();
		this.textLines = new ArrayList<String>();
		for(byte i=0; i<textLineCount; ++i){
			textLines.add(readStringFromBuffer(buf));
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(textLines.size());
		for(String textLine : textLines){
			writeStringToBuffer(textLine, buf);
		}
	}
	
	@Override
	public boolean handle(IWrapperWorld world, IWrapperPlayer player, EntityVehicleF_Physics vehicle){
		int vehicleLines = 0;
		if(vehicle.definition.rendering.textObjects != null){
			for(int i=0; i<vehicle.definition.rendering.textObjects.size(); ++i){
				vehicle.textLines.set(i, textLines.get(i));
				++vehicleLines;
			}
		}
		for(APart part : vehicle.parts){
			if(part.definition.rendering != null && part.definition.rendering.textObjects != null){
				for(int i=0; i<part.definition.rendering.textObjects.size(); ++i){
					part.textLines.set(i, textLines.get(i + vehicleLines));
				}
			}
		}
		return true;
	}
}
