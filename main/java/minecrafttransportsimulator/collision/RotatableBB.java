package minecrafttransportsimulator.collision;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**This class contains a custom bounding box that is specifically designed for MTS vehicle collision.
 * Unlike the AABB Minecraft uses, this box can rotate.  It also allows for mutation of the center position of the
 * box without creating a new box object, which allows for it to be translated quickly and efficiently.
 * As these boxes are able to rotate, a depth parameter has been added to allow for rectangular boxes.
 * Additionally, it contains an AABB that's cached to provide a quick pre-check for Minecraft operations
 * that don't have precise collision pre-check detection built-in.
 * 
 * @author don_bruce
 */
public class RotatableBB{
	private double xPos;
	private double yPos;
	private double zPos;
	
	private float rotationX;
	private float rotationY;
	private float rotationZ;
	
	private final float width;
	private final float height;
	private final float depth;
	
	//Points are defined for this system as follows:
	//The first 4 are the top of the RBB, with the 0 index being the top-left and the 3 index being top-right.
	//The last 4 are the bottom of the RBB, with the 4 index being the bottom-left and the 7 index being bottom-right.
	private final double[] xCoords = new double[8];
	private final double[] yCoords = new double[8];
	private final double[] zCoords = new double[8];
	
	//These aren't actual points.  They are the index for the point lists above.
	private byte minX;
	private byte maxX;
	private byte minY;
	private byte maxY;
	private byte minZ;
	private byte maxZ;
	
	private Vec3d collisionXAxisNormal;
	private Vec3d collisionYAxisNormal;
	private Vec3d collisionZAxisNormal;
	
	private double minXProjection;
	private double maxXProjection;
	private double minYProjection;
	private double maxYProjection;
	private double minZProjection;
	private double maxZProjection;
	
	private AxisAlignedBB encompassingAABB;
	
	public final float smallestDistance;
	
	public RotatableBB(double xPos, double yPos, double zPos, float rotationX, float rotationY, float rotationZ, float width, float height, float depth){
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.smallestDistance = Math.min(Math.min(width, height), depth);
		recalculatePoints(xPos, yPos, zPos, rotationX, rotationY, rotationZ);
	}
	
