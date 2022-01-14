package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

public class RenderPoleComponent extends ARenderEntityDefinable<ATileEntityPole_Component>{
	
	@Override
	public void adjustPosition(ATileEntityPole_Component component, Point3d entityPositionDelta, float partialTicks){
		component.core.getRenderer().adjustPosition(component.core, entityPositionDelta, partialTicks);
	}
}
