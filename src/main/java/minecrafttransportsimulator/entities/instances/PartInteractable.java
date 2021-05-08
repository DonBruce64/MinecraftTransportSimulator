package minecrafttransportsimulator.entities.instances;

import java.util.Map;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.BuilderItem;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperTileEntity;
import minecrafttransportsimulator.packets.instances.PacketPartInteractable;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public final class PartInteractable extends APart{
	private final WrapperTileEntity interactable;
	public final NonNullList<ItemStack> inventory;
	public final EntityFluidTank tank;
	public PartInteractable linkedPart;
	public String jerrycanFluid;
	public EntityVehicleF_Physics linkedVehicle;
	
	public PartInteractable(AEntityE_Multipart<?> entityOn, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn, placementDefinition, data, parentPart);
		switch(definition.interactable.interactionType){
			case CRATE: this.interactable = null;  break;
			case BARREL: this.interactable = null;; break;
			case CRAFTING_TABLE: this.interactable = null; break;
			case FURNACE: this.interactable = InterfaceCore.getFakeTileEntity("furnace", world, data, 0); break;
			case BREWING_STAND: this.interactable = InterfaceCore.getFakeTileEntity("brewing_stand", world, data, 0); break;
			case JERRYCAN: this.interactable = null; break;
			case CRAFTING_BENCH: this.interactable = null; break;
			default: throw new IllegalArgumentException(definition.interactable.interactionType + " is not a valid type of interactable part.");
		}
		this.inventory = NonNullList.<ItemStack>withSize((int) (definition.interactable.inventoryUnits*9F), ItemStack.EMPTY);
		ItemStackHelper.loadAllItems(data.tag, inventory);
		this.tank = definition.interactable.interactionType.equals(InteractableComponentType.BARREL) ? new EntityFluidTank(world, data.getDataOrNew("tank"), (int) definition.interactable.inventoryUnits*10000) : null;
		this.jerrycanFluid = data.getString("jerrycanFluid");
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		if(!entityOn.locked){
			if(definition.interactable.interactionType.equals(InteractableComponentType.CRATE) || definition.interactable.interactionType.equals(InteractableComponentType.CRAFTING_BENCH)){
				player.sendPacket(new PacketPartInteractable(this, player));
			}else if(definition.interactable.interactionType.equals(InteractableComponentType.CRAFTING_TABLE)){
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
			player.sendPacket(new PacketPlayerChatMessage(player, "interact.failure.vehiclelocked"));
		}
		return true;
    }
	
	@Override
	public void attack(Damage damage){
		if(!damage.isWater){
			double explosivePower = getExplosiveContribution();
			if(explosivePower > 0 && isValid){
				//Set invalid so this explosion doesn't let us attack ourselves again.
				isValid = false;
				world.spawnExplosion(position, explosivePower, true);
				if(vehicleOn != null){
					vehicleOn.destroyAt(position);
				}
			}
		}
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			if(interactable != null){
				interactable.update();
			}
			
			//Check to see if we are linked and need to send fluid to the linked tank.
			//Only do checks on the server.  Clients get packets.
			if(!world.isClient()){
				EntityFluidTank linkedTank =  null;
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
							((WrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((WrapperPlayer) entity, linkedMessage));
						}
					}
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("interactable_count"): return getInventoryCount();
			case("interactable_percent"): return getInventoryPercent();
			case("interactable_capacity"): return getInventoryCapacity();
		}
		
		return super.getRawVariableValue(variable, partialTicks);
	}
	
	public int getInventoryCount(){
		int count = 0;
		for(ItemStack stack : inventory){
			if(!stack.isEmpty()){
				++count;
			}
		}
		return count;
	}
	
	public double getInventoryPercent(){
		if(tank != null){
			return tank.getFluidLevel()/tank.getMaxLevel();
		}else{
			return getInventoryCount()/(double)inventory.size();
		}
	}
	
	public int getInventoryCapacity(){
		if(tank != null){
			return tank.getMaxLevel()/1000;
		}else{
			return inventory.size();
		}
	}
	
	public double getInventoryWeight(){
		if(tank != null){
			return tank.getWeight();
		}else{
			return getInventoryWeight(ConfigSystem.configObject.general.itemWeights.weights);
		}
	}
	
	/**
	 *  Tries to add the passed-in stack to this inventory.  Removes as many items from the
	 *  stack as possible, but may or may not remove all of them.  As such, the actual number of items
	 *  removed is returned.
	 */
	public int addStackToInventory(ItemStack stackToAdd){
		int priorCount = stackToAdd.getCount();
		for(int i=0; i<inventory.size() && !stackToAdd.isEmpty(); ++i){
			ItemStack stack = inventory.get(i);
			if(stack.isEmpty()){
				inventory.set(i, stackToAdd.copy());
				stackToAdd.setCount(0);
			}else if(stackToAdd.isItemEqual(stack) && (stackToAdd.hasTagCompound() ? stackToAdd.getTagCompound().equals(stack.getTagCompound()) : !stack.hasTagCompound())){
				int amountToAdd = stack.getMaxStackSize() - stack.getCount();
				stack.grow(amountToAdd);
				stackToAdd.shrink(amountToAdd);
			}
		}
		return priorCount - stackToAdd.getCount();
	}
	
	/**
	 *  Gets the explosive power of this part.  Used when it is blown up or attacked.
	 *  For our calculations, only ammo is checked.  While we could check for fuel, we assume
	 *  that fuel-containing items are stable enough to not blow up when this container is hit.
	 */
	public double getExplosiveContribution(){
		if(tank != null){
			return tank.getExplosiveness();
		}else{
			double explosivePower = 0;
			for(ItemStack stack : inventory){
				Item item = stack.getItem();
				if(item instanceof BuilderItem && ((BuilderItem) item).item instanceof ItemBullet){
					ItemBullet bullet = (ItemBullet) ((BuilderItem) item).item;
					if(bullet.definition.bullet != null){
						double blastSize = bullet.definition.bullet.blastStrength == 0 ? bullet.definition.bullet.diameter/10D : bullet.definition.bullet.blastStrength;
						explosivePower += stack.getCount()*blastSize/10D;
					}
				}
			}
			return explosivePower;
		}
	}
	
	/**
	 * Gets the weight of this inventory.
	 */
	public float getInventoryWeight(Map<String, Double> heavyItems){
		float weight = 0;
		for(ItemStack stack : inventory){
			if(stack != null){
				double weightMultiplier = 1.0;
				for(String heavyItemName : heavyItems.keySet()){
					if(stack.getItem().getRegistryName().toString().contains(heavyItemName)){
						weightMultiplier = heavyItems.get(heavyItemName);
						break;
					}
				}
				weight += 5F*stack.getCount()/stack.getMaxStackSize()*weightMultiplier;
			}
		}
		return weight;
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		ItemStackHelper.saveAllItems(data.tag, inventory);
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