	/**
	 * Re-calculates the points that make up this rotatableBB and updates the cached AABB.  This is a VERY CPU-intensive operation 
	 * and should only be performed when absolutely required!  Ideally once every tick, once the RBB position is set and needs to be
	 * ready to handle Minecraft collision.  The static AABB is useful in doing pre-checks when dealing MC's lousy chunk-based collision 
	 * checking, as we save on CPU time more for not checking a bunch of rotated boxes than we use making a new AABB and having the GC 
	 * eat up the old one.  Note that this only needs to be called if the rotation of the box has updated.  If the box has only
	 * translated then updateStaticAABB should be called instead which provides the same effect without costing CPU time for
	 * the rotation calculations.
	 */
	public void recalculatePoints(double xPos, double yPos, double zPos, float rotationX, float rotationY, float rotationZ){
		//First set the position and rotation.
		this.xPos = xPos;
		this.yPos = yPos;
		this.zPos = zPos;
		this.rotationX = rotationX;
		this.rotationY = rotationY;
		this.rotationZ = rotationZ;
		
		//Next get the 8 points that make up this RBB.
		Vec3d[] points = new Vec3d[8];
		points[0] = RotationSystem.getRotatedPoint(new Vec3d(-width/2F, height/2F, depth/2F), rotationX, rotationY, rotationZ).addVector(xPos, yPos, zPos);
		points[1] = RotationSystem.getRotatedPoint(new Vec3d(width/2F, height/2F, depth/2F), rotationX, rotationY, rotationZ).addVector(xPos, yPos, zPos);
		points[2] = RotationSystem.getRotatedPoint(new Vec3d(-width/2F, height/2F, -depth/2F), rotationX, rotationY, rotationZ).addVector(xPos, yPos, zPos);
		points[3] = RotationSystem.getRotatedPoint(new Vec3d(width/2F, height/2F, -depth/2F), rotationX, rotationY, rotationZ).addVector(xPos, yPos, zPos);
		points[4] = RotationSystem.getRotatedPoint(new Vec3d(-width/2F, -height/2F, depth/2F), rotationX, rotationY, rotationZ).addVector(xPos, yPos, zPos);
		points[5] = RotationSystem.getRotatedPoint(new Vec3d(width/2F, -height/2F, depth/2F), rotationX, rotationY, rotationZ).addVector(xPos, yPos, zPos);
		points[6] = RotationSystem.getRotatedPoint(new Vec3d(-width/2F, -height/2F, -depth/2F), rotationX, rotationY, rotationZ).addVector(xPos, yPos, zPos);
		points[7] = RotationSystem.getRotatedPoint(new Vec3d(width/2F, -height/2F, -depth/2F), rotationX, rotationY, rotationZ).addVector(xPos, yPos, zPos);
		for(byte i=0; i<8; ++i){
			this.xCoords[i] = points[i].xCoord;
			this.yCoords[i] = points[i].yCoord;
			this.zCoords[i] = points[i].zCoord;
		}
		
		//Now find the min and max points for each axis.
		minX = 0;
		maxX = 0;
		minY = 0;
		maxY = 0;
		minZ = 0;
		maxZ = 0;
		for(byte i=1; i<8; ++i){
			if(xCoords[i] < xCoords[minX]){
				minX = i;
			}
			if(xCoords[i] > xCoords[maxX]){
				maxX = i;
			}
			if(yCoords[i] < yCoords[minY]){
				minY = i;
			}
			if(yCoords[i] > yCoords[maxY]){
				maxY = i;
			}
			if(zCoords[i] < zCoords[minZ]){
				minZ = i;
			}
			if(zCoords[i] > zCoords[maxZ]){
				maxZ = i;
			}
		}
		
		//Now calculate the normal axis for collisions.
		collisionXAxisNormal = points[1].subtract(points[0]).normalize();
		collisionYAxisNormal = points[3].subtract(points[4]).normalize();
		collisionZAxisNormal = points[1].subtract(points[2]).normalize();
		
		//Finally, get the projection values for the axis.
		minXProjection = collisionXAxisNormal.xCoord*xCoords[minX] + collisionXAxisNormal.yCoord*xCoords[minX] + collisionXAxisNormal.zCoord*xCoords[minX];
		maxXProjection = collisionXAxisNormal.xCoord*xCoords[maxX] + collisionXAxisNormal.yCoord*xCoords[maxX] + collisionXAxisNormal.zCoord*xCoords[maxX];
		minYProjection = collisionYAxisNormal.xCoord*yCoords[minY] + collisionYAxisNormal.yCoord*yCoords[minY] + collisionYAxisNormal.zCoord*yCoords[minY];
		maxYProjection = collisionYAxisNormal.xCoord*yCoords[maxY] + collisionYAxisNormal.yCoord*yCoords[maxY] + collisionYAxisNormal.zCoord*yCoords[maxY];
		minZProjection = collisionZAxisNormal.xCoord*zCoords[minZ] + collisionZAxisNormal.yCoord*zCoords[minZ] + collisionZAxisNormal.zCoord*zCoords[minZ];
		maxZProjection = collisionZAxisNormal.xCoord*zCoords[maxZ] + collisionZAxisNormal.yCoord*zCoords[maxZ] + collisionZAxisNormal.zCoord*zCoords[maxZ];
		updateStaticAABB();
	}
		
	/**
	 * Updates the static AABB representation of this box.  Do this once we know this box is ready for interaction
	 * with Minecraft systems.  This should NOT be used for custom collision detection, as it takes a bit of RAM.
	 * The AABB, however, is useful in doing pre-checks when dealing MC's lousy chunk-based collision checking,
	 * as we save on CPU time more for not checking a bunch of rotated boxes than we use making a new AABB
	 * and having the GC eat up the old one.
	 */
	private void updateStaticAABB(){
		//0.70710678118F is sin/cos 45.  We angle the box here to get the extreme points at a 45 degree rotation.
		float maxX = 0.70710678118F*width/2F - 0.70710678118F*height/2F;
		float maxY = 0.70710678118F*width/2F + 0.70710678118F*height/2F;
		encompassingAABB = new AxisAlignedBB(this.xPos - maxX, this.yPos - maxY, this.zPos - maxX, this.xPos + maxX, this.yPos + maxY, this.zPos + maxX);
	}

	/**
	 * Returns the distance between this RBB and the passed-in AABB in the axis specified.  This is a precise check that should be used for 
	 * collision, therefore it is more CPU-intensive than normal AABB collision checks.  Axis is a byte that determines on which axis the 
	 * collision depth is returned, while offset is used in the same way as the calculate(X,Y,Z)Offset function that AABBs use.  All of 
	 * this allows for a single method for RBB collision checks.  Note that this function will NOT behave well if the boxes already collide. 
	 * Use doesBoxIntersect for that.  It is up to the user of this box to assure that at no point does an entity get inside it and try to
	 * do collision checks as the behavior when this happens is un-defined!
	 */
	public double getPredictiveCollision(AxisAlignedBB box, byte axis, double offset){
		if(!box.intersectsWith(this.encompassingAABB)){
			return offset;
		}else if(box.intersectsWith(this.encompassingAABB)){
			return offset;
		}

		//Now use the single-axis theorem here to calculate if this box collides.
		//First calculate for the AABB that's being passed in.
		//This is easier to do as we already know the normal planes and don't
		//need to do vector projection as it's just the difference in points.
		//We need to check the axis that are not part of the passed-in axis first.
		//Then when we know we collide in those axis, we can get our resultant axis depth.
		//Note that this gets tricky when dealing with the RBB axis as they are transformed.
		//We will need to check ALL of those axis as they all have components of the main ones.
		
		//First calculate if we intersect the plane requested by the axis.
		//This will instantly tell us if we need to continue.
		if(axis == 1){
			if(!(this.yCoords[minY] < box.maxY && this.yCoords[maxY] > box.minY) || !(this.zCoords[minZ] < box.maxZ && this.zCoords[maxZ] > box.minZ)){
				return offset;
			}
		}else if(axis == 2){
			if(!(this.xCoords[minX] < box.maxX && this.xCoords[maxX] > box.minX) || !(this.zCoords[minZ] < box.maxZ && this.zCoords[maxZ] > box.minZ)){
				return offset;
			}
		}else if(axis == 3){
			if(!(this.xCoords[minX] < box.maxX && this.xCoords[maxX] > box.minX) || !(this.yCoords[minY] < box.maxY && this.yCoords[maxY] > box.minY)){
				return offset;
			}
		}
		
		//Now we know we intersect in the desired plane, it is time to calculate the offset for the axis.
		//First get the results of the AABB axis.
		if(axis == 1){
			if(offset > 0 && box.maxX <= this.xCoords[minX]){
                double minMaxDistance = this.xCoords[minX] - box.maxX;
                if(minMaxDistance < offset){
                    offset = minMaxDistance;
                }
            }else if(offset < 0 && box.minX >= this.xCoords[maxX]){
                double minMaxDistance = this.maxX - box.minX;
                if (minMaxDistance > offset){
                    offset = minMaxDistance;
                }
            }
		}else if(axis == 2){
			if(offset > 0 && box.maxY <= this.yCoords[minY]){
                double minMaxDistance = this.yCoords[minY] - box.maxY;
                if(minMaxDistance < offset){
                    offset = minMaxDistance;
                }
            }else if(offset < 0 && box.minY >= this.yCoords[maxY]){
                double minMaxDistance = this.maxY - box.minY;
                if (minMaxDistance > offset){
                    offset = minMaxDistance;
                }
            }
		}else if(axis == 3){
			if(offset > 0 && box.maxZ <= this.zCoords[minZ]){
                double minMaxDistance = this.zCoords[minZ] - box.maxZ;
                if(minMaxDistance < offset){
                    offset = minMaxDistance;
                }
            }else if(offset < 0 && box.minZ >= this.zCoords[maxZ]){
                double minMaxDistance = this.maxZ - box.minZ;
                if (minMaxDistance > offset){
                    offset = minMaxDistance;
                }
            }
		}
		
		//Now get the deltas for the RBB axis projections.
		///We only care about the component in the axis specified, so we can ignore some of the projection values.
		//If our offset is positive, it means AABB max < RBB min, and check RBB min - AABB max.
		//If our offset is negative, it means AABB min > RBB max, and check RBB max - AABB min. 
		if(axis == 1){
			if(offset > 0){
				double maxproj = collisionXAxisNormal.xCoord*box.maxX + collisionXAxisNormal.yCoord*box.maxX + collisionXAxisNormal.zCoord*box.maxX;
				if(maxproj <= this.minXProjection){
	                double minMaxDistance = (this.minXProjection - maxproj)*collisionXAxisNormal.xCoord;
	                if(minMaxDistance < offset){
	                    offset = minMaxDistance;
	                }
				}
	        }else{
	        	double minproj = collisionXAxisNormal.xCoord*box.minX + collisionXAxisNormal.yCoord*box.minX + collisionXAxisNormal.zCoord*box.minX;
	        	if(minproj >= this.maxXProjection){
	        		double minMaxDistance = (this.maxXProjection - minproj)*collisionXAxisNormal.xCoord;
	                if (minMaxDistance > offset){
	                    offset = minMaxDistance;
	                }
	        	}
	        }
		}else if(axis == 2){
			if(offset > 0){
				double maxproj = collisionYAxisNormal.xCoord*box.maxY + collisionYAxisNormal.yCoord*box.maxY + collisionYAxisNormal.zCoord*box.maxY;
				if(maxproj <= this.minYProjection){
	                double minMaxDistance = (this.minYProjection - maxproj)*collisionYAxisNormal.xCoord;
	                if(minMaxDistance < offset){
	                    offset = minMaxDistance;
	                }
				}
	        }else{
	        	double minproj = collisionYAxisNormal.xCoord*box.minY + collisionYAxisNormal.yCoord*box.minY + collisionYAxisNormal.zCoord*box.minY;
	        	if(minproj >= this.maxYProjection){
	        		double minMaxDistance = (this.maxYProjection - minproj)*collisionYAxisNormal.xCoord;
	                if (minMaxDistance > offset){
	                    offset = minMaxDistance;
	                }
	        	}
	        }
		}else if(axis == 3){
			if(offset > 0){
				double maxproj = collisionZAxisNormal.xCoord*box.maxZ + collisionZAxisNormal.yCoord*box.maxZ + collisionZAxisNormal.zCoord*box.maxZ;
				if(maxproj <= this.minZProjection){
	                double minMaxDistance = (this.minZProjection - maxproj)*collisionZAxisNormal.xCoord;
	                if(minMaxDistance < offset){
	                    offset = minMaxDistance;
	                }
				}
	        }else{
	        	double minproj = collisionZAxisNormal.xCoord*box.minZ + collisionZAxisNormal.yCoord*box.minZ + collisionZAxisNormal.zCoord*box.minZ;
	        	if(minproj >= this.maxZProjection){
	        		double minMaxDistance = (this.maxZProjection - minproj)*collisionZAxisNormal.xCoord;
	                if (minMaxDistance > offset){
	                    offset = minMaxDistance;
	                }
	        	}
	        }
		}	
		//We are done doing collisions.  Return the offset.
		return offset;
	}
	
	
	/**
	 * Returns true if this RBB intersects the passed-in AABB.  This is a precise check that should be used for collision, therefore it is
	 * more CPU-intensive than normal AABB collision checks.  Note that this method will not tell you how FAR the box has collided.
	 * For information on that use getPredictiveCollision prior to moving a RBB in the world, as once you collide the RBB can't return
	 * collision information as it doesn't know on what side it collided to get stuck inside the AABB.
	 */
	public boolean doesBoxIntersect(AxisAlignedBB box){
		//First do a pre-check using the encompassing AABB to make sure we could even collide.
		if(!box.intersectsWith(this.encompassingAABB)){
			return false;
		}
		
		//Now use the single-axis theorem here to calculate if this box collides.
		//First calculate for the AABB that's being passed in.
		//This is easier to do as we already know the normal planes and don't
		//need to do vector projection as it's just the difference in points.
		//To simplify things, we check in the X axis first, and then the Y and Z axis.
		//First get the projection of the RBB points on the ABB X-axis.
		//The Y and Z components are obviously 0 here so don't bother calculating them.
		//And since we are using normalized vectors, we know the X-component will be 1.
		//Therefore we can just use the X-coord points of this RBB as-is.
		//If none of the points are between the AABB X-coords then we know we missed it.
		//If one of the points are between the AABB, then we move on to the Y-axis.
		
		//Check to see if either the min or max points for this box are between the min and max points for the AABB.
		if(this.xCoords[minX] < box.maxX && this.xCoords[maxX] > box.minX){
			//We know the X-coords overlap.  Check the Y-coords.
			if(this.yCoords[minY] < box.maxY && this.yCoords[maxY] > box.minY){
				//We know the Y-coords overlap.  Check the Z-coords.
				if(this.zCoords[minZ] < box.maxZ && this.zCoords[maxZ] > box.minZ){
					//All coords overlap on the AABB axis.  Now we need to check from the RBB axis.
					//Use projection and the axis to align the RBB and AABB coords to the RBB axis.
					//Then check to see if the projections overlap.
					double minproj = collisionXAxisNormal.xCoord*box.minX + collisionXAxisNormal.yCoord*box.minX + collisionXAxisNormal.zCoord*box.minX;
					double maxproj = collisionXAxisNormal.xCoord*box.maxX + collisionXAxisNormal.yCoord*box.maxX + collisionXAxisNormal.zCoord*box.maxX;
					if(this.minXProjection < maxproj && this.maxXProjection > minproj){
						//We do overlap in the projected X-axis.  Check Y.
						minproj = collisionYAxisNormal.xCoord*box.minY + collisionYAxisNormal.yCoord*box.minY + collisionYAxisNormal.zCoord*box.minY;
						maxproj = collisionYAxisNormal.xCoord*box.maxY + collisionYAxisNormal.yCoord*box.maxY + collisionYAxisNormal.zCoord*box.maxY;
						if(this.minYProjection < maxproj && this.maxYProjection > minproj){
							//We do overlap in the projected Y-axis.  Check Z.
							minproj = collisionZAxisNormal.xCoord*box.minZ + collisionZAxisNormal.yCoord*box.minZ + collisionZAxisNormal.zCoord*box.minZ;
							maxproj = collisionZAxisNormal.xCoord*box.maxZ + collisionZAxisNormal.yCoord*box.maxZ + collisionZAxisNormal.zCoord*box.maxZ;
							if(this.minZProjection < maxproj && this.maxZProjection > minproj){
								//We FINALLY know that we collide in all axis.
								//At this point we know we must collide, and can return true.
								return true;
							}
						}
					}
				}
			}
		}
		//We did not collide on one of our axis, therefore we are not colliding.
		return false;
	}
	
	
	/**
	 * Checks to see if the point is inside this box.  This is not very CPU-intensive as we are only checking point 
	 * bounds and not doing any vector projection.  Most calls for collision checks will be between 
	 * this box and an AABB,  but there are times that the point itself needs to be checked, such as when we are 
	 * comparing a Vec3d.  This is commonly done for raytracing by MC to see if the box was clicked.
	 */
	public boolean isPointInsideBox(double x, double y, double z){
		if(this.xCoords[minX] < x && this.xCoords[maxX] > x){
			if(this.yCoords[minY] < y && this.yCoords[maxY] > y){
				if(this.zCoords[minZ] < z && this.zCoords[maxZ] > z){
					return true;
				}
			}
		}
		return false;
	}
	
	public RotatableBB getExpandedBox(float widthExpansion, float heightExpansion, float depthExpansion){
		return new RotatableBB(this.xPos, this.yPos, this.zPos, this.rotationX, this.rotationY, this.rotationZ, this.width + widthExpansion, this.height + heightExpansion, this.depth + depthExpansion);
	}
	
	public RotatableBB getOffsetBox(double xOffset, double yOffset, double zOffset){
		return new RotatableBB(this.xPos + xOffset, this.yPos + yOffset, this.zPos + zOffset, this.rotationX, this.rotationY, this.rotationZ, this.width, this.height, this.depth);
	}
	
	@SideOnly(Side.CLIENT)
	public void renderBox(){
		GL11.glBegin(GL11.GL_LINES);
		//Top segments.
		GL11.glVertex3d(xCoords[0], yCoords[0], zCoords[0]);
		GL11.glVertex3d(xCoords[1], yCoords[1], zCoords[1]);
		GL11.glVertex3d(xCoords[1], yCoords[1], zCoords[1]);
		GL11.glVertex3d(xCoords[3], yCoords[3], zCoords[3]);
		GL11.glVertex3d(xCoords[3], yCoords[3], zCoords[3]);
		GL11.glVertex3d(xCoords[2], yCoords[2], zCoords[2]);
		GL11.glVertex3d(xCoords[2], yCoords[2], zCoords[2]);
		GL11.glVertex3d(xCoords[0], yCoords[0], zCoords[0]);

		//Bottom segments.
		GL11.glVertex3d(xCoords[4], yCoords[4], zCoords[4]);
		GL11.glVertex3d(xCoords[5], yCoords[5], zCoords[5]);
		GL11.glVertex3d(xCoords[5], yCoords[5], zCoords[5]);
		GL11.glVertex3d(xCoords[7], yCoords[7], zCoords[7]);
		GL11.glVertex3d(xCoords[7], yCoords[7], zCoords[7]);
		GL11.glVertex3d(xCoords[6], yCoords[6], zCoords[6]);
		GL11.glVertex3d(xCoords[6], yCoords[6], zCoords[6]);
		GL11.glVertex3d(xCoords[4], yCoords[4], zCoords[4]);
		
		//Vertical segments.
		GL11.glVertex3d(xCoords[0], yCoords[0], zCoords[0]);
		GL11.glVertex3d(xCoords[4], yCoords[4], zCoords[4]);
		GL11.glVertex3d(xCoords[1], yCoords[1], zCoords[1]);
		GL11.glVertex3d(xCoords[5], yCoords[5], zCoords[5]);
		GL11.glVertex3d(xCoords[2], yCoords[2], zCoords[2]);
		GL11.glVertex3d(xCoords[6], yCoords[6], zCoords[6]);
		GL11.glVertex3d(xCoords[3], yCoords[3], zCoords[3]);
		GL11.glVertex3d(xCoords[7], yCoords[7], zCoords[7]);		
		GL11.glEnd();
	}
}
