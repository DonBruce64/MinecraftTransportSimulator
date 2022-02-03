package minecrafttransportsimulator.rendering.instances;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

public class RenderBullet extends ARenderEntityDefinable<EntityBullet>{

	@Override
	protected void renderBoundingBoxes(EntityBullet entity, Point3d entityPositionDelta){
		//Draw the box for the bullet.
		GL11.glTranslated(entityPositionDelta.x, entityPositionDelta.y, entityPositionDelta.z);
		entity.boundingBox.renderable.render();
		GL11.glTranslated(-entityPositionDelta.x, -entityPositionDelta.y, -entityPositionDelta.z);
	}
}
