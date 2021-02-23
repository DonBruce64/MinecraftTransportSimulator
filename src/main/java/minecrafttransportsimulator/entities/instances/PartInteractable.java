package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperTileEntity;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;

public final class PartInteractable extends APart{
	private final WrapperTileEntity interactable;
	public final WrapperInventory inventory;
	public final FluidTank tank;
	public PartInteractable linkedPart;
	public String jerrycanFluid;
	public EntityVehicleF_Physics linkedVehicle;
	
	public PartInteractable(AEntityE_Multipart<?> entityOn, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn, placementDefinition, data, parentPart);
		switch(definition.interactable.interactionType){
			case CRATE: this.interactable = InterfaceCore.getFakeTileEntity("chest", world, data, definition.interactable.inventoryUnits*9); break;
			case BARREL: this.interactable = null; break;
			case CRAFTING_TABLE: this.interactable = null; break;
			case FURNACE: this.interactable = InterfaceCore.getFakeTileEntity("furnace", world, data, 0); break;
			case BREWING_STAND: this.interactable = InterfaceCore.getFakeTileEntity("brewing_stand", world, data, 0); break;
			case JERRYCAN: this.interactable = null; break;
			default: throw new IllegalArgumentException(definition.interactable.interactionType + " is not a valid type of interactable part.");
		}
		this.inventory = interactable != null ? interactable.getInventory() : null;
		this.tank = definition.interactable.interactionType.equals(InteractableComponentType.BARREL) ? new FluidTank(world, data.getDataOrNew("tank"), definition.interactable.inventoryUnits*10000) : null;
		this.jerrycanFluid = data.getString("jerrycanFluid");
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		if(!entityOn.locked){
			if(definition.interactable.interactionType.equals(InteractableComponentType.CRAFTING_TABLE)){
				player.openCraftingGUI();
			}else if(definition.interactable.interactionType.equals(InteractableComponentType.JERRYCAN)){
				entityOn.removePart(this, null);
				WrapperNBT data = new WrapperNBT();
				save(data);
				world.spawnItem(getItem(), data, position);
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
			world.spawnExplosion(position, explosivePower, true);
			if(vehicleOn != null){
				vehicleOn.destroyAt(position);
			}
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
		if(!world.isClient()){
			FluidTank linkedTank =  null;
			String linkedMessage = null;
			if(linkedVehicle != null){
				if(linkedVehicle.position.distanceTo(position) > 16){
					linkedMessage = "interact.fuelhose.linkdropped";
				}else{
					linkedTank = linkedVehicle.fuelTank;
				}
			}else if(linkedPart != null){
				if(linkedPart.position.distanceTo(position) > 16){
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
				for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))){
					if(entity instanceof WrapperPlayer){
						((WrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage(linkedMessage));
					}
				}
			}
		}
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
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		if(interactable != null){
			interactable.save(data);
		}else if(tank != null){
			WrapperNBT tankData = new WrapperNBT();
			tank.save(tankData);
			data.setData("tank", tankData);
		}else if(definition.interactable.interactionType.equals(InteractableComponentType.JERRYCAN)){
			data.setString("jerrycanFluid", jerrycanFluid);
		}
	}
}
