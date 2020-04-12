package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_CrossingSignal.CrossingState;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_StreetLight.LightState;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_TrafficSignal.SignalState;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.rendering.blocks.RenderDecor;
import minecrafttransportsimulator.wrappers.WrapperNBT;

/**Traffic signal controller tile entity.  Responsible for keeping the state of traffic
 * intersections.
*
* @author don_bruce
*/
public class TileEntitySignalController extends ATileEntityBase<JSONDecor> implements ITileEntityTickable{	
	//Mode state.
	public OpMode currentOpMode = OpMode.TIMED_CYCLE;
	
	//Timers and controls for automatic control modes.
	public boolean lightsOn = true;
	public boolean mainDirectionXAxis = false;
	public OpState currentOpState = OpState.GREEN_MAIN_RED_CROSS;
	public long timeOperationStarted;
	public int greenMainTime = 20;
	public int greenCrossTime = 10;
	public int yellowMainTime = 2;
	public int yellowCrossTime = 2;
	public int allRedTime = 1;
	
	//Locations of blocks.
	public final List<Point3i> componentLocations = new ArrayList<Point3i>();
	
	@Override
	public void update(){
		long currentTime = world.getTime()/20;
		//If we aren't in remote control mode, do checks for state changes.
		if(!currentOpMode.equals(OpMode.REMOTE_CONTROL)){
			//Change light status based on redstone state.
			if(lightsOn ^ world.getRedstonePower(position.newOffset(0, -1, 0)) == 0){
				lightsOn = !lightsOn;
				changeState(currentOpState);
			}
			
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
						for(Point3i controllerSignalPos : componentLocations){
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
					if(timeOperationStarted + greenMainTime <= currentTime){
						changeState(OpState.YELLOW_MAIN_RED_CROSS);
					}
				}
			}else{
				//In the middle of a cycle.  Do logic.
				switch(currentOpState){
					case GREEN_MAIN_RED_CROSS : break; //Not gonna happen, we tested for this.
					case YELLOW_MAIN_RED_CROSS : {
						if(timeOperationStarted + yellowMainTime <= currentTime){
							changeState(OpState.RED_MAIN_RED_CROSS);
						}
						break;
					}
					case RED_MAIN_RED_CROSS : {
						if(timeOperationStarted + allRedTime <= currentTime){
							changeState(OpState.RED_MAIN_GREEN_CROSS);
						}
						break;
					}
					case RED_MAIN_GREEN_CROSS : {
						if(timeOperationStarted + greenCrossTime <= currentTime){
							changeState(OpState.RED_MAIN_YELLOW_CROSS);
						}
						break;
					}
					case RED_MAIN_YELLOW_CROSS : {
						if(timeOperationStarted + yellowCrossTime <= currentTime){
							changeState(OpState.RED_MAIN2_RED_CROSS2);
						}
						break;
					}
					case RED_MAIN2_RED_CROSS2 : {
						if(timeOperationStarted + allRedTime <= currentTime){
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
		timeOperationStarted = world.getTime()/20;
		Iterator<Point3i> iterator = componentLocations.iterator();
		while(iterator.hasNext()){
			TileEntityPole signal = (TileEntityPole) world.getTileEntity(iterator.next());
			if(signal != null){
				for(Axis axis : signal.components.keySet()){
					ATileEntityPole_Component component = signal.components.get(axis);
					if(component instanceof TileEntityPole_TrafficSignal){
						((TileEntityPole_TrafficSignal) component).state = (axis.equals(Axis.NORTH) || axis.equals(Axis.SOUTH)) ^ mainDirectionXAxis ? state.mainSignalState : state.crossSignalState;
					}else if(component instanceof TileEntityPole_CrossingSignal){
						((TileEntityPole_CrossingSignal) component).state = (axis.equals(Axis.NORTH) || axis.equals(Axis.SOUTH)) ^ mainDirectionXAxis ? state.mainCrossingState : state.crossCrossingState;
					}else if(component instanceof TileEntityPole_StreetLight){
						((TileEntityPole_StreetLight) component).state = lightsOn ? LightState.ON : LightState.OFF;
					}
				}
			}
		}
	}
	
	@Override
	public RenderDecor getRenderer(){
		return new RenderDecor();
	}
	
	@Override
    public void load(WrapperNBT data){
		super.load(data);
		currentOpMode = OpMode.values()[data.getInteger("currentOpMode")];
		mainDirectionXAxis = data.getBoolean("mainDirectionXAxis");
        greenMainTime = data.getInteger("greenMainTime");
        greenCrossTime = data.getInteger("greenCrossTime");
        yellowMainTime = data.getInteger("yellowMainTime");
        yellowCrossTime = data.getInteger("yellowCrossTime");
        allRedTime = data.getInteger("allRedTime");
        
        componentLocations.clear();
        componentLocations.addAll(data.getPoints("componentLocations"));
    }
    
	@Override
    public void save(WrapperNBT data){
		super.save(data);
		data.setInteger("currentOpMode", currentOpMode.ordinal());
        data.setBoolean("mainDirectionXAxis", mainDirectionXAxis);
        data.setInteger("greenMainTime", greenMainTime);
        data.setInteger("greenCrossTime", greenCrossTime);
        data.setInteger("yellowMainTime", yellowMainTime);
        data.setInteger("yellowCrossTime", yellowCrossTime);
        data.setInteger("allRedTime", allRedTime);
        data.setPoints("componentLocations", componentLocations);
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
