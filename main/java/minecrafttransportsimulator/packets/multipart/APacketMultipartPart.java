package minecrafttransportsimulator.packets.multipart;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.multipart.main.EntityMultipartA_Base;

public abstract class APacketMultipartPart extends APacketMultipart{
	protected double offsetX;
	protected double offsetY;
	protected double offsetZ;

	public APacketMultipartPart(){}
	
	public APacketMultipartPart(EntityMultipartA_Base multipart, double offsetX, double offsetY, double offsetZ){
		super(multipart);
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
