package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.rendering.components.ARenderEntity;

public final class RenderPart extends ARenderEntity<APart>{
		
	@Override
	public String getTexture(APart part){
		if(!part.definition.generic.useVehicleTexture){
			return part.definition.getTextureLocation(part.subName);
		}else{
			return part.entityOn.definition.getTextureLocation(part.entityOn.subName);
		}
	}
	
	@Override
	public boolean disableMainRendering(APart part, float partialTicks){
		return part.isFake() || part.isDisabled;
	}
	
	@Override
	public boolean isMirrored(APart part){
		return ((part.placementOffset.x < 0 && !part.placementDefinition.inverseMirroring) || (part.placementOffset.x >= 0 && part.placementDefinition.inverseMirroring)) && !part.disableMirroring;
	}
	
	@Override
	public void adjustPositionRotation(APart part, float partialTicks, Point3d entityPosition, Point3d entityRotation){
		//Rotate the part according to its rendering rotation if we need to do so.
		entityRotation.add(part.getRenderingRotation(partialTicks, false));
	}
	
	@Override
	public double getScale(APart part, float partialTicks){
		return part.prevScale + (part.scale - part.prevScale)*partialTicks;
	}
}
