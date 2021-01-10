package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Gun;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Packet used for sending bullet hit information to and from the server.  The first instance of this packet
 * is sent from clients to the server when they detect that a bullet that was spawned by the client player
 * has hit another entity or block.  Logic is performed here for the bullet hit action, and if the bullet hit
 * action requires clients to create effects for the hit, a corresponding packet is sent back.
 * 
 * @author don_bruce
 */
public class PacketBulletHit extends APacketBase{
	private final int gunID;
	private final Point3d localCenter;
	private final Point3d globalCenter;
	private final double bulletVelocity;
	private final ItemPart bullet;
	private final int bulletNumber;
	private final int hitEntityID;
	private final int controllerEntityID;

	public PacketBulletHit(BoundingBox box, double velocity, ItemPart bullet, Gun gun, int bulletNumber, WrapperEntity hitEntity, WrapperEntity controllerEntity){
		super(null);
		this.gunID = gun.gunID;
		this.localCenter = box.localCenter;
		this.globalCenter = box.globalCenter;
		this.bulletVelocity = velocity;
		this.bullet = bullet;
		this.bulletNumber = bulletNumber;
		this.hitEntityID = hitEntity != null ? hitEntity.getID() : -1;
		this.controllerEntityID = controllerEntity != null ? controllerEntity.getID() : -1;
	}
	
	public PacketBulletHit(ByteBuf buf){
		super(buf);
		this.gunID = buf.readInt();
		this.localCenter = readPoint3dFromBuffer(buf);
		this.globalCenter = readPoint3dFromBuffer(buf);
		this.bulletVelocity = buf.readDouble();
		this.bullet = PackParserSystem.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf), readStringFromBuffer(buf));
		this.bulletNumber = buf.readInt();
		this.hitEntityID = buf.readInt();
		this.controllerEntityID = buf.readInt();
	}

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(gunID);
		writePoint3dToBuffer(localCenter, buf);
		writePoint3dToBuffer(globalCenter, buf);
		buf.writeDouble(bulletVelocity);
		writeStringToBuffer(bullet.definition.packID, buf);
		writeStringToBuffer(bullet.definition.systemName, buf);
		writeStringToBuffer(bullet.subName, buf);
		buf.writeInt(bulletNumber);
		buf.writeInt(hitEntityID);
		buf.writeInt(controllerEntityID);
	}
	
	@Override
	public void handle(WrapperWorld world, WrapperPlayer player){
		if(!world.isClient()){
			//Get the bullet definition, and the position the bullet hit.  Also get the gun that fired the bullet.
			//We need this to make sure that this isn't a duplicate packet from another client.
			JSONPart bulletDefinition = bullet.definition;
			float blastSize = bulletDefinition.bullet.blastStrength == 0f ? bulletDefinition.bullet.diameter/10f : bulletDefinition.bullet.blastStrength;
			BoundingBox box = new BoundingBox(localCenter, globalCenter, blastSize/100F, blastSize/100F, blastSize/100F, false, false, false, 0);
			Gun gun = Gun.createdServerGuns.get(gunID);
			
			//If the bullet hasn't been marked as hit yet, do hit logic.
			if(!gun.bulletsHitOnServer.contains(bulletNumber)){
				gun.bulletsHitOnServer.add(bulletNumber);
				//If we are an explosive bullet, blow up at our current position.
				//Otherwise do attack logic.
				if(bulletDefinition.bullet.types.contains("explosive")){
					world.spawnExplosion(gun.provider.getController(), box.globalCenter, blastSize, bulletDefinition.bullet.types.contains("incendiary"));
				}else{
					//If we hit an entity, apply damage to them.
					if(hitEntityID != -1){
						WrapperEntity entityHit = world.getEntity(hitEntityID);
						if(entityHit != null){
							//Create damage object and attack the entity.
							WrapperEntity attacker = world.getEntity(controllerEntityID);
							double damageAmount = bulletVelocity*bulletDefinition.bullet.diameter/5D*ConfigSystem.configObject.damage.bulletDamageFactor.value;
							Damage damage = new Damage("bullet", damageAmount, box, attacker).ignoreCooldown();
							if(bulletDefinition.bullet.types.contains("water")){
								damage.isWater = true;
							}
							if(bulletDefinition.bullet.types.contains("incendiary")){
								damage.isFire = true;
							}
							if(bulletDefinition.bullet.types.contains("armor_piercing")){
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
						Point3i hitPosition = new Point3i(box.globalCenter);
						if(bulletDefinition.bullet.types.contains("water")){
							hitPosition.add(0, 1, 0);
							if(world.isFire(hitPosition)){
								world.destroyBlock(hitPosition);
							}
						}else{
							//This block may be null in the case of air bursts or proximity fuses
							//If we can break the block we hit, do so now.
							float hardnessHit = world.getBlockHardness(hitPosition);
							if(!world.isAir(hitPosition) && hardnessHit > 0 && hardnessHit <= (Math.random()*0.3F + 0.3F*bulletDefinition.bullet.diameter/20F)){
								world.destroyBlock(hitPosition);
							}else if(bulletDefinition.bullet.types.contains("incendiary")){
								//Couldn't break block, but we might be able to set it on fire.
								hitPosition.add(0, 1, 0);
								if(world.isAir(hitPosition)){
									world.setToFire(hitPosition);
								}
							}else{
								//Couldn't break the block or set it on fire.  Have clients do effects.
								InterfacePacket.sendToAllClients(this);
							}
						}
					}
				}
			}
		}else{
			//We only get a packet back if we hit a block and didn't break it.
			//If this is the case, play the block break sound and spawn some particles.
			InterfaceRender.spawnBlockBreakParticles(new Point3i(globalCenter), true);
		}
	}
}
