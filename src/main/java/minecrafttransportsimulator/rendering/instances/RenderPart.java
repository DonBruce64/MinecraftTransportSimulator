package minecrafttransportsimulator.rendering.instances;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.rendering.components.ARenderEntity;

public final class RenderPart extends ARenderEntity<APart>{
		
	@Override
	public String getTexture(APart part){
		return part.definition.generic.useVehicleTexture ? part.entityOn.definition.getTextureLocation(part.entityOn.subName) : super.getTexture(part);
	}
	
	@Override
	public boolean disableMainRendering(APart part, float partialTicks){
		return part.isFake() || part.isDisabled;
	}
	
	@Override
	public boolean isMirrored(APart part){
		return ((part.placementOffset.x < 0 && !part.placementDefinition.inverseMirroring) || (part.placementOffset.x >= 0 && part.placementDefinition.inverseMirroring)) && !part.disableMirroring;
	}
	
	@Override
	public void adjustPositionRotation(APart part, float partialTicks, Point3d entityPosition, Point3d entityRotation){
		//Rotate the part according to its rendering rotation if we need to do so.
		entityRotation.add(part.getRenderingRotation(partialTicks, false));
	}
	
	@Override
	public double getScale(APart part, float partialTicks){
		return part.prevScale + (part.scale - part.prevScale)*partialTicks;
	}
	
	@Override
	protected void renderBoundingBoxes(APart part, Point3d entityPositionDelta){
		super.renderBoundingBoxes(part, entityPositionDelta);
		//Draw the gun muzzle bounding boxes.
		if(part instanceof PartGun){
			InterfaceRender.setColorState(0.0F, 0.0F, 1.0F, 1.0F);
			Point3d origin = ((PartGun) part).getFiringOrigin().rotateFine(part.localAngles).rotateFine(part.entityOn.angles).add(entityPositionDelta);
			GL11.glTranslated(origin.x, origin.y, origin.z);
			RenderBoundingBox.renderWireframe(new BoundingBox(origin, 0.25, 0.25, 0.25));
			GL11.glTranslated(-origin.x, -origin.y, -origin.z);
			InterfaceRender.setColorState(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}
}
