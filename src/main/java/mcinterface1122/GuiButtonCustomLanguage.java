package mcinterface1122;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButtonLanguage;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiButtonCustomLanguage extends GuiButtonLanguage {
    public GuiButtonCustomLanguage(int buttonID, int xPos, int yPos) {
        super(buttonID, xPos, yPos);
    }

    public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
        if (this.visible) {
            minecraft.getTextureManager().bindTexture(new ResourceLocation("mts:textures/guis/treated_wood_button.png"));
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            drawModalRectWithCustomSizedTexture(this.x, this.y, 0, 0, this.width, this.height, 32, 32);
            minecraft.getTextureManager().bindTexture(new ResourceLocation("mts:textures/guis/lang.png"));
            int texY = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height ? 20 : 0;
            drawModalRectWithCustomSizedTexture(this.x, this.y, 0, texY, this.width, this.height, 20, 40);
        }
    }
}
