package minecrafttransportsimulator.blocks.tileentities.components;

import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONPoleComponent;

/**Base class for components that can go on poles.  Not actually a TE, just sits on one.
 * 
 * @author don_bruce
 */
public abstract class ATileEntityPole_Component{
	
	public final JSONPoleComponent definition;
	
	public ATileEntityPole_Component(JSONPoleComponent definition){
		this.definition = definition;
	}
	
	/**
	 *  Gets the light level for this component.  If non-zero, this block will emit light
	 *  when this component is present.  If multiple components are placed, the highest value
	 *  of all components is used.  Value is from 0.0-1.0.
	 */
	public abstract float lightLevel();
	
	/**
	 *  Gets the text lines for this component.  May return null if
	 *  this component doesn't have any.
	 */
	public List<String> getTextLines(){
		return null;
	}
	
	/**
	 *  Sets the text lines for this component from the passed-in list.
	 *  This is a valid operation even on components that don't have
	 *  text lines.  Null may be passed-in here on components that do
	 *  have text lines.
	 */
	public void setTextLines(List<String> textLines){}
}
