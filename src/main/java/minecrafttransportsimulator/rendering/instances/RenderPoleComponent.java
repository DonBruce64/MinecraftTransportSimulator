package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

public class RenderPoleComponent extends ARenderEntityDefinable<ATileEntityPole_Component>{
	
	@Override
	public void adjustPositionRotation(ATileEntityPole_Component component, Point3d entityPositionDelta, Point3d entityRotationDelta, float partialTicks){
		component.core.getRenderer().adjustPositionRotation(component.core, entityPositionDelta, entityRotationDelta, partialTicks);
	}
}
