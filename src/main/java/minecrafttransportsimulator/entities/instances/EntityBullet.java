package minecrafttransportsimulator.entities.instances;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHit;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitBlock;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitEntity;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitWrapper;
import minecrafttransportsimulator.rendering.instances.RenderBullet;
import minecrafttransportsimulator.systems.ConfigSystem;

/**This part class is special, in that it does not extend APart.
 * This is because bullets do not render as vehicle parts, and instead
 * are particles.  This allows them to be independent of the
 * vehicle that fired them.
 * 
 * As particles, bullets are client-side only.  This prevents them from getting stuck
 * in un-loaded chunks on the server, and prevents the massive network usage that
 * would be required to spawn 100s of bullets from a machine gun into the world.
 * 
 * @author don_bruce
 */

public class EntityBullet extends AEntityD_Definable<JSONBullet>{	
	//Properties
	public final PartGun gun;
	public final int bulletNumber;
	private final boolean isBomb;
	public final double initialVelocity;
	private final double velocityToAddEachTick;
	private final Point3D motionToAddEachTick;
	
	//States
	private Point3D targetPosition;
	public double targetDistance;
	private Point3D targetVector;
	private PartEngine engineTargeted;
	private WrapperEntity externalEntityTargeted;
	private HitType lastHit;
	
	private static RenderBullet renderer;
	
	/**Generic constructor for no target.**/
    public EntityBullet(Point3D position, Point3D motion, RotationMatrix orientation, PartGun gun){
    	super(gun.world, position, motion, ZERO_FOR_CONSTRUCTOR, gun.loadedBullet);
    	this.gun = gun;
        this.bulletNumber = gun.bulletsFired;
        this.isBomb = gun.definition.gun.muzzleVelocity == 0;
        this.boundingBox.widthRadius = definition.bullet.diameter/1000D/2D;
        this.boundingBox.heightRadius = definition.bullet.diameter/1000D/2D;
        this.boundingBox.depthRadius = definition.bullet.diameter/1000D/2D;
        this.initialVelocity = motion.length();
        if(definition.bullet.accelerationTime > 0){
        	velocityToAddEachTick = (definition.bullet.maxVelocity/20D/10D - motion.length())/definition.bullet.accelerationTime;
        	this.motionToAddEachTick = new Point3D(0, 0, velocityToAddEachTick).rotate(gun.orientation);
        }else{
        	velocityToAddEachTick = 0;
        	motionToAddEachTick = null;
        }
        this.orientation.set(orientation);
        prevOrientation.set(orientation);
    }
    
    /**Positional target.**/
  private EntityBullet(Point3D position, Point3D motion, RotationMatrix orientation, PartGun gun, Point3D targetPosition){
    	this(position, motion, orientation, gun);
    	this.targetPosition = targetPosition;
    }
    
    /**Engine target.**/
    public EntityBullet(Point3D position, Point3D motion, RotationMatrix orientation, PartGun gun, PartEngine engineTargeted){
    	this(position, motion, orientation, gun, engineTargeted.position);
    	if(engineTargeted.entityOn instanceof EntityVehicleF_Physics){
    		((EntityVehicleF_Physics) engineTargeted.entityOn).acquireMissile(this);
    	}
	    this.engineTargeted = engineTargeted;
	    if(ConfigSystem.settings.general.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, "LOCKON ENGINE " + engineTargeted.definition.systemName + " @ " + targetPosition);
    }
    
    /**Wrapper target.**/
    public EntityBullet(Point3D position, Point3D motion, RotationMatrix orientation, PartGun gun, WrapperEntity externalEntityTargeted){
    	this(position, motion, orientation, gun, externalEntityTargeted.getPosition().copy());
	    this.externalEntityTargeted = externalEntityTargeted;
	    if(ConfigSystem.settings.general.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, "LOCKON ENTITY " + externalEntityTargeted.getName() + " @ " + externalEntityTargeted.getPosition());
    }
	
    @Override
    public void update(){
		super.update();
		//Add gravity and slowdown forces, if we don't have a burning motor.
		if(ticksExisted > definition.bullet.burnTime){
			if(definition.bullet.slowdownSpeed > 0){
				motion.add(motion.copy().normalize().scale(-definition.bullet.slowdownSpeed));
			}
			motion.y -= gun.definition.gun.gravitationalVelocity;

			//Check to make sure we haven't gone too many ticks.
			if(ticksExisted > definition.bullet.burnTime + 200){
				if(ConfigSystem.settings.general.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, "TIMEOUT");
				remove();
				return;
			}
		}
		
		//Add motion requested watch tick we are accelerating.
		boolean notAcceleratingYet = definition.bullet.accelerationDelay != 0 && ticksExisted < definition.bullet.accelerationDelay;   
		if(velocityToAddEachTick != 0 && !notAcceleratingYet && ticksExisted - definition.bullet.accelerationDelay < definition.bullet.accelerationTime){
			motionToAddEachTick.set(0, 0, velocityToAddEachTick).rotate(orientation);
			motion.add(motionToAddEachTick);
		}
		
		//We have a target.  Go to it, unless we are waiting for acceleration.
		//If the target is an external entity, update target position.
		if(targetPosition != null && !notAcceleratingYet){
			if(externalEntityTargeted != null){
				if(externalEntityTargeted.isValid()){
					targetPosition.set(externalEntityTargeted.getPosition());
				}else{
					//Entity is dead.  Don't target it anymore.
					externalEntityTargeted = null;
					targetPosition = null;
				}
			}else if(engineTargeted != null){
				//Don't need to update the position variable for engines, as it auto-syncs.
				//Do need to check if the engine is still warm and valid, however.
				if(!engineTargeted.isValid){// || engineTargeted.temp <= PartEngine.COLD_TEMP){
					engineTargeted = null;
					targetPosition = null;
				}
			}
			
			if(targetPosition != null){
				//Get the angular delta between us and our target, in our local orientation coordinates.
				if(targetVector == null){
					targetVector = new Point3D();
				}
				targetVector.set(targetPosition).subtract(position).reOrigin(orientation).getAngles(true);
				
				//Clamp angular delta to match turn rate and apply.
				if(targetVector.y > definition.bullet.turnRate){
					targetVector.y = definition.bullet.turnRate;
				}else if(targetVector.y < -definition.bullet.turnRate){
					targetVector.y = -definition.bullet.turnRate;
				}
				orientation.rotateY(targetVector.y);
				
				if(targetVector.x > definition.bullet.turnRate){
					targetVector.x = definition.bullet.turnRate;
				}else if(targetVector.x < -definition.bullet.turnRate){
					targetVector.x = -definition.bullet.turnRate;
				}
				orientation.rotateX(targetVector.x);
				
				//Set motion to new orientation.
				targetVector.set(0, 0, motion.length()).rotate(orientation);
				motion.set(targetVector);
				
				//Update target distance.
				targetDistance = targetPosition.distanceTo(position);
			}
		}	
		
		//Now that we have an accurate motion, check for collisions.
		//First get a damage object.
		Damage damage = new Damage((velocity/initialVelocity)*definition.bullet.damage*ConfigSystem.settings.damage.bulletDamageFactor.value, boundingBox, gun, null, null);
		
		//Check for collided external entities and attack them.
		List<WrapperEntity> attackedEntities = world.attackEntities(damage, motion);
		if(!attackedEntities.isEmpty()){
			for(WrapperEntity entity : attackedEntities){
				//Check to make sure we don't hit our controller.
				//This can happen with hand-held guns at speed.
				if(!entity.equals(gun.lastController)){
					//Only attack the first entity.  Bullets don't get to attack multiple per scan.
					InterfacePacket.sendToServer(new PacketEntityBulletHitWrapper(this, entity));
					lastHit = HitType.ENTITY;
					if(ConfigSystem.settings.general.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, "HIT ENTITY");
					remove();
					return;
				}
			}
		}
		
		//Check for collided internal entities and attack them.
		//This is a bit more involved, as we need to check all possible types and check hitbox distance.
		Point3D endPoint = position.copy().add(motion);
		BoundingBox bulletMovmenetBounds = new BoundingBox(position, endPoint);
		for(EntityVehicleF_Physics entity : world.getEntitiesOfType(EntityVehicleF_Physics.class)){
			double armorPenetrated = 0;
			//Don't attack the entity that has the gun that fired us.
			if(!entity.parts.contains(gun)){
				//Make sure that we could even possibly hit this vehicle before we try and attack it.
				if(entity.encompassingBox.intersects(bulletMovmenetBounds)){
					//Get all collision boxes on the vehicle, and check if we hit any of them.
					//Sort them by distance for later.
					TreeMap<Double, BoundingBox> hitBoxes = new TreeMap<Double, BoundingBox>();
					for(BoundingBox box : entity.allInteractionBoxes){
						Point3D delta = box.getIntersectionPoint(position, endPoint); 
						if(delta != null){
							hitBoxes.put(delta.distanceTo(position), box);
						}
					}
					
					//If we hit at least one hitbox, do logic.
					if(!hitBoxes.isEmpty()){
						//Check all boxes for armor and see if we penetrated them.
						Iterator<BoundingBox> hitBoxIterator = hitBoxes.values().iterator();
						while(hitBoxIterator.hasNext()){
							BoundingBox hitBox = hitBoxIterator.next();
							if(hitBox.definition != null && hitBox.definition.armorThickness > 0){
								armorPenetrated += hitBox.definition.armorThickness;
								if(armorPenetrated > definition.bullet.armorPenetration*velocity/initialVelocity){
									//Hit too much armor.  Don't do anything except spawn explosions.
									InterfacePacket.sendToServer(new PacketEntityBulletHit(this, hitBox.globalCenter));
									lastHit = HitType.ARMOR;
									if(ConfigSystem.settings.general.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, "HIT TOO MUCH ARMOR.  MAX PEN: " + (int)(definition.bullet.armorPenetration*velocity/initialVelocity));
									remove();
									return;
								}
							}else{
								APart hitPart = entity.getPartWithBox(hitBox);
								if(hitPart != null){
									InterfacePacket.sendToServer(new PacketEntityBulletHitEntity(this, hitBox, hitPart));
									lastHit = HitType.PART;
									if(ConfigSystem.settings.general.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, "HIT PART");
									remove();
									return;
								}else{
									InterfacePacket.sendToServer(new PacketEntityBulletHitEntity(this, hitBox, entity));
									lastHit = HitType.ENTITY;
									if(ConfigSystem.settings.general.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, "HIT VEHICLE");
									remove();
									return;
								}
							}
						}
						if(armorPenetrated != 0){
							if(ConfigSystem.settings.general.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, "PEN ARMOR: " + (int)armorPenetrated + " TOTAL UNITS OUT OF " + (int)(definition.bullet.armorPenetration*velocity/initialVelocity) + " POSSIBLE");
						}
					}
				}
			}
		}
		
		//Didn't hit an entity.  Check for blocks.
		Point3D hitPos = world.getBlockHit(position, motion);
		if(hitPos != null){
			InterfacePacket.sendToServer(new PacketEntityBulletHitBlock(this, hitPos));
			lastHit = HitType.BLOCK;
			if(ConfigSystem.settings.general.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, "HIT BLOCK");
			remove();
			return;
		}
		
		//Check proximity fuze against our target and blocks.
		if(definition.bullet.proximityFuze != 0){
			Point3D targetToHit;
			if(targetPosition != null){
				targetToHit = targetPosition;
			}else{
				targetToHit = world.getBlockHit(position, motion.copy().normalize().scale(definition.bullet.proximityFuze + velocity));
			}
			if(targetToHit != null){
				double distanceToTarget = position.distanceTo(targetToHit);
				if(distanceToTarget < definition.bullet.proximityFuze + velocity){
					if(distanceToTarget > definition.bullet.proximityFuze){
						position.interpolate(targetToHit, (distanceToTarget - definition.bullet.proximityFuze)/definition.bullet.proximityFuze);
					}
					InterfacePacket.sendToServer(new PacketEntityBulletHit(this, position));
					if(externalEntityTargeted != null){
						lastHit =  HitType.ENTITY;
						if(ConfigSystem.settings.general.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, "PROX FUZE HIT ENTITY");	
					}else if(engineTargeted != null){
						lastHit = HitType.PART;
						if(ConfigSystem.settings.general.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, "PROX FUZE HIT ENGINE");
					}else{
						lastHit = HitType.BLOCK;
						if(ConfigSystem.settings.general.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, "PROX FUZE HIT BLOCK");
					}
					remove();
					return;
				}
			}
		}
		
		//Didn't hit a block either. Check the air-burst time, if it was used.
		if(definition.bullet.airBurstDelay != 0) {
			if(ticksExisted > definition.bullet.airBurstDelay){
				InterfacePacket.sendToServer(new PacketEntityBulletHit(this, position));
				lastHit = HitType.BURST;
				if(ConfigSystem.settings.general.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, "BURST");
				remove();
				return;
			}
		}
		
		//Add our updated motion to the position.
		//Then set the angles to match the motion.
		//Doing this last lets us damage on the first update tick.
		position.add(motion);
		if(!isBomb && (definition.bullet.accelerationDelay == 0 || ticksExisted > definition.bullet.accelerationDelay)){
			orientation.setToVector(motion, true);
		}
	}
    
    @Override
	public void remove(){
    	//Check one final time for particles in case we have some that spawn when we hit something.
    	if(world.isClient()){
    		this.spawnParticles(0);
    	}
    	super.remove();
    }
    
    @Override
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("bullet_hit"): return lastHit != null ? 1 : 0;
			case("bullet_burntime"): return ticksExisted > definition.bullet.burnTime ? 0 : definition.bullet.burnTime - ticksExisted;	
			case("bullet_hit_block"): return HitType.BLOCK.equals(lastHit) ? 1 : 0;
			case("bullet_hit_entity"): return HitType.ENTITY.equals(lastHit) ? 1 : 0;
			case("bullet_hit_part"): return HitType.PART.equals(lastHit) ? 1 : 0;
			case("bullet_hit_armor"): return HitType.ARMOR.equals(lastHit) ? 1 : 0;
			case("bullet_hit_burst"): return HitType.BURST.equals(lastHit) ? 1 : 0;
		}
		
		return super.getRawVariableValue(variable, partialTicks);
	}
	
	@Override
	public boolean shouldSync(){
		return false;
	}
	
	@Override
	public boolean shouldSavePosition(){
		return false;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public RenderBullet getRenderer(){
		if(renderer == null){
			renderer = new RenderBullet();
		}
		return renderer;
	}
	
	private static enum HitType{
		BLOCK,
		ENTITY,
		PART,
		ARMOR,
		BURST;
	}
}
