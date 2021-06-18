package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
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
	public boolean isDiagonalIntersection;
	public boolean mainDirectionNorthOrNortheast;
	public boolean unsavedClientChangesPreset;
	
	//Settings for trigger operation.
	public double mainLaneWidth;
	public double mainRoadWidth;
	public double crossLaneWidth;
	public double crossRoadWidth;
	public int mainLeftLaneCount;
	public int mainCenterLaneCount;
	public int mainRightLaneCount;
	public int crossLeftLaneCount;
	public int crossCenterLaneCount;
	public int crossRightLaneCount;
	public Point3d intersectionCenterPoint;
	
	//Settings for timed operation.
	public int greenMainTime = 20;
	public int greenCrossTime = 10;
	public int yellowMainTime = 2;
	public int yellowCrossTime = 2;
	public int allRedTime = 1;
	
	//Locations of blocks where signals are.
	public final Set<Point3d> componentLocations = new HashSet<Point3d>();
	private final Set<TileEntityPole> foundPoles = new HashSet<TileEntityPole>();
	
	/**Signal blocks used in this controller.  Based on components.**/
	private final Set<SignalGroup> signalGroups = new HashSet<SignalGroup>();
	
	/**Lane widths for each lane for each axis.  Used with the groups to determine intersection bounds.**/
	public final Map<Axis, Map<SignalDirection, Double>> laneWidths = new HashMap<Axis, Map<SignalDirection, Double>>();
	
	public TileEntitySignalController(WrapperWorld world, Point3d position, WrapperNBT data){
		super(world, position, data);
		initializeController(data);
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			//Check every 1 seconds to make sure controlled components are in their correct states.
			//This could have changed due to chunkloading or the components being destroyed.
			if(world.getTick()%20 == 0){
				//Check for any missing components, if we are missing some.
				if(componentLocations.size() > foundPoles.size()){
					Iterator<Point3d> iterator = componentLocations.iterator();
					while(iterator.hasNext()){
						Point3d poleLocation = iterator.next();
						TileEntityPole pole = (TileEntityPole) world.getTileEntity(poleLocation);
						if(pole != null && !foundPoles.contains(pole)){
							for(Axis axis : Axis.values()){
								ATileEntityPole_Component component = pole.components.get(axis);
								if(component instanceof TileEntityPole_TrafficSignal){
									foundPoles.add(pole);
									for(SignalGroup signalGroup : signalGroups){
										if(signalGroup.axis.equals(axis)){
											signalGroup.setSignals(null, signalGroup.currentLight);
										}
									}
								}
							}
						}
					}
				}
			}
			
			//All valid poles and components found.  Update signal blocks.
			for(SignalGroup signalGroup : signalGroups){
				signalGroup.update();
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
	 */
	public void initializeController(WrapperNBT data){
		//Load state data.
		isRightHandDrive = data.getBoolean("isRightHandDrive");
		mainDirectionNorthOrNortheast = data.getBoolean("mainDirectionNorthOrNortheast");
		
		mainLaneWidth = data.getInteger("mainLaneWidth");
		mainRoadWidth = data.getDouble("mainRoadWidth");
		crossLaneWidth = data.getInteger("crossLaneWidth");
		crossRoadWidth = data.getDouble("crossRoadWidth");
		mainLeftLaneCount = data.getInteger("mainLeftLaneCount");
		mainCenterLaneCount = data.getInteger("mainCenterLaneCount");
		mainRightLaneCount = data.getInteger("mainRightLaneCount");
		crossLeftLaneCount = data.getInteger("crossLeftLaneCount");
		crossCenterLaneCount = data.getInteger("crossCenterLaneCount");
		crossRightLaneCount = data.getInteger("crossRightLaneCount");
		intersectionCenterPoint = data.getPoint3dCompact("intersectionCenterPoint");
		
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
        
        //Get all signal axis to create all groups.
        Set<Axis> activeAxis = new HashSet<Axis>();
        for(Point3d poleLocation : componentLocations){
			TileEntityPole pole = (TileEntityPole) world.getTileEntity(poleLocation);
			if(pole != null){
				for(Entry<Axis, ATileEntityPole_Component> componentEntry : pole.components.entrySet()){
					if(componentEntry.getValue() instanceof TileEntityPole_TrafficSignal){
						activeAxis.add(componentEntry.getKey());
					}
				}
			}
		}
        
        //Create all applicable signal groups.
        signalGroups.clear();
        for(Axis axis : activeAxis){
        	boolean isAxisMain = mainDirectionNorthOrNortheast ? axis.equals(Axis.NORTH) || axis.equals(Axis.NORTHEAST) || axis.equals(Axis.SOUTH) || axis.equals(Axis.SOUTHWEST) : axis.equals(Axis.EAST) || axis.equals(Axis.SOUTHEAST) || axis.equals(Axis.WEST) || axis.equals(Axis.NORTHWEST); 
        	signalGroups.add(new SignalGroupCenter(axis, data.getData(axis.name() + SignalDirection.CENTER.name())));
        	if(isAxisMain ? mainLeftLaneCount > 0 : crossLeftLaneCount > 0)signalGroups.add(new SignalGroupLeft(axis, data.getData(axis.name() + SignalDirection.LEFT.name())));
        	if(isAxisMain ? mainRightLaneCount > 0 : crossRightLaneCount > 0)signalGroups.add(new SignalGroupRight(axis, data.getData(axis.name() + SignalDirection.RIGHT.name())));
        }
        
        isDiagonalIntersection = activeAxis.contains(Axis.NORTHEAST) || activeAxis.contains(Axis.SOUTHWEST);
	}
    
	@Override
    public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setBoolean("isRightHandDrive", isRightHandDrive);
		data.setBoolean("mainDirectionNorthOrNortheast", mainDirectionNorthOrNortheast);
		
		data.setDouble("mainLaneWidth", mainLaneWidth);
		data.setDouble("mainRoadWidth", mainRoadWidth);
		data.setDouble("crossLaneWidth", crossLaneWidth);
		data.setDouble("crossRoadWidth", crossRoadWidth);
		data.setInteger("mainLeftLaneCount", mainLeftLaneCount);
		data.setInteger("mainCenterLaneCount", mainCenterLaneCount);
		data.setInteger("mainRightLaneCount", mainRightLaneCount);
		data.setInteger("crossLeftLaneCount", crossLeftLaneCount);
		data.setInteger("crossCenterLaneCount", crossCenterLaneCount);
		data.setInteger("crossRightLaneCount", crossRightLaneCount);
		data.setPoint3dCompact("intersectionCenterPoint", intersectionCenterPoint);
		
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
	
	private abstract class SignalGroup{
		protected final Axis axis;
		protected final SignalDirection direction;
		protected final boolean isMainSignal;
		
		protected LightType currentLight;
		protected LightType requestedLight;
		protected int currentCooldown;
		protected Set<SignalGroup> blockingSignals = new HashSet<SignalGroup>();
		
		//Parameters for this signal boxes bounds.  These are all based with a south-facing reference.
		//when checking, the point will be rotated to be in this reference plane.
		protected final double signalLineWidth;
		protected final Point3d signalLineCenter;
		
		private SignalGroup(Axis axis, SignalDirection direction, WrapperNBT data){
			this.axis = axis;
			this.direction = direction;
			if(mainDirectionNorthOrNortheast){
				if(isDiagonalIntersection){
					isMainSignal = axis.equals(Axis.NORTHEAST) || axis.equals(Axis.SOUTHWEST);
				}else{
					isMainSignal = axis.equals(Axis.NORTH) || axis.equals(Axis.SOUTH);
				}
			}else{
				if(isDiagonalIntersection){
					isMainSignal = axis.equals(Axis.SOUTHEAST) || axis.equals(Axis.NORTHWEST);
				}else{
					isMainSignal = axis.equals(Axis.EAST) || axis.equals(Axis.WEST);
				}
			}
			
			String currentLightName = data.getString("currentLight");
			if(!currentLightName.isEmpty()){
				currentLight = LightType.valueOf(currentLightName);
			}
			String requestedLightName = data.getString("requestedLight");
			if(!requestedLightName.isEmpty()){
				requestedLight = LightType.valueOf(requestedLightName);
			}
			currentCooldown = data.getInteger("currentCooldown");
			
			double totalRoadWidth;
			double distanceToSignalsFromCenter;
			if(isMainSignal){
				totalRoadWidth = mainRoadWidth;
				distanceToSignalsFromCenter = (crossLeftLaneCount + crossCenterLaneCount + crossRightLaneCount)*crossLaneWidth/2D;
			}else{
				totalRoadWidth = crossRoadWidth;
				distanceToSignalsFromCenter = (mainLeftLaneCount + mainCenterLaneCount + mainRightLaneCount)*mainLaneWidth/2D;
			}
			switch(direction){
				case CENTER: {
					this.signalLineWidth = isMainSignal ? mainCenterLaneCount*mainLaneWidth : crossCenterLaneCount*crossLaneWidth;
					double leftSegmentWidth = isMainSignal ? mainLeftLaneCount*mainLaneWidth : crossLeftLaneCount*crossLaneWidth;
					this.signalLineCenter = new Point3d(-totalRoadWidth/2D + leftSegmentWidth + signalLineWidth/2D, 0, distanceToSignalsFromCenter);
					break;
				}
				case LEFT: {
					this.signalLineWidth = isMainSignal ? mainLeftLaneCount*mainLaneWidth : crossLeftLaneCount*crossLaneWidth;
					this.signalLineCenter = new Point3d(-totalRoadWidth/2D + signalLineWidth/2D, 0, distanceToSignalsFromCenter);
					break;
				}
				case RIGHT: {
					this.signalLineWidth = isMainSignal ? mainRightLaneCount*mainLaneWidth : crossRightLaneCount*crossLaneWidth;
					this.signalLineCenter = new Point3d(totalRoadWidth/2D - signalLineWidth/2D, 0, distanceToSignalsFromCenter);
					break;
				}
				default: throw new IllegalStateException("We'll never get here, shut up compiler!");
			}
		}
		
		protected void update(){
			if(currentCooldown > 0){
				//Currently changing lights.  Handle this logic instead of signal-based logic.
				if(--currentCooldown == 0){
					LightType nextLight = getNextLight();
					setSignals(currentLight, nextLight);
					currentCooldown = getSignalCooldown();
					if(nextLight.equals(requestedLight)){
						requestedLight = null;
					}
				}
			}else if(blockingSignals.isEmpty()){
				//See if we have a vehicle in our intersection bounds and need to change other signals.
				//We only do this once every 2 seconds, and only if we aren't a main-central intersection.
				if(!(isMainSignal && direction.equals(SignalDirection.CENTER)) && world.getTick()%40 == 0){
					for(AEntityC_Definable<?> entity : AEntityC_Definable.getRenderableEntities(world)){
						if(entity instanceof EntityVehicleF_Physics){
							Point3d adjustedPos = entity.position.copy().subtract(intersectionCenterPoint).rotateY(-axis.yRotation);
							if(adjustedPos.x > signalLineCenter.x - signalLineWidth/2D && adjustedPos.x < signalLineCenter.x + signalLineWidth/2D && adjustedPos.z > signalLineCenter.z && adjustedPos.z < signalLineCenter.z + 16){
								//Vehicle present.  If we are blocked, send the respective signal states to the other signals to change them.
								//Flag this signal as pending changes to blocked signals to avoid checking until those signals change.
								for(SignalGroup otherSignal : signalGroups){
									if(!otherSignal.equals(this) && this.isSignalBlocking(otherSignal)){
										blockingSignals.add(otherSignal);
									}
								}
								break;
							}
						}
					}
				}
			}else{
				//Wait until blocking signals are clear, then make ourselves green.
				//If the blocking signal isn't red, try to make it be red.  If it is red, remove the signal as blocking.
				Iterator<SignalGroup> iterator = blockingSignals.iterator();
				while(iterator.hasNext()){
					SignalGroup blockingSignal = iterator.next();
					if(blockingSignal.requestedLight == null && blockingSignal.currentCooldown == 0){
						LightType redLight = blockingSignal.getRedLight();
						if(!blockingSignal.currentLight.equals(redLight)){
							blockingSignal.setSignals(blockingSignal.currentLight, blockingSignal.getRedLight());
						}else{
							iterator.remove();
						}
					}
				}
				if(blockingSignals.isEmpty()){
					//Change our signal state as no signals are blocking.
					setSignals(currentLight, getGreenLight());
				}
			}
		}
		
		protected void setSignals(LightType oldLight, LightType newLight){
			for(TileEntityPole pole : foundPoles){
				ATileEntityPole_Component component = pole.components.get(axis);
				if(component instanceof TileEntityPole_TrafficSignal){
					if(oldLight != null){
						component.variablesOn.remove(oldLight.lowercaseName);
					}
					component.variablesOn.add(newLight.lowercaseName);
				}
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
						case CENTER: return false;
						case LEFT: return !isRightHandDrive;
						case RIGHT: return false;
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
						case CENTER: return false;
						case LEFT: return false;
						case RIGHT: return !isRightHandDrive;
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
						case CENTER: return false;
						case LEFT: return isRightHandDrive;
						case RIGHT: return true;
					}
				}
				case NORTH : { //Opposite direction.
					switch(otherSignal.direction){
						case CENTER: return !isRightHandDrive;
						case LEFT: return true;
						case RIGHT: return false;
					}
				}
				case WEST : { //Other signal to the left.
					switch(otherSignal.direction){
						case CENTER: return isRightHandDrive;
						case LEFT: return isRightHandDrive;
						case RIGHT: return true;
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
						case CENTER: return false;
						case LEFT: return true;
						case RIGHT: return !isRightHandDrive;
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
						case CENTER: return false;
						case LEFT: return true;
						case RIGHT: return !isRightHandDrive;
					}
				}
				default : return true; //Unknown direction.
			}
		}
	}
	
	private static enum SignalDirection{
		CENTER,
		LEFT,
		RIGHT;
	}
}
