package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.mcinterface.MasterLoader;
import net.minecraft.item.ItemStack;

/**Custom item render class.  This class is designed to render a {@link ItemStack} 
 * into the GUI without, or calling any MC methods.  This allows us to use a simple string 
 * name for the render to prevent version changes from causing issues.
 * Note that the item parameters are not final.  This allows for switching items in GUIs.
 * This is especially useful in crafting GUIs, where you want a static set of item components
 * that switch their states depending on other selections.
 *
 * @author don_bruce
 */
public class GUIComponentItem{		
	public final int x;
	public final int y;
	public final float scale;
	public ItemStack stack;
		    	
	public GUIComponentItem(int x, int y, float scale, ItemStack stack){
		this.x = x;
		this.y = y;
		this.scale = scale;
		this.stack = stack;
	}

	
	/**
	 *  Renders the item by calling the appropriate method in the bas GUI.
	 *  This is done to keep all nasty MC-specific methods out of this component.
	 */
    public void renderItem(){
    	if(stack != null){
    		MasterLoader.guiInterface.drawItem(stack, x, y, scale);
    	}
    }
    
	/**
	 *  Renders the tooltip for this item.  This needs to be done after the main item
	 *  render as otherwise it will render behind other items.  This method needs an
	 *  instance of {@link AGUIBase} to ensure the tooltip doesn't render off the screen.
	 */
    public void renderTooltip(AGUIBase gui, int mouseX, int mouseY){
    	if(stack != null){
    		float itemTooltipBounds = 16*scale;
    		if(mouseX > x && mouseX < x + itemTooltipBounds && mouseY > y && mouseY < y + itemTooltipBounds){
    			MasterLoader.guiInterface.drawItemTooltip(gui, mouseX, mouseY, stack);
    		}
    	}
    }
}
