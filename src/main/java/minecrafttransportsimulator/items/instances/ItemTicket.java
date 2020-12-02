package minecrafttransportsimulator.items.instances;

import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

public class ItemTicket extends AItemBase implements IItemVehicleInteractable{
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data){
		for(byte i=1; i<=3; ++i){
			tooltipLines.add(MasterLoader.coreInterface.translate("info.item.ticket.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, IWrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(rightClick && !vehicle.world.isClient()){
			if(player.isSneaking()){
				Iterator<IWrapperEntity> iterator = vehicle.locationRiderMap.inverse().keySet().iterator();
				while(iterator.hasNext()){
					IWrapperEntity entity = iterator.next();
					if(!(entity instanceof IWrapperPlayer)){
						vehicle.removeRider(entity, iterator);
					}
				}
			}else{
				vehicle.world.loadEntities(new BoundingBox(player.getPosition(), 8D, 8D, 8D), vehicle);
			}
		}
		return CallbackType.NONE;
	}
}
