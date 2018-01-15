package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.packets.general.PropellerBenchStartPacket;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class GUIPropellerBench extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation(MTS.MODID, "textures/guis/long_blank.png");
	
	private static final ItemStack planksStack = new ItemStack(Blocks.PLANKS);
	private static final ItemStack ingotStack = new ItemStack(Items.IRON_INGOT);
	private static final ItemStack obsidianStack = new ItemStack(Blocks.OBSIDIAN);
	private static final ItemStack redstoneStack = new ItemStack(Items.REDSTONE);
	private static final ItemStack[] propellerStacks = new ItemStack[]{new ItemStack(MTSRegistry.propeller, 1, 0), new ItemStack(MTSRegistry.propeller, 1, 1), new ItemStack(MTSRegistry.propeller, 1, 2)};
	
	private int guiLeft;
	private int guiTop;
	private int propellerMaterialQty;
	
	private GuiButton tier0Button;
	private GuiButton tier1Button;
	private GuiButton tier2Button;
	private GuiButton bladesUpButton;
	private GuiButton bladesDownButton;
	private GuiButton pitchUpButton;
	private GuiButton pitchDownButton;
	private GuiButton diameterUpButton;
	private GuiButton diameterDownButton;
	private GuiButton startButton;
	
	private TileEntityPropellerBench bench;
	private int numberPlayerPlanks;
	private int numberPlayerIronIngots;
	private int numberPlayerObsidian;
	private int numberPlayerRedstone;
	private EntityPlayer player;
	
	public GUIPropellerBench(TileEntityPropellerBench bench, EntityPlayer player){
		this.bench = bench;
		this.player = player;
	}
	
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (this.width - 176)/2;
		guiTop = (this.height - 222)/2;
	
		buttonList.add(tier0Button = new GuiButton(0, guiLeft + 10, guiTop + 10, 20, 20, ""));
		buttonList.add(tier1Button = new GuiButton(0, guiLeft + 40, guiTop + 10, 20, 20, ""));
		buttonList.add(tier2Button = new GuiButton(0, guiLeft + 70, guiTop + 10, 20, 20, ""));		
		buttonList.add(bladesDownButton = new GuiButton(0, guiLeft + 20, guiTop + 55, 20, 20, "-"));
		buttonList.add(bladesUpButton = new GuiButton(0, guiLeft + 40, guiTop + 55, 20, 20, "+"));
		buttonList.add(pitchDownButton = new GuiButton(0, guiLeft + 65, guiTop + 55, 20, 20, "-"));
		buttonList.add(pitchUpButton = new GuiButton(0, guiLeft + 85, guiTop + 55, 20, 20, "+"));
		buttonList.add(diameterDownButton = new GuiButton(0, guiLeft + 110, guiTop + 55, 20, 20, "-"));
		buttonList.add(diameterUpButton = new GuiButton(0, guiLeft + 130, guiTop + 55, 20, 20, "+"));
		buttonList.add(startButton = new GuiButton(0, guiLeft + 130, guiTop + 145, 20, 20, ""));
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		super.drawScreen(mouseX, mouseY, renderPartialTicks);
		if(bench.isRunning()){
			mc.thePlayer.closeScreen();
			return;
		}
		
		propellerMaterialQty = bench.diameter < 90 ? bench.numberBlades : bench.numberBlades*2;
		this.getPlayerMaterials();
		GL11.glColor3f(1, 1, 1); //Not sure why buttons make this grey, but whatever...
		this.drawDefaultBackground();
		mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 176, 222);
		
		tier0Button.enabled = bench.propellerType != 0;
		tier1Button.enabled = bench.propellerType != 1;
		tier2Button.enabled = bench.propellerType != 2;
		bladesUpButton.enabled = bench.numberBlades < 9;
		bladesDownButton.enabled = bench.numberBlades > 2;
		pitchUpButton.enabled = bench.pitch < 82;
		pitchDownButton.enabled = bench.pitch > 55;
		diameterUpButton.enabled = bench.diameter < 115;
		diameterDownButton.enabled = bench.diameter > 70;
		startButton.enabled = doesPlayerHaveMaterials();
		
		for(Object obj : buttonList){
			((GuiButton) obj).drawButton(mc, mouseX, mouseY);
		}
		
		ItemStack propellerMaterial;
		switch(bench.propellerType){
			case(0): propellerMaterial = planksStack; break;
			case(1): propellerMaterial = ingotStack; break;
			case(2): propellerMaterial = obsidianStack; break;
			default: propellerMaterial = new ItemStack(Blocks.ANVIL);
		}

		RenderHelper.enableGUIStandardItemLighting();
		mc.getRenderItem().renderItemIntoGUI(propellerStacks[0], guiLeft + 10, guiTop + 12);
		mc.getRenderItem().renderItemIntoGUI(propellerStacks[1], guiLeft + 40, guiTop + 12);
		mc.getRenderItem().renderItemIntoGUI(propellerStacks[2], guiLeft + 70, guiTop + 12);
		mc.getRenderItem().renderItemIntoGUI(propellerMaterial, guiLeft + 10, guiTop + 120);
		mc.getRenderItem().renderItemIntoGUI(ingotStack, guiLeft + 10, guiTop + 150);
		mc.getRenderItem().renderItemIntoGUI(redstoneStack, guiLeft + 10, guiTop + 180);
		RenderHelper.disableStandardItemLighting();
		
		this.drawRect(guiLeft + 132, guiTop + 163, guiLeft + 148, guiTop + 147, doesPlayerHaveMaterials() ? Color.GREEN.getRGB() : Color.RED.getRGB());
		
		mc.fontRendererObj.drawString(I18n.format("info.item.propeller.numberBlades"), guiLeft + 20, guiTop + 40, Color.BLACK.getRGB());
		mc.fontRendererObj.drawString(I18n.format("info.item.propeller.pitch"), guiLeft + 65, guiTop + 40, Color.BLACK.getRGB());
		mc.fontRendererObj.drawString(I18n.format("info.item.propeller.diameter"), guiLeft + 110, guiTop + 40, Color.BLACK.getRGB());
		
		GL11.glPushMatrix();
		GL11.glScalef(2, 2, 2);
		mc.fontRendererObj.drawString(String.valueOf(bench.numberBlades), (guiLeft + 35)/2, (guiTop + 85)/2, Color.BLACK.getRGB());
		mc.fontRendererObj.drawString(String.valueOf(bench.pitch), (guiLeft + 75)/2, (guiTop + 85)/2, Color.BLACK.getRGB());
		mc.fontRendererObj.drawString(String.valueOf(bench.diameter), (guiLeft + 115)/2, (guiTop + 85)/2, Color.BLACK.getRGB());
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glScalef(2, 2, 2);
		switch(bench.propellerType){
			case(0): mc.fontRendererObj.drawString(String.valueOf(numberPlayerPlanks), (guiLeft + 35)/2, (guiTop + 120)/2, numberPlayerPlanks >= propellerMaterialQty ? Color.GREEN.getRGB() : Color.RED.getRGB()); break;
			case(1): mc.fontRendererObj.drawString(String.valueOf((numberPlayerIronIngots <= 1 ? 0 : numberPlayerIronIngots - 1)), (guiLeft + 35)/2, (guiTop + 120)/2, numberPlayerIronIngots >= propellerMaterialQty ? Color.GREEN.getRGB() : Color.RED.getRGB()); break;
			case(2): mc.fontRendererObj.drawString(String.valueOf(numberPlayerObsidian), (guiLeft + 35)/2, (guiTop + 120)/2, numberPlayerObsidian >= propellerMaterialQty ? Color.GREEN.getRGB() : Color.RED.getRGB()); break;
			default: ;
		}
		mc.fontRendererObj.drawString("/" + String.valueOf(propellerMaterialQty), (guiLeft + 70)/2, (guiTop + 120)/2, Color.BLACK.getRGB());
		mc.fontRendererObj.drawString(String.valueOf(numberPlayerIronIngots), (guiLeft + 35)/2, (guiTop + 150)/2, numberPlayerIronIngots >= 1 ? Color.GREEN.getRGB() : Color.RED.getRGB());
		mc.fontRendererObj.drawString("/1", (guiLeft + 70)/2, (guiTop + 150)/2, Color.BLACK.getRGB());
		mc.fontRendererObj.drawString(String.valueOf(numberPlayerRedstone), (guiLeft + 35)/2, (guiTop + 180)/2, numberPlayerRedstone >= 5 ? Color.GREEN.getRGB() : Color.RED.getRGB());
		mc.fontRendererObj.drawString("/5", (guiLeft + 70)/2, (guiTop + 180)/2, Color.BLACK.getRGB());
		GL11.glPopMatrix();
	}
    
	@Override
    protected void actionPerformed(GuiButton buttonClicked) throws IOException {
		super.actionPerformed(buttonClicked);
		if(buttonClicked.equals(tier0Button)){
			bench.propellerType=0;
		}else if(buttonClicked.equals(tier1Button)){
			bench.propellerType=1;
		}else if(buttonClicked.equals(tier2Button)){
			bench.propellerType=2;
		}else if(buttonClicked.equals(bladesUpButton)){
			bench.numberBlades+=1;
		}else if(buttonClicked.equals(bladesDownButton)){
			bench.numberBlades-=1;
		}else if(buttonClicked.equals(pitchUpButton)){			
			bench.pitch+=3;
		}else if(buttonClicked.equals(pitchDownButton)){
			bench.pitch-=3;
		}else if(buttonClicked.equals(diameterUpButton)){
			bench.diameter+=5;
		}else if(buttonClicked.equals(diameterDownButton)){
			bench.diameter-=5;
		}else if(buttonClicked.equals(startButton)){
			bench.timeOperationFinished = bench.getWorld().getTotalWorldTime() + 1000;
			MTS.MTSNet.sendToServer(new PropellerBenchStartPacket(bench));
			mc.thePlayer.closeScreen();
			return;
		}
		MTS.MTSNet.sendToServer(new PropellerBenchStartPacket(bench));
	}
	
	private void getPlayerMaterials(){
		numberPlayerPlanks = 0;
		numberPlayerIronIngots = 0;
		numberPlayerObsidian = 0;
		numberPlayerRedstone = 0;
		if(player.capabilities.isCreativeMode){
			numberPlayerPlanks = 999;
			numberPlayerIronIngots = 999;
			numberPlayerObsidian = 999;
			numberPlayerRedstone = 999;
		}else{
			for(ItemStack stack : player.inventory.mainInventory){
				if(stack != null){
					if(stack.getItem().equals(Item.getItemFromBlock(Blocks.PLANKS))){
						numberPlayerPlanks+=stack.stackSize;
					}else if(stack.getItem().equals(Items.IRON_INGOT)){
						numberPlayerIronIngots+=stack.stackSize;
					}else if(stack.getItem().equals(Item.getItemFromBlock(Blocks.OBSIDIAN))){
						numberPlayerObsidian+=stack.stackSize;
					}else if(stack.getItem().equals(Items.REDSTONE)){
						numberPlayerRedstone+=stack.stackSize;
					}
				}
			}
		}
	}
	
	@Override
	public boolean doesGuiPauseGame(){
		return false;
	}
	
	private boolean doesPlayerHaveMaterials(){
		switch(bench.propellerType){
			case(0): return numberPlayerPlanks >= propellerMaterialQty && numberPlayerIronIngots >= 1 && numberPlayerRedstone >= 5;
			case(1): return numberPlayerIronIngots >= propellerMaterialQty + 1 && numberPlayerRedstone >= 5;
			case(2): return numberPlayerObsidian >= propellerMaterialQty && numberPlayerIronIngots >= 1 && numberPlayerRedstone >= 5;
			default: return false;
		}
	}
}
