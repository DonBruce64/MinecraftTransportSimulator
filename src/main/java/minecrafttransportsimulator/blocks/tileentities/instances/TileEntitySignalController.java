package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_StreetLight.LightState;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_TrafficSignal.SignalState;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.rendering.instances.RenderDecor;
import minecrafttransportsimulator.vehicles.main.AEntityBase;

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
	public boolean mainDirectionXAxis;
	public OpState currentOpState = OpState.GREEN_MAIN_RED_CROSS;
	public int timeOperationStarted;
	public int greenMainTime = 20;
	public int greenCrossTime = 10;
	public int yellowMainTime = 2;
	public int yellowCrossTime = 2;
	public int allRedTime = 1;
	
	//Locations of blocks.
	public final List<Point3i> componentLocations = new ArrayList<Point3i>();
	
	public TileEntitySignalController(IWrapperWorld world, Point3i position, IWrapperNBT data){
		super(world, position, data);
		//Load state data.
		currentOpMode = OpMode.values()[data.getInteger("currentOpMode")];
		currentOpState = OpState.values()[data.getInteger("currentOpState")];
		timeOperationStarted = data.getInteger("timeOperationStarted");
		mainDirectionXAxis = data.getBoolean("mainDirectionXAxis");
		if(data.getBoolean("hasCustomTimes")){
			greenMainTime = data.getInteger("greenMainTime");
	        greenCrossTime = data.getInteger("greenCrossTime");
	        yellowMainTime = data.getInteger("yellowMainTime");
	        yellowCrossTime = data.getInteger("yellowCrossTime");
	        allRedTime = data.getInteger("allRedTime");
		}
        componentLocations.clear();
        componentLocations.addAll(data.getPoints("componentLocations"));
	}
	
	@Override
	public void update(){
		//Check every 1 seconds to make sure controlled components are in their correct states.
		//This could have changed due to chunkloading.  We also check light redstone state here.
		if(world.getTime()%20 == 0){
			updateState(currentOpState, false);
		}
		
		int currentTime = (int) ((world.getTime()/20)%Integer.MAX_VALUE);
		int redstoneSignal = world.getRedstonePower(position.copy().add(0, -1, 0));
		//If we aren't in remote control mode, do checks for state changes.
		if(!currentOpMode.equals(OpMode.REMOTE_CONTROL)){
			if(!currentOpMode.equals(OpMode.REDSTONE_TRIGGER)){
				//If we aren't in redstone signal mode, check lights.
				if(lightsOn ^ redstoneSignal == 0){
					lightsOn = !lightsOn;
					updateState(currentOpState, false);
				}
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
						for(AEntityBase entity : (world.isClient() ? AEntityBase.createdClientEntities : AEntityBase.createdServerEntities)){
							if(entity.position.x > minX && entity.position.x < maxX && entity.position.z > minZ && entity.position.z < maxZ){
								updateState(OpState.YELLOW_MAIN_RED_CROSS, true);
								break;
							}
						}
					}
				}else if(currentOpMode.equals(OpMode.REDSTONE_TRIGGER)){
					//If redstone is active, start sequence.
					if(redstoneSignal > 0){
						updateState(OpState.YELLOW_MAIN_RED_CROSS, true);
					}
				}else{
					//Not a triggered signal, we must be timed.
					if(timeOperationStarted + greenMainTime <= currentTime){
						updateState(OpState.YELLOW_MAIN_RED_CROSS, true);
					}
				}
			}else{
				//In the middle of a cycle.  Do logic.
				switch(currentOpState){
					case GREEN_MAIN_RED_CROSS : break; //Not gonna happen, we tested for this.
					case YELLOW_MAIN_RED_CROSS : {
						if(timeOperationStarted + yellowMainTime <= currentTime){
							updateState(OpState.RED_MAIN_RED_CROSS, true);
						}
						break;
					}
					case RED_MAIN_RED_CROSS : {
						if(timeOperationStarted + allRedTime <= currentTime){
							updateState(OpState.RED_MAIN_GREEN_CROSS, true);
						}
						break;
					}
					case RED_MAIN_GREEN_CROSS : {
						if(timeOperationStarted + greenCrossTime <= currentTime){
							updateState(OpState.RED_MAIN_YELLOW_CROSS, true);
						}
						break;
					}
					case RED_MAIN_YELLOW_CROSS : {
						if(timeOperationStarted + yellowCrossTime <= currentTime){
							updateState(OpState.RED_MAIN2_RED_CROSS2, true);
						}
						break;
					}
					case RED_MAIN2_RED_CROSS2 : {
						if(timeOperationStarted + allRedTime <= currentTime){
							updateState(OpState.GREEN_MAIN_RED_CROSS, true);
						}
						break;
					}
				}
			}
		}else{
			//We are remotely-controlled.  Adjust state to redstone.
			//First three bits are the state of the controller, the last bit is the light state.
			int stateOpCode = redstoneSignal & 7;
			boolean lightOnSignal = redstoneSignal >> 3 > 0;
			if(lightsOn ^ lightOnSignal){
				lightsOn = !lightsOn;
				updateState(currentOpState, false);
			}
			if(currentOpState.ordinal() != stateOpCode && stateOpCode < OpState.values().length){
				updateState(OpState.values()[stateOpCode], false);
			}
		}
	}
	
	/**
	 * Method to change signal state.  Can be internally called or externally called.
	 * If cycleUpdate is true, then this is assumed to be a cycle increment, so the
	 * timeOperationStarted value is set to the current time.
	 */
	public void updateState(OpState state, boolean cycleUpdate){
		currentOpState = state;
		if(cycleUpdate){
			timeOperationStarted = (int) ((world.getTime()/20)%Integer.MAX_VALUE);
		}
		Iterator<Point3i> iterator = componentLocations.iterator();
		while(iterator.hasNext()){
			TileEntityPole signal = (TileEntityPole) world.getTileEntity(iterator.next());
			if(signal != null){
				for(Axis axis : signal.components.keySet()){
					ATileEntityPole_Component component = signal.components.get(axis);
					if(component instanceof TileEntityPole_TrafficSignal){
						((TileEntityPole_TrafficSignal) component).state = (axis.equals(Axis.NORTH) || axis.equals(Axis.SOUTH)) ^ mainDirectionXAxis ? state.mainSignalState : state.crossSignalState;
					}else if(component instanceof TileEntityPole_StreetLight){
						if(((TileEntityPole_StreetLight) component).state.equals(LightState.ON) ^ lightsOn){
							((TileEntityPole_StreetLight) component).state = lightsOn ? LightState.ON : LightState.OFF;
							signal.updateLightState();
						}
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
    public void save(IWrapperNBT data){
		super.save(data);
		data.setInteger("currentOpMode", currentOpMode.ordinal());
		data.setInteger("currentOpState", currentOpState.ordinal());
		data.setInteger("timeOperationStarted", timeOperationStarted);
		data.setBoolean("mainDirectionXAxis", mainDirectionXAxis);
		data.setBoolean("hasCustomTimes", true);
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
		REDSTONE_TRIGGER,
		REMOTE_CONTROL;
	}
	
	public static enum OpState{
		GREEN_MAIN_RED_CROSS(SignalState.GREEN, SignalState.RED),
		YELLOW_MAIN_RED_CROSS(SignalState.YELLOW, SignalState.RED),
		RED_MAIN_RED_CROSS(SignalState.RED, SignalState.RED),
		RED_MAIN_GREEN_CROSS(SignalState.RED, SignalState.GREEN),
		RED_MAIN_YELLOW_CROSS(SignalState.RED, SignalState.YELLOW),
		RED_MAIN2_RED_CROSS2(SignalState.RED, SignalState.RED);
		
		public final SignalState mainSignalState;
		public final SignalState crossSignalState;
		
		private OpState(SignalState mainSignalState, SignalState crossSignalState){
			this.mainSignalState = mainSignalState;
			this.crossSignalState = crossSignalState;
		}
	}
}
