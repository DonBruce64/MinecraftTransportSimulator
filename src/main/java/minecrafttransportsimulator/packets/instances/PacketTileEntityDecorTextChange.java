package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityBeacon;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketTileEntity;

/**Packet sent to decors to update their their text lines.  This is sent from the
 * text GUI to servers to update the text, and then sent back to all clients for syncing.
 * 
 * @author don_bruce
 */
public class PacketTileEntityDecorTextChange extends APacketTileEntity<TileEntityDecor>{
	private final List<String> textLines;
	
	public PacketTileEntityDecorTextChange(TileEntityDecor decor, List<String> textLines){
		super(decor);
		this.textLines = textLines;
	}
	
	public PacketTileEntityDecorTextChange(ByteBuf buf){
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
	public boolean handle(IWrapperWorld world, IWrapperPlayer player, TileEntityDecor decor){
		if(decor instanceof TileEntityBeacon){
			((TileEntityBeacon) decor).updateBeaconToText(textLines);
		}else{
			int linesChecked = 0;
			for(Entry<JSONText, String> textEntry : decor.text.entrySet()){
				textEntry.setValue(textLines.get(linesChecked));
				++linesChecked;
			}
		}
		return true;
	}
}
