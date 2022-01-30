package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.systems.ConfigSystem;

/**Packet sent when a bullet hits a block.
 * 
 * @author don_bruce
 */
public class PacketEntityBulletHitBlock extends PacketEntityBulletHit{

	public PacketEntityBulletHitBlock(EntityBullet bullet, Point3d blockPosition){
		super(bullet, blockPosition);
	}
	
	public PacketEntityBulletHitBlock(ByteBuf buf){
		super(buf);
	}
	
	@Override
	public boolean handleBulletHit(WrapperWorld world){
		if(!world.isClient()){
			//If the bullet is big, and the block is soft, then break the block.
			//If we are an incendiary bullet, set the block on fire.
			//If we are a water bullet, and we hit fire, put it out. 
			//Otherwise, send this packet back to the client to spawn SFX as we didn't do any state changes.
			//In this case, we need to simply spawn a few block particles to alert the player of a hit.
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
					return true;
				}
			}
			return false;
		}else{
			//We only get a packet back if we hit a block and didn't break it.
			//If this is the case, play the block break sound.
			InterfaceClient.playBlockBreakSound(hitPosition);
		}
		return false;
	}
}
