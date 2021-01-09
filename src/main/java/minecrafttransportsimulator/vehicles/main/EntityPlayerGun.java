package minecrafttransportsimulator.vehicles.main;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Gun;
import minecrafttransportsimulator.baseclasses.IGunProvider;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.packets.components.NetworkSystem;
import minecrafttransportsimulator.packets.instances.PacketGunChange;
import minecrafttransportsimulator.packets.instances.PacketPlayerGunChange;
import minecrafttransportsimulator.rendering.components.AnimationsGun;
import minecrafttransportsimulator.rendering.instances.RenderPlayerGun;
import minecrafttransportsimulator.sound.SoundInstance;

/**Entity class responsible for storing and syncing information about the current gun
 * any player is holding.  This entity will render the held gun, if it exists, as well as
 * spawn any bullets the gun is told to fire.  The current item the player is holding is
 * stored, and whenever the player either changes this item, or stops firing, the data
 * is saved back to that item to ensure that the gun's state is maintained.
 *  
 * @author don_bruce
 */
public class EntityPlayerGun extends AEntityBase implements IGunProvider{
	public static final Map<String, EntityPlayerGun> playerServerGuns = new HashMap<String, EntityPlayerGun>();
	public static final Map<String, EntityPlayerGun> playerClientGuns = new HashMap<String, EntityPlayerGun>();
	
	public final IWrapperPlayer player;
	public int hotbarSelected = -1;
	public IWrapperItemStack gunStack;
	public ItemPart gunItem;
	public Gun gun;
	public boolean fireCommand;
	
	private static final AnimationsGun animator = new AnimationsGun();
	
