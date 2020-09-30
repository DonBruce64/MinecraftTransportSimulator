package minecrafttransportsimulator.guis.components;

import mcinterface.BuilderGUI;
import mcinterface.WrapperItemStack;

/**Custom item render class.  This class is designed to render a {@link WrapperItemStack} 
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
	public WrapperItemStack stack;
		    	
	public GUIComponentItem(int x, int y, float scale, WrapperItemStack stack){
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
    		BuilderGUI.drawItem(stack, x, y, scale);
    	}
    }
    
	/**
	 *  Renders the tooltip for this item.  This needs to be done after the main item
	 *  render as otherwise it will render behind other items.  This method needs an
	 *  instance of {@link BuilderGUI} due to the MC GUI system hard-linking the
	 *  tooltip rendering sequence to the GUI instance.
	 */
    public void renderTooltip(BuilderGUI wrapper, int mouseX, int mouseY){
    	if(stack != null){
    		float itemTooltipBounds = 16*scale;
    		if(mouseX > x && mouseX < x + itemTooltipBounds && mouseY > y && mouseY < y + itemTooltipBounds){
    			wrapper.drawItemTooltip(stack, mouseX, mouseY);
    		}
    	}
    }
}
