package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperTileEntity;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public final class PartInteractable extends APart{
	private final WrapperTileEntity interactable;
	public final WrapperInventory inventory;
	public final FluidTank tank;
	public PartInteractable linkedPart;
	public EntityVehicleF_Physics linkedVehicle;
	
	public PartInteractable(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, ItemPart item, WrapperNBT data, APart parentPart){
		super(vehicle, packVehicleDef, item, data, parentPart);
		switch(definition.interactable.interactionType){
			case("crate"): this.interactable = InterfaceCore.getFakeTileEntity("chest", vehicle.world, data, definition.interactable.inventoryUnits*9); break;
			case("barrel"): this.interactable = null; break;
			case("crafting_table"): this.interactable = null; break;
			case("furnace"): this.interactable = InterfaceCore.getFakeTileEntity("furnace", vehicle.world, data, 0); break;
			case("brewing_stand"): this.interactable = InterfaceCore.getFakeTileEntity("brewing_stand", vehicle.world, data, 0); break;
			default: throw new IllegalArgumentException("ERROR: " + definition.interactable.interactionType + " is not a valid type of interactable part.");
		}
		this.inventory = interactable != null ? interactable.getInventory() : null;
		this.tank = definition.interactable.interactionType.equals("barrel") ? new FluidTank(data, definition.interactable.inventoryUnits*10000, vehicle.world.isClient()) : null;
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		if(!vehicle.locked){
			if(definition.interactable.interactionType.equals("crafting_table")){
				player.openCraftingGUI();
			}else if(interactable != null){
				player.openTileEntityGUI(interactable);
			}else if(tank != null){
				tank.interactWith(player);
			}	
		}else{
			player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehiclelocked"));
		}
		return true;
    }
	
	@Override
	public void attack(Damage damage){
		double explosivePower = getExplosiveContribution();
		if(explosivePower > 0){
			vehicle.world.spawnExplosion(vehicle, worldPos, explosivePower, true);
			//We just set ourselves invalid rather than removing as we might be in a part-loop here.
			//Doing a removal would get us a CME.
			isValid = false;
		}
	}
	
	@Override
	public void update(){
		super.update();
		if(interactable != null){
			interactable.update();
		}
		
		//Check to see if we are linked and need to send fluid to the linked tank.
		//Only do checks on the server.  Clients get packets.
		if(!vehicle.world.isClient()){
			FluidTank linkedTank =  null;
			String linkedMessage = null;
			if(linkedVehicle != null){
				if(linkedVehicle.position.distanceTo(worldPos) > 16){
					linkedMessage = "interact.fuelhose.linkdropped";
				}else{
					linkedTank = linkedVehicle.fuelTank;
				}
			}else if(linkedPart != null){
				if(linkedPart.worldPos.distanceTo(worldPos) > 16){
					linkedMessage = "interact.fuelhose.linkdropped";
				}else{
					linkedTank = linkedPart.tank;
				}
			}
			
			//If we have a linked tank to transfer to, do so now.
			if(linkedTank != null){
				String fluidToTransfer = tank.getFluid();
				if(!fluidToTransfer.isEmpty()){
					double amountToTransfer = linkedTank.fill(fluidToTransfer, 10, false);
					if(amountToTransfer > 0){
						amountToTransfer = tank.drain(fluidToTransfer, amountToTransfer, true);
						if(amountToTransfer > 0){
							linkedTank.fill(fluidToTransfer, amountToTransfer, true);
						}else{
							linkedMessage = "interact.fuelhose.tankempty";
						}
					}else{
						linkedMessage = "interact.fuelhose.tankfull";
					}
				}else{
					linkedMessage = "interact.fuelhose.tankempty";
				}
			}
			
			//If we have an error message, display it an null our our linkings.
			if(linkedMessage != null){
				linkedVehicle = null;
				linkedPart = null;
				for(WrapperEntity entity : vehicle.world.getEntitiesWithin(new BoundingBox(worldPos, 16, 16, 16))){
					if(entity instanceof WrapperPlayer){
						((WrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage(linkedMessage));
					}
				}
			}
		}
	}
	
	@Override
	public WrapperNBT getData(){
		WrapperNBT data = super.getData();
		if(interactable != null){
			interactable.save(data);
		}else if(tank != null){
			tank.save(data);
		}
		return data;
	}
	
	@Override
	public float getWidth(){
		return 1.0F;
	}

	@Override
	public float getHeight(){
		return 1.0F;
	}
	
	public int getInventoryCount(){
		int count = 0;
		if(inventory != null){
			for(int i=0; i<inventory.getSize(); ++i){
				if(inventory.getItemInSlot(i) != null){
					++count;
				}
			}
		}
		return count;
	}
	
	public double getInventoryPercent(){
		if(inventory != null){
			int count = 0;
			for(int i=0; i<inventory.getSize(); ++i){
				if(inventory.getItemInSlot(i) != null){
					++count;
				}
			}
			return count/(double)inventory.getSize();
		}else if(tank != null){
			return tank.getFluidLevel()/tank.getMaxLevel();
		}else{
			return 0;
		}
	}
	
	public int getInventoryCapacity(){
		if(inventory != null){
			return inventory.getSize();
		}else if(tank != null){
			return tank.getMaxLevel()/1000;
		}else{
			return 0;
		}
	}
	
	public double getInventoryWeight(){
		if(inventory != null){
			return inventory.getInventoryWeight(ConfigSystem.configObject.general.itemWeights.weights);
		}else if(tank != null){
			return tank.getWeight();
		}else{
			return 0;
		}
	}
	
	public double getExplosiveContribution(){
		if(inventory != null){
			return inventory.getExplosiveness();
		}else if(tank != null){
			return tank.getExplosiveness();
		}else{
			return 0;
		}
	}
}
