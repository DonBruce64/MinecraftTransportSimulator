package minecrafttransportsimulator.entities.instances;

import java.util.Iterator;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONDoor;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packloading.PackMaterialComponent;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.item.ItemStack;

/**Now that we have an existing vehicle its time to add the ability to collide with it,
 * and for it to do collision with other entities in the world.  This is where collision
 * bounds are added, as well as the mass of the entity is calculated, as that's required
 * for collision physics forces.  We also add vectors here for the vehicle's orientation,
 * as those are required for us to know how the vehicle collided in the first place.
 * 
 * @author don_bruce
 */
abstract class EntityVehicleC_Colliding extends EntityVehicleB_Rideable{
	
	//Internal states.
	private float hardnessHitThisTick = 0;
	public double currentMass;
	public double velocity;
	public double axialVelocity;
	public final Point3d headingVector = new Point3d();
	public final Point3d verticalVector = new Point3d();
	public final Point3d sideVector = new Point3d();
	public final Point3d normalizedVelocityVector = new Point3d();
	
	public EntityVehicleC_Colliding(WrapperWorld world, WrapperNBT data){
		super(world, data);
	}
	
	@Override
	public void update(){
		super.update();
		//Set vectors to current velocity and orientation.
		headingVector.set(0D, 0D, 1D).rotateFine(angles);
		verticalVector.set(0D, 1D, 0D).rotateFine(angles);
		sideVector.setTo(verticalVector.crossProduct(headingVector));
		normalizedVelocityVector.setTo(motion).normalize();
		velocity = motion.length();
		axialVelocity = Math.abs(motion.dotProduct(headingVector));
		
		//Update mass.
		currentMass = getCurrentMass();
		
		//Auto-close any open doors that should be closed.
		//Only do this once a second to prevent lag.
		if(definition.doors != null && velocity > 0.5 && ticksExisted%20 == 0){
			Iterator<String> variableIterator = variablesOn.iterator();
			while(variableIterator.hasNext()){
				String openDoorName = variableIterator.next();
				for(JSONDoor doorDef : definition.doors){
					if(doorDef.name.equals(openDoorName)){
						if(doorDef.closeOnMovement){
							variableIterator.remove();
						}
						break;
					}
				}
			}
		}
		
		//Set hardness hit this tick to 0 to reset collision force calculations.
		hardnessHitThisTick = 0;
	}
	
	/**
	 * Checks collisions and returns the collision depth for a box.
	 * Returns -1 if collision was hard enough to destroy the vehicle.
	 * Otherwise, we return the collision depth in the specified axis.
	 */
	protected double getCollisionForAxis(BoundingBox box, boolean xAxis, boolean yAxis, boolean zAxis){
		//Get the motion the entity is trying to move, and add it to the passed-in box value.
		Point3d collisionMotion = motion.copy().multiply(SPEED_FACTOR);
		
		//If we collided, so check to see if we can break some blocks or if we need to explode.
		//Don't bother with this logic if it's impossible for us to break anything.
		if(box.updateCollidingBlocks(world, collisionMotion)){
			for(Point3d blockPosition : box.collidingBlockPositions){
				float blockHardness = world.getBlockHardness(blockPosition);
				if(!world.isBlockLiquid(blockPosition) && blockHardness <= velocity*currentMass/250F && blockHardness >= 0){
					if(ConfigSystem.configObject.general.blockBreakage.value){
						hardnessHitThisTick += blockHardness;
						motion.multiply(Math.max(1.0F - blockHardness*0.5F/((1000F + currentMass)/1000F), 0.0F));
						if(!world.isClient()){
							if(ticksExisted > 500){
								world.destroyBlock(blockPosition);
							}else{
								motion.set(0D, 0D, 0D);
								return -1;
							}
						}
					}else{
						hardnessHitThisTick = 0;
						motion.set(0D, 0D, 0D);
					}
				}
			}
			
			if(ConfigSystem.configObject.general.vehicleDestruction.value && hardnessHitThisTick > currentMass/(0.75 + velocity)/250F){
				if(!world.isClient()){
					destroyAt(box.globalCenter);
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
	
	/**
	 * Call this to remove this vehicle.  This should be called when the vehicle has crashed, as it
	 * ejects all parts and damages all players.  Explosions may not occur in crashes depending on config 
	 * settings or a lack of fuel or explodable cargo.  Call only on the SERVER as this is for item-spawning 
	 * code and player damage code.
	 */
	public void destroyAt(Point3d location){
		//Do normal removal operations.
		remove();
		
		//Remove all parts from the vehicle and place them as items.
		for(APart part : parts){
			if(part.getItem() != null){
				WrapperNBT partData = new WrapperNBT();
				part.save(partData);
				world.spawnItem(part.getItem(), partData, part.position);
			}
		}
		
		//Also drop some crafting ingredients as items.
		for(PackMaterialComponent material : PackMaterialComponent.parseFromJSON(getItem(), true, true, false)){
			for(ItemStack stack : material.possibleItems){
				if(Math.random() < ConfigSystem.configObject.damage.crashItemDropPercentage.value){
					world.spawnItemStack(new ItemStack(stack.getItem(), material.qty, material.meta), location);
				}
				break;
			}
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
