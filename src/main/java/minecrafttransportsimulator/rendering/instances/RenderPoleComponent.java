package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Matrix4dPlus;
import minecrafttransportsimulator.baseclasses.Point3dPlus;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

public class RenderPoleComponent extends ARenderEntityDefinable<ATileEntityPole_Component>{
	
	@Override
	public void adjustPositionOrientation(ATileEntityPole_Component component, Point3dPlus entityPositionDelta, Matrix4dPlus interpolatedOrientation, float partialTicks){
		component.core.getRenderer().adjustPositionOrientation(component.core, entityPositionDelta, interpolatedOrientation, partialTicks);
	}
}
