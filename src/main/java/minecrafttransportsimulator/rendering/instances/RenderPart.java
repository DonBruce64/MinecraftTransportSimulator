package minecrafttransportsimulator.rendering.instances;

import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition.AnimationComponentType;
import minecrafttransportsimulator.rendering.components.ARenderEntity;
import minecrafttransportsimulator.rendering.components.InterfaceRender;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.rendering.components.RenderableModelObject;

public final class RenderPart extends ARenderEntity<APart>{
	
	@Override
	public void renderModel(APart part, float partialTicks){
		if(!part.isFake()){
			//Check to see if the part has a visibility animation and it's set to not be visible.
			if(part.placementDefinition.animations != null){
				for(JSONAnimationDefinition animation : part.placementDefinition.animations){
					if(animation.animationType.equals(AnimationComponentType.VISIBILITY)){
						double value = part.getAnimator().getAnimatedVariableValue(part, animation, 0, null, partialTicks);
						if(value < animation.clampMin || value > animation.clampMax){
							return;
						}
					}
				}
			}
			
			//Not a fake part, do rendering.
			GL11.glPushMatrix();
			String partModelLocation = part.definition.getModelLocation();
			if(!objectLists.containsKey(partModelLocation)){
				Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(partModelLocation);
				objectLists.put(partModelLocation, OBJParser.generateRenderables(part, partModelLocation, parsedModel, part.definition.rendering != null ? part.definition.rendering.animatedObjects : null));
			}
			
			//If we aren't using the vehicle texture, bind the texture for this part.
			//Otherwise, bind the vehicle texture as it may have been un-bound prior to this from another part.
			if(!part.definition.generic.useVehicleTexture){
				InterfaceRender.setTexture(part.definition.getTextureLocation(part.subName));
			}else{
				InterfaceRender.setTexture(part.entityOn.definition.getTextureLocation(part.entityOn.subName));
			}
			
			//Rotate the part prior to rendering the displayList.
			//We will already have been translated to our position prior to this call.
			Point3d renderingRotation = part.getRenderingRotation(partialTicks, false);
			if(!renderingRotation.isZero()){
				GL11.glRotated(renderingRotation.y, 0, 1, 0);
				GL11.glRotated(renderingRotation.x, 1, 0, 0);
				GL11.glRotated(renderingRotation.z, 0, 0, 1);
			}
			
			//Mirror the model if we need to do so.
			//If we are a sub-part, don't mirror as we'll already be mirrored.
			boolean mirrored = ((part.placementOffset.x < 0 && !part.placementDefinition.inverseMirroring) || (part.placementOffset.x >= 0 && part.placementDefinition.inverseMirroring)) && !part.disableMirroring && (part.definition.ground == null || !part.definition.ground.isTread || part.placementDefinition.isSpare);
			if(mirrored){
				GL11.glScalef(-1.0F, 1.0F, 1.0F);
				GL11.glCullFace(GL11.GL_FRONT);
			}
			
			//Render all modelObjects.
			List<RenderableModelObject<APart>> modelObjects = objectLists.get(partModelLocation);
			for(RenderableModelObject<APart> modelObject : modelObjects){
				if(modelObject.applyAfter == null){
					modelObject.render(part, partialTicks, modelObjects);
				}
			}
			
			//Render any static text.
			InterfaceRender.renderTextMarkings(part, null);
			
			//Set cullface back to normal if we switched it and pop matrix.
			if(mirrored){
				GL11.glCullFace(GL11.GL_BACK);
			}
			GL11.glPopMatrix();
		}
	}
}
