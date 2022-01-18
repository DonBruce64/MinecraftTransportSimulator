package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Matrix4dPlus;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

public class RenderBullet extends ARenderEntityDefinable<EntityBullet>{

	@Override
	protected void renderBoundingBoxes(EntityBullet entity, Matrix4dPlus transform){
		entity.boundingBox.renderWireframe(entity, transform, null);
	}
}
