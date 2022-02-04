package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.jsondefs.JSONPart.FurnaceComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.JSONPartInteractable;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketFurnaceFuelAdd;
import minecrafttransportsimulator.packets.instances.PacketFurnaceTimeSet;

/**Basic furnace class.  Class is essentially an inventory that holds state of smelting
 * operations.  Has a method for ticking
 *
 * @author don_bruce
 */
public class EntityFurnace extends EntityInventoryContainer{
	public int ticksAddedOfFuel;
	public int ticksLeftOfFuel;
	public int ticksNeededToSmelt;
	public int ticksLeftToSmelt;
	public double powerToDrawPerTick;
	public final JSONPartInteractable definition;
	
	public static final int SMELTING_ITEM_SLOT = 0;
	public static final int SMELTED_ITEM_SLOT = 1;
	public static final int FUEL_ITEM_SLOT = 2;
	public static final String FURNACE_FUEL_NAME = "furnace";
	
	public EntityFurnace(WrapperWorld world, WrapperNBT data, JSONPartInteractable definition){
		super(world, data, 3);
		this.ticksAddedOfFuel = data.getInteger("ticksAddedOfFuel");
		this.ticksLeftOfFuel = data.getInteger("ticksLeftOfFuel");
		this.ticksNeededToSmelt = data.getInteger("ticksNeededToSmelt");
		this.ticksLeftToSmelt = data.getInteger("ticksLeftToSmelt");
		this.powerToDrawPerTick = data.getDouble("powerToDrawPerTick");
		this.definition = definition;
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			if(ticksLeftToSmelt > 0){
				//If we have no fuel, and are a standard type, get fuel from the stack in us.
				if(!world.isClient() && ticksLeftOfFuel == 0 && definition.furnaceType.equals(FurnaceComponentType.STANDARD)){
					WrapperItemStack fuelStack = getStack(FUEL_ITEM_SLOT);
					if(!fuelStack.isEmpty()){
						ticksAddedOfFuel = fuelStack.getFuelValue();
						ticksLeftOfFuel = ticksAddedOfFuel;
						InterfacePacket.sendToAllClients(new PacketFurnaceFuelAdd(this));
						removeFromSlot(FUEL_ITEM_SLOT, 1);
					}
				}
				
				//Make sure the smelting stack didn't get removed.
				WrapperItemStack smeltingStack = getStack(SMELTING_ITEM_SLOT);
				if(!world.isClient() && smeltingStack.isEmpty()){
					ticksNeededToSmelt = 0;
					ticksLeftToSmelt = ticksNeededToSmelt;
					InterfacePacket.sendToAllClients(new PacketFurnaceTimeSet(this));
				}
				
				//We are smelting and have fuel, continue the process.
				if(ticksLeftOfFuel > 0){
					--ticksLeftOfFuel;
					if(world.isClient()){
						if(ticksLeftToSmelt > 0){
							--ticksLeftToSmelt;
						}
					}else{
						if(--ticksLeftToSmelt == 0){
							//Add to output, and remove from input.
							//Need to set the stack in case the output is empty or we have multiple items.
							WrapperItemStack smeltingResult = smeltingStack.getSmeltedItem();
							WrapperItemStack stackInResult = getStack(SMELTED_ITEM_SLOT);
							
							if(stackInResult.isEmpty()){
								stackInResult = smeltingResult; 
							}else{
								stackInResult.add(smeltingResult.getSize());
							}
							setStack(stackInResult, SMELTED_ITEM_SLOT);
							
							removeFromSlot(SMELTING_ITEM_SLOT, 1);
							ticksNeededToSmelt = 0;
						}
					}
				}else{
					ticksAddedOfFuel = 0;
				}
			}else{
				//Not currently smelting, see if we can smelt anything.
				if(!world.isClient()){
					WrapperItemStack smeltingStack = getStack(SMELTING_ITEM_SLOT);
					if(!smeltingStack.isEmpty()){
						WrapperItemStack smeltingResult = smeltingStack.getSmeltedItem();
						WrapperItemStack stackInResult = getStack(SMELTED_ITEM_SLOT);
						if(stackInResult.isEmpty() || (stackInResult.isCompleteMatch(smeltingResult) && (stackInResult.getMaxSize() - stackInResult.getSize() >= smeltingResult.getSize()))){
							ticksNeededToSmelt = (int) (smeltingStack.getSmeltingTime()*1F/definition.furnaceRate);
							ticksLeftToSmelt = ticksNeededToSmelt;
							InterfacePacket.sendToAllClients(new PacketFurnaceTimeSet(this));	
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
	public boolean isStackValid(WrapperItemStack stackToCheck, int index){
		if(index == SMELTING_ITEM_SLOT){
			return !stackToCheck.getSmeltedItem().isEmpty(); 
		}else if(index == FUEL_ITEM_SLOT){
			return definition.furnaceType.equals(FurnaceComponentType.STANDARD) && stackToCheck.getFuelValue() != 0;
		}else{
			return false;
		}
	}
	
	/**
	 *  Saves tank data to the passed-in NBT.
	 */
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setInteger("ticksAddedOfFuel", ticksAddedOfFuel);
		data.setInteger("ticksLeftOfFuel", ticksLeftOfFuel);
		data.setInteger("ticksNeededToSmelt", ticksNeededToSmelt);
		data.setInteger("ticksLeftToSmelt", ticksLeftToSmelt);
		data.setDouble("powerToDrawPerTick", powerToDrawPerTick);
		return data;
	}
}
