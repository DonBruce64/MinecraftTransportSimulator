package minecrafttransportsimulator.baseclasses;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

/**This class is an upgraded AABB class that allows for AABBs to put into a collective for
 * vehicle collision operations.  They contain rel and pos vectors to allow for collision
 * handling in simpler ways in the vehicle systems where they need to be rotated and
 * offset to deal with vehicle movement.  Note that pos is the actual center position of
 * this box, while rel is the relative offset center to the vehicle (identical to part offsets
 * positions when used for those).
 * 
 * @author don_bruce
 */
public class VehicleAxisAlignedBB extends AxisAlignedBB{
	public final boolean isInterior;
	public final Vec3d pos;
	public final Vec3d rel;
	public final float width;
	public final float height;
	
	public VehicleAxisAlignedBB(Vec3d pos, Vec3d rel, float width, float height, boolean isInterior){
		super(pos.xCoord - width/2F, pos.yCoord - height/2F, pos.zCoord - width/2F, pos.xCoord + width/2F, pos.yCoord + height/2F, pos.zCoord + width/2F);
		this.pos = pos;
		this.rel = rel;
		this.width = width;
		this.height = height;
		this.isInterior = isInterior;
	}
	
	public VehicleAxisAlignedBB getBoxWithOrigin(Vec3d pos){
		return new VehicleAxisAlignedBB(pos, this.rel, this.width, this.height, this.isInterior);
	}
	
	@Override
	public VehicleAxisAlignedBB offset(double xOffset, double yOffset, double zOffset){
		return getBoxWithOrigin(this.pos.addVector(xOffset, yOffset, zOffset));
	}
	
	@Override
	public VehicleAxisAlignedBB expandXyz(double value){
		return new VehicleAxisAlignedBB(this.pos, this.rel, (float) (this.width + value*2F), (float) (this.height + value*2F), this.isInterior);
    }
}
