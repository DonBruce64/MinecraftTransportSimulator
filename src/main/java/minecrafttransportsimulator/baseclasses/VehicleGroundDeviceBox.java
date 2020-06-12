package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
	private final EntityVehicleE_Powered vehicle;
	private final boolean isFront;
	private final boolean isLeft;
	
	public boolean isCollided;
	public boolean isCollidedLiquid;
	public boolean isGrounded;
	public boolean isGroundedLiquid;
	public double collisionDepth;
	public VehicleAxisAlignedBB currentBox;
	public double xCoord;
	public double yCoord;
	public double zCoord;
	
	//The following variables are only used for intermediary calculations.
	private final List<PartGroundDevice> groundDevices = new ArrayList<PartGroundDevice>();
	private final List<PartGroundDevice> liquidDevices = new ArrayList<PartGroundDevice>();
	private final List<VehicleAxisAlignedBB> liquidCollisionBoxes = new ArrayList<VehicleAxisAlignedBB>();
	
	
	public VehicleGroundDeviceBox(EntityVehicleE_Powered vehicle, boolean isFront, boolean isLeft){
		this.vehicle = vehicle;
		this.isFront = isFront;
		this.isLeft = isLeft;
		
		//Get all liquid collision boxes during construction.
		//While their actual position may change, their relative position is static.
		for(VehicleAxisAlignedBB box : vehicle.collisionBoxes){
			if(box.collidesWithLiquids){
				if(isFront && box.rel.z > 0){
					if(isLeft && box.rel.x >= 0){
						liquidCollisionBoxes.add(box);
					}else if(!isLeft && box.rel.x <= 0){
						liquidCollisionBoxes.add(box);
					}
				}else if(!isFront && box.rel.z <= 0){
					if(isLeft && box.rel.x >= 0){
						liquidCollisionBoxes.add(box);
					}else if(!isLeft && box.rel.x <= 0){
						liquidCollisionBoxes.add(box);
					}
				}
			}
		}
		
		//Do an initial update once constructed.
		updateGroundDevices();
		update();
	}
	
	/**Updates ground devices for this box.  This may be done independently of updating the box itself to save calculations.*/
	public void updateGroundDevices(){
		groundDevices.clear();
		liquidDevices.clear();
		for(APart part : vehicle.getVehicleParts()){
			if(part instanceof PartGroundDevice){
				//X-offsets of 0 are both left and right as they are center points.
				//This ensures we don't roll to try and align a center point.
				if(isFront && part.placementOffset.z > 0){
					if(isLeft && part.placementOffset.x >= 0){
						groundDevices.add((PartGroundDevice) part);
						if(part.definition.ground.canFloat){
							liquidDevices.add((PartGroundDevice) part);
						}
					}else if(!isLeft && part.placementOffset.x <= 0){
						groundDevices.add((PartGroundDevice) part);
						if(part.definition.ground.canFloat){
							liquidDevices.add((PartGroundDevice) part);
						}
					}
				}else if(!isFront && part.placementOffset.z <= 0){
					if(isLeft && part.placementOffset.x >= 0){
						groundDevices.add((PartGroundDevice) part);
						if(part.definition.ground.canFloat){
							liquidDevices.add((PartGroundDevice) part);
						}
					}else if(!isLeft && part.placementOffset.x <= 0){
						groundDevices.add((PartGroundDevice) part);
						if(part.definition.ground.canFloat){
							liquidDevices.add((PartGroundDevice) part);
						}
					}
				}
			}
		}
	}
	
	/**Updates this box, taking into account all ground devices and collision boxes.*/
	public void update(){
		//Initialize all values.
		isCollided = false;
		isGrounded = false;
		collisionDepth = 0;
		if(!groundDevices.isEmpty()){
			currentBox = getSolidPoint();
			final List<AxisAlignedBB> groundCollidingBoxes = currentBox.getAABBCollisions(vehicle.world, null);
			isCollided = !groundCollidingBoxes.isEmpty();
			isGrounded = isCollided ? false : !currentBox.offset(0, PartGroundDevice.groundDetectionOffset.y, 0).getAABBCollisions(vehicle.world, null).isEmpty();
			collisionDepth = isCollided ? getCollisionDepthForCollisions(currentBox, groundCollidingBoxes) : 0;
			xCoord = currentBox.rel.x;
			yCoord = currentBox.rel.y - currentBox.height/2D;
			zCoord = currentBox.rel.z;
		}
		
		if(!liquidDevices.isEmpty() || !liquidCollisionBoxes.isEmpty()){
			final VehicleAxisAlignedBB liquidCollisionBox = getLiquidPoint();
			final List<AxisAlignedBB> liquidCollidingBoxes = getLiquidCollisions(liquidCollisionBox, vehicle.world);
			//Liquids are checked a bit differently as we already checked solids.
			isCollidedLiquid = !liquidCollidingBoxes.isEmpty();
			isGroundedLiquid = isCollidedLiquid ? false : !getLiquidCollisions(liquidCollisionBox.offset(0, PartGroundDevice.groundDetectionOffset.y, 0), vehicle.world).isEmpty(); 
			double liquidCollisionDepth = isCollidedLiquid ? getCollisionDepthForCollisions(liquidCollisionBox, liquidCollidingBoxes) : 0;
			
			//If the liquid boxes are more collided, set collisions to those.
			//Otherwise, use the solid values.
			if(isCollidedLiquid && (liquidCollisionDepth > collisionDepth)){
				isCollided = isCollidedLiquid;
				isGrounded = isGroundedLiquid;
				collisionDepth = liquidCollisionDepth;
				xCoord = liquidCollisionBox.rel.x;
				yCoord = liquidCollisionBox.rel.y - liquidCollisionBox.height/2D;
				zCoord = liquidCollisionBox.rel.z;
			}
		}
	}
	
	/**Gets the solid collision point based on position of ground devices.**/
	private VehicleAxisAlignedBB getSolidPoint(){
		float heights = 0;
		float widths = 0;
		double xCoords = 0;
		double yCoords = 0;
		double zCoords = 0;
		
		for(APart groundDevice : groundDevices){
			heights += groundDevice.getHeight();
			widths += groundDevice.getWidth();
			xCoords += groundDevice.totalOffset.x;
			yCoords += groundDevice.totalOffset.y;
			zCoords += groundDevice.totalOffset.z;
		}
		
		heights /= groundDevices.size();
		widths /= groundDevices.size();
		xCoords /= groundDevices.size();
		yCoords /= groundDevices.size();
		zCoords /= groundDevices.size();
		
		Point3d boxRelativePosition = new Point3d(xCoords, yCoords, zCoords);
		Point3d offset = RotationSystem.getRotatedPoint(boxRelativePosition, vehicle.rotationPitch + vehicle.motionPitch, vehicle.rotationYaw + vehicle.motionYaw, vehicle.rotationRoll + vehicle.motionRoll);
		return new VehicleAxisAlignedBB(offset.add(vehicle.currentPosition).add(vehicle.motionX*vehicle.SPEED_FACTOR, vehicle.motionY*vehicle.SPEED_FACTOR, vehicle.motionZ*vehicle.SPEED_FACTOR), boxRelativePosition, widths, heights, false, false);
	}
	
	/**Updates the liquid collision point based on position of liquid devices and collision boxes.**/
	private VehicleAxisAlignedBB getLiquidPoint(){
		float heights = 0;
		float widths = 0;
		double xCoords = 0;
		double yCoords = 0;
		double zCoords = 0;
		
		for(APart groundDevice : liquidDevices){
			heights += groundDevice.getHeight();
			widths += groundDevice.getWidth();
			xCoords += groundDevice.totalOffset.x;
			yCoords += groundDevice.totalOffset.y;
			zCoords += groundDevice.totalOffset.z;
		}
		
		for(VehicleAxisAlignedBB box : liquidCollisionBoxes){
			heights += box.height;
			widths += box.width;
			xCoords += box.rel.x;
			yCoords += box.rel.y;
			zCoords += box.rel.z;
		}
		
		heights /= (liquidDevices.size() + liquidCollisionBoxes.size());
		widths /= (liquidDevices.size() + liquidCollisionBoxes.size());
		xCoords /= (liquidDevices.size() + liquidCollisionBoxes.size());
		yCoords /= (liquidDevices.size() + liquidCollisionBoxes.size());
		zCoords /= (liquidDevices.size() + liquidCollisionBoxes.size());
		
		Point3d boxRelativePosition = new Point3d(xCoords, yCoords, zCoords);
		Point3d offset = RotationSystem.getRotatedPoint(boxRelativePosition, vehicle.rotationPitch + vehicle.motionPitch, vehicle.rotationYaw + vehicle.motionYaw, vehicle.rotationRoll + vehicle.motionRoll);
		return new VehicleAxisAlignedBB(offset.add(vehicle.currentPosition).add(vehicle.motionX*vehicle.SPEED_FACTOR, vehicle.motionY*vehicle.SPEED_FACTOR, vehicle.motionZ*vehicle.SPEED_FACTOR), boxRelativePosition, widths, heights, false, true);
	}
	
	/**
	 * Helper function for calculating the collision depth between a box and the boxes that collide with it.
	 * This function is used for ground device collisions only, and makes some assumptions that are incorrect
	 * for a general-purpose function.
	 */
	private static double getCollisionDepthForCollisions(VehicleAxisAlignedBB groundDeviceBox, List<AxisAlignedBB> boxList){
		double collisionDepth = 0;
		for(AxisAlignedBB box : boxList){
			if(groundDeviceBox.minY < box.maxY){
				collisionDepth = Math.max(collisionDepth, box.maxY - groundDeviceBox.minY);
			}
		}
		//Don't return small collisions.  These can be due to floating-point errors in calculations.		
		if(Math.abs(collisionDepth) < 0.0001){
			return 0;
		}else{
			return collisionDepth;
		}
	}
	
	
	/**
	 * Returns the collisions with liquids for a given box, and not the collisions with solid blocks.
	 * Used for doing liquid collisions with liquid ground device parts.
	 */
	private static List<AxisAlignedBB> getLiquidCollisions(VehicleAxisAlignedBB box, World world){
		int minTestX = (int) Math.floor(box.minX);
    	int maxTestX = (int) Math.floor(box.maxX + 1.0D);
    	int minTestY = (int) Math.floor(box.minY);
    	int maxTestY = (int) Math.floor(box.maxY + 1.0D);
    	int minTestZ = (int) Math.floor(box.minZ);
    	int maxTestZ = (int) Math.floor(box.maxZ + 1.0D);
    	List<AxisAlignedBB> collidingAABBList = new ArrayList<AxisAlignedBB>();
    	
    	for(int i = minTestX; i < maxTestX; ++i){
    		for(int j = minTestY; j < maxTestY; ++j){
    			for(int k = minTestZ; k < maxTestZ; ++k){
    				BlockPos pos = new BlockPos(i, j, k);
    				IBlockState state = world.getBlockState(pos);
					if(state.getMaterial().isLiquid()){
						collidingAABBList.add(state.getBoundingBox(world, pos).offset(pos));
					}
    			}
    		}
    	}
		return collidingAABBList;
	}
}
