package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Sign;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;

/**Packet sent to poles to change their states.
 * 
 * @author don_bruce
 */
public class PacketTileEntityPoleChange extends APacketTileEntity<TileEntityPole>{
	private final ATileEntityPole_Component component;
	private final Axis axis;
	
	public PacketTileEntityPoleChange(TileEntityPole pole, ATileEntityPole_Component component, Axis axis){
		super(pole);
		this.component = component;
		this.axis = axis;
	}
	
	public PacketTileEntityPoleChange(ByteBuf buf){
		super(buf);
		this.axis = Axis.values()[buf.readByte()];
		String packID = readStringFromBuffer(buf);
		if(!packID.isEmpty()){
			String systemName = readStringFromBuffer(buf);
			this.component = TileEntityPole.createComponent((JSONPoleComponent) MTSRegistry.packItemMap.get(packID).get(systemName).definition);
			if(component instanceof TileEntityPole_Sign){
				for(byte i=0; i<component.definition.general.textLines.length; ++i){
					((TileEntityPole_Sign) component).textLines.add(readStringFromBuffer(buf));
				}
			}
			return;
		}
		this.component = null;
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(axis.ordinal());
		if(component != null){
			writeStringToBuffer(component.definition.packID, buf);
			writeStringToBuffer(component.definition.systemName, buf);
			if(component instanceof TileEntityPole_Sign){
				for(String textLine : ((TileEntityPole_Sign) component).textLines){
					writeStringToBuffer(textLine, buf);
				}
			}
		}else{
			writeStringToBuffer("", buf);
		}
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, TileEntityPole pole){
		//Check perms and do operations.
		if((!ConfigSystem.configObject.general.opSignEditingOnly.value || player.isOP()) || world.isClient()){
			if(component != null){
				pole.components.put(axis, component);
			}else{
				pole.components.remove(axis);
			}
			return true;
		}else{
			return false;
		}
	}
}
