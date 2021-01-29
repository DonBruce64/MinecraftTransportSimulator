package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentOBJModel;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketTileEntityDecorColorChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleColorChange;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartColorChange;
import minecrafttransportsimulator.packloading.PackMaterialComponent;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.item.ItemStack;

/**A GUI that is used to craft vehicle parts and other pack components.  This GUI displays
 * the items required to craft a vehicle, the item that will be crafted, and some properties
 * of that item.  Allows for scrolling via a scroll wheel, and remembers the last item that
 * was selected to allow for faster lookup next time the GUI is opened.
 * 
 * @author don_bruce
 */
public class GUIPaintGun extends AGUIBase{
	
	//Init variables.
	private final EntityVehicleF_Physics vehicle;
	private final APart part;
	private final TileEntityDecor decor;
	private final TileEntityPole pole;
	private final Axis axis;
	private final WrapperPlayer player;
	
	//Buttons and labels.
	private GUIComponentLabel partName;
	
	private GUIComponentButton prevColorButton;
	private GUIComponentButton nextColorButton;
	
	private GUIComponentButton confirmButton;
	
	//Crafting components.
	private final List<GUIComponentItem> craftingItemIcons = new ArrayList<GUIComponentItem>();
	
	//Renders for the item.
	private GUIComponentOBJModel modelRender;
	
	//Runtime variables.	
	private AItemSubTyped<?> currentItem;
	private AItemSubTyped<?> prevSubItem;
	private AItemSubTyped<?> nextSubItem;

	public GUIPaintGun(EntityVehicleF_Physics vehicle, WrapperPlayer player){
		this.vehicle = vehicle;
		this.part = null;
		this.decor = null;
		this.pole = null;
		this.axis = null;
		this.player = player;
		this.currentItem = (AItemSubTyped<?>) PackParserSystem.getItem(vehicle.definition.packID, vehicle.definition.systemName, vehicle.currentSubName);
	}
	
	public GUIPaintGun(APart part, WrapperPlayer player){
		this.vehicle = null;
		this.part = part;
		this.decor = null;
		this.pole = null;
		this.axis = null;
		this.player = player;
		this.currentItem = (AItemSubTyped<?>) PackParserSystem.getItem(part.definition.packID, part.definition.systemName, part.currentSubName);
	}
	
	public GUIPaintGun(TileEntityDecor decor, WrapperPlayer player){
		this.vehicle = null;
		this.part = null;
		this.decor = decor;
		this.pole = null;
		this.axis = null;
		this.player = player;
		this.currentItem = (AItemSubTyped<?>) PackParserSystem.getItem(decor.definition.packID, decor.definition.systemName, decor.currentSubName);
	}
	
	public GUIPaintGun(TileEntityPole pole, Axis axis, WrapperPlayer player){
		this.vehicle = null;
		this.part = null;
		this.decor = null;
		this.pole = pole;
		this.axis = axis;
		this.player = player;
		ATileEntityPole_Component component = pole.components.get(axis);
		this.currentItem = component.getItem();
	}

