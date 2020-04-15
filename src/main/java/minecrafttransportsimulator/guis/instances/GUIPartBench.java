package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.blocks.instances.BlockPartsBench;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentOBJModel;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.items.packs.ItemVehicle;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.instances.PacketPlayerCraftItem;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperGame;
import minecrafttransportsimulator.wrappers.WrapperInput;
import minecrafttransportsimulator.wrappers.WrapperNetwork;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**A GUI that is used to craft vehicle parts and other pack components.  This GUI displays
 * the items required to craft a vehicle, the item that will be crafted, and some properties
 * of that item.  Allows for scrolling via a scroll wheel, and remembers the last item that
 * was selected to allow for faster lookup next time the GUI is opened.
 * 
 * @author don_bruce
 */
public class GUIPartBench extends AGUIBase{
	/*Last item this bench was on when closed.  Keyed by block*/
	private static final Map<BlockPartsBench, AItemPack<? extends AJSONItem<?>>> lastOpenedItem = new HashMap<BlockPartsBench, AItemPack<? extends AJSONItem<?>>>();
	
	//Init variables.
	private final BlockPartsBench bench;
	private final WrapperPlayer player;
	
	//Buttons and labels.
	private GUIComponentButton prevPackButton;
	private GUIComponentButton nextPackButton;
	private GUIComponentLabel packName;
	
	private GUIComponentButton prevPartButton;
	private GUIComponentButton nextPartButton;
	private GUIComponentLabel partName;
	
	private GUIComponentButton prevColorButton;
	private GUIComponentButton nextColorButton;
	
	private GUIComponentLabel partInfo;
	private GUIComponentButton switchInfoButton;
	private GUIComponentButton confirmButton;
	
	//Crafting components.
	private final List<GUIComponentItem> craftingItemIcons = new ArrayList<GUIComponentItem>();
	
	//Renders for the item.
	private GUIComponentItem itemRender;
	private GUIComponentOBJModel modelRender;
	
	//Runtime variables.
	private String prevPack;
	private String currentPack;
	private String nextPack;
	
	private AItemPack<? extends AJSONItem<?>> prevItem;
	private AItemPack<? extends AJSONItem<?>> currentItem;
	private AItemPack<? extends AJSONItem<?>> nextItem;
	
	//Only used for vehicles.
	private AItemPack<? extends AJSONItem<?>> prevSubItem;
	private AItemPack<? extends AJSONItem<?>> nextSubItem;
	
	boolean displayDescription = true;
	private String descriptionText;
	private String informationText;
	

	public GUIPartBench(BlockPartsBench bench, WrapperPlayer player){
		this.bench = bench;
		this.player = player;
		if(lastOpenedItem.containsKey(bench)){
			currentItem = lastOpenedItem.get(bench);
			currentPack = currentItem.definition.packID;
		}else{
			//Find a pack that has the item we are supposed to craft and set it.
			for(String packID : MTSRegistry.packItemMap.keySet()){
				if(currentPack == null){
					for(AItemPack<? extends AJSONItem<?>> packItem : MTSRegistry.packItemMap.get(packID).values()){
						if(bench.isJSONValid(packItem.definition)){
							currentItem = packItem;
							currentPack = packItem.definition.packID;
							return;
						}
					}
				}
			}
		}
	}

