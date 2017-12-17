package minecrafttransportsimulator.guis;

import java.awt.Color;

import minecrafttransportsimulator.MTS;
import net.minecraft.client.gui.GuiScreen;

public class GUICredits extends GuiScreen{
	private static int line = 0;
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		this.drawDefaultBackground();
		
		line = 0;
		drawTextLine("Thank you for downloading MTS Version " + MTS.MODVER + "! Please see the minecraftforum");
		drawTextLine("page for help, flight tips, recipes, questions, and current mod news.");
		drawTextLine("Happy flying! ~The MTS Team");
		drawTextLine("");
		drawTextLine("CREDITS:");
		drawTextLine("don_bruce 'Don':");
		drawTextLine("     Coding, MC-172, Trimotor, Seats, Wheels, Flight Instruments");
		drawTextLine("DietPepsi1997 'Limit1997':");
		drawTextLine("     PZL P11, Engines, Propellers, Pontoons, Propeller Bench");
		drawTextLine("Wolfiader 'Wolfvanox':");
		drawTextLine("     Vulcanair, Comanche");
		drawTextLine("");
		drawTextLine("TRANSLATIONS:");
		drawTextLine("     sotakan0808 -> ja_JP.lang; Minddao -> ru_RU.lang");
	}
	
	private void drawTextLine(String text){
		fontRendererObj.drawStringWithShadow(text, 5, 10+(line++)*15, Color.WHITE.getRGB());
	}
}