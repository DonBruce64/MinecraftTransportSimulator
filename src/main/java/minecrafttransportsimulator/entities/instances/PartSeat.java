package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketPartSeat;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.PackParserSystem;

public final class PartSeat extends APart{
	public boolean canControlGuns;
	public ItemPartGun activeGun;
	public int gunIndex;
	
	public PartSeat(AEntityF_Multipart<?> entityOn, WrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn, placingPlayer, placementDefinition, data, parentPart);
		this.activeGun = PackParserSystem.getItem(data.getString("activeGunPackID"), data.getString("activeGunSystemName"), data.getString("activeGunSubName"));
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		//See if we can interact with the seats of this vehicle.
		//This can happen if the vehicle is not locked, or we're already inside a locked vehicle.
		if(isActive){
			if(!entityOn.locked || entityOn.equals(player.getEntityRiding())){
				WrapperEntity riderForSeat = entityOn.locationRiderMap.get(placementOffset);
				if(riderForSeat != null){
					//We already have a rider for this seat.  If it's not us, mark the seat as taken.
					//If it's an entity that can be leashed, dismount the entity and leash it.
					if(riderForSeat instanceof WrapperPlayer){
						if(!player.equals(riderForSeat)){
							player.sendPacket(new PacketPlayerChatMessage(player, "interact.failure.seattaken"));
						}
					}else if(!riderForSeat.leashTo(player)){
						//Can't leash up this entity, so mark the seat as taken.
						player.sendPacket(new PacketPlayerChatMessage(player, "interact.failure.seattaken"));
					}
				}else{
					//Seat is free.  Either mount this seat, or if we have a leashed animal, set it in that seat.
					WrapperEntity leashedEntity = player.getLeashedEntity();
					if(leashedEntity != null){
						entityOn.addRider(leashedEntity, placementOffset);
					}else{
						//Didn't find an animal.  Just mount the player.
						//Don't mount them if they are sneaking, however.  This will confuse MC.
						if(!player.isSneaking()){
							//Check if the rider is riding something before adding them.
							//If they aren't riding us our the entity we are on, we need to remove them.
							if(player.getEntityRiding() != null && !entityOn.equals(player.getEntityRiding())){
								player.getEntityRiding().removeRider(player, null);
							}
							entityOn.addRider(player, placementOffset);
							//If this seat can control a gun, and isn't controlling one, set it now.
							//This prevents the need to select a gun when initially mounting.
							//If we do have an active gun, validate that it's still correct.
							if(activeGun == null){
								setNextActiveGun();
								InterfacePacket.sendToAllClients(new PacketPartSeat(this));
							}else{
								for(AItemPart partItem : entityOn.partsByItem.keySet()){
									if(partItem.definition.gun != null){
										for(APart part : entityOn.partsByItem.get(partItem)){
											if(player.equals(((PartGun) part).getController())){
												if(partItem.equals(activeGun)){
													return true;
												}
											}
										}
									}
								}
								
								//Invalid active gun detected.  Select a new one.
								activeGun = null;
								setNextActiveGun();
								InterfacePacket.sendToAllClients(new PacketPartSeat(this));
							}
						}
					}
				}
			}else{
				player.sendPacket(new PacketPlayerChatMessage(player, "interact.failure.vehiclelocked"));
			}
		}
		return true;
    }
	
	/**
	 * Sets the next active gun for this seat.  Active guns are queried by checking guns to
	 * see if this rider can control them.  If so, then the active gun is set to that gun type.
	 */
	public void setNextActiveGun(){
		//If we don't have an active gun, just get the next possible unit.
		if(activeGun == null){
			WrapperEntity rider = entityOn.locationRiderMap.get(placementOffset);
			for(AItemPart partItem : entityOn.partsByItem.keySet()){
				if(partItem instanceof ItemPartGun){
					for(APart part : entityOn.partsByItem.get(partItem)){
						if(rider.equals(((PartGun) part).getController())){
							activeGun = (ItemPartGun) partItem;
							gunIndex = 0;
							return;
						}
					}
				}
			}
		}else{
			//If we didn't find an active gun, try to get another one.
			//This will be our first gun, unless we had an active gun and we can disable our gun.
			//In this case, we will just set our active gun to null.
			activeGun = getNextActiveGun();
		}
	}
	
	/**
	 * Helper method to get the next active gun in the gun listings.
	 */
	private ItemPartGun getNextActiveGun(){
		WrapperEntity rider = entityOn.locationRiderMap.get(placementOffset);
		boolean pastActiveGun = false;
		ItemPartGun firstPossibleGun = null;
		
		//Iterate over all the gun types, attempting to get the type after our selected type.
		for(AItemPart partItem : entityOn.partsByItem.keySet()){
			if(partItem instanceof ItemPartGun){
				for(APart part : entityOn.partsByItem.get(partItem)){
					
					//Can the player control this gun, or is it for another seat?
					if(rider.equals(((PartGun) part).getController())){
						//If we already found our active gun in our gun list, we use the next entry as our next gun.
						if(pastActiveGun){
							return (ItemPartGun) partItem;
						}else{
							//Add the first possible gun in case we go all the way around.
							if(firstPossibleGun == null){
								firstPossibleGun = (ItemPartGun) partItem;
							}
							//If the gun type is the same as the active gun, check if it's set to fireSolo.
							//If we, we didn't group it and need to go to the next active gun with that type.
							if(partItem.equals(activeGun)){
								if(part.definition.gun.fireSolo){
									if(entityOn.partsByItem.get(partItem).size() <= ++gunIndex){
										gunIndex = 0;
										pastActiveGun = true;
									}else{
										return (ItemPartGun) partItem;
									}
								}else{
									pastActiveGun = true;
								}
							}
							break;
						}
					}
				}
			}
		}
		
		//Got down here.  Either we don't have a gun, or we need the first.
		//If our current gun is active, and we have the first, and we can disable guns,
		//return null.  This will make the guns inactive this cycle.
		return placementDefinition.canDisableGun && activeGun != null ? null : firstPossibleGun;
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			if(!canControlGuns && activeGun != null){
				canControlGuns = true;
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public void remove(){
		super.remove();
		WrapperEntity rider = entityOn.locationRiderMap.get(placementOffset);
		if(rider != null){
			entityOn.removeRider(rider, null);
		}
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		double value = super.getRawVariableValue(variable, partialTicks);
		if(!Double.isNaN(value)){
			return value;
		}
		
		WrapperEntity riderForSeat = entityOn.locationRiderMap.get(placementOffset);
		switch(variable){
			case("seat_occupied"): return riderForSeat != null ? 1 : 0;
			case("seat_occupied_client"): return InterfaceClient.getClientPlayer().equals(riderForSeat) ? 1 : 0;
			case("seat_rider_yaw"): {
				if(riderForSeat != null){
					double riderYaw = riderForSeat.getYaw() - entityOn.angles.y;
					while(riderYaw < -180) riderYaw += 360;
					while(riderYaw > 180) riderYaw -= 360;
					return riderYaw;
				}else{
					return 0;
				}
			}
			case("seat_rider_pitch"): {
				if(riderForSeat != null) {
					double pitch = entityOn.angles.x;
	            	double roll = entityOn.angles.z;
	            	double riderYaw = riderForSeat.getYaw() - entityOn.angles.y;
	            	while(pitch > 180){pitch -= 360;}
	    			while(pitch < -180){pitch += 360;}
	    			while(roll > 180){roll -= 360;}
	    			while(roll < -180){roll += 360;}

	            	double rollRollComponent = -Math.sin(Math.toRadians(riderYaw))*roll;
	            	double pitchRollComponent = Math.cos(Math.toRadians(riderYaw))*pitch;
	            	return riderForSeat.getPitch() - (rollRollComponent + pitchRollComponent);
            	}
				else {
					return 0;
				}
			}
		}
		
		return Double.NaN;
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		if(activeGun != null){
			data.setString("activeGunPackID", activeGun.definition.packID);
			data.setString("activeGunSystemName", activeGun.definition.systemName);
			data.setString("activeGunSubName", activeGun.subName);
		}
		return data;
	}
}
