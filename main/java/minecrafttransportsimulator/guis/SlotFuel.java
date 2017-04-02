package minecrafttransportsimulator.guis;


	/*
	private EntityVehicle vehicle;
	
	public SlotFuel(EntityVehicle vehicle, int xDisplayPosition, int yDisplayPosition){
		super(vehicle, xDisplayPosition, yDisplayPosition, vehicle.fuelBucketSlot);
		this.vehicle = vehicle;
	}
	
	//DEL194START
    public boolean isItemValid(ItemStack stack){
    	FluidStack fluidStack;
    	if(stack.getItem() instanceof IFluidContainerItem){
    		fluidStack = ((IFluidContainerItem) stack.getItem()).getFluid(stack);
    	}else{
    		fluidStack = FluidContainerRegistry.getFluidForFilledItem(stack);
    	}
    	if(fluidStack != null){
    		return ConfigSystem.getFuelValue(fluidStack.getFluid().getName()) > 0;
		}
    	return false;
    }
    
    @Override
    public void putStack(ItemStack stack){
    	super.putStack(stack);
    	if(stack != null){
    		if(stack.getItem() instanceof IFluidContainerItem){
    			if(vehicle.fuel < vehicle.maxFuel && vehicle.getStackInSlot(vehicle.emptyBucketSlot) == null){
	    			FluidStack fluidStack = ((IFluidContainerItem) stack.getItem()).getFluid(stack);
	    			if(fluidStack != null){
	    				if(fluidStack.getFluid() != null){
	    					if(ConfigSystem.getFuelValue(fluidStack.getFluid().getName()) > 0){
	    						double fuelValue = ConfigSystem.getFuelValue(fluidStack.getFluid().getName());
	    						FluidStack drainedFluid = ((IFluidContainerItem) stack.getItem()).drain(stack, (int) ((vehicle.maxFuel - vehicle.fuel)*fuelValue), true);
	    						vehicle.fuel += drainedFluid.amount;
	    						vehicle.setInventorySlotContents(vehicle.emptyBucketSlot, stack);
	    						this.putStack(null);
    						}
    					}
    				}
    			}
    		}else if(FluidContainerRegistry.isFilledContainer(stack)){
    			FluidStack fluidStack = FluidContainerRegistry.getFluidForFilledItem(stack);
    			if(vehicle.getStackInSlot(vehicle.emptyBucketSlot) != null){
    				ItemStack emptyBucketStack = vehicle.getStackInSlot(vehicle.emptyBucketSlot);
    				ItemStack emptyContainerStack = FluidContainerRegistry.drainFluidContainer(stack.copy());
    				if(!emptyBucketStack.getItem().equals(emptyContainerStack.getItem()) || emptyBucketStack.stackSize == emptyBucketStack.getMaxStackSize()){
    					return;
    				}
    			}
    			if(ConfigSystem.getFuelValue(fluidStack.getFluid().getName()) > 0){
    				double fuelValue = ConfigSystem.getFuelValue(fluidStack.getFluid().getName());  
    				if((vehicle.fuel + fluidStack.amount*fuelValue) - 100 < vehicle.maxFuel){
    					vehicle.fuel = Math.min(vehicle.fuel + fluidStack.amount*fuelValue, vehicle.maxFuel);
    					if(vehicle.getStackInSlot(vehicle.emptyBucketSlot) != null){
    						++vehicle.getStackInSlot(vehicle.emptyBucketSlot).stackSize; 
    					}else{
    						vehicle.setInventorySlotContents(vehicle.emptyBucketSlot, FluidContainerRegistry.drainFluidContainer(stack));
    					}
    					this.putStack(null);
    				}
    			}
    		}
    	}
    }*/
    //DEL194END
    /*INS194
    public boolean isItemValid(ItemStack stack){
    	ICapabilityProvider capabilities = stack.getItem().initCapabilities(stack, stack.getTagCompound());
    	if(capabilities instanceof FluidBucketWrapper){
    		FluidBucketWrapper bucket = (FluidBucketWrapper) capabilities;
    		return bucket.getFluid() != null ? ConfigSystem.getFuelValue(FluidRegistry.getFluidName(bucket.getFluid())) > 0 : false;
    	}else if(stack.getItem() instanceof ItemFluidContainer){
    		FluidHandlerItemStack handlerStack = (FluidHandlerItemStack) capabilities;
    		return ConfigSystem.getFuelValue(FluidRegistry.getFluidName(handlerStack.getFluid())) > 0;
    	}
    	return false;
    }
    
    @Override
    public void putStack(ItemStack stack){
    	super.putStack(stack);
    	if(stack != null){
    		ICapabilityProvider capabilities = stack.getItem().initCapabilities(stack, stack.getTagCompound());
        	if(capabilities instanceof FluidBucketWrapper){
        		if(vehicle.getStackInSlot(vehicle.emptyBucketSlot) != null){
        			if(vehicle.getStackInSlot(vehicle.emptyBucketSlot).stackSize == 16){
        				return;
        			}
        		}
        		FluidBucketWrapper bucket = ((FluidBucketWrapper) capabilities);
        		FluidStack bucketFluid = bucket.getFluid();
        		if(bucketFluid != null){
        			double fuelFactor = ConfigSystem.getFuelValue(FluidRegistry.getFluidName(bucketFluid.getFluid()));
        			if(bucketFluid.amount*fuelFactor <= (vehicle.maxFuel - vehicle.fuel)){
        				FluidStack drainedFluid = bucket.drain((int) ((vehicle.maxFuel - vehicle.fuel)*fuelFactor), true);
        				vehicle.fuel += drainedFluid.amount;
        				if(vehicle.getStackInSlot(vehicle.emptyBucketSlot) != null){
        					++vehicle.getStackInSlot(vehicle.emptyBucketSlot).stackSize;
        				}else{
        					vehicle.setInventorySlotContents(vehicle.emptyBucketSlot, stack);
        				}
						this.putStack(null);
            		}
        		}
        	}else if(stack.getItem() instanceof ItemFluidContainer){
        		if(vehicle.getStackInSlot(vehicle.emptyBucketSlot) == null){
        			FluidHandlerItemStack handlerStack = (FluidHandlerItemStack) capabilities;
        			if(handlerStack.getFluid() != null){
        				double fuelFactor = ConfigSystem.getFuelValue(FluidRegistry.getFluidName(handlerStack.getFluid()));
            			FluidStack drainedFluid = handlerStack.drain((int) ((vehicle.maxFuel - vehicle.fuel)*fuelFactor), true);            			
            			vehicle.fuel += drainedFluid.amount;
    					vehicle.setInventorySlotContents(vehicle.emptyBucketSlot, stack);
    					this.putStack(null);
        			}
        		}
        	}
    	}
    }
    INS194*/
	
