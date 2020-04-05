package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleSign;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;

/**Packet sent to pumps on clients to change what vehicle they are connected to.
 * 
 * @author don_bruce
 */
public class PacketSignChange extends APacketTileEntity<TileEntityPoleSign>{
	private final String packID;
	private final String systemName;
	private final List<String> text;
	
	public PacketSignChange(TileEntityPoleSign sign){
		super(sign);
		this.packID = sign.definition.packID;
		this.systemName = sign.definition.systemName;
		this.text = sign.text;
	}
	
	public PacketSignChange(ByteBuf buf){
		super(buf);
		this.packID = readStringFromBuffer(buf);
		this.systemName = readStringFromBuffer(buf);
		byte textLines = buf.readByte();
		this.text = new ArrayList<String>();
		for(byte i=0; i<textLines; ++i){
			text.add(readStringFromBuffer(buf));
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(packID, buf);
		writeStringToBuffer(systemName, buf);
		buf.writeByte(text.size());
		for(String text : text){
			writeStringToBuffer(text, buf);
		}
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, TileEntityPoleSign sign){
		if((!ConfigSystem.configObject.general.opSignEditingOnly.value || player.isOP()) || world.isClient()){
			sign.definition = MTSRegistry.packSignMap.get(packID).get(systemName);
			sign.text = text;
			return true;
		}else{
			return false;
		}
	}
}
