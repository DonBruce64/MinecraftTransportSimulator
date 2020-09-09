package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import mcinterface.InterfaceNetwork;
import mcinterface.InterfaceRender;
import mcinterface.WrapperBlock;
import mcinterface.WrapperEntity;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.parts.ItemPartBullet;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.systems.ConfigSystem;

/**Packet used for sending bullet hit information to and from the server.  The first instance of this packet
 * is sent from clients to the server when they detect that a bullet that was spawned by the client player
 * has hit another entity or block.  Logic is performed here for the bullet hit action, and if the bullet hit
 * action requires clients to create effects for the hit, a corresponding packet is sent back.
 * Because the server knows which player sent this packet, and the packet is only sent by players that are
 * the "owners" of bullets, it can be assumed that the player who sent the packet is responsible for any
 * effects that the bullet may have on the world.
 * 
 * @author don_bruce
 */
public class PacketBulletHit extends APacketBase{
	private final Point3d localCenter;
	private final Point3d globalCenter;
	private final double bulletVelocity;
	private final String bulletPackID;
	private final String bulletSystemName;
	private final int hitEntityID;

	public PacketBulletHit(BoundingBox box, double velocity, ItemPartBullet bullet, WrapperEntity hitEntity){
		super(null);
		this.localCenter = box.localCenter;
		this.globalCenter = box.globalCenter;
		this.bulletVelocity = velocity;
		this.bulletPackID = bullet.definition.packID;
		this.bulletSystemName = bullet.definition.systemName;
		this.hitEntityID = hitEntity != null ? hitEntity.getID() : -1;
	}
	
	public PacketBulletHit(ByteBuf buf){
		super(buf);
		this.localCenter = readPoint3dFromBuffer(buf);
		this.globalCenter = readPoint3dFromBuffer(buf);
		this.bulletVelocity = buf.readDouble();
		this.bulletPackID = readStringFromBuffer(buf);
		this.bulletSystemName = readStringFromBuffer(buf);
		this.hitEntityID = buf.readInt();
	}

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writePoint3dToBuffer(localCenter, buf);
		writePoint3dToBuffer(globalCenter, buf);
		buf.writeDouble(bulletVelocity);
		writeStringToBuffer(bulletPackID, buf);
		writeStringToBuffer(bulletSystemName, buf);
		buf.writeInt(hitEntityID);
	}
	
	@Override
	public void handle(WrapperWorld world, WrapperPlayer player){
		JSONPart bulletDefinition = (JSONPart) MTSRegistry.packItemMap.get(bulletPackID).get(bulletSystemName).definition;
		BoundingBox box = new BoundingBox(localCenter, globalCenter, bulletDefinition.bullet.diameter/1000F, bulletDefinition.bullet.diameter/1000F, bulletDefinition.bullet.diameter/1000F, false, false);
		if(!world.isClient()){
			//If we are an explosive bullet, just blow up at our current position.
			//Otherwise do attack logic.
			if(bulletDefinition.bullet.type.equals("explosive")){
				world.spawnExplosion(player, box.globalCenter, bulletDefinition.bullet.diameter/10F, false);
			}else{
				//If we hit an entity, apply damage to them.
				if(hitEntityID != -1){
					WrapperEntity entityHit = world.getEntity(hitEntityID);
					if(entityHit != null){
						//Create damage object and attack the entity.
						double damageAmount = bulletVelocity*bulletDefinition.bullet.diameter/5D*ConfigSystem.configObject.damage.bulletDamageFactor.value;
						Damage damage = new Damage("bullet", damageAmount, box, player).ignoreCooldown();
						if(bulletDefinition.bullet.type.equals("water")){
							damage.isWater = true;
						}
						if(bulletDefinition.bullet.type.equals("incendiary")){
							damage.isFire = true;
						}
						if(bulletDefinition.bullet.type.equals("armor_piercing")){
							damage.ignoreArmor = true;
						}
						entityHit.attack(damage);
					}
				}else{
					//We didn't hit an entity, so we must have hit a block.
					//If the bullet is big, and the block is soft, then break the block.
					//If we are an incendiary bullet, set the block on fire.
					//If we are a water bullet, and we hit fire, put it out. 
					//Otherwise, send this packet back to the client to spawn SFX as we didn't do any state changes.
					//In this case, we need to simply spawn a few block particles to alert the player of a hit.
					Point3i hitPosition = new Point3i(box.globalCenter);
					if(bulletDefinition.bullet.type.equals("water")){
						hitPosition.add(0, 1, 0);
						if(world.isFire(hitPosition)){
							world.destroyBlock(hitPosition);
						}
					}else{
						//If we can break the block we hit, do so now.
						WrapperBlock hitBlock = world.getWrapperBlock(hitPosition);
						if(hitBlock.getHardness() > 0 && hitBlock.getHardness() <= (Math.random()*0.3F + 0.3F*bulletDefinition.bullet.diameter/20F)){
							world.destroyBlock(hitPosition);
						}else if(bulletDefinition.bullet.type.equals("incendiary")){
							//Couldn't break block, but we might be able to set it on fire.
							hitPosition.add(0, 1, 0);
							if(world.isAir(hitPosition)){
								world.setToFire(hitPosition);
							}
						}else{
							//Couldn't break the block or set it on fire.  Have clients do effects.
							InterfaceNetwork.sendToAllClients(this);
						}
					}
				}
			}
		}else{
			//We only get a packet back if we hit a block and didn't break it.
			//If this is the case, play the block break sound and spawn some particles.
			InterfaceRender.spawnBlockBreakParticles(new Point3i(box.globalCenter));
		}
	}
}
