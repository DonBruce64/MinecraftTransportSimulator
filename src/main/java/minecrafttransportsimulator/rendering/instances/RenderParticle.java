package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Matrix4dPlus;
import minecrafttransportsimulator.entities.instances.EntityParticle;
import minecrafttransportsimulator.rendering.components.ARenderEntity;

public class RenderParticle extends ARenderEntity<EntityParticle>{
	
	@Override
	protected void renderModel(EntityParticle particle, Matrix4dPlus transform, boolean blendingEnabled, float partialTicks){
		if(blendingEnabled){
			particle.render(partialTicks);
		}
	}

	@Override
	protected void renderBoundingBoxes(EntityParticle entity, Matrix4dPlus transform){
		entity.boundingBox.renderWireframe(entity, transform, null);
	}
}
