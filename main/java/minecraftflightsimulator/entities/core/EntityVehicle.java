package minecraftflightsimulator.entities.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.containers.ContainerVehicle;
import minecraftflightsimulator.containers.GUIParent;
import minecraftflightsimulator.entities.parts.EntityChest;
import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.items.IItemNBT;
import minecraftflightsimulator.items.ItemEngine;
import minecraftflightsimulator.utilities.ConfigSystem;
import minecraftflightsimulator.utilities.MFSVector;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public abstract class EntityVehicle extends EntityParent implements IInventory{
	public boolean brakeOn;
	public boolean parkingBrakeOn;
	public boolean lightsOn;
	public boolean auxLightsOn;
	public byte numberLights;
	public byte textureOptions;
	public byte throttle;
	public int maxFuel;
	public double fuel;
	public double prevFuel;
	public double fuelFlow;
	public double electricPower;
	public double electricUsage;
	public double electricFlow;
	public double velocity;
	public double airDensity;
	public double trackAngle;
	public String playerInInv = "";
	public String ownerName="MFS";
	public String displayName="";
	
	public MFSVector velocityVec = new MFSVector(0, 0, 0);
	public MFSVector headingVec = new MFSVector(0, 0, 0);
	public MFSVector verticalVec = new MFSVector(0, 0, 0);
	public MFSVector sideVec = new MFSVector(0, 0, 0);
	
	public static final int controllerSlot = 28;
	public static final int passengerSlot = 29;
	public static final int cargoSlot = 30;
	public static final int forwardMixedSlot = 31;
	public static final int aftMixedSlot = 32;
	public static final int instrumentStartSlot = 40;
	public static final int emptyBucketSlot = 50;
	public static final int fuelBucketSlot = 52;
	
	private List<AxisAlignedBB> collidingBoxes = new ArrayList<AxisAlignedBB>();

	/**
	 * ItemStack used to store all vehicle items.    Slot rules are as follows:
	 * Slots 1-27 can be used for any part. 
	 * Slot 28 is for pilot seats.
	 * Slot 29 is for passenger seats.
	 * 
	 * 
	 * Slots 30 through 40 are used for instruments.
	 * Slot 41 is for empty buckets.
	 * Slot 42 is for fuel buckets, which drop out of the inventory if left in slot during closing.
	 */
	private ItemStack[] compenentItems = new ItemStack[fuelBucketSlot+1];
	protected boolean[] itemChanged = new boolean[fuelBucketSlot+1];
	
	/**
	 * Array containing locations of all child positions with respect to slots.
	 * All positions should be initialized in entity's {@link initChildPositions} method.
	 * Note that core entities should NOT be put here, as they're
	 * directly linked to the parent and can't be added in the GUI.
	 * Also note that seats don't use this mapping as their slots are able to hold multiple seats.
	 */
	protected Map<Integer, float[]> partPositions;

	private List<float[]> controllerPositions;
	private List<float[]> passengerPositions;
	private List<float[]> cargoPositions;
	private List<float[]> mixedPositions;
	
	/**
	 * Map that contains child mappings.  Keyed by child's UUID.
	 * Note that this is for moving and linking children, and will be empty until
	 * children get linked.
	 */
	private Map<String, EntityChild> children = new HashMap<String, EntityChild>();
	
	/**
	 * List containing data about what instruments are equipped.
	 */
	public List<ItemStack> instrumentList = new ArrayList<ItemStack>();
	
	public EntityVehicle(World world){
		super(world);
		for(int i=0; i<10; ++i){
			instrumentList.add(null);
		}
		electricPower = 12;
	}
	
	public EntityVehicle(World world, float posX, float posY, float posZ, float playerRotation){
		super(world, posX, posY, posZ, playerRotation);
		for(int i=0; i<10; ++i){
			instrumentList.add(null);
		}
		electricPower = 12;
	}
	
	@Override
	protected void entityInit(){
		partPositions = new HashMap<Integer, float[]>();
		controllerPositions = new ArrayList<float[]>();
		passengerPositions = new ArrayList<float[]>();
		cargoPositions = new ArrayList<float[]>();
		mixedPositions = new ArrayList<float[]>();
		this.initChildPositions();
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(!linked){return;}
		airDensity = 1.225*Math.pow(2, -posY/500);
		if(fuel < 0){fuel = 0;}
		fuelFlow = prevFuel - fuel;
		prevFuel = fuel;
		if(electricPower > 2){
			if(lightsOn){
				electricUsage += 0.001;
				if(auxLightsOn){
					electricUsage += 0.001*(numberLights - 0);
				}
			}
		}
		electricPower = Math.max(0, Math.min(13, electricPower -= electricUsage));
		electricFlow = electricUsage;
		electricUsage = 0;
	}
	
	//Start of custom methods
	@Override
	public boolean performRightClickAction(EntityPlayer player){
		if(!worldObj.isRemote){
			if(player.inventory.getCurrentItem() != null){
				if(player.inventory.getCurrentItem().getItem().equals(Items.name_tag)){
					this.displayName = player.inventory.getCurrentItem().getDisplayName().length() > 12 ? player.inventory.getCurrentItem().getDisplayName().substring(0, 11) : player.inventory.getCurrentItem().getDisplayName();
					this.sendDataToClient();
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean performAttackAction(DamageSource source, float damage){
		if(!worldObj.isRemote){
			if(source.getEntity() instanceof EntityPlayer){
				EntityPlayer attackingPlayer = (EntityPlayer) source.getEntity();
				if(attackingPlayer.isSneaking()){
					if(attackingPlayer.capabilities.isCreativeMode || attackingPlayer.getDisplayName().endsWith(this.ownerName)){
						this.setDead();
					}
				}
			}
		}
		return true;
	}

	@Override
	public void setDead(){
		if(!worldObj.isRemote){
			loadInventory("");
			for(int i=1; i<getSizeInventory(); ++i){
				ItemStack item = getStackInSlot(i);
				if(item != null){
					worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, item));
				}
			}
		}
		super.setDead();
	}
	
	protected List<AxisAlignedBB> getChildCollisions(EntityChild child, AxisAlignedBB box){
		this.collidingBoxes.clear();
		int minX = MathHelper.floor_double(box.minX);
		int maxX = MathHelper.floor_double(box.maxX + 1.0D);
		int minY = MathHelper.floor_double(box.minY);
		int maxY = MathHelper.floor_double(box.maxY + 1.0D);
		int minZ = MathHelper.floor_double(box.minZ);
		int maxZ = MathHelper.floor_double(box.maxZ + 1.0D);

        for(int i = minX; i < maxX; ++i){
        	for(int j = minY; j < maxY; ++j){
        		for(int k = minZ; k < maxZ; ++k){
        			//DEL180START
                    Block block = worldObj.getBlock(i, j, k);
                    AxisAlignedBB blockBox = block.getCollisionBoundingBoxFromPool(worldObj, i, j, k);
                    if(blockBox != null && box.intersectsWith(blockBox)){
                    	if(block.getBlockHardness(worldObj, i, j, k) <= 0.2F && velocity > 0.5){
                    		worldObj.setBlockToAir(i, j, k);
                    		motionX *= 0.95;
                    		motionY *= 0.95;
                    		motionZ *= 0.95;
                    	}else{
                    		collidingBoxes.add(blockBox);
                    	}
                    }else if(child.collidesWithLiquids()){
                    	if(child.isLiquidAt(i, j, k)){
                    		blockBox = AxisAlignedBB.getBoundingBox(i, j, k, i+1, j+1, k+1);
                    		collidingBoxes.add(blockBox);
                    	}
                    }
                  //DEL180END
                  /*INS180
					BlockPos pos = new BlockPos(i, j, k);
        			IBlockState state = worldObj.getBlockState(pos);
                    AxisAlignedBB blockBox = state.getBlock().getCollisionBoundingBox(worldObj, pos, state);
                    if(blockBox != null && box.intersectsWith(blockBox)){
                    	if(state.getBlock().getBlockHardness(worldObj, pos) <= 0.2F && velocity > 0.5){
                    		worldObj.setBlockToAir(pos);
                    		motionX *= 0.95;
                    		motionY *= 0.95;
                    		motionZ *= 0.95;
                    	}else{
                    		collidingBoxes.add(blockBox);
                    	}
                    }else if(child.collidesWithLiquids()){
                    	if(child.isLiquidAt(i, j, k)){
                    		collidingBoxes.add(box);
                    	}
                    }
                  INS180*/
                }
            }
        }
		return collidingBoxes;
	}
	
	public void explodeAtPosition(double x, double y, double z){
		this.setDead();
		worldObj.newExplosion(this, x, y, z, (float) (fuel/1000 + 1F), true, true);
	}
	
	protected void addPartPosition(float[] coords, int slot){
		if(!partPositions.containsKey(slot)){
			partPositions.put(slot, coords);
			return;
		}else{
			System.err.println("AN ENTITY HAS REGISTERED TWO PARTS FOR SLOT " + slot + "!  THINGS MAY GO BADLY!");
		}
	}
	
	protected void addControllerPosition(float[] coords){
		controllerPositions.add(coords);
	}
	
	protected void addPassengerPosition(float[] coords){
		passengerPositions.add(coords);
	}

	protected void addCargorPosition(float[] coords){
		cargoPositions.add(coords);
	}
	
	protected void addMixedPosition(float[] coords){
		mixedPositions.add(coords);
	}
	

	
	public static float calculateInventoryWeight(IInventory inventory){
		float weight = 0;
		for(int i=0; i<inventory.getSizeInventory(); ++i){
			ItemStack stack = inventory.getStackInSlot(i);
			if(stack != null){
				weight += 1.2F*stack.stackSize/stack.getMaxStackSize()*(ConfigSystem.getStringConfig("HeavyItems").contains(stack.getItem().getUnlocalizedName().substring(5)) ? 2 : 1);
			}
		}
		return weight;
	}
	
	/**
	 * Starts and sets engine states.  Action performed depends on the
	 * data of the byte.  0 is a hit-start, 1 is a starter engaged, 2 is a
	 * starter disengaged, a 3 is an engine cutoff, and a 4 is an engineOn
	 * check signal.  EngineID is only used in a hand-start or engineOn check.
	 * @return Whether or not the state set was successful.
	 */
	public boolean setEngineState(byte engineCode, int engineID){
		if(engineCode == 0){
			if(throttle < 15){throttle = 15;}
			for(EntityChild child : getChildren()){
				if(child.getEntityId() == engineID){
					return ((EntityEngine) child).handStartEngine();
				}
			}
			return false;
		}else if(engineCode == 1){
			if(throttle < 15){throttle = 15;}
			for(EntityChild child : getChildren()){
				if(child instanceof EntityEngine){
					((EntityEngine) child).setElectricStarterState(true);
				}
			}
			return true;
		}else if(engineCode == 2){
			for(EntityChild child : getChildren()){
				if(child instanceof EntityEngine){
					((EntityEngine) child).setElectricStarterState(false);
				}
			}
			return true;
		}else if(engineCode == 3){
			throttle = 0;
			for(EntityChild child : getChildren()){
				if(child instanceof EntityEngine){
					((EntityEngine) child).stopEngine(true);
				}
			}
			return true;
		}else if(engineCode == 4){
			for(EntityChild child : getChildren()){
				if(child.getEntityId() == engineID){
					((EntityEngine) child).engineOn = true;
					return true;
				}
			}
			return false;
		}else{
			return false;
		}
	}
	
	public int getControllerCapacity(){return controllerPositions.size();}
	public int getPassengerCapacity(){return passengerPositions.size();}
	public int getCargoCapacity(){return cargoPositions.size();}
	public int getForwardMixedCapacity(){return getStackInSlot(aftMixedSlot) != null ? mixedPositions.size() - getStackInSlot(aftMixedSlot).stackSize : mixedPositions.size();}
	public int getAftMixedCapacity(){return getStackInSlot(forwardMixedSlot) != null ? mixedPositions.size() - getStackInSlot(forwardMixedSlot).stackSize : mixedPositions.size();}
	
	/**
	 * Gets the GUI background for the specific vehicle.
	 * GUI texture location is based on the class name and the superclass name.
	 */
	public GUIParent getGUI(EntityPlayer player){return new GUIParent(player, this, new ResourceLocation("mfs", "textures/" + this.getClass().getSuperclass().getSimpleName().substring(6).toLowerCase() + "s/" + this.getClass().getSimpleName().substring(6).toLowerCase() + "/gui.png"));} 
	
	protected abstract void initChildPositions();
	public abstract void drawHUD(int width, int height);
	public abstract void initVehicleContainerSlots(ContainerVehicle container);
	
	
	//Start of IInventory section
	public void clear(){}
	public void markDirty(){}
	public void openInventory(){}
	public void closeInventory(){}
	public void setField(int id, int value){}
	public boolean hasCustomInventoryName(){return false;}
	public boolean isUseableByPlayer(EntityPlayer player){return player.getDistanceToEntity(this) < 5;}
	public boolean isItemValidForSlot(int slot, ItemStack stack){return false;}
	public int getField(int id){return 0;}
	public int getFieldCount(){return 0;}
	public int getSizeInventory(){return compenentItems.length;}
	public int getInventoryStackLimit(){return 1;}
	public String getInventoryName(){return "Vehicle Inventory";}
	public ItemStack getStackInSlot(int slot){return compenentItems[slot];}
	public ItemStack getStackInSlotOnClosing(int slot){return null;}
	
	public void setInventorySlotContents(int slot, ItemStack stack){
		if(!worldObj.isRemote){
			itemChanged[slot] = true;
		}
		compenentItems[slot] = stack;
	}
	
    public ItemStack decrStackSize(int slot, int number){
    	if(!worldObj.isRemote){
			itemChanged[slot] = true;
		}
        if(compenentItems[slot] != null){
            ItemStack itemstack;
            if(compenentItems[slot].stackSize <= number){
                itemstack = compenentItems[slot];
                compenentItems[slot] = null;
                return itemstack;
            }else{
                itemstack = compenentItems[slot].splitStack(number);
                if(compenentItems[slot].stackSize == 0){
                	compenentItems[slot] = null;
                }
                return itemstack;
            }
        }else{
            return null;
        }
    }
	
    public ItemStack removeStackFromSlot(int index){
		ItemStack removedStack = getStackInSlot(index);
		setInventorySlotContents(index, null);
		return removedStack;
	}
    
	
	/**
	 * Loads inventory upon opening.  Needs to run each opening to account for broken parts.
	 * Requires a player name to hold until inventory is closed.  This is to prevent multiple
	 * players from accessing it and causing de-syncs.  A hack, I know, but I don't
	 * know how to fix it.
	 */
	public void loadInventory(String playerName){
		if(!worldObj.isRemote){
			//First, match part positions to slots.
			for(int i=1; i<controllerSlot; ++i){
				EntityChild child = getChildAtLocation(partPositions.get(i));
				if(child != null && !child.isDead){
					Item childItem = MFSRegistry.entityItems.get(child.getClass());
					if(childItem instanceof IItemNBT){
						setInventorySlotContents(i, ((IItemNBT) childItem).createStackFromEntity(child));
					}else if(childItem != null){
						setInventorySlotContents(i, new ItemStack(childItem, 1, child.propertyCode));
					}else if(child instanceof EntityEngine){
						setInventorySlotContents(i, ((ItemEngine) MFSRegistry.engineSmall).createStackFromEntity(child));
					}
				}else{
					setInventorySlotContents(i, null);
				}
			}
			
			//Next, get seats and cargo positions.
			setLoadableSlot(controllerPositions, controllerSlot);
			setLoadableSlot(passengerPositions, passengerSlot);
			setLoadableSlot(cargoPositions, cargoSlot);
			setMixedSlots();
			
			//Finally, do instrument and bucket things.  Also clear changes.
			for(int i=0; i<10; ++i){
				setInventorySlotContents(i+instrumentStartSlot,  instrumentList.get(i));
			}
			itemChanged = new boolean[fuelBucketSlot+1];
		}
	}
	
	/**
	 * Sets the contents of the slot to the children located at the points in
	 * positionList.  Used to set seats and chests in the inventory given
	 * a list of places they could be.  Only used for regular slots, not
	 * the mixed slots.
	 */
	private void setLoadableSlot(List<float[]> positionList, int itemSlot){
		ItemStack stack = null;
		for(float[] coords : positionList){
			EntityChild child = getChildAtLocation(coords);
			if(child != null && !child.isDead){
				if(stack == null){
					stack = new ItemStack(MFSRegistry.entityItems.get(child.getClass()), 1, child.propertyCode);
				}else{
					++stack.stackSize;
				}
			}
		}
		setInventorySlotContents(itemSlot, stack);
	}
	
	/**
	 * Sets the two mixed slots that can be either chests or seats.
	 */
	private void setMixedSlots(){
		EntityChild forwardChild = null;
		EntityChild aftChild = null;
		int forwardSpots = 0;
		int aftSpots = 0;
		boolean firstSlot = true;
		
		for(float[] coords : mixedPositions){
			EntityChild currentChild = getChildAtLocation(coords);
			if(currentChild != null && !currentChild.isDead){
				if(forwardChild == null){
					if(firstSlot){
						forwardChild = currentChild;
					}else if(aftChild == null){
						aftChild = currentChild;
					}
				}else{
					if((!currentChild.getClass().equals(forwardChild.getClass()) || currentChild.propertyCode != forwardChild.propertyCode) && aftChild == null){
						aftChild = currentChild;
					}
				}
				if(forwardChild != null && aftChild == null){
					++forwardSpots;
				}else if(aftChild != null){
					++aftSpots;
				}
			}
			firstSlot = false;
		}
		
		if(forwardChild != null){
			setInventorySlotContents(forwardMixedSlot, new ItemStack(MFSRegistry.entityItems.get(forwardChild.getClass()), forwardSpots, forwardChild.propertyCode));
		}else{
			setInventorySlotContents(forwardMixedSlot, null);
		}
		
		if(aftChild != null){
			setInventorySlotContents(aftMixedSlot, new ItemStack(MFSRegistry.entityItems.get(aftChild.getClass()), aftSpots, aftChild.propertyCode));
		}else{
			setInventorySlotContents(aftMixedSlot, null);
		}
	}
	
	/**
	 * Spawns all loadable items and removes extra entities 
	 * if a loadable slot lost items.
	 */
	protected void spawnLoadableContents(){
		spawnLoadablesFromPositions(controllerPositions, controllerSlot);
		spawnLoadablesFromPositions(passengerPositions, passengerSlot);
		spawnLoadablesFromPositions(cargoPositions, cargoSlot);

		ItemStack[] spawnedItemData = new ItemStack[mixedPositions.size()];
		ItemStack[] mixedItemData = new ItemStack[mixedPositions.size()];
		ItemStack forwardStack = getStackInSlot(forwardMixedSlot);
		ItemStack aftStack = getStackInSlot(aftMixedSlot);
		
		for(int i=0; i<mixedPositions.size(); ++i){
			EntityChild child = getChildAtLocation(mixedPositions.get(i));
			spawnedItemData[i] = child != null ? new ItemStack(MFSRegistry.entityItems.get(child.getClass()), 1, child.propertyCode) : null;
		}
		if(forwardStack != null){
			for(int i=0; i<forwardStack.stackSize; ++i){
				mixedItemData[i] = forwardStack;
			}
		}
		if(aftStack != null){
			for(int i=mixedPositions.size() - 1; i>mixedPositions.size() - 1 - aftStack.stackSize; --i){
				mixedItemData[i] = aftStack;
			}
		}
		
		for(int i=0; i<mixedPositions.size(); ++i){
			float[] coords = mixedPositions.get(i);
			if(mixedItemData[i] != null || spawnedItemData[i] != null){
				if(mixedItemData[i] != null){
					if(spawnedItemData[i] != null){
						if(mixedItemData[i].isItemEqual(spawnedItemData[i]) && mixedItemData[i].getItemDamage() == spawnedItemData[i].getItemDamage()){
							continue;
						}else{
							getChildAtLocation(coords).setDead();
						}
					}
					if(mixedItemData[i].getItem().equals(MFSRegistry.seat)){
						EntityChild newChild = new EntitySeat(worldObj, this, this.UUID, coords[0], coords[1], coords[2], mixedItemData[i].getItemDamage(), false);
						addChild(newChild.UUID, newChild, true);
					}else if(mixedItemData[i].getItem().equals(Item.getItemFromBlock(Blocks.chest))){
						EntityChild newChild = new EntityChest(worldObj, this, this.UUID, coords[0], coords[1], coords[2]);
						addChild(newChild.UUID, newChild, true);
					}
				}else{
					getChildAtLocation(mixedPositions.get(i)).setDead();
				}
			}
		}
	}
	
	
	private void spawnLoadablesFromPositions(List<float[]> positionList, int itemSlot){
		ItemStack stack = getStackInSlot(itemSlot);
		for(int i=0; i<positionList.size(); ++i){
			float[] coords = positionList.get(i);
			EntityChild child = getChildAtLocation(coords);
			if(stack != null){
				if(child != null && !child.isDead){
					if(stack.stackSize <= i){
						child.setDead();
					}else{
						if(child.propertyCode != stack.getItemDamage()){
							child.setDead();
							if(stack.getItem().equals(MFSRegistry.seat)){
								EntityChild newChild = new EntitySeat(worldObj, this, this.UUID, coords[0], coords[1], coords[2], getStackInSlot(itemSlot).getItemDamage(), itemSlot == controllerSlot);
								addChild(newChild.UUID, newChild, true);
							}else if(stack.getItem().equals(Item.getItemFromBlock(Blocks.chest))){
								EntityChild newChild = new EntityChest(worldObj, this, this.UUID, coords[0], coords[1], coords[2]);
								addChild(newChild.UUID, newChild, true);
							}
						}
					}	
				}else{
					if(stack.stackSize > i){
						if(stack.getItem().equals(MFSRegistry.seat)){
							EntityChild newChild = new EntitySeat(worldObj, this, this.UUID, coords[0], coords[1], coords[2], getStackInSlot(itemSlot).getItemDamage(), itemSlot == controllerSlot);
							addChild(newChild.UUID, newChild, true);
						}else if(stack.getItem().equals(Item.getItemFromBlock(Blocks.chest))){
							EntityChild newChild = new EntityChest(worldObj, this, this.UUID, coords[0], coords[1], coords[2]);
							addChild(newChild.UUID, newChild, true);
						}
					}
				}
			}else{
				if(child != null){
					child.setDead();
				}				
			}
		}
	}
	
	/**
	 * Called on container close to save inventory.
	 * New items are found by checking to see if itemChanged is true for that slot.
	 * This should be implemented on subclasses to define component spawning behavior.
	 */
    public abstract void saveInventory();
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.brakeOn=tagCompound.getBoolean("brakeOn");
		this.parkingBrakeOn=tagCompound.getBoolean("parkingBrakeOn");
		this.lightsOn=tagCompound.getBoolean("lightsOn");
		this.auxLightsOn=tagCompound.getBoolean("auxLightsOn");
		this.textureOptions=tagCompound.getByte("textureOptions");
		this.throttle=tagCompound.getByte("throttle");
		this.fuel=tagCompound.getDouble("fuel");
		this.electricPower=tagCompound.getDouble("electricPower");
		this.ownerName=tagCompound.getString("ownerName");
		this.displayName=tagCompound.getString("displayName");
		for(int i=0; i<10; ++i){
			if(tagCompound.hasKey("instrument" + i)){
				instrumentList.set(i, new ItemStack(MFSRegistry.flightInstrument, 1, tagCompound.getInteger("instrument" + i)));
			}else{
				instrumentList.set(i, null);
			}
		}
		if(tagCompound.hasKey("bucketId")){
			this.setInventorySlotContents(emptyBucketSlot, new ItemStack(Item.getItemById(tagCompound.getShort("bucketId")), tagCompound.getByte("bucketCount"), tagCompound.getShort("bucketDamage")));
		}
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("brakeOn", this.brakeOn);
		tagCompound.setBoolean("parkingBrakeOn", this.parkingBrakeOn);
		tagCompound.setBoolean("lightsOn", this.lightsOn);
		tagCompound.setBoolean("auxLightsOn", this.auxLightsOn);
		tagCompound.setByte("textureOptions", this.textureOptions);
		tagCompound.setByte("throttle", this.throttle);
		tagCompound.setDouble("fuel", this.fuel);
		tagCompound.setDouble("electricPower", this.electricPower);
		tagCompound.setString("ownerName", this.ownerName);
		tagCompound.setString("displayName", this.displayName);
		for(int i=0; i<instrumentList.size(); ++i){
			if(instrumentList.get(i) != null){
				tagCompound.setInteger("instrument" + i, instrumentList.get(i).getItemDamage());
			}
		}
		if(this.getStackInSlot(emptyBucketSlot) != null){
			ItemStack bucketStack = getStackInSlot(emptyBucketSlot);
			tagCompound.setShort("bucketId", (short) Item.getIdFromItem(bucketStack.getItem()));
			tagCompound.setByte("bucketCount", (byte) bucketStack.stackSize);
			tagCompound.setShort("bucketDamage", (short) bucketStack.getItemDamage());
		}
	}
}
