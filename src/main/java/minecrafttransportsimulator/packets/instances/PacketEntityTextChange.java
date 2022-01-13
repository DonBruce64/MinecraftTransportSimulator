package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet sent to entities to update their their text lines.  This is sent from the
 * text GUI to servers to update the text, and then sent back to all clients for syncing.
 * 
 * @author don_bruce
 */
public class PacketEntityTextChange extends APacketEntity<AEntityD_Definable<?>>{
	private final List<String> textLines;
	
	public PacketEntityTextChange(AEntityD_Definable<?> entity, List<String> textLines){
		super(entity);
		this.textLines = textLines;
	}
	
	public PacketEntityTextChange(ByteBuf buf){
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
	public boolean handle(WrapperWorld world, AEntityD_Definable<?> entity){
		entity.updateText(textLines);
		return true;
	}
}
