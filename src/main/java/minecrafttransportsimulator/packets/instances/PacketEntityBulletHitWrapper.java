package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Packet sent when a bullet hits a wrapped (external) entity.
 * 
 * @author don_bruce
 */
public class PacketEntityBulletHitWrapper extends APacketBase{
    private final ItemBullet bulletItem;
	private final double damageAmount;
	private final UUID hitEntityID;
	private final UUID controllerEntityID;

	public PacketEntityBulletHitWrapper(EntityBullet bullet, double damageAmount, IWrapperEntity hitEntity){
		super(null);
		this.bulletItem = bullet.getItem();
		this.damageAmount = damageAmount;
		this.hitEntityID = hitEntity.getID();
		this.controllerEntityID = bullet.gun.lastController != null ? bullet.gun.lastController.getID() : null;
	}
	
	public PacketEntityBulletHitWrapper(ByteBuf buf){
		super(buf);
		this.bulletItem = PackParserSystem.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf), readStringFromBuffer(buf));
		this.damageAmount = buf.readDouble();
		this.hitEntityID = readUUIDFromBuffer(buf);
		this.controllerEntityID = buf.readBoolean() ? readUUIDFromBuffer(buf) : null;
	}

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(bulletItem.definition.packID, buf);
        writeStringToBuffer(bulletItem.definition.systemName, buf);
        writeStringToBuffer(bulletItem.subName, buf);
		buf.writeDouble(damageAmount);
		writeUUIDToBuffer(hitEntityID, buf);
		buf.writeBoolean(controllerEntityID != null);
		if(controllerEntityID != null){
			writeUUIDToBuffer(controllerEntityID, buf);
		}
	}
	
	@Override
	public void handle(AWrapperWorld world){
		IWrapperEntity entityHit = world.getExternalEntity(hitEntityID);
		if(entityHit != null){	
			//Create damage object and attack the entity.
			IWrapperEntity attacker = controllerEntityID != null ? world.getExternalEntity(controllerEntityID) : null;
			LanguageEntry language = attacker != null ? JSONConfigLanguage.DEATH_BULLET_PLAYER : JSONConfigLanguage.DEATH_BULLET_NULL;
			Damage damage = new Damage(damageAmount, entityHit.getBounds(), null, attacker, language);
			damage.setBullet(bulletItem);
			entityHit.attack(damage);
		}
	}
}
