package mcinterface;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**This wrapper is essentially a collective list of AABB classes.  It intercepts all AABB 
 * calls and does checks for each AABB that's in the passed-in list.
 * Mostly used for entities that need complex collision mapping, because MC don't let you have more
 * than one AABB per entity, but somehow you can have more than one for something as small as a block?
 * 
 * @author don_bruce
 */
public class WrapperAABBCollective extends AxisAlignedBB{
	final List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
	AxisAlignedBB lastBoxRayTraced;
	
	public WrapperAABBCollective(AEntityBase entity, List<BoundingBox> boxes){
		super(
			entity.position.x - entity.builder.width/2,
			entity.position.y - entity.builder.height/2,
			entity.position.z - entity.builder.width/2,
			entity.position.x + entity.builder.width/2,
			entity.position.y + entity.builder.height/2,
			entity.position.z + entity.builder.width/2
		);
		for(BoundingBox box : boxes){
			this.boxes.add(new AxisAlignedBB(
				box.globalCenter.x - box.widthRadius,
				box.globalCenter.y - box.heightRadius,
				box.globalCenter.z - box.depthRadius,
				box.globalCenter.x + box.widthRadius,
				box.globalCenter.y + box.heightRadius,
				box.globalCenter.z + box.depthRadius
			));
		}
	}
	
	@Override
	public WrapperAABBCollective grow(double value){
		return this;
    }
	
	@Override
	public double calculateXOffset(AxisAlignedBB other, double offsetX){
		for(AxisAlignedBB box : boxes){
			offsetX = box.calculateXOffset(other, offsetX);
		}
		return offsetX;
    }
	
	@Override
	public double calculateYOffset(AxisAlignedBB other, double offsetY){
		for(AxisAlignedBB box : boxes){
			offsetY = box.calculateYOffset(other, offsetY);
		}
		return offsetY;
    }
	
	@Override
	public double calculateZOffset(AxisAlignedBB other, double offsetZ){
		for(AxisAlignedBB box : boxes){
			offsetZ = box.calculateZOffset(other, offsetZ);
		}
		return offsetZ;
    }
	
	@Override
    public boolean intersects(AxisAlignedBB other){
		for(AxisAlignedBB box : boxes){
			if(box.intersects(other)){
				return true;
			}
		}
		return false;
    }
	
	@Override
    public boolean intersects(double x1, double y1, double z1, double x2, double y2, double z2){
		for(AxisAlignedBB box : boxes){
			if(box.intersects(x1, y1, z1, x2, y2, z2)){
				return true;
			}
		}
		return false;
    }
	
	@Override
	public boolean contains(Vec3d vec){
		for(AxisAlignedBB box : boxes){
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
		for(AxisAlignedBB box : boxes){
			result = box.calculateIntercept(vecA, vecB);
			if(result != null){
				lastBoxRayTraced = box;
				return result;
			}
		}
		return result;
	}
}
