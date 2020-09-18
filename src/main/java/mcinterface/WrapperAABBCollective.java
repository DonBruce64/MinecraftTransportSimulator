package mcinterface;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**This wrapper is essentially a collective list of BoundingBoxes.  It intercepts all AABB 
 * calls and does checks for each BoundingBox that's in the passed-in list.
 * Mostly used for entities that need complex collision mapping, because MC don't let you have more
 * than one AABB per entity, but somehow you can have more than one for something as small as a block?
 * 
 * @author don_bruce
 */
public class WrapperAABBCollective extends AxisAlignedBB{
	private final List<BoundingBox> boxes;
	BoundingBox lastBoxRayTraced;
	
	public WrapperAABBCollective(BuilderEntity entity, List<BoundingBox> boxes){
		super(	entity.posX - entity.width/2D,
				entity.posY - entity.height/2D,
				entity.posZ - entity.width/2D,
				entity.posX + entity.width/2D,
				entity.posY + entity.height/2D,
				entity.posZ + entity.width/2D
			);
		this.boxes = boxes;
	}
	
	@Override
	public WrapperAABBCollective grow(double value){
		return this;
    }
	
	@Override
	public double calculateXOffset(AxisAlignedBB box, double offset){
		for(BoundingBox testBox : boxes){
			if(box.maxY > testBox.globalCenter.y - testBox.heightRadius && box.minY < testBox.globalCenter.y + testBox.heightRadius && box.maxZ > testBox.globalCenter.z - testBox.depthRadius && box.minZ < testBox.globalCenter.z + testBox.depthRadius){
				if(offset > 0.0D){
					//Positive offset, box.maxX <= this.minX.
	                double collisionDepth = testBox.globalCenter.x - testBox.widthRadius - box.maxX;
	                if(collisionDepth >= 0 && collisionDepth < offset){
	                    offset = collisionDepth;
	                }
	            }else if(offset < 0.0D){
	            	//Negative offset, box.minX >= this.maxX.
	                double collisionDepth = testBox.globalCenter.x + testBox.widthRadius - box.minX;
	                if(collisionDepth <= 0 && collisionDepth > offset){
	                    offset = collisionDepth;
	                }
	            }
	        }
		}
		return offset;
    }
	
	@Override
	public double calculateYOffset(AxisAlignedBB box, double offset){
		for(BoundingBox testBox : boxes){
			if(box.maxX > testBox.globalCenter.x - testBox.widthRadius && box.minX < testBox.globalCenter.x + testBox.widthRadius && box.maxZ > testBox.globalCenter.z - testBox.depthRadius && box.minZ < testBox.globalCenter.z + testBox.depthRadius){
				if(offset > 0.0D){
					//Positive offset, box.maxX <= this.minX.
	                double collisionDepth = testBox.globalCenter.y - testBox.heightRadius - box.maxY;
	                if(collisionDepth >= 0 && collisionDepth < offset){
	                    offset = collisionDepth;
	                }
	            }else if(offset < 0.0D){
	            	//Negative offset, box.minX >= this.maxX.
	                double collisionDepth = testBox.globalCenter.y + testBox.heightRadius - box.minY;
	                if(collisionDepth <= 0 && collisionDepth > offset){
	                    offset = collisionDepth;
	                }
	            }
	        }
		}
		return offset;
    }
	
	@Override
	public double calculateZOffset(AxisAlignedBB box, double offset){
		for(BoundingBox testBox : boxes){
			if(box.maxX > testBox.globalCenter.x - testBox.widthRadius && box.minX < testBox.globalCenter.x + testBox.widthRadius && box.maxY > testBox.globalCenter.y - testBox.heightRadius && box.minY < testBox.globalCenter.y + testBox.heightRadius){
				if(offset > 0.0D){
					//Positive offset, box.maxX <= this.minX.
	                double collisionDepth = testBox.globalCenter.z - testBox.depthRadius - box.maxZ;
	                if(collisionDepth >= 0 && collisionDepth < offset){
	                    offset = collisionDepth;
	                }
	            }else if(offset < 0.0D){
	            	//Negative offset, box.minX >= this.maxX.
	                double collisionDepth = testBox.globalCenter.z + testBox.depthRadius - box.minZ;
	                if(collisionDepth <= 0 && collisionDepth > offset){
	                    offset = collisionDepth;
	                }
	            }
	        }
		}
		return offset;
    }
	
	@Override
    public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ){
		for(BoundingBox testBox : boxes){
				if(
					maxX > testBox.globalCenter.x - testBox.widthRadius && 
					minX < testBox.globalCenter.x + testBox.widthRadius && 
					maxY > testBox.globalCenter.y - testBox.heightRadius && 
					minY < testBox.globalCenter.y + testBox.heightRadius &&
					maxZ > testBox.globalCenter.z - testBox.depthRadius &&
					minZ < testBox.globalCenter.z + testBox.depthRadius)
				{
				return true;
			}
		}
		return false;
    }
	
	@Override
	public boolean contains(Vec3d vec){
		return this.intersects(vec.x, vec.y, vec.z, vec.x, vec.y, vec.z);
    }
	
	@Override
	@Nullable
    public RayTraceResult calculateIntercept(Vec3d vecA, Vec3d vecB){
		RayTraceResult result = null;
		lastBoxRayTraced = null;
		//Check all the bounding boxes for collision to see if we hit one of them.
		Point3d start = new Point3d(vecA.x, vecA.y, vecA.z);
		Point3d end = new Point3d(vecB.x, vecB.y, vecB.z);
		for(BoundingBox testBox : boxes){
			Point3d intersection = testBox.getIntersectionPoint(start, end);
			if(intersection != null){
				EnumFacing sideHit;
				if(intersection.x == testBox.globalCenter.x - testBox.widthRadius){
					sideHit = EnumFacing.WEST;
				}else if(intersection.x == testBox.globalCenter.x + testBox.widthRadius){
					sideHit = EnumFacing.EAST;
				}else if(intersection.y == testBox.globalCenter.y - testBox.heightRadius){
					sideHit = EnumFacing.UP;
				}else if(intersection.y == testBox.globalCenter.y + testBox.heightRadius){
					sideHit = EnumFacing.DOWN;
				}else if(intersection.z == testBox.globalCenter.z - testBox.depthRadius){
					sideHit = EnumFacing.NORTH;
				}else{
					sideHit = EnumFacing.SOUTH;
				}
				lastBoxRayTraced = testBox;
				return new RayTraceResult(new Vec3d(intersection.x, intersection.y, intersection.z), sideHit);
			}
		}
		return result;
	}   
}