	@Override
	public void setupComponents(int guiLeft, int guiTop){	
		//Create pack navigation section.
		addButton(prevPackButton = new GUIComponentButton(guiLeft + 17, guiTop + 11, 20, "<", 20, true, 20, 20, 0, 196, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(){
				currentPack = prevPack;
				currentItem = null;
				updateNames();
			}
		});
		addButton(nextPackButton = new GUIComponentButton(guiLeft + 243, guiTop + 11, 20, ">", 20, true, 20, 20, 0, 196, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(){
				currentPack = nextPack;
				currentItem = null;
				updateNames();
			}
		});
		int centerBetweenButtons = prevPackButton.x + prevPackButton.width + (nextPackButton.x - (prevPackButton.x + prevPackButton.width))/2;
		addLabel(packName = new GUIComponentLabel(centerBetweenButtons, guiTop + 16, Color.WHITE, "", 1.0F, true, false, 0));
		
		
		//Create part navigation section.
		addButton(prevPartButton = new GUIComponentButton(prevPackButton.x, prevPackButton.y + prevPackButton.height, 20, "<", 20, true, 20, 20, 0, 196, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(){
				currentItem = prevItem;
				updateNames();
			}
		});
		addButton(nextPartButton = new GUIComponentButton(nextPackButton.x, nextPackButton.y + nextPackButton.height, 20, ">", 20, true, 20, 20, 0, 196, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(){
				currentItem = nextItem;
				updateNames();
			}
		});
		addLabel(partName = new GUIComponentLabel(packName.x, packName.y + prevPackButton.height, Color.WHITE, "", 0.75F, true, false, 0));
		addLabel(partInfo = new GUIComponentLabel(guiLeft + 17, guiTop + 58, Color.WHITE, "", 1.0F, false, false, 150));
		
		
		//Create color navigation section.
		addButton(prevColorButton = new GUIComponentButton(guiLeft + 175, guiTop + 131, 20, "<", 15, true, 20, 20, 0, 196, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(){
				currentItem = prevSubItem;
				updateNames();
			}
		});
		addButton(nextColorButton = new GUIComponentButton(guiLeft + 245, guiTop + 131, 20, ">", 15, true, 20, 20, 0, 196, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(){
				currentItem = nextSubItem;
				updateNames();
			}
		});
		addLabel(new GUIComponentLabel(prevColorButton.x + prevColorButton.width + (nextColorButton.x - (prevColorButton.x + prevColorButton.width))/2, guiTop + 136, Color.WHITE, WrapperGUI.translate("gui.vehicle_bench.color"), 1.0F, true, false, 0).setButton(nextColorButton));
		
		
		//Create the crafting item slots.  14 18X18 slots (7X2) need to be made here.
		craftingItemIcons.clear();
		final int craftingIconSize = 18;
		for(byte i=0; i<7*2; ++i){				
			GUIComponentItem craftingItem = new GUIComponentItem(guiLeft + 276 + craftingIconSize*(i/7), guiTop + 20 + craftingIconSize*(i%7), craftingIconSize/16F, null, 1, -1);
			addItem(craftingItem);
			craftingItemIcons.add(craftingItem);
		}
		
		
		//Create both the item and OBJ renders.  We choose which to display later.
		addItem(itemRender = new GUIComponentItem(guiLeft + 175, guiTop + 56, 5.625F, null, 1, -1));
		addOBJModel(modelRender = new GUIComponentOBJModel(guiLeft + 220, guiTop + 101, 32.0F, true, true, false));
		
		
		//Create the info switching button.
		addButton(switchInfoButton = new GUIComponentButton(guiLeft + 147, guiTop + 159, 20, "?", 20, true, 20, 20, 0, 196, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(){
				if(displayDescription){
					displayDescription = false;
					partInfo.text = informationText; 
				}else{
					displayDescription = true;
					partInfo.text = descriptionText; 
				}
				WrapperNetwork.sendToServer(new PacketPlayerCraftItem(currentItem));
			}
		});
				
		
		//Create the confirm button.
		addButton(confirmButton = new GUIComponentButton(guiLeft + 211, guiTop + 156, 20, "", 20, true, 20, 20, 20, 196, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(){
				WrapperNetwork.sendToServer(new PacketPlayerCraftItem(currentItem));
			}
		});
		
		//Update the names now that we have everything put together.
		updateNames();
	}

