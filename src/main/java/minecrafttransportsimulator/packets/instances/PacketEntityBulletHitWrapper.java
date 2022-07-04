package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;

/**Packet sent when a bullet hits a wrapped (external) entity.
 * 
 * @author don_bruce
 */
public class PacketEntityBulletHitWrapper extends PacketEntityBulletHit{
	private final double damageAmount;
	private final UUID hitEntityID;
	private final UUID controllerEntityID;

	public PacketEntityBulletHitWrapper(EntityBullet bullet, IWrapperEntity hitEntity){
		super(bullet, hitEntity.getPosition());
		this.damageAmount = bullet.currentDamage.amount;
		this.hitEntityID = hitEntity.getID();
		this.controllerEntityID = bullet.gun.lastController != null ? bullet.gun.lastController.getID() : null;
	}
	
	public PacketEntityBulletHitWrapper(ByteBuf buf){
		super(buf);
		this.damageAmount = buf.readDouble();
		this.hitEntityID = readUUIDFromBuffer(buf);
		this.controllerEntityID = buf.readBoolean() ? readUUIDFromBuffer(buf) : null;
	}

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeDouble(damageAmount);
		writeUUIDToBuffer(hitEntityID, buf);
		buf.writeBoolean(controllerEntityID != null);
		if(controllerEntityID != null){
			writeUUIDToBuffer(controllerEntityID, buf);
		}
	}
	
	@Override
	public boolean handleBulletHit(AWrapperWorld world){
		if(!world.isClient()){
			IWrapperEntity entityHit = world.getExternalEntity(hitEntityID);
			if(entityHit != null){	
				//Create damage object and attack the entity.
				BoundingBox hitBox = new BoundingBox(hitPosition, bulletItem.definition.bullet.diameter*1000, bulletItem.definition.bullet.diameter*1000, bulletItem.definition.bullet.diameter*1000);
				IWrapperEntity attacker = controllerEntityID != null ? world.getExternalEntity(controllerEntityID) : null;
				LanguageEntry language = attacker != null ? JSONConfigLanguage.DEATH_BULLET_PLAYER : JSONConfigLanguage.DEATH_BULLET_NULL;
				Damage damage = new Damage(damageAmount, hitBox, null, attacker, language);
				damage.setBullet(bulletItem);
				entityHit.attack(damage);
			}
		}
		return false;
	}
}
