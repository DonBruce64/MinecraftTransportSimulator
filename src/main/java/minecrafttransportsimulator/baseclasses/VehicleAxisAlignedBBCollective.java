package minecrafttransportsimulator.baseclasses;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**This "collective" is essentially a wrapper AABB class.  It intercepts all AABB calls
 * and re-routes them to all the vehicle AABBs that were passed-in.  Essentially, it's a 
 * way to  have a multi-AABB system for vehicles.
 * 
 * @author don_bruce
 */
public class VehicleAxisAlignedBBCollective extends VehicleAxisAlignedBB{
	public final List<VehicleAxisAlignedBB> boxes;
	public VehicleAxisAlignedBB lastBoxRayTraced;
	
	public VehicleAxisAlignedBBCollective(EntityVehicleF_Physics vehicle, float width, float height, List<VehicleAxisAlignedBB> boxes){
		super(vehicle.positionVector, new Point3d(0, 0, 0), width, height, false, false);
		this.boxes = boxes;
	}
	
	@Override
	public VehicleAxisAlignedBBCollective grow(double value){
		return this;
    }
	
	@Override
	public double calculateXOffset(AxisAlignedBB other, double offsetX){
		for(VehicleAxisAlignedBB box : boxes){
			offsetX = box.calculateXOffset(other, offsetX);
		}
		return offsetX;
    }
	
	@Override
	public double calculateYOffset(AxisAlignedBB other, double offsetY){
		for(VehicleAxisAlignedBB box : boxes){
			offsetY = box.calculateYOffset(other, offsetY);
		}
		return offsetY;
    }
	
	@Override
	public double calculateZOffset(AxisAlignedBB other, double offsetZ){
		for(VehicleAxisAlignedBB box : boxes){
			offsetZ = box.calculateZOffset(other, offsetZ);
		}
		return offsetZ;
    }
	
	@Override
    public boolean intersects(AxisAlignedBB other){
		for(VehicleAxisAlignedBB box : boxes){
			if(box.intersects(other)){
				return true;
			}
		}
		return false;
    }
	
	@Override
    public boolean intersects(double x1, double y1, double z1, double x2, double y2, double z2){
		for(VehicleAxisAlignedBB box : boxes){
			if(box.intersects(x1, y1, z1, x2, y2, z2)){
				return true;
			}
		}
		return false;
    }
	
	@Override
	public boolean contains(Vec3d vec){
		for(VehicleAxisAlignedBB box : boxes){
			if(box.contains(vec)){
				return true;
			}
		}
		return false;
    }
	
	@Override
	@Nullable
    public RayTraceResult calculateIntercept(Vec3d vecA, Vec3d vecB){
		RayTraceResult result = null;
		lastBoxRayTraced = null;
		for(VehicleAxisAlignedBB box : boxes){
			result = box.calculateIntercept(vecA, vecB);
			if(result != null){
				lastBoxRayTraced = box;
				return result;
			}
		}
		return result;
	}
}
