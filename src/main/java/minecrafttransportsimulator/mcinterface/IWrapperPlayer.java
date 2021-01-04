package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.baseclasses.Gun;
import minecrafttransportsimulator.baseclasses.IGunProvider;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.rendering.components.IParticleProvider;
import minecrafttransportsimulator.sound.SoundInstance;

/**Wrapper for the player entity class.  This class wraps the player into a more
 * friendly instance that allows for common operations, like checking if the player
 * has an item, checking if they are OP, etc.  Also prevents the need to interact
 * with the class directly, which allows for abstraction in the code.
 *
 * @author don_bruce
 */
public interface IWrapperPlayer extends IWrapperEntity, IGunProvider, IParticleProvider{
	
	/**
	 *  Returns the player's global UUID.  This is an ID that's unique to every player on Minecraft.
	 *  Useful for assigning ownership where the entity ID of a player might change between sessions.
	 *  <br><br>
	 *  NOTE: While this ID isn't supposed to change, some systems WILL, in fact, change it.  Cracked
	 *  servers, and the nastiest of Bukkit systems will deliberately change the UUID of players, which,
	 *  when combined with their changing of entity IDs, makes server-client lookup impossible.
	 */
	public String getUUID();

	/**
	 *  Returns true if this player is OP.  Will always return true on single-player worlds.
	 */
	public boolean isOP();
	
	/**
	 *  Displays the passed-in chat message to the player.  This interface assumes that the message is
	 *  untranslated and will attempt to translate it prior to display.  Should this fail, the
	 *  raw message will be displayed.
	 */
	public void displayChatMessage(String message);
	
	/**
	 *  Returns true if this player is in creative mode.
	 */
	public boolean isCreative();
	
	/**
	 *  Returns true if this player is sneaking.
	 */
	public boolean isSneaking();
	
	/**
	 *  Gets the currently-leashed entity for this player, or null if it doesn't exist.
	 */
	public IWrapperEntity getLeashedEntity();
	
	/**
	 *  Returns the held item.  Only valid for {@link AItemBase} items.
	 */
	public AItemBase getHeldItem();
	
	/**
	 *  Returns the held stack as a wrapper.
	 */
	public IWrapperItemStack getHeldStack();
	
	/**
	 *  Gets the inventory of the player.
	 */
	public IWrapperInventory getInventory();
	
	/**
	 *  Sends a packet to this player over the network.
	 *  Convenience method so we don't need to call the
	 *  {@link IInterfaceNetwork} for player-specific packets.
	 */
	public void sendPacket(APacketBase packet);
	
	/**
	 *  Opens the crafting table GUI.  This overrides the normal GUI opened
	 *  when a block is clicked, which allows players to open a GUI by clicking
	 *  an entity instead.  Required as normally MC checks if there is a block
	 *  present in the internal code, which automatically closes the GUI.
	 */
	public void openCraftingGUI();
	
	/**
	 *  Opens the GUI for the passed-in TE, or fails to open any GUI if the TE doesn't have one.
	 *  Actual validity of the GUI being open is left to the TE implementation.
	 *  Note: This method is for any TE that has inventory.  This includes, but is not limited to,
	 *  chests, furnaces, and brewing stands.
	 */
	public void openTileEntityGUI(IWrapperTileEntity tile);
	
	
	//----------START OF GUN SOUND AND FUNCTION CODE----------
	@Override
	public default Point3d getProviderPosition(){
		return getPosition().add(0, getEyeHeight(), 0);
	}
	
	@Override
	public default Point3d getProviderVelocity(){
		return getVelocity();
	}

	@Override
	public default Point3d getProviderRotation(){
		return new Point3d(getPitch(), getHeadYaw(), 0);
	}
	
	@Override
	public default IWrapperWorld getProviderWorld(){
		return getWorld();
	}
	
	@Override
	public default void reloadGunBullets(){
		//Check the player's inventory for bullets.
		IWrapperInventory inventory = getInventory();
		for(int i=0; i<inventory.getSize(); ++i){
			AItemBase item = inventory.getItemInSlot(i);
			if(item instanceof ItemPart){
				if(ItemPart.getGunForPlayer(this).tryToReload((ItemPart) item)){
					//Bullet is right type, and we can fit it.  Remove from crate and add to the gun.
					//Return here to ensure we don't set the loadedBullet to blank since we found bullets.
					inventory.decrementSlot(i);
					return;
				}
			}
		}
	}

	@Override
	public default IWrapperEntity getController(){
		return this;
	}
	
	@Override
	public default boolean isGunActive(IWrapperEntity controller){
		return true;
	}
	
	@Override
	public default double getDesiredYaw(IWrapperEntity controller){
		return 0;
	}
	
	@Override
	public default double getDesiredPitch(IWrapperEntity controller){
		return 0;
	}

	@Override
	public default int getGunNumber(){
		return 1;
	}
	
	@Override
	public default int getTotalGuns(){
		return 1;
	}
	
	@Override
	public default void updateProviderSound(SoundInstance sound){
		Gun gun = ItemPart.getGunForPlayer(this);
		if(gun != null){
			gun.updateProviderSound(sound);
		}
	}
	
	@Override
	public default void startSounds(){
		Gun gun = ItemPart.getGunForPlayer(this);
		if(gun != null){
			gun.startSounds();
		}
	}
	
	@Override
	public default void spawnParticles(){
		//Forward calls to the gun.
		Gun gun = ItemPart.getGunForPlayer(this);
		if(gun != null){
			gun.spawnParticles();
		}
	}
}