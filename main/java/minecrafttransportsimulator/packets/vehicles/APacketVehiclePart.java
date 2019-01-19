package minecrafttransportsimulator.packets.vehicles;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;

public abstract class APacketVehiclePart extends APacketVehicle{
	protected double offsetX;
	protected double offsetY;
	protected double offsetZ;

	public APacketVehiclePart(){}
	
	public APacketVehiclePart(EntityVehicleA_Base vehicle, double offsetX, double offsetY, double offsetZ){
		super(vehicle);
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.offsetX = buf.readDouble();
		this.offsetY = buf.readDouble();
		this.offsetZ = buf.readDouble();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeDouble(this.offsetX);
		buf.writeDouble(this.offsetY);
		buf.writeDouble(this.offsetZ);
	}
}
