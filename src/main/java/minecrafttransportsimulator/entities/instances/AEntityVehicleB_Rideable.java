package minecrafttransportsimulator.entities.instances;

import java.util.Iterator;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityG_Towable;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIHUD;
import minecrafttransportsimulator.guis.instances.GUIPanelAircraft;
import minecrafttransportsimulator.guis.instances.GUIPanelGround;
import minecrafttransportsimulator.guis.instances.GUIRadio;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceInput;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;


/**This is the first vehicle class level.  This class sits on top of the base
 * entity class, and overrides many of the base entity class functions
 * to add greater flexibility to the riding systems.  This allows for riders to
 * change their position and rotation based on what seat they are currently in.
 * 
 * @author don_bruce
 */
abstract class AEntityVehicleB_Rideable extends AEntityG_Towable<JSONVehicle>{
	public static boolean lockCameraToMovement = true;
	
	/**Cached value for speedFactor.  Saves us from having to use the long form all over.*/
	public final double speedFactor;
	
	public AEntityVehicleB_Rideable(WrapperWorld world, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, placingPlayer, data);
		this.speedFactor = (definition.motorized.isAircraft ? ConfigSystem.configObject.general.aircraftSpeedFactor.value : ConfigSystem.configObject.general.carSpeedFactor.value)*ConfigSystem.configObject.general.packSpeedFactors.value.get(definition.packID);
	}
	
	@Override
	public double getMass(){
		return super.getMass() + definition.motorized.emptyMass;
	}
	
	@Override
	public void updateRider(WrapperEntity rider, Iterator<WrapperEntity> iterator){
		//We override the default rider update behavior here as the riders can move depending
		//on how the part they are riding moves.  If we modified the rider position, then we'd
		//allow for multiple riders at the same position.  That's Bad Stuff.
		//Update rider positions based on the location they are set to.
		if(rider.isValid()){
			PartSeat seat = getSeatForRider(rider);
			
			//Add all vehicle-wide effects to the rider
			if(this.definition.effects != null) {
				for(JSONPotionEffect effect: this.definition.effects){
					rider.addPotionEffect(effect);
				}
			}
			
			//Add all seat-specific effects to the rider
			if(seat.placementDefinition.seatEffects != null) {
				for(JSONPotionEffect effect: seat.placementDefinition.seatEffects){
					rider.addPotionEffect(effect);
				}
			}
			
			//Set rider to the Y position of the seat.
			//This assumes their bottom is where the seat's position is.
			//For animals, it will be their feet.  For players, their literal bottom (unless the seat is standing type).
			//Also set motion here, as that's used in other places.
			rider.setPosition(seat.position, false);
			rider.setVelocity(motion);
			
			//If we are on the client, and the rider is the main client player, check controls.
			//If the seat is a controller, and we have mouseYoke enabled, and our view is locked disable the mouse from MC.            	
            //We also need to make sure the player in this event is the actual client player.  If we are on a server,
            //another player could be getting us to this logic point and thus we'd be making their inputs in the vehicle.
			if(world.isClient() && !InterfaceClient.isChatOpen() && rider.equals(InterfaceClient.getClientPlayer())){
    			ControlSystem.controlVehicle((EntityVehicleF_Physics) this, seat.placementDefinition.isController);
    			InterfaceInput.setMouseEnabled(!(seat.placementDefinition.isController && ConfigSystem.configObject.clientControls.mouseYoke.value && lockCameraToMovement));
    		}
		}else{
			//Remove invalid rider.
			removeRider(rider, iterator);
		}
	}
	
	@Override
	public boolean addRider(WrapperEntity rider, Point3D riderLocation){
		//We override the default rider addition behavior here as we need to rotate
		//riders to face forwards in seats that they start riding in.
		//Check if this rider is already riding this vehicle.
		boolean riderAlreadyInSeat = locationRiderMap.containsValue(rider);
		boolean success = super.addRider(rider, riderLocation);
		
		if(success){
			//If we weren't riding before, set the player's yaw to the same yaw as the vehicle.
			//We do this to ensure we don't have 360+ rotations to deal with.
			//Need to invert the lookup as location may be null from the builder.
			//Rider won't be, as it's required, so we can use it to get the actual location.
			PartSeat sittingSeat = getSeatForRider(rider);
			if(!riderAlreadyInSeat){
				//Set pitch and yaw to 0 as we need to align with the seat.
				rider.setYaw(0);
				rider.setPitch(0);
			}else{
				//Clear out the panel if we're not in a controller seat.
				if(world.isClient() && InterfaceClient.getClientPlayer().equals(rider)){
					if(definition.motorized.isAircraft){
						AGUIBase.closeIfOpen(GUIPanelAircraft.class);
					}else{
						AGUIBase.closeIfOpen(GUIPanelGround.class);
					}
				}
				//Close the HUD so we don't make two of them later.
				AGUIBase.closeIfOpen(GUIHUD.class);
			}
			//Open HUD if seat is controller. 
			if(world.isClient() && InterfaceClient.getClientPlayer().equals(rider)){
				new GUIHUD((EntityVehicleF_Physics) this, sittingSeat);
			}
		}
		
		return success;
	}
	
	@Override
	public void removeRider(WrapperEntity rider, Iterator<WrapperEntity> iterator){
		//We override the default rider removal behavior here as the dismount position
		//of riders can be modified via JSON or via part placement location.
		//Get the position the rider was sitting in before we dismount them.
		PartSeat seat = getSeatForRider(rider);;
		super.removeRider(rider, iterator);

		//Get rid of any potion effects that were caused by the vehicle
		if(this.definition.effects != null) {
			for(JSONPotionEffect effect: this.definition.effects){
				rider.removePotionEffect(effect);
			}
		}
		
		//Get rid of any potion effects that were caused by the seat
		if(seat.placementDefinition.seatEffects != null) {
			for(JSONPotionEffect effect: seat.placementDefinition.seatEffects){
				rider.removePotionEffect(effect);
			}
		}
		
		//Set the rider dismount position.
		//If we have a dismount position in the JSON.  Use it.
		//Otherwise, put us to the right or left of the seat depending on x-offset.
		//Make sure to take into the movement of the seat we were riding if it had moved.
		//This ensures the dismount moves with the seat.
		Point3D dismountPosition;
		if(seat.placementDefinition.dismountPos != null){
			dismountPosition = seat.placementDefinition.dismountPos.copy();
		}else{
			dismountPosition = seat.localOffset.copy();
			if(seat.placementOffset.x < 0){
				dismountPosition.add(-2D, 0D, 0D);
			}else{
				dismountPosition.add(2D, 0D, 0D);
			}
		}
		dismountPosition.rotate(orientation).add(position);
		rider.setPosition(dismountPosition, false);
		rider.setOrientation(orientation);
		
		//If we are on the client, disable mouse-yoke blocking.
		if(world.isClient() && InterfaceClient.getClientPlayer().equals(rider)){
			//Client player is the one that left the vehicle.  Make sure they don't have their mouse locked or a GUI open.
			InterfaceInput.setMouseEnabled(true);
			if(definition.motorized.isAircraft){
				AGUIBase.closeIfOpen(GUIPanelAircraft.class);
			}else{
				AGUIBase.closeIfOpen(GUIPanelGround.class);
			}
			AGUIBase.closeIfOpen(GUIHUD.class);
			AGUIBase.closeIfOpen(GUIRadio.class);
		}
	}
}
