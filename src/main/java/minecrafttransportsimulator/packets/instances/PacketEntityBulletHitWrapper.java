package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.systems.ConfigSystem;

/**Packet sent when a bullet hits a wrapped (external) entity.
 * 
 * @author don_bruce
 */
public class PacketEntityBulletHitWrapper extends PacketEntityBulletHit{
	private final double bulletVelocity;
	private final UUID hitEntityID;
	private final UUID controllerEntityID;

	public PacketEntityBulletHitWrapper(EntityBullet bullet, WrapperEntity hitEntity){
		super(bullet, hitEntity.getPosition());
		this.bulletVelocity = bullet.velocity;
		this.hitEntityID = hitEntity.getID();
		this.controllerEntityID = bullet.gun.lastController != null ? bullet.gun.lastController.getID() : null;
	}
	
	public PacketEntityBulletHitWrapper(ByteBuf buf){
		super(buf);
		this.bulletVelocity = buf.readDouble();
		this.hitEntityID = readUUIDFromBuffer(buf);
		this.controllerEntityID = buf.readBoolean() ? readUUIDFromBuffer(buf) : null;
	}

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeDouble(bulletVelocity);
		writeUUIDToBuffer(hitEntityID, buf);
		buf.writeBoolean(controllerEntityID != null);
		if(controllerEntityID != null){
			writeUUIDToBuffer(controllerEntityID, buf);
		}
	}
	
	@Override
	public boolean handleBulletHit(WrapperWorld world){
		if(!world.isClient()){
			WrapperEntity entityHit = world.getExternalEntity(hitEntityID);
			if(entityHit != null){	
				//Create damage object and attack the entity.
				BoundingBox hitBox = new BoundingBox(hitPosition, bulletItem.definition.bullet.diameter*1000, bulletItem.definition.bullet.diameter*1000, bulletItem.definition.bullet.diameter*1000);
				WrapperEntity attacker = controllerEntityID != null ? world.getExternalEntity(controllerEntityID) : null;
				double damageAmount = bulletVelocity*bulletItem.definition.bullet.diameter/5D*ConfigSystem.configObject.damage.bulletDamageFactor.value;
				Damage damage = new Damage("bullet", damageAmount, hitBox, null, attacker);
				damage.setBullet(bulletItem);
				entityHit.attack(damage);
			}
		}
		return false;
	}
}
