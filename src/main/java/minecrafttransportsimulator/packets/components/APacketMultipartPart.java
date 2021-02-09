package minecrafttransportsimulator.packets.components;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.AEntityE_Multipart;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Packet class that includes a default implementation for transmitting a locator
 * for a part on an entity.  Part may or may not exist; only the location is assured.
 *
 * @author don_bruce
 */
public abstract class APacketMultipartPart extends APacketEntity<AEntityE_Multipart<?>>{
	private final Point3d offset;
	
	public APacketMultipartPart(AEntityE_Multipart<?> entity, Point3d offset){
		super(entity);
		this.offset = offset; 
	}
	
	public APacketMultipartPart(ByteBuf buf){
		super(buf);
		this.offset = readPoint3dFromBuffer(buf);
	};

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writePoint3dToBuffer(offset, buf);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, AEntityE_Multipart<?> entity){
		return handle(world, player, entity, offset);
	}
	
	/**
	 *  Handler method with an extra parameter for the location of the part we are interacting with.
	 *  Supplements {{@link #handle(WrapperWorld, WrapperPlayer, AEntityE_Multipart)}
	 */
	protected abstract boolean handle(WrapperWorld world, WrapperPlayer player, AEntityE_Multipart<?> entity, @SuppressWarnings("hiding") Point3d offset);
}