	public EntityPlayerGun(IWrapperWorld world, IWrapperEntity wrapper, IWrapperPlayer playerSpawning, WrapperNBT data){
		super(world, wrapper, data);
		if(playerSpawning != null){
			//Newly-spawned entity.
			this.player = playerSpawning;
			position.setTo(player.getPosition());
			rotation.set(player.getPitch(), player.getHeadYaw(), 0);
		}else{
			//Saved entity.  Either on the server or client.
			//Get player via saved NBT.  If the player isn't found, we're not valid.
			String playerUUID = data.getString("playerUUID");
			IWrapperPlayer foundPlayer = null;
			for(IWrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 10, 10, 10))){
				if(entity instanceof IWrapperPlayer){
					if(((IWrapperPlayer) entity).getUUID().equals(playerUUID)){
						foundPlayer = (IWrapperPlayer) entity;
						break;
					}
				}
			}
			if(foundPlayer != null){
				this.player = foundPlayer;
			}else{
				this.player = null;
				isValid = false;
				if(!world.isClient()){
					playerServerGuns.remove(playerUUID);
				}
				return;
			}
		}
		
		hotbarSelected = player.getHotbarIndex();
		if(world.isClient()){
			playerClientGuns.put(player.getUUID(), this);
		}else{
			playerServerGuns.put(player.getUUID(), this);
		}
	}
	
	@Override
	public void update(){
		super.update();
		//Need to make sure we have the player.
		//We might be invalid here and haven't had the system report this.
		if(player != null){
			//Make sure player is still valid and haven't left the server.
			if(player.isValid()){
				//Set our position to the player's position.  We may update this later if we have a gun.
				position.setTo(player.getPosition());
				motion.setTo(player.getVelocity());
				
				//Check to make sure if we had a gun, that it didn't change.
				if(gun != null && (!gunItem.equals(player.getHeldItem()) || hotbarSelected != player.getHotbarIndex())){
					WrapperNBT data = gunStack.getData();
					gun.save(data);
					gunStack.setData(data);
					gun = null;
				}
				
				//If we don't have a gun yet, try to get the current one if the player is holding one.
				if(gun == null){
					AItemBase heldItem = player.getHeldItem();
					if(heldItem instanceof ItemPart && ((ItemPart) heldItem).isHandHeldGun()){
						if(!world.isClient()){
							createNewGun(-1);
							NetworkSystem.sendToAllClients(new PacketPlayerGunChange(this));
						}
					}
				}
				
				//If we have a gun, do updates to it.
				//Only change firing command on servers to prevent de-syncs.
				//Packets will get sent to clients to change them.
				if(gun != null){
					//Set our position the player's hands.
					if(player.isSneaking()){
						//Set to scope center.
						angles.set(player.getPitch(), player.getHeadYaw(), 0);
						position.setTo(gunItem.definition.gun.handHeldAimedOffset).rotateFine(angles).add(player.getPosition()).add(0, player.getEyeHeight(), 0);
					}else{
						//Set to hand.
						angles.set(player.getPitch(), player.getHeadYaw(), 0);
						position.setTo(gunItem.definition.gun.handHeldNormalOffset).rotateFine(angles).add(player.getPosition()).add(0, player.getEyeHeight(), 0);
					}
					
					
					if(fireCommand && !gun.firing && !world.isClient()){
						gun.firing = true;
						NetworkSystem.sendToAllClients(new PacketGunChange(gun, true));
					}else if(!fireCommand && gun.firing && !world.isClient()){
						gun.firing = false;
						NetworkSystem.sendToAllClients(new PacketGunChange(gun, false));
						//Also save data to the item.
						WrapperNBT data = gunStack.getData();
						gun.save(data);
						gunStack.setData(data);
					}
					gun.update();
				}
			}else{
				isValid = false;
			}
		}
	}
	
	public void createNewGun(int optionalGunID){
		gunStack = player.getHeldStack();
		gunItem = (ItemPart) gunStack.getItem();
		WrapperNBT data = gunStack.getData();
		if(optionalGunID != -1){
			data.setInteger("gunID", optionalGunID);
		}
		gun = new Gun(this, gunItem.definition, 0, 0, 0, 0, data);
		hotbarSelected = player.getHotbarIndex();
	}
	
	
	//----------START OF GUN INTERFACE CODE----------
	@Override
	public void reloadGunBullets(){
		if(gunItem.definition.gun.autoReload || gun.bulletsLeft == 0){
			//Check the player's inventory for bullets.
			IWrapperInventory inventory = player.getInventory();
			for(int i=0; i<inventory.getSize(); ++i){
				AItemBase item = inventory.getItemInSlot(i);
				if(item instanceof ItemPart){
					if(gun.tryToReload((ItemPart) item)){
						//Bullet is right type, and we can fit it.  Remove from crate and add to the gun.
						//Return here to ensure we don't set the loadedBullet to blank since we found bullets.
						inventory.decrementSlot(i);
						return;
					}
				}
			}
		}
	}

	@Override
	public IWrapperEntity getController(){
		return player;
	}

	@Override
	public boolean isGunActive(IWrapperEntity controller){
		return true;
	}

	@Override
	public double getDesiredYaw(IWrapperEntity controller){
		return 0;
	}

	@Override
	public double getDesiredPitch(IWrapperEntity controller){
		return 0;
	}

	@Override
	public int getGunNumber(){
		return 1;
	}

	@Override
	public int getTotalGuns(){
		return 1;
	}
	
	@Override
	public void spawnParticles(){
		if(gun != null){
			gun.spawnParticles();
		}
	}
	
	
	//----------START OF SOUND INTERFACE CODE----------
	
	@Override
	public void startSounds(){
		if(gun != null){
			gun.startSounds();
		}
	}

	@Override
	public void updateProviderSound(SoundInstance sound){
		if(gun != null){
			gun.startSounds();
		}
	}
	
	//----------START OF RENDERING INTERFACE CODE----------
	@Override
    public AnimationsGun getAnimationSystem(){
		return animator;
	}
	
	@Override
	public Point3d getProviderRotation(){
		return angles;
	}
	
	@Override
	public Point3d getProviderVelocity(){
		return motion;
	}

	@Override
	public float getLightPower(){
		return 1;
	}

	@Override
	public boolean isLitUp(){
		return true;
	}

	@Override
	public void render(float partialTicks){
		RenderPlayerGun.render(this, partialTicks);
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		data.setString("playerUUID", player.getUUID());
		if(gun != null){
			gun.save(data);
		}
	}
}
