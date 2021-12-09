package minecrafttransportsimulator.guis.components;

/**Base class for all components.  Contains basic common variables to all rendering.
 *
 * @author don_bruce
 */
public abstract class AGUIComponent{
	
	//Rendering variables.
	public final int x;
	public int offsetX;
	public final int y;
	public int offsetY;
	
	//State variables.
	public boolean visible = true;
	public String text;
	
	public AGUIComponent(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	/**
	 *  Renders the main portion of the component.
	 *  Note that this method is not called if {@link #visible} is false.
	 */
    public abstract void render(int mouseX, int mouseY, int textureWidth, int textureHeight, boolean blendingEnabled, float partialTicks);
    
    /**
	 *  Renders the component's text.  This is done separately from the main render as text uses its own texture,
	 *  and we don't want to do this until the rest of the GUI is rendered as it will cause a texture bind.
	 *  Note that this method is not called if {@link #visible} is false or {@link #text} is empty.
	 */
    public void renderText(boolean renderTextLit){}
    

    
    /**
	 *  Renders the tooltip for this component.  This needs to be done after the main render
	 *  of all components as otherwise it will render behind other components.  This method needs an
	 *  instance of {@link AGUIBase} to ensure the tooltip doesn't render off the screen.
	 *  Most components don't have tooltips, but components that have complex functionality 
	 *  may need them to help explain what they do.
	 */
    public void renderTooltip(AGUIBase gui, int mouseX, int mouseY){}
}
