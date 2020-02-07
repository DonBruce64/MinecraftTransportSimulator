package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.BlockBench;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.items.packs.ItemVehicle;
import minecrafttransportsimulator.items.packs.parts.AItemPart;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.AJSONMultiModel;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.general.PacketPlayerCrafting;
import minecrafttransportsimulator.systems.OBJParserSystem;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GUIPartBench extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation(MTS.MODID, "textures/guis/crafting.png");	
	/*Last item this bench was on when closed.  Keyed by block*/
	private static final Map<BlockBench, AItemPack<? extends AJSONItem<?>>> lastOpenedItem = new HashMap<BlockBench, AItemPack<? extends AJSONItem<?>>>();
	
	private final BlockBench bench;
	private final EntityPlayer player;
	
	private GuiButton leftPackButton;
	private GuiButton rightPackButton;
	private GuiButton leftPartButton;
	private GuiButton rightPartButton;
	private GuiButton leftColorButton;
	private GuiButton rightColorButton;
	private GuiButton startButton;
	
	private int guiLeft;
	private int guiTop;
	
	private String prevPack;
	private String currentPack;
	private String nextPack;
	
	private AItemPack<? extends AJSONItem<?>> prevItem;
	private AItemPack<? extends AJSONItem<?>> currentItem;
	private AItemPack<? extends AJSONItem<?>> nextItem;
	
	//Only used for vehicles.
	private AItemPack<? extends AJSONItem<?>> prevSubItem;
	private AItemPack<? extends AJSONItem<?>> nextSubItem;
	
	/**Display list GL integers.  Keyed by pack item.*/
	private final Map<AItemPack<? extends AJSONItem<?>>, Integer> partDisplayLists = new HashMap<AItemPack<? extends AJSONItem<?>>, Integer>();
	private final Map<AItemPack<? extends AJSONItem<?>>, Float> partScalingFactors = new HashMap<AItemPack<? extends AJSONItem<?>>, Float>();
	
	/**Part texture.  Keyed by pack item.*/
	private final Map<AItemPack<? extends AJSONItem<?>>, ResourceLocation> textureMap = new HashMap<AItemPack<? extends AJSONItem<?>>, ResourceLocation>();
	
	public GUIPartBench(BlockBench bench, EntityPlayer player){
		this.bench = bench;
		this.player = player;
		if(lastOpenedItem.containsKey(bench)){
			currentItem = lastOpenedItem.get(bench);
			currentPack = currentItem.definition.packID;
		}else{
			//Find a pack that has the item we are supposed to craft and set it.
			//If we are for vehicles, make sure to set the next subItem if we can.
			for(String packID : MTSRegistry.packItemMap.keySet()){
				if(currentPack == null){
					for(AItemPack<? extends AJSONItem<?>> packItem : MTSRegistry.packItemMap.get(packID).values()){
						if(bench.isJSONValid(packItem.definition)){
							currentPack = packID;
							break;
						}
					}
				}
			}
		}
		updatePartNames();
	}
	
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = bench.renderType.isForVehicles ? (this.width - 356)/2 : (this.width - 256)/2;
		guiTop = (this.height - 220)/2;
		
		buttonList.add(leftPackButton = new GuiButton(0, guiLeft + 10, guiTop + 5, 20, 20, "<"));
		buttonList.add(rightPackButton = new GuiButton(0, guiLeft + 226, guiTop + 5, 20, 20, ">"));
		buttonList.add(leftPartButton = new GuiButton(0, guiLeft + 10, guiTop + 25, 20, 20, "<"));
		buttonList.add(rightPartButton = new GuiButton(0, guiLeft + 226, guiTop + 25, 20, 20, ">"));
		if(bench.renderType.isForVehicles){
			buttonList.add(leftColorButton = new GuiButton(0, guiLeft + 280, guiTop + 25, 20, 20, "<"));
			buttonList.add(rightColorButton = new GuiButton(0, guiLeft + 300, guiTop + 25, 20, 20, ">"));
		}
		buttonList.add(startButton = new GuiButton(0, guiLeft + 188, guiTop + 170, 20, 20, ""));
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		super.drawScreen(mouseX, mouseY, renderPartialTicks);
		//Draw background layer.
		GL11.glColor3f(1, 1, 1); //Not sure why buttons make this grey, but whatever...
		mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 256, 201);
		
		//If we are for vehicles, draw an extra segment to the right for the info text.
		if(bench.renderType.isForVehicles){
			drawTexturedModalRect(guiLeft + 250, guiTop, 144, 0, 111, 201);
		}
		
		//If we don't have a pack it means we don't have any compatible items.
		//Don't go any further as we'll end up crashing the game.
		if(currentPack == null){
			return;
		}
		
		//If we can make this part, draw the start arrow.
		if(startButton.enabled){
			drawTexturedModalRect(guiLeft + 140, guiTop + 173, 0, 201, 44, 16);
		}
		
		//Render the text headers.
		drawCenteredString(MTSRegistry.packTabs.get(currentPack).getTranslatedTabLabel(), guiLeft + 130, guiTop + 10);
		drawCenteredString(currentItem.definition.general.name != null ? currentItem.definition.general.name : currentItem.definition.systemName, guiLeft + 130, guiTop + 30);
		if(bench.renderType.isForVehicles){
			drawCenteredString(I18n.format("gui.vehicle_bench.color"), guiLeft + 300, guiTop + 10);
		}
		
		//Set button states and render.
		startButton.enabled = PacketPlayerCrafting.doesPlayerHaveMaterials(player, currentItem);
		leftPackButton.enabled = prevPack != null;
		rightPackButton.enabled = nextPack != null;
		if(bench.renderType.isForVehicles){
			//If we are for vehicles, don't enable the part button if there's not a part that doesn't match the color.
			//We need to enable the color button instead for that.
			leftPartButton.enabled = prevItem != null;
			rightPartButton.enabled = nextItem != null;
			leftColorButton.enabled = prevSubItem != null;
			rightColorButton.enabled = nextSubItem != null;
		}else{
			leftPartButton.enabled = prevItem != null;
			rightPartButton.enabled = nextItem != null;
		}
		for(Object obj : buttonList){
			((GuiButton) obj).drawButton(mc, mouseX, mouseY, 0);
		}
		drawRect(guiLeft + 190, guiTop + 188, guiLeft + 206, guiTop + 172, startButton.enabled ? Color.GREEN.getRGB() : Color.RED.getRGB());
		
		//Render descriptive text.
		if(bench.renderType.isForVehicles){
			renderVehicleInfoText();
		}else{
			renderInfoText();
		}
		
		//Render materials in the bottom slots.
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.enableGUIStandardItemLighting();
		int stackOffset = 9;
		for(ItemStack craftingStack : MTSRegistry.getMaterials(currentItem)){
			ItemStack renderedStack = new ItemStack(craftingStack.getItem(), craftingStack.getCount(), craftingStack.getMetadata());
			this.itemRender.renderItemAndEffectIntoGUI(renderedStack, guiLeft + stackOffset, guiTop + 172);
			this.itemRender.renderItemOverlays(fontRenderer, renderedStack, guiLeft + stackOffset, guiTop + 172);
			stackOffset += 18;
		}
		
		//We render the text afterwards to ensure it doesn't render behind the items.
		stackOffset = 9;
		int itemTooltipBounds = 16;
		for(ItemStack craftingStack : MTSRegistry.getMaterials(currentItem)){
			if(mouseX > guiLeft + stackOffset && mouseX < guiLeft + stackOffset + itemTooltipBounds && mouseY > guiTop + 172 && mouseY < guiTop + 172 + itemTooltipBounds){
				ItemStack renderedStack = new ItemStack(craftingStack.getItem(), craftingStack.getCount(), craftingStack.getMetadata());
				renderToolTip(renderedStack, guiLeft + stackOffset,  guiTop + 172);
			}
			stackOffset += 18;
		}
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		
		//If we are for 2D components, render the 2D item and be done.
		if(!bench.renderType.isFor3DModels){
			GL11.glPushMatrix();
			GL11.glTranslatef(guiLeft + 172.5F, guiTop + 82.5F, 0);
			GL11.glScalef(3, 3, 3);
			this.itemRender.renderItemAndEffectIntoGUI(new ItemStack(currentItem), 0, 0);
			GL11.glPopMatrix();
			return;
		}
		
		//Parse the model if we haven't already.
		if(!partDisplayLists.containsKey(currentItem)){
			if(bench.renderType.isForVehicles){
				String genericName = ((JSONVehicle) currentItem.definition).genericName;
				//Check to make sure we haven't parsed this model for another item with another texture but same model.
				for(AItemPack<? extends AJSONItem<?>> parsedItem : partDisplayLists.keySet()){
					if(parsedItem instanceof ItemVehicle){
						if(((ItemVehicle) parsedItem).definition.genericName.equals(genericName)){
							partDisplayLists.put(currentItem, partDisplayLists.get(parsedItem));
							partScalingFactors.put(currentItem, partScalingFactors.get(parsedItem));
							break;
						}
					}
				}
				
				//If we didn't find an existing model, parse one now.
				if(!partDisplayLists.containsKey(currentItem)){
					parseModel(currentItem.definition.packID, "objmodels/vehicles/" + genericName + ".obj");
				}
			}else if(bench.renderType.isFor3DModels){
				String customModel = ((AJSONMultiModel<?>.General) currentItem.definition.general).modelName;
				if(customModel != null){
					if(currentItem instanceof AItemPart){
						parseModel(currentItem.definition.packID, "objmodels/parts/" + customModel + ".obj");
					}else{
						parseModel(currentItem.definition.packID, "objmodels/decors/" + customModel + ".obj");
					}
				}else{
					if(currentItem instanceof AItemPart){
						parseModel(currentItem.definition.packID, "objmodels/parts/" + currentItem.definition.systemName + ".obj");
					}else{
						parseModel(currentItem.definition.packID, "objmodels/decors/" + currentItem.definition.systemName + ".obj");
					}
				}
			}
		}
		
		//Cache the texture mapping if we haven't seen this part before.
		if(!textureMap.containsKey(currentItem)){
			final ResourceLocation partTextureLocation;
			if(bench.renderType.isForVehicles){
				partTextureLocation = new ResourceLocation(currentItem.definition.packID, "textures/vehicles/" + currentItem.definition.systemName + ".png");
			}else if(currentItem instanceof AItemPart){
				partTextureLocation = new ResourceLocation(currentItem.definition.packID, "textures/parts/" + currentItem.definition.systemName + ".png");
			}else{
				partTextureLocation = new ResourceLocation(currentItem.definition.packID, "textures/decors/" + currentItem.definition.systemName + ".png");
			}
			textureMap.put(currentItem, partTextureLocation);
		}
		
		//Render the part in the GUI.
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_LIGHTING);
		mc.getTextureManager().bindTexture(textureMap.get(currentItem));
		GL11.glTranslatef(guiLeft + 190, guiTop + 110, 100);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glRotatef(45, 0, 1, 0);
		GL11.glRotatef(35.264F, 1, 0, 1);
		GL11.glRotatef(-player.world.getTotalWorldTime()*2, 0, 1, 0);
		float scale = 30F*partScalingFactors.get(currentItem);
		GL11.glScalef(scale, scale, scale);
		GL11.glCallList(partDisplayLists.get(currentItem));
		GL11.glPopMatrix();
		
		
		
	}
	
	private void renderVehicleInfoText(){
		JSONVehicle vehicleDefinition = (JSONVehicle) currentItem.definition;
		byte controllers = 0;
		byte passengers = 0;
		byte cargo = 0;
		byte mixed = 0;
		float minFuelConsumption = 99;
		float maxFuelConsumption = 0;
		float minWheelSize = 99;
		float maxWheelSize = 0;
		for(VehiclePart part : vehicleDefinition.parts){
			if(part.isController){
				++controllers;
			}else{
				boolean canAcceptSeat = false;
				boolean canAcceptChest = false;
				if(part.types.contains("seat")){
					canAcceptSeat = true;
				}
				if(part.types.contains("crate")){
					canAcceptChest = true;
				}
				if(canAcceptSeat && !canAcceptChest){
					++passengers;
				}else if(canAcceptChest && !canAcceptSeat){
					++cargo;
				}else if(canAcceptChest && canAcceptSeat){
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
		
		if(minFuelConsumption == 99){
			minFuelConsumption = 0;
			maxFuelConsumption = 99;
		}
		
		if(minWheelSize == 99){
			minWheelSize = 0;
			maxWheelSize = 99;
		}
		
		List<String> headerLines = new ArrayList<String>();
		headerLines.add(I18n.format("gui.vehicle_bench.type") + ":");
		headerLines.add(I18n.format("gui.vehicle_bench.weight") + ":");
		headerLines.add(I18n.format("gui.vehicle_bench.fuel") + ":");
		headerLines.add(I18n.format("gui.vehicle_bench.controllers") + ":");
		headerLines.add(I18n.format("gui.vehicle_bench.passengers") + ":");
		headerLines.add(I18n.format("gui.vehicle_bench.cargo") + ":");
		headerLines.add(I18n.format("gui.vehicle_bench.mixed") + ":");
		headerLines.add(I18n.format("gui.vehicle_bench.engine") + ":");
		headerLines.add(I18n.format("gui.vehicle_bench.wheel") + ":");
		
		int lineOffset = 55;
		for(String line : headerLines){
			mc.fontRenderer.drawStringWithShadow(line, guiLeft + 10, guiTop + lineOffset, Color.WHITE.getRGB());
			lineOffset += 10;
		}
		
		List<String> descriptiveLines = new ArrayList<String>();
		descriptiveLines.add(String.valueOf(vehicleDefinition.general.type));
		descriptiveLines.add(String.valueOf(vehicleDefinition.general.emptyMass));
		descriptiveLines.add(String.valueOf(vehicleDefinition.motorized.fuelCapacity));
		descriptiveLines.add(String.valueOf(controllers));
		descriptiveLines.add(String.valueOf(passengers));
		descriptiveLines.add(String.valueOf(cargo));
		descriptiveLines.add(String.valueOf(mixed));
		descriptiveLines.add(String.valueOf(minFuelConsumption) + "-" + String.valueOf(maxFuelConsumption));
		descriptiveLines.add(String.valueOf(minWheelSize) + "-" + String.valueOf(maxWheelSize));
		lineOffset = 55;
		for(String line : descriptiveLines){
			mc.fontRenderer.drawStringWithShadow(line, guiLeft + 90, guiTop + lineOffset, Color.WHITE.getRGB());
			lineOffset += 10;
		}

		GL11.glPushMatrix();
		GL11.glTranslatef(guiLeft + 255, guiTop + 55, 0);
		GL11.glScalef(0.8F, 0.8F, 0.8F);
		fontRenderer.drawSplitString(vehicleDefinition.general.description != null ? vehicleDefinition.general.description : "No description for this vehicle.", 0, 0, 120, Color.WHITE.getRGB());
		GL11.glPopMatrix();
	}
	
	private void renderInfoText(){
		ItemStack tempStack = new ItemStack(currentItem);
		tempStack.setTagCompound(new NBTTagCompound());
		List<String> descriptiveLines = new ArrayList<String>();
		tempStack.getItem().addInformation(tempStack, player.world, descriptiveLines, ITooltipFlag.TooltipFlags.NORMAL);
		int lineOffset = 55;
		for(String line : descriptiveLines){
			mc.fontRenderer.drawStringWithShadow(line, guiLeft + 10, guiTop + lineOffset, Color.WHITE.getRGB());
			lineOffset += 10;
		}
	}
    
	private void parseModel(String partPack, String partModelLocation){
		float minX = 999;
		float maxX = -999;
		float minY = 999;
		float maxY = -999;
		float minZ = 999;
		float maxZ = -999;
		Map<String, Float[][]> parsedModel = OBJParserSystem.parseOBJModel(partPack, partModelLocation);
		int displayListIndex = GL11.glGenLists(1);
		GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
			for(Float[] vertex : entry.getValue()){
				GL11.glTexCoord2f(vertex[3], vertex[4]);
				GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
				GL11.glVertex3f(-vertex[0], vertex[1], vertex[2]);
				minX = Math.min(minX, vertex[0]);
				maxX = Math.max(maxX, vertex[0]);
				minY = Math.min(minY, vertex[1]);
				maxY = Math.max(maxY, vertex[1]);
				minZ = Math.min(minZ, vertex[2]);
				maxZ = Math.max(maxZ, vertex[2]);
			}
		}
		float globalMax = Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
		partScalingFactors.put(currentItem, globalMax > 1.5 ? 1.5F/globalMax : 1.0F);
		GL11.glEnd();
		GL11.glEndList();
		partDisplayLists.put(currentItem, displayListIndex);
	}
	
	@Override
    protected void actionPerformed(GuiButton buttonClicked) throws IOException{
		super.actionPerformed(buttonClicked);
		if(buttonClicked.equals(startButton)){
			MTS.proxy.playSound(player.getPositionVector(), MTS.MODID + ":bench_running", 1, 1);
			MTS.MTSNet.sendToServer(new PacketPlayerCrafting(player, currentItem));
		}else{
			if(buttonClicked.equals(leftPackButton)){
				currentPack = prevPack;
				currentItem = null;
			}else if(buttonClicked.equals(rightPackButton)){
				currentPack = nextPack;
				currentItem = null;
			}else if(buttonClicked.equals(leftPartButton)){
				currentItem = prevItem;
			}else if(buttonClicked.equals(rightPartButton)){
				currentItem = nextItem;
			}else if(buttonClicked.equals(leftColorButton)){
				currentItem = prevSubItem;
			}else if(buttonClicked.equals(rightColorButton)){
				currentItem = nextSubItem;
			}
			updatePartNames();
		}
	}
	
	/**
	 * We also use the mouse wheel for selections as well as buttons.
	 * Forward the call to the button input system for processing.
	 */
	@Override
    public void handleMouseInput() throws IOException{
        super.handleMouseInput();
        int i = Mouse.getEventDWheel();
        if(i > 0 && rightPartButton.enabled){
        	this.actionPerformed(rightPartButton);
        }else if(i < 0 && leftPartButton.enabled){
        	this.actionPerformed(leftPartButton);
        }
	}
	
	@Override
    public void onGuiClosed(){
		//Clear out the displaylists to free RAM once we no longer need them here.
		for(int displayListID : partDisplayLists.values()){
			GL11.glDeleteLists(displayListID, 1);
		}
		
		//Save the last clicked part for reference later.
		lastOpenedItem.put(bench, currentItem);
    }
	
	@Override
	public boolean doesGuiPauseGame(){
		return false;
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException{
		if(keyCode == 1 || mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)){
			super.keyTyped('0', 1);
        }
	}
	
	private void drawCenteredString(String stringToDraw, int x, int y){
		mc.fontRenderer.drawString(stringToDraw, x - mc.fontRenderer.getStringWidth(stringToDraw)/2, y, 4210752);
	}
	
	/**
	 * Loop responsible for updating pack/part names whenever an action occurs.
	 * Looks through all items in the list that was passed-in on GUI construction time and
	 * uses the order to determine which pack/item to scroll to when a button is clicked.
	 * Sets the variables to be used on a button action, so once an action is performed this
	 * logic MUST be called to update the button action states!
	 */
	private void updatePartNames(){
		//Set back indexes.
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
		//If currentItem is null, it means we swtiched packs and need to re-set it to the first item of the new pack.
		//Do so now before we do looping to prevent crashes.
		//Find a pack that has the item we are supposed to craft and set it.
		//If we are for vehicles, make sure to set the next subItem if we can.
		if(currentItem == null){
			for(AItemPack<? extends AJSONItem<?>> packItem : MTSRegistry.packItemMap.get(currentPack).values()){
				if(currentItem == null || (bench.renderType.isForVehicles && nextSubItem == null)){
					if(bench.isJSONValid(packItem.definition)){
						if(currentItem == null){
							currentItem = packItem;
							currentItemIndex = packItems.indexOf(currentItem);
						}else if(bench.renderType.isForVehicles && nextSubItem == null){
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
					if(bench.renderType.isForVehicles){
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
			for(int i=currentItemIndex-1; i>=0 && (prevItem == null || bench.renderType.isForVehicles); --i){
				if(bench.isJSONValid(packItems.get(i).definition)){
					//If we are for vehicles, and we didn't switch items, and this item
					//is the same sub-item classification, set prevSubItem and continue on.
					//If we did switch, we want the first subItem in the set of items to
					//be the prevItem we pick.  This ensures when we switch we'll be on the 
					//same subItem each time we switch items.
					if(bench.renderType.isForVehicles){
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
	}
}
