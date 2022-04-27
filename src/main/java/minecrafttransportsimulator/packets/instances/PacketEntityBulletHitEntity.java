package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.systems.ConfigSystem;

/**Packet sent when a bullet hits a normal entity.
 * 
 * @author don_bruce
 */
public class PacketEntityBulletHitEntity extends PacketEntityBulletHit{
	private final Point3D localCenter;
	private final double bulletVelocityFactor;
	private final UUID hitEntityID;
	private final UUID controllerEntityID;

	public PacketEntityBulletHitEntity(EntityBullet bullet, BoundingBox box, AEntityE_Interactable<?> hitEntity){
		super(bullet, box.globalCenter);
		this.localCenter = box.localCenter;
		this.bulletVelocityFactor = bullet.velocity/bullet.initialVelocity;
		this.hitEntityID = hitEntity.uniqueUUID;
		this.controllerEntityID = bullet.gun.lastController != null ? bullet.gun.lastController.getID() : null;
	}
	
	public PacketEntityBulletHitEntity(ByteBuf buf){
		super(buf);
		this.localCenter = readPoint3dFromBuffer(buf);
		this.bulletVelocityFactor = buf.readDouble();
		this.hitEntityID = readUUIDFromBuffer(buf);
		this.controllerEntityID = buf.readBoolean() ? readUUIDFromBuffer(buf) : null;
	}

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writePoint3dToBuffer(localCenter, buf);
		buf.writeDouble(bulletVelocityFactor);
		writeUUIDToBuffer(hitEntityID, buf);
		buf.writeBoolean(controllerEntityID != null);
		if(controllerEntityID != null){
			writeUUIDToBuffer(controllerEntityID, buf);
		}
	}
	
	@Override
	public boolean handleBulletHit(AWrapperWorld world){
		if(!world.isClient()){
			AEntityE_Interactable<?> entityHit = world.getEntity(hitEntityID);
			if(entityHit != null){
				//Create damage object and attack the entity.
				BoundingBox hitBox = new BoundingBox(localCenter, hitPosition, bulletItem.definition.bullet.diameter*1000, bulletItem.definition.bullet.diameter*1000, bulletItem.definition.bullet.diameter*1000, false);
				IWrapperEntity attacker = controllerEntityID != null ? world.getExternalEntity(controllerEntityID) : null;
				LanguageEntry language = attacker != null ? JSONConfigLanguage.DEATH_BULLET_PLAYER : JSONConfigLanguage.DEATH_BULLET_NULL;
				double damageAmount = bulletVelocityFactor*bulletItem.definition.bullet.damage*ConfigSystem.settings.damage.bulletDamageFactor.value;
				Damage damage = new Damage(damageAmount, hitBox, null, attacker, language);
				damage.setBullet(bulletItem);
				entityHit.attack(damage);
			}
		}
		return false;
	}
}
