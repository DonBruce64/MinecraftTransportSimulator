package minecrafttransportsimulator.blocks.tileentities.components;

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
}
