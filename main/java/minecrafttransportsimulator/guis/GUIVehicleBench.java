package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackMultipartObject;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.packets.general.PlayerCraftingPacket;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class GUIVehicleBench extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation(MTS.MODID, "textures/guis/crafting_parts.png");	
	private final EntityPlayer player;
	
	private GuiButton leftPackButton;
	private GuiButton rightPackButton;
	private GuiButton leftVehicleButton;
	private GuiButton rightVehicleButton;
	private GuiButton startButton;
	
	private int guiLeft;
	private int guiTop;
	
	private String packName = "";
	private String prevPackName = "";
	private String nextPackName = "";
	
	private String vehicleName = "";
	private String prevVehicleName = "";
	private String nextVehicleName = "";
	
	/**Display list GL integers.  Keyed by JSON name.*/
	private final Map<String, Integer> vehicleDisplayLists = new HashMap<String, Integer>();
	private final Map<String, Float> vehicleScalingFactor = new HashMap<String, Float>();
	
	/**Vehicle texture name.  Keyed by vehicle name.*/
	private final Map<String, ResourceLocation> textureMap = new HashMap<String, ResourceLocation>();
	
	public GUIVehicleBench(EntityPlayer player){
		this.player = player;
		updateVehicleNames();
	}
	
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (this.width - 356)/2;
		guiTop = (this.height - 201)/2;
		
		buttonList.add(leftPackButton = new GuiButton(0, guiLeft + 25, guiTop + 5, 20, 20, "<"));
		buttonList.add(rightPackButton = new GuiButton(0, guiLeft + 215, guiTop + 5, 20, 20, ">"));
		buttonList.add(leftVehicleButton = new GuiButton(0, guiLeft + 25, guiTop + 25, 20, 20, "<"));
		buttonList.add(rightVehicleButton = new GuiButton(0, guiLeft + 215, guiTop + 25, 20, 20, ">"));
		buttonList.add(startButton = new GuiButton(0, guiLeft + 188, guiTop + 170, 20, 20, ""));
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		super.drawScreen(mouseX, mouseY, renderPartialTicks);
		
		//Draw header text, graphics, and buttons.
		GL11.glColor3f(1, 1, 1); //Not sure why buttons make this grey, but whatever...
		mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 256, 201);
		drawTexturedModalRect(guiLeft + 250, guiTop, 144, 0, 111, 201);
		if(startButton.enabled){
			drawTexturedModalRect(guiLeft + 140, guiTop + 173, 0, 201, 44, 16);
		}
		drawCenteredString(!packName.isEmpty() ? I18n.format("itemGroup." + packName) : "", guiLeft + 130, guiTop + 10);
		drawCenteredString(!vehicleName.isEmpty() ? I18n.format(MTSRegistry.multipartItemMap.get(vehicleName).getUnlocalizedName() + ".name") : "", guiLeft + 130, guiTop + 30);
		
		//Render descriptive text.
		PackMultipartObject pack = PackParserSystem.getMultipartPack(vehicleName);
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
		fontRendererObj.drawSplitString(I18n.format("description." + PackParserSystem.getMultipartJSONName(vehicleName)), 0, 0, 120, Color.WHITE.getRGB());
		GL11.glPopMatrix();

		
		
		
		//Set button states and render.
		startButton.enabled = PlayerCraftingPacket.doesPlayerHaveMaterials(player, vehicleName);
		leftPackButton.enabled = !prevPackName.isEmpty();
		rightPackButton.enabled = !nextPackName.isEmpty();
		leftVehicleButton.enabled = !prevVehicleName.isEmpty();
		rightVehicleButton.enabled = !nextVehicleName.isEmpty();
		for(Object obj : buttonList){
			((GuiButton) obj).drawButton(mc, mouseX, mouseY);
		}
		this.drawRect(guiLeft + 190, guiTop + 188, guiLeft + 206, guiTop + 172, startButton.enabled ? Color.GREEN.getRGB() : Color.RED.getRGB());
		
		//Render materials in the bottom slots.
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.enableGUIStandardItemLighting();
		int stackOffset = 9;
		for(ItemStack craftingStack : PackParserSystem.getMaterials(vehicleName)){
			this.itemRender.renderItemAndEffectIntoGUI(craftingStack, guiLeft + stackOffset, guiTop + 172);
			this.itemRender.renderItemOverlays(fontRendererObj, craftingStack, guiLeft + stackOffset, guiTop + 172);
			stackOffset += 18;
		}
		
		//Render the 3D model.  Cache the model if we haven't done so already.
		String jsonName = PackParserSystem.getMultipartJSONName(vehicleName);
		if(!vehicleDisplayLists.containsKey(jsonName)){
			ResourceLocation vehicleModelLocation = new ResourceLocation(vehicleName.substring(0, vehicleName.indexOf(':')), "objmodels/vehicles/" + jsonName + ".obj");
			Map<String, Float[][]> parsedModel = OBJParserSystem.parseOBJModel(vehicleModelLocation);
			int displayListIndex = GL11.glGenLists(1);
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			
			float minX = 999;
			float maxX = -999;
			float minY = 999;
			float maxY = -999;
			float minZ = 999;
			float maxZ = -999;
			for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
				if(!entry.getKey().toLowerCase().contains("window")){
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
			}
			float globalMax = Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
			vehicleScalingFactor.put(jsonName, globalMax > 2 ? 2F/globalMax : 1.0F);
			GL11.glEnd();
			GL11.glEndList();
			vehicleDisplayLists.put(jsonName, displayListIndex);
		}
		if(!textureMap.containsKey(vehicleName)){
			ResourceLocation partTextureLocation = new ResourceLocation(vehicleName.substring(0, vehicleName.indexOf(':')), "textures/vehicles/" + vehicleName.substring(vehicleName.indexOf(':') + 1) + ".png");
			textureMap.put(vehicleName, partTextureLocation);
		}
		
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_LIGHTING);
		mc.getTextureManager().bindTexture(textureMap.get(vehicleName));
		GL11.glTranslatef(guiLeft + 200, guiTop + 110, 100);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glRotatef(45, 0, 1, 0);
		GL11.glRotatef(35.264F, 1, 0, 1);
		GL11.glRotatef(-player.worldObj.getTotalWorldTime()*2, 0, 1, 0);
		float scale = 30F*vehicleScalingFactor.get(jsonName);
		GL11.glScalef(scale, scale, scale);
		GL11.glCallList(vehicleDisplayLists.get(jsonName));
		GL11.glPopMatrix();
		
	}
    
	@Override
    protected void actionPerformed(GuiButton buttonClicked) throws IOException{
		super.actionPerformed(buttonClicked);
		if(buttonClicked.equals(startButton)){
			MTS.proxy.playSound(player.getPositionVector(), MTS.MODID + ":bench_running", 1, 1);
			MTS.MTSNet.sendToServer(new PlayerCraftingPacket(player, vehicleName));
			mc.thePlayer.closeScreen();
			return;
		}else{
			if(buttonClicked.equals(leftPackButton)){
				packName = prevPackName;
				vehicleName = "";
			}else if(buttonClicked.equals(rightPackButton)){
				packName = nextPackName;
				vehicleName = "";
			}else if(buttonClicked.equals(leftVehicleButton)){
				vehicleName = prevVehicleName;
			}else if(buttonClicked.equals(rightVehicleButton)){
				vehicleName = nextVehicleName;
			}
			updateVehicleNames();
		}
	}
	
	@Override
	public boolean doesGuiPauseGame(){
		return false;
	}
	
	@Override
    public void onGuiClosed(){
		//Clear out the displaylists to free RAM once we no longer need them here.
		for(int displayListID : vehicleDisplayLists.values()){
			GL11.glDeleteLists(displayListID, 1);
		}
    }
	
	private void drawCenteredString(String stringToDraw, int x, int y){
		mc.fontRendererObj.drawString(stringToDraw, x - mc.fontRendererObj.getStringWidth(stringToDraw)/2, y, 4210752);
	}
	
	private void updateVehicleNames(){
		prevPackName = "";
		nextPackName = "";	
		prevVehicleName = "";
		nextVehicleName = "";
		
		boolean passedPack = false;
		boolean passedVehicle = false;
		for(String name : MTSRegistry.multipartItemMap.keySet()){
			if(packName.isEmpty()){
				packName = name.substring(0, name.indexOf(':'));
			}else if(!passedPack && !name.startsWith(packName)){
				prevPackName = name.substring(0, name.indexOf(':'));
			}
			if(name.startsWith(packName)){
				passedPack = true;
				if(vehicleName.isEmpty()){
					vehicleName = name;
					passedVehicle = true;
				}else if(vehicleName.equals(name)){
					passedVehicle = true;
				}else if(!passedVehicle){
					prevVehicleName = name;
				}else if(nextVehicleName.isEmpty()){
					nextVehicleName = name;
				}
			}else if(nextPackName.isEmpty() && passedPack){
				nextPackName = name.substring(0, name.indexOf(':'));
			}
		}
	}
}
