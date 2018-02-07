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
			drawTextLine("One of the most important is that MTS now has a pack-based system like Flans.");
			drawTextLine("But seeing as how you've already installed a pack I don't need to tell you that.");
			drawTextLine("I will say that if you're interested in making your own packs, come join the Discord.");
			drawTextLine("The link to the Discord is on the Minecraftforums page.");
			drawTextLine("");
			drawTextLine("All the devs hang out on Discord regularly, so it's a great place to get help fast.");
			drawTextLine("You can also get more specifics about things the manual doesn't fully cover,");
			drawTextLine("and get sneak peeks on how the mod is progressing.");
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