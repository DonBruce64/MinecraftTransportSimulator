package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;

/**This class is a wrapper for vehicle ground device collision points.  It's used to get a point
 * to reference for ground collisions, and contains helper methods for doing calculations of those
 * points.  Four of these can be used in a set to get four ground device points to use in
 * ground device operations on a vehicle.  Note that this class differentiates between floating
 * and non-floating objects, and includes collision boxes for the latter.  This ensures a
 * seamless transition from a floating to ground state in movement.
 * 
 * @author don_bruce
 */
public class VehicleGroundDeviceBox{
	private final EntityVehicleF_Physics vehicle;
	private final boolean isFront;
	private final boolean isLeft;
	private final BoundingBox solidBox = new BoundingBox(new Point3d(), new Point3d(), 0D, 0D, 0D, false, false, false, 0);
	private final BoundingBox liquidBox = new BoundingBox(new Point3d(), new Point3d(), 0D, 0D, 0D, true, false, false, 0);
	private final List<BoundingBox> liquidCollisionBoxes = new ArrayList<BoundingBox>();
	private final List<PartGroundDevice> groundDevices = new ArrayList<PartGroundDevice>();
	private final List<PartGroundDevice> liquidDevices = new ArrayList<PartGroundDevice>();
	
	public boolean canRollOnGround;
	public boolean contactedEntity;
	public boolean isCollided;
	public boolean isCollidedLiquid;
	public boolean isGrounded;
	public boolean isGroundedLiquid;
	public boolean isAbleToDoGroundOperations;
	public boolean isAbleToDoGroundOperationsLiquid;
	public boolean isLiquidCollidedWithGround;
	public double collisionDepth;
	public final Point3d contactPoint = new Point3d();
	
	public VehicleGroundDeviceBox(EntityVehicleF_Physics vehicle, boolean isFront, boolean isLeft){
		this.vehicle = vehicle;
		this.isFront = isFront;
		this.isLeft = isLeft;
		
		//Do an initial update once constructed.
		updateMembers();
		updateBounds();
		updateCollisionStatuses(null);
	}
	
