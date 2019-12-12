package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public abstract class APartGroundEffector extends APart{
	protected final BlockPos[] lastBlocksModified;
	protected final BlockPos[] affectedBlocks;
	
	public APartGroundEffector(EntityVehicleE_Powered vehicle, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(vehicle, packPart, partName, dataTag);
		lastBlocksModified = new BlockPos[pack.effector.blocksWide];
		affectedBlocks = new BlockPos[pack.effector.blocksWide];
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		int startingIndex = -pack.effector.blocksWide/2;
		for(int i=0; i<pack.effector.blocksWide; ++i){
			int xOffset = startingIndex + i;
			if(effectIsBelowPart()){
				affectedBlocks[i] = new BlockPos(RotationSystem.getRotatedPoint(new Vec3d(xOffset, 0, 0), vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll).add(partPos)).down();
			}else{
				affectedBlocks[i] = new BlockPos(RotationSystem.getRotatedPoint(new Vec3d(xOffset, 0, 0), vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll).add(partPos));
			}
		}
		
		for(byte i=0; i<affectedBlocks.length; ++i){
			if(!affectedBlocks[i].equals(lastBlocksModified[i])){
				performEffectsAt(affectedBlocks[i]);
				lastBlocksModified[i] = affectedBlocks[i];
			}
		}
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		return new NBTTagCompound(); 
	}
	
	@Override
	public float getWidth(){
		return 1.0F;
	}

	@Override
	public float getHeight(){
		return 1.0F;
	}
	
	protected abstract void performEffectsAt(BlockPos pos);
	
	protected abstract boolean effectIsBelowPart();
}
