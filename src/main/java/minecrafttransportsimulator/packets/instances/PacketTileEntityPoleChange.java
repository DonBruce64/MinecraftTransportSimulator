package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet sent to poles to change their states.  This gets sent when a player clicks a pole on the server
 * and a component is added or removed from the pole.
 * 
 * @author don_bruce
 */
public class PacketTileEntityPoleChange extends APacketEntity<TileEntityPole>{
	private final Axis axis;
	private final WrapperNBT data;
	
	
	public PacketTileEntityPoleChange(TileEntityPole pole, Axis axis, WrapperNBT data){
		super(pole);
		this.axis = axis;
		this.data = data;
	}
	
	public PacketTileEntityPoleChange(ByteBuf buf){
		super(buf);
		this.axis = Axis.values()[buf.readByte()];
		if(buf.readBoolean()){
			this.data = readDataFromBuffer(buf);
		}else{
			this.data = null;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(axis.ordinal());
		if(data != null){
			buf.writeBoolean(true);
			writeDataToBuffer(data, buf);
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	protected boolean handle(WrapperWorld world, TileEntityPole pole){
		if(data != null){
			//Player clicked with a component.  Add it.
			pole.components.put(axis, PoleComponentType.createComponent(pole, axis, data));
		}else{
			pole.components.remove(axis).remove();
		}
		pole.updateLightState();
		return false;
	}
}