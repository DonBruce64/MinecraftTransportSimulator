package minecrafttransportsimulator.guis.components;

import java.util.List;

import minecrafttransportsimulator.mcinterface.InterfaceGUI;
import net.minecraft.item.ItemStack;

/**Custom item render class.  This class is designed to render a {@link ItemStack} 
 * or list of stacks into the GUI.  This allows us to use a simple string 
 * name for the render rather than a bunch of MC calls.
 * Note that this component does not get a stack assigned during construction: you must
 * assign it manually either by setting {@link #stack} for a single stack rendering operation,
 * or {@link #stacks} for a cyclclic rendering operation.  This allows for switching items in GUIs.
 * This is especially useful in crafting GUIs, where you want a static set of item components
 * that switch their states depending on other selections.  The scale is based on the assumption that
 * a single item is 16x16px.
 *
 * @author don_bruce
 */
public class GUIComponentItem extends AGUIComponent{		
	public final float scale;
	public ItemStack stack;
	public List<ItemStack> stacks;
	
	/**Default item constructor.**/
	public GUIComponentItem(int x, int y, float scale){
		super(x, y);
		this.scale = scale;
	}
	
	/**Constructor for an item linked with a button.  Button is assumed to be 18x18px so item will be offset 1px to center.**/
	public GUIComponentItem(GUIComponentButton linkedButton){
		this(linkedButton.x + 1, linkedButton.y + 1, 1.0F);
	}

    @Override
	public void render(int mouseX, int mouseY, int textureWidth, int textureHeight, boolean blendingEnabled, float partialTicks){
    	if(stack != null){
    		InterfaceGUI.drawItem(stack, x, y, scale);
    	}else if(stacks != null && !stacks.isEmpty()){
    		InterfaceGUI.drawItem(stacks.get((int) (System.currentTimeMillis()%(stacks.size()*500)/500)), x, y, scale);
    	}
    }
    
    @Override
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
