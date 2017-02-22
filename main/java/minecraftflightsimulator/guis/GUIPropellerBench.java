package minecraftflightsimulator.guis;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.minecrafthelpers.ItemStackHelper;
import minecraftflightsimulator.minecrafthelpers.PlayerHelper;
import minecraftflightsimulator.packets.general.PropellerBenchSyncPacket;
import minecraftflightsimulator.systems.GL11DrawSystem;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class GUIPropellerBench extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation("mfs", "textures/guis/long_blank.png");
	
	private static final ResourceLocation woodPropellerIcon = new ResourceLocation("mfs", "textures/items/propeller0.png");
	private static final ResourceLocation ironPropellerIcon = new ResourceLocation("mfs", "textures/items/propeller1.png");
	private static final ResourceLocation obsidianPropellerIcon = new ResourceLocation("mfs", "textures/items/propeller2.png");
	
	private static final ResourceLocation planksIcon = new ResourceLocation("mfs", "textures/guis/propeller_bench_planks.png");
	private static final ResourceLocation ironIngotIcon = new ResourceLocation("minecraft", "textures/items/iron_ingot.png");
	private static final ResourceLocation obsidianIcon = new ResourceLocation("mfs", "textures/guis/propeller_bench_obsidian.png");
	private static final ResourceLocation redstoneIcon = new ResourceLocation("minecraft", "textures/items/redstone_dust.png");
	
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
		GL11DrawSystem.bindTexture(background);
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
		
		GL11.glColor3f(1, 1, 1);
		GL11DrawSystem.bindTexture(woodPropellerIcon);
		GL11DrawSystem.renderSquare(guiLeft + 10, guiLeft + 26, guiTop + 28, guiTop + 12, 0, 0, false);
		GL11DrawSystem.bindTexture(ironPropellerIcon);
		GL11DrawSystem.renderSquare(guiLeft + 40, guiLeft + 56, guiTop + 28, guiTop + 12, 0, 0, false);
		GL11DrawSystem.bindTexture(obsidianPropellerIcon);
		GL11DrawSystem.renderSquare(guiLeft + 70, guiLeft + 86, guiTop + 28, guiTop + 12, 0, 0, false);
		
		GL11DrawSystem.drawString(PlayerHelper.getTranslatedText("info.item.propeller.numberBlades"), guiLeft + 20, guiTop + 40, Color.BLACK);
		GL11DrawSystem.drawString(PlayerHelper.getTranslatedText("info.item.propeller.pitch"), guiLeft + 65, guiTop + 40, Color.BLACK);
		GL11DrawSystem.drawString(PlayerHelper.getTranslatedText("info.item.propeller.diameter"), guiLeft + 110, guiTop + 40, Color.BLACK);
		
		GL11.glPushMatrix();
		GL11.glScalef(2, 2, 2);
		GL11DrawSystem.drawString(String.valueOf(bench.numberBlades), (guiLeft + 35)/2, (guiTop + 85)/2, Color.BLACK);
		GL11DrawSystem.drawString(String.valueOf(bench.pitch), (guiLeft + 75)/2, (guiTop + 85)/2, Color.BLACK);
		GL11DrawSystem.drawString(String.valueOf(bench.diameter), (guiLeft + 115)/2, (guiTop + 85)/2, Color.BLACK);
		GL11.glPopMatrix();
		
		switch(bench.propellerType){
			case(0): GL11DrawSystem.bindTexture(planksIcon); break;
			case(1): GL11DrawSystem.bindTexture(ironIngotIcon); break;
			case(2): GL11DrawSystem.bindTexture(obsidianIcon); break;
			default: ;
		}
		GL11DrawSystem.renderSquare(guiLeft + 10, guiLeft + 26, guiTop + 136, guiTop + 120, 0, 0, false);
		GL11DrawSystem.bindTexture(ironIngotIcon);
		GL11DrawSystem.renderSquare(guiLeft + 10, guiLeft + 26, guiTop + 166, guiTop + 150, 0, 0, false);
		GL11DrawSystem.bindTexture(redstoneIcon);
		GL11DrawSystem.renderSquare(guiLeft + 10, guiLeft + 26, guiTop + 196, guiTop + 180, 0, 0, false);
		
		GL11.glPushMatrix();
		GL11.glScalef(2, 2, 2);
		switch(bench.propellerType){
			case(0): GL11DrawSystem.drawString(String.valueOf(numberPlayerPlanks), (guiLeft + 35)/2, (guiTop + 120)/2, numberPlayerPlanks >= propellerMaterialQty ? Color.GREEN : Color.RED); break;
			case(1): GL11DrawSystem.drawString(String.valueOf((numberPlayerIronIngots <= 1 ? 0 : numberPlayerIronIngots - 1)), (guiLeft + 35)/2, (guiTop + 120)/2, numberPlayerIronIngots >= propellerMaterialQty ? Color.GREEN : Color.RED); break;
			case(2): GL11DrawSystem.drawString(String.valueOf(numberPlayerObsidian), (guiLeft + 35)/2, (guiTop + 120)/2, numberPlayerObsidian >= propellerMaterialQty ? Color.GREEN : Color.RED); break;
			default: ;
		}
		GL11DrawSystem.drawString("/" + String.valueOf(propellerMaterialQty), (guiLeft + 70)/2, (guiTop + 120)/2, Color.BLACK);
		GL11DrawSystem.drawString(String.valueOf(numberPlayerIronIngots), (guiLeft + 35)/2, (guiTop + 150)/2, numberPlayerIronIngots >= 1 ? Color.GREEN : Color.RED);
		GL11DrawSystem.drawString("/1", (guiLeft + 70)/2, (guiTop + 150)/2, Color.BLACK);
		GL11DrawSystem.drawString(String.valueOf(numberPlayerRedstone), (guiLeft + 35)/2, (guiTop + 180)/2, numberPlayerRedstone >= 5 ? Color.GREEN : Color.RED);
		GL11DrawSystem.drawString("/5", (guiLeft + 70)/2, (guiTop + 180)/2, Color.BLACK);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		if(doesPlayerHaveMaterials()){
			GL11.glColor3f(0, 1, 0);
		}else{
			GL11.glColor3f(1, 0, 0);
		}
		GL11DrawSystem.renderSquare(guiLeft + 132, guiLeft + 148, guiTop + 163, guiTop + 147, 0, 0, false);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glPopMatrix();
		
	}
    
	@Override
    protected void actionPerformed(GuiButton buttonClicked){
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
			bench.timeOperationFinished = bench.getWorldObj().getTotalWorldTime() + 1000;
			MFS.MFSNet.sendToServer(new PropellerBenchSyncPacket(bench));
			mc.thePlayer.closeScreen();
			return;
		}
		MFS.MFSNet.sendToServer(new PropellerBenchSyncPacket(bench));
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
					if(ItemStackHelper.getItemFromStack(stack).equals(Item.getItemFromBlock(Blocks.planks))){
						numberPlayerPlanks+=ItemStackHelper.getStackSize(stack);
					}else if(ItemStackHelper.getItemFromStack(stack).equals(Items.iron_ingot)){
						numberPlayerIronIngots+=ItemStackHelper.getStackSize(stack);
					}else if(ItemStackHelper.getItemFromStack(stack).equals(Item.getItemFromBlock(Blocks.obsidian))){
						numberPlayerObsidian+=ItemStackHelper.getStackSize(stack);
					}else if(ItemStackHelper.getItemFromStack(stack).equals(Items.redstone)){
						numberPlayerRedstone+=ItemStackHelper.getStackSize(stack);
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
