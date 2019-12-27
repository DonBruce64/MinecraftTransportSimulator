package minecrafttransportsimulator.guis;

import java.awt.Color;

import minecrafttransportsimulator.MTS;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/**Custom button class.  This allows for a custom button texture, as well as a cleaner constructor.
 *
 * @author don_bruce
 */
public class GUIButton extends GuiButton{
	private static final ResourceLocation standardTexture = new ResourceLocation(MTS.MODID, "textures/guis/standard.png");
	private static final int BUTTON_TEXTURE_U_OFFSET = 196;
	private static final int BUTTON_TEXTURE_WIDTH = 200;
	private static final int BUTTON_TEXTURE_HEIGHT = 20;
	    	
	public GUIButton(int x, int y, int width, String text){
		this(x, y, width, text, 20);
	}
	
	public GUIButton(int x, int y, int width, String text, int height){
		super(0, x, y, width,height, text);
	}

	/**
	 *  Renders the button's texture using the default MC system for rendering 2D textures.
	 *  This scales the texture vertically to match the height ob the button, unlike the traditional 
	 *  MC way of using a constant value which can overrun or underrun.
	 *  Because of this, button sizes other than 20px tall are supported, though values significantly
	 *  outside this range may look odd.
	 */
	@Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks){
        if(this.visible){
        	if(visible){
        		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        		mc.getTextureManager().bindTexture(standardTexture);
				int textureUStart;
        		if(enabled){
					if(mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height){
						textureUStart = BUTTON_TEXTURE_U_OFFSET + 2*BUTTON_TEXTURE_HEIGHT;//Highlighted
					}else{
						textureUStart = BUTTON_TEXTURE_U_OFFSET + 1*BUTTON_TEXTURE_HEIGHT;//Normal
					}
				}else{
					textureUStart = BUTTON_TEXTURE_U_OFFSET;//Disabled
				}
        		GUIBase.drawSheetTexture(x, y, width/2, height, 0, textureUStart, width/2, textureUStart + BUTTON_TEXTURE_HEIGHT);
				GUIBase.drawSheetTexture(x + width/2, y, width/2, height, BUTTON_TEXTURE_WIDTH - width/2, textureUStart, BUTTON_TEXTURE_WIDTH, textureUStart + BUTTON_TEXTURE_HEIGHT);
				this.drawCenteredString(mc.fontRenderer, displayString, x + width / 2, y + (height - 8) / 2, Color.LIGHT_GRAY.getRGB());
			}
        }
    }
}
