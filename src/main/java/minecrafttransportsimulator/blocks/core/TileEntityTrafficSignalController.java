package minecrafttransportsimulator.blocks.core;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Car;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

public class TileEntityTrafficSignalController extends TileEntityBase implements ITickable{
	public boolean orientedOnX;
	public boolean triggerMode;
	public int greenMainTime;
	public int greenCrossTime;
	public int yellowTime;
	public int allRedTime;
	public final List<BlockPos> trafficSignalLocations = new ArrayList<BlockPos>();
	
	public byte operationIndex;
	public long timeOperationStarted;	
	
	@Override
	public void update(){
		long worldTime = world.getTotalWorldTime();
		if(operationIndex == 0){
			//First step, main light turns green, cross light stays red.
			//If we are a signal-controlled system, we stay here until we get a signal.
			//If we are a timed system, we immediately move to state 1 as our
			//green time is an extra state to enable looping.
			if(triggerMode){
				//Only check every two seconds to prevent lag.
				if(worldTime%40 == 0){
					//Get a bounding box for all lights in the controller system.
					int minX = Integer.MAX_VALUE;
					int maxX = Integer.MIN_VALUE;
					int minZ = Integer.MAX_VALUE;
					int maxZ = Integer.MIN_VALUE;
					for(BlockPos controllerSignalPos : trafficSignalLocations){
						minX = Math.min(minX, controllerSignalPos.getX());
						maxX = Math.max(maxX, controllerSignalPos.getX());
						minZ = Math.min(minX, controllerSignalPos.getZ());
						maxZ = Math.max(maxX, controllerSignalPos.getZ());
					}
					
					//Take 10 off to expand the detection boxes for the axis.
					if(orientedOnX){
						minZ -= 10;
						maxZ += 10;
					}else{
						minX -= 10;
						maxX += 10;
					}
					
					//Now we have min-max, check for any vehicles in the area.
					//We need to check along the non-primary axis.
					for(Entity entity : world.loadedEntityList){
						if(entity instanceof EntityVehicleG_Car){
							if(orientedOnX){
								if((entity.posZ > minZ && entity.posZ < minZ + (maxZ - minZ)/2F) || (entity.posZ < maxZ && entity.posZ > maxZ - (maxZ - minZ)/2F)){
									if(entity.posX > minX && entity.posX < maxX){
										timeOperationStarted = worldTime;
										operationIndex = 1;
									}
								}
							}else{
								if((entity.posX > minX && entity.posX < minX + (maxX - minX)/2F) || (entity.posX < maxX && entity.posX > maxX - (maxX - minX)/2F)){
									if(entity.posZ > minZ && entity.posZ < maxZ){
										timeOperationStarted = worldTime;
										operationIndex = 1;
									}
								}
							}
						}
					}
				}
			}else{
				timeOperationStarted = worldTime;
				operationIndex = 1;
			}
		}else if(operationIndex == 1){
			//Second step, main light turns yellow, cross light stays red.
			if(timeOperationStarted + yellowTime <= worldTime){
				timeOperationStarted = worldTime;
				operationIndex = 2;
			}
		}else if(operationIndex == 2){
			//Third step, main light turns red, cross light stays red.
			if(timeOperationStarted + allRedTime <= worldTime){
				timeOperationStarted = worldTime;
				operationIndex = 3;
			}
		}else if(operationIndex == 3){
			//Fourth step, main light stays red, cross light turns green.
			if(timeOperationStarted + greenCrossTime <= worldTime){
				timeOperationStarted = worldTime;
				operationIndex = 4;
			}
		}else if(operationIndex == 4){
			//Fifth step, main light stays red, cross light turns yellow.
			if(timeOperationStarted + yellowTime <= worldTime){
				timeOperationStarted = worldTime;
				operationIndex = 5;
			}
		}else if(operationIndex == 5){
			//Sixth step, main light stays red, cross light turns red.
			if(timeOperationStarted + allRedTime <= worldTime){
				timeOperationStarted = worldTime;
				//If we are a triggered light, we go back to 0.
				//Otherwise, we perform another cycle and go back to 1.
				if(triggerMode){
					operationIndex = 0;
				}else{
					timeOperationStarted = worldTime;
					operationIndex = 6;
				}
			}
		}else{
			//Seventh step, main light turns green, cross light stays red.
			if(timeOperationStarted + greenMainTime <= worldTime){
				timeOperationStarted = worldTime;
				operationIndex = 0;
			}
		}
	}
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        this.orientedOnX = tagCompound.getBoolean("orientedOnX");
        this.triggerMode = tagCompound.getBoolean("triggerMode");
        this.greenMainTime = tagCompound.getInteger("greenMainTime");
        this.greenCrossTime = tagCompound.getInteger("greenCrossTime");
        this.yellowTime = tagCompound.getInteger("yellowTime");
        this.allRedTime = tagCompound.getInteger("allRedTime");
        
        trafficSignalLocations.clear();
        for(byte i=0; i<tagCompound.getInteger("trafficSignalCount"); ++i){
        	int[] posArray = tagCompound.getIntArray("trafficSignalLocation" + i);
        	trafficSignalLocations.add(new BlockPos(posArray[0], posArray[1], posArray[2]));
        }
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("orientedOnX", this.orientedOnX);
        tagCompound.setBoolean("triggerMode", this.triggerMode);
        tagCompound.setInteger("greenMainTime", this.greenMainTime);
        tagCompound.setInteger("greenCrossTime", this.greenCrossTime);
        tagCompound.setInteger("yellowTime", this.yellowTime);
        tagCompound.setInteger("allRedTime", this.allRedTime);
        
        //Save all pos data to NBT.
        for(byte i=0; i<trafficSignalLocations.size(); ++i){
        	BlockPos trafficSignalPos = trafficSignalLocations.get(i);
	    	tagCompound.setIntArray("trafficSignalLocation" + i, new int[]{trafficSignalPos.getX(), trafficSignalPos.getY(), trafficSignalPos.getZ()});
        }
        tagCompound.setInteger("trafficSignalCount", trafficSignalLocations.size());
        return tagCompound;
    }
}
