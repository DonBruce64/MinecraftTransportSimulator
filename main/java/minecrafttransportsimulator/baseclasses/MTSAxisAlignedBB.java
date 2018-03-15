package minecrafttransportsimulator.baseclasses;

import javax.annotation.Nullable;

import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class MTSAxisAlignedBB extends AxisAlignedBB{
	public final float posX;
	public final float posY;
	public final float posZ;
	public final float relX;
	public final float relY;
	public final float relZ;
	public final float width;
	public final float height;
	
	public MTSAxisAlignedBB(double posX, double posY, double posZ, double relX, double relY, double relZ, float width, float height){
		super(posX - width/2F, posY - height/2F, posZ - width/2F, posX + width/2F, posY + height/2F, posZ + width/2F);
		this.posX = (float) posX;
		this.posY = (float) posY;
		this.posZ = (float) posZ;
		this.relX = (float) relX;
		this.relY = (float) relY;
		this.relZ = (float) relZ;
		this.width = width;
		this.height = height;
	}
	
	public MTSAxisAlignedBB getBoxWithOrigin(double posX, double posY, double posZ){
		return new MTSAxisAlignedBB(posX, posY, posZ, this.relX, this.relY, this.relZ, this.width, this.height);
	}
	
	@Override
	public MTSAxisAlignedBB offset(double xOffset, double yOffset, double zOffset){
		return getBoxWithOrigin(this.posX + xOffset, this.posY + yOffset, this.posZ + zOffset);
	}
	
	@Override
	public MTSAxisAlignedBB expandXyz(double value){
		return new MTSAxisAlignedBB(this.posX, this.posY, this.posZ, this.relX, this.relY, this.relZ, (float) (this.width + value*2F), (float) (this.height + value*2F));
    }

	
	
	
	public static class MTSAxisAlignedBBCollective extends MTSAxisAlignedBB{
		private final EntityMultipartMoving mover;
		
		public MTSAxisAlignedBBCollective(EntityMultipartMoving mover, float width, float height){
			super(mover.posX, mover.posY, mover.posZ, 0, 0, 0, width, height);
			this.mover = mover;
		}
		
		@Override
		public MTSAxisAlignedBB expandXyz(double value){
			return this;
	    }
		
		@Override
		public double calculateXOffset(AxisAlignedBB other, double offsetX){
			for(MTSAxisAlignedBB box : mover.getCurrentCollisionBoxes()){
				offsetX = box.calculateXOffset(other, offsetX);
			}
			return offsetX;
	    }
		
		@Override
		public double calculateYOffset(AxisAlignedBB other, double offsetY){
			for(MTSAxisAlignedBB box : mover.getCurrentCollisionBoxes()){
				offsetY = box.calculateYOffset(other, offsetY);
			}
			return offsetY;
	    }
		
		@Override
		public double calculateZOffset(AxisAlignedBB other, double offsetZ){
			for(MTSAxisAlignedBB box : mover.getCurrentCollisionBoxes()){
				offsetZ = box.calculateZOffset(other, offsetZ);
			}
			return offsetZ;
	    }
		
		@Override
	    public boolean intersects(double x1, double y1, double z1, double x2, double y2, double z2){
			for(MTSAxisAlignedBB box : mover.getCurrentCollisionBoxes()){
				if(box.intersects(x1, y1, z1, x2, y2, z2)){
					return true;
				}
			}
			return false;
	    }
		
		@Override
		public boolean isVecInside(Vec3d vec){
			for(MTSAxisAlignedBB box : mover.getCurrentCollisionBoxes()){
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
			for(MTSAxisAlignedBB box : mover.getCurrentCollisionBoxes()){
				result = box.calculateIntercept(vecA, vecB);
				if(result != null){
					return result;
				}
			}
			return result;
		}
	}
}
