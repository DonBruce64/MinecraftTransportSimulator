package minecrafttransportsimulator.guis.components;

import mcinterface.BuilderGUI;

/**Custom item render class.  This class is designed to render an item into the GUI without
 * actually having an instance of said item, or calling any MC methods.  This allows us to
 * use a simple string name for the render to prevent version changes from causing issues.
 * Metadata is also used here, though it may not be required in later versions.
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
	public String itemName;
	public int itemQty;
	public int itemMetadata;
		    	
	public GUIComponentItem(int x, int y, float scale, String itemName, int itemQty, int itemMetadata){
		this.x = x;
		this.y = y;
		this.scale = scale;
		this.itemName = itemName;
		this.itemQty = itemQty;
		this.itemMetadata = itemMetadata;
	}

	
	/**
	 *  Renders the item by calling the appropriate method in the bas GUI.
	 *  This is done to keep all nasty MC-specific methods out of this component.
	 */
    public void renderItem(){
    	if(itemName != null){
    		BuilderGUI.drawItem(itemName, itemQty, itemMetadata, x, y, scale);
    	}
    }
    
	/**
	 *  Renders the tooltip for this item.  This needs to be done after the main item
	 *  render as otherwise it will render behind other items.  This method needs an
	 *  instance of {@link BuilderGUI} due to the MC GUI system hard-linking the
	 *  tooltip rendering sequence to the GUI instance.
	 */
    public void renderTooltip(BuilderGUI wrapper, int mouseX, int mouseY){
    	if(itemName != null){
    		float itemTooltipBounds = 16*scale;
    		if(mouseX > x && mouseX < x + itemTooltipBounds && mouseY > y && mouseY < y + itemTooltipBounds){
    			wrapper.drawItemTooltip(itemName, itemQty, itemMetadata, mouseX, mouseY);
    		}
    	}
    }
}
