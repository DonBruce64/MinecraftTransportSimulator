package minecraftflightsimulator.containers;

import minecraftflightsimulator.entities.EntityParent;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class GUIParent extends GuiContainer{
	private static final ResourceLocation background = new ResourceLocation("mfs", "textures/gui_background.png");
	private ResourceLocation foreground;

	public GUIParent(EntityPlayer player, EntityParent parent, ResourceLocation foreground){
		super(new ContainerParent(player.inventory, parent));
		this.foreground=foreground;
		this.allowUserInput=true;
		this.xSize = 175;
		this.ySize = 222;
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int x, int y){
		this.mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		this.mc.getTextureManager().bindTexture(foreground);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
	}
    
	@Override
    public boolean doesGuiPauseGame(){return false;}
}
