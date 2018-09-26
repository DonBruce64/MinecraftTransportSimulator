package minecrafttransportsimulator.baseclasses;

import javax.annotation.Nullable;

import minecrafttransportsimulator.multipart.main.EntityMultipartC_Colliding;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**This "collective" is essentially a wrapper AABB class.  It intercepts all AABB calls
 * and re-routes them to all the AABBs in the passed-in multipart.  Essentially,
 * it's a way to have one multipart with multiple collision AABBs.
 * 
 * @author don_bruce
 */
public class MultipartAxisAlignedBBCollective extends MultipartAxisAlignedBB{
	private final EntityMultipartC_Colliding multipart;
	
	public MultipartAxisAlignedBBCollective(EntityMultipartC_Colliding multipart, float width, float height){
		super(multipart.getPositionVector(), Vec3d.ZERO, width, height, false);
		this.multipart = multipart;
	}
	
	@Override
	public MultipartAxisAlignedBBCollective expandXyz(double value){
		return this;
    }
	
	@Override
	public double calculateXOffset(AxisAlignedBB other, double offsetX){
		for(MultipartAxisAlignedBB box : multipart.getCurrentCollisionBoxes()){
			offsetX = box.calculateXOffset(other, offsetX);
		}
		for(MultipartAxisAlignedBB box : multipart.getCurrentInteractionBoxes()){
			offsetX = box.calculateXOffset(other, offsetX);
		}
		return offsetX;
    }
	
	@Override
	public double calculateYOffset(AxisAlignedBB other, double offsetY){
		for(MultipartAxisAlignedBB box : multipart.getCurrentCollisionBoxes()){
			offsetY = box.calculateYOffset(other, offsetY);
		}
		for(MultipartAxisAlignedBB box : multipart.getCurrentInteractionBoxes()){
			offsetY = box.calculateXOffset(other, offsetY);
		}
		return offsetY;
    }
	
	@Override
	public double calculateZOffset(AxisAlignedBB other, double offsetZ){
		for(MultipartAxisAlignedBB box : multipart.getCurrentCollisionBoxes()){
			offsetZ = box.calculateZOffset(other, offsetZ);
		}
		for(MultipartAxisAlignedBB box : multipart.getCurrentInteractionBoxes()){
			offsetZ = box.calculateXOffset(other, offsetZ);
		}
		return offsetZ;
    }
	
	@Override
    public boolean intersectsWith(AxisAlignedBB other){
		for(MultipartAxisAlignedBB box : multipart.getCurrentCollisionBoxes()){
			if(box.intersectsWith(other)){
				return true;
			}
		}
		for(MultipartAxisAlignedBB box : multipart.getCurrentInteractionBoxes()){
			if(box.intersectsWith(other)){
				return true;
			}
		}
		return false;
    }
	
	@Override
    public boolean intersects(double x1, double y1, double z1, double x2, double y2, double z2){
		for(MultipartAxisAlignedBB box : multipart.getCurrentCollisionBoxes()){
			if(box.intersects(x1, y1, z1, x2, y2, z2)){
				return true;
			}
		}
		for(MultipartAxisAlignedBB box : multipart.getCurrentInteractionBoxes()){
			if(box.intersects(x1, y1, z1, x2, y2, z2)){
				return true;
			}
		}
		return false;
    }
	
	@Override
	public boolean isVecInside(Vec3d vec){
		for(MultipartAxisAlignedBB box : multipart.getCurrentCollisionBoxes()){
			if(box.isVecInside(vec)){
				return true;
			}
		}
		for(MultipartAxisAlignedBB box : multipart.getCurrentInteractionBoxes()){
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
		for(MultipartAxisAlignedBB box : multipart.getCurrentCollisionBoxes()){
			result = box.calculateIntercept(vecA, vecB);
			if(result != null){
				return result;
			}
		}
		for(MultipartAxisAlignedBB box : multipart.getCurrentInteractionBoxes()){
			result = box.calculateIntercept(vecA, vecB);
			if(result != null){
				return result;
			}
		}
		return result;
	}
}
