package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityPlayerGun;

/**Packet used for sending the firing state of player guns to player gun entities.
 * 
 * @author don_bruce
 */
public class PacketPlayerGunFiring extends APacketEntity{
	private final boolean fireCommand;
	
	public PacketPlayerGunFiring(EntityPlayerGun entity, boolean fireCommand){
		super(entity);
		this.fireCommand = fireCommand;
	}
	
	public PacketPlayerGunFiring(ByteBuf buf){
		super(buf);
		this.fireCommand = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeBoolean(fireCommand);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, AEntityBase entity){
		((EntityPlayerGun) entity).fireCommand = fireCommand;
		return true;
	}
}
