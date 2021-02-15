package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.AEntityE_Multipart;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONPlayerGun;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.instances.AnimationsPlayerGun;
import minecrafttransportsimulator.rendering.instances.RenderPlayerGun;
import minecrafttransportsimulator.vehicles.parts.PartGun;
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
	
	private static final AnimationsPlayerGun animator = new AnimationsPlayerGun();
	private static RenderPlayerGun renderer;
	
	public EntityPlayerGun(WrapperWorld world, WrapperPlayer playerSpawning, WrapperNBT data){
		super(world, data);
		//Get the player spawning us.
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
				remove();
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
	public JSONPlayerGun generateDefaultDefinition(){
		JSONPlayerGun defaultDefinition = new JSONPlayerGun();
		JSONPartDefinition fakeDef = new JSONPartDefinition();
		fakeDef.pos = new Point3d();
		fakeDef.rot = new Point3d();
		fakeDef.types = new ArrayList<String>();
		fakeDef.maxValue = Float.MAX_VALUE;
		defaultDefinition.parts = new ArrayList<JSONPartDefinition>();
		defaultDefinition.parts.add(fakeDef);
		return defaultDefinition;
	}
	
	@Override
	public void update(){
		super.update();
		//Make sure player is still valid and haven't left the server.
		if(player.isValid()){
			//Set our position to the player's position.  We may update this later if we have a gun.
			position.setTo(player.getPosition());
			motion.setTo(player.getVelocity());
			
			//Get the current gun.
			activeGun = parts.isEmpty() ? null : (PartGun) parts.get(0);
			
			//Check to make sure if we had a gun, that it didn't change.
			if(activeGun != null && (!activeGun.getItem().equals(player.getHeldItem()) || hotbarSelected != player.getHotbarIndex())){
				saveGun(true);
			}
			
			//If we don't have a gun yet, try to get the current one if the player is holding one.
			if(activeGun == null){
				AItemBase heldItem = player.getHeldItem();
				if(heldItem instanceof ItemPart){
					ItemPart heldPart = (ItemPart) heldItem;
					if(heldPart.isHandHeldGun() && !world.isClient()){
						gunStack = player.getHeldStack();
						//Need to add the type here to allow us to add the part.
						if(!definition.parts.get(0).types.contains(heldPart.definition.generic.type)){
							definition.parts.get(0).types.add(heldPart.definition.generic.type);
						}
						addPartFromItem(heldPart, new WrapperNBT(gunStack), new Point3d(), false);
						hotbarSelected = player.getHotbarIndex();
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
				
				//Save gun data if we stopped firing.
				if(!world.isClient()){
					if(activeGun.firing){
						didGunFireLastTick = true;
					}else if(!activeGun.firing && didGunFireLastTick){
						saveGun(false);
					}
				}
			}
		}else{
			remove();
		}
	}
	
	@Override
	public boolean shouldSavePosition(){
		return false;
	}
	
	@Override
	public void remove(){
		super.remove();
		if(player != null){
			if(world.isClient()){
				playerClientGuns.remove(player.getUUID());
			}else{
				playerServerGuns.remove(player.getUUID());
			}
		}
	}
	
	private void saveGun(boolean remove){
		WrapperNBT data = new WrapperNBT();
		activeGun.save(data);
		gunStack.setTagCompound(data.tag);
		didGunFireLastTick = false;
		if(remove){
			activeGun.remove();
			activeGun = null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public AnimationsPlayerGun getAnimator(){
		return animator;
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
	public void save(WrapperNBT data){
		super.save(data);
		if(player != null){
			data.setString("playerUUID", player.getUUID());
		}
	}
}
