package minecrafttransportsimulator.rendering.instances;

import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;

/**Main render class for all vehicles.  Renders the vehicle, along with all parts.
 * As entities don't render above 255 well due to the new chunk visibility system, 
 * this code is called both from the regular render loop and manually from
 * the event-based last pass.  This pass is -1, and should allow both the regular
 * and blending operations to run.
 *
 * @author don_bruce
 */
public class RenderVehicle extends ARenderEntityDefinable<EntityVehicleF_Physics>{	
	
	@Override
	protected void renderHolographicBoxes(EntityVehicleF_Physics vehicle, TransformationMatrix transform){
		//If we are holding a part, render the valid slots.
		//If we are holding a scanner, render all slots.
		vehicle.world.beginProfiling("PartHoloboxes", true);
		WrapperPlayer player = InterfaceClient.getClientPlayer();
		AItemBase heldItem = player.getHeldItem();
		AItemPart heldPart = heldItem instanceof AItemPart ? (AItemPart) heldItem : null;
		boolean holdingScanner = player.isHoldingItemType(ItemComponentType.SCANNER);
		if(heldPart != null || holdingScanner){
			if(holdingScanner){
				for(Entry<BoundingBox, JSONPartDefinition> partSlotEntry : vehicle.allPartSlotBoxes.entrySet()){
					JSONPartDefinition placementDefinition = partSlotEntry.getValue();
					if(!vehicle.areVariablesBlocking(placementDefinition, player) && (placementDefinition.validSubNames == null || placementDefinition.validSubNames.contains(vehicle.subName))){
						BoundingBox box = partSlotEntry.getKey();
						Point3D boxCenterDelta = box.globalCenter.copy().subtract(vehicle.position);
						box.renderHolographic(transform, boxCenterDelta, ColorRGB.BLUE);
					}
				}
			}else{
				for(Entry<BoundingBox, JSONPartDefinition> partSlotEntry : vehicle.activePartSlotBoxes.entrySet()){
					boolean isHoldingCorrectTypePart = false;
					boolean isHoldingCorrectParamPart = false;
					
					if(heldPart.isPartValidForPackDef(partSlotEntry.getValue(), vehicle.subName, false)){
						isHoldingCorrectTypePart = true;
						if(heldPart.isPartValidForPackDef(partSlotEntry.getValue(), vehicle.subName, true)){
							isHoldingCorrectParamPart = true;
						}
					}
							
					if(isHoldingCorrectTypePart){
						BoundingBox box = partSlotEntry.getKey();
						Point3D boxCenterDelta = box.globalCenter.copy().subtract(vehicle.position);
						box.renderHolographic(transform, boxCenterDelta, isHoldingCorrectParamPart ? ColorRGB.GREEN : ColorRGB.RED);
					}
				}
			}
		}
		vehicle.world.endProfiling();
	}
	
	@Override
	public void renderBoundingBoxes(EntityVehicleF_Physics vehicle, TransformationMatrix transform){
		super.renderBoundingBoxes(vehicle, transform);
		for(BoundingBox box : vehicle.groundDeviceCollective.getGroundBounds()){
			box.renderWireframe(vehicle, transform, null, ColorRGB.BLUE);
		}
	}
}
