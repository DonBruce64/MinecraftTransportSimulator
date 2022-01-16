package minecrafttransportsimulator.rendering.instances;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3dPlus;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

public class RenderBullet extends ARenderEntityDefinable<EntityBullet>{

	@Override
	protected void renderBoundingBoxes(EntityBullet entity, Point3dPlus entityPositionDelta){
		//Draw the box for the bullet.
		GL11.glTranslated(entityPositionDelta.x, entityPositionDelta.y, entityPositionDelta.z);
		entity.boundingBox.renderable.render();
		GL11.glTranslated(-entityPositionDelta.x, -entityPositionDelta.y, -entityPositionDelta.z);
	}
}
