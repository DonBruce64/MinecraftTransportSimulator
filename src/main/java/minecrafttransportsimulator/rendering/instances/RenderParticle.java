package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.instances.EntityParticle;
import minecrafttransportsimulator.rendering.components.ARenderEntity;

public class RenderParticle extends ARenderEntity<EntityParticle>{
	
	@Override
	protected void renderModel(EntityParticle particle, TransformationMatrix transform, boolean blendingEnabled, float partialTicks){
		if(blendingEnabled){
			particle.render(transform, partialTicks);
		}
	}
}
