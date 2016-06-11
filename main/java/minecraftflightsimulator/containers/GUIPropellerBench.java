package minecraftflightsimulator.containers;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GUIPropellerBench extends GuiContainer{
	private static final ResourceLocation background = new ResourceLocation("mfs", "textures/gui_background.png");
	private static final ResourceLocation foreground = new ResourceLocation("mfs", "textures/guis/propeller_bench.png");

	private GuiButton diameterUpButton;
	private GuiButton diameterDownButton;
	private GuiButton pitchUpButton;
	private GuiButton pitchDownButton;
	
	public GUIPropellerBench(InventoryPlayer invPlayer){
		super(new ContainerPropellerBench(invPlayer));
		this.allowUserInput=true;
		this.xSize = 175;
		this.ySize = 222;
	}
	
	@Override 
	public void initGui(){
		guiLeft = (this.width - this.xSize)/2;
		guiTop = (this.height - this.ySize)/2;
	
		diameterDownButton = new GuiButton(0, guiLeft + 110, guiTop + 20, 15, 20, "-");
		diameterUpButton = new GuiButton(0, guiLeft + 130, guiTop + 20, 15, 20, "+");
		pitchDownButton = new GuiButton(0, guiLeft + 110, guiTop + 80, 15, 20, "-");
		pitchUpButton = new GuiButton(0, guiLeft + 130, guiTop + 80, 15, 20, "+");
		
		buttonList.add(diameterDownButton);
		buttonList.add(diameterUpButton);
		buttonList.add(pitchDownButton);
		buttonList.add(pitchUpButton);
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int x, int y){
		this.mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		this.mc.getTextureManager().bindTexture(foreground);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		super.drawScreen(mouseX, mouseY, renderPartialTicks);
		//for(Object button : buttonList){
			//((GuiButton) button).drawButton(mc, mouseX, mouseY);
		//}
	}
    
	@Override
    public boolean doesGuiPauseGame(){return false;}
}
