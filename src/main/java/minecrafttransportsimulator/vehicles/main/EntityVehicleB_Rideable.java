package minecrafttransportsimulator.vehicles.main;

import java.util.Iterator;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartGun;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;
import minecrafttransportsimulator.vehicles.parts.PartSeat;


/**This is the next class level above the base vehicle.
 * This level adds support for riders.  Various methods are overridden here
 * to add greater flexibility to the riding systems.  This allows for riders to
 * change their position and rotation based on what seat they are currently in.
 * 
 * @author don_bruce
 */
abstract class EntityVehicleB_Rideable extends EntityVehicleA_Base{
	public static boolean lockCameraToMovement = true;
	
	public EntityVehicleB_Rideable(IWrapperWorld world, IWrapperNBT data){
		super(world, data);
	}
	
	@Override
	public void updateRider(IWrapperEntity rider, Iterator<IWrapperEntity> iterator){
		//We override the default rider update behavior here as the riders can move depending
		//on how the part they are riding moves.  If we modified the rider position, then we'd
		//allow for multiple riders at the same position.  That's Bad Stuff.
		//Update rider positions based on the location they are set to.
		Point3d riderPositionOffset = locationRiderMap.inverse().get(rider);
		if(rider.isValid()){
			//Get the part (seat) this rider is riding.
			PartSeat seat = (PartSeat) getPartAtLocation(riderPositionOffset);

			//Now set the actual position/motion for the seat.
			Point3d seatLocationOffset = new Point3d(0D, rider.getEyeHeight() + rider.getSeatOffset(), 0D).rotateFine(seat.totalRotation).add(seat.totalOffset).rotateFine(angles).add(position).add(0D, -rider.getEyeHeight(), 0D);
			rider.setPosition(seatLocationOffset);
			rider.setVelocity(motion);
			
			//Rotate the player with the vehicle.
			//This depends on camera state and what we are in.  If we are in a seat with a gun, we need to keep these changes-in sync with the server.
			boolean controllingGun = false;
			for(APart part : parts){
				if(part instanceof PartGun){
					if(rider.equals(((PartGun) part).getCurrentController())){
						controllingGun = true;
					}
				}
			}
            if(controllingGun || !world.isClient() || MasterLoader.gameInterface.inFirstPerson() || lockCameraToMovement){
            	//Get yaw delta between entity and player from -180 to 180.
            	double playerYawDelta = (360 + (angles.y - rider.getYaw())%360)%360;
            	if(playerYawDelta > 180){
            		playerYawDelta-=360;
            	}
            	rider.setYaw(rider.getYaw() + angles.y - prevAngles.y);
        		rider.setPitch(rider.getPitch() + Math.cos(Math.toRadians(playerYawDelta))*(angles.x - prevAngles.x) + Math.sin(Math.toRadians(playerYawDelta))*(angles.z - prevAngles.z));
             }
			
			//If we are on the client, and the rider is the main client player, check controls.
			//If the seat is a controller, and we have mouseYoke enabled, and our view is locked disable the mouse from MC.            	
            //We also need to make sure the player in this event is the actual client player.  If we are on a server,
            //another player could be getting us to this logic point and thus we'd be making their inputs in the vehicle.
			if(world.isClient() && !MasterLoader.gameInterface.isChatOpen() && rider.equals(MasterLoader.gameInterface.getClientPlayer())){
    			ControlSystem.controlVehicle((EntityVehicleF_Physics) this, seat.vehicleDefinition.isController);
    			MasterLoader.inputInterface.setMouseEnabled(!(seat.vehicleDefinition.isController && ConfigSystem.configObject.client.mouseYoke.value && lockCameraToMovement));
    		}
		}else{
			//Remove invalid rider.
			removeRider(rider, iterator);
		}
	}
	
	@Override
	public boolean addRider(IWrapperEntity rider, Point3d riderLocation){
		//We override the default rider addition behavior here as we need to rotate
		//riders to face forwards in seats that they start riding in.
		//Check if this rider is already riding this vehicle.
		boolean riderAlreadyInSeat = locationRiderMap.containsValue(rider);
		boolean success = super.addRider(rider, riderLocation);
		
		if(success){
			//If we weren't riding before, set the player's yaw to the same yaw as the vehicle.
			//We do this to ensure we don't have 360+ rotations to deal with.
			if(!riderAlreadyInSeat){
				//Need to invert the lookup as location may be null from the builder.
				//Rider won't be, as it's required, so we can use it to get the actual location.
				PartSeat seat = (PartSeat) getPartAtLocation(locationRiderMap.inverse().get(rider));
				rider.setYaw(angles.y + seat.placementRotation.y);
			}
		}
		return success;
	}
	
	@Override
	public void removeRider(IWrapperEntity rider, Iterator<IWrapperEntity> iterator){
		//We override the default rider removal behavior here as the dismount position
		//of riders can be modified via JSON or via part placement location.
		//Get the position the rider was sitting in before we dismount them.
		Point3d riderLocation = locationRiderMap.inverse().get(rider);
		super.removeRider(rider, iterator);
		
		//Set the rider dismount position if we are on the server.
		//If we are on the client, disable mouse-yoke blocking.
		if(!world.isClient()){
			VehiclePart packPart = getPackDefForLocation(riderLocation);
			Point3d dismountPosition;
			if(packPart.dismountPos != null){
				//We have a dismount position in the JSON.  Use it.
				dismountPosition = packPart.dismountPos.copy().rotateCoarse(angles).add(position);
			}else{
				//We don't have a dismount position.  Put us to the right or left of the seat depending on x-offset.
				dismountPosition = riderLocation.copy().add(riderLocation.x > 0 ? 2D : -2D, 0D, 0D).rotateCoarse(angles).add(position);	
			}
			rider.setPosition(dismountPosition);
		}else{
			MasterLoader.inputInterface.setMouseEnabled(true);
		}
	}
	
	/**
	 *  Helper method used to get the controlling player for this vehicle.
	 */
	public IWrapperPlayer getController(){
		for(Point3d location : locationRiderMap.keySet()){
			PartSeat seat = (PartSeat) getPartAtLocation(location);
			IWrapperEntity rider = locationRiderMap.get(location);
			if(seat != null && seat.vehicleDefinition.isController && rider instanceof IWrapperPlayer){
				return (IWrapperPlayer) rider;
			}
		}
		return null;
	}
	
	/**
	 * Calculates the current mass of the vehicle.
	 * Includes core mass, player weight (including inventory), and cargo.
	 */
	protected float getCurrentMass(){
		int currentMass = definition.general.emptyMass;
		for(APart part : parts){
			if(part instanceof PartInteractable){
				currentMass += ((PartInteractable) part).getInventoryWeight();
			}
		}
		
		//Add passenger inventory mass as well.
		for(IWrapperEntity rider : locationRiderMap.values()){
			if(rider instanceof IWrapperPlayer){
				currentMass += 100 + ((IWrapperPlayer) rider).getInventory().getInventoryWeight(ConfigSystem.configObject.general.itemWeights.weights);
			}else{
				currentMass += 100;
			}
		}
		return currentMass;
	}
}
