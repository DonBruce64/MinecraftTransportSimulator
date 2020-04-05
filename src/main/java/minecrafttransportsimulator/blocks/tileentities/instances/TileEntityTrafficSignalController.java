package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityTickable;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleCrossingSignal.CrossingState;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleTrafficSignal.SignalState;
import minecrafttransportsimulator.rendering.blocks.ARenderTileEntityBase;
import minecrafttransportsimulator.wrappers.WrapperNBT;

/**Traffic signal controller tile entity.  Responsible for keeping the state of traffic
 * intersections.
*
* @author don_bruce
*/
public class TileEntityTrafficSignalController extends ATileEntityTickable{	
	//Mode state.
	public OpMode currentOpMode = OpMode.TIMED_CYCLE;
	
	//Timers and controls for automatic control modes.
	public boolean mainDirectionXAxis = false;
	public OpState currentOpState = OpState.GREEN_MAIN_RED_CROSS;
	public long timeOperationStarted;
	public int greenMainTime = 20;
	public int greenCrossTime = 10;
	public int yellowMainTime = 2;
	public int yellowCrossTime = 2;
	public int allRedTime = 1;
	
	//Locations of blocks.
	public final List<Point3i> trafficSignalLocations = new ArrayList<Point3i>();
	public final List<Point3i> crossingSignalLocations = new ArrayList<Point3i>();
	
	
	@Override
	public void update(){
		long currentTime = System.currentTimeMillis()/1000;
		//If we aren't in remote control mode, do checks for state changes.
		if(!currentOpMode.equals(OpMode.REMOTE_CONTROL)){
			//If we are in the idle op sate, check if we need to start a cycle.
			if(currentOpState.equals(OpState.GREEN_MAIN_RED_CROSS)){
				if(currentOpMode.equals(OpMode.VEHICLE_TRIGGER)){
					//We're a triggered signal, check for vehicles.
					//Check only once every two seconds to prevent lag.
					if(currentTime%2 == 0){
						//Get a bounding box for all lights in the controller system.
						int minX = Integer.MAX_VALUE;
						int maxX = Integer.MIN_VALUE;
						int minZ = Integer.MAX_VALUE;
						int maxZ = Integer.MIN_VALUE;
						for(Point3i controllerSignalPos : trafficSignalLocations){
							minX = Math.min(minX, controllerSignalPos.x);
							maxX = Math.max(maxX, controllerSignalPos.x);
							minZ = Math.min(minZ, controllerSignalPos.z);
							maxZ = Math.max(maxZ, controllerSignalPos.z);
						}
						
						//Take 16 off to expand the detection boxes for the axis.
						if(mainDirectionXAxis){
							minZ -= 16;
							maxZ += 16;
						}else{
							minX -= 16;
							maxX += 16;
						}
						
						//Now we have min-max, check for any vehicles in the area.
						//We need to check along the non-primary axis, but we don't care about Y.
						BoundingBox bounds = new BoundingBox(minX + (maxX - minX)/2, 0, minZ + (maxZ - minZ)/2, (maxX - minX)/2, Double.MAX_VALUE, (maxZ - minZ)/2);
						if(!world.getVehiclesWithin(bounds).isEmpty()){
							changeState(OpState.YELLOW_MAIN_RED_CROSS);
						}
					}
				}else{
					//Not a triggered signal, we must be timed.
					if(timeOperationStarted + greenMainTime < currentTime){
						changeState(OpState.YELLOW_MAIN_RED_CROSS);
					}
				}
			}else{
				//In the middle of a cycle.  Do logic.
				switch(currentOpState){
					case GREEN_MAIN_RED_CROSS : break; //Not gonna happen, we tested for this.
					case YELLOW_MAIN_RED_CROSS : {
						if(timeOperationStarted + yellowMainTime < currentTime){
							changeState(OpState.RED_MAIN_RED_CROSS);
						}
						break;
					}
					case RED_MAIN_RED_CROSS : {
						if(timeOperationStarted + allRedTime < currentTime){
							changeState(OpState.RED_MAIN_GREEN_CROSS);
						}
						break;
					}
					case RED_MAIN_GREEN_CROSS : {
						if(timeOperationStarted + greenCrossTime < currentTime){
							changeState(OpState.RED_MAIN_YELLOW_CROSS);
						}
						break;
					}
					case RED_MAIN_YELLOW_CROSS : {
						if(timeOperationStarted + yellowCrossTime < currentTime){
							changeState(OpState.RED_MAIN2_RED_CROSS2);
						}
						break;
					}
					case RED_MAIN2_RED_CROSS2 : {
						if(timeOperationStarted + allRedTime < currentTime){
							changeState(OpState.GREEN_MAIN_RED_CROSS);
						}
						break;
					}
				}
			}
		}
	}
	
