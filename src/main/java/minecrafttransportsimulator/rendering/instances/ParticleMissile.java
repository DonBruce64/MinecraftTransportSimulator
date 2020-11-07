package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.vehicles.parts.PartGun;

public final class ParticleMissile extends ParticleBullet {
	
	private final Point3i blockTarget;
	private final double anglePerTickSpeed;

	public ParticleMissile(Point3d position, Point3d motion, ItemPart bullet, PartGun gun, IWrapperEntity gunController, Point3i target) {
		super(position, motion, bullet, gun, gunController);
		
		this.blockTarget = target;
		this.anglePerTickSpeed = bullet.definition.bullet.turnFactor * 100/bullet.definition.bullet.diameter;		
	}
	
	@Override
	public void update() {
		double currentYaw = Math.toDegrees(Math.atan2(motion.x, motion.z));
		double currentPitch = -Math.toDegrees(Math.atan2(motion.y, Math.hypot(motion.x, motion.z)));
		
		double yawTarget = Math.toDegrees(Math.atan2(blockTarget.x - position.x, blockTarget.z - position.z));
		double pitchTarget = -Math.toDegrees(Math.atan2(blockTarget.y - position.y, Math.hypot(blockTarget.x - position.x, blockTarget.z - position.z)));
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
