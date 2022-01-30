package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Packet used for sending bullet hit information to and from the server.  The first instance of this packet
 * is sent from clients to the server when they detect that a bullet that was spawned by the client player
 * has hit something, be it an internal entity, external entity, or block.  Logic is performed here for the 
 * bullet hit action, and if the bullet hit action requires clients to create effects for the hit, a corresponding 
 * packet is sent back.
 * 
 * @author don_bruce
 */
public class PacketEntityBulletHit extends APacketEntity<PartGun>{
	protected final ItemBullet bulletItem;
	protected final Point3d hitPosition;
	private final int bulletNumber;

	public PacketEntityBulletHit(EntityBullet bullet, Point3d hitPosition){
		super(bullet.gun);
		this.bulletItem = bullet.getItem();
		this.hitPosition = hitPosition;
		this.bulletNumber = bullet.bulletNumber;
	}
	
	public PacketEntityBulletHit(ByteBuf buf){
		super(buf);
		this.bulletItem = PackParserSystem.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf), readStringFromBuffer(buf));
		this.hitPosition = readPoint3dFromBuffer(buf);
		this.bulletNumber = buf.readInt();
	}

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(bulletItem.definition.packID, buf);
		writeStringToBuffer(bulletItem.definition.systemName, buf);
		writeStringToBuffer(bulletItem.subName, buf);
		writePoint3dToBuffer(hitPosition, buf);
		buf.writeInt(bulletNumber);
	}
	
	@Override
	public boolean handle(WrapperWorld world, PartGun gun){
		if(!world.isClient()){
			//If the bullet hasn't been marked as hit yet, do hit logic.
			if(!gun.bulletsHitOnServer.contains(bulletNumber)){
				gun.bulletsHitOnServer.add(bulletNumber);
				if(bulletItem.definition.bullet.types.contains(BulletType.EXPLOSIVE)){
					float blastSize = bulletItem.definition.bullet.blastStrength == 0 ? bulletItem.definition.bullet.diameter/10F : bulletItem.definition.bullet.blastStrength;
					world.spawnExplosion(hitPosition, blastSize, bulletItem.definition.bullet.types.contains(BulletType.INCENDIARY));
					return false;
				}else{
					return handleBulletHit(world);
				}
			}
		}else{
			handleBulletHit(world);
		}
		return false;
	}
	
	/**
	 *  Helper handler for this abstract packet.
	 *  This handler checks to make sure that the bullet
	 *  isn't a duplicate hit, or is on the client, before being called.
	 *  Return method has the same behavior as {@link #handle(WrapperWorld, PartGun)}.
	 */
	public boolean handleBulletHit(WrapperWorld world){
		return false;
	}
}
