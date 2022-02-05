package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.systems.ConfigSystem;

/**Now that we have an existing vehicle its time to add the ability to collide with it,
 * and for it to do collision with other entities in the world.  This is where collision
 * bounds are added, as well as the mass of the entity is calculated, as that's required
 * for collision physics forces.  We also add vectors here for the vehicle's orientation,
 * as those are required for us to know how the vehicle collided in the first place.
 * 
 * @author don_bruce
 */
abstract class AEntityVehicleC_Colliding extends AEntityVehicleB_Rideable{
	
	//Internal states.
	private float hardnessHitThisTick = 0;
	public double currentMass;
	public double axialVelocity;
	public final Point3d headingVector = new Point3d();
	public final Point3d verticalVector = new Point3d();
	public final Point3d sideVector = new Point3d();
	public final Point3d normalizedVelocityVector = new Point3d();
	
	public AEntityVehicleC_Colliding(WrapperWorld world, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, placingPlayer, data);
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			world.beginProfiling("VehicleC_Level", true);
			
			//Set vectors to current velocity and orientation.
			world.beginProfiling("SetVectors", true);
			headingVector.set(0D, 0D, 1D).rotateFine(angles);
			verticalVector.set(0D, 1D, 0D).rotateFine(angles);
			sideVector.setTo(verticalVector.crossProduct(headingVector));
			normalizedVelocityVector.setTo(motion).normalize();
			axialVelocity = Math.abs(motion.dotProduct(headingVector));
			
			//Update mass.
			world.beginProfiling("SetMass", false);
			currentMass = getMass();
			
			//Auto-close any open doors that should be closed.
			//Only do this once a second to prevent lag.
			if(velocity > 0.5 && ticksExisted%20 == 0){
				world.beginProfiling("CloseDoors", false);
				Iterator<String> variableIterator = variables.keySet().iterator();
				while(variableIterator.hasNext()){
					if(variableIterator.next().startsWith("door")){
						variableIterator.remove();
					}
				}
			}
			
			//Set hardness hit this tick to 0 to reset collision force calculations.
			hardnessHitThisTick = 0;
			world.endProfiling();
			world.endProfiling();
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Checks collisions and returns the collision depth for a box.
	 * Returns -1 if collision was hard enough to destroy the vehicle.
	 * Returns -2 if the vehicle hit a block but had to stop because blockBreaking was disabled.
	 * Otherwise, we return the collision depth in the specified axis.
	 */
	protected double getCollisionForAxis(BoundingBox box, boolean xAxis, boolean yAxis, boolean zAxis){
		//Get the motion the entity is trying to move, and add it to the passed-in box value.
		Point3d collisionMotion = motion.copy().multiply(SPEED_FACTOR);
		
		//If we collided, so check to see if we can break some blocks or if we need to explode.
		//Don't bother with this logic if it's impossible for us to break anything.
		if(box.updateCollidingBlocks(world, collisionMotion)){
			float hardnessHitThisBox = 0;
			for(Point3d blockPosition : box.collidingBlockPositions){
				float blockHardness = world.getBlockHardness(blockPosition);
				if(!world.isBlockLiquid(blockPosition)){
					if(ConfigSystem.configObject.general.blockBreakage.value && blockHardness <= velocity*currentMass/250F && blockHardness >= 0){
						hardnessHitThisBox += blockHardness;
						if(!yAxis){
							//Only add hardness if we hit in XZ movement.  Don't want to blow up from falling fast, just break tons of dirt.
							hardnessHitThisTick += blockHardness;
						}
						motion.multiply(Math.max(1.0F - blockHardness*0.5F/((1000F + currentMass)/1000F), 0.0F));
						if(!world.isClient()){
							if(ticksExisted > 500){
								world.destroyBlock(blockPosition, true);
							}else{
								motion.set(0D, 0D, 0D);
								return -1;
							}
						}
					}else{
						hardnessHitThisTick = 0;
						motion.set(0, 0, 0);
						return -2;
					}
				}
			}
			
			if(ConfigSystem.configObject.general.vehicleDestruction.value && hardnessHitThisTick > currentMass/(0.75 + velocity)/250F){
				if(!world.isClient()){
					APart partHit = getPartWithBox(box);
					if(partHit != null){
						hardnessHitThisTick -= hardnessHitThisBox;
						removePart(partHit, null);
					}else{
						destroy(box);
					}
				}
				return -1;
			}else if(xAxis){
				return box.currentCollisionDepth.x;
			}else if(yAxis){
				return box.currentCollisionDepth.y;
			}else if(zAxis){
				return box.currentCollisionDepth.z;
			}else{
				throw new IllegalArgumentException("Collision requested but no axis was specified!");
			}
		}else{
			return 0;
		}
	}
	
	@Override
	public void destroy(BoundingBox box){
		super.destroy(box);
		
		//Spawn drops from us and our parts.
		List<WrapperItemStack> drops = new ArrayList<WrapperItemStack>();
		addDropsToList(drops);
		for(APart part : parts){
			part.addDropsToList(drops);
		}
		for(WrapperItemStack stack : drops){
			world.spawnItemStack(stack, box.globalCenter);
		}
		
		//Damage all riders, including the controller.
		WrapperPlayer controller = getController();
		Damage controllerCrashDamage = new Damage("crash", ConfigSystem.configObject.damage.crashDamageFactor.value*velocity*20, null, this, null);
		Damage passengerCrashDamage = new Damage("crash", ConfigSystem.configObject.damage.crashDamageFactor.value*velocity*20, null, this, controller);
		for(WrapperEntity rider : locationRiderMap.values()){
			if(rider.equals(controller)){
				rider.attack(controllerCrashDamage);
			}else{
				rider.attack(passengerCrashDamage);
			}
		}
		
		//Now remove all riders from the vehicle.
		Iterator<WrapperEntity> riderIterator = locationRiderMap.inverse().keySet().iterator();
		while(riderIterator.hasNext()){
			removeRider(riderIterator.next(), riderIterator);
		}
	}
}
