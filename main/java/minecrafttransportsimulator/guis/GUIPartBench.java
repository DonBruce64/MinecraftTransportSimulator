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
import minecrafttransportsimulator.dataclasses.PackVehicleObject;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.packets.general.PacketPlayerCrafting;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
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
	private final Map<String, ? extends Item> itemMap;
	
	private GuiButton leftPackButton;
	private GuiButton rightPackButton;
	private GuiButton leftPartButton;
	private GuiButton rightPartButton;
	private GuiButton startButton;
	
	private int guiLeft;
	private int guiTop;
	
	private String packName = "";
	private String prevPackName = "";
	private String nextPackName = "";
	
	private String partName = "";
	private String prevPartName = "";
	private String nextPartName = "";
	
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
		this.itemMap = isForVehicles ? MTSRegistry.vehicleItemMap : (isForInstruments ? MTSRegistry.instrumentItemMap : MTSRegistry.partItemMap);
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
		
		buttonList.add(leftPackButton = new GuiButton(0, guiLeft + 25, guiTop + 5, 20, 20, "<"));
		buttonList.add(rightPackButton = new GuiButton(0, guiLeft + 215, guiTop + 5, 20, 20, ">"));
		buttonList.add(leftPartButton = new GuiButton(0, guiLeft + 25, guiTop + 25, 20, 20, "<"));
		buttonList.add(rightPartButton = new GuiButton(0, guiLeft + 215, guiTop + 25, 20, 20, ">"));
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
		
		//Set button states and render.
		startButton.enabled = PacketPlayerCrafting.doesPlayerHaveMaterials(player, partName);
		leftPackButton.enabled = !prevPackName.isEmpty();
		rightPackButton.enabled = !nextPackName.isEmpty();
		leftPartButton.enabled = !prevPartName.isEmpty();
		rightPartButton.enabled = !nextPartName.isEmpty();
		for(Object obj : buttonList){
			((GuiButton) obj).drawButton(mc, mouseX, mouseY);
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
			ItemStack renderedStack = new ItemStack(craftingStack.getItem(), craftingStack.stackSize, craftingStack.getMetadata() == Integer.MAX_VALUE ? 0 : craftingStack.getMetadata());
			this.itemRender.renderItemAndEffectIntoGUI(renderedStack, guiLeft + stackOffset, guiTop + 172);
			this.itemRender.renderItemOverlays(fontRendererObj, renderedStack, guiLeft + stackOffset, guiTop + 172);
			stackOffset += 18;
		}
		
		//If we are for instruments, render the 2D item and be done.
		if(this.isForInstruments){
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
			}else{
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
		GL11.glRotatef(-player.worldObj.getTotalWorldTime()*2, 0, 1, 0);
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
			}
		}
		
		List<String> headerLines = new ArrayList<String>();
		headerLines.add(I18n.format("gui.vehicle_bench.type") + ":");
		headerLines.add(I18n.format("gui.vehicle_bench.weight") + ":");
		headerLines.add(I18n.format("gui.vehicle_bench.fuel") + ":");
		headerLines.add(I18n.format("gui.vehicle_bench.controllers") + ":");
		headerLines.add(I18n.format("gui.vehicle_bench.passengers") + ":");
		headerLines.add(I18n.format("gui.vehicle_bench.cargo") + ":");
		headerLines.add(I18n.format("gui.vehicle_bench.mixed") + ":");
		int lineOffset = 55;
		for(String line : headerLines){
			mc.fontRendererObj.drawStringWithShadow(line, guiLeft + 10, guiTop + lineOffset, Color.WHITE.getRGB());
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
		lineOffset = 55;
		for(String line : descriptiveLines){
			mc.fontRendererObj.drawStringWithShadow(line, guiLeft + 90, guiTop + lineOffset, Color.WHITE.getRGB());
			lineOffset += 10;
		}

		GL11.glPushMatrix();
		GL11.glTranslatef(guiLeft + 255, guiTop + 55, 0);
		GL11.glScalef(0.8F, 0.8F, 0.8F);
		fontRendererObj.drawSplitString(I18n.format("description." + partName.substring(0, partName.indexOf(':')) + "." + PackParserSystem.getVehicleJSONName(partName)), 0, 0, 120, Color.WHITE.getRGB());
		GL11.glPopMatrix();
	}
	
	private void renderPartInfoText(){
		ItemStack tempStack = new ItemStack(itemMap.get(partName));
		tempStack.setTagCompound(new NBTTagCompound());
		List<String> descriptiveLines = new ArrayList<String>();
		tempStack.getItem().addInformation(tempStack, player, descriptiveLines, false);
		int lineOffset = 55;
		for(String line : descriptiveLines){
			mc.fontRendererObj.drawStringWithShadow(line, guiLeft + 10, guiTop + lineOffset, Color.WHITE.getRGB());
			lineOffset += 10;
		}
	}
	
	private void renderInstrumentInfoText(){
		fontRendererObj.drawSplitString(I18n.format(itemMap.get(partName).getUnlocalizedName() + ".description"), guiLeft + 10, guiTop + 55, 120, Color.WHITE.getRGB());
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
			mc.thePlayer.closeScreen();
			return;
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
			}
			updatePartNames();
		}
	}
	
	
	/**
	 * We also use the mouse wheel for selections as well as buttons.
	 */
	@Override
    public void handleMouseInput() throws IOException{
        super.handleMouseInput();
        int i = Mouse.getEventDWheel();
        if(i > 0 && rightPartButton.enabled){
        	partName = nextPartName;
        	updatePartNames();
        }else if(i < 0 && leftPartButton.enabled){
        	partName = prevPartName;
			updatePartNames();
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
		mc.fontRendererObj.drawString(stringToDraw, x - mc.fontRendererObj.getStringWidth(stringToDraw)/2, y, 4210752);
	}
	
	private void updatePartNames(){
		prevPackName = "";
		nextPackName = "";	
		prevPartName = "";
		nextPartName = "";
		
		boolean passedPack = false;
		boolean passedPart = false;
		for(String partItemName : itemMap.keySet()){
			final boolean isValid;
			if(this.isForVehicles){
				isValid = partTypes.contains(PackParserSystem.getVehiclePack(partItemName).general.type);
			}else if(!this.isForInstruments){
				isValid = partTypes.contains(PackParserSystem.getPartPack(partItemName).general.type);
			}else{
				isValid = true;
			}
			if(isValid){
				if(packName.isEmpty()){
					packName = partItemName.substring(0, partItemName.indexOf(':'));
				}else if(!passedPack && !partItemName.startsWith(packName)){
					prevPackName = partItemName.substring(0, partItemName.indexOf(':'));
				}
				if(partItemName.startsWith(packName)){
					passedPack = true;
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
				}else if(nextPackName.isEmpty() && passedPack){
					nextPackName = partItemName.substring(0, partItemName.indexOf(':'));
				}
			}
		}
	}
}