	/**
	 * Method to change signal state.  Can be internally called or externally called.
	 */
	public void changeState(OpState state){
		currentOpState = state;
		timeOperationStarted = System.currentTimeMillis()/1000;
		Iterator<Point3i> iterator = trafficSignalLocations.iterator();
		while(iterator.hasNext()){
			TileEntityPoleTrafficSignal signal = (TileEntityPoleTrafficSignal) world.getTileEntity(iterator.next());
			if(signal != null){
				ABlockBase block = signal.getBlock();
				if(block != null){
					//If the rotation is 0 or 180, we're on the z-axis.
					//Therefore, if that is true and main direction isn't x, then we're a main signal.
					if(block.getRotation(world, signal.position)%180 == 0 ^ mainDirectionXAxis){
						//On z-axis and main direction z, or on x-axis and main direction x.  Main signal.
						signal.state = state.mainSignalState;
					}else{
						//On z-axis and main direction x, or on x-axis and main direction z.  Cross signal.
						signal.state = state.crossSignalState;
					}
				}
			}
		}
		iterator = crossingSignalLocations.iterator();
		while(iterator.hasNext()){
			TileEntityPoleCrossingSignal signal = (TileEntityPoleCrossingSignal) world.getTileEntity(iterator.next());
			if(signal != null){
				ABlockBase block = signal.getBlock();
				if(block != null){
					//If the rotation is 0 or 180, we're on the z-axis.
					//Therefore, if that is true and main direction isn't x, then we're a main crossing.
					if(block.getRotation(world, signal.position)%180 == 0 ^ mainDirectionXAxis){
						//On z-axis and main direction z, or on x-axis and main direction x.  Main crossing.
						signal.state = state.mainCrossingState;
					}else{
						//On z-axis and main direction x, or on x-axis and main direction z.  Cross crossing.
						signal.state = state.crossCrossingState;
					}
				}
			}
		}
	}
	
	@Override
	public ARenderTileEntityBase<? extends ATileEntityBase, ? extends IBlockTileEntity> getRenderer(){
		return null;
	}
	
	@Override
    public void load(WrapperNBT data){
		currentOpMode = OpMode.values()[data.getInteger("currentOpMode")];
		mainDirectionXAxis = data.getBoolean("mainDirectionXAxis");
        greenMainTime = data.getInteger("greenMainTime");
        greenCrossTime = data.getInteger("greenCrossTime");
        yellowMainTime = data.getInteger("yellowMainTime");
        yellowCrossTime = data.getInteger("yellowCrossTime");
        allRedTime = data.getInteger("allRedTime");
        
        trafficSignalLocations.clear();
        crossingSignalLocations.clear();
        trafficSignalLocations.addAll(data.getPoints("trafficSignalLocations"));
        crossingSignalLocations.addAll(data.getPoints("crossingSignalLocations"));
    }
    
	@Override
    public void save(WrapperNBT data){
		data.setInteger("currentOpMode", currentOpMode.ordinal());
        data.setBoolean("mainDirectionXAxis", mainDirectionXAxis);
        data.setInteger("greenMainTime", greenMainTime);
        data.setInteger("greenCrossTime", greenCrossTime);
        data.setInteger("yellowMainTime", yellowMainTime);
        data.setInteger("yellowCrossTime", yellowCrossTime);
        data.setInteger("allRedTime", allRedTime);
        data.setPoints("trafficSignalLocations", trafficSignalLocations);
        data.setPoints("crossingSignalLocations", crossingSignalLocations);
    }
	
	public static enum OpMode{
		TIMED_CYCLE,
		VEHICLE_TRIGGER,
		REMOTE_CONTROL;
	}
	
	public static enum OpState{
		GREEN_MAIN_RED_CROSS(SignalState.GREEN, SignalState.RED, CrossingState.WALK, CrossingState.DONTWALK),
		YELLOW_MAIN_RED_CROSS(SignalState.YELLOW, SignalState.RED, CrossingState.FLASHING_DONTWALK, CrossingState.DONTWALK),
		RED_MAIN_RED_CROSS(SignalState.RED, SignalState.RED, CrossingState.DONTWALK, CrossingState.DONTWALK),
		RED_MAIN_GREEN_CROSS(SignalState.RED, SignalState.GREEN, CrossingState.DONTWALK, CrossingState.WALK),
		RED_MAIN_YELLOW_CROSS(SignalState.RED, SignalState.YELLOW, CrossingState.DONTWALK, CrossingState.FLASHING_DONTWALK),
		RED_MAIN2_RED_CROSS2(SignalState.RED, SignalState.RED, CrossingState.DONTWALK, CrossingState.DONTWALK);
		
		public final SignalState mainSignalState;
		public final SignalState crossSignalState;
		public final CrossingState mainCrossingState;
		public final CrossingState crossCrossingState;
		
		private OpState(SignalState mainSignalState, SignalState crossSignalState, CrossingState mainCrossingState, CrossingState crossCrossingState){
			this.mainSignalState = mainSignalState;
			this.crossSignalState = crossSignalState;
			this.mainCrossingState = mainCrossingState;
			this.crossCrossingState = crossCrossingState;
		}
	}
}