	@Override
	public void setStates(){				
		//Set buttons based on if we have prev or next items.
		prevPackButton.enabled = prevPack != null;
		nextPackButton.enabled = nextPack != null;
		prevPartButton.enabled = prevItem != null;
		nextPartButton.enabled = nextItem != null;
		prevColorButton.visible = currentItem instanceof ItemVehicle;
		prevColorButton.enabled = prevSubItem != null;
		nextColorButton.visible = currentItem instanceof ItemVehicle;
		nextColorButton.enabled = nextSubItem != null;
		switchInfoButton.visible = currentItem instanceof ItemVehicle;
		
		//Set confirm button based on if player has materials.
		confirmButton.enabled = currentItem != null && player.hasMaterials(currentItem);
		
		//Check the mouse to see if it updated and we need to change items.
		int wheelMovement = WrapperInput.getTrackedMouseWheel();
		if(wheelMovement > 0 && nextPartButton.enabled){
			nextPartButton.onClicked();
		}else if(wheelMovement < 0 && prevPartButton.enabled){
			prevPartButton.onClicked();
		}
	}
	
	@Override
	public int getWidth(){
		return 327;
	}
	
	@Override
	public int getHeight(){
		return 196;
	}
	
	@Override
	public String getTexture(){
		return "mts:textures/guis/crafting.png";
	}
	
