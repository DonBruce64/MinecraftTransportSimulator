package minecrafttransportsimulator.vehicles.main;

import java.util.Iterator;

import mcinterface.BuilderEntity;
import mcinterface.WrapperEntity;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.parts.PartSeat;


/**This is the next class level above the base vehicle.
 * This level adds support for riders.  Various methods are overridden here
 * to add greater flexibility to the riding systems.  This allows for riders to
 * change their position and rotation based on what seat they are currently in.
 * 
 * @author don_bruce
 */
abstract class EntityVehicleB_Rideable extends EntityVehicleA_Base{		
	
	public EntityVehicleB_Rideable(BuilderEntity builder, WrapperWorld world, WrapperNBT data){
		super(builder, world, data);
	}
	
	@Override
	public void updateRiders(){
		//We override the default rider update behavior here as the riders can move depending
		//on how the part they are riding moves.  If we modified the rider position, then we'd
		//allow for multiple riders at the same position.  That's Bad Stuff.
		//Update rider positions based on the location they are set to.
		Iterator<WrapperEntity> riderIterator = ridersToLocations.keySet().iterator();
		while(riderIterator.hasNext()){
			WrapperEntity rider = riderIterator.next();
			Point3d riderPositionOffset = ridersToLocations.get(rider);
			if(rider.isValid()){
				//Get the part (seat) this rider is riding.
				PartSeat seat = (PartSeat) getPartAtLocation(riderPositionOffset);

				//Now set the actual position for the seat.  Taking into account height as it's required for pitch offsets.
				Point3d seatOffset = new Point3d(0D, -seat.getHeight()/2D + rider.getYOffset() + rider.getHeight(), 0D).rotateFine(seat.totalRotation).add(seat.totalOffset).add(0D, -rider.getHeight(), 0D);
				rider.setPosition(seatOffset);
			}else{
				//Remove invalid rider.
				removeRider(rider, riderIterator);
			}
		}
	}
	
	@Override
	public boolean addRider(WrapperEntity rider, Point3d riderLocation){
		//We override the default rider addition behavior here as we need to rotate
		//riders to face forwards in seats that they start riding in.
		//Check if this rider is already riding this vehicle.
		boolean riderAlreadyInSeat = ridersToLocations.containsKey(rider);
		boolean success = super.addRider(rider, riderLocation);
		
		if(success){
			//If we weren't riding before, set the player's yaw to the same yaw as the vehicle.
			//We do this to ensure we don't have 360+ rotations to deal with.
			if(!riderAlreadyInSeat){
				PartSeat seat = (PartSeat) getPartAtLocation(riderLocation);
				rider.setYaw(angles.y + seat.placementRotation.y);
			}
		}
		return success;
	}
	
	@Override
	public void removeRider(WrapperEntity rider, Iterator<WrapperEntity> iterator){
		//We override the default rider removal behavior here as the dismount position
		//of riders can be modified via JSON or via part placement location.
		//Get the position the rider was sitting in before we dismount them.
		Point3d riderLocation = ridersToLocations.get(rider);
		super.removeRider(rider, iterator);
		
		//Set the rider dismount position if we are on the server
		if(!world.isClient()){
			VehiclePart packPart = getPackDefForLocation(riderLocation);
			Point3d dismountPosition;
			if(packPart.dismountPos != null){
				//We have a dismount position in the JSON.  Use it.
				dismountPosition = new Point3d(packPart.dismountPos[0], packPart.dismountPos[1], packPart.dismountPos[2]).rotateCoarse(angles).add(position);
			}else{
				//We don't have a dismount position.  Put us to the right or left of the seat depending on x-offset.
				dismountPosition = riderLocation.copy().add(riderLocation.x > 0 ? 2D : -2D, 0D, 0D).rotateCoarse(angles).add(position);	
			}
			rider.setPosition(dismountPosition);
		}
	}
	
	/**
	 *  Helper method used to get the controlling player for this vehicle.
	 */
	public WrapperPlayer getController(){
		for(WrapperEntity rider : ridersToLocations.keySet()){
			PartSeat seat = (PartSeat) getPartAtLocation(ridersToLocations.get(rider));
			if(seat != null && seat.vehicleDefinition.isController && rider instanceof WrapperPlayer){
				return (WrapperPlayer) rider;
			}
		}
		return null;
	}
}
