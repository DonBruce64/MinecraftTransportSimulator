package minecrafttransportsimulator.guis;

import java.awt.Color;

import net.minecraft.client.gui.GuiScreen;

public class GUISplash extends GuiScreen{
	private static int line = 0;
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		this.drawDefaultBackground();
		
		line = 0;
		drawTextLine("Thank you for downloading MTS, the new version of MFS!  Please craft a copy");
		drawTextLine("of the fancy new manual to get started, as many things have changed from MFS.");
		drawTextLine("");
		drawTextLine("One of the most important is that MTS now has a pack-based system like Flans.");
		drawTextLine("You can find the official MTS pack on Curse, so go there if you don't have it.");
		drawTextLine("If you're interested in making your own packs, come join the Discord.");
		drawTextLine("The link is on the Minecraftforums page.");
		drawTextLine("");
		drawTextLine("You can also get more specifics about things the manual doesn't fully cover,");
		drawTextLine("and get sneak peeks on how the mod is progressing.  We don't frequent");
		drawTextLine("Minecraftforum much anymore so Discord is where you can bother a Don for.");
		drawTextLine("things like update status, features, and how to put your model into a pack.");
		drawTextLine("");
		drawTextLine("Oh, and one final thing.  Please don't re-distribute this mod.  I still have issues with");
		drawTextLine("people downloading old MFS versions that people re-posted an didn't update...");
	}
	
	private void drawTextLine(String text){
		fontRendererObj.drawStringWithShadow(text, 5, 10+(line++)*15, Color.WHITE.getRGB());
	}
}