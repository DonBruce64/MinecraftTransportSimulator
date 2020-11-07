package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.vehicles.parts.PartGun;

public final class ParticleMissile extends ParticleBullet {
	
	private Point3d targetPosition;
	
	private final IWrapperEntity entityTarget;
	private final double anglePerTickSpeed;
	private final float desiredAngleOfAttack;

	//Constructor for when an entity could not be found, so a block position will be the target
	public ParticleMissile(Point3d position, Point3d motion, ItemPart bullet, PartGun gun, IWrapperEntity gunController, Point3i target) {
		super(position, motion, bullet, gun, gunController);
		this.targetPosition = new Point3d(target.x, target.y, target.z);
		this.entityTarget = null;
		this.anglePerTickSpeed = bullet.definition.bullet.turnFactor * 1000/bullet.definition.bullet.diameter;
		this.desiredAngleOfAttack = bullet.definition.bullet.angleOfAttack;
	}
	
	//Passes in an entity to be used as the target
	public ParticleMissile(Point3d position, Point3d motion, ItemPart bullet, PartGun gun, IWrapperEntity gunController, IWrapperEntity target) {
		super(position, motion, bullet, gun, gunController);
		this.entityTarget = target;
		this.anglePerTickSpeed = bullet.definition.bullet.turnFactor * 1000/bullet.definition.bullet.diameter;
		this.desiredAngleOfAttack = bullet.definition.bullet.angleOfAttack;	
	}
	
	@Override
	public void update() {
		double currentYaw = Math.toDegrees(Math.atan2(motion.x, motion.z));
		double currentPitch = -Math.toDegrees(Math.atan2(motion.y, Math.hypot(motion.x, motion.z)));
		
		double yawTarget = currentYaw;
		double pitchTarget = currentPitch;
		
		//If the target is a valid entity, update target position
		//Otherwise, use the last position
		if (entityTarget != null && entityTarget.isValid()) {
			targetPosition = entityTarget.getPosition();
		}
		if (targetPosition != null) {
			yawTarget = Math.toDegrees(Math.atan2(targetPosition.x - position.x, targetPosition.z - position.z));
			pitchTarget = -Math.toDegrees(Math.atan2(targetPosition.y - position.y, Math.hypot(targetPosition.x - position.x, targetPosition.z - position.z)));
			//Remain flat if not yet at desired angle of attack
			if (pitchTarget < desiredAngleOfAttack) {
				pitchTarget = 0D;
			}
		}
		double deltaYaw = yawTarget - currentYaw;
		double deltaPitch = pitchTarget - currentPitch;
		
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
				//lockedOn = false;
			}
			motion.rotateFine(new Point3d(0, deltaYaw, 0)); 
		}else if(deltaYaw > 0){
			if(deltaYaw > anglePerTickSpeed){
				deltaYaw = anglePerTickSpeed;
				//lockedOn = false;
			}
			motion.rotateFine(new Point3d(0, deltaYaw, 0)); 
		}
		
		//Axis for pitch is orthogonal to the horizontal velocity vector
		if(deltaPitch < 0){
			if(deltaPitch < -anglePerTickSpeed){
				deltaPitch = -anglePerTickSpeed;
				//lockedOn = false;
			}
			motion.rotateFine((new Point3d(motion.z, 0, -1*motion.x)).multiply(deltaPitch)); 
		}else if(deltaPitch > 0){
			if(deltaPitch > anglePerTickSpeed){
				deltaPitch = anglePerTickSpeed;
				//lockedOn = false;
			}
			motion.rotateFine((new Point3d(motion.z, 0, -1*motion.x)).multiply(deltaPitch)); 
		}
				
		super.update();
	}

}
