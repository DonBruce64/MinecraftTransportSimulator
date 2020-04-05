package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityTrafficSignalController;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityTrafficSignalController.OpMode;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityTrafficSignalController.OpState;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;

public class PacketTrafficSignalControllerChange extends APacketTileEntity<TileEntityTrafficSignalController>{
	private final OpMode currentOpMode;
	private final boolean mainDirectionXAxis;
	private final short greenMainTime;
	private final short greenCrossTime;
	private final short yellowMainTime;
	private final short yellowCrossTime;
	private final short allRedTime;
    private final List<Point3i> trafficSignalLocations;
    private final List<Point3i> crossingSignalLocations;
	
	public PacketTrafficSignalControllerChange(TileEntityTrafficSignalController controller){
		super(controller);
		this.currentOpMode = controller.currentOpMode;
		this.mainDirectionXAxis = controller.mainDirectionXAxis;
		this.greenMainTime = (short) controller.greenMainTime;
		this.greenCrossTime = (short) controller.greenCrossTime;
		this.yellowMainTime = (short) controller.yellowMainTime;
		this.yellowCrossTime = (short) controller.yellowCrossTime;
		this.allRedTime = (short) controller.allRedTime;
	    this.trafficSignalLocations = controller.trafficSignalLocations;
	    this.crossingSignalLocations = controller.crossingSignalLocations;
	}
	
	public PacketTrafficSignalControllerChange(ByteBuf buf){
		super(buf);
		this.currentOpMode = OpMode.values()[buf.readByte()];
		this.mainDirectionXAxis = buf.readBoolean();
		this.greenMainTime = buf.readShort();
		this.greenCrossTime = buf.readShort();
		this.yellowMainTime = buf.readShort();
		this.yellowCrossTime = buf.readShort();
		this.allRedTime = buf.readShort();
		
		byte trafficSignals = buf.readByte();
		this.trafficSignalLocations = new ArrayList<Point3i>();
		for(byte i=0; i<trafficSignals; ++i){
			trafficSignalLocations.add(new Point3i(buf.readInt(), buf.readInt(), buf.readInt()));
		}
		
		byte crossingSignals = buf.readByte();
		this.crossingSignalLocations = new ArrayList<Point3i>();
		for(byte i=0; i<crossingSignals; ++i){
			crossingSignalLocations.add(new Point3i(buf.readInt(), buf.readInt(), buf.readInt()));
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
		
		buf.writeByte(trafficSignalLocations.size());
		for(Point3i signalLocation : trafficSignalLocations){
			buf.writeInt(signalLocation.x);
			buf.writeInt(signalLocation.y);
			buf.writeInt(signalLocation.z);
		}
		
		buf.writeByte(crossingSignalLocations.size());
		for(Point3i signalLocation : crossingSignalLocations){
			buf.writeInt(signalLocation.x);
			buf.writeInt(signalLocation.y);
			buf.writeInt(signalLocation.z);
		}
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, TileEntityTrafficSignalController controller){
		controller.currentOpMode = currentOpMode;
		controller.mainDirectionXAxis = mainDirectionXAxis;
		controller.greenMainTime = greenMainTime;
		controller.greenCrossTime = greenCrossTime;
		controller.yellowMainTime = yellowMainTime;
		controller.yellowCrossTime = yellowCrossTime;
		controller.allRedTime = allRedTime;
		controller.trafficSignalLocations.clear();
		controller.trafficSignalLocations.addAll(trafficSignalLocations);
		controller.crossingSignalLocations.clear();
		controller.crossingSignalLocations.addAll(crossingSignalLocations);
		
		//Reset controller opstate.
		controller.changeState(OpState.GREEN_MAIN_RED_CROSS);
		return true;
	}
}
