package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Matrix4dPlus;
import minecrafttransportsimulator.baseclasses.Point3dPlus;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.jsondefs.JSONMuzzle;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

public class RenderPart extends ARenderEntityDefinable<APart>{
	
	@Override
	public boolean disableRendering(APart part, float partialTicks){
		return super.disableRendering(part, partialTicks) || part.isFake() || part.isInvisible;
	}
	
	@Override
	protected void renderBoundingBoxes(APart part, Matrix4dPlus transform){
		if(!part.entityOn.areVariablesBlocking(part.placementDefinition, InterfaceClient.getClientPlayer())){
			super.renderBoundingBoxes(part, transform);
			//Draw the gun muzzle bounding boxes.
			if(part instanceof PartGun){
				PartGun gun = (PartGun) part;
				Point3dPlus bulletPosition = new Point3dPlus();
				Point3dPlus bulletVelocity = new Point3dPlus();
				for(JSONMuzzle muzzle : gun.definition.gun.muzzleGroups.get(gun.currentMuzzleGroupIndex).muzzles){
					//FIXME this is probably not right, but we need guns working to test.
					gun.setBulletSpawn(bulletPosition, bulletVelocity, muzzle);
					bulletPosition.subtract(gun.position);
					gun.muzzleWireframe.transform.set(transform);
					gun.muzzleWireframe.transform.translate(bulletPosition);
					gun.muzzleWireframe.render();
				}
			}
		}
	}
}
