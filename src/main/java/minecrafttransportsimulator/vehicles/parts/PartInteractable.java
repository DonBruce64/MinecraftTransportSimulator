package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperTileEntity;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public final class PartInteractable extends APart{
	private final IWrapperTileEntity interactable;
	public final IWrapperInventory inventory;
	public final FluidTank tank;
	
	public PartInteractable(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, ItemPart item, IWrapperNBT data, APart parentPart){
		super(vehicle, packVehicleDef, item, data, parentPart);
		switch(definition.interactable.interactionType){
			case("crate"): this.interactable = MasterLoader.coreInterface.getFakeTileEntity("chest", vehicle.world, data, definition.interactable.inventoryUnits*9); break;
			case("barrel"): this.interactable = null; break;
			case("crafting_table"): this.interactable = null; break;
			case("furnace"): this.interactable = MasterLoader.coreInterface.getFakeTileEntity("furnace", vehicle.world, data, 0); break;
			case("brewing_stand"): this.interactable = MasterLoader.coreInterface.getFakeTileEntity("brewing_stand", vehicle.world, data, 0); break;
			default: throw new IllegalArgumentException("ERROR: " + definition.interactable.interactionType + " is not a valid type of interactable part.");
		}
		this.inventory = interactable != null ? interactable.getInventory() : null;
		this.tank = definition.interactable.interactionType.equals("barrel") ? new FluidTank(data, definition.interactable.inventoryUnits*10000, vehicle.world.isClient()) : null;
	}
	
	@Override
	public boolean interact(IWrapperPlayer player){
		if(!vehicle.locked){
			if(definition.interactable.interactionType.equals("crafting_table")){
				player.openCraftingGUI();
			}else if(interactable != null){
				player.openTileEntityGUI(interactable);
			}else if(tank != null){
				player.getHeldStack().interactWithTank(tank, player);
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
	public IWrapperNBT getData(){
		IWrapperNBT data = super.getData();
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
