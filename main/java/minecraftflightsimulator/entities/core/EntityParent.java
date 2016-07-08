package minecraftflightsimulator.entities.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.containers.ContainerParent;
import minecraftflightsimulator.containers.GUIParent;
import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntityEngineLarge;
import minecraftflightsimulator.entities.parts.EntityEngineSmall;
import minecraftflightsimulator.entities.parts.EntityPlaneChest;
import minecraftflightsimulator.entities.parts.EntityPontoon;
import minecraftflightsimulator.entities.parts.EntityPontoonDummy;
import minecraftflightsimulator.entities.parts.EntityPropeller;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.entities.parts.EntitySkid;
import minecraftflightsimulator.entities.parts.EntityWheel;
import minecraftflightsimulator.entities.parts.EntityWheelLarge;
import minecraftflightsimulator.entities.parts.EntityWheelSmall;
import minecraftflightsimulator.helpers.MFSVector;
import minecraftflightsimulator.helpers.RotationHelper;
import minecraftflightsimulator.packets.general.ServerSyncPacket;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public abstract class EntityParent extends EntityBase implements IInventory{
	public boolean brakeOn;
	public boolean parkingBrakeOn;
	public byte textureOptions;
	public byte numberChildren;
	public byte throttle;
	public int maxFuel;
	public float rotationRoll;
	public float prevRotationRoll;
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
	public MFSVector wingVec = new MFSVector(0, 0, 0);
	public MFSVector sideVec = new MFSVector(0, 0, 0);
	
	public static final int pilotSeatSlot = 24;
	public static final int passengerSeatSlot = 25;
	public static final int instrumentStartSlot = 30;
	public static final int emptyBucketSlot = 41;
	public static final int fuelBucketSlot = 42;

	/**
	 * ItemStack used to store all parent items.  Slot rules are as follows:
	 * Slot 1 is for the center wheel on aircraft.
	 * Slots 2 and 3 are for left wheels, while slot 4 and 5 are for right wheels.
	 * Slots 6-9 are for engines.
	 * Slots 10-13 are for propellers.
	 * Slots 24 is for pilot seats.
	 * Slots 25 is for passenger seats.
	 * 
	 * 
	 * Slots 30 through 40 are used for gauges.
	 * Slot 41 is for empty buckets.
	 * Slot 42 is for fuel buckets, which drop out of the inventory if left in slot during closing.
	 */
	private ItemStack[] compenentItems = new ItemStack[fuelBucketSlot+1];
	
	/**
	 * Array containing locations of all child positions with respect to slots.
	 * All positions should be initialized in entity's {@link initChildPositions} method.
	 * Note that core entities should NOT be put here, as they're
	 * directly linked to the parent and can't be added in the GUI.
	 * Also note that seats don't use this mapping as their slots are able to hold multiple seats.
	 */
	private Map<Integer, float[]> partPositions;

	private List<float[]> pilotPositions;
	
	private List<float[]> passengerPositions;
	
	/**
	 * Map that contains child mappings.  Keyed by child's UUID.
	 * Note that this is for moving and linking children, and will be empty until
	 * children get linked.
	 */
	private Map<String, EntityChild> children = new HashMap<String, EntityChild>();

	/**
	 * Map that contains landing gear mappings.  Keyed by landing gear's UUID.
	 * Note that this is for performing landing operations, and will update as landing gears are
	 * linked and destroyed.
	 */
	private Map<String, EntityLandingGear> landingGears = new HashMap<String, EntityLandingGear>();
	
	/**
	 * Map that contains engine mappings.  Keyed by engine's UUID.
	 * Note that this is for performing engine operations, and will update as engines are
	 * linked and destroyed.
	 */
	private Map<String, EntityEngine> engines = new HashMap<String, EntityEngine>();
	
	/**
	 * Map that contains propeller mappings.  Keyed by propeller's UUID.
	 * Note that this is for performing propeller operations, and will update as propeller are
	 * linked and destroyed.
	 */
	private Map<String, EntityPropeller> propellers = new HashMap<String, EntityPropeller>();
	
	/**
	 * BiMap that contains mappings of engine UUID to propeller UUID.  Used when propellers
	 * need to know what engine powers them.  Or when engines need to know what propeller to power.
	 */
	private BiMap<String, String> enginePropellers = HashBiMap.create();
	
	/**
	 * List containing data about what instruments are equipped.
	 */
	public List<ItemStack> instrumentList = new ArrayList<ItemStack>();
	
	public EntityParent(World world){
		super(world);
		this.setSize(0.75F, 0.75F);
		this.ignoreFrustumCheck=true;
		this.preventEntitySpawning = false;
		for(int i=0; i<10; ++i){
			instrumentList.add(null);
		}
	}
	
	public EntityParent(World world, float posX, float posY, float posZ, float playerRotation){
		this(world);
		this.setPositionAndRotation(posX, posY, posZ, playerRotation-90, 0);
		this.UUID=String.valueOf(this.getUniqueID());
		this.numberChildren=(byte) this.getCoreLocations().length;
	}
	
	@Override
	protected void entityInit(){
		this.initPlaneProperties();
		partPositions = new HashMap<Integer, float[]>();
		pilotPositions = new ArrayList<float[]>();
		passengerPositions = new ArrayList<float[]>();
		this.initChildPositions();
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(!this.hasUUID()){return;}
		if(!linked){
			if(this.ticksExisted>100){
				System.err.println("KILLING PARENT WITH WRONG NUMBER OF CHILDREN.  WANTED:" + numberChildren + " FOUND:" + children.size() +".");
				this.setDead();
			}else{
				linked = children.size() == numberChildren ? true : false;
			}
		}else if(!worldObj.isRemote && this.ticksExisted%5==0){
			MFS.MFSNet.sendToAll(new ServerSyncPacket(getEntityId(), posX, posY, posZ, motionX, motionY, motionZ, rotationPitch, rotationRoll, rotationYaw));
		}
		airDensity = 1.225*Math.pow(2, -posY/500);
		if(fuel < 0){fuel = 0;}
		fuelFlow = prevFuel - fuel;
		prevFuel = fuel;
	}
	
	@Override
    public boolean interactFirst(EntityPlayer player){
		return this.performRightClickAction(this, player);
	}
	
	@Override
    public boolean attackEntityFrom(DamageSource source, float damage){
		return this.performAttackAction(this, source, damage);
	}
	
	public MFSVector getHeadingVec(){
        float f1 = MathHelper.cos(-this.rotationYaw * 0.017453292F - (float)Math.PI);
        float f2 = MathHelper.sin(-this.rotationYaw * 0.017453292F - (float)Math.PI);
        float f3 = -MathHelper.cos(-this.rotationPitch * 0.017453292F);
        float f4 = MathHelper.sin(-this.rotationPitch * 0.017453292F);
        return new MFSVector((double)(f2 * f3), (double)f4, (double)(f1 * f3));
   	}
	
	@Override
	public void setDead(){
		super.setDead();
		if(!worldObj.isRemote){
			openInventory();
			for(int i=1; i<getSizeInventory(); ++i){
				ItemStack item = getStackInSlot(i);
				if(item != null){
					worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, item));
				}
			}
		}
		for(EntityChild child : getChildren()){
			removeChild(child.UUID);
		}
	}
	
	//Start of custom methods
	/**
	 * Handler for all right-clicking actions performed.  May be circumvented by overriding
	 * the appropriate methods in subclassed child entities.
	 * @param entityClicked the entity that was clicked
	 * @param player the player that clicked this entity
	 * 
	 * @return whether or not an action occurred.
	 */
	public boolean performRightClickAction(EntityBase entityClicked, EntityPlayer player){
		if(!worldObj.isRemote){
			if(player.getHeldItem() != null){
				if(player.getHeldItem().getItem().equals(Items.name_tag)){
					this.displayName = player.getHeldItem().getDisplayName().length() > 12 ? player.getHeldItem().getDisplayName().substring(0, 11) : player.getHeldItem().getDisplayName();
					this.sendDataToClient();
					return true;
				}
			}
			if(entityClicked.equals(this)){
				for(EntityChild child : this.getChildren()){
					if(child.getEntityBoundingBox().intersectsWith(this.getEntityBoundingBox()) && child instanceof EntitySeat){
						child.interactFirst(player);
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean performAttackAction(EntityBase attackedEntity, DamageSource source, float damage){
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
	
	public void explodeAtPosition(double x, double y, double z){
		this.setDead();
		worldObj.newExplosion(this, x, y, z, (float) (fuel/1000 + 1F), true, true);
	}
	
	protected void addCenterGearPosition(float[] coords){
		if(!partPositions.containsKey(1)){
			partPositions.put(1, coords);
		}else{
			System.err.println("AN ENTITY HAS TRIED TO ADD TOO MANY CENTER GEARS!  THINGS MAY GO BADLY!");
		}
	}
	
	protected void addLeftGearPosition(float[] coords){
		if(!partPositions.containsKey(2)){
			partPositions.put(2, coords);
		}else if(!partPositions.containsKey(3)){
			partPositions.put(3, coords);
		}else{
			System.err.println("AN ENTITY HAS TRIED TO ADD TOO MANY LEFT GEARS!  THINGS MAY GO BADLY!");
		}
	}
	
	protected void addRightGearPosition(float[] coords){
		if(!partPositions.containsKey(4)){
			partPositions.put(4, coords);
		}else if(!partPositions.containsKey(5)){
			partPositions.put(5, coords);
		}else{
			System.err.println("AN ENTITY HAS TRIED TO ADD TOO MANY RIGHT GEARS!  THINGS MAY GO BADLY!");
		}
	}
	
	protected void addEnginePosition(float[] coords){
		for(int i=6; i<=9; ++i){
			if(!partPositions.containsKey(i)){
				partPositions.put(i, coords);
				return;
			}
		}
		System.err.println("AN ENTITY HAS TRIED TO ADD TOO MANY ENGINES!  THINGS MAY GO BADLY!");
	}
	
	protected void addPropellerPosition(float[] coords){
		for(int i=10; i<=13; ++i){
			if(!partPositions.containsKey(i)){
				partPositions.put(i, coords);
				return;
			}
		}
		System.err.println("AN ENTITY HAS TRIED TO ADD TOO MANY PROPELLERS!  THINGS MAY GO BADLY!");
	}
	
	protected void addPilotPosition(float[] coords){
		pilotPositions.add(coords);
	}
	
	protected void addPassengerPosition(float[] coords){
		passengerPositions.add(coords);
	}
	
	/**
	 * Spawns a child and adds a child to all appropriate mappings.
	 * Set newChild to true if parent needs to keep track of an additional child.
	 * @param childUUID
	 * @param child
	 * @param newChild
	 */
	public void addChild(String childUUID, EntityChild child, boolean newChild){
		if(!children.containsKey(childUUID)){
			children.put(childUUID, child);
			if(newChild){
				++numberChildren;
				if(child.isCollidedHorizontally()){
					float boost = Math.max(0, -child.offsetY);
					this.rotationRoll = 0;
					this.setPositionAndRotation(posX, posY + boost, posZ, rotationYaw, 0);
					child.setPosition(posX + child.offsetX, posY + child.offsetY + boost, posZ + child.offsetZ);
				}
				worldObj.spawnEntityInWorld(child);
			}
		}
		if(child instanceof EntityLandingGear){
			landingGears.put(childUUID, (EntityLandingGear) child);
		}else if(child instanceof EntityEngine){
			engines.put(childUUID, (EntityEngine) child);
			float[] childOffset = new float[]{child.offsetX, child.offsetY, child.offsetZ};
			for(int i=1; i<pilotSeatSlot; ++i){
				if(Arrays.equals(childOffset, partPositions.get(i))){
					if(getChildAtLocation(partPositions.get(i+4)) != null){
						enginePropellers.forcePut(childUUID, getChildAtLocation(partPositions.get(i+4)).UUID);
					}
				}
			}
		}else if(child instanceof EntityPropeller){
			propellers.put(childUUID, (EntityPropeller) child);
			float[] childOffset = new float[]{child.offsetX, child.offsetY, child.offsetZ};
			for(int i=1; i<pilotSeatSlot; ++i){
				if(Arrays.equals(childOffset, partPositions.get(i))){
					if(getChildAtLocation(partPositions.get(i-4)) != null){
						enginePropellers.forcePut(getChildAtLocation(partPositions.get(i-4)).UUID, childUUID);
					}
				}
			}
		}
	}
	
	/**
	 * Removes a child from mappings, setting it dead in the process.
	 * @param childUUID
	 */
	public void removeChild(String childUUID){
		if(children.containsKey(childUUID)){
			children.remove(childUUID).setDead();
			--numberChildren;
		}
		landingGears.remove(childUUID);
		engines.remove(childUUID);
		propellers.remove(childUUID);
		enginePropellers.remove(childUUID);
		enginePropellers.inverse().remove(childUUID);
	}

	public void moveChildren(){
		for(EntityChild child : getChildren()){
			if(child.isDead){
				removeChild(child.UUID);
			}else{
				MFSVector offset = RotationHelper.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch, rotationYaw, rotationRoll);
				child.setPosition(posX + offset.xCoord, posY + offset.yCoord, posZ + offset.zCoord);
				child.updateRiderPosition();
			}
		}
	}
	
	/**
	 * Given an offset coordinate set, returns the child at that offset.
	 * Used for finding out which slot a specified entity belongs to.
	 * @param offsetCoords
	 * @return the child with the specified offset.
	 */
	private EntityChild getChildAtLocation(float[] offsetCoords){
		for(EntityChild child : getChildren()){
			if(child.getClass().equals(EntityCore.class)){continue;}
			float[] childOffset = new float[]{child.offsetX, child.offsetY, child.offsetZ};
			if(Arrays.equals(childOffset, offsetCoords)){
				return child;
			}
		}
		return null;
	}
	
	//TODO fix bug where client engine starts without server.
	public void setEngineState(byte engineCode){
		if(engineCode == 0){
			throttle = 0;
			for(EntityEngine engine : getEngines()){
				engine.stopEngine(true);
			}
		}else if(engineCode != 1){
			if(throttle < 15){throttle = 15;}
			for(EntityEngine engine : getEngines()){
				float[] enginePosition = {engine.offsetX, engine.offsetY, engine.offsetZ};
				if(Arrays.equals(partPositions.get((int) engineCode), enginePosition)){
					engine.startEngine();
					return;
				}
			}
		}else{
			if(throttle < 15){throttle = 15;}
			for(EntityEngine engine : getEngines()){
				engine.startEngine();
			}
		}
	}
	
	public byte getEngineOfHitPropeller(String propellerUUID){
		String engineUUID = enginePropellers.inverse().get(propellerUUID);
		for(EntityEngine engine : getEngines()){
			if(engine.UUID.equals(engineUUID)){
				float[] enginePosition = {engine.offsetX, engine.offsetY, engine.offsetZ};
				for(int i=6; i<=9; ++i){
					if(Arrays.equals(enginePosition, partPositions.get(i))){
						return (byte) i;
					}
				}
			}
		}
		return 0;
	}
	
	public List<Double> getEngineSpeeds(){
		List<Double> speeds = new ArrayList<Double>();
		for(EntityEngine engine : getEngines()){
			speeds.add(engine.engineRPM);
		}
		return speeds;
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
	
	public int getNumberPilotSeats(){return pilotPositions.size();}
	public int getNumberPassengerSeats(){return passengerPositions.size();}
	protected EntityChild[] getChildren(){return children.values().toArray(new EntityChild[children.size()]);}	
	protected EntityWheel[] getWheels(){return landingGears.values().toArray(new EntityWheel[landingGears.size()]);}
	protected EntityEngine[] getEngines(){return engines.values().toArray(new EntityEngine[engines.size()]);}
	protected EntityPropeller[] getPropellers(){return propellers.values().toArray(new EntityPropeller[propellers.size()]);}
	public EntityPropeller getPropellerForEngine(String engineUUID){return propellers.get(enginePropellers.get(engineUUID));}
	
	protected abstract void initPlaneProperties();
	protected abstract void initChildPositions();
	public abstract float[][] getCoreLocations();
	public abstract void drawHUD(int width, int height);
	public abstract GUIParent getGUI(EntityPlayer player);
	public abstract void initParentContainerSlots(ContainerParent container);
	
	
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
	public String getInventoryName(){return "Parent Inventory";}
	public ItemStack getStackInSlot(int slot){return compenentItems[slot];}
	public ItemStack getStackInSlotOnClosing(int slot){return null;}
	
	@Override
	public void setInventorySlotContents(int slot, ItemStack stack){
		compenentItems[slot] = stack;
	}
	
	@Override
    public ItemStack decrStackSize(int slot, int number){
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
	
	public void loadInventory(){
		if(!worldObj.isRemote){
			//First, match part positions to slots.
			for(int i=1; i<pilotSeatSlot; ++i){
				EntityChild child = getChildAtLocation(partPositions.get(i));				
				if(child != null){
					if(child instanceof EntityWheelSmall){
						setInventorySlotContents(i, new ItemStack(MFS.proxy.wheelSmall));
					}else if(child instanceof EntityWheelLarge){
						setInventorySlotContents(i, new ItemStack(MFS.proxy.wheelLarge));
					}else if(child instanceof EntityPontoon){
						setInventorySlotContents(i, new ItemStack(MFS.proxy.pontoon));
					}else if(child instanceof EntitySkid){
						setInventorySlotContents(i, new ItemStack(MFS.proxy.skid));
					}else if(child instanceof EntityEngineSmall){
						setInventorySlotContents(i, new ItemStack(MFS.proxy.engineSmall, 1, child.propertyCode));
					}else if(child instanceof EntityEngineLarge){
						setInventorySlotContents(i, new ItemStack(MFS.proxy.engineLarge, 1, child.propertyCode));
					}else if(child instanceof EntityPropeller){
						setInventorySlotContents(i, new ItemStack(MFS.proxy.propeller, 1, child.propertyCode));
					}else{
						setInventorySlotContents(i, null);
					}
				}else{
					setInventorySlotContents(i, null);
				}
			}
			
			//Next, get seats inputed
			int numberPilotSeats = 0;
			int pilotPropertyCode = 0;
			for(int i=0; i<pilotPositions.size(); ++i){
				EntityChild child = getChildAtLocation(pilotPositions.get(i));
				if(child != null){
					++numberPilotSeats;
					pilotPropertyCode = child.propertyCode;
				}
			}
			if(numberPilotSeats > 0){
				setInventorySlotContents(pilotSeatSlot, new ItemStack(MFS.proxy.seat, numberPilotSeats, pilotPropertyCode));
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
					setInventorySlotContents(passengerSeatSlot, new ItemStack(MFS.proxy.seat, numberPassengerSeats, passengerPropertyCode));
				}
			}
			
			//Finally, do instrument and bucket things
			for(int i=0; i<10; ++i){
				setInventorySlotContents(i+instrumentStartSlot, instrumentList.get(i));
			}
		}
	}

    public void saveInventory(){
		if(!worldObj.isRemote){
			EntityChild newChild;
			float boostAmount = 0;
			
			//First, spawn components
			for(int i=1; i<pilotSeatSlot; ++i){
				float[] position = partPositions.get(i);
				EntityChild child = getChildAtLocation(partPositions.get(i));
				ItemStack stack = getStackInSlot(i);
				
				if(stack != null){
					//Is a propeller not connected to an engine?
					if(i >= 10 && i <= 13 && getStackInSlot(i-4) == null){
						worldObj.spawnEntityInWorld(new EntityItem(worldObj, this.posX, this.posY, this.posZ, new ItemStack(MFS.proxy.propeller, 1, stack.getItemDamage())));
						setInventorySlotContents(i, null);
						if(child != null){
							removeChild(child.UUID);
						}
						continue;
					}
					
					if(child != null){
						if(child.propertyCode != stack.getItemDamage() || (!((child instanceof EntityWheel || child instanceof EntitySkid) ^ stack.getItem().equals(MFS.proxy.pontoon)) && i <= 5)){
							removeChild(child.UUID);
						}else{
							continue;
						}
					}
					
					if(stack.getItem().equals(MFS.proxy.wheelSmall)){
						newChild = new EntityWheelSmall(worldObj, this, this.UUID, position[0], position[1], position[2]);
					}else if(stack.getItem().equals(MFS.proxy.wheelLarge)){
						newChild = new EntityWheelLarge(worldObj, this, this.UUID, position[0], position[1], position[2]);
					}else if(stack.getItem().equals(MFS.proxy.pontoon)){
						newChild = new EntityPontoon(worldObj, this, this.UUID, position[0], position[1] - (position[2] > 0 ? 0 : 0.1F), position[2] + (position[2] > 0 ? 0 : 2));
						EntityPontoonDummy pontoonDummy = new EntityPontoonDummy(worldObj, this, this.UUID, position[0], position[1] + (position[2] > 0 ? 0.25F : 0), position[2] - (position[2] > 0 ? 2 : 0));
						pontoonDummy.setOtherHalf((EntityPontoon) newChild);
						((EntityPontoon) newChild).setOtherHalf(pontoonDummy);
						addChild(pontoonDummy.UUID, pontoonDummy, true);
					}else if(stack.getItem().equals(MFS.proxy.skid)){
						newChild = new EntitySkid(worldObj, this, this.UUID, position[0], position[1], position[2]);
					}else if(stack.getItem().equals(MFS.proxy.engineSmall)){
						newChild = new EntityEngineSmall(worldObj, this, this.UUID, position[0], position[1], position[2], stack.getItemDamage());
					}else if(stack.getItem().equals(MFS.proxy.engineLarge)){
						newChild = new EntityEngineLarge(worldObj, this, this.UUID, position[0], position[1], position[2], stack.getItemDamage());
					}else if(stack.getItem().equals(MFS.proxy.propeller)){
						newChild = new EntityPropeller(worldObj, this, this.UUID, position[0], position[1], position[2], stack.getItemDamage());
					}else{
						continue;
					}
					addChild(newChild.UUID, newChild, true);
				}else{
					if(child != null){
						removeChild(child.UUID);
					}
				}
			}
			
			//Next, spawn new seats
			int numberPilotSeats = getStackInSlot(pilotSeatSlot) == null ? 0 : getStackInSlot(pilotSeatSlot).stackSize;
			for(int i=0; i<pilotPositions.size(); ++i){
				float[] position = pilotPositions.get(i);
				EntityChild child = getChildAtLocation(position);
				
				if(child != null){
					if(getStackInSlot(pilotSeatSlot) == null ? true : (i+1 > numberPilotSeats || getStackInSlot(pilotSeatSlot).getItemDamage() != child.propertyCode)){
						child.setDead();
						removeChild(child.UUID);
					}
				}
				if(child == null ? true : child.isDead){
					if(i+1 <= numberPilotSeats){
						newChild = new EntitySeat(worldObj, this, this.UUID, position[0], position[1], position[2], getStackInSlot(pilotSeatSlot).getItemDamage(), true);
						addChild(newChild.UUID, newChild, true);
					}
				}
			}
			
			int numberPassengerSeats = getStackInSlot(passengerSeatSlot) == null ? 0 : getStackInSlot(passengerSeatSlot).stackSize;
			boolean chests = getStackInSlot(passengerSeatSlot) == null ? false : (getStackInSlot(passengerSeatSlot).getItem().equals(Item.getItemFromBlock(Blocks.chest)) ? true : false);
			for(int i=0; i<passengerPositions.size(); ++i){
				float[] position = passengerPositions.get(i);
				EntityChild child = getChildAtLocation(position);
				if(child != null){
					if(getStackInSlot(passengerSeatSlot) == null ? true : (i+1 > numberPassengerSeats || getStackInSlot(passengerSeatSlot).getItemDamage() != child.propertyCode || !(child instanceof EntitySeat ^ chests))){
						child.setDead();
						removeChild(child.UUID);
					}
				}
				if(child == null ? true : child.isDead){
					if(i+1 <= numberPassengerSeats){
						if(chests){ 
							newChild = new EntityPlaneChest(worldObj, this, this.UUID, position[0], position[1], position[2]);
						}else{
							newChild = new EntitySeat(worldObj, this, this.UUID, position[0], position[1], position[2], getStackInSlot(passengerSeatSlot).getItemDamage(), false);
						}
						addChild(newChild.UUID, newChild, true);
					}
				}
			}
			
			//Finally, do instrument and bucket things
			for(int i=0; i<10; ++i){
				instrumentList.set(i, getStackInSlot(i+instrumentStartSlot));
			}
			
			if(getStackInSlot(fuelBucketSlot) != null){
				worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, getStackInSlot(fuelBucketSlot)));
				setInventorySlotContents(fuelBucketSlot, null);
			}
			this.sendDataToClient();
		}
	}
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.brakeOn=tagCompound.getBoolean("brakeOn");
		this.parkingBrakeOn=tagCompound.getBoolean("parkingBrakeOn");
		this.numberChildren=tagCompound.getByte("numberChildren");
		this.textureOptions=tagCompound.getByte("textureOptions");
		this.throttle=tagCompound.getByte("throttle");
		this.rotationRoll=tagCompound.getFloat("rotationRoll");
		this.fuel=tagCompound.getDouble("fuel");
		this.ownerName=tagCompound.getString("ownerName");
		this.displayName=tagCompound.getString("displayName");
		for(int i=0; i<10; ++i){
			if(tagCompound.hasKey("instrument" + i)){
				instrumentList.set(i, new ItemStack(MFS.proxy.flightInstrument, 1, tagCompound.getInteger("instrument" + i)));
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
		tagCompound.setByte("numberChildren", this.numberChildren);
		tagCompound.setByte("textureOptions", this.textureOptions);
		tagCompound.setByte("throttle", this.throttle);
		tagCompound.setFloat("rotationRoll", this.rotationRoll);
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
