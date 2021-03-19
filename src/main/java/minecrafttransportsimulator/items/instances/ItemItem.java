package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUIBooklet;
import minecrafttransportsimulator.guis.instances.GUIInstruments;
import minecrafttransportsimulator.guis.instances.GUIPackExporter;
import minecrafttransportsimulator.guis.instances.GUITextEditor;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemFood;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.systems.ConfigSystem;

public class ItemItem extends AItemPack<JSONItem> implements IItemVehicleInteractable, IItemFood{
	/*Current page of this item, if it's a booklet.  Kept here locally as only one item class is constructed for each booklet definition.*/
	public int pageNumber;
	/*Last engine clicked for jumper cable items.  ept here locally as only one item class is constructed for each jumper cable definition.*/
	private static PartEngine lastEngineClicked;
	
	public ItemItem(JSONItem definition){
		super(definition, null);
	}
	
	@Override
	public boolean canBreakBlocks(){
		return !definition.item.type.equals(ItemComponentType.WRENCH);
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		switch(definition.item.type){
			case WRENCH : {
				//If the player isn't the owner of the vehicle, they can't interact with it.
				if(!ownerState.equals(PlayerOwnerState.USER)){
					if(rightClick){
						if(vehicle.world.isClient()){
							if(ConfigSystem.configObject.clientControls.devMode.value && vehicle.equals(player.getEntityRiding())){
								InterfaceGUI.openGUI(new GUIPackExporter(vehicle));
							}else if(player.isSneaking()){
								InterfaceGUI.openGUI(new GUITextEditor(vehicle));
							}else{
								InterfaceGUI.openGUI(new GUIInstruments(vehicle, player));
							}
						}else{
							return CallbackType.PLAYER;
						}
					}else if(!vehicle.world.isClient()){
						if(part != null && !player.isSneaking()){
							//Player can remove part.  Check that the part isn't permanent, or a part with subparts.
							//If not, spawn item in the world and remove part.
							//Make sure to remove the part before spawning the item.  Some parts
							//care about this order and won't spawn items unless they've been removed.
							if(!part.placementDefinition.isPermanent){
								vehicle.removePart(part, null);
								ItemPart droppedItem = part.getItem();
								if(droppedItem != null){
									WrapperNBT partData = new WrapperNBT();
									part.save(partData);
									vehicle.world.spawnItem(droppedItem, partData, part.position);
								}
							}
						}else if(player.isSneaking()){
							//Attacker is a sneaking player with a wrench.
							//Remove this vehicle if possible.
							if((!ConfigSystem.configObject.general.opPickupVehiclesOnly.value || ownerState.equals(PlayerOwnerState.ADMIN)) && (!ConfigSystem.configObject.general.creativePickupVehiclesOnly.value || player.isCreative())){
								//Make sure we disconnect any trailers linked to this vehicle.  We don't want to save those.
								if(vehicle.towedVehicle != null){
									vehicle.changeTrailer(null, null, null, null, null);
								}
								if(vehicle.towedByVehicle != null){
									vehicle.towedByVehicle.changeTrailer(null, null, null, null, null);
								}
								ItemVehicle vehicleItem = vehicle.getItem();
								WrapperNBT vehicleData = new WrapperNBT();
								vehicle.save(vehicleData);
								vehicle.world.spawnItem(vehicleItem, vehicleData, vehicle.position);
								vehicle.isValid = false;
							}
						}
					}
				}else{
					player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehicleowned"));
				}
				return CallbackType.NONE;
			}
			case JUMPER_CABLES : {
				if(!vehicle.world.isClient()){
					if(rightClick){
						if(part instanceof PartEngine){
							PartEngine engine = (PartEngine) part;
							if(engine.linkedEngine == null){
								if(lastEngineClicked == null){
									lastEngineClicked = engine;
									player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.firstlink"));
								}else if(!lastEngineClicked.equals(engine)){
									if(lastEngineClicked.entityOn.equals(engine.entityOn)){
										lastEngineClicked = null;
										player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.samevehicle"));
									}else if(engine.position.distanceTo(lastEngineClicked.position) < 15){
										engine.linkedEngine = lastEngineClicked;
										lastEngineClicked.linkedEngine = engine;
										InterfacePacket.sendToAllClients(new PacketPartEngine(engine, lastEngineClicked));
										InterfacePacket.sendToAllClients(new PacketPartEngine(lastEngineClicked, engine));
										lastEngineClicked = null;
										player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.secondlink"));
									}else{
										lastEngineClicked = null;
										player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.toofar"));
									}
								}
							}else{
								player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.alreadylinked"));
							}
						}
					}
				}
				return CallbackType.NONE;
			}
			case JUMPER_PACK : {
				if(rightClick){
					//Use jumper on vehicle.
					vehicle.electricPower = 12;
					if(!vehicle.world.isClient()){
						InterfacePacket.sendToPlayer(new PacketPlayerChatMessage("interact.jumperpack.success"), player);
						if(!player.isCreative()){
							player.getInventory().removeStack(player.getHeldStack(), 1);
						}
						return CallbackType.ALL;
					}
				}
				return CallbackType.NONE;
			}
			default : return CallbackType.SKIP; 
		}
	}
	
	@Override
	public boolean onUsed(WrapperWorld world, WrapperPlayer player){
		if(world.isClient() && definition.item.type.equals(ItemComponentType.BOOKLET)){
			InterfaceGUI.openGUI(new GUIBooklet(this));
		}
        return true;
    }

	@Override
	public int getTimeToEat(){
		return definition.item.type.equals(ItemComponentType.FOOD) ? definition.food.timeToEat : 0;
	}
	
	@Override
	public boolean isDrink(){
		return definition.food.isDrink;
	}

	@Override
	public int getHungerAmount(){
		return definition.food.hungerAmount;
	}

	@Override
	public float getSaturationAmount(){
		return definition.food.saturationAmount;
	}

	@Override
	public List<JSONPotionEffect> getEffects(){
		return definition.food.effects;
	}
	
	public static enum ItemComponentType{
		@JSONDescription("Creates an item with no functionality.")
		NONE,
		@JSONDescription("Creates a booklet, which is a book-like item.")
		BOOKLET,
		@JSONDescription("Creates an item that can be eaten.")
		FOOD,
		@JSONDescription("Creates an item that works as a part scanner.")
		SCANNER,
		@JSONDescription("Creates an item that works as a wrench.")
		WRENCH,
		@JSONDescription("Creates an item that works as jumper cables.")
		JUMPER_CABLES,
		@JSONDescription("Creates an item that works as a jumper pack.")
		JUMPER_PACK;
	}
}
