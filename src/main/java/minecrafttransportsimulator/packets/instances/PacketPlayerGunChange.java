package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityPlayerGun;

/**Packet used for sending the gun system a notification that the player changed their gun.
 * This triggers the gun to change on all clients it is sent to to sync with the server.
 * We need to send this packet as we can't change the gun without changing the ID, but
 * since the ID is part of the gun, and that's created on-demand, we won't have that data
 * without saving and getting NBT changes.  Normally gun IDs are statically-linked to things,
 * but in the case of the player gun entity, the gun changes every cycle rather than us
 * just spawning a new entity every time the player changes their gun.
 * 
 * @author don_bruce
 */
public class PacketPlayerGunChange extends APacketEntity{
	private final int gunID;
	
	public PacketPlayerGunChange(EntityPlayerGun entity){
		super(entity);
		this.gunID = entity.gun.gunID;
	}
	
	public PacketPlayerGunChange(ByteBuf buf){
		super(buf);
		this.gunID = buf.readInt();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(gunID);
	}
	
	@Override
	protected boolean handle(IWrapperWorld world, IWrapperPlayer player, AEntityBase entity){
		((EntityPlayerGun) entity).createNewGun(gunID);
		return true;
	}
}
