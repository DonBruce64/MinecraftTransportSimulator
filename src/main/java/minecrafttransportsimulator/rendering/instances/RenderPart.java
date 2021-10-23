package minecrafttransportsimulator.rendering.instances;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.jsondefs.JSONPart.JSONPartGun.JSONMuzzle;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.rendering.components.ARenderEntity;

public final class RenderPart extends ARenderEntity<APart>{
	
	@Override
	public boolean disableRendering(APart part, float partialTicks){
		return part.isFake() || part.isDisabled;
	}
	
	@Override
	public boolean isMirrored(APart part){
		return ((part.placementOffset.x < 0 && !part.placementDefinition.inverseMirroring) || (part.placementOffset.x >= 0 && part.placementDefinition.inverseMirroring)) && !part.disableMirroring;
	}
	
	@Override
	public void adjustPositionRotation(APart part, float partialTicks, Point3d entityPosition, Point3d entityRotation){
		//Rotate the part according to its rendering rotation if we need to do so.
		entityRotation.add(part.getRenderingRotation(partialTicks));
	}
	
	@Override
	public double getScale(APart part, float partialTicks){
		return part.prevScale + (part.scale - part.prevScale)*partialTicks;
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
				InterfaceRender.setColorState(ColorRGB.BLUE);
				for(JSONMuzzle muzzle : gun.definition.gun.muzzleGroups.get(gun.currentMuzzleGroupIndex).muzzles){
					gun.setBulletSpawn(bulletPosition, bulletVelocity, muzzle);
					bulletPosition.subtract(gun.position).add(entityPositionDelta);
					GL11.glTranslated(bulletPosition.x, bulletPosition.y, bulletPosition.z);
					RenderBoundingBox.renderWireframe(new BoundingBox(bulletPosition, 0.25, 0.25, 0.25));
					GL11.glTranslated(-bulletPosition.x, -bulletPosition.y, -bulletPosition.z);
				}
				InterfaceRender.setColorState(ColorRGB.WHITE);
			}
		}
	}
}
