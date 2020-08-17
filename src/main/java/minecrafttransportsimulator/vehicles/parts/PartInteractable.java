package minecrafttransportsimulator.vehicles.parts;

import mcinterface.WrapperInventory;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperTileEntity;
import mcinterface.WrapperTileEntity.WrapperEntityBrewingStand;
import mcinterface.WrapperTileEntity.WrapperEntityChest;
import mcinterface.WrapperTileEntity.WrapperEntityFurnace;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public final class PartInteractable extends APart{
	private final WrapperTileEntity interactable;
	public final WrapperInventory inventory;
	public final FluidTank tank;
	
	public PartInteractable(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, WrapperNBT data){
		super(vehicle, packVehicleDef, definition, data);
		switch(definition.interactable.type){
			case("crate"): this.interactable = new WrapperEntityChest(vehicle.world, data, definition.interactable.inventoryUnits*9); break;
			case("barrel"): this.interactable = null; break;
			case("crafting_table"): this.interactable = null; break;
			case("furnace"): this.interactable = new WrapperEntityFurnace(vehicle.world, data); break;
			case("brewing_stand"): this.interactable = new WrapperEntityBrewingStand(vehicle.world, data); break;
			default: throw new IllegalArgumentException("ERROR: " + definition.interactable.type + " is not a valid type of interactable part.");
		}
		this.inventory = WrapperInventory.getTileEntityInventory(interactable);
		this.tank = definition.interactable.type.equals("barrel") ? new FluidTank(data, definition.interactable.inventoryUnits*1000, vehicle.world.isClient()) : null;
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		if(!vehicle.locked){
			if(definition.interactable.type.equals("crafting_table")){
				player.openCraftingGUI();
			}else if(interactable != null){
				player.openTileEntityGUI(interactable);
			}
		}else{
			player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehiclelocked"));
		}
		return true;
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
		WrapperNBT data = new WrapperNBT();
		if(interactable != null){
			interactable.save(data);
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
}