	@Override
	public void setupComponents(int guiLeft, int guiTop){	
		//Create color navigation section.
		addButton(prevColorButton = new GUIComponentButton(guiLeft + 38, guiTop + 135, 20, "<", 20, true, 20, 20, 0, 196, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(){
				currentItem = prevSubItem;
				updateNames();
			}
		});
		addButton(nextColorButton = new GUIComponentButton(guiLeft + 160, guiTop + 135, 20, ">", 20, true, 20, 20, 0, 196, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(){
				currentItem = nextSubItem;
				updateNames();
			}
		});
		addLabel(partName = new GUIComponentLabel(guiLeft + 60, guiTop + 120, Color.WHITE, "", TextPosition.LEFT_ALIGNED, 98, 1.0F, false));
		
		//Create the crafting item slots.  8 18X18 slots (8X2) need to be made here.
		craftingItemIcons.clear();
		final int craftingIconSize = 18;
		for(byte i=0; i<4*2; ++i){				
			GUIComponentItem craftingItem = new GUIComponentItem(guiLeft + 225 + craftingIconSize*(i/4), guiTop + 26 + craftingIconSize*(i%4), craftingIconSize/16F, null);
			addItem(craftingItem);
			craftingItemIcons.add(craftingItem);
		}
		
		//Create the OBJ render.
		addOBJModel(modelRender = new GUIComponentOBJModel(guiLeft + 109, guiTop + 57, 32.0F, true, true, false));
		
		//Create the confirm button.
		addButton(confirmButton = new GUIComponentButton(guiLeft + 99, guiTop + 167, 20, "", 20, true, 20, 20, 20, 196, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(){
				if(vehicle != null){
					InterfacePacket.sendToServer(new PacketVehicleColorChange(vehicle, (ItemVehicle) currentItem));
				}else if(part != null){
					InterfacePacket.sendToServer(new PacketVehiclePartColorChange(part, (ItemPart) currentItem));
				}else if(decor != null){
					InterfacePacket.sendToServer(new PacketTileEntityDecorColorChange(decor, (ItemDecor) currentItem));
				}else{
					InterfacePacket.sendToServer(new PacketTileEntityPoleChange(pole, axis, (ItemPoleComponent) currentItem, null, false));
				}
				InterfaceGUI.closeGUI();
			}
		});
		
		//Update the names now that we have everything put together.
		updateNames();
	}

	@Override
	public void setStates(){				
		//Set buttons based on if we have prev or next items.
		prevColorButton.enabled = prevSubItem != null;
		nextColorButton.enabled = nextSubItem != null;
		
		//Set confirm button based on if player has materials.
		confirmButton.enabled = currentItem != null && (player.isCreative() || player.getInventory().hasMaterials(currentItem, false, true));
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
		return "mts:textures/guis/paintgun.png";
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
		List<AItemPack<?>> packItems = PackParserSystem.getAllItemsForPack(currentItem.definition.packID);
		int currentItemIndex = packItems.indexOf(currentItem);
		
		//Loop forwards in our pack to find the next item in that pack.
		nextSubItem = null;
		if(currentItemIndex < packItems.size()){
			for(int i=currentItemIndex+1; i<packItems.size() && nextSubItem == null; ++i){
				if(packItems.get(i).definition.systemName.equals(currentItem.definition.systemName)){
					nextSubItem = (AItemSubTyped<?>) packItems.get(i);
					break;
				}
			}
		}
		
		//Loop backwards in our pack to find the prev item in that pack.
		prevSubItem = null;
		if(currentItemIndex > 0){
			for(int i=currentItemIndex-1; i>=0 && prevSubItem == null; --i){
				if(packItems.get(i).definition.systemName.equals(currentItem.definition.systemName)){
					prevSubItem = (AItemSubTyped<?>) packItems.get(i);
					break;
				}
			}
		}
		
		//All item bits are now set and updated.  Update info labels and item icons.
		partName.text = currentItem.getItemName();
		
		//Parse crafting items and set icon items.
		List<PackMaterialComponent> materials = PackMaterialComponent.parseFromJSON(currentItem, false, true, false);
		for(byte i=0; i<craftingItemIcons.size(); ++i){
			if(i < materials.size()){
				craftingItemIcons.get(i).stacks = materials.get(i).possibleItems;
				for(ItemStack stack : craftingItemIcons.get(i).stacks){
					stack.setCount(materials.get(i).qty);
				}
	    	}else{
	    		craftingItemIcons.get(i).stacks = null;
	    	}			
		}
		
		
		//Set model render properties.
		modelRender.modelLocation = currentItem.definition.getModelLocation();
		modelRender.textureLocation = currentItem.definition.getTextureLocation(currentItem.subName);
	}
}
