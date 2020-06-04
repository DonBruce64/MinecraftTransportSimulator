package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.OpMode;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.OpState;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;

public class PacketTileEntitySignalControllerControlled extends APacketTileEntity<TileEntitySignalController>{
	private final OpState currentOpState;
	private final boolean lightsOn;
	
	public PacketTileEntitySignalControllerControlled(TileEntitySignalController controller){
		super(controller);
		this.currentOpState = controller.currentOpState;
		this.lightsOn = controller.lightsOn;
	}
	
	public PacketTileEntitySignalControllerControlled(ByteBuf buf){
		super(buf);
		this.currentOpState = OpState.values()[buf.readByte()];
		this.lightsOn = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(currentOpState.ordinal());
		buf.writeBoolean(lightsOn);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, TileEntitySignalController controller){
		controller.currentOpMode = OpMode.REMOTE_CONTROL;
		controller.lightsOn = lightsOn;
		controller.updateState(currentOpState, false);
		return true;
	}
}
