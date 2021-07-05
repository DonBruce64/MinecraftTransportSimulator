package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.components.LightType;

/**Traffic signal controller tile entity.  Responsible for keeping the state of traffic
 * intersections.
*
* @author don_bruce
*/
public class TileEntitySignalController extends TileEntityDecor implements ITileEntityTickable{	
		
	//Main settings for all operation.
	public boolean isRightHandDrive;
	public boolean timedMode;
	public boolean unsavedClientChangesPreset;
	public Axis mainDirectionAxis = Axis.NORTH;
	
	//Settings for trigger operation.
	public Point3d intersectionCenterPoint;
	
	//Settings for timed operation.
	public int greenMainTime = 20*20;
	public int greenCrossTime = 10*20;
	public int yellowMainTime = 2*20;
	public int yellowCrossTime = 2*20;
	public int allRedTime = 1*20;
	
	/*8Locations of blocks where signals are.**/
	public final Set<Point3d> componentLocations = new HashSet<Point3d>();
	private final Set<TileEntityPole> foundPoles = new HashSet<TileEntityPole>();
	
	/**Signal blocks used in this controller.  Based on components.**/
	public final Set<SignalGroup> signalGroups = new HashSet<SignalGroup>();
	
	/**Lane counts and intersection widths.**/
	public final Map<Axis, IntersectionProperties> intersectionProperties = new HashMap<Axis, IntersectionProperties>();
	
