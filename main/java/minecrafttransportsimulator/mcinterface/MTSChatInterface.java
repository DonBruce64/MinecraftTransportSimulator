package minecrafttransportsimulator.mcinterface;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;

/**Static class that is designed to interface with the current
 * MC chat system.  Handles console chat operations, as well
 * as .lang file translation operations.
 * 
 * @author don_bruce
 */
public class MTSChatInterface{
	private static GuiNewChat chatGUI;
	
	/**Prints the passed-in message to the chat GUI.  Applies a .lang translation.*/
	public static void displayChatMessage(String message){
		if(chatGUI == null){
			chatGUI = Minecraft.getMinecraft().ingameGUI.getChatGUI();
		}
		chatGUI.printChatMessage(new TextComponentString(getTranslatedMessage(message)));
	}
	
	/**Translates the passed-in string from the .lang files.*/
	public static String getTranslatedMessage(String message){
		return I18n.format(message);
	}
}
