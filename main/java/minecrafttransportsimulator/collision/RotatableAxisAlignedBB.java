package minecrafttransportsimulator.collision;

import javax.annotation.Nullable;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**This class is an upgraded AABB class that contains a {@link RotatableBB}.
 * This allows it to be sensitive to rotation, while still being compatible with Minecraft's
 * standard AABB collision system.  Everything in here serves as a wrapper for collision,
 * so if you want to actually <i>modify</i> the properties of this AABB then you will need
 * to modify the RBB inside it.  This is done to allow modification of the RBB in mods like
 * MTS without making a whole new AABB, and to allow the RBB to not be re-generated when not
 * required when this class is re-generated.  Note that this system is geared towards letting
 * MC do collisions with a static rotatable body, NOT for use in performing collision checks with
 * said body.  Because of this, most of the methods are not optimized for predictive collisions.
 * 
 * @author don_bruce
 */
public class RotatableAxisAlignedBB extends AxisAlignedBB{
	private final RotatableBB rotatableBox;
	
	public RotatableAxisAlignedBB(double xPos, double yPos, double zPos, float rotationX, float rotationY, float rotationZ, float width, float height, float depth){
		super(0, 0, 0, 1, 1, 1);
		this.rotatableBox = new RotatableBB(xPos, yPos, zPos, rotationX, rotationY, rotationZ, width, height, depth);
	}
	
	private RotatableAxisAlignedBB(RotatableBB rotatableBox){
		super(0, 0, 0, 1, 1, 1);
		this.rotatableBox = rotatableBox;
	}
	
	@Override
	public RotatableAxisAlignedBB expandXyz(double value){
		return new RotatableAxisAlignedBB(rotatableBox.getExpandedBox((float) value, (float) value, (float) value));
    }
	
	@Override
	public AxisAlignedBB offset(double x, double y, double z){
		return new RotatableAxisAlignedBB(rotatableBox.getOffsetBox((float) x, (float) y, (float) z));
    }
	
	@Override
    public AxisAlignedBB offset(BlockPos pos){
		return this.offset(pos.getX(), pos.getY(), pos.getZ());
    }
	
	@Override
	public double calculateXOffset(AxisAlignedBB other, double offsetX){
		return rotatableBox.getPredictiveCollision(other, (byte) 1, offsetX);
	}
	
	@Override
	public double calculateYOffset(AxisAlignedBB other, double offsetY){
		return rotatableBox.getPredictiveCollision(other, (byte) 2, offsetY);
    }
	
	@Override
	public double calculateZOffset(AxisAlignedBB other, double offsetZ){
		return rotatableBox.getPredictiveCollision(other, (byte) 3, offsetZ);
    }
	
	@Override
    public boolean intersectsWith(AxisAlignedBB other){
		return rotatableBox.doesBoxIntersect(other);
    }
	
	@Override
    public boolean intersects(double x1, double y1, double z1, double x2, double y2, double z2){
		return this.intersectsWith(new AxisAlignedBB(x1, y1, z1, x2, y2, z2));
    }
	
	@Override
	public boolean isVecInside(Vec3d vec){
		return rotatableBox.isPointInsideBox(vec.xCoord, vec.yCoord, vec.zCoord);
    }
	
	/**
	 * Best I can tell, this method is used to detect if this AABB is hit.  MC usually passes in the
	 * the vector start position as the first parameter, and the action vector as the second.
	 * So for players the first parameter is their eyes, and the second is their hit vector,
	 * while for projectiles the first parameter is their position, and the second is their velocity.
	 * In general, MC seems to expect a hit/miss result, with the side being hit appearing to only be
	 * used for blocks as entities don't really care which side you hit them from.
	 * We should be good just checking if the vector is inside this RBB at discrete points...
	 * 
	 */
	@Override
	@Nullable
    public RayTraceResult calculateIntercept(Vec3d vecA, Vec3d vecB){
		double lengthTrace = vecB.lengthVector();
		for(double d=0; d<=lengthTrace; d = (d + rotatableBox.smallestDistance > lengthTrace ? lengthTrace : rotatableBox.smallestDistance/lengthTrace)){
			if(rotatableBox.isPointInsideBox(vecA.xCoord + vecB.xCoord*d, vecA.yCoord + vecB.yCoord*d, vecA.zCoord + vecB.zCoord*d)){
				return new RayTraceResult(new Vec3d(vecA.xCoord + vecB.xCoord*d, vecA.yCoord + vecB.yCoord*d, vecA.zCoord + vecB.zCoord*d), EnumFacing.DOWN);
			}
		}
		return null;
	}
}