	/**
	 * Loop responsible for updating pack/part names whenever an action occurs.
	 * Looks through all items in the list that was passed-in on GUI construction time and
	 * uses the order to determine which pack/item to scroll to when a button is clicked.
	 * Sets the variables to be used on a button action, so once an action is performed this
	 * logic MUST be called to update the button action states!
	 */
	private void updateNames(){
		//Get all pack indexes.
		List<String> packIDs = new ArrayList<String>(MTSRegistry.packItemMap.keySet());
		int currentPackIndex = packIDs.indexOf(currentPack);
		
		//Loop forwards to find a pack that has the items we need and set that as the next pack.
		//Only set the pack if it has items in it that match our bench's parameters.
		nextPack = null;
		if(currentPackIndex < packIDs.size()){
			for(int i=currentPackIndex+1; i<packIDs.size() && nextPack == null; ++i){
				for(AItemPack<? extends AJSONItem<?>> packItem : MTSRegistry.packItemMap.get(packIDs.get(i)).values()){
					if(bench.isJSONValid(packItem.definition)){
						nextPack = packIDs.get(i);
						break;
					}
				}
			}
		}
		
		//Loop backwards to find a pack that has the items we need and set that as the prev pack.
		//Only set the pack if it has items in it that match our bench's parameters.
		prevPack = null;
		if(currentPackIndex > 0){
			for(int i=currentPackIndex-1; i>=0 && prevPack == null; --i){
				for(AItemPack<? extends AJSONItem<?>> packItem : MTSRegistry.packItemMap.get(packIDs.get(i)).values()){
					if(bench.isJSONValid(packItem.definition)){
						prevPack = packIDs.get(i);
						break;
					}
				}
			}
		}
		
		
		//Set item indexes.
		//If we don't have a pack, it means we don't have any items that are for this bench, so we shouldn't do anything else.
		if(currentPack == null){
			return;
		}
		List<AItemPack<? extends AJSONItem<?>>> packItems = new ArrayList<AItemPack<? extends AJSONItem<?>>>(MTSRegistry.packItemMap.get(currentPack).values());
		int currentItemIndex = packItems.indexOf(currentItem);
		//If currentItem is null, it means we switched packs and need to re-set it to the first item of the new pack.
		//Do so now before we do looping to prevent crashes.
		//Find a pack that has the item we are supposed to craft and set it.
		//If we are for vehicles, make sure to set the next subItem if we can.
		if(currentItem == null){
			for(AItemPack<? extends AJSONItem<?>> packItem : MTSRegistry.packItemMap.get(currentPack).values()){
				if(currentItem == null || (currentItem instanceof ItemVehicle && nextSubItem == null)){
					if(bench.isJSONValid(packItem.definition)){
						if(currentItem == null){
							currentItem = packItem;
							currentItemIndex = packItems.indexOf(currentItem);
						}else if(currentItem instanceof ItemVehicle && nextSubItem == null){
							if(((JSONVehicle) packItem.definition).genericName.equals(((JSONVehicle) currentItem.definition).genericName)){
								nextSubItem = packItem;
							}
						}
					}
				}
			}
		}

		//Loop forwards in our pack to find the next item in that pack.
		//Only set the pack if it has items in it that match our bench's parameters.
		nextItem = null;
		nextSubItem = null;
		if(currentItemIndex < packItems.size()){
			for(int i=currentItemIndex+1; i<packItems.size() && nextItem == null; ++i){
				if(bench.isJSONValid(packItems.get(i).definition)){
					//If we are for vehicles, and this item is the same sub-item classification, 
					//set nextSubItem and continue on.
					if(currentItem instanceof ItemVehicle){
						if(((JSONVehicle) packItems.get(i).definition).genericName.equals(((JSONVehicle) currentItem.definition).genericName)){
							if(nextSubItem == null){
								nextSubItem = packItems.get(i);
							}
							continue;
						}
					}
					nextItem = packItems.get(i);
					break;
				}
			}
		}
		
		//Loop backwards in our pack to find the prev item in that pack.
		//Only set the pack if it has items in it that match our bench's parameters.
		prevItem = null;
		prevSubItem = null;
		if(currentItemIndex > 0){
			for(int i=currentItemIndex-1; i>=0 && (prevItem == null || currentItem instanceof ItemVehicle); --i){
				if(bench.isJSONValid(packItems.get(i).definition)){
					//If we are for vehicles, and we didn't switch items, and this item
					//is the same sub-item classification, set prevSubItem and continue on.
					//If we did switch, we want the first subItem in the set of items to
					//be the prevItem we pick.  This ensures when we switch we'll be on the 
					//same subItem each time we switch items.
					if(currentItem instanceof ItemVehicle){
						if(((JSONVehicle) packItems.get(i).definition).genericName.equals(((JSONVehicle) currentItem.definition).genericName)){
							if(prevSubItem == null){
								prevSubItem = packItems.get(i);
							}
						}else{
							if(prevItem == null){
								prevItem = packItems.get(i);
							}else if(((JSONVehicle) packItems.get(i).definition).genericName.equals(((JSONVehicle) prevItem.definition).genericName)){
								prevItem = packItems.get(i);
							}
						}
					}else{
						prevItem = packItems.get(i);
						break;
					}
				}
			}
		}
		
		
		//All pack and part bits are now set and updated.  Update info labels and item icons.
		packName.text = WrapperGame.getModName(currentPack);
		partName.text = currentItem.definition.general.name != null ? currentItem.definition.general.name : currentItem.definition.systemName;
		
		//TODO this needs to go away when we get a wrapper itemstack.
		//Create part description text.
		ItemStack tempStack = new ItemStack(currentItem);
		tempStack.setTagCompound(new NBTTagCompound());
		List<String> descriptiveLines = new ArrayList<String>();
		tempStack.getItem().addInformation(tempStack, Minecraft.getMinecraft().world, descriptiveLines, ITooltipFlag.TooltipFlags.NORMAL);
		descriptionText = "";
		for(String line : descriptiveLines){
			descriptionText += line + "\n";
		}
		
		//Create vehicle information text, if we are a vehicle item.
		if(currentItem instanceof ItemVehicle){
			informationText = getVehicleInfoText();
		}
		
		//Set the info text.
		partInfo.text = displayDescription ? descriptionText : informationText;

		
		//Parse crafting items and set icon items.
		String[] craftingItemTexts = MTSRegistry.packCraftingMap.get(currentItem);
		for(byte i=0; i<craftingItemIcons.size(); ++i){
			try{
		    	if(i < craftingItemTexts.length){
		    		String itemText = craftingItemTexts[i];
		    		craftingItemIcons.get(i).itemQty = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
					itemText = itemText.substring(0, itemText.lastIndexOf(':'));
					craftingItemIcons.get(i).itemMetadata = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
					itemText = itemText.substring(0, itemText.lastIndexOf(':'));
					craftingItemIcons.get(i).itemName = itemText;
		    	}else{
		    		craftingItemIcons.get(i).itemName = null;
		    	}
			}catch(Exception e){
				throw new NullPointerException("ERROR: Could not parse crafting ingredients for item: " + currentItem.definition.packID + ":" + currentItem.definition.systemName + ".  Report this to the pack author!");
			}			
		}
		
		//Enable render based on what component we have.
		String modelLocation = currentItem.getModelLocation();
		if(modelLocation != null){
			String textureLocation = currentItem.getTextureLocation();
			modelRender.modelDomain = currentPack;
			modelRender.modelLocation = modelLocation;
			modelRender.textureDomain = currentPack;
			modelRender.textureLocation = textureLocation;
			itemRender.itemName = null;
			//Don't spin signs.  That gets annoying.
			modelRender.spin = !(currentItem.definition instanceof JSONPoleComponent && ((JSONPoleComponent) currentItem.definition).general.type.equals("sign"));
		}else{
			itemRender.itemName = currentItem.definition.packID + ":" + currentItem.definition.systemName;
			modelRender.modelDomain = null;
		}
		
		//Now update the last saved item.
		lastOpenedItem.put(bench, currentItem);
	}
	
