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
import minecraftflightsimulator.entities.parts.EntityPropeller;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.entities.parts.EntityWheel;
import minecraftflightsimulator.entities.parts.EntityWheelLarge;
import minecraftflightsimulator.entities.parts.EntityWheelSmall;
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
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public abstract class EntityParent extends EntityBase implements IInventory{
	public boolean brakeOn;
	public boolean parkingBrakeOn;
	public byte textureOptions;
	public byte numberChildren;
	public byte throttle;
	public byte emptyBuckets;
	public int maxFuel;
	public float rotationRoll;
	public float prevRotationRoll;
	public double fuel;
	public double prevFuel;
	public double fuelFlow;
	public double velocity;
	public double airDensity;
	public double trackAngle;
	public String ownerName;
	
	public Vec3 velocityVec = Vec3.createVectorHelper(0, 0, 0);
	public Vec3 bearingVec = Vec3.createVectorHelper(0, 0, 0);
	public Vec3 wingVec = Vec3.createVectorHelper(0, 0, 0);
	public Vec3 sideVec = Vec3.createVectorHelper(0, 0, 0);
	
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
	 * Map that contains wheel mappings.  Keyed by wheel's UUID.
	 * Note that this is for performing wheel operations, and will update as wheels are
	 * linked and destroyed.
	 */
	private Map<String, EntityWheel> wheels = new HashMap<String, EntityWheel>();
	
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
	 * Map that contains mappings from propeller UUID to engine UUID.  Used when propellers
	 * need to know what engine powers them.
	 */
	private Map<String, String> propellerEngines = new HashMap<String, String>();
	
	/**
	 * Map that contains mappings from engine UUID to propeller UUID.  Used when engines
	 * need to know what propeller to power.
	 */
	private Map<String, String> enginePropellers = new HashMap<String, String>();
	
	/**
	 * List containing data about what instruments are equipped.
	 */
	public List<ItemStack> instrumentList = new ArrayList<ItemStack>();
	
	public EntityParent(World world){
		super(world);
		this.setSize(1F, 1F);
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
		this.ownerName="MFS";
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
    public boolean interactFirst(EntityPlayer player){
		if(!worldObj.isRemote){
			if(player.isSneaking()){
				player.openGui(MFS.instance, this.getEntityId(), worldObj, (int) posX, (int) posY, (int) posZ);
				return true;
			}else{
				for(EntityChild child : getChildren()){
					if(child instanceof EntitySeat){
						if(this.boundingBox.intersectsWith(child.boundingBox)){
							child.interactFirst(player);
						}
					}
				}
			}
		}
		return false;
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
	public Vec3 getLookVec(){
        float f1 = MathHelper.cos(-this.rotationYaw * 0.017453292F - (float)Math.PI);
        float f2 = MathHelper.sin(-this.rotationYaw * 0.017453292F - (float)Math.PI);
        float f3 = -MathHelper.cos(-this.rotationPitch * 0.017453292F);
        float f4 = MathHelper.sin(-this.rotationPitch * 0.017453292F);
        return Vec3.createVectorHelper((double)(f2 * f3), (double)f4, (double)(f1 * f3));
   	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float damage){
		super.attackEntityFrom(source, damage);
		if(!worldObj.isRemote){
			if(source.getEntity() instanceof EntityPlayer){
				if(((EntityPlayer) source.getEntity()).isSneaking()){
					if(((EntityPlayer) source.getEntity()).capabilities.isCreativeMode){
						this.setDead();
					}
				}
			}
		}
		return false;
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
			child.setDead();
		}
	}
	
	public void explodeAtPosition(double x, double y, double z){
		this.setDead();
		worldObj.newExplosion(this, x, y, z, (float) (fuel/1000 + 1F), true, true);
	}

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
		EntityEngine engine = engines.get(propellerEngines.get(propellerUUID));
		if(engine != null){
			float[] enginePosition = {engine.offsetX, engine.offsetY, engine.offsetZ};
			for(int i=6; i<=9; ++i){
				if(Arrays.equals(enginePosition, partPositions.get(i))){
					return (byte) i;
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
	
	protected void addCenterWheelPosition(float[] coords){
		if(!partPositions.containsKey(1)){
			partPositions.put(1, coords);
		}else{
			System.err.println("AN ENTITY HAS TRIED TO ADD TOO MANY CENTER WHEELS!  THINGS MAY GO BADLY!");
		}
	}
	
	protected void addLeftWheelPosition(float[] coords){
		if(!partPositions.containsKey(2)){
			partPositions.put(2, coords);
		}else if(!partPositions.containsKey(3)){
			partPositions.put(3, coords);
		}else{
			System.err.println("AN ENTITY HAS TRIED TO ADD TOO MANY LEFT WHEELS!  THINGS MAY GO BADLY!");
		}
	}
	
	protected void addRightWheelPosition(float[] coords){
		if(!partPositions.containsKey(4)){
			partPositions.put(4, coords);
		}else if(!partPositions.containsKey(5)){
			partPositions.put(5, coords);
		}else{
			System.err.println("AN ENTITY HAS TRIED TO ADD TOO MANY RIGHT WHEELS!  THINGS MAY GO BADLY!");
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
	 * Adds a child to all appropriate mappings.  Set newChild to true if parent needs
	 * to keep track of an additional child.
	 * @param childUUID
	 * @param child
	 * @param newChild
	 */
	public void addChild(String childUUID, EntityChild child, boolean newChild){
		if(!children.containsKey(childUUID)){
			children.put(childUUID, child);
			if(newChild){
				++numberChildren;
			}
		}
		if(child instanceof EntityWheel){
			wheels.put(childUUID, (EntityWheel) child);
		}else if(child instanceof EntityEngine){
			engines.put(childUUID, (EntityEngine) child);
			for(EntityPropeller propeller : getPropellers()){
				if(propeller.offsetX == child.offsetX && Math.abs(propeller.offsetZ - child.offsetZ) < 2){
					propellerEngines.put(propeller.UUID, childUUID);
					enginePropellers.put(childUUID, propeller.UUID);
				}
			}
		}else if(child instanceof EntityPropeller){
			propellers.put(childUUID, (EntityPropeller) child);
			for(EntityEngine engine : getEngines()){
				if(engine.offsetX == child.offsetX && Math.abs(engine.offsetZ - child.offsetZ) < 2){
					propellerEngines.put(childUUID, engine.UUID);
					enginePropellers.put(engine.UUID, childUUID);
				}
			}
		}
	}
	
	/**
	 * Removes a child from mappings.
	 * remove method is not used.
	 * @param childUUID
	 */
	public void removeChild(String childUUID){
		if(children.containsKey(childUUID)){
			children.remove(childUUID);
			--numberChildren;
		}
		wheels.remove(childUUID);
		engines.remove(childUUID);
		propellers.remove(childUUID);
		propellerEngines.remove(childUUID);
		enginePropellers.remove(childUUID);
	}
	
	public void moveChildren(){
		for(EntityChild child : getChildren()){
			if(child.isDead){
				removeChild(child.UUID);
			}else{
				Vec3 offset = RotationHelper.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch, rotationYaw, rotationRoll);
				child.setPosition(posX + offset.xCoord, posY + offset.yCoord, posZ + offset.zCoord);
				child.updateRiderPosition();
			}
		}
	}
	
	private EntityChild getChildAtLocation(float[] coords){
		for(EntityChild child : getChildren()){
			if(child.getClass().equals(EntityCore.class)){continue;}
			float[] childPos = new float[]{child.offsetX, child.offsetY, child.offsetZ};
			if(Arrays.equals(childPos, coords)){
				return child;
			}
		}
		return null;
	}
	
	public int getNumberPilotSeats(){
		return pilotPositions.size();
	}
	
	public int getNumberPassengerSeats(){
		return passengerPositions.size();
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
	
	protected EntityChild[] getChildren(){return children.values().toArray(new EntityChild[children.size()]);}	
	protected EntityWheel[] getWheels(){return wheels.values().toArray(new EntityWheel[wheels.size()]);}
	protected EntityEngine[] getEngines(){return engines.values().toArray(new EntityEngine[engines.size()]);}
	protected EntityPropeller[] getPropellers(){return propellers.values().toArray(new EntityPropeller[propellers.size()]);}
	public EntityPropeller getPropellerForEngine(String engineUUID){return propellers.get(enginePropellers.get(engineUUID));}
	
	protected abstract void initPlaneProperties();
	protected abstract void initChildPositions();
	public abstract float[][] getCoreLocations();
	public abstract void drawHUD(int width, int height);
	public abstract GUIParent getGUI(EntityPlayer player);
	public abstract void initParentContainerSlots(ContainerParent container);
	
	public boolean canBeCollidedWith(){
		return true;
	}
	
	//Start of IInventory section
	@Override
	public int getSizeInventory(){
		return compenentItems.length;
	}
	
	@Override
	public ItemStack getStackInSlot(int slot){
		return compenentItems[slot];
	}
	
	@Override
	public ItemStack decrStackSize(int slot, int amount){
		ItemStack stack = getStackInSlot(slot);
		if(stack != null){
			setInventorySlotContents(slot, null);
		}
		return stack;
	}
	
	@Override
	public ItemStack getStackInSlotOnClosing(int slot){
		ItemStack stack = getStackInSlot(slot);
		if(stack != null){
			setInventorySlotContents(slot, null);
		}
		return stack;
	}
	
	@Override
	public void setInventorySlotContents(int slot, ItemStack stack){
		compenentItems[slot] = stack;
	}
	
	@Override
	public String getInventoryName(){
		return "Parent Inventory";
	}
	
	@Override
	public boolean hasCustomInventoryName(){
		return false;
	}
	
	@Override
	public int getInventoryStackLimit(){
		return 1;
	}
	
	@Override
	public void markDirty(){}
	
	@Override
	public boolean isUseableByPlayer(EntityPlayer player){
		return player.getDistanceToEntity(this) < 5;
	}
	
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack){
		return false;
	}
	
	@Override
    public void openInventory(){
		if(!worldObj.isRemote){
			EntityChild child;
			//First match part positions to slots.
			for(int i=1; i<pilotSeatSlot; ++i){
				child = getChildAtLocation(partPositions.get(i));
				if(child != null){
					if(i <= 5){
						if(child instanceof EntityWheelLarge){
							setInventorySlotContents(i, new ItemStack(MFS.proxy.wheelLarge));
						}else{
							setInventorySlotContents(i, new ItemStack(MFS.proxy.wheelSmall));
						}
					}else if(i <= 9){
						if(child instanceof EntityEngineLarge){
							setInventorySlotContents(i, new ItemStack(MFS.proxy.engineLarge, 1, child.propertyCode));
						}else{
							setInventorySlotContents(i, new ItemStack(MFS.proxy.engineSmall, 1, child.propertyCode));
						}
					}else if(i <= 13){
						setInventorySlotContents(i, new ItemStack(MFS.proxy.propeller, 1, child.propertyCode));
					}
				}else{
					setInventorySlotContents(i, null);
				}
			}
			
			//Next get seats inputed
			int numberPilotSeats = 0;
			int pilotPropertyCode = 0;
			for(int i=0; i<pilotPositions.size(); ++i){
				child = getChildAtLocation(pilotPositions.get(i));
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
				child = getChildAtLocation(passengerPositions.get(i));
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
			
			for(int i=0; i<10; ++i){
				setInventorySlotContents(i+instrumentStartSlot, instrumentList.get(i));
			}

			if(emptyBuckets>0){
				setInventorySlotContents(emptyBucketSlot, new ItemStack(Items.bucket, emptyBuckets));
			}
		}
	}

	@Override
    public void closeInventory(){
		if(!worldObj.isRemote){
			EntityChild child;
			EntityChild newChild;
			ItemStack stack;
			boolean needsBoosting = true;
			
			//First spawn components
			for(int i=1; i<pilotSeatSlot; ++i){
				child = getChildAtLocation(partPositions.get(i));
				stack = getStackInSlot(i);
				if(stack != null){
					if(child != null){
						if(child.propertyCode == stack.getItemDamage()){
							if(i >= 10 && i <= 13 && getStackInSlot(i-4) == null){
								worldObj.spawnEntityInWorld(new EntityItem(worldObj, this.posX, this.posY, this.posZ, new ItemStack(MFS.proxy.propeller, 1,stack.getItemDamage())));
								child.setDead();
								removeChild(child.UUID);
								setInventorySlotContents(i, null);
							}
							continue;
						}else{
							child.setDead();
							removeChild(child.UUID);
						}
					}
					
					//TODO fix false boosting
					if(needsBoosting){
						this.rotationPitch = 0;
						this.rotationRoll = 0;
						this.setPosition(posX, posY+1.5, posZ);
						this.sendDataToClient();
						needsBoosting = false;
					}
					
					float[] position = partPositions.get(i);
					if(i <= 5){
						if(stack.getItem().equals(MFS.proxy.wheelLarge)){
							newChild = new EntityWheelLarge(worldObj, this, this.UUID, position[0], position[1], position[2]);
						}else{
							newChild = new EntityWheelSmall(worldObj, this, this.UUID, position[0], position[1], position[2]);
						}
					}else if(i <= 9){
						if(stack.getItem().equals(MFS.proxy.engineLarge)){
							newChild = new EntityEngineLarge(worldObj, this, this.UUID, position[0], position[1], position[2], stack.getItemDamage());
						}else{
							newChild = new EntityEngineSmall(worldObj, this, this.UUID, position[0], position[1], position[2], stack.getItemDamage());
						}
					}else if(i <= 13){
						newChild = new EntityPropeller(worldObj, this, this.UUID, position[0], position[1], position[2], stack.getItemDamage());
					}else{
						continue;
					}
					worldObj.spawnEntityInWorld(newChild);
					addChild(newChild.UUID, newChild, true);
				}else{
					if(child != null){
						child.setDead();
						removeChild(child.UUID);
					}
				}
			}
			
			//Next spawn new seats
			int numberPilotSeats = getStackInSlot(pilotSeatSlot) == null ? 0 : getStackInSlot(pilotSeatSlot).stackSize;
			for(int i=0; i<pilotPositions.size(); ++i){
				float[] position = pilotPositions.get(i);
				child = getChildAtLocation(position);
				if(child != null){
					if(getStackInSlot(pilotSeatSlot) == null ? true : (i+1 > numberPilotSeats || getStackInSlot(pilotSeatSlot).getItemDamage() != child.propertyCode)){
						child.setDead();
						removeChild(child.UUID);
					}
				}
				if(child == null ? true : child.isDead){
					if(i+1 <= numberPilotSeats){
						newChild = new EntitySeat(worldObj, this, this.UUID, position[0], position[1], position[2], getStackInSlot(pilotSeatSlot).getItemDamage(), true);
						worldObj.spawnEntityInWorld(newChild);
						addChild(newChild.UUID, newChild, true);
					}
				}
			}
			
			int numberPassengerSeats = getStackInSlot(passengerSeatSlot) == null ? 0 : getStackInSlot(passengerSeatSlot).stackSize;
			boolean chests = getStackInSlot(passengerSeatSlot) == null ? false : (getStackInSlot(passengerSeatSlot).getItem().equals(Item.getItemFromBlock(Blocks.chest)) ? true : false);
			for(int i=0; i<passengerPositions.size(); ++i){
				float[] position = passengerPositions.get(i);
				child = getChildAtLocation(position);
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
						worldObj.spawnEntityInWorld(newChild);
						addChild(newChild.UUID, newChild, true);
					}
				}
			}
			
			for(int i=0; i<10; ++i){
				instrumentList.set(i, getStackInSlot(i+instrumentStartSlot));
			}
			
			if(getStackInSlot(emptyBucketSlot) != null){
				this.emptyBuckets = (byte) getStackInSlot(emptyBucketSlot).stackSize;
			}
			
			if(getStackInSlot(fuelBucketSlot) != null){
				worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, getStackInSlot(fuelBucketSlot)));
				setInventorySlotContents(fuelBucketSlot, null);
			}
			this.sendDataToClient();
			//MFS.MFSNet.sendToAll(new FuelPacket(this.getEntityId(), fuel, emptyBuckets));
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
		this.emptyBuckets=tagCompound.getByte("emptyBuckets");
		this.rotationRoll=tagCompound.getFloat("rotationRoll");
		this.fuel=tagCompound.getDouble("fuel");
		this.ownerName=tagCompound.getString("ownerName");
		for(int i=0; i<10; ++i){
			if(tagCompound.hasKey("instrument" + i)){
				instrumentList.set(i, new ItemStack(MFS.proxy.flightInstrument, 1, tagCompound.getInteger("instrument" + i)));
			}else{
				instrumentList.set(i, null);
			}
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
		tagCompound.setByte("emptyBuckets", this.emptyBuckets);
		tagCompound.setFloat("rotationRoll", this.rotationRoll);
		tagCompound.setDouble("fuel", this.fuel);
		tagCompound.setString("ownerName", this.ownerName);
		for(int i=0; i<instrumentList.size(); ++i){
			if(instrumentList.get(i) != null){
				tagCompound.setInteger("instrument" + i, instrumentList.get(i).getItemDamage());
			}
		}
	}
}