	/**
	 * Updates what objects make up this GDB.  These should change as parts are added and removed.
	 */
	public void updateMembers(){
		//Get all liquid collision boxes.  Parts can add these via their collision boxes.
		liquidCollisionBoxes.clear();
		for(BoundingBox box : vehicle.allBlockCollisionBoxes){
			APart partOn = vehicle.getPartWithBox(box);
			final boolean boxFront;
			final boolean boxLeft;
			final boolean boxRight;
			if(partOn != null){
				boxFront = partOn.placementOffset.z > 0;
				boxLeft = partOn.placementOffset.x >= 0;
				boxRight = partOn.placementOffset.x <= 0;
			}else{
				boxFront = box.localCenter.z > 0;
				boxLeft = box.localCenter.x >= 0;
				boxRight = box.localCenter.x <= 0;
			}
			if(box.collidesWithLiquids){
				if(isFront && boxFront){
					if(isLeft && boxLeft){
						liquidCollisionBoxes.add(box);
					}else if(!isLeft && boxRight){
						liquidCollisionBoxes.add(box);
					}
				}else if(!isFront && !boxFront){
					if(isLeft && boxLeft){
						liquidCollisionBoxes.add(box);
					}else if(!isLeft && boxRight){
						liquidCollisionBoxes.add(box);
					}
				}
			}
		}
		
		//Get all part-based collision boxes.  This includes solid and liquid ground devices.
		groundDevices.clear();
		liquidDevices.clear();
		canRollOnGround = false;
		for(APart part : vehicle.parts){
			if(part instanceof PartGroundDevice){
				if(!part.placementDefinition.isSpare && part.isActive){
					//X-offsets of 0 are both left and right as they are center points.
					//This ensures we don't roll to try and align a center point.
					if(isFront && part.placementOffset.z > 0){
						if(isLeft && part.placementOffset.x >= 0){
							groundDevices.add((PartGroundDevice) part);
							if(part.definition.ground.isWheel || part.definition.ground.isTread){
								canRollOnGround = true;
							}
							if(part.definition.ground.canFloat){
								liquidDevices.add((PartGroundDevice) part);
							}
						}else if(!isLeft && part.placementOffset.x <= 0){
							groundDevices.add((PartGroundDevice) part);
							if(part.definition.ground.isWheel || part.definition.ground.isTread){
								canRollOnGround = true;
							}
							if(part.definition.ground.canFloat){
								liquidDevices.add((PartGroundDevice) part);
							}
						}
					}else if(!isFront && part.placementOffset.z <= 0){
						if(isLeft && part.placementOffset.x >= 0){
							groundDevices.add((PartGroundDevice) part);
							if(part.definition.ground.isWheel || part.definition.ground.isTread){
								canRollOnGround = true;
							}
							if(part.definition.ground.canFloat){
								liquidDevices.add((PartGroundDevice) part);
							}
						}else if(!isLeft && part.placementOffset.x <= 0){
							groundDevices.add((PartGroundDevice) part);
							if(part.definition.ground.isWheel || part.definition.ground.isTread){
								canRollOnGround = true;
							}
							if(part.definition.ground.canFloat){
								liquidDevices.add((PartGroundDevice) part);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Updates this boxes' bounds to match the included members.  This should only be done when we
	 * change members, or if a member has changed position.
	 */
	public void updateBounds(){
		//Update solid box local center and size.
		//We use the lowest-contacting ground device for size.
		//Position is average for XZ, and min for Y.
		solidBox.localCenter.set(0D, Double.MAX_VALUE, 0D);
		solidBox.widthRadius = 0;
		solidBox.heightRadius = 0;
		for(APart groundDevice : groundDevices){
			solidBox.localCenter.x += groundDevice.localOffset.x;
			solidBox.localCenter.z += groundDevice.localOffset.z;
			if(groundDevice.localOffset.y - groundDevice.getHeight()/2D < solidBox.localCenter.y - solidBox.heightRadius){
				solidBox.localCenter.y = groundDevice.localOffset.y;
				solidBox.heightRadius = groundDevice.getHeight()/2D;
				solidBox.widthRadius = groundDevice.getWidth()/2D;
			}
		}
		solidBox.depthRadius = solidBox.widthRadius;
		solidBox.localCenter.x *= 1D/groundDevices.size();
		solidBox.localCenter.z *= 1D/groundDevices.size();
		
		//Update liquid box local center and size.
		liquidBox.localCenter.set(0D, Double.MAX_VALUE, 0D);
		liquidBox.widthRadius = 0;
		liquidBox.heightRadius = 0;
		for(APart groundDevice : liquidDevices){
			liquidBox.localCenter.x += groundDevice.localOffset.x;
			liquidBox.localCenter.z += groundDevice.localOffset.z;
			if(groundDevice.localOffset.y - groundDevice.getHeight()/2D < liquidBox.localCenter.y - liquidBox.heightRadius){
				liquidBox.localCenter.y = groundDevice.localOffset.y;
				liquidBox.heightRadius = groundDevice.getHeight()/2D;
				liquidBox.widthRadius = groundDevice.getWidth()/2D;
			}
		}
		for(BoundingBox box : liquidCollisionBoxes){
			liquidBox.localCenter.x += box.localCenter.x;
			liquidBox.localCenter.z += box.localCenter.z;
			if(box.localCenter.y - box.heightRadius < liquidBox.localCenter.y - liquidBox.heightRadius){
				liquidBox.localCenter.y = box.localCenter.y;
				liquidBox.heightRadius = box.heightRadius;
				liquidBox.widthRadius = box.widthRadius;
			}
		}
		liquidBox.depthRadius = liquidBox.widthRadius;
		liquidBox.localCenter.x *= 1D/(liquidDevices.size() + liquidCollisionBoxes.size());
		liquidBox.localCenter.z *= 1D/(liquidDevices.size() + liquidCollisionBoxes.size());
	}
	
	/**
	 * Updates this boxes' collision properties to take into account its new position.
	 * If the passed-in list is non-null, all grounded ground devices will be added to it.
	 */
	public void updateCollisionStatuses(Set<PartGroundDevice> groundedGroundDevices){
		//Initialize all values.
		isCollided = false;
		isGrounded = false;
		isAbleToDoGroundOperations = false;
		isCollidedLiquid = false;
		isGroundedLiquid = false;
		isAbleToDoGroundOperationsLiquid = false;
		isLiquidCollidedWithGround = false;
		collisionDepth = 0;
		
		Point3d vehicleMotionOffset = vehicle.motion.copy().multiply(EntityVehicleF_Physics.SPEED_FACTOR);
		Point3d groundCollisionOffset = vehicleMotionOffset.copy().add(PartGroundDevice.groundDetectionOffset);
		if(!groundDevices.isEmpty()){
			contactPoint.setTo(solidBox.localCenter).add(0D, -solidBox.heightRadius, 0D);
			solidBox.globalCenter.setTo(solidBox.localCenter).rotateFine(vehicle.angles.copy().add(vehicle.rotation)).add(vehicle.position).add(vehicleMotionOffset);
			vehicle.world.updateBoundingBoxCollisions(solidBox, vehicleMotionOffset, false);
			contactedEntity = checkEntityCollisions(vehicleMotionOffset);
			isCollided = contactedEntity || !solidBox.collidingBlockPositions.isEmpty();
			collisionDepth = solidBox.currentCollisionDepth.y;
			PartGroundDevice.groundOperationOffset.set(0 , -0.5, 0);
			if(isCollided){
				isGrounded = true;
			}else{
				solidBox.globalCenter.add(PartGroundDevice.groundDetectionOffset);
				vehicle.world.updateBoundingBoxCollisions(solidBox, groundCollisionOffset, false);
				contactedEntity = checkEntityCollisions(groundCollisionOffset);
				solidBox.globalCenter.subtract(PartGroundDevice.groundDetectionOffset);
				isGrounded = contactedEntity || !solidBox.collidingBlockPositions.isEmpty();
			}
			
			if(isGrounded){
				isAbleToDoGroundOperations = true;
			}else{
				groundCollisionOffset = vehicleMotionOffset.copy().add(PartGroundDevice.groundOperationOffset);
				solidBox.globalCenter.add(PartGroundDevice.groundOperationOffset);
				vehicle.world.updateBoundingBoxCollisions(solidBox, groundCollisionOffset, false);
				contactedEntity = checkEntityCollisions(groundCollisionOffset);
				solidBox.globalCenter.subtract(PartGroundDevice.groundOperationOffset);
				isAbleToDoGroundOperations = contactedEntity || !solidBox.collidingBlockPositions.isEmpty();
			}
		}
		
		if(!canRollOnGround || !isAbleToDoGroundOperations){
			if(!liquidDevices.isEmpty() || !liquidCollisionBoxes.isEmpty()){
				liquidBox.globalCenter.setTo(liquidBox.localCenter).rotateFine(vehicle.angles.copy().add(vehicle.rotation)).add(vehicle.position).add(vehicleMotionOffset);
				vehicle.world.updateBoundingBoxCollisions(liquidBox, vehicleMotionOffset, false);
				isCollidedLiquid = !liquidBox.collidingBlockPositions.isEmpty();
				double liquidCollisionDepth = liquidBox.currentCollisionDepth.y;
				
				if(isCollidedLiquid){
					isGroundedLiquid = true;
				}else{
					liquidBox.globalCenter.add(PartGroundDevice.groundDetectionOffset);
					vehicle.world.updateBoundingBoxCollisions(liquidBox, groundCollisionOffset, false);
					liquidBox.globalCenter.subtract(PartGroundDevice.groundDetectionOffset);
					isGroundedLiquid = !liquidBox.collidingBlockPositions.isEmpty();
				}
				
				if(isGroundedLiquid){
					isAbleToDoGroundOperationsLiquid = true;
				}else{
					groundCollisionOffset = vehicleMotionOffset.copy().add(PartGroundDevice.groundOperationOffset);
					liquidBox.globalCenter.add(PartGroundDevice.groundOperationOffset);
					vehicle.world.updateBoundingBoxCollisions(liquidBox, groundCollisionOffset, false);
					liquidBox.globalCenter.subtract(PartGroundDevice.groundOperationOffset);
					isAbleToDoGroundOperationsLiquid = !liquidBox.collidingBlockPositions.isEmpty();
				}
				
				isLiquidCollidedWithGround = false;
				for(Point3d blockPosition : liquidBox.collidingBlockPositions){
					if(!vehicle.world.isBlockLiquid(blockPosition)){
						isLiquidCollidedWithGround = true;
						break;
					}
				}
				
				//If the liquid boxes are grounded and are more collided, use liquid values.
				//Otherwise, use the solid values (if we have them).
				if((isGroundedLiquid && (liquidCollisionDepth >= collisionDepth)) || groundDevices.isEmpty()){
					isCollided = isCollidedLiquid;
					isGrounded = isGroundedLiquid;
					isAbleToDoGroundOperations = isAbleToDoGroundOperationsLiquid;
					collisionDepth = liquidCollisionDepth;
					contactPoint.setTo(liquidBox.localCenter).add(0D, -liquidBox.heightRadius, 0D);
				}
			}
		}else{
			isCollidedLiquid = false;
			isGroundedLiquid = false;
			isAbleToDoGroundOperationsLiquid = false;
		}
		
		//Add ground devices to the list.
		if(groundedGroundDevices != null && isAbleToDoGroundOperations){
			groundedGroundDevices.addAll(groundDevices);
		}
	}
	
	/**
	 * Helper method for checking for entity collisions.
	 */
	private boolean checkEntityCollisions(Point3d collisionMotion){
		boolean didCollision = false;
		//Commented out as LT ents don't support collision yet.
		/*for(WrapperEntity entity : vehicle.world.getEntitiesClassNamed("com.creativemd.littletiles.common.entity.EntityAnimation")){
			if(entity.updateBoundingBoxCollisions(solidBox, collisionMotion, false)){
				didCollision = true;
			}
		}*/
		for(AEntityC_Definable<?> entity : AEntityC_Definable.getRenderableEntities(vehicle.world)){
			if(entity instanceof AEntityD_Interactable && !entity.equals(vehicle)){
				AEntityD_Interactable<?> interactable = (AEntityD_Interactable<?>) entity;
				if(vehicle.canCollideWith(interactable) && !interactable.collidedEntities.contains(vehicle) && interactable.boundingBox.intersects(solidBox)){
					//We know we could have hit this entity.  Check if we actually did.
					BoundingBox collidingBox = null;
					double boxCollisionDepth = 0;
					for(BoundingBox box : interactable.getCollisionBoxes()){
						if(box.intersects(solidBox)){
							if(collisionMotion.y > 0){
								boxCollisionDepth = solidBox.globalCenter.y + solidBox.heightRadius - (box.globalCenter.y - box.heightRadius);
								if(boxCollisionDepth > solidBox.currentCollisionDepth.y){
									solidBox.currentCollisionDepth.y = boxCollisionDepth;
									collidingBox = box;
								}
							}else{
								boxCollisionDepth = box.globalCenter.y + box.heightRadius - (solidBox.globalCenter.y - solidBox.heightRadius);
								if(boxCollisionDepth > solidBox.currentCollisionDepth.y){
									solidBox.currentCollisionDepth.y = boxCollisionDepth;
									collidingBox = box;
								}
							}
						}
					}
					if(collidingBox != null){
						vehicle.collidedEntities.add(interactable);
						didCollision = true;
					}
				}
			}
		}
		return didCollision;
	}
	
	/**
	 * Returns true if this box has any boxes and is ready for collision operations.
	 */
	public boolean isReady(){
		return !groundDevices.isEmpty() || !liquidCollisionBoxes.isEmpty() || !liquidDevices.isEmpty();
	}
	
	/**
	 * Returns the bounding box that currently represents this box.  This can change depending on what
	 * ground devices we have and if we are colliding with liquids or solids.
	 */
	public BoundingBox getBoundingBox(){
		return isAbleToDoGroundOperationsLiquid || groundDevices.isEmpty() ? liquidBox : solidBox;
	}
}
