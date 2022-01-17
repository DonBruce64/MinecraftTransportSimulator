package minecrafttransportsimulator.rendering.instances;

import java.util.Set;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3dPlus;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.SignalGroup;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.rendering.components.ARenderTileEntityBase;

public class RenderDecor extends ARenderTileEntityBase<TileEntityDecor>{
	
	@Override
	protected void renderModel(TileEntityDecor decor, boolean blendingEnabled, float partialTicks){
		super.renderModel(decor, blendingEnabled, partialTicks);
		
		//Render lane holo-boxes if we are a signal controller that's being edited.
		if(blendingEnabled && decor instanceof TileEntitySignalController){
			TileEntitySignalController controller = (TileEntitySignalController) decor;
			if(controller.unsavedClientChangesPreset || InterfaceRender.shouldRenderBoundingBoxes()){
				for(Set<SignalGroup> signalGroupSet : controller.signalGroups.values()){
		        	for(SignalGroup signalGroup : signalGroupSet){ 
						if(signalGroup.signalLineWidth != 0 && controller.intersectionProperties.get(signalGroup.axis).isActive){
							//Get relative center coord.
							//First start with center signal line, which is distance from center of intersection to the edge of the stop line..
							Point3dPlus boxRelativeCenter = new Point3dPlus(signalGroup.signalLineCenter);
							//Add 8 units to center on the box which is 16 units long.
							boxRelativeCenter.add(0, 0, 8);
							//Rotate box based on signal orientation to proper signal.
							boxRelativeCenter.rotateY(signalGroup.axis.yRotation);
							
							//Add delta from controller to intersection center.
							boxRelativeCenter.add(controller.intersectionCenterPoint);
							boxRelativeCenter.sub(controller.position);
							
							//Create bounding box.
							BoundingBox box = new BoundingBox(boxRelativeCenter, signalGroup.signalLineWidth/2D, 2, 8);
							signalGroup.renderable.setHolographicBoundingBox(box);
							
							//Undo controller transform and then translate to the box position and render.
							//Will need to rotate to the proper orientation once there.
							GL11.glPushMatrix();
							InterfaceRender.applyTransformOpenGL(controller.orientation, true);
							GL11.glTranslated(box.globalCenter.x, box.globalCenter.y, box.globalCenter.z);
							GL11.glRotated(signalGroup.axis.yRotation, 0, 1, 0);
							signalGroup.renderable.render();
							GL11.glPopMatrix();
							
						}
		        	}
				}
			}
		}
	}
}
