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
import minecrafttransportsimulator.jsondefs.PackVehicleObject;
import minecrafttransportsimulator.jsondefs.PackVehicleObject.PackPart;
import minecrafttransportsimulator.packets.general.PacketPlayerCrafting;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GUIPartBench extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation(MTS.MODID, "textures/guis/crafting.png");	
	private static final Map<String, String[]> lastOpenedItem = new HashMap<String, String[]>();
	
	private final List<String> partTypes;
	private final EntityPlayer player;
	private final boolean isForVehicles;
	private final boolean isForInstruments;
	private final boolean isForItems;
	private final Map<String, ? extends Item> itemMap;
	
	private GuiButton leftPackButton;
	private GuiButton rightPackButton;
	private GuiButton leftPartButton;
	private GuiButton rightPartButton;
	private GuiButton leftColorButton;
	private GuiButton rightColorButton;
	private GuiButton startButton;
	
	private int guiLeft;
	private int guiTop;
	
	private String packName = "";
	private String prevPackName = "";
	private String nextPackName = "";
	
	private String partName = "";
	private String prevPartName = "";
	private String nextPartName = "";
	
	private String colorName = "";
	private String prevColorName = "";
	private String nextColorName = "";
	
	/**Display list GL integers.  Keyed by part name.*/
	private final Map<String, Integer> partDisplayLists = new HashMap<String, Integer>();
	private final Map<String, Float> partScalingFactors = new HashMap<String, Float>();
	
	/**Part texture name.  Keyed by part name.*/
	private final Map<String, ResourceLocation> textureMap = new HashMap<String, ResourceLocation>();
	
	public GUIPartBench(BlockBench bench, EntityPlayer player){
		this.partTypes = bench.partTypes;
		this.player = player;
		this.isForVehicles = this.partTypes.contains("plane") || this.partTypes.contains("car");
		this.isForInstruments = this.partTypes.contains("instrument");
		this.isForItems = this.partTypes.contains("item");
		this.itemMap = isForVehicles ? MTSRegistry.vehicleItemMap : (isForInstruments ? MTSRegistry.instrumentItemMap : (isForItems ? MTSRegistry.itemItemMap :MTSRegistry.partItemMap));
		if(lastOpenedItem.containsKey(bench.partTypes.get(0))){
			packName = lastOpenedItem.get(bench.partTypes.get(0))[0];
			partName = lastOpenedItem.get(bench.partTypes.get(0))[1];
		}
		updatePartNames();
	}
	
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = this.isForVehicles ? (this.width - 356)/2 : (this.width - 256)/2;
		guiTop = (this.height - 220)/2;
		
		buttonList.add(leftPackButton = new GuiButton(0, guiLeft + 10, guiTop + 5, 20, 20, "<"));
		buttonList.add(rightPackButton = new GuiButton(0, guiLeft + 226, guiTop + 5, 20, 20, ">"));
		buttonList.add(leftPartButton = new GuiButton(0, guiLeft + 10, guiTop + 25, 20, 20, "<"));
		buttonList.add(rightPartButton = new GuiButton(0, guiLeft + 226, guiTop + 25, 20, 20, ">"));
		if(isForVehicles){
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
		if(this.isForVehicles){
			drawTexturedModalRect(guiLeft + 250, guiTop, 144, 0, 111, 201);
		}
		
		//If we can make this part, draw the start arrow.
		if(startButton.enabled){
			drawTexturedModalRect(guiLeft + 140, guiTop + 173, 0, 201, 44, 16);
		}
		
		//Render the text headers.
		drawCenteredString(!packName.isEmpty() ? I18n.format("itemGroup." + packName) : "", guiLeft + 130, guiTop + 10);
		drawCenteredString(!partName.isEmpty() ? I18n.format(itemMap.get(partName).getUnlocalizedName() + ".name") : "", guiLeft + 130, guiTop + 30);
		if(this.isForVehicles){
			drawCenteredString(I18n.format("gui.vehicle_bench.color"), guiLeft + 300, guiTop + 10);
		}
		
		//Set button states and render.
		startButton.enabled = PacketPlayerCrafting.doesPlayerHaveMaterials(player, partName);
		leftPackButton.enabled = !prevPackName.isEmpty();
		rightPackButton.enabled = !nextPackName.isEmpty();
		if(this.isForVehicles){
			//If we are for vehicles, don't enable the part button if there's not a part that doesn't match the color.
			//We need to enable the color button instead for that.
			leftPartButton.enabled = !prevPartName.isEmpty();
			rightPartButton.enabled = !nextPartName.isEmpty();
			leftColorButton.enabled = !prevColorName.isEmpty();
			rightColorButton.enabled = !nextColorName.isEmpty();
		}else{
			leftPartButton.enabled = !prevPartName.isEmpty();
			rightPartButton.enabled = !nextPartName.isEmpty();
		}
		for(Object obj : buttonList){
			((GuiButton) obj).drawButton(mc, mouseX, mouseY, 0);
		}
		this.drawRect(guiLeft + 190, guiTop + 188, guiLeft + 206, guiTop + 172, startButton.enabled ? Color.GREEN.getRGB() : Color.RED.getRGB());
		
		//If we don't have any parts of this type, don't do anything else.
		if(partName.isEmpty()){
			return;
		}
		
		//Render descriptive text.
		if(this.isForVehicles){
			renderVehicleInfoText();
		}else if(!this.isForInstruments){
			renderPartInfoText();
		}else{
			renderInstrumentInfoText();
		}
		
		//Render materials in the bottom slots.
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.enableGUIStandardItemLighting();
		int stackOffset = 9;
		for(ItemStack craftingStack : PackParserSystem.getMaterials(partName)){
			ItemStack renderedStack = new ItemStack(craftingStack.getItem(), craftingStack.getCount(), craftingStack.getMetadata());
			this.itemRender.renderItemAndEffectIntoGUI(renderedStack, guiLeft + stackOffset, guiTop + 172);
			this.itemRender.renderItemOverlays(fontRenderer, renderedStack, guiLeft + stackOffset, guiTop + 172);
			stackOffset += 18;
		}
		
		//We render the text afterwards to ensure it doesn't render behind the items.
		stackOffset = 9;
		int itemTooltipBounds = 16;
		for(ItemStack craftingStack : PackParserSystem.getMaterials(partName)){
			if(mouseX > guiLeft + stackOffset && mouseX < guiLeft + stackOffset + itemTooltipBounds && mouseY > guiTop + 172 && mouseY < guiTop + 172 + itemTooltipBounds){
				ItemStack renderedStack = new ItemStack(craftingStack.getItem(), craftingStack.getCount(), craftingStack.getMetadata());
				renderToolTip(renderedStack, guiLeft + stackOffset,  guiTop + 172);
			}
			stackOffset += 18;
		}
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		
		//If we are for instruments or items, render the 2D item and be done.
		if(this.isForInstruments || this.isForItems){
			GL11.glPushMatrix();
			GL11.glTranslatef(guiLeft + 172.5F, guiTop + 82.5F, 0);
			GL11.glScalef(3, 3, 3);
			this.itemRender.renderItemAndEffectIntoGUI(new ItemStack(itemMap.get(partName)), 0, 0);
			GL11.glPopMatrix();
			return;
		}
		
		//Parse the model if we haven't already.
		if(!partDisplayLists.containsKey(partName)){
			if(this.isForVehicles){
				String jsonName = PackParserSystem.getVehicleJSONName(partName);
				//Check to make sure we haven't parsed this model for another item with another texture but same model.
				for(String parsedItemName : partDisplayLists.keySet()){
					if(PackParserSystem.getVehicleJSONName(parsedItemName).equals(jsonName)){
						partDisplayLists.put(partName, partDisplayLists.get(parsedItemName));
						partScalingFactors.put(partName, partScalingFactors.get(parsedItemName));
						break;
					}
				}
				
				//If we didn't find an existing model, parse one now.
				if(!partDisplayLists.containsKey(partName)){
					parseModel(partName.substring(0, partName.indexOf(':')), "objmodels/vehicles/" + jsonName + ".obj");
				}
			}else if(!this.isForItems){
				if(PackParserSystem.getPartPack(partName).general.modelName != null){
					parseModel(partName.substring(0, partName.indexOf(':')), "objmodels/parts/" + PackParserSystem.getPartPack(partName).general.modelName + ".obj");
				}else{
					parseModel(partName.substring(0, partName.indexOf(':')), "objmodels/parts/" + partName.substring(partName.indexOf(':') + 1) + ".obj");
				}
			}
		}
		
		//Cache the texture mapping if we haven't seen this part before.
		if(!textureMap.containsKey(partName)){
			final ResourceLocation partTextureLocation;
			if(this.isForVehicles){
				partTextureLocation = new ResourceLocation(partName.substring(0, partName.indexOf(':')), "textures/vehicles/" + partName.substring(partName.indexOf(':') + 1) + ".png");
			}else{
				partTextureLocation = new ResourceLocation(partName.substring(0, partName.indexOf(':')), "textures/parts/" + partName.substring(partName.indexOf(':') + 1) + ".png");
			}
			textureMap.put(partName, partTextureLocation);
		}
		
		//Render the part in the GUI.
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_LIGHTING);
		mc.getTextureManager().bindTexture(textureMap.get(partName));
		GL11.glTranslatef(guiLeft + 190, guiTop + 110, 100);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glRotatef(45, 0, 1, 0);
		GL11.glRotatef(35.264F, 1, 0, 1);
		GL11.glRotatef(-player.world.getTotalWorldTime()*2, 0, 1, 0);
		float scale = 30F*partScalingFactors.get(partName);
		GL11.glScalef(scale, scale, scale);
		GL11.glCallList(partDisplayLists.get(partName));
		GL11.glPopMatrix();
		
		
		
	}
	
	private void renderVehicleInfoText(){
		PackVehicleObject pack = PackParserSystem.getVehiclePack(partName);
		byte controllers = 0;
		byte passengers = 0;
		byte cargo = 0;
		byte mixed = 0;
		float minFuelConsumption = 99;
		float maxFuelConsumption = 0;
		float minWheelSize = 99;
		float maxWheelSize = 0;
		for(PackPart part : pack.parts){
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
		descriptiveLines.add(String.valueOf(pack.general.type));
		descriptiveLines.add(String.valueOf(pack.general.emptyMass));
		descriptiveLines.add(String.valueOf(pack.motorized.fuelCapacity));
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
		fontRenderer.drawSplitString(I18n.format("description." + partName.substring(0, partName.indexOf(':')) + "." + PackParserSystem.getVehicleJSONName(partName)), 0, 0, 120, Color.WHITE.getRGB());
		GL11.glPopMatrix();
	}
	
	private void renderPartInfoText(){
		ItemStack tempStack = new ItemStack(itemMap.get(partName));
		tempStack.setTagCompound(new NBTTagCompound());
		List<String> descriptiveLines = new ArrayList<String>();
		tempStack.getItem().addInformation(tempStack, player.world, descriptiveLines, ITooltipFlag.TooltipFlags.NORMAL);
		int lineOffset = 55;
		for(String line : descriptiveLines){
			mc.fontRenderer.drawStringWithShadow(line, guiLeft + 10, guiTop + lineOffset, Color.WHITE.getRGB());
			lineOffset += 10;
		}
	}
	
	private void renderInstrumentInfoText(){
		fontRenderer.drawSplitString(I18n.format(itemMap.get(partName).getUnlocalizedName() + ".description"), guiLeft + 10, guiTop + 55, 120, Color.WHITE.getRGB());
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
		partScalingFactors.put(partName, globalMax > 1.5 ? 1.5F/globalMax : 1.0F);
		GL11.glEnd();
		GL11.glEndList();
		partDisplayLists.put(partName, displayListIndex);
	}
	
	@Override
    protected void actionPerformed(GuiButton buttonClicked) throws IOException{
		super.actionPerformed(buttonClicked);
		if(buttonClicked.equals(startButton)){
			MTS.proxy.playSound(player.getPositionVector(), MTS.MODID + ":bench_running", 1, 1);
			MTS.MTSNet.sendToServer(new PacketPlayerCrafting(player, partName));
		}else{
			if(buttonClicked.equals(leftPackButton)){
				packName = prevPackName;
				partName = "";
			}else if(buttonClicked.equals(rightPackButton)){
				packName = nextPackName;
				partName = "";
			}else if(buttonClicked.equals(leftPartButton)){
				partName = prevPartName;
			}else if(buttonClicked.equals(rightPartButton)){
				partName = nextPartName;
			}else if(buttonClicked.equals(leftColorButton)){
				partName = prevColorName;
			}else if(buttonClicked.equals(rightColorButton)){
				partName = nextColorName;
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
		lastOpenedItem.put(partTypes.get(0), new String[]{packName, partName});
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
		//Set all prev/next variables blank before executing the loop.
		prevPackName = "";
		nextPackName = "";	
		prevPartName = "";
		nextPartName = "";
		prevColorName = "";
		nextColorName = "";
		
		boolean passedPack = false;
		boolean passedPart = false;
		boolean passedColor = false;
		for(String partItemName : itemMap.keySet()){
			//First check to make sure this item matches the type on the bench.
			final boolean isValid;
			if(this.isForVehicles){
				isValid = partTypes.contains(PackParserSystem.getVehiclePack(partItemName).general.type);
			}else if(!this.isForInstruments && !this.isForItems){
				isValid = partTypes.contains(PackParserSystem.getPartPack(partItemName).general.type);
			}else{
				isValid = true;
			}
			if(isValid){
				//If packName is empty, set it now.  Otherwise, if we haven't seen the pack for the
				//currently-selected item, and the pack is different than the item, set the prevPack.
				//This will continue to be set until we see the same pack as the current item, so it
				//will be the pack of the closest item in the list with a different pack.
				if(packName.isEmpty()){
					packName = partItemName.substring(0, partItemName.indexOf(':'));
				}else if(!passedPack && !partItemName.startsWith(packName)){
					prevPackName = partItemName.substring(0, partItemName.indexOf(':'));
				}
				if(partItemName.startsWith(packName)){
					//Set the variable to show we are in the same pack.
					//Subsequent logic is executed to set other variables, and differs depending on
					//if this GUI is for vehicles.  This is done because vehicles have a color selector
					//so even though the pack might be the same, the vehicle may just be 6 colors rather
					//that 6 unique vehicles.  This keeps packs with 16-colors of sedans from taking
					//up 100s of part slots.
					passedPack = true;
					if(this.isForVehicles){
						if(partName.isEmpty()){
							partName = partItemName;
							passedPart = true;
						}else if(partName.equals(partItemName)){
							passedPart = true;
						}else if(!passedPart){
							//Set the prevColorName if the part is the same color (JSON) as the current part.
							//Set the prevPartName only if the part is a different color (JSON) than the current part.
							if(PackParserSystem.getVehicleJSONName(partName).equals(PackParserSystem.getVehicleJSONName(partItemName))){
								prevColorName = partItemName;
							}else{
								//For prevPartName, we want only the first part that is different.
								//Once we have this, we don't set it again unless the JSON differs.
								//This ensures that when we hit the prevPart button, the color is always
								//the first color in the set and the right button is lit for subsequent colors.
								if(prevPartName.isEmpty() || !PackParserSystem.getVehicleJSONName(prevPartName).equals(PackParserSystem.getVehicleJSONName(partItemName))){
									prevPartName = partItemName;
								}
							}
						}else if(nextPartName.isEmpty()){
							//Set the nextColorName if the part is the same color (JSON) as the current part.
							//Set the nextPartName only if the part is a different color (JSON) than the current part.
							if(PackParserSystem.getVehicleJSONName(partName).equals(PackParserSystem.getVehicleJSONName(partItemName))){
								//Need this here to prevent the color from being overwritten by further colors.
								if(nextColorName.isEmpty()){
									nextColorName = partItemName;
								}
							}else{
								nextPartName = partItemName;
							}
						}
					}else{
						if(partName.isEmpty()){
							partName = partItemName;
							passedPart = true;
						}else if(partName.equals(partItemName)){
							passedPart = true;
						}else if(!passedPart){
							prevPartName = partItemName;
						}else if(nextPartName.isEmpty()){
							nextPartName = partItemName;
						}
					}
				}else if(nextPackName.isEmpty() && passedPack){
					//Since we've passed our pack, and this item isn't the same as our pack, it must be an item from the next pack.
					//Set the variable and then return, as we don't need to iterate any more as everything is set.
					nextPackName = partItemName.substring(0, partItemName.indexOf(':'));
					return;
				}
			}
		}
	}
}
