package minecrafttransportsimulator.items.components;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable.PlayerOwnerState;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketEntityInteract;

/**
 * Interface that performs an action on vehicles.  The methods in here will be called
 * from {@link PacketEntityInteract} on the server when a player clicks a vehicle on a client.
 *
 * @author don_bruce
 */
public interface IItemEntityInteractable {

    /**
     * Performs interaction on the entity.  Dependent on the state of the passed-in entity.
     * Also passes-in a flag that determines if the player is allowed to edit the entity's state.
     * Is a combination of owner name and if the player is OP.  This method is initially called on the
     * server when the server gets a packet that the player has interacted with a entity.  Actions should
     * be taken at this point.
     * {@link CallbackType#PLAYER} will send this interaction back to the player  that initiated it, while
     * {@link CallbackType#ALL} will send this to all players that have this entity loaded.
     * {@link CallbackType#NONE} will abort processing and will prevent any packets from being sent.
     * {@link CallbackType#SKIP} passes over this item an allow for further entity processing.  Useful for items
     * that may only be active sometimes  based on their state, and shouldn't impede other entity interactions.
     * <br><br>
     * NOTE: When this method is called on the server, the passed-in player is the player clicking the entity.
     * However, when the callback packet is sent to trigger this method on clients, the player instance is the
     * client player for that client, NOT the player who initially interacted with the entity.  Because of
     * this, any client-side interactions that need to know something about the player who initially interacted
     * with this entity should NOT use {@link CallbackType#ALL}, as this will not give the "correct" player instance.
     */
    CallbackType doEntityInteraction(AEntityE_Interactable<?> entity, BoundingBox hitBox, IWrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick);

    enum CallbackType {
        NONE,
        PLAYER,
        ALL,
        SKIP
    }
}
