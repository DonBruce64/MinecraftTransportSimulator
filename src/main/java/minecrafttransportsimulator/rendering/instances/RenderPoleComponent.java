package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Matrix4dPlus;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

public class RenderPoleComponent extends ARenderEntityDefinable<ATileEntityPole_Component>{
	
	@Override
	public void renderBoundingBoxes(ATileEntityPole_Component component, Matrix4dPlus transform){
		//Only render the bounding box for the core component.
		if(component.axis.equals(Axis.NONE)){
			component.core.getRenderer().renderBoundingBoxes(component.core, transform);
		}
	}
}
