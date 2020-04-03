package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.wrappers.WrapperNBT;

/**Base Tile Entity class with the ability to store fluids.
 *
 * @author don_bruce
 */
public abstract class ATileEntityFluidTank extends ATileEntityTickable{
	private String currentFluid = "";
	private int currentFluidLevel = 0;
	
	/**
	 *  Gets the current fluid level.
	 */
	public int getFluidLevel(){
		return currentFluidLevel;
	}
	
	/**
	 *  Gets the max fluid level.
	 */
	public abstract int getMaxLevel();
	
	/**
	 *  Gets the name of the fluid in the tank.
	 *  If no fluid is in the tank, an empty string should be returned.
	 */
	public String getFluid(){
		return currentFluid;
	}
	
	/**
	 *  Sets the fluid in this tank.
	 */
	public void getFluid(String fluidName){
		this.currentFluid = fluidName;
	}
	
	/**
	 *  Tries to fill fluid in the tank, returning the amount
	 *  filled, up to the passed-in maxAmount.  If doFill is false, 
	 *  only the possible amount filled should be returned, and the 
	 *  internal state should be left as-is.  Return value is the
	 *  amount filled.
	 */
	public int fill(String fluid, int maxAmount, boolean doFill){
		if(currentFluid.isEmpty() || currentFluid.equals(fluid)){
			if(maxAmount >= getMaxLevel() - currentFluidLevel){
				maxAmount = getMaxLevel() - currentFluidLevel;
			}
			if(doFill){
				//FIXME add fluid packet here to clients.
				currentFluidLevel += maxAmount;
				if(currentFluid.isEmpty()){
					currentFluid = fluid;
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
	public int drain(String fluid, int maxAmount, boolean doDrain){
		if(!currentFluid.isEmpty() && currentFluid.equals(fluid)){
			if(maxAmount >= currentFluidLevel){
				maxAmount = currentFluidLevel;
			}
			if(doDrain){
				//FIXME add fluid packet here to clients.
				currentFluidLevel -= maxAmount;
				if(currentFluidLevel == 0){
					currentFluid = "";
				}
			}
			return maxAmount;
		}else{
			return 0;
		}
	}
	
	@Override
	public void load(WrapperNBT data){
		currentFluid = data.getString("fluidName");
		currentFluidLevel = data.getInteger("fluidLevel");
	}
	
	@Override
	public void save(WrapperNBT data){
		data.setString("fluidName", currentFluid);
		data.setInteger("fluidLevel", currentFluidLevel);
	}
}
