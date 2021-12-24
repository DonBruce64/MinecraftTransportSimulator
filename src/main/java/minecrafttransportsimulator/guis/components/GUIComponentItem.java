package minecrafttransportsimulator.guis.components;

import java.util.List;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.mcinterface.InterfaceGUI;
import minecrafttransportsimulator.rendering.components.RenderableObject;
import minecrafttransportsimulator.rendering.instances.RenderText;
import minecrafttransportsimulator.rendering.instances.RenderText.TextAlignment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

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
	private ItemStack stackToRender;
	private ItemStack lastStackRendered;
	private RenderableObject stackModel;
	
	/**Default item constructor.**/
	public GUIComponentItem(int x, int y, float scale){
		super(x, y, (int) (16*scale), (int) (16*scale));
		this.scale = scale;
	}
	
	/**Constructor for an item linked with a button.  Button is assumed to be 18x18px so item will be offset 1px to center.**/
	public GUIComponentItem(GUIComponentButton linkedButton){
		this(linkedButton.x + 1, linkedButton.y + 1, 1.0F);
	}

    @Override
	public void render(int mouseX, int mouseY, int textureWidth, int textureHeight, boolean blendingEnabled, float partialTicks){
    	
    	if(stack != null){
    		//InterfaceGUI.drawItem(stack, x, y, scale);
    	}else if(stacks != null && !stacks.isEmpty()){
    		//InterfaceGUI.drawItem(stacks.get((int) (System.currentTimeMillis()%(stacks.size()*500)/500)), x, y, scale);
    	}
    	if(stack != null){
    		stackToRender = stack;
    	}else if(stacks != null && !stacks.isEmpty()){
    		stackToRender = stacks.get((int) (System.currentTimeMillis()%(stacks.size()*500)/500));
    	}else{
    		stackToRender = null;
    	}
    	
    	if(stackToRender != null && !stackToRender.isEmpty()){
    		if(!stackToRender.equals(lastStackRendered)){
	    		stackModel = InterfaceGUI.getItemModel(stackToRender);
	    		lastStackRendered = stackToRender;
	    		if(stackToRender.getCount() > 1){
	    			text = ((char) 167) + "l" + String.valueOf(stackToRender.getCount());
	    		}
    		}
    	}else{
    		stackModel = null;
    		lastStackRendered = null;
    		text = null;
    	}
    	
    	if(stackModel != null){
    		GL11.glPushMatrix();
			GL11.glTranslatef(x, y, 0);
			
			//Required to get 3D models 100% in front of the background.
	        GlStateManager.translate(0, 0, 100.0F + 50);
			
			GL11.glScalef(scale, scale, scale);
			
	        //This makes sure normal-lighting doesn't get fouled by scaling operations.
	         GlStateManager.enableRescaleNormal(); 
	        
	        //Items are normally rendered with origin at bottom-right like normal models.
	        //This moves them to top-left orientation.
	        GlStateManager.translate(0, 16.0F, 0.0F);
	        
	        //Flip the model across the Y-axis as GUIs are inverted.
	        GlStateManager.scale(1.0F, -1.0F, 1.0F);
	        
	        //Scale up model, as it's normally 1 unit -> 1 block, not 1px, and we want 16px default.
	        GlStateManager.scale(16.0F, 16.0F, 16.0F);
	        		
    		//Need to enable lighting for proper shading on block-items.
    		GlStateManager.enableLighting();
    		
    		//Renders.
        	stackModel.render();
	        
	        
	        GlStateManager.disableRescaleNormal();
	        GlStateManager.disableLighting();
			
			
			GL11.glPopMatrix();
    	}
    }
    
    @Override
    public void renderText(boolean renderTextLit){
    	//TODO make this dynamic or something?
    	GL11.glTranslated(0, 0, 250);
    	RenderText.draw2DText(text, null, x + (int) (scale*16), y + (int) (scale*8), ColorRGB.WHITE, TextAlignment.RIGHT_ALIGNED, scale, false, 0);
    	GL11.glTranslated(0, 0, -250);
    }
    
    @Override
	public List<String> getTooltipText(){
    	if(stackToRender != null && !stackToRender.isEmpty()){
    		Minecraft mc = Minecraft.getMinecraft();
			List<String> tooltipText = stackToRender.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
	        for(int i = 0; i < tooltipText.size(); ++i){
	            if(i != 0){
	            	//Add grey formatting text to non-first line tooltips.
	            	tooltipText.set(i, TextFormatting.GRAY + tooltipText.get(i));
	            }
	        }
	        return tooltipText;
    	}else{
    		return null;
    	}
    }
}
