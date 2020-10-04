package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public final class PartSeat extends APart{
	
	public PartSeat(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, ItemPart item, IWrapperNBT data, APart parentPart){
		super(vehicle, packVehicleDef, item, data, parentPart);
	}
	
	@Override
	public boolean interact(IWrapperPlayer player){
		//See if we can interact with the seats of this vehicle.
		//This can happen if the vehicle is not locked, or we're already inside a locked vehicle.
		if(!vehicle.locked || vehicle.equals(player.getEntityRiding())){
			IWrapperEntity riderForSeat = vehicle.locationRiderMap.get(placementOffset);
			if(riderForSeat != null){
				//We already have a rider for this seat.  If it's not us, mark the seat as taken.
				//If it's an entity that can be leashed, dismount the entity and leash it.
				if(riderForSeat instanceof IWrapperPlayer){
					if(!player.equals(riderForSeat)){
						player.sendPacket(new PacketPlayerChatMessage("interact.failure.seattaken"));
					}
				}else if(!riderForSeat.leashTo(player)){
					//Can't leash up this entity, so mark the seat as taken.
					player.sendPacket(new PacketPlayerChatMessage("interact.failure.seattaken"));
				}
			}else{
				//Seat is free.  Either mount this seat, or if we have a leashed animal, set it in that seat.
				IWrapperEntity leashedEntity = player.getLeashedEntity();
				if(leashedEntity != null){
					vehicle.addRider(leashedEntity, placementOffset);
				}else{
					//Didn't find an animal.  Just mount the player.
					//Don't mount them if they are sneaking, however.  This will confuse MC.
					if(!player.isSneaking()){
						vehicle.addRider(player, placementOffset);
					}
				}
			}
		}else{
			player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehiclelocked"));
		}
		return true;
    }
	
	@Override
	public void remove(){
		super.remove();
		IWrapperEntity rider = vehicle.locationRiderMap.get(placementOffset);
		if(rider != null){
			vehicle.removeRider(rider, null);
		}
	}

	@Override
	public float getWidth(){
		return 0.75F;
	}

	@Override
	public float getHeight(){
		return 0.75F;
	}
}