	private String getVehicleInfoText(){
		JSONVehicle vehicleDefinition = (JSONVehicle) currentItem.definition;
		int controllers = 0;
		int passengers = 0;
		int cargo = 0;
		int mixed = 0;
		float minFuelConsumption = 99;
		float maxFuelConsumption = 0;
		float minWheelSize = 99;
		float maxWheelSize = 0;
		
		//Get how many passengers and cargo this vehicle can hold.
		for(VehiclePart part : vehicleDefinition.parts){
			if(part.isController){
				++controllers;
			}else{
				boolean canAcceptSeat = false;
				boolean canAcceptCargo = false;
				if(part.types.contains("seat")){
					canAcceptSeat = true;
				}
				if(part.types.contains("crate") || part.types.contains("barrel")){
					canAcceptCargo = true;
				}
				if(canAcceptSeat && !canAcceptCargo){
					++passengers;
				}else if(canAcceptCargo && !canAcceptSeat){
					++cargo;
				}else if(canAcceptCargo && canAcceptSeat){
					++mixed;
				}
				
				for(String partNameEntry : part.types){
					if(partNameEntry.startsWith("engine")){
						minFuelConsumption = Math.min(part.minValue, minFuelConsumption);
						maxFuelConsumption = Math.max(part.maxValue, maxFuelConsumption);
						break;
					}
				}
				
				if(part.types.contains("wheel")){
					minWheelSize = Math.min(part.minValue, minWheelSize);
					maxWheelSize = Math.max(part.maxValue, maxWheelSize);
				}
			}
		}
		
		//Combine translated header and info text together into a single string and return.
		String totalInformation = "";
		totalInformation += WrapperGUI.translate("gui.vehicle_bench.type") + ": " + String.valueOf(vehicleDefinition.general.type) + "\n";
		totalInformation += WrapperGUI.translate("gui.vehicle_bench.weight") + ": " + String.valueOf(vehicleDefinition.general.emptyMass) + "\n";
		totalInformation += WrapperGUI.translate("gui.vehicle_bench.fuel") + ": " + String.valueOf(vehicleDefinition.motorized.fuelCapacity) + "\n";
		totalInformation += WrapperGUI.translate("gui.vehicle_bench.controllers") + ": " + String.valueOf(controllers) + "\n";
		totalInformation += WrapperGUI.translate("gui.vehicle_bench.passengers") + ": " + String.valueOf(passengers) + "\n";
		totalInformation += WrapperGUI.translate("gui.vehicle_bench.cargo") + ": " + String.valueOf(cargo) + "\n";
		totalInformation += WrapperGUI.translate("gui.vehicle_bench.mixed") + ": " + String.valueOf(mixed) + "\n";
		if(minFuelConsumption != 99){
			totalInformation += WrapperGUI.translate("gui.vehicle_bench.engine") + ": " + String.valueOf(minFuelConsumption) + "-" + String.valueOf(maxFuelConsumption) + "\n";
		}
		if(minWheelSize != 99){
			totalInformation += WrapperGUI.translate("gui.vehicle_bench.wheel") + ": " + String.valueOf(minWheelSize) + "-" + String.valueOf(maxWheelSize) + "\n";
		}
		return totalInformation;
	}
}
