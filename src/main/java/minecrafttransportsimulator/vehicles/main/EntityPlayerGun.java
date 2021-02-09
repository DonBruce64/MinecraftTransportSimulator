package minecrafttransportsimulator.vehicles.main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.AEntityB_Existing;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Gun;
import minecrafttransportsimulator.baseclasses.IGunProvider;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONSound;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketGunChange;
import minecrafttransportsimulator.packets.instances.PacketPlayerGunChange;
import minecrafttransportsimulator.packets.instances.PacketPlayerGunFiring;
import minecrafttransportsimulator.rendering.instances.AnimationsGun;
import minecrafttransportsimulator.rendering.instances.RenderPlayerGun;
import net.minecraft.item.ItemStack;

/**Entity class responsible for storing and syncing information about the current gun
 * any player is holding.  This entity will render the held gun, if it exists, as well as
 * spawn any bullets the gun is told to fire.  The current item the player is holding is
 * stored, and whenever the player either changes this item, or stops firing, the data
 * is saved back to that item to ensure that the gun's state is maintained.
 *  
 * @author don_bruce
 */
public class EntityPlayerGun extends AEntityB_Existing implements IGunProvider{
	public static final Map<String, EntityPlayerGun> playerServerGuns = new HashMap<String, EntityPlayerGun>();
	public static final Map<String, EntityPlayerGun> playerClientGuns = new HashMap<String, EntityPlayerGun>();
	
	public final WrapperPlayer player;
	private int ticksOnGun;
	private int hotbarSelected = -1;
	private ItemStack gunStack;
	private ItemPart gunItem;
	public Gun gun;
	public boolean fireCommand;
	public String currentSubName;
	
	private static final AnimationsGun animator = new AnimationsGun();
	
	public EntityPlayerGun(WrapperWorld world, WrapperEntity wrapper, WrapperPlayer playerSpawning, WrapperNBT data){
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
			WrapperPlayer foundPlayer = null;
			for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 10, 10, 10))){
				if(entity instanceof WrapperPlayer){
					if(((WrapperPlayer) entity).getUUID().equals(playerUUID)){
						foundPlayer = (WrapperPlayer) entity;
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
					saveGun(true);
					fireCommand = false;
					InterfacePacket.sendToAllClients(new PacketPlayerGunFiring(this, false));
				}
				
				//If we don't have a gun yet, try to get the current one if the player is holding one.
				if(gun == null){
					AItemBase heldItem = player.getHeldItem();
					if(heldItem instanceof ItemPart){
						ItemPart heldPart = (ItemPart) heldItem;
						if(heldPart.isHandHeldGun() && !world.isClient()){
							if(++ticksOnGun == 5){
								createNewGun(-1);
								InterfacePacket.sendToAllClients(new PacketPlayerGunChange(this, gun.gunID));
								ticksOnGun = 0;
							}
						}
					}
				}
				
				//If we have a gun, do updates to it.
				//Only change firing command on servers to prevent de-syncs.
				//Packets will get sent to clients to change them.
				if(gun != null){
					//Set our position relative to the the player's hand.
					//Center point is at the player's arm, with offset being where the offset is.
					Point3d heldVector;
					if(player.isSneaking()){
						heldVector = gunItem.definition.gun.handHeldAimedOffset;
					}else{
						heldVector = gunItem.definition.gun.handHeldNormalOffset;
					}
					angles.set(player.getPitch(), player.getHeadYaw(), 0);
					
					//Arm center is 0.3125 blocks away in X, 1.375 blocks up in Y.
					//Sneaking lowers arm by 0.2 blocks.
					//First rotate point based on pitch.  This is for only the arm movement.
					Point3d armRotation = new Point3d(angles.x, 0, 0);
					position.setTo(heldVector).rotateFine(armRotation);
					
					//Now rotate based on player yaw.  We need to take the arm offset into account here.
					armRotation.set(0, angles.y, 0);
					position.add(-0.3125, 0, 0).rotateFine(armRotation);
					
					//Now add the player's position and model center point offsets.
					position.add(player.getPosition()).add(0, player.isSneaking() ? 1.3125 - 0.2 : 1.3125, 0);
					
					//If the player is riding something, add to our position the vehicle's motion.
					//This is because the player gets moved with the vehicle.
					if(player.getEntityRiding() != null){
						position.add(player.getEntityRiding().motion.copy().multiply(EntityVehicleF_Physics.SPEED_FACTOR));
					}
					
					if(fireCommand && !gun.firing && !world.isClient()){
						gun.firing = true;
						InterfacePacket.sendToAllClients(new PacketGunChange(gun, true));
					}else if(!fireCommand && gun.firing && !world.isClient()){
						gun.firing = false;
						InterfacePacket.sendToAllClients(new PacketGunChange(gun, false));
						saveGun(false);
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
		gunItem = (ItemPart) player.getHeldItem();
		currentSubName = gunItem.subName;
		
		WrapperNBT data = new WrapperNBT(gunStack);
		if(optionalGunID != -1){
			data.setInteger("gunID", optionalGunID);
		}
		gun = new Gun(this, gunItem.definition, 0, 0, 0, 0, data);
		gun.firing = fireCommand;
		hotbarSelected = player.getHotbarIndex();
	}
	
	private void saveGun(boolean delete){
		WrapperNBT data = new WrapperNBT();
		gun.save(data);
		gunStack.setTagCompound(data.tag);
		if(delete){
			gun = null;
		}
	}
	
	
	//----------START OF GUN INTERFACE CODE----------
	@Override
	public void reloadGunBullets(){
		if(gunItem.definition.gun.autoReload || gun.bulletsLeft == 0){
			//Check the player's inventory for bullets.
			WrapperInventory inventory = player.getInventory();
			for(int i=0; i<inventory.getSize(); ++i){
				AItemBase item = inventory.getItemInSlot(i);
				if(item instanceof ItemPart){
					if(gun.tryToReload((ItemPart) item)){
						//Bullet is right type, and we can fit it.  Remove from player's inventory and add to the gun.
						inventory.decrementSlot(i);
						return;
					}
				}
			}
		}
	}

	@Override
	public WrapperEntity getController(){
		return player;
	}

	@Override
	public boolean isGunActive(WrapperEntity controller){
		return true;
	}

	@Override
	public double getDesiredYaw(WrapperEntity controller){
		return 0;
	}

	@Override
	public double getDesiredPitch(WrapperEntity controller){
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
	public List<JSONSound> getSoundDefinitions(){
		return gun != null ? gun.getSoundDefinitions() : null;
	}
	
	//----------START OF RENDERING INTERFACE CODE----------
	@Override
    public AnimationsGun getAnimationSystem(){
		return animator;
	}
	
	@Override
	public void orientToProvider(Point3d point){
		point.rotateFine(angles);
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
		return false;
	}

	@Override
	public void render(float partialTicks){
		RenderPlayerGun.render(this, partialTicks);
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		if(player != null){
			data.setString("playerUUID", player.getUUID());
		}
		if(gun != null){
			gun.save(data);
		}
	}
}
