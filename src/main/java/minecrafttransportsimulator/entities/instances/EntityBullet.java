package minecrafttransportsimulator.entities.instances;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.packets.instances.PacketPartGunBulletHit;
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

public class EntityBullet extends AEntityC_Definable<JSONBullet>{	
	//Properties
	private final PartGun gun;
	public final int bulletNumber;
	private final double initialVelocity;
	private final double anglePerTickSpeed;
	private final Point3d velocityToAddEachTick;
	
	//States
	private Point3d targetPosition;
	public double targetDistance;
	private WrapperEntity externalEntityTargeted;
	private PartEngine engineTargeted;
	private HitType lastHit;
	
	private static RenderBullet renderer;
	
    public EntityBullet(Point3d position, Point3d motion, PartGun gun){
    	super(gun.world, position, motion, ZERO_FOR_CONSTRUCTOR, gun.loadedBullet);
    	this.gun = gun;
        this.bulletNumber = gun.bulletsFired;
        this.boundingBox = new BoundingBox(this.position, definition.bullet.diameter/1000D/2D, definition.bullet.diameter/1000D/2D, definition.bullet.diameter/1000D/2D);
        this.initialVelocity = motion.length();
        this.anglePerTickSpeed = definition.bullet.turnFactor * 1000/definition.bullet.diameter;
        if(definition.bullet.accelerationTime > 0){
        	double velocityDelta = definition.bullet.maxVelocity/20D/10D - motion.length();
        	this.velocityToAddEachTick = new Point3d(0, 0, 1).rotateFine(gun.angles).multiply(velocityDelta/definition.bullet.accelerationTime);
        }else{
        	velocityToAddEachTick = new Point3d();
        }
        angles.setTo(motion.copy().getAngles(true));
        prevAngles.setTo(angles);
    }
    
    public EntityBullet(Point3d position, Point3d motion,  PartGun gun, Point3d blockTargetPos){
    	this(position, motion, gun);
    	this.targetPosition = blockTargetPos;
    }
    
    public EntityBullet(Point3d position, Point3d motion, PartGun gun, WrapperEntity externalEntityTargeted){
    	this(position, motion, gun);
    	AEntityA_Base entity = externalEntityTargeted.getBaseEntity();
		if(entity != null){
			if(entity instanceof AEntityE_Multipart){
				for(APart part : ((AEntityE_Multipart<?>) entity).parts){
					if(part instanceof PartEngine && ((PartEngine) part).temp > PartEngine.COLD_TEMP){
						engineTargeted = (PartEngine) part;
						targetPosition = engineTargeted.position;
						externalEntityTargeted = null;
						if(engineTargeted.entityOn instanceof EntityVehicleF_Physics){
				    		((EntityVehicleF_Physics) engineTargeted.entityOn).acquireMissile(this);
				    	}
						break;
					}
				}
			}
		}
		if(externalEntityTargeted != null){
	    	this.targetPosition = new Point3d().setTo(externalEntityTargeted.getPosition());
	    	this.externalEntityTargeted = externalEntityTargeted;
		}
    }
	
