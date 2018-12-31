package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.collision.RotatableAxisAlignedBB;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public final class PartGroundDevicePontoon extends APartGroundDevice{
	public PartGroundDevicePontoon(EntityMultipartD_Moving multipart, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(multipart, packPart, partName, dataTag);
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		return new NBTTagCompound();
	}
	
	@Override
	public float getWidth(){
		return this.pack.pontoon.width;
	}
	
	@Override
	public float getLength(){
		return this.pack.pontoon.length;
	}
	
	@Override
	public float getHeight(){
		return this.getWidth();
	}
	
	@Override
	public boolean isPartCollidingWithBlocks(){
		if(super.isPartCollidingWithBlocks()){
			return true;
    	}else{
    		RotatableAxisAlignedBB collisionBox = this.getPartBox();    		
			int minX = (int) Math.floor(collisionBox.minX);
	    	int maxX = (int) Math.floor(collisionBox.maxX + 1.0D);
	    	int minY = (int) Math.floor(collisionBox.minY);
	    	int maxY = (int) Math.floor(collisionBox.maxY + 1.0D);
	    	int minZ = (int) Math.floor(collisionBox.minZ);
	    	int maxZ = (int) Math.floor(collisionBox.maxZ + 1.0D);
	    	
	    	for(int i = minX; i < maxX; ++i){
	    		for(int j = minY; j < maxY; ++j){
	    			for(int k = minZ; k < maxZ; ++k){
	    				BlockPos checkPos = new BlockPos(i, j, k);
	    				if(multipart.worldObj.isBlockLoaded(checkPos)){
		    				if(multipart.worldObj.getBlockState(checkPos).getMaterial().isLiquid()){
		    					return true;
		    				}
	    				}
	    			}
	    		}
	    	}
	    	return false;
    	}
    }
	
	@Override
	public float getMotiveFriction(){
		return 0;
	}
	
	@Override
	public float getLateralFriction(){
		return this.pack.pontoon.lateralFriction;
	}
	
	@Override
	public boolean canBeDrivenByEngine(){
		return false;
	}
}
