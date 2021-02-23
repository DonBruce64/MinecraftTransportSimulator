package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.mcinterface.WrapperEntity;

public final class ParticleMissile extends ParticleBullet{
	
	private Point3d targetPosition;
	private PartEngine engineTarget;
	
	private WrapperEntity entityTarget;
	private final double anglePerTickSpeed;
	private final float desiredAngleOfAttack;
	private final float proximityFuzeDistance;

	//Constructor for when an entity could not be found, so a block position will be the target
	public ParticleMissile(Point3d position, Point3d motion, Point3d direction, ItemPart bullet, PartGun gun, WrapperEntity gunController, Point3d targetPosition) {
		super(position, motion, direction, bullet, gun, gunController);
		this.targetPosition = targetPosition;
		this.entityTarget = null;
		this.anglePerTickSpeed = bullet.definition.bullet.turnFactor * 1000/bullet.definition.bullet.diameter;
		this.desiredAngleOfAttack = bullet.definition.bullet.angleOfAttack;
		this.proximityFuzeDistance = bullet.definition.bullet.proximityFuze;
	}
	
	//Passes in an entity to be used as the target
	public ParticleMissile(Point3d position, Point3d motion, Point3d direction, ItemPart bullet, PartGun gun, WrapperEntity gunController, WrapperEntity target) {
		super(position, motion, direction, bullet, gun, gunController);
		this.entityTarget = target;
		this.anglePerTickSpeed = bullet.definition.bullet.turnFactor * 1000/bullet.definition.bullet.diameter;
		this.desiredAngleOfAttack = bullet.definition.bullet.angleOfAttack;
		this.proximityFuzeDistance = bullet.definition.bullet.proximityFuze;
	}
	
	@Override
	public void update() {
		double yawTarget = this.getYaw();
		double pitchTarget = this.getPitch();
		
		//If the target is a valid entity, update target position
		//Otherwise, use the last target position
		//If there is no valid target position, just continue forward
		if (entityTarget != null && entityTarget.isValid()) {
			//If the target is a vehicle, and we're not currently locked onto an engine,
			//Try to find a valid, warm engine to track.
			//If we were tracking an engine, but it is no longer valid or is cold, find a new one.
			AEntityA_Base entityTargetBase = entityTarget.getBaseEntity();
			if(entityTargetBase instanceof EntityVehicleF_Physics) {
				if (engineTarget == null || !engineTarget.isValid || engineTarget.temp < 30f) {
					EntityVehicleF_Physics vehicleTarget = (EntityVehicleF_Physics) entityTargetBase;
					PartEngine nearestEngine = null;
					float smallestDistance = 0f;
					for (APart part : vehicleTarget.parts) {
						if(part instanceof PartEngine) {
							PartEngine currentEngine = (PartEngine) part;
							//Can't see the engine if it's cold
							if(currentEngine.temp < 30f) {
								continue;
							}
							float distanceToEngine = (float)position.distanceTo(currentEngine.position);
							if (nearestEngine == null || distanceToEngine < smallestDistance) {
								nearestEngine = currentEngine;
								smallestDistance = distanceToEngine;
							}
						}
					}
					//If we found a valid, warm engine, track it.
					//Otherwise, forget about this vehicle.
					if (nearestEngine != null) {
						engineTarget = nearestEngine;
						targetPosition = engineTarget.position;
						vehicleTarget.acquireMissile(this);
					}
					else {
						engineTarget = null;
						entityTarget = null;
					}
				}
			}
			//Not a vehicle, so just track its position
			else {
				targetPosition = entityTarget.getPosition();
			}
		}
		
		if (targetPosition != null) {
			yawTarget = Math.toDegrees(Math.atan2(targetPosition.x - position.x, targetPosition.z - position.z));
			pitchTarget = -Math.toDegrees(Math.atan2(targetPosition.y - position.y, Math.hypot(targetPosition.x - position.x, targetPosition.z - position.z)));
			//Remain flat if not yet at desired angle of attack
			//Or climb up if needed to get above the target
			if (pitchTarget > 0 && pitchTarget < desiredAngleOfAttack) {
				if(this.position.y < targetPosition.y + 0.5*desiredAngleOfAttack) {
					pitchTarget = -desiredAngleOfAttack;
				}
				else {
					pitchTarget = 0D;
				}
			}
		}

		//Check proximity fuze against the distance to the target
		if(this.proximityFuzeDistance != 0 && this.targetPosition != null) {
			float distanceUntilImpact = (float)this.position.distanceTo(this.targetPosition);
			if (distanceUntilImpact <= this.proximityFuzeDistance) {
				this.doBulletHit(this.position, motion.length());
				return;
			}
		}
		
		double deltaYaw = yawTarget - this.getYaw();
		double deltaPitch = pitchTarget - this.getPitch();
		
		//Adjust deltaYaw as necessary, then apply it
		while(deltaYaw > 180){
			deltaYaw -= 360;
		}
		while(deltaYaw < -180){
			deltaYaw += 360;
		}
		if(deltaYaw < 0){
			if(deltaYaw < -anglePerTickSpeed){
				deltaYaw = -anglePerTickSpeed;
			}
			motion.rotateFine(new Point3d(0, deltaYaw, 0)); 
		}else if(deltaYaw > 0){
			if(deltaYaw > anglePerTickSpeed){
				deltaYaw = anglePerTickSpeed;
			}
			motion.rotateFine(new Point3d(0, deltaYaw, 0)); 
		}
		
		//Axis for pitch is orthogonal to the horizontal velocity vector
		if(deltaPitch < 0){
			if(deltaPitch < -anglePerTickSpeed){
				deltaPitch = -anglePerTickSpeed;
			}
			motion.rotateFine((new Point3d(motion.z, 0, -1*motion.x)).multiply(deltaPitch)); 
		}else if(deltaPitch > 0){
			if(deltaPitch > anglePerTickSpeed){
				deltaPitch = anglePerTickSpeed;
			}
			motion.rotateFine((new Point3d(motion.z, 0, -1*motion.x)).multiply(deltaPitch)); 
		}
				
		super.update();
	}

}
