package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.OpMode;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.OpState;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;

public class PacketTileEntitySignalControllerChange extends APacketTileEntity<TileEntitySignalController>{
	private final OpMode currentOpMode;
	private final boolean mainDirectionXAxis;
	private final short greenMainTime;
	private final short greenCrossTime;
	private final short yellowMainTime;
	private final short yellowCrossTime;
	private final short allRedTime;
    private final List<Point3i> componentLocations;
	
	public PacketTileEntitySignalControllerChange(TileEntitySignalController controller){
		super(controller);
		this.currentOpMode = controller.currentOpMode;
		this.mainDirectionXAxis = controller.mainDirectionXAxis;
		this.greenMainTime = (short) controller.greenMainTime;
		this.greenCrossTime = (short) controller.greenCrossTime;
		this.yellowMainTime = (short) controller.yellowMainTime;
		this.yellowCrossTime = (short) controller.yellowCrossTime;
		this.allRedTime = (short) controller.allRedTime;
	    this.componentLocations = controller.componentLocations;
	}
	
	public PacketTileEntitySignalControllerChange(ByteBuf buf){
		super(buf);
		this.currentOpMode = OpMode.values()[buf.readByte()];
		this.mainDirectionXAxis = buf.readBoolean();
		this.greenMainTime = buf.readShort();
		this.greenCrossTime = buf.readShort();
		this.yellowMainTime = buf.readShort();
		this.yellowCrossTime = buf.readShort();
		this.allRedTime = buf.readShort();
		
		byte components = buf.readByte();
		this.componentLocations = new ArrayList<Point3i>();
		for(byte i=0; i<components; ++i){
			componentLocations.add(new Point3i(buf.readInt(), buf.readInt(), buf.readInt()));
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(currentOpMode.ordinal());
		buf.writeBoolean(mainDirectionXAxis);
		buf.writeShort(greenMainTime);
		buf.writeShort(greenCrossTime);
		buf.writeShort(yellowMainTime);
		buf.writeShort(yellowCrossTime);
		buf.writeShort(allRedTime);
		
		buf.writeByte(componentLocations.size());
		for(Point3i componentLocation : componentLocations){
			buf.writeInt(componentLocation.x);
			buf.writeInt(componentLocation.y);
			buf.writeInt(componentLocation.z);
		}
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, TileEntitySignalController controller){
		controller.currentOpMode = currentOpMode;
		controller.mainDirectionXAxis = mainDirectionXAxis;
		controller.greenMainTime = greenMainTime;
		controller.greenCrossTime = greenCrossTime;
		controller.yellowMainTime = yellowMainTime;
		controller.yellowCrossTime = yellowCrossTime;
		controller.allRedTime = allRedTime;
		controller.componentLocations.clear();
		controller.componentLocations.addAll(componentLocations);
		
		//Reset controller opstate.
		controller.changeState(OpState.GREEN_MAIN_RED_CROSS);
		return true;
	}
}
