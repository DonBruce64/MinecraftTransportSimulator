package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.PartGun.GunState;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONPlayerGun;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.instances.RenderPlayerGun;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.item.ItemStack;

/**Entity class responsible for storing and syncing information about the current gun
 * any player is holding.  This entity will trigger rendering of the held gun, if it exists.
 * The current item the player is holding is stored, and whenever the player either changes 
 * this item, or stops firing, the data is saved back to that item to ensure that the gun's 
 * state is maintained.
 *  
 * @author don_bruce
 */
public class EntityPlayerGun extends AEntityE_Multipart<JSONPlayerGun>{
	public static final Map<String, EntityPlayerGun> playerClientGuns = new HashMap<String, EntityPlayerGun>();
	public static final Map<String, EntityPlayerGun> playerServerGuns = new HashMap<String, EntityPlayerGun>();
	
	public final WrapperPlayer player;
	private int hotbarSelected = -1;
	private ItemStack gunStack;
	private boolean didGunFireLastTick;
	public PartGun activeGun;
	
	private static RenderPlayerGun renderer;
	
	public EntityPlayerGun(WrapperWorld world, WrapperPlayer playerSpawning, WrapperNBT data){
		super(world, data);
		//Get the player spawning us.
		if(playerSpawning != null){
			//Newly-spawned entity.
			this.player = playerSpawning;
			position.setTo(player.getPosition());
			prevPosition.setTo(position);
			angles.set(player.getPitch(), player.getHeadYaw(), 0);
			prevAngles.setTo(angles);
		}else{
			//Saved entity.  Either on the server or client.
			//Get player via saved NBT.  If the player isn't found, we're not valid.
			String playerUUID = data.getString("playerUUID");
			WrapperPlayer foundPlayer = null;
			for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))){
				if(entity instanceof WrapperPlayer){
					if(((WrapperPlayer) entity).getID().equals(playerUUID)){
						foundPlayer = (WrapperPlayer) entity;
						break;
					}
				}
			}
			if(foundPlayer != null){
				this.player = foundPlayer;
			}else{
				this.player = null;
				remove();
				return;
			}
		}
		

		//Don't load duplicates.  However, do load saved guns.
		//These come after the player joins the world, so replace them.
		if(world.isClient()){
			if(playerClientGuns.containsKey(player.getID())){
				playerClientGuns.get(player.getID()).remove();
			}
			playerClientGuns.put(player.getID(), this);
		}else{
			if(playerServerGuns.containsKey(player.getID())){
				playerServerGuns.get(player.getID()).remove();
			}
			playerServerGuns.put(player.getID(), this);
		}
	}
	
	@Override
	public JSONPlayerGun generateDefaultDefinition(){
		JSONPlayerGun defaultDefinition = new JSONPlayerGun();
		defaultDefinition.packID = "dummy";
		defaultDefinition.systemName = "dummy";
		
		JSONPartDefinition fakeDef = new JSONPartDefinition();
		fakeDef.pos = new Point3d();
		fakeDef.rot = new Point3d();
		fakeDef.types = new ArrayList<String>();
		//Look though all gun types and add them.
		for(AItemPack<?> packItem : PackParserSystem.getAllPackItems()){
			if(packItem instanceof ItemPartGun){
				ItemPartGun gunItem = (ItemPartGun) packItem;
				if(gunItem.definition.gun.handHeld){
					if(!fakeDef.types.contains(gunItem.definition.generic.type)){
						fakeDef.types.add(gunItem.definition.generic.type);
					}
				}
			}
		}
		
		fakeDef.maxValue = Float.MAX_VALUE;
		defaultDefinition.parts = new ArrayList<JSONPartDefinition>();
		defaultDefinition.parts.add(fakeDef);
		return defaultDefinition;
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			//Make sure player is still valid and haven't left the server.
			if(player != null && player.isValid()){
				//Set our position to the player's position.  We may update this later if we have a gun.
				position.setTo(player.getPosition());
				motion.setTo(player.getVelocity());
				
				//Get the current gun.
				activeGun = parts.isEmpty() ? null : (PartGun) parts.get(0);
				
				//If we have a gun, but the player's held stack is null, get it now.
				//This happens if we load a gun as a saved part.
				if(activeGun != null && gunStack == null){
					AItemBase heldItem = player.getHeldItem();
					if(heldItem instanceof ItemPartGun){
						ItemPartGun heldGun = (ItemPartGun) heldItem;
						if(heldGun.definition.gun.handHeld){
							gunStack = player.getHeldStack();
							hotbarSelected = player.getHotbarIndex();
						}
					}
					if(activeGun != null && gunStack == null){
						//Either the player's held item changed, or the pack did.
						//Held gun is invalid, so don't use or save it.
						removePart(activeGun, null);
						activeGun = null;
					}
				}
				
				if(!world.isClient()){
					//Check to make sure if we had a gun, that it didn't change.
					if(activeGun != null && (!activeGun.getItem().equals(player.getHeldItem()) || hotbarSelected != player.getHotbarIndex())){
						saveGun(true);
					}
					
					//If we don't have a gun yet, try to get the current one if the player is holding one.
					if(activeGun == null){
						AItemBase heldItem = player.getHeldItem();
						if(heldItem instanceof ItemPartGun){
							ItemPartGun heldGun = (ItemPartGun) heldItem;
							if(heldGun.definition.gun.handHeld){
								gunStack = player.getHeldStack();
								addPartFromItem(heldGun, new WrapperNBT(gunStack), new Point3d(), false);
								hotbarSelected = player.getHotbarIndex();
							}
						}
					}
				}
				
				//If we have a gun, do updates to it.
				//Only change firing command on servers to prevent de-syncs.
				//Packets will get sent to clients to change them.
				if(activeGun != null){
					//Set our position relative to the the player's hand.
					//Center point is at the player's arm, with offset being where the offset is.
					Point3d heldVector;
					if(player.isSneaking()){
						heldVector = activeGun.definition.gun.handHeldAimedOffset;
					}else{
						heldVector = activeGun.definition.gun.handHeldNormalOffset;
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
					
					if(!world.isClient()){
						//Save gun data if we stopped firing the prior tick.
						if(activeGun.state.isAtLeast(GunState.FIRING_CURRENTLY)){
							didGunFireLastTick = true;
						}else if(didGunFireLastTick){
							saveGun(false);
						}
					}else{
						//Check for player input.  Only do this for the main player.
						if(player.equals(InterfaceClient.getClientPlayer())){
							ControlSystem.controlPlayerGun(this);
						}
					}
				}
			}else{
				//Player is either null or not valid.  Remove us.
				//Don't update post movement, as the gun will crash on update.
				remove();
				return false;
			}
			
			//Update the gun now, if we have one.
			updatePostMovement();
			//If we have a gun, and the player is spectating, don't allow the gun to render.
			if(activeGun != null){
				activeGun.isDisabled = player != null && player.isSpectator();
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public void remove(){
		super.remove();
		if(player != null){
			if(world.isClient()){
				playerClientGuns.remove(player.getID());
			}else{
				playerServerGuns.remove(player.getID());
			}
		}
	}
	
	@Override
	protected void updateCollisionBoxes(){
		//Do nothing and don't add any collision.  This could block player actions.
	}
	
	private void saveGun(boolean remove){
		WrapperNBT data = new WrapperNBT();
		activeGun.save(data);
		gunStack.setTagCompound(data.tag);
		didGunFireLastTick = false;
		if(remove){
			removePart(activeGun, null);
			activeGun = null;
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public RenderPlayerGun getRenderer(){
		if(renderer == null){
			renderer = new RenderPlayerGun();
		}
		return renderer;
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		if(player != null){
			data.setString("playerUUID", player.getID());
		}
		return data;
	}
}
