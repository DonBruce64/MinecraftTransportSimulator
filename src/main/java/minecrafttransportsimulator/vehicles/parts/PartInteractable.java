package minecrafttransportsimulator.vehicles.parts;

import mcinterface.WrapperInventory;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperTileEntity;
import mcinterface.WrapperTileEntity.WrapperEntityBrewingStand;
import mcinterface.WrapperTileEntity.WrapperEntityChest;
import mcinterface.WrapperTileEntity.WrapperEntityFurnace;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public final class PartInteractable extends APart{
	private final WrapperTileEntity interactable;
	public final WrapperInventory inventory;
	public final FluidTank tank;
	
	public PartInteractable(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, WrapperNBT data, APart parentPart){
		super(vehicle, packVehicleDef, definition, data, parentPart);
		switch(definition.interactable.interactionType){
			case("crate"): this.interactable = new WrapperEntityChest(vehicle.world, data, definition.interactable.inventoryUnits); break;
			case("barrel"): this.interactable = null; break;
			case("crafting_table"): this.interactable = null; break;
			case("furnace"): this.interactable = new WrapperEntityFurnace(vehicle.world, data); break;
			case("brewing_stand"): this.interactable = new WrapperEntityBrewingStand(vehicle.world, data); break;
			default: throw new IllegalArgumentException("ERROR: " + definition.interactable.interactionType + " is not a valid type of interactable part.");
		}
		this.inventory = interactable != null ? WrapperInventory.getTileEntityInventory(interactable) : null;
		this.tank = definition.interactable.interactionType.equals("barrel") ? new FluidTank(data, definition.interactable.inventoryUnits*1000, vehicle.world.isClient()) : null;
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		if(!vehicle.locked){
			if(definition.interactable.interactionType.equals("crafting_table")){
				player.openCraftingGUI();
			}else if(interactable != null){
				player.openTileEntityGUI(interactable);
			}else if(tank != null){
				player.getHeldWrapperStack().interactWithTank(tank, player);
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
				if(inventory.getStackInSlot(i) != null){
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
				if(inventory.getStackInSlot(i) != null){
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
