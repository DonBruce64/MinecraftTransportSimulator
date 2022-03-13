package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.jsondefs.JSONMuzzle;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

public class RenderPart extends ARenderEntityDefinable<APart>{
	private static final Point3D bulletPosition = new Point3D();
	private static final Point3D bulletVelocity = new Point3D();
	private static final RotationMatrix bulletOrientation = new RotationMatrix();
	
	@Override
	public boolean disableRendering(APart part, float partialTicks){
		return super.disableRendering(part, partialTicks) || part.isFake() || part.isInvisible;
	}
	
	@Override
	public void renderBoundingBoxes(APart part, TransformationMatrix transform){
		if(!part.entityOn.areVariablesBlocking(part.placementDefinition, InterfaceClient.getClientPlayer())){
			super.renderBoundingBoxes(part, transform);
			//Draw the gun muzzle bounding boxes.
			if(part instanceof PartGun){
				PartGun gun = (PartGun) part;
				for(JSONMuzzle muzzle : gun.definition.gun.muzzleGroups.get(gun.currentMuzzleGroupIndex).muzzles){
					gun.setBulletSpawn(bulletPosition, bulletVelocity, bulletOrientation, muzzle);
					new BoundingBox(bulletPosition, 0.25, 0.25, 0.25).renderWireframe(part, transform, null, ColorRGB.BLUE);
				}
			}
		}
	}
}
