package minecrafttransportsimulator.baseclasses;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketFluidTankChange;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/**Basic fluid tanks class.  Class contains methods for filling and draining, as well as automatic
 * syncing of fluid levels across clients and servers.  This allows the tank to be put on any object
 * without the need to worry about packets getting out of whack.
 *
 * @author don_bruce
 */
public class FluidTank{
	public static final Map<Integer, FluidTank> createdClientTanks = new HashMap<Integer, FluidTank>();
	public static final Map<Integer, FluidTank> createdServerTanks = new HashMap<Integer, FluidTank>();
	/**Internal counter for tank IDs.  Increments each time an tank is created, and only valid on the server.**/
	private static int idCounter = 1;
	
	public final int tankID;
	private final int maxLevel;
	private final boolean onClient;
	private String currentFluid;
	private double fluidLevel;
	private double fluidDispensed;
	
	public FluidTank(WrapperNBT data, int maxLevel, boolean onClient){
		this.tankID = onClient ? data.getInteger("tankID") : idCounter++;
		this.maxLevel = maxLevel;
		this.onClient = onClient;
		this.currentFluid = data.getString("currentFluid");
		this.fluidLevel = data.getDouble("fluidLevel");
		this.fluidDispensed = data.getDouble("fluidDispensed");
		if(onClient){
			createdClientTanks.put(tankID, this);
		}else{
			createdServerTanks.put(tankID, this);
		}
	}
	
	/**
	 *  Gets the current fluid level.
	 */
	public double getFluidLevel(){
		return fluidLevel;
	}
	
	/**
	 *  Gets the max fluid level.
	 */
	public int getMaxLevel(){
		return maxLevel;
	}
	
	/**
	 *  Gets the amount of fluid dispensed since the last call to {@link #resetAmountDispensed()} 
	 */
	public double getAmountDispensed(){
		return fluidDispensed;
	}
	
	/**
	 *  Resets the total fluid dispensed counter.
	 */
	public void resetAmountDispensed(){
		fluidDispensed = 0;
	}
	
	/**
	 *  Gets the name of the fluid in the tank.
	 *  If no fluid is in the tank, an empty string should be returned.
	 */
	public String getFluid(){
		return currentFluid;
	}
	
	/**
	 *  Manually sets the fluid and level of this tank.  Used for initial filling of the tank when
	 *  you don't want to sent packets or perform any validity checks.  Do NOT use for normal operations!
	 */
	public void manuallySet(String fluidName, double setLevel){
		this.currentFluid = fluidName;
		this.fluidLevel = setLevel;
	}
	
	/**
	 *  Tries to fill fluid in the tank, returning the amount
	 *  filled, up to the passed-in maxAmount.  If doFill is false, 
	 *  only the possible amount filled should be returned, and the 
	 *  internal state should be left as-is.  Return value is the
	 *  amount filled.
	 */
	public double fill(String fluid, double maxAmount, boolean doFill){
		if(currentFluid.isEmpty() || currentFluid.equals(fluid)){
			if(maxAmount >= getMaxLevel() - fluidLevel){
				maxAmount = getMaxLevel() - fluidLevel;
			}
			if(doFill){
				fluidLevel += maxAmount;
				if(currentFluid.isEmpty()){
					currentFluid = fluid;
				}
				//Send off packet now that we know what fluid we will have on this tank.
				if(!onClient){
					InterfacePacket.sendToAllClients(new PacketFluidTankChange(this, maxAmount));
				}
			}
			return maxAmount;
		}else{
			return 0;
		}
	}
	
	/**
	 *  Tries to drain the fluid from the tank, returning the amount
	 *  drained, up to the passed-in maxAmount.  If doDrain is false, 
	 *  only the possible amount drained should be returned, and the 
	 *  internal state should be left as-is.  Return value is the
	 *  amount drained.
	 */
	public double drain(String fluid, double maxAmount, boolean doDrain){
		if(!currentFluid.isEmpty() && (currentFluid.equals(fluid) || fluid.isEmpty())){
			if(maxAmount >= fluidLevel){
				maxAmount = fluidLevel;
			}
			if(doDrain){
				//Need to send off packet before we remove fluid due to empty tank.
				if(!onClient){
					InterfacePacket.sendToAllClients(new PacketFluidTankChange(this, -maxAmount));
				}
				fluidLevel -= maxAmount;
				fluidDispensed += maxAmount;
				if(fluidLevel == 0){
					currentFluid = "";
				}
			}
			return maxAmount;
		}else{
			return 0;
		}
	}
	
	/**
	 *  Attempts to fill the passed-in tank with the contents of the item held by the player,
	 *  or drain the tank into that stack if the player is sneaking.
	 *  Returns the amount filled or drained if successful.
	 */
	public double interactWith(WrapperPlayer player){
		ItemStack stack = player.getHeldStack();
		if(stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)){
			//If we are sneaking, drain this tank.  If we are not, fill it.
			if(!player.isSneaking()){
				//Item can provide fluid.  Check if the tank can accept it.
				IFluidHandlerItem handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
				FluidStack drainedStack = handler.drain(Integer.MAX_VALUE, false);
				if(drainedStack != null){
					//Able to take fluid from item, attempt to do so.
					int amountToDrain = (int) fill(drainedStack.getFluid().getName(), drainedStack.amount, false);
					drainedStack = handler.drain(amountToDrain, !player.isCreative());
					if(drainedStack != null){
						//Was able to provide liquid from item.  Fill the tank.
						double amountFilled = fill(drainedStack.getFluid().getName(), drainedStack.amount, true);
						player.player.setHeldItem(EnumHand.MAIN_HAND, handler.getContainer());
						return amountFilled;
					}
				}
			}else{
				//Item can hold fluid.  Check if we can fill it.
				IFluidHandlerItem handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
				FluidStack containedStack = FluidRegistry.getFluidStack(getFluid(), (int) getFluidLevel());
				int amountFilled = handler.fill(containedStack, !player.isCreative());
				if(amountFilled > 0){
					//Were able to fill the item.  Apply state change to tank and item.
					double amountDrained = drain(getFluid(), amountFilled, true);
					player.player.setHeldItem(EnumHand.MAIN_HAND, handler.getContainer());
					return amountDrained;
				}
			}
		}
		return 0;
	}
	
	/**
	 *  Gets the explosive power of this fluid.  Used when this tank is blown up.
	 *  In general, 10000 units is one level of explosion.  Explosion is multiplied
	 *  by the fuel potency, so water won't blow up, but high-octane avgas will do nicely.
	 */
	public double getExplosiveness(){
		for(Map<String, Double> fuelEntry : ConfigSystem.configObject.fuel.fuels.values()){
			if(fuelEntry.containsKey(currentFluid)){
				return fluidLevel*fuelEntry.get(currentFluid)/10000D;
			}
		}
		return 0;
	}
	
	/**
	 *  Gets the weight of the fluid in this tank.
	 */
	public double getWeight(){
		return fluidLevel/50D;
	}
	
	/**
	 *  Saves tank data to the passed-in NBT.
	 */
	public void save(WrapperNBT data){
		data.setInteger("tankID", tankID);
		data.setString("currentFluid", currentFluid);
		data.setDouble("fluidLevel", fluidLevel);
		data.setDouble("fluidDispensed", fluidDispensed);
	}
}
