package minecraftflightsimulator.containers;

import java.awt.Color;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.packets.general.PropellerBenchTilepdatePacket;
import minecraftflightsimulator.utilities.RenderHelper;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class GUIPropellerBench extends GuiContainer{
	private static final ResourceLocation woodPropellerIcon = new ResourceLocation("mfs", "textures/items/propeller0.png");
	private static final ResourceLocation ironPropellerIcon = new ResourceLocation("mfs", "textures/items/propeller1.png");
	private static final ResourceLocation obsidianPropellerIcon = new ResourceLocation("mfs", "textures/items/propeller2.png");
	private static final ResourceLocation ironIngotIcon = new ResourceLocation("minecraft", "textures/items/iron_ingot.png");
	private static final ResourceLocation redstoneIcon = new ResourceLocation("minecraft", "textures/items/redstone_dust.png");
	private static final ResourceLocation background = new ResourceLocation("mfs", "textures/guis/propeller_bench.png");
	private static final ResourceLocation arrow = new ResourceLocation("mfs", "textures/guis/arrow.png");

	private GuiButton tier1Button;
	private GuiButton tier2Button;
	private GuiButton tier3Button;
	private GuiButton bladesUpButton;
	private GuiButton bladesDownButton;
	private GuiButton diameterUpButton;
	private GuiButton diameterDownButton;
	private GuiButton pitchUpButton;
	private GuiButton pitchDownButton;
	private GuiButton powerButton;
	
	private TileEntityPropellerBench tile;
	private byte propType;
	private byte propBlades;
	private byte propPitch;
	private byte propDiameter;
	
	public GUIPropellerBench(InventoryPlayer invPlayer, TileEntityPropellerBench tile){
		super(new ContainerPropellerBench(invPlayer, tile));
		this.allowUserInput=true;
		this.xSize = 226;
		this.ySize = 222;
		this.tile = tile;
		this.propType = (byte) (tile.propertyCode%10 + 1);
		this.propBlades = (byte) (tile.propertyCode%100/10);
		this.propPitch = (byte) (55+3*(tile.propertyCode%1000/100));
		this.propDiameter = (byte) (70+5*(tile.propertyCode/1000));
	}
	
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (this.width - this.xSize)/2;
		guiTop = (this.height - this.ySize)/2;
	
		buttonList.add(tier1Button = new GuiButton(0, guiLeft + 10, guiTop + 10, 20, 20, ""));
		buttonList.add(tier2Button = new GuiButton(0, guiLeft + 40, guiTop + 10, 20, 20, ""));
		buttonList.add(tier3Button = new GuiButton(0, guiLeft + 70, guiTop + 10, 20, 20, ""));		
		buttonList.add(bladesDownButton = new GuiButton(0, guiLeft + 40, guiTop + 105, 20, 20, "-"));
		buttonList.add(bladesUpButton = new GuiButton(0, guiLeft + 60, guiTop + 105, 20, 20, "+"));
		buttonList.add(diameterDownButton = new GuiButton(0, guiLeft + 85, guiTop + 105, 20, 20, "-"));
		buttonList.add(diameterUpButton = new GuiButton(0, guiLeft + 105, guiTop + 105, 20, 20, "+"));		
		buttonList.add(pitchDownButton = new GuiButton(0, guiLeft + 130, guiTop + 105, 20, 20, "-"));
		buttonList.add(pitchUpButton = new GuiButton(0, guiLeft + 150, guiTop + 105, 20, 20, "+"));
		buttonList.add(powerButton = new GuiButton(0, guiLeft + 157, guiTop + 52, 20, 20, ""));
		
		tier1Button.enabled = propType != 1;
		tier2Button.enabled = propType != 2;
		tier3Button.enabled = propType != 3;
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int x, int y){
		RenderHelper.bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY){
        GL11.glEnable(GL11.GL_ALPHA_TEST);
		RenderHelper.bindTexture(woodPropellerIcon);
		RenderHelper.renderSquare(10, 26, 28, 12, 0, 0, false);
		RenderHelper.bindTexture(ironPropellerIcon);
		RenderHelper.renderSquare(40, 56, 28, 12, 0, 0, false);
		RenderHelper.bindTexture(obsidianPropellerIcon);
		RenderHelper.renderSquare(70, 86, 28, 12, 0, 0, false);
		RenderHelper.bindTexture(ironIngotIcon);
		RenderHelper.renderSquare(12, 28, 52, 36, 0, 0, false);
		RenderHelper.bindTexture(redstoneIcon);
		RenderHelper.renderSquare(12, 28, 106, 90, 0, 0, false);
		RenderHelper.bindTexture(arrow);
		if(tile.timeLeft > 0){
			RenderHelper.renderSquareUV(89, 113 - 24F*tile.timeLeft/tile.opTime, 70, 54, 0, 0, 0, 1 - 1F*tile.timeLeft/tile.opTime, 0, 1, false);
		}
		
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		if(tile.isOn){
			GL11.glColor3f(1, 0, 0);
		}else{
			GL11.glColor3f(0, 1, 0);
		}
		RenderHelper.renderSquare(159, 175, 70, 54, 0, 0, false);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glPopMatrix();
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		super.drawScreen(mouseX, mouseY, renderPartialTicks);
		if(propType == 1){
			tile.setInventorySlotContents(4, new ItemStack(Blocks.planks, propDiameter < 90 ? propBlades : propBlades*2));
		}else if(propType == 2){
			tile.setInventorySlotContents(4, new ItemStack(Items.iron_ingot, propDiameter < 90 ? propBlades : propBlades*2));
		}else if(propType == 3){
			tile.setInventorySlotContents(4, new ItemStack(Blocks.obsidian, propDiameter < 90 ? propBlades : propBlades*2));
		}
		
		RenderHelper.drawString("Blades", guiLeft + 43, guiTop + 90, Color.BLACK);
		RenderHelper.drawString("Diameter", guiLeft + 86, guiTop + 90, Color.BLACK);
		RenderHelper.drawString("Pitch", guiLeft + 139, guiTop + 90, Color.BLACK);
		
		RenderHelper.drawString(String.valueOf(propBlades), guiLeft + 205, guiTop + 10, Color.BLACK);
		RenderHelper.drawString(String.valueOf(propDiameter), guiLeft + 196, guiTop + 38, Color.BLACK);
		RenderHelper.drawString(String.valueOf(propPitch), guiLeft + 194, guiTop + 84, Color.BLACK);
	}
    
	@Override
    protected void actionPerformed(GuiButton buttonClicked){
		super.actionPerformed(buttonClicked);
		if(buttonClicked.equals(tier1Button)){
			propType=1;
		}else if(buttonClicked.equals(tier2Button)){
			propType=2;
		}else if(buttonClicked.equals(tier3Button)){
			propType=3;
		}else if(buttonClicked.equals(bladesUpButton)){
			if(propBlades<9)propBlades+=1;
		}else if(buttonClicked.equals(bladesDownButton)){
			if(propBlades>2)propBlades-=1;
		}else if(buttonClicked.equals(diameterUpButton)){
			if(propDiameter<115)propDiameter+=5;
		}else if(buttonClicked.equals(diameterDownButton)){
			if(propDiameter>70)propDiameter-=5;
		}else if(buttonClicked.equals(pitchUpButton)){			
			if(propPitch<82)propPitch+=3;
		}else if(buttonClicked.equals(pitchDownButton)){
			if(propPitch>55)propPitch-=3;
		}else if(buttonClicked.equals(powerButton)){
			if(tile.isOn){
				MFS.MFSNet.sendToServer(new PropellerBenchTilepdatePacket(tile, (short) 0));
			}else{
				if(tile.isMaterialCorrect() && tile.isMaterialSufficient() && tile.getStackInSlot(3) == null){
					MFS.MFSNet.sendToServer(new PropellerBenchTilepdatePacket(tile, (short) -tile.opTime));
				}
			}
			return;
		}
			
		tier1Button.enabled = propType != 1;
		tier2Button.enabled = propType != 2;
		tier3Button.enabled = propType != 3;

		MFS.MFSNet.sendToServer(new PropellerBenchTilepdatePacket(tile, (short) ((propDiameter - 70)/5*1000 + (propPitch-55)/3*100 + propBlades*10 + (propType - 1))));
	}
}