	public TileEntitySignalController(WrapperWorld world, Point3d position, WrapperNBT data){
		super(world, position, data);
		initializeController(data);
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			//Check every 1 seconds to make sure controlled components are in their correct states.
			//This could have changed due to chunkloading or the components being destroyed.
			//We also check if we're doing changes on the client, as that needs to happen instantly.
			if(world.getTick()%20 == 0 || unsavedClientChangesPreset){
				//Check for any missing components, if we are missing some.
				if(componentLocations.size() > foundPoles.size()){
					Iterator<Point3d> iterator = componentLocations.iterator();
					while(iterator.hasNext()){
						Point3d poleLocation = iterator.next();
						TileEntityPole pole = (TileEntityPole) world.getTileEntity(poleLocation);
						if(pole != null && !foundPoles.contains(pole)){
							foundPoles.add(pole);
							for(Axis axis : Axis.values()){
								ATileEntityPole_Component component = pole.components.get(axis);
								if(component instanceof TileEntityPole_TrafficSignal){
									//Add to valid poles and set signals for all groups that have that axis.
									for(SignalGroup signalGroup : signalGroups){
										if(signalGroup.axis.equals(axis)){
											signalGroup.controlledSignals.add((TileEntityPole_TrafficSignal) component);
											signalGroup.setSignals(signalGroup.currentLight);
										}
									}
								}
							}
						}
					}
				}
			}
			
			//All valid poles and components found.  Update signal blocks that have signals..
			for(SignalGroup signalGroup : signalGroups){
				if(!signalGroup.controlledSignals.isEmpty() && signalGroup.laneCount != 0){
					signalGroup.update();
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 *  Updates all components and creates the signal groups for an intersection.  Also updates
	 *  the settings for the controller based on the saved NBT.  Use this whenever the controller
	 *  settings are changed, either by placing the block the first time, or updating via the GUI.
	 *  If the GUI is used, pass null in here to prevent data re-parsing.  Otherwise, pass-in the data.
	 */
	public void initializeController(WrapperNBT data){
		if(data != null){
			//Load state data.
			isRightHandDrive = data.getBoolean("isRightHandDrive");
			timedMode = data.getBoolean("timedMode");
			String axisName = data.getString("mainDirectionAxis");
			mainDirectionAxis = axisName.isEmpty() ? Axis.NORTH : Axis.valueOf(axisName);
			
			intersectionCenterPoint = data.getPoint3d("intersectionCenterPoint");
			if(intersectionCenterPoint.isZero()){
				intersectionCenterPoint.setTo(position);
			}
			
			//Got saved lane info.
			for(Axis axis : Axis.values()){
				intersectionProperties.put(axis, new IntersectionProperties(data.getDataOrNew(axis.name() + "properties")));
			}
			
			if(data.getBoolean("hasCustomTimes")){
				greenMainTime = data.getInteger("greenMainTime");
		        greenCrossTime = data.getInteger("greenCrossTime");
		        yellowMainTime = data.getInteger("yellowMainTime");
		        yellowCrossTime = data.getInteger("yellowCrossTime");
		        allRedTime = data.getInteger("allRedTime");
			}
					
	        //Set new component locations. 
			componentLocations.clear();
	        componentLocations.addAll(data.getPoint3dsCompact("componentLocations"));
	        
	        //Create all signal groups.
	        signalGroups.clear();
	        for(Axis axis : Axis.values()){
	        	signalGroups.add(new SignalGroupCenter(axis, data.getDataOrNew(axis.name() + SignalDirection.CENTER.name())));
	        	signalGroups.add(new SignalGroupLeft(axis, data.getDataOrNew(axis.name() + SignalDirection.LEFT.name())));
	        	signalGroups.add(new SignalGroupRight(axis, data.getDataOrNew(axis.name() + SignalDirection.RIGHT.name())));
	        }
	        
	        //Set all signals to red, except the main-center ones.
	        for(SignalGroup signalGroup : signalGroups){
	        	if(signalGroup.isMainSignal && signalGroup.direction.equals(SignalDirection.CENTER)){
	        		signalGroup.requestedLight = signalGroup.getGreenLight();
	        	}else{
	        		signalGroup.requestedLight = signalGroup.getRedLight();	
	        	}
	        	if(signalGroup.requestedLight.equals(signalGroup.currentLight)){
	        		signalGroup.requestedLight = null;
	        	}
	        	signalGroup.currentCooldown = 0;
	        }
		}
        
        //Clear all found poles as they won't be found anymore for the set groups.
        foundPoles.clear();
	}
    
	@Override
    public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setBoolean("isRightHandDrive", isRightHandDrive);
		data.setBoolean("timedMode", timedMode);
		data.setString("mainDirectionAxis", mainDirectionAxis.name());
		
		data.setPoint3d("intersectionCenterPoint", intersectionCenterPoint);
		
		for(Axis axis : intersectionProperties.keySet()){
			data.setData(axis.name() + "properties", intersectionProperties.get(axis).getData());
		}
		
		data.setBoolean("hasCustomTimes", true);
        data.setInteger("greenMainTime", greenMainTime);
        data.setInteger("greenCrossTime", greenCrossTime);
        data.setInteger("yellowMainTime", yellowMainTime);
        data.setInteger("yellowCrossTime", yellowCrossTime);
        data.setInteger("allRedTime", allRedTime);
        
        data.setPoint3dsCompact("componentLocations", componentLocations);
        for(SignalGroup signalGroup : signalGroups){
        	data.setData(signalGroup.axis.name() + signalGroup.direction.name(), signalGroup.getData());
        }
        return data;
    }
	
	public static class IntersectionProperties{
		public int centerLaneCount;
		public int leftLaneCount;
		public int rightLaneCount;
		public double roadWidth;
		public double centerDistance;
		public double centerOffset;
		
		public IntersectionProperties(WrapperNBT data){
			this.centerLaneCount = data.getInteger("centerLaneCount");
			this.leftLaneCount = data.getInteger("leftLaneCount");
			this.rightLaneCount = data.getInteger("rightLaneCount");
			this.roadWidth = data.getDouble("roadWidth");
			this.centerDistance = data.getDouble("centerDistance");
			this.centerOffset = data.getDouble("centerOffset");
		}
		
		public WrapperNBT getData(){
			WrapperNBT data = new WrapperNBT();
			data.setInteger("centerLaneCount", centerLaneCount);
			data.setInteger("leftLaneCount", leftLaneCount);
			data.setInteger("rightLaneCount", rightLaneCount);
			data.setDouble("roadWidth", roadWidth);
			data.setDouble("centerDistance", centerDistance);
			data.setDouble("centerOffset", centerOffset);
			return data;
		}
	}
	
	public abstract class SignalGroup{
		public final Axis axis;
		public final SignalDirection direction;
		public final boolean isMainSignal;
		public final Set<TileEntityPole_TrafficSignal> controlledSignals = new HashSet<TileEntityPole_TrafficSignal>();
		
		protected LightType currentLight;
		protected LightType requestedLight;
		protected int currentCooldown;
		protected boolean stateChangeRequested;
		
		//Parameters for this signal boxes bounds.  These are all based with a south-facing reference.
		//when checking, the point will be rotated to be in this reference plane.
		public final int laneCount;
		public final double signalLineWidth;
		public final Point3d signalLineCenter;
		
		private SignalGroup(Axis axis, SignalDirection direction, WrapperNBT data){
			this.axis = axis;
			this.direction = direction;
			this.isMainSignal = axis.equals(mainDirectionAxis) || axis.equals(mainDirectionAxis.getOpposite());
			
			//Get saved light status.
			String currentLightName = data.getString("currentLight");
			if(!currentLightName.isEmpty()){
				currentLight = LightType.valueOf(currentLightName);
			}else{
				currentLight = getRedLight();
			}
			String requestedLightName = data.getString("requestedLight");
			if(!requestedLightName.isEmpty()){
				requestedLight = LightType.valueOf(requestedLightName);
			}
			currentCooldown = data.getInteger("currentCooldown");
			
			//Create hitbox bounds.
			IntersectionProperties properties = intersectionProperties.get(axis);
			double laneWidth = properties.roadWidth/(properties.leftLaneCount + properties.centerLaneCount + properties.rightLaneCount);
			switch(direction){
				case CENTER: {
					this.laneCount = properties.centerLaneCount;
					this.signalLineWidth = properties.centerLaneCount*laneWidth;
					this.signalLineCenter = new Point3d(properties.centerOffset + (properties.leftLaneCount + properties.centerLaneCount/2D)*laneWidth, 0, properties.centerDistance);
					break;
				}
				case LEFT: {
					this.laneCount = properties.leftLaneCount;
					this.signalLineWidth = properties.leftLaneCount*laneWidth;
					this.signalLineCenter = new Point3d(properties.centerOffset + (properties.leftLaneCount/2D)*laneWidth, 0, properties.centerDistance);
					break;
				}
				case RIGHT: {
					this.laneCount = properties.rightLaneCount;
					this.signalLineWidth = properties.rightLaneCount*laneWidth;
					this.signalLineCenter = new Point3d(properties.centerOffset + (properties.leftLaneCount + properties.centerLaneCount + properties.rightLaneCount/2D)*laneWidth, 0, properties.centerDistance);
					break;
				}
				default: throw new IllegalStateException("We'll never get here, shut up compiler!");
			}
		}
		
		protected void update(){
			if(currentCooldown > 0){
				--currentCooldown;
			}
			if(requestedLight != null){
				//Currently changing lights.  Handle this logic instead of signal-based logic.
				if(currentCooldown == 0){
					LightType nextLight = getNextLight();
					setSignals(nextLight);
					if(nextLight.equals(requestedLight)){
						requestedLight = null;
						//Need to double cooldown here to prevent us from changing the next tick due to the
						//Opposite light being red.  This prevents us from grabbing the "next" cycle.
						currentCooldown = nextLight.equals(getRedLight()) ? allRedTime + 20 : 0;
					}else{
						currentCooldown = getSignalCooldown();
					}
				}
			}else if(!stateChangeRequested){
				if(requestedLight == null && currentCooldown == 0){
					//Do either time or trigger-based logic.
					if(!currentLight.equals(getGreenLight())){
						if(timedMode){
							//If we are a signal, and not currently changing, and are not green, try to turn green.
							if(direction.equals(SignalDirection.CENTER)){
								stateChangeRequested = true;
							}
						}else{
							//See if we have a vehicle in our intersection bounds and need to change other signals.
							//We only do this once every 2 seconds, and only if we aren't a main-central intersection.
							if(world.getTick()%40 == 0){
								if(isMainSignal && direction.equals(SignalDirection.CENTER)){
									//Just wait until the other signals don't have any cooldown, then set them red.
									stateChangeRequested = true;
								}else{
									for(AEntityC_Definable<?> entity : AEntityC_Definable.getRenderableEntities(world)){
										if(entity instanceof EntityVehicleF_Physics){
											Point3d adjustedPos = entity.position.copy().subtract(intersectionCenterPoint).rotateY(-axis.yRotation);
											if(adjustedPos.x > signalLineCenter.x - signalLineWidth/2D && adjustedPos.x < signalLineCenter.x + signalLineWidth/2D && adjustedPos.z > signalLineCenter.z && adjustedPos.z < signalLineCenter.z + 16){
												//Vehicle present.  If we are blocked, send the respective signal states to the other signals to change them.
												//Flag this signal as pending changes to blocked signals to avoid checking until those signals change.
												stateChangeRequested = true;
												break;
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			if(stateChangeRequested){
				//Check for blocking signals.  If some are present, don't change state.
				//Instead, set those to red if we can, and change when able.
				boolean foundBlockingSignal = false;
				for(SignalGroup otherSignal : signalGroups){
					if(!otherSignal.currentLight.equals(otherSignal.getRedLight()) && isSignalBlocking(otherSignal)){
						//Found blocking signal.  Set to red if able.  Don't do any further checks.
						foundBlockingSignal = true;
						if(otherSignal.requestedLight == null && otherSignal.currentCooldown == 0){
							otherSignal.requestedLight = otherSignal.getRedLight();
							otherSignal.currentCooldown = otherSignal.getSignalCooldown();
						}
					}
				}
				if(!foundBlockingSignal){
					//No blocking signals, or signals were just un-blocked.  Do state-change.
					requestedLight = getGreenLight();
					currentCooldown = getSignalCooldown();
					stateChangeRequested = false;
				}
			}
		}
		
		protected void setSignals(LightType newLight){
			for(TileEntityPole_TrafficSignal signal : controlledSignals){
				if(currentLight != null){
					signal.variablesOn.remove(currentLight.lowercaseName);
				}
				signal.variablesOn.add(newLight.lowercaseName);
			}
			currentLight = newLight;
		}
		
		protected abstract LightType getNextLight();
		
		protected abstract LightType getRedLight();
		
		protected abstract LightType getGreenLight();
		
		protected abstract int getSignalCooldown();
		
		protected abstract boolean isSignalBlocking(SignalGroup otherSignal);
		
		protected WrapperNBT getData(){
			WrapperNBT data = new WrapperNBT();
			data.setString("currentLight", currentLight.name());
			if(requestedLight != null){
				data.setString("requestedLight", requestedLight.name());
			}
			data.setInteger("currentCooldown", currentCooldown);
			return data;
		}
	}
	
	private class SignalGroupCenter extends SignalGroup{
		
		private SignalGroupCenter(Axis axis, WrapperNBT data){
			super(axis, SignalDirection.CENTER, data);
			this.requestedLight = LightType.STOPLIGHT;
		}
		
		@Override
		protected LightType getNextLight(){
			switch(currentLight){
				case GOLIGHT: return LightType.CAUTIONLIGHT;
				case CAUTIONLIGHT: return LightType.STOPLIGHT;
				case STOPLIGHT: return LightType.GOLIGHT;
				default: return null;
			}
		}
		
		@Override
		protected LightType getRedLight(){
			return LightType.STOPLIGHT;
		}
		
		@Override
		protected LightType getGreenLight(){
			return LightType.GOLIGHT;
		}
		
		@Override
		protected int getSignalCooldown(){
			switch(currentLight){
				case GOLIGHT: return isMainSignal ? greenMainTime : greenCrossTime;
				case CAUTIONLIGHT: return isMainSignal ? yellowMainTime : yellowCrossTime;
				case STOPLIGHT: return allRedTime;
				default: return 0;
			}
		}
		
		@Override
		protected boolean isSignalBlocking(SignalGroup otherSignal){
			switch(Axis.getFromRotation(otherSignal.axis.yRotation - axis.yRotation)){
				case SOUTH : { //Same direction.
					return false;
				}
				case EAST : { //Other signal to the right.
					switch(otherSignal.direction){
						case CENTER: return true;
						case LEFT: return !isRightHandDrive;
						case RIGHT: return true;
					}
				}
				case NORTH : { //Opposite direction.
					switch(otherSignal.direction){
						case CENTER: return false;
						case LEFT: return !isRightHandDrive;
						case RIGHT: return isRightHandDrive;
					}
				}
				case WEST : { //Other signal to the left.
					switch(otherSignal.direction){
						case CENTER: return true;
						case LEFT: return true;
						case RIGHT: return isRightHandDrive;
					}
				}
				default : return true; //Unknown direction.
			}
		}
	}
	
	private class SignalGroupLeft extends SignalGroup{
		
		private SignalGroupLeft(Axis axis, WrapperNBT data){
			super(axis, SignalDirection.LEFT, data);
			this.requestedLight = LightType.STOPLIGHTLEFT;
		}
		
		@Override
		protected LightType getNextLight(){
			switch(currentLight){
				case GOLIGHTLEFT: return LightType.CAUTIONLIGHTLEFT;
				case CAUTIONLIGHTLEFT: return LightType.STOPLIGHTLEFT;
				case STOPLIGHTLEFT: return LightType.GOLIGHTLEFT;
				default: return null;
			}
		}
		
		@Override
		protected LightType getRedLight(){
			return LightType.STOPLIGHTLEFT;
		}
		
		@Override
		protected LightType getGreenLight(){
			return LightType.GOLIGHTLEFT;
		}
		
		@Override
		protected int getSignalCooldown(){
			switch(currentLight){
				case GOLIGHTLEFT: return (isMainSignal ? greenMainTime : greenCrossTime)/4;
				case CAUTIONLIGHTLEFT: return (isMainSignal ? yellowMainTime : yellowCrossTime)/4;
				case STOPLIGHTLEFT: return allRedTime;
				default: return 0;
			}
		}
		
		@Override
		protected boolean isSignalBlocking(SignalGroup otherSignal){
			switch(Axis.getFromRotation(otherSignal.axis.yRotation - axis.yRotation)){
				case SOUTH : { //Same direction.
					return false;
				}
				case EAST : { //Other signal to the right.
					switch(otherSignal.direction){
						case CENTER: return true;
						case LEFT: return !isRightHandDrive;
						case RIGHT: return false;
					}
				}
				case NORTH : { //Opposite direction.
					switch(otherSignal.direction){
						case CENTER: return !isRightHandDrive;
						case LEFT: return false;
						case RIGHT: return true;
					}
				}
				case WEST : { //Other signal to the left.
					switch(otherSignal.direction){
						case CENTER: return !isRightHandDrive;
						case LEFT: return !isRightHandDrive;
						case RIGHT: return false;
					}
				}
				default : return true; //Unknown direction.
			}
		}
	}
	
	private class SignalGroupRight extends SignalGroup{
		
		private SignalGroupRight(Axis axis, WrapperNBT data){
			super(axis, SignalDirection.RIGHT, data);
			this.requestedLight = LightType.STOPLIGHTRIGHT;
		}
		
		@Override
		protected LightType getNextLight(){
			switch(currentLight){
				case GOLIGHTRIGHT: return LightType.CAUTIONLIGHTRIGHT;
				case CAUTIONLIGHTRIGHT: return LightType.STOPLIGHTRIGHT;
				case STOPLIGHTRIGHT: return LightType.GOLIGHTRIGHT;
				default: return null;
			}
		}
		
		@Override
		protected LightType getRedLight(){
			return LightType.STOPLIGHTRIGHT;
		}
		
		@Override
		protected LightType getGreenLight(){
			return LightType.GOLIGHTRIGHT;
		}
		
		@Override
		protected int getSignalCooldown(){
			switch(currentLight){
				case GOLIGHTRIGHT: return (isMainSignal ? greenMainTime : greenCrossTime)/4;
				case CAUTIONLIGHTRIGHT: return (isMainSignal ? yellowMainTime : yellowCrossTime)/4;
				case STOPLIGHTRIGHT: return allRedTime;
				default: return 0;
			}
		}
		
		@Override
		protected boolean isSignalBlocking(SignalGroup otherSignal){
			switch(Axis.getFromRotation(otherSignal.axis.yRotation - axis.yRotation)){
				case SOUTH : { //Same direction.
					return false;
				}
				case EAST : { //Other signal to the right.
					switch(otherSignal.direction){
						case CENTER: return true;
						case LEFT: return false;
						case RIGHT: return isRightHandDrive;
					}
				}
				case NORTH : { //Opposite direction.
					switch(otherSignal.direction){
						case CENTER: return isRightHandDrive;
						case LEFT: return true;
						case RIGHT: return false;
					}
				}
				case WEST : { //Other signal to the left.
					switch(otherSignal.direction){
						case CENTER: return true;
						case LEFT: return false;
						case RIGHT: return isRightHandDrive;
					}
				}
				default : return true; //Unknown direction.
			}
		}
	}
	
	public static enum SignalDirection{
		CENTER,
		LEFT,
		RIGHT;
	}
}
