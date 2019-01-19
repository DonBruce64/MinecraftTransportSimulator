package minecrafttransportsimulator.baseclasses;

import javax.annotation.Nullable;

import minecrafttransportsimulator.vehicles.main.EntityVehicleC_Colliding;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**This "collective" is essentially a wrapper AABB class.  It intercepts all AABB calls
 * and re-routes them to all the AABBs in the passed-in vehicle.  Essentially,
 * it's a way to have one vehicle with multiple collision AABBs.
 * 
 * @author don_bruce
 */
public class VehicleAxisAlignedBBCollective extends VehicleAxisAlignedBB{
	private final EntityVehicleC_Colliding vehicle;
	
	public VehicleAxisAlignedBBCollective(EntityVehicleC_Colliding vehicle, float width, float height){
		super(vehicle.getPositionVector(), Vec3d.ZERO, width, height, false);
		this.vehicle = vehicle;
	}
	
	@Override
	public VehicleAxisAlignedBBCollective expandXyz(double value){
		return this;
    }
	
	@Override
	public double calculateXOffset(AxisAlignedBB other, double offsetX){
		for(VehicleAxisAlignedBB box : vehicle.getCurrentCollisionBoxes()){
			offsetX = box.calculateXOffset(other, offsetX);
		}
		for(VehicleAxisAlignedBB box : vehicle.getCurrentInteractionBoxes()){
			offsetX = box.calculateXOffset(other, offsetX);
		}
		return offsetX;
    }
	
	@Override
	public double calculateYOffset(AxisAlignedBB other, double offsetY){
		for(VehicleAxisAlignedBB box : vehicle.getCurrentCollisionBoxes()){
			offsetY = box.calculateYOffset(other, offsetY);
		}
		for(VehicleAxisAlignedBB box : vehicle.getCurrentInteractionBoxes()){
			offsetY = box.calculateXOffset(other, offsetY);
		}
		return offsetY;
    }
	
	@Override
	public double calculateZOffset(AxisAlignedBB other, double offsetZ){
		for(VehicleAxisAlignedBB box : vehicle.getCurrentCollisionBoxes()){
			offsetZ = box.calculateZOffset(other, offsetZ);
		}
		for(VehicleAxisAlignedBB box : vehicle.getCurrentInteractionBoxes()){
			offsetZ = box.calculateXOffset(other, offsetZ);
		}
		return offsetZ;
    }
	
	@Override
    public boolean intersectsWith(AxisAlignedBB other){
		for(VehicleAxisAlignedBB box : vehicle.getCurrentCollisionBoxes()){
			if(box.intersectsWith(other)){
				return true;
			}
		}
		for(VehicleAxisAlignedBB box : vehicle.getCurrentInteractionBoxes()){
			if(box.intersectsWith(other)){
				return true;
			}
		}
		return false;
    }
	
	@Override
    public boolean intersects(double x1, double y1, double z1, double x2, double y2, double z2){
		for(VehicleAxisAlignedBB box : vehicle.getCurrentCollisionBoxes()){
			if(box.intersects(x1, y1, z1, x2, y2, z2)){
				return true;
			}
		}
		for(VehicleAxisAlignedBB box : vehicle.getCurrentInteractionBoxes()){
			if(box.intersects(x1, y1, z1, x2, y2, z2)){
				return true;
			}
		}
		return false;
    }
	
	@Override
	public boolean isVecInside(Vec3d vec){
		for(VehicleAxisAlignedBB box : vehicle.getCurrentCollisionBoxes()){
			if(box.isVecInside(vec)){
				return true;
			}
		}
		for(VehicleAxisAlignedBB box : vehicle.getCurrentInteractionBoxes()){
			if(box.isVecInside(vec)){
				return true;
			}
		}
		return false;
    }
	
	@Override
	@Nullable
    public RayTraceResult calculateIntercept(Vec3d vecA, Vec3d vecB){
		RayTraceResult result = null;
		for(VehicleAxisAlignedBB box : vehicle.getCurrentCollisionBoxes()){
			result = box.calculateIntercept(vecA, vecB);
			if(result != null){
				return result;
			}
		}
		for(VehicleAxisAlignedBB box : vehicle.getCurrentInteractionBoxes()){
			result = box.calculateIntercept(vecA, vecB);
			if(result != null){
				return result;
			}
		}
		return result;
	}
}
