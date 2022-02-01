package minecrafttransportsimulator.rendering.instances;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.jsondefs.JSONMuzzle;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

public class RenderPart extends ARenderEntityDefinable<APart>{
	
	@Override
	public boolean disableRendering(APart part, float partialTicks){
		return super.disableRendering(part, partialTicks) || part.isFake() || part.isDisabled;
	}
	
	@Override
	public void adjustPositionRotation(APart part, Point3d entityPositionDelta, Point3d entityRotationDelta, float partialTicks){
		//Rotate the part according to its rendering rotation if we need to do so.
		entityRotationDelta.add(part.getRenderingRotation(partialTicks));
	}
	
	@Override
	protected void renderBoundingBoxes(APart part, Point3d entityPositionDelta){
		if(!part.entityOn.areVariablesBlocking(part.placementDefinition, InterfaceClient.getClientPlayer())){
			super.renderBoundingBoxes(part, entityPositionDelta);
			//Draw the gun muzzle bounding boxes.
			if(part instanceof PartGun){
				PartGun gun = (PartGun) part;
				Point3d bulletPosition = new Point3d();
				Point3d bulletVelocity = new Point3d();
				for(JSONMuzzle muzzle : gun.definition.gun.muzzleGroups.get(gun.currentMuzzleGroupIndex).muzzles){
					gun.setBulletSpawn(bulletPosition, bulletVelocity, muzzle);
					bulletPosition.subtract(gun.position).add(entityPositionDelta);
					GL11.glTranslated(bulletPosition.x, bulletPosition.y, bulletPosition.z);
					gun.muzzleWireframe.render();
					GL11.glTranslated(-bulletPosition.x, -bulletPosition.y, -bulletPosition.z);
				}
			}
		}
	}
}
