package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartSeat;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public final class PartSeat extends APart{
	public boolean canControlGuns;
	public ItemPart activeGun;
	public int gunIndex;
	
	public PartSeat(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, ItemPart item, WrapperNBT data, APart parentPart){
		super(vehicle, packVehicleDef, item, data, parentPart);
		this.activeGun = PackParserSystem.getItem(data.getString("activeGunPackID"), data.getString("activeGunSystemName"), data.getString("activeGunSubName"));
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		//See if we can interact with the seats of this vehicle.
		//This can happen if the vehicle is not locked, or we're already inside a locked vehicle.
		if(!vehicle.locked || vehicle.equals(player.getEntityRiding())){
			WrapperEntity riderForSeat = vehicle.locationRiderMap.get(placementOffset);
			if(riderForSeat != null){
				//We already have a rider for this seat.  If it's not us, mark the seat as taken.
				//If it's an entity that can be leashed, dismount the entity and leash it.
				if(riderForSeat instanceof WrapperPlayer){
					if(!player.equals(riderForSeat)){
						player.sendPacket(new PacketPlayerChatMessage("interact.failure.seattaken"));
					}
				}else if(!riderForSeat.leashTo(player)){
					//Can't leash up this entity, so mark the seat as taken.
					player.sendPacket(new PacketPlayerChatMessage("interact.failure.seattaken"));
				}
			}else{
				//Seat is free.  Either mount this seat, or if we have a leashed animal, set it in that seat.
				WrapperEntity leashedEntity = player.getLeashedEntity();
				if(leashedEntity != null){
					vehicle.addRider(leashedEntity, placementOffset);
				}else{
					//Didn't find an animal.  Just mount the player.
					//Don't mount them if they are sneaking, however.  This will confuse MC.
					if(!player.isSneaking()){
						vehicle.addRider(player, placementOffset);
						//If this seat can control a gun, and isn't controlling one, set it now.
						//This prevents the need to select a gun when initially mounting.
						//If we do have an active gun, validate that it's still correct.
						if(activeGun == null){
							setNextActiveGun();
							InterfacePacket.sendToAllClients(new PacketVehiclePartSeat(this));
						}else{
							for(ItemPart gunType : vehicle.guns.keySet()){
								for(PartGun gun : vehicle.guns.get(gunType)){
									if(player.equals(gun.getController())){
										if(gunType.equals(activeGun)){
											return true;
										}
									}
								}
							}
							
							//Invalid active gun detected.  Select a new one.
							activeGun = null;
							setNextActiveGun();
							InterfacePacket.sendToAllClients(new PacketVehiclePartSeat(this));
						}
					}
				}
			}
		}else{
			player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehiclelocked"));
		}
		return true;
    }
	
	/**
	 * Sets the next active gun for this seat.  Active guns are queried by checking guns to
	 * see if this rider can control them.  If so, then the active gun is set to that gun type.
	 */
	public void setNextActiveGun(){
		WrapperEntity rider = vehicle.locationRiderMap.get(placementOffset);
		//Iterate over all the gun types, attempting to get the type after our selected type.
		//If we don't have an active gun, just get the next possible unit.
		if(activeGun == null){
			for(ItemPart gunType : vehicle.guns.keySet()){
				for(PartGun gun : vehicle.guns.get(gunType)){
					if(rider.equals(gun.getController())){
						activeGun = gunType;
						gunIndex = 0;
						return;
					}
				}
			}
		}else{
			ItemPart firstPossibleGun = null;
			ItemPart currentGun = activeGun;
			ItemPart nextActiveGun = null;
			boolean pastActiveGun = false;
			for(ItemPart gunType : vehicle.guns.keySet()){
				for(PartGun gun : vehicle.guns.get(gunType)){
					if(rider.equals(gun.getController())){
						if(pastActiveGun){
							nextActiveGun = gunType;
							break;
						}else{
							if(firstPossibleGun == null){
								firstPossibleGun = gunType;
							}
							if(gunType.equals(currentGun)){
								if(gunType.definition.gun.fireSolo){
									//If this type of gun can't be grouped, iterate through the different instances.
									//Once we run out of this type, move onto the next type.
									if(vehicle.guns.get(gunType).size() <= ++gunIndex){
										pastActiveGun = true;
									}else{
										nextActiveGun = gunType;
									}
								}else{
									pastActiveGun = true;
								}
							}
						}
						break;
					}
					if(nextActiveGun != null){
						break;
					}
				}
			}
			//If we didn't find an active gun, try to get another one.
			//This will be our first gun, unless we had an active gun and we can disable our gun.
			//In this case, we will just set our active gun to null.
			if(nextActiveGun == null){
				if(activeGun == null || !vehicleDefinition.canDisableGun){
					activeGun = firstPossibleGun;
				}else{
					activeGun = null;
				}
				gunIndex = 0;
			}else{
				activeGun = nextActiveGun;
			}
		}
	}
	
	@Override
	public void update(){
		super.update();
		if(!canControlGuns && activeGun != null){
			canControlGuns = true;
		}
	}
	
	@Override
	public void remove(){
		super.remove();
		WrapperEntity rider = vehicle.locationRiderMap.get(placementOffset);
		if(rider != null){
			vehicle.removeRider(rider, null);
		}
	}
	
	@Override
	public WrapperNBT getData(){
		WrapperNBT data = super.getData();
		if(activeGun != null){
			data.setString("activeGunPackID", activeGun.definition.packID);
			data.setString("activeGunSystemName", activeGun.definition.systemName);
			data.setString("activeGunSubName", activeGun.subName);
		}
		return data;
	}
}
