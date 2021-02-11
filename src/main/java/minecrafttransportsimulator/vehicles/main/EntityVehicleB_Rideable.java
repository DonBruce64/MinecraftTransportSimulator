package minecrafttransportsimulator.vehicles.main;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.AEntityE_Multipart;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.controls.ControlSystem;
import minecrafttransportsimulator.controls.InterfaceInput;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.systems.ConfigSystem;
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
abstract class EntityVehicleB_Rideable extends AEntityE_Multipart<JSONVehicle>{
	public static boolean lockCameraToMovement = true;
	
	/**Cached value for speedFactor.  Saves us from having to use the long form all over.  Not like it'll change in-game...*/
	public static final double SPEED_FACTOR = ConfigSystem.configObject.general.speedFactor.value;
	
	public EntityVehicleB_Rideable(WrapperWorld world, WrapperNBT data){
		super(world, data);
	}
	
	@Override
	public void updateRider(WrapperEntity rider, Iterator<WrapperEntity> iterator){
		//We override the default rider update behavior here as the riders can move depending
		//on how the part they are riding moves.  If we modified the rider position, then we'd
		//allow for multiple riders at the same position.  That's Bad Stuff.
		//Update rider positions based on the location they are set to.
		Point3d riderPositionOffset = locationRiderMap.inverse().get(rider);
		PartSeat seat = (PartSeat) getPartAtLocation(riderPositionOffset);
		if(rider.isValid() && seat != null){
			//Add all vehicle-wide effects to the rider
			if(this.definition.effects != null) {
				for(JSONPotionEffect effect: this.definition.effects){
					rider.addPotionEffect(effect);
				}
			}
			
			//Add all seat-specific effects to the rider
			if(seat.partDefinition.seatEffects != null) {
				for(JSONPotionEffect effect: seat.partDefinition.seatEffects){
					rider.addPotionEffect(effect);
				}
			}

			//Now set the actual position/motion for the seat.
			double seatYPos = rider.getEyeHeight() + rider.getSeatOffset();
			if(seat.definition.seat.heightScale != 0){
				seatYPos *= seat.definition.seat.heightScale;
			}
			Point3d seatLocationOffset = new Point3d(0D, seatYPos, 0D).rotateFine(seat.totalRotation).add(seat.totalOffset).rotateFine(angles).add(position).add(0D, -rider.getEyeHeight(), 0D);
			rider.setPosition(seatLocationOffset);
			rider.setVelocity(motion);
			
			//Rotate the player with the vehicle.
			//This depends on camera state and what we are in.  If we are in a seat with a gun, we need to keep these changes-in sync with the server.
			boolean controllingGun = false;
			for(APart part : parts){
				if(part instanceof PartGun){
					if(rider.equals(((PartGun) part).getController())){
						controllingGun = true;
					}
				}
			}
            if(controllingGun || !world.isClient() || InterfaceClient.inFirstPerson() || lockCameraToMovement){
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
			if(world.isClient() && !InterfaceClient.isChatOpen() && rider.equals(InterfaceClient.getClientPlayer())){
    			ControlSystem.controlVehicle((EntityVehicleF_Physics) this, seat.partDefinition.isController);
    			InterfaceInput.setMouseEnabled(!(seat.partDefinition.isController && ConfigSystem.configObject.clientControls.mouseYoke.value && lockCameraToMovement));
    		}
		}else{
			//Remove invalid rider.
			removeRider(rider, iterator);
		}
	}
	
	@Override
	public boolean addRider(WrapperEntity rider, Point3d riderLocation){
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
				rider.setYaw(angles.y + seat.totalRotation.y);
			}
		}
		
		return success;
	}
	
	@Override
	public void removeRider(WrapperEntity rider, Iterator<WrapperEntity> iterator){
		//We override the default rider removal behavior here as the dismount position
		//of riders can be modified via JSON or via part placement location.
		//Get the position the rider was sitting in before we dismount them.
		Point3d riderLocation = locationRiderMap.inverse().get(rider);
		super.removeRider(rider, iterator);

		//Get rid of any potion effects that were caused by the vehicle
		if(this.definition.effects != null) {
			for(JSONPotionEffect effect: this.definition.effects){
				rider.removePotionEffect(effect);
			}
		}
		
		//Get rid of any potion effects that were caused by the seat
		JSONPartDefinition packPart = getPackDefForLocation(riderLocation);
		if(packPart.seatEffects != null) {
			for(JSONPotionEffect effect: packPart.seatEffects){
				rider.removePotionEffect(effect);
			}
		}
		
		//Set the rider dismount position.
		//If we have a dismount position in the JSON.  Use it.
		//Otherwise, put us to the right or left of the seat depending on x-offset.
		//Make sure to take into the movement of the seat we were riding if it had moved.
		//This ensures the dismount moves with the seat.
		Point3d dismountPosition;
		APart partRiding = getPartAtLocation(riderLocation);
		if(packPart.dismountPos != null){
			if(partRiding != null){
				dismountPosition = packPart.dismountPos.copy().add(partRiding.totalOffset).subtract(partRiding.placementOffset).rotateCoarse(angles).add(position);
			}else{
				dismountPosition = packPart.dismountPos.copy().rotateCoarse(angles).add(position);
			}
		}else{
			if(partRiding != null){
				Point3d partDelta = partRiding.totalOffset.copy().subtract(partRiding.placementOffset);
				if(riderLocation.x < 0){
					partDelta.x = -partDelta.x;
					dismountPosition = riderLocation.copy().add(-2D, 0D, 0D).add(partDelta).rotateCoarse(angles).add(position);
				}else{
					dismountPosition = riderLocation.copy().add(2D, 0D, 0D).add(partDelta).rotateCoarse(angles).add(position);
				}
			}else{
				dismountPosition = riderLocation.copy().add(riderLocation.x > 0 ? 2D : -2D, 0D, 0D).rotateCoarse(angles).add(position);
			}
		}
		rider.setPosition(dismountPosition);
		
		//If we are on the client, disable mouse-yoke blocking.
		if(world.isClient() && InterfaceClient.getClientPlayer().equals(rider)){
			//Client player is the one that left the vehicle.  Make sure they don't have their mouse locked or a GUI open.
			InterfaceInput.setMouseEnabled(true);
			InterfaceGUI.closeGUI();
		}
	}
	
	/**
	 *  Helper method used to get the controlling player for this vehicle.
	 */
	public WrapperPlayer getController(){
		for(Point3d location : locationRiderMap.keySet()){
			PartSeat seat = (PartSeat) getPartAtLocation(location);
			WrapperEntity rider = locationRiderMap.get(location);
			if(seat != null && seat.partDefinition.isController && rider instanceof WrapperPlayer){
				return (WrapperPlayer) rider;
			}
		}
		return null;
	}
	
	/**
	 * Calculates the current mass of the vehicle.
	 * Includes core mass, player weight (including inventory), and cargo.
	 */
	protected float getCurrentMass(){
		int currentMass = definition.motorized.emptyMass;
		for(APart part : parts){
			if(part instanceof PartInteractable){
				currentMass += ((PartInteractable) part).getInventoryWeight();
			}
		}
		
		//Add passenger inventory mass as well.
		for(WrapperEntity rider : locationRiderMap.values()){
			if(rider instanceof WrapperPlayer){
				currentMass += 100 + ((WrapperPlayer) rider).getInventory().getInventoryWeight(ConfigSystem.configObject.general.itemWeights.weights);
			}else{
				currentMass += 100;
			}
		}
		return currentMass;
	}
}
