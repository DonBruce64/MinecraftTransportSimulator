package minecrafttransportsimulator.guis.components;

/**Custom item render class.  This class is designed to render an item into the GUI without
 * actually having an instance of said item, or calling any MC methods.  This allows us to
 * use a simple string name for the render to prevent version changes from causing issues.
 * Metadata is also used here, though it may not be required in later versions.
 *
 * @author don_bruce
 */
public class GUIComponentItem{		
	public final int x;
	public final int y;
	public final float scale;
	public final String itemName;
	public final int itemQty;
	public final int itemMetadata;
	
	public boolean visible = true;
	    	
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
    public void renderItem(GUIBase currentGUI){
    	if(visible){
    		currentGUI.drawItem(itemName, itemQty, itemMetadata, x, y, scale);
    	}
    }
    
	/**
	 *  Renders the tooltip for this item.  This needs to be done after the main item
	 *  render as otherwise it will render behind other items.
	 */
    public void renderTooltip(GUIBase currentGUI, int mouseX, int mouseY){
    	if(visible){
    		float itemTooltipBounds = 16*scale;
    		if(mouseX > x && mouseX < x + itemTooltipBounds && mouseY > y && mouseY < y + itemTooltipBounds){
    			currentGUI.drawItemTooltip(itemName, itemQty, itemMetadata, mouseX, mouseY);
    		}
    	}
    }
}
