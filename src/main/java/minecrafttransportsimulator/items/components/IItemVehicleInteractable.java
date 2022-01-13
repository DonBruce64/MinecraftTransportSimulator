package minecrafttransportsimulator.items.components;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable.PlayerOwnerState;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketVehicleInteract;

/**Interface that performs an action on vehicles.  The methods in here will be called 
 * from {@link PacketVehicleInteract} on the server when a player clicks a vehicle on a client.
 * 
 * @author don_bruce
 */
public interface IItemVehicleInteractable{
	
	/**
	 *  Performs interaction on the vehicle.  Dependent on the state of the passed-in vehicle.
	 *  Also passes-in a flag that determines if the player is allowed to edit the vehicle's state.
	 *  Is a combination of owner name and if the player is OP.  This method is initially called on the
	 *  server when the server gets a packet that the player has interacted with a vehicle.  Actions should
	 *  be taken at this point.
	 *  {@link CallbackType#PLAYER} will send this interaction back to the player  that initiated it, while 
	 *  {@link CallbackType#ALL} will send this to all players that have this vehicle loaded.
	 *  {@link CallbackType#NONE} will abort processing and will prevent any packets from being sent.
	 *  {@link CallbackType#SKIP} passes over this item an allow for further vehicle processing.  Useful for items
	 *  that may only be active sometimes  based on their state, and shouldn't impede other vehicle interactions.
	 *  {@link CallbackType#ALL_AND_MORE} will return the packet like ALL, but will also continue processing like SKIP.
	 *  <br><br>
	 *  NOTE: When this method is called on the server, the passed-in player is the player clicking the vehicle.
	 *  However, when the callback packet is sent to trigger this method on clients, the player instance is the
	 *  client player for that client, NOT the player who initially interacted with the vehicle.  Because of 
	 *  this, any client-side interactions that need to know something about the player who initially interacted 
	 *  with this vehicle should NOT use {@link CallbackType#ALL}, as this will not give the "correct" player instance.
	 */
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, BoundingBox hitBox, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick);
	
	public static enum CallbackType{
		NONE,
		PLAYER,
		ALL,
		ALL_AND_MORE,
		SKIP;
	}
}
