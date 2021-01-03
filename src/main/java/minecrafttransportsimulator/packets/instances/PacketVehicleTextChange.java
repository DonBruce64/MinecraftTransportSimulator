package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.jsondefs.JSONText;
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
		int linesChecked = 0;
		for(Entry<JSONText, String> textEntry : vehicle.text.entrySet()){
			textEntry.setValue(textLines.get(linesChecked));
			++linesChecked;
		}
		for(APart part : vehicle.parts){
			for(Entry<JSONText, String> textEntry : part.text.entrySet()){
				textEntry.setValue(textLines.get(linesChecked));
				++linesChecked;
			}
		}
		return true;
	}
}
