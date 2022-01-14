package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Orientation3d;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

public class RenderPoleComponent extends ARenderEntityDefinable<ATileEntityPole_Component>{
	
	@Override
	public void adjustPositionOrientation(ATileEntityPole_Component component, Point3d entityPositionDelta, Orientation3d interpolatedOrientation, float partialTicks){
		component.core.getRenderer().adjustPositionOrientation(component.core, entityPositionDelta, interpolatedOrientation, partialTicks);
	}
}
