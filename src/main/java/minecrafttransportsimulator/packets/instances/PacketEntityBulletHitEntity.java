package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;

/**Packet sent when a bullet hits a normal entity.
 * 
 * @author don_bruce
 */
public class PacketEntityBulletHitEntity extends PacketEntityBulletHit{
	private final int hitboxGroupIndex;
	private final int hitboxIndex;
	private final double damageAmount;
	private final UUID hitEntityID;
	private final UUID controllerEntityID;

	public PacketEntityBulletHitEntity(EntityBullet bullet, BoundingBox box, AEntityE_Interactable<?> hitEntity){
		super(bullet, box.globalCenter);
		this.hitboxGroupIndex = hitEntity.definition.collisionGroups.indexOf(box.groupDef);
		this.hitboxIndex = box.groupDef.collisions.indexOf(box.definition);
		this.damageAmount = bullet.currentDamage.amount*(box.definition.damageMultiplier != 0 ? box.definition.damageMultiplier : 1);
		this.hitEntityID = hitEntity.uniqueUUID;
		this.controllerEntityID = bullet.gun.lastController != null ? bullet.gun.lastController.getID() : null;
	}
	
	public PacketEntityBulletHitEntity(ByteBuf buf){
		super(buf);
		this.hitboxGroupIndex = buf.readInt();
		this.hitboxIndex = buf.readInt();
		this.damageAmount = buf.readDouble();
		this.hitEntityID = readUUIDFromBuffer(buf);
		this.controllerEntityID = buf.readBoolean() ? readUUIDFromBuffer(buf) : null;
	}

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(hitboxGroupIndex);
		buf.writeInt(hitboxIndex);
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
			AEntityE_Interactable<?> entityHit = world.getEntity(hitEntityID);
			if(entityHit != null){
				//Create damage object and attack the entity.
				BoundingBox hitBox = entityHit.definitionCollisionBoxes.get(hitboxGroupIndex).get(hitboxIndex);;
				IWrapperEntity attacker = controllerEntityID != null ? world.getExternalEntity(controllerEntityID) : null;
				LanguageEntry language = attacker != null ? JSONConfigLanguage.DEATH_BULLET_PLAYER : JSONConfigLanguage.DEATH_BULLET_NULL;
				Damage damage = new Damage(damageAmount, hitBox, null, attacker, language);
				damage.setBullet(bulletItem);
				entityHit.attack(damage);
				
				//If we are explosive, and we blew up a part, also blow up the vehicle too.
				if(bulletItem.definition.bullet.blastStrength > 0 && entityHit instanceof APart) {
					EntityVehicleF_Physics vehicle = ((APart) entityHit).vehicleOn;
					if(vehicle != null) {
						vehicle.attack(damage);
					}
				}
			}
		}
		return false;
	}
}
