package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.rendering.components.ARenderEntity;

public class RenderPoleComponent extends ARenderEntity<ATileEntityPole_Component>{
	
	@Override
	public String getTexture(ATileEntityPole_Component component){
		return component.definition.getTextureLocation(component.subName);
	}
	
	@Override
	public void adjustPositionRotation(ATileEntityPole_Component component, float partialTicks, Point3d entityPosition, Point3d entityRotation){
		component.core.getRenderer().adjustPositionRotation(component.core, partialTicks, entityPosition, entityRotation);
	}
}