    @Override
	public boolean update(){
		if(super.update()){
			//Get possible damage.
			Damage damage = new Damage("bullet", velocity*definition.bullet.diameter/5*ConfigSystem.configObject.damage.bulletDamageFactor.value, boundingBox, gun, null);
			
			//Check for collided entities and attack them.
			//If we collide with an armored vehicle, try to penetrate it.
			Map<WrapperEntity, Collection<BoundingBox>> attackedEntities = world.attackEntities(damage, motion);
			if(!attackedEntities.isEmpty()){
				double armorPenetrated = 0;
				for(WrapperEntity entity : attackedEntities.keySet()){
					Collection<BoundingBox> hitBoxes = attackedEntities.get(entity);
					if(hitBoxes != null){
						AEntityA_Base baseEntity = entity.getBaseEntity();
						BoundingBox armorBoxHit = null;
						
						//Check all boxes for armor and see if we penetrated them.
						Iterator<BoundingBox> hitBoxIterator = hitBoxes.iterator();
						while(hitBoxIterator.hasNext()){
							BoundingBox hitBox = hitBoxIterator.next();
							if(hitBox.definition != null && hitBox.definition.armorThickness > 0){
								if(hitBox.definition.armorThickness < definition.bullet.armorPenetration*velocity/initialVelocity - armorPenetrated){
									armorPenetrated += hitBox.definition.armorThickness;
									hitBoxIterator.remove();
								}else{
									armorBoxHit = hitBox;
								}
							}else if(baseEntity instanceof AEntityE_Multipart){
								if(((AEntityE_Multipart<?>) baseEntity).getPartWithBox(hitBox) != null){
									break;
								}
							}
						}	
						
						//If we hit an armor box, set that to what we attacked.
						//If we didn't, see if we hit a part instead.
						if(armorBoxHit != null){
							lastHit = HitType.ARMOR;
							remove();
							return false;
						}else{
							for(BoundingBox hitBox : hitBoxes){
								if(baseEntity instanceof AEntityE_Multipart && ((AEntityE_Multipart<?>) baseEntity).getPartWithBox(hitBox) != null){
									lastHit = HitType.PART;
								}else{
									lastHit = HitType.ENTITY;
								}
								InterfacePacket.sendToServer(new PacketPartGunBulletHit(gun, this, hitBox, entity));
								remove();
								return false;
							}
						}
					}else{
						//Must of hit a normal entity.  Set our box to the entity's box and attack it.
						boundingBox.globalCenter.setTo(entity.getPosition());
						InterfacePacket.sendToServer(new PacketPartGunBulletHit(gun, this, boundingBox, entity));
						lastHit = HitType.ENTITY;
						remove();
						return false;
					}
				}
			}
			
			//Didn't hit an entity.  Check for blocks.
			Point3d hitPos = world.getBlockHit(position, motion);
			if(hitPos != null){
				boundingBox.globalCenter.setTo(hitPos);
				InterfacePacket.sendToServer(new PacketPartGunBulletHit(gun, this, boundingBox, null));
				lastHit = HitType.BLOCK;
				remove();
				return false;
			}
			
			//Check proximity fuze against our target or any blocks that might be out front
			if(definition.bullet.proximityFuze != 0){
				if(targetPosition != null){
					double distanceUntilImpact = position.distanceTo(targetPosition);
					if(distanceUntilImpact <= definition.bullet.proximityFuze){
						InterfacePacket.sendToServer(new PacketPartGunBulletHit(gun, this, boundingBox, null));
						lastHit = externalEntityTargeted != null ? HitType.ENTITY : (engineTargeted != null ? HitType.PART : HitType.BLOCK);
						remove();
						return false;
					}
				}
				Point3d projectedImpactPoint = world.getBlockHit(position, motion.copy().normalize().multiply(definition.bullet.proximityFuze));
				if(projectedImpactPoint != null){
					InterfacePacket.sendToServer(new PacketPartGunBulletHit(gun, this, boundingBox, null));
					lastHit = HitType.BLOCK;
					remove();
					return false;
				}
			}
			
			//Didn't hit a block either. Check the air-burst time, if it was used.
			if(definition.bullet.airBurstDelay != 0) {
				if(ticksExisted > definition.bullet.airBurstDelay){
					InterfacePacket.sendToServer(new PacketPartGunBulletHit(gun, this, boundingBox, null));
					lastHit = HitType.BURST;
					remove();
					return false;
				}
			}
			
			//Nothing was hit, as we haven't returned yet.  Adjust motion to compensate for bullet movement and gravity.
			//Ignore this if the bullet has a (rocket motor) burnTime that hasn't yet expired,
			//And if the bullet is still accelerating, increase the velocity appropriately.
			if(ticksExisted > definition.bullet.burnTime){
				if(definition.bullet.slowdownSpeed > 0){
					motion.add(motion.copy().normalize().multiply(-definition.bullet.slowdownSpeed));
				}
				motion.y -= gun.definition.gun.gravitationalVelocity;
	
				//Check to make sure we haven't gone too many ticks.
				if(ticksExisted > definition.bullet.burnTime + 200){
					remove();
					return false;
				}
			}else{
				if(ticksExisted < definition.bullet.accelerationTime){
					//Add velocity requested watch tick we are accelerating.
					motion.add(velocityToAddEachTick);
				}
				if(targetPosition != null){
					//We have a target.  Go to it.
					//If the target is an external entity, update target position.
					if(externalEntityTargeted != null){
						if(externalEntityTargeted.isValid()){
							targetPosition.setTo(externalEntityTargeted.getPosition());
						}else{
							//Entity is dead.  Don't target it anymore.
							externalEntityTargeted = null;
							targetPosition = null;
						}
					}else if(engineTargeted != null){
						//Don't need to update the position variable for engines, as it auto-syncs.
						//Do need to check if the engine is still warm and valid, however.
						if(!engineTargeted.isValid || engineTargeted.temp <= PartEngine.COLD_TEMP){
							List<APart> vehicleParts = engineTargeted.entityOn.parts;
							engineTargeted = null;
							double closestEngineDistance = Double.MAX_VALUE;
							for(APart part : vehicleParts){
								if(part instanceof PartEngine){
									PartEngine engine = (PartEngine) part;
									if(engine.isValid && engine.temp > PartEngine.COLD_TEMP){
										double engineDistance = position.distanceTo(engine.position);
										if(engineDistance < closestEngineDistance){
											engineTargeted = engine;
											targetPosition = engineTargeted.position;
											closestEngineDistance = engineDistance;
										}
									}
								}
							}
						}
					}
					
					if(targetPosition != null){
						targetDistance = position.distanceTo(targetPosition);
						double yawTarget = Math.toDegrees(Math.atan2(targetPosition.x - position.x, targetPosition.z - position.z));
						double pitchTarget = -Math.toDegrees(Math.atan2(targetPosition.y - position.y, Math.hypot(targetPosition.x - position.x, targetPosition.z - position.z)));
						//Remain flat if not yet at desired angle of attack
						//Or climb up if needed to get above the target
						if (pitchTarget > 0 && pitchTarget < definition.bullet.angleOfAttack){
							if(position.y < targetPosition.y + 0.5*definition.bullet.angleOfAttack){
								pitchTarget = -definition.bullet.angleOfAttack;
							}else{
								pitchTarget = 0D;
							}
						}
						
						Point3d deltas = motion.copy().getAngles(true).add(-pitchTarget, -yawTarget, 0).multiply(-1);
						//Adjust deltaYaw as necessary, then apply it
						while(deltas.y > 180)deltas.y -= 360;
						while(deltas.y < -180)deltas.y += 360;
						if(deltas.y < 0){
							if(deltas.y < -anglePerTickSpeed){
								deltas.y = -anglePerTickSpeed;
							}
							motion.rotateY(deltas.y);
						}else if(deltas.y > 0){
							if(deltas.y > anglePerTickSpeed){
								deltas.y = anglePerTickSpeed;
							}
							motion.rotateY(deltas.y); 
						}
						
						//Axis for pitch is orthogonal to the horizontal velocity vector
						if(deltas.x < 0){
							if(deltas.x < -anglePerTickSpeed){
								deltas.x = -anglePerTickSpeed;
							}
							motion.rotateFine((new Point3d(motion.z, 0, -1*motion.x)).multiply(deltas.x)); 
						}else if(deltas.x > 0){
							if(deltas.x > anglePerTickSpeed){
								deltas.x = anglePerTickSpeed;
							}
							motion.rotateFine((new Point3d(motion.z, 0, -1*motion.x)).multiply(deltas.x)); 
						}
					}
				}	
			}
			
			//Add our updated motion to the position.
			//Then set the angles to match the motion.
			//Doing this last lets us damage on the first update tick.
			angles.setTo(angles.setTo(motion).copy().getAngles(true));
			position.add(motion);
			return true;
		}else{
			return false;
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
