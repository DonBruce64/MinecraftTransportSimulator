package minecraftflightsimulator.entities.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.containers.ContainerVehicle;
import minecraftflightsimulator.containers.GUIParent;
import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntityPlaneChest;
import minecraftflightsimulator.items.ItemEngine;
import minecraftflightsimulator.utilities.MFSVector;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public abstract class EntityVehicle extends EntityParent implements IInventory{
	public boolean brakeOn;
	public boolean parkingBrakeOn;
	public boolean lightsOn;
	public byte textureOptions;
	public byte throttle;
	public int maxFuel;
	public double fuel;
	public double prevFuel;
	public double fuelFlow;
	public double velocity;
	public double airDensity;
	public double trackAngle;
	public String ownerName="MFS";
	public String displayName="";
	
	public MFSVector velocityVec = new MFSVector(0, 0, 0);
	public MFSVector headingVec = new MFSVector(0, 0, 0);
	public MFSVector verticalVec = new MFSVector(0, 0, 0);
	public MFSVector sideVec = new MFSVector(0, 0, 0);
	
	public static final int controllerSeatSlot = 28;
	public static final int passengerSeatSlot = 29;
	public static final int instrumentStartSlot = 30;
	public static final int emptyBucketSlot = 41;
	public static final int fuelBucketSlot = 42;

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

	protected List<float[]> controllerPositions;
	
	protected List<float[]> passengerPositions;
	
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
	}
	
	public EntityVehicle(World world, float posX, float posY, float posZ, float playerRotation){
		super(world, posX, posY, posZ, playerRotation);
		for(int i=0; i<10; ++i){
			instrumentList.add(null);
		}
	}
	
	@Override
	protected void entityInit(){
		partPositions = new HashMap<Integer, float[]>();
		controllerPositions = new ArrayList<float[]>();
		passengerPositions = new ArrayList<float[]>();
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
			openInventory();
			for(int i=1; i<getSizeInventory(); ++i){
				ItemStack item = getStackInSlot(i);
				if(item != null){
					worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, item));
				}
			}
		}
		super.setDead();
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
	
	public static float calculateInventoryWeight(IInventory inventory){
		float weight = 0;
		for(int i=0; i<inventory.getSizeInventory(); ++i){
			ItemStack stack = inventory.getStackInSlot(i);
			if(stack != null){
				weight += 1.2F*stack.stackSize/stack.getMaxStackSize()*(MFS.heavyItems.contains(stack.getItem().getUnlocalizedName().substring(5)) ? 2 : 1);
			}
		}
		return weight;
	}
	
	public int getNumberControllerSeats(){return controllerPositions.size();}
	public int getNumberPassengerSeats(){return passengerPositions.size();}
	//TODO make this work with abstract classes.
	public GUIParent getGUI(EntityPlayer player){return new GUIParent(player, this, new ResourceLocation("mfs", "textures/planes/" + this.getClass().getSimpleName().substring(6).toLowerCase() + "/gui.png"));} 
	
	protected abstract void initChildPositions();
	public abstract void drawHUD(int width, int height);
	public abstract void initVehicleContainerSlots(ContainerVehicle container);
	
	
	//Start of IInventory section
	public void markDirty(){}
	public void clear(){}
	public void setField(int id, int value){}
	public void openInventory(){this.openInventory(null);}
	public void openInventory(EntityPlayer player){loadInventory();}
	public void closeInventory(){this.closeInventory(null);}
    public void closeInventory(EntityPlayer player){saveInventory();}
    
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
	 */
	public void loadInventory(){
		if(!worldObj.isRemote){
			//First, match part positions to slots.
			for(int i=1; i<controllerSeatSlot; ++i){
				EntityChild child = getChildAtLocation(partPositions.get(i));
				if(child != null){
					if(child instanceof EntityEngine){
						setInventorySlotContents(i, ItemEngine.createStack(((EntityEngine) child).type, child.propertyCode, ((EntityEngine) child).hours));
					}else if(MFSRegistry.entityItems.containsKey(child.getClass())){
						setInventorySlotContents(i, new ItemStack(MFSRegistry.entityItems.get(child.getClass()), 1, child.propertyCode));
					}
				}else{
					setInventorySlotContents(i, null);
				}
			}
			
			//Next, get seats inputed
			int numberControllerSeats = 0;
			int controllerPropertyCode = 0;
			for(int i=0; i<controllerPositions.size(); ++i){
				EntityChild child = getChildAtLocation(controllerPositions.get(i));
				if(child != null){
					++numberControllerSeats;
					controllerPropertyCode = child.propertyCode;
				}
			}
			if(numberControllerSeats > 0){
				setInventorySlotContents(controllerSeatSlot, new ItemStack(MFSRegistry.seat, numberControllerSeats, controllerPropertyCode));
			}
			
			int numberPassengerSeats = 0;
			int passengerPropertyCode = 0;
			boolean chests = false;
			for(int i=0; i<passengerPositions.size(); ++i){
				EntityChild child = getChildAtLocation(passengerPositions.get(i));
				if(child != null){
					++numberPassengerSeats;
					passengerPropertyCode = child.propertyCode;
					chests = child instanceof EntityPlaneChest;
				}
			}
			if(numberPassengerSeats > 0){
				if(chests){
					setInventorySlotContents(passengerSeatSlot, new ItemStack(Item.getItemFromBlock(Blocks.chest), numberPassengerSeats));
				}else{
					setInventorySlotContents(passengerSeatSlot, new ItemStack(MFSRegistry.seat, numberPassengerSeats, passengerPropertyCode));
				}
			}
			
			//Finally, do instrument and bucket things.  Also clear changes.
			for(int i=0; i<10; ++i){
				setInventorySlotContents(i+instrumentStartSlot,  instrumentList.get(i));
			}
			itemChanged = new boolean[fuelBucketSlot+1];
		}
	}

	/**
	 * Saves inventory after closing.
	 * New items are found by checking to see if itemChanged is true for that slot.
	 * This must be implemented on subclasses to define component spawning behavior.
	 */
    public abstract void saveInventory();
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.brakeOn=tagCompound.getBoolean("brakeOn");
		this.parkingBrakeOn=tagCompound.getBoolean("parkingBrakeOn");
		this.textureOptions=tagCompound.getByte("textureOptions");
		this.throttle=tagCompound.getByte("throttle");
		this.fuel=tagCompound.getDouble("fuel");
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
		tagCompound.setByte("textureOptions", this.textureOptions);
		tagCompound.setByte("throttle", this.throttle);
		tagCompound.setDouble("fuel", this.fuel);
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
