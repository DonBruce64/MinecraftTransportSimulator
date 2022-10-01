package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.packets.components.APacketBase;

/**
 * IWrapper for the player entity class.  This class wraps the player into a more
 * friendly instance that allows for common operations, like checking if the player
 * has an item, checking if they are OP, etc.  Also prevents the need to interact
 * with the class directly, which allows for abstraction in the code.
 *
 * @author don_bruce
 */
public interface IWrapperPlayer extends IWrapperEntity {

    /**
     * Returns true if this player is OP.  Will always return true on single-player worlds.
     */
    boolean isOP();

    /**
     * Displays the passed-in chat message to the player.
     * Arguments will be substituted into the string as applicable.
     */
    void displayChatMessage(LanguageEntry language, Object... args);

    /**
     * Returns true if this player is in creative mode.
     */
    boolean isCreative();

    /**
     * Returns true if this player is in spectator mode.
     */
    boolean isSpectator();

    /**
     * Returns true if this player is sneaking.
     */
    boolean isSneaking();

    /**
     * Gets the currently-leashed entity for this player, or null if it doesn't exist.
     */
    IWrapperEntity getLeashedEntity();

    /**
     * Returns true if the player is holding the pack-item type passed-in.
     */
    boolean isHoldingItemType(ItemComponentType type);

    /**
     * Returns the held item.  Only valid for {@link AItemBase} items.
     * This is less RAM-intensive than {@link #getHeldStack()} due to
     * not needing to make a new IWrapper during the call.  So if you only
     * need to know the item the player is holding, you should use this method
     * rather than getting the item out of the stack.  Of course, if you already
     * have a stack, just use that.
     */
    AItemBase getHeldItem();

    /**
     * Returns the held stack.
     */
    IWrapperItemStack getHeldStack();

    /**
     * Sets the held stack.  Overwrites what was in the hand before this.
     */
    void setHeldStack(IWrapperItemStack stack);

    /**
     * Gets the index of the held stack in the hotbar.
     * This corresponds to the inventory slot the item is in.
     */
    int getHotbarIndex();

    /**
     * Gets the inventory of the player.
     */
    IWrapperInventory getInventory();

    /**
     * Sends a packet to this player over the network.
     * Convenience method so we don't need to call the
     * {@link InterfaceManager.packetInterface} for player-specific packets.
     * Note that this may ONLY be called on the server, as
     * clients don't know about other player's network pipelines.
     */
    void sendPacket(APacketBase packet);

    /**
     * Opens the crafting table GUI.  This overrides the normal GUI opened
     * when a block is clicked, which allows players to open a GUI by clicking
     * an entity instead.  Required as normally MC checks if there is a block
     * present in the internal code, which automatically closes the GUI.
     */
    void openCraftingGUI();
}