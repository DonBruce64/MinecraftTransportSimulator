package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Packet used for sending bullet hit information to and from the server.  The first instance of this packet
 * is sent from clients to the server when they detect that a bullet that was spawned by the client player
 * has hit another entity or block.  Logic is performed here for the bullet hit action, and if the bullet hit
 * action requires clients to create effects for the hit, a corresponding packet is sent back.
 * 
 * @author don_bruce
 */
public class PacketPartGunBulletHit extends APacketEntity<PartGun>{
	private final Point3d localCenter;
	private final Point3d globalCenter;
	private final double bulletVelocity;
	private final ItemBullet bulletItem;
	private final int bulletNumber;
	private final String hitEntityID;
	private final String controllerEntityID;

	public PacketPartGunBulletHit(PartGun gun, EntityBullet bullet, BoundingBox box, WrapperEntity hitEntity){
		super(gun);
		this.localCenter = box.localCenter;
		this.globalCenter = box.globalCenter;
		this.bulletVelocity = bullet.velocity;
		this.bulletItem = bullet.getItem();
		this.bulletNumber = bullet.bulletNumber;
		this.hitEntityID = hitEntity != null ? hitEntity.getID() : null;
		this.controllerEntityID = gun.lastController != null ? gun.lastController.getID() : null;
	}
	
	public PacketPartGunBulletHit(ByteBuf buf){
		super(buf);
		this.localCenter = readPoint3dFromBuffer(buf);
		this.globalCenter = readPoint3dFromBuffer(buf);
		this.bulletVelocity = buf.readDouble();
		this.bulletItem = PackParserSystem.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf), readStringFromBuffer(buf));
		this.bulletNumber = buf.readInt();
		this.hitEntityID = buf.readBoolean() ? readStringFromBuffer(buf) : null;
		this.controllerEntityID = buf.readBoolean() ? readStringFromBuffer(buf) : null;
	}

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writePoint3dToBuffer(localCenter, buf);
		writePoint3dToBuffer(globalCenter, buf);
		buf.writeDouble(bulletVelocity);
		writeStringToBuffer(bulletItem.definition.packID, buf);
		writeStringToBuffer(bulletItem.definition.systemName, buf);
		writeStringToBuffer(bulletItem.subName, buf);
		buf.writeInt(bulletNumber);
		buf.writeBoolean(hitEntityID != null);
		if(hitEntityID != null){
			writeStringToBuffer(hitEntityID, buf);
		}
		buf.writeBoolean(controllerEntityID != null);
		if(controllerEntityID != null){
			writeStringToBuffer(controllerEntityID, buf);
		}
	}
	
	@Override
	public boolean handle(WrapperWorld world, PartGun gun){
		if(!world.isClient()){
			//Get the bullet definition, and the position the bullet hit.  Also get the gun that fired the bullet.
			//We need this to make sure that this isn't a duplicate packet from another client.
			float blastSize = bulletItem.definition.bullet.blastStrength == 0f ? bulletItem.definition.bullet.diameter/10f : bulletItem.definition.bullet.blastStrength;
			
			//If the bullet hasn't been marked as hit yet, do hit logic.
			if(!gun.bulletsHitOnServer.contains(bulletNumber)){
				gun.bulletsHitOnServer.add(bulletNumber);
				//If we hit an entity, apply damage to them.
				if(hitEntityID != null){
					WrapperEntity entityHit = world.getEntity(hitEntityID);
					if(entityHit != null){
						BoundingBox hitBox = null;
						if(entityHit.getBaseEntity() instanceof AEntityE_Multipart){
							//Need to get the part box hit for reference.
							for(BoundingBox box : ((AEntityE_Multipart<?>) entityHit.getBaseEntity()).allInteractionBoxes){
								if(box.localCenter.equals(localCenter)){
									hitBox = box;
									break;
								}
							}
						}
						if(hitBox == null){	
							hitBox = new BoundingBox(localCenter, globalCenter, blastSize/100F, blastSize/100F, blastSize/100F, false);
						}
						//Create damage object and attack the entity.
						WrapperEntity attacker = world.getEntity(controllerEntityID);
						double damageAmount = bulletVelocity*bulletItem.definition.bullet.diameter/5D*ConfigSystem.configObject.damage.bulletDamageFactor.value;
						Damage damage = new Damage("bullet", damageAmount, hitBox, gun, attacker).ignoreCooldown().setEffects(bulletItem.definition.bullet.effects);
						if(bulletItem.definition.bullet.types.contains(BulletType.WATER)){
							damage.isWater = true;
						}
						if(bulletItem.definition.bullet.types.contains(BulletType.INCENDIARY)){
							damage.isFire = true;
						}
						if(bulletItem.definition.bullet.types.contains(BulletType.ARMOR_PIERCING)){
							damage.ignoreArmor = true;
						}
						entityHit.attack(damage);
					}
				}else{
					//We didn't hit an entity, so check to see if we hit a block.
					//If the bullet is big, and the block is soft, then break the block.
					//If we are an incendiary bullet, set the block on fire.
					//If we are a water bullet, and we hit fire, put it out. 
					//Otherwise, send this packet back to the client to spawn SFX as we didn't do any state changes.
					//In this case, we need to simply spawn a few block particles to alert the player of a hit.
					Point3d hitPosition = globalCenter.copy();
					if(bulletItem.definition.bullet.types.contains(BulletType.WATER)){
						world.extinguish(hitPosition);
					}else{
						//This block may be null in the case of air bursts or proximity fuses
						//If we can break the block we hit, do so now.
						float hardnessHit = world.getBlockHardness(hitPosition);
						if(ConfigSystem.configObject.general.blockBreakage.value && !world.isAir(hitPosition) && hardnessHit > 0 && hardnessHit <= (Math.random()*0.3F + 0.3F*bulletItem.definition.bullet.diameter/20F)){
							world.destroyBlock(hitPosition, true);
						}else if(bulletItem.definition.bullet.types.contains(BulletType.INCENDIARY)){
							//Couldn't break block, but we might be able to set it on fire.
							hitPosition.add(0, 1, 0);
							if(world.isAir(hitPosition)){
								world.setToFire(hitPosition);
							}
						}else{
							//Couldn't break the block or set it on fire.  Have clients do sounds.
							InterfacePacket.sendToAllClients(this);
						}
					}
				}
			}
				
			//If we are an explosive bullet, blow up at our current position.
			//Otherwise do attack logic.
			if(bulletItem.definition.bullet.types.contains(BulletType.EXPLOSIVE)){
				world.spawnExplosion(globalCenter, blastSize, bulletItem.definition.bullet.types.contains(BulletType.INCENDIARY));
			}
		}else{
			//We only get a packet back if we hit a block and didn't break it.
			//If this is the case, play the block break sound.
			InterfaceClient.playBlockBreakSound(globalCenter);
		}
		return false;
	}
}
