package minecrafttransportsimulator.rendering.instances;

import java.util.Set;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Matrix4dPlus;
import minecrafttransportsimulator.baseclasses.Point3dPlus;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.SignalDirection;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.SignalGroup;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.rendering.components.ARenderTileEntityBase;

public class RenderDecor extends ARenderTileEntityBase<TileEntityDecor>{
	
	@Override
	protected void renderHolographicBoxes(TileEntityDecor decor, Matrix4dPlus transform){
		//Render lane holo-boxes if we are a signal controller that's being edited.
		if(decor instanceof TileEntitySignalController){
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
							boxRelativeCenter.y += 1;
							
							//Create bounding box and transform for it..
							BoundingBox box = new BoundingBox(boxRelativeCenter, signalGroup.signalLineWidth/2D, 1, 8);
							Matrix4dPlus newTransform = new Matrix4dPlus(transform);
							newTransform.translate(boxRelativeCenter);
							newTransform.rotate(signalGroup.axis.yRotation, 0, 1, 0);
							box.renderHolographic(newTransform, null, signalGroup.direction.equals(SignalDirection.LEFT) ? ColorRGB.BLUE : (signalGroup.direction.equals(SignalDirection.RIGHT) ? ColorRGB.YELLOW : ColorRGB.GREEN));
						}
		        	}
				}
			}
		}
	}
}
