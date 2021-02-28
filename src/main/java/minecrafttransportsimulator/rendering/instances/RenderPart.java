package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition.AnimationComponentType;
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
		if(part.isFake()){
			return true;
		}else{
			//Check to see if the part has a visibility animation and it's set to not be visible.
			if(part.placementDefinition.animations != null){
				for(JSONAnimationDefinition animation : part.placementDefinition.animations){
					if(animation.animationType.equals(AnimationComponentType.VISIBILITY)){
						double value = part.getAnimator().getAnimatedVariableValue(part, animation, 0, null, partialTicks);
						if(value < animation.clampMin || value > animation.clampMax){
							return true;
						}
					}
				}
			}
		}
		return false;
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
}
