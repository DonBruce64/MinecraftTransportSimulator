package minecrafttransportsimulator.guis;

import java.awt.Color;

import net.minecraft.client.gui.GuiScreen;

public class GUISplash extends GuiScreen{
	private final boolean hasPackInstalled;
	private int line = 0;
	
	public GUISplash(boolean hasPackInstalled){
		this.hasPackInstalled = hasPackInstalled;
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		this.drawDefaultBackground();
		this.line = 0;
		if(hasPackInstalled){
			drawTextLine("Thank you for downloading MTS, the new version of MFS!  Please craft a copy");
			drawTextLine("of the fancy new manual to get started, as many things have changed from MFS.");
			drawTextLine("");
			drawTextLine("This is the start of the V9 series of MTS.  This update string will focus");
			drawTextLine("primarially on cars.  As of V9.0.0 car physics are now their own thing and");
			drawTextLine("the CAR section on the config menu actually has meaning.  Keep an eye out");
			drawTextLine("for new vehicles in the content pack and a new HUD system.");
			drawTextLine("");
			drawTextLine("If you haven't updated you content pack past V01 you should do so now");
			drawTextLine("as the Scout has been re-configured to use car physics.");
			drawTextLine("");
			drawTextLine("");
			drawTextLine("Oh, and one final thing.  Please don't re-distribute this mod.  I still have issues with");
			drawTextLine("people downloading old MFS versions that people re-posted and didn't update...");
		}else{
			drawTextLine("Hey, you.  Yeah you.  It appears that you don't have a content pack installed.");
			drawTextLine("Content packs are the new way MTS distributes files to users.");
			drawTextLine("You kinda need one if you want vehicles in-game, so how about installing one?.");
			drawTextLine("");
			drawTextLine("If you don't have a pack, how about downloading the offical pack?");
			drawTextLine("There's a link to it on the MTS Curse page, or you can visit it on its own page.");
			drawTextLine("");
			drawTextLine("If have a pack but don't know how to install it, see the instructions on Curse.");
			drawTextLine("");
			drawTextLine("If you're still confused, Discord can be really helpful. Link to that is on Curse.");
			drawTextLine("");
			drawTextLine("Basically, go to Curse for info if you're confused.");
		}
	}
	
	private void drawTextLine(String text){
		fontRendererObj.drawStringWithShadow(text, 5, 10+(line++)*15, Color.WHITE.getRGB());
	}
}