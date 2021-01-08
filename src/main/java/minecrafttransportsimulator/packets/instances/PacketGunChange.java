package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Gun;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.packets.components.NetworkSystem;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Packet used to send signals to guns.  This can be either to start/stop the firing of the gun,
 * or to re-load the gun with the specified bullets.  If we are doing start/stop commands, then
 * this packet first gets sent to the server from the client who requested the command.  After this,
 * it is send to all players tracking the gun.  If this packet is for re-loading bullets, then it will
 * only appear on clients after the server has verified the bullets can in fact be loaded.
 * 
 * @author don_bruce
 */
public class PacketGunChange extends APacketBase{
	private final int gunID;
	private final boolean controlPulse;
	private final boolean triggerState;
	private final String bulletPackID;
	private final String bulletSystemName;
	
	public PacketGunChange(Gun gun, boolean triggerState){
		super(null);
		this.gunID = gun.gunID;
		this.controlPulse = true;
		this.triggerState = triggerState;
		this.bulletPackID = null;
		this.bulletSystemName = null;
	}
	
	public PacketGunChange(Gun gun, ItemPart bullet){
		super(null);
		this.gunID = gun.gunID;
		this.controlPulse = false;
		this.triggerState = false;
		this.bulletPackID = bullet.definition.packID;
		this.bulletSystemName = bullet.definition.systemName;
	}
	
	public PacketGunChange(ByteBuf buf){
		super(buf);
		this.gunID = buf.readInt();
		this.controlPulse = buf.readBoolean();
		if(controlPulse){
			this.triggerState = buf.readBoolean();
			this.bulletPackID = null;
			this.bulletSystemName = null;
		}else{
			this.triggerState = false;
			this.bulletPackID = readStringFromBuffer(buf);
			this.bulletSystemName = readStringFromBuffer(buf);
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(gunID);
		buf.writeBoolean(controlPulse);
		if(controlPulse){
			buf.writeBoolean(triggerState);
		}else{
			writeStringToBuffer(bulletPackID, buf);
			writeStringToBuffer(bulletSystemName, buf);
		}
	}
	
	@Override
	public void handle(IWrapperWorld world, IWrapperPlayer player){
		Gun gun = world.isClient() ? Gun.createdClientGuns.get(gunID) : Gun.createdServerGuns.get(gunID);
		if(gun != null){
			if(controlPulse){
				gun.firing = triggerState && gun.active;
			}else{
				gun.tryToReload(PackParserSystem.getItem(bulletPackID, bulletSystemName));
			}
			if(!world.isClient()){
				NetworkSystem.sendToAllClients(this);
			}
		}else{
			System.out.println("NULL GUN" + gunID);
		}
	}
}
