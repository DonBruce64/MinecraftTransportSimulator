package minecrafttransportsimulator.guis.components;

import java.util.List;

import net.minecraft.item.ItemStack;

/**Custom item render class.  This class is designed to render a {@link ItemStack} 
 * or list of stacks into the GUI.  This allows us to use a simple string 
 * name for the render rather than a bunch of MC calls.
 * Note that the item parameters are not final.  This allows for switching items in GUIs.
 * This is especially useful in crafting GUIs, where you want a static set of item components
 * that switch their states depending on other selections.  Also note that there is a single
 * stack and a list of stacks.  If the list is used, the items will cycle through.  This is
 * useful for crafting GUIs, where multiple items may be used for a single component.
 *
 * @author don_bruce
 */
public class GUIComponentItem{		
	public final int x;
	public final int y;
	public final float scale;
	public ItemStack stack;
	public List<ItemStack> stacks;
		    	
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
    		InterfaceGUI.drawItem(stack, x, y, scale);
    	}else if(stacks != null && !stacks.isEmpty()){
    		InterfaceGUI.drawItem(stacks.get((int) (System.currentTimeMillis()%(stacks.size()*500)/500)), x, y, scale);
    	}
    }
    
	/**
	 *  Renders the tooltip for this item.  This needs to be done after the main item
	 *  render as otherwise it will render behind other items.  This method needs an
	 *  instance of {@link AGUIBase} to ensure the tooltip doesn't render off the screen.
	 */
    public void renderTooltip(AGUIBase gui, int mouseX, int mouseY){
    	ItemStack stackToRender = null;
    	if(stack != null){
    		stackToRender = stack;
    	}else if(stacks != null && !stacks.isEmpty()){
    		stackToRender = stacks.get((int) (System.currentTimeMillis()%(stacks.size()*1000)/1000));
    	}
    	if(stackToRender != null && !stackToRender.isEmpty()){
    		float itemTooltipBounds = 16*scale;
    		if(mouseX > x && mouseX < x + itemTooltipBounds && mouseY > y && mouseY < y + itemTooltipBounds){
    			InterfaceGUI.drawItemTooltip(gui, mouseX, mouseY, stackToRender);
    		}
    	}
    }
}
