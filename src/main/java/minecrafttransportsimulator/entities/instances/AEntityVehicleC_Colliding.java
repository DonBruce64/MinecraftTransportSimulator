package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
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

			//If we were placed down, and this is our first tick, check our collision boxes to make sure we are't in the ground.
			if(placingPlayer != null && ticksExisted == 2 && !world.isClient()){
				//Get how far above the ground the vehicle needs to be, and move it to that position.
				//First boost Y based on collision boxes.
				double furthestDownPoint = 0;
				for(JSONCollisionGroup collisionGroup : definition.collisionGroups){
					for(JSONCollisionBox collisionBox : collisionGroup.collisions){
						furthestDownPoint = Math.min(collisionBox.pos.y - collisionBox.height/2F, furthestDownPoint);
					}
				}
				
				//Next, boost based on parts.
				for(APart part : parts){
					furthestDownPoint = Math.min(part.placementOffset.y - part.getHeight()/2F, furthestDownPoint);
				}
				
				//Add on -0.1 blocks for the default collision clamping.
				//This prevents the clamping of the collision boxes from hitting the ground if they were clamped.
				furthestDownPoint += -0.1;
				
				//Apply the boost, and check collisions.
				//If the core collisions are colliding, set the vehicle as dead and abort.
				//We need to update the boxes first, however, as they haven't been updated yet.
				position.y += -furthestDownPoint;
				for(BoundingBox coreBox : allBlockCollisionBoxes){
					coreBox.updateToEntity(this, null);
					if(coreBox.updateCollidingBlocks(world, new Point3d(0D, -furthestDownPoint, 0D))){
						//New vehicle shouldn't have been spawned.  Bail out.
						remove();
						placingPlayer.sendPacket(new PacketPlayerChatMessage(placingPlayer, "interact.failure.nospace"));
						//Need to add stack back as it will have been removed here.
						if(!placingPlayer.isCreative()){
							placingPlayer.setHeldStack(getItem().getNewStack());
						}
						return false;
					}
				}
			}
			
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
				Iterator<String> variableIterator = variablesOn.iterator();
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
				if(!world.isBlockLiquid(blockPosition) && blockHardness <= velocity*currentMass/250F && blockHardness >= 0){
					if(ConfigSystem.configObject.general.blockBreakage.value){
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
						motion.set(0D, 0D, 0D);
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
		List<ItemStack> drops = new ArrayList<ItemStack>();
		addDropsToList(drops);
		for(APart part : parts){
			part.addDropsToList(drops);
		}
		for(ItemStack stack : drops){
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
