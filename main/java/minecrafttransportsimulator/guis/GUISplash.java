package minecrafttransportsimulator.guis;

import java.awt.Color;

import net.minecraft.client.gui.GuiScreen;

public class GUISplash extends GuiScreen{
	private int line = 0;
		
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		this.drawDefaultBackground();
		this.line = 0;
		drawTextLine("Thank you for downloading MTS, the new version of MFS!  Please craft a copy");
		drawTextLine("of the fancy new manual to get started, as many things have changed from MFS.");
		drawTextLine("");
		drawTextLine("This is the start of the V10 series of MTS.  This update string will focus");
		drawTextLine("primarially on car components and making the user-interfaces simpler.");
		drawTextLine("Also expect a LOT more vehicles to hit soon in the content pack.");
		drawTextLine("");
		drawTextLine("If you haven't updated you content pack past V03 you might not have anything");
		drawTextLine("in the creative tabs as the pack system now checks versions to prevent crashes.");
		drawTextLine("If you are one of those people to ignore such things, an error will appear");
		drawTextLine("on the creative tab screen just in case.");
		drawTextLine("");
		drawTextLine("Oh, and one final thing.  Please don't re-distribute this mod.  I still have issues with");
		drawTextLine("people downloading old MFS versions that people re-posted and didn't update...");
	}
	
	private void drawTextLine(String text){
		fontRendererObj.drawStringWithShadow(text, 5, 10+(line++)*15, Color.WHITE.getRGB());
	}
}