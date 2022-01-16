package minecrafttransportsimulator.rendering.instances;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3dPlus;
import minecrafttransportsimulator.entities.instances.EntityParticle;
import minecrafttransportsimulator.rendering.components.ARenderEntity;

public class RenderParticle extends ARenderEntity<EntityParticle>{
	
	@Override
	protected void renderModel(EntityParticle particle, boolean blendingEnabled, float partialTicks){
		if(blendingEnabled){
			particle.render(partialTicks);
		}
	}

	@Override
	protected void renderBoundingBoxes(EntityParticle entity, Point3dPlus entityPositionDelta){
		//Draw the box for the particle.
		GL11.glTranslated(entityPositionDelta.x, entityPositionDelta.y, entityPositionDelta.z);
		entity.boundingBox.renderable.render();
		GL11.glTranslated(-entityPositionDelta.x, -entityPositionDelta.y, -entityPositionDelta.z);
	}
}
