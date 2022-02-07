package minecrafttransportsimulator.entities.instances;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONBullet;
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
	private final double initialVelocity;
	private final double anglePerTickSpeed;
	private final Point3d velocityToAddEachTick;
	
	//States
	private Point3d targetPosition;
	public double targetDistance;
	private PartEngine engineTargeted;
	private WrapperEntity externalEntityTargeted;
	private HitType lastHit;
	
	private static RenderBullet renderer;
	
	/**Generic constructor for no target.**/
    public EntityBullet(Point3d position, Point3d motion, PartGun gun){
    	super(gun.world, position, motion, ZERO_FOR_CONSTRUCTOR, gun.loadedBullet);
    	this.gun = gun;
        this.bulletNumber = gun.bulletsFired;
        this.isBomb = gun.definition.gun.muzzleVelocity == 0;
        this.boundingBox = new BoundingBox(this.position, definition.bullet.diameter/1000D/2D, definition.bullet.diameter/1000D/2D, definition.bullet.diameter/1000D/2D);
        this.initialVelocity = motion.length();
        this.anglePerTickSpeed = definition.bullet.turnFactor * 1000/definition.bullet.diameter;
        if(definition.bullet.accelerationTime > 0){
        	double velocityDelta = definition.bullet.maxVelocity/20D/10D - motion.length();
        	this.velocityToAddEachTick = new Point3d(0, 0, 1).rotateFine(gun.angles).multiply(velocityDelta/definition.bullet.accelerationTime);
        }else{
        	velocityToAddEachTick = new Point3d();
        }
        if(isBomb){
        	angles.setTo(gun.angles);
        }else{
        	angles.setTo(motion).getAngles(true);
        }
        prevAngles.setTo(angles);
    }
    
    /**Positional target.**/
    public EntityBullet(Point3d position, Point3d motion,  PartGun gun, Point3d blockTargetPos){
    	this(position, motion, gun);
    	this.targetPosition = blockTargetPos;
    }
    
    /**Engine target.**/
    public EntityBullet(Point3d position, Point3d motion, PartGun gun, PartEngine engineTargeted){
    	this(position, motion, gun, engineTargeted.position);
    	if(engineTargeted.entityOn instanceof EntityVehicleF_Physics){
    		((EntityVehicleF_Physics) engineTargeted.entityOn).acquireMissile(this);
    	}
		if(externalEntityTargeted != null){
	    	this.engineTargeted = engineTargeted;
		}
    }
    
    /**Wrapper target.**/
    public EntityBullet(Point3d position, Point3d motion, PartGun gun, WrapperEntity externalEntityTargeted){
    	this(position, motion, gun, new Point3d(externalEntityTargeted.getPosition()));
	    this.externalEntityTargeted = externalEntityTargeted;
    }
	
    @Override
	public boolean update(){
		if(super.update()){
			//Set motion before checking for collisions.  Adjust motion to compensate for bullet movement and gravity.
			//Ignore this if the bullet has a burnTime (rocket motor) that hasn't yet expired,
			//If the bullet is still accelerating, increase the velocity appropriately.
			if(ticksExisted > definition.bullet.burnTime){
				if(definition.bullet.slowdownSpeed > 0){
					motion.add(motion.copy().normalize().multiply(-definition.bullet.slowdownSpeed));
				}
				motion.y -= gun.definition.gun.gravitationalVelocity;
	
				//Check to make sure we haven't gone too many ticks.
				if(ticksExisted > definition.bullet.burnTime + 200){
					if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage("TIMEOUT");
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
			
			//Now that we have an accurate motion, check for collisions.
			//First get a damage object.
			Damage damage = new Damage("bullet", velocity*definition.bullet.diameter/5*ConfigSystem.configObject.damage.bulletDamageFactor.value, boundingBox, gun, null);
			
			//Check for collided external entities and attack them.
			List<WrapperEntity> attackedEntities = world.attackEntities(damage, motion);
			if(!attackedEntities.isEmpty()){
				//Only attack the first entity.  Bullets don't get to attack multiple per scan.
				WrapperEntity entity = attackedEntities.get(0);
				InterfacePacket.sendToServer(new PacketEntityBulletHitWrapper(this, entity));
				lastHit = HitType.ENTITY;
				if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage("HIT ENTITY");
				remove();
				return false;
			}
			
			//Check for collided internal entities and attack them.
			//This is a bit more involved, as we need to check all possible types and check hitbox distance.
			Point3d endPoint = position.copy().add(motion);
			BoundingBox bulletMovmenetBounds = new BoundingBox(endPoint.copy().subtract(position).multiply(0.5D).add(position), Math.abs(motion.x/2D), Math.abs(motion.y/2D), Math.abs(motion.z/2D));
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
							Point3d delta = box.getIntersectionPoint(position, endPoint); 
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
										if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage("HIT TOO MUCH ARMOR.  MAX PEN: " + (int)(definition.bullet.armorPenetration*velocity/initialVelocity));
										remove();
										return false;
									}
								}else{
									APart hitPart = entity.getPartWithBox(hitBox);
									if(hitPart != null){
										InterfacePacket.sendToServer(new PacketEntityBulletHitEntity(this, hitBox, hitPart));
										lastHit = HitType.PART;
										if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage("HIT PART");
										remove();
										return false;
									}else{
										InterfacePacket.sendToServer(new PacketEntityBulletHitEntity(this, hitBox, entity));
										lastHit = HitType.ENTITY;
										if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage("HIT VEHICLE");
										remove();
										return false;
									}
								}
							}
							if(armorPenetrated != 0){
								if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage("PEN ARMOR: " + (int)armorPenetrated + " TOTAL UNITS OUT OF " + (int)(definition.bullet.armorPenetration*velocity/initialVelocity) + " POSSIBLE");
							}
						}
					}
				}
			}
			
			//Didn't hit an entity.  Check for blocks.
			Point3d hitPos = world.getBlockHit(position, motion);
			if(hitPos != null){
				InterfacePacket.sendToServer(new PacketEntityBulletHitBlock(this, hitPos));
				lastHit = HitType.BLOCK;
				if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage("HIT BLOCK");
				remove();
				return false;
			}
			
			//Check proximity fuze against our target or any blocks that might be out front
			if(definition.bullet.proximityFuze != 0){
				if(targetPosition != null){
					double distanceUntilImpact = position.distanceTo(targetPosition);
					if(distanceUntilImpact <= definition.bullet.proximityFuze){
						InterfacePacket.sendToServer(new PacketEntityBulletHit(this, position));
						lastHit = externalEntityTargeted != null ? HitType.ENTITY : (engineTargeted != null ? HitType.PART : HitType.BLOCK);
						if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage("PROX FUSE");
						remove();
						return false;
					}
				}
				
				if(world.getBlockHit(position, motion.copy().normalize().multiply(definition.bullet.proximityFuze)) != null){
					InterfacePacket.sendToServer(new PacketEntityBulletHitBlock(this, position));
					lastHit = HitType.BLOCK;
					if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage("HIT BLOCK");
					remove();
					return false;
				}
			}
			
			//Didn't hit a block either. Check the air-burst time, if it was used.
			if(definition.bullet.airBurstDelay != 0) {
				if(ticksExisted > definition.bullet.airBurstDelay){
					InterfacePacket.sendToServer(new PacketEntityBulletHit(this, position));
					lastHit = HitType.BURST;
					if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceClient.getClientPlayer().displayChatMessage("BURST");
					remove();
					return false;
				}
			}
			
			//Add our updated motion to the position.
			//Then set the angles to match the motion.
			//Doing this last lets us damage on the first update tick.
			if(!isBomb){
				angles.setTo(motion).getAngles(true);
			}
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
