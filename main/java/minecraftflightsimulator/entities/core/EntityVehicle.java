package minecraftflightsimulator.entities.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.minecrafthelpers.AABBHelper;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import minecraftflightsimulator.minecrafthelpers.ItemStackHelper;
import minecraftflightsimulator.minecrafthelpers.PlayerHelper;
import minecraftflightsimulator.systems.ConfigSystem;
import minecraftflightsimulator.utilites.MFSVector;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public abstract class EntityVehicle extends EntityParent{
	public boolean brakeOn;
	public boolean parkingBrakeOn;
	public boolean openTop;
	public byte lightSetup;
	public byte numberPowerfulLights;
	public byte lightStatus;
	public byte textureOptions;
	public byte throttle;
	public int maxFuel;
	public double fuel;
	public double electricPower = 12;
	public double electricUsage;
	public double electricFlow;
	public double velocity;
	public double airDensity;
	public double trackAngle;
	public double health;
	public String ownerName=MFS.MODID;
	public String displayName="";
	
	/**Map of instrument slots to type.
	 *Setting a slot to -1 prevents instruments from being placed there.
	 *Note that this is different than the -1 you give the instrument draw systems!
	 **/
	public Map<Byte, Byte> instruments;
	
	public MFSVector velocityVec = new MFSVector(0, 0, 0);
	public MFSVector headingVec = new MFSVector(0, 0, 0);
	public MFSVector verticalVec = new MFSVector(0, 0, 0);
	public MFSVector sideVec = new MFSVector(0, 0, 0);
	
	private Map<AxisAlignedBB, Integer[]> collisionMap = new HashMap<AxisAlignedBB, Integer[]>();
	protected List<AxisAlignedBB> collidingBoxes = new ArrayList<AxisAlignedBB>();
	
	private byte numberEngineBays = 0;
	private Map<Byte, EntityEngine> engineByNumber = new HashMap<Byte, EntityEngine>();
	
	public EntityVehicle(World world){
		super(world);
	}
	
	public EntityVehicle(World world, float posX, float posY, float posZ, float playerRotation){
		super(world, posX, posY, posZ, playerRotation);
	}
	
	@Override
	protected void entityInit(){
		super.entityInit();
		initProperties();
		instruments = new HashMap<Byte, Byte>();
		initProhibitedInstruments();
	}
	
	protected abstract void initProperties();
	protected abstract void initProhibitedInstruments();
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(!linked){return;}
		airDensity = 1.225*Math.pow(2, -posY/500);
		if(fuel < 0){fuel = 0;}
		if(electricPower > 2){
			if((lightStatus & 1) == 1){
				electricUsage += 0.001;
			}
			if((lightStatus & 2) == 2){
				electricUsage += 0.001;
			}
			if((lightStatus & 4) == 4){
				electricUsage += 0.001;
			}
			if((lightStatus & 8) == 8){
				electricUsage += 0.001*(1+numberPowerfulLights);
			}
		}
		electricPower = Math.max(0, Math.min(13, electricPower -= electricUsage));
		electricFlow = electricUsage;
		electricUsage = 0;
	}
	
	//Start of custom methods
	@Override
	public boolean performRightClickAction(EntityBase clicked, EntityPlayer player){
		if(!worldObj.isRemote){
			if(PlayerHelper.getHeldStack(player) != null){
				if(ItemStackHelper.getItemFromStack(PlayerHelper.getHeldStack(player)).equals(Items.name_tag)){
					this.displayName = PlayerHelper.getHeldStack(player).getDisplayName().length() > 12 ? PlayerHelper.getHeldStack(player).getDisplayName().substring(0, 11) : PlayerHelper.getHeldStack(player).getDisplayName();
					this.sendDataToClient();
					return true;
				}
			}
		}
		return super.performRightClickAction(clicked, player);
	}
	
	@Override
	public boolean performAttackAction(DamageSource source, float damage){
		if(!worldObj.isRemote){
			if(source.getEntity() instanceof EntityPlayer){
				EntityPlayer attackingPlayer = (EntityPlayer) source.getEntity();
				if(attackingPlayer.isSneaking()){
					if(attackingPlayer.capabilities.isCreativeMode || attackingPlayer.getDisplayName().endsWith(this.ownerName)){
						this.setDead();
						return true;
					}
				}
			}
			if(!this.equals(source.getEntity())){
				if(!this.isDead){
					health -= damage;
					if(health <= 0){
						this.explodeAtPosition(this.posX, this.posY, this.posZ);
					}
				}
			}
		}
		return true;
	}

	@Override
	public void setDead(){
		if(!worldObj.isRemote){
			for(EntityChild child : this.getChildren()){
				ItemStack stack = child.getItemStack();
				if(stack != null){
					worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, stack));
				}
			}
			for(Byte instrumentNumber : instruments.values()){
				ItemStack stack = new ItemStack(MFSRegistry.flightInstrument, 1, instrumentNumber);
				worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, stack));
			}
		}
		super.setDead();
	}
	
	protected List<AxisAlignedBB> getChildCollisions(EntityChild child, AxisAlignedBB box){
		//Need to contract the box because sometimes the slight error in math causes issues.
		collisionMap = AABBHelper.getCollidingBlockBoxes(worldObj, box.contract(0.01F, 0.01F,  0.01F), child.collidesWithLiquids());
		collidingBoxes.clear();
		if(!collisionMap.isEmpty()){
			for(Entry<AxisAlignedBB, Integer[]> entry : collisionMap.entrySet()){
				float hardness = BlockHelper.getBlockHardness(worldObj, entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]);
				if(hardness  <= 0.2F && hardness >= 0){
					BlockHelper.setBlockToAir(worldObj, entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]);
            		motionX *= 0.95;
            		motionY *= 0.95;
            		motionZ *= 0.95;
				}else{
					collidingBoxes.add(entry.getKey());
				}
			}
		}
		return collidingBoxes;
	}
	
	public void explodeAtPosition(double x, double y, double z){
		this.setDead();
		if(ConfigSystem.getBooleanConfig("PlaneExplosions")){
			worldObj.newExplosion(this, x, y, z, (float) (fuel/1000 + 1F), true, true);
		}
	}
	
	public void updateHeadingVec(){
        double f1 = Math.cos(-this.rotationYaw * 0.017453292F - (float)Math.PI);
        double f2 = Math.sin(-this.rotationYaw * 0.017453292F - (float)Math.PI);
        double f3 = -Math.cos(-this.rotationPitch * 0.017453292F);
        double f4 = Math.sin(-this.rotationPitch * 0.017453292F);
        headingVec.set((f2 * f3), f4, (f1 * f3));
   	}
	
	public static float calculateInventoryWeight(IInventory inventory){
		float weight = 0;
		for(int i=0; i<inventory.getSizeInventory(); ++i){
			ItemStack stack = inventory.getStackInSlot(i);
			if(stack != null){
				weight += 1.2F*stack.stackSize/stack.getMaxStackSize()*(ConfigSystem.getStringConfig("HeavyItems").contains(ItemStackHelper.getItemFromStack(stack).getUnlocalizedName().substring(5)) ? 2 : 1);
			}
		}
		return weight;
	}
	
	/**
	 * Handles engine packets.
	 * 0 is magneto off.
	 * 1 is magneto on.
	 * 2 is electric starter off.
	 * 3 is electric starter on.
	 * 4 is hand starter on.
	 */
	public void handleEngineSignal(EntityEngine engine, byte signal){
		switch (signal){
			case 0: engine.setMagnetoStatus(false); break;
			case 1: engine.setMagnetoStatus(true); break;
			case 2: engine.setElectricStarterStatus(false); break;
			case 3: engine.setElectricStarterStatus(true); break;
			case 4: engine.handStartEngine(); break;
			case 5: engine.backfireEngine(); break;
		}
	}
	
	/**
	 * Gets the number of bays available for engines.
	 * Cached for efficiency.
	 */
	public byte getNumberEngineBays(){
		if(numberEngineBays == 0){
			for(PartData data : this.partData){
				for(Class<? extends EntityChild> aClass : data.acceptableClasses){
					if(EntityEngine.class.isAssignableFrom(aClass)){
						++numberEngineBays;
					}
				}
			}
		}
		return numberEngineBays;
	}
	
	/**
	 * Gets the 'numbered' engine.
	 * Cached for efficiency.
	 */
	public EntityEngine getEngineByNumber(byte number){
		if(engineByNumber.containsKey(number)){
			if(!engineByNumber.get(number).isDead){
				return engineByNumber.get(number);
			}else{
				engineByNumber.remove(number);
			}
		}
		
		//Because this is a list, the #0 engine will always come before the #1 engine.
		//We use array notation here to keep with Java standards.
		byte engineNumber = 0;
		for(PartData data : this.partData){
			for(Class<? extends EntityChild> aClass : data.acceptableClasses){
				if(EntityEngine.class.isAssignableFrom(aClass)){
					for(EntityChild child : this.getChildren()){
						if(child instanceof EntityEngine){
							if(child.offsetX == data.offsetX && child.offsetY == data.offsetY && child.offsetZ == data.offsetZ){
								engineByNumber.put(engineNumber, (EntityEngine) child);
							}
						}
					}
					++engineNumber;
				}
			}
		}
		return engineByNumber.get(number);
	}
	
	public float getLightBrightness(byte lightBank){
		return (lightBank & this.lightStatus) != 0 ? (electricPower > 2 ? (float) electricPower/12F : 0) : 0;
	}
	
	public abstract ResourceLocation getBackplateTexture();
	public abstract ResourceLocation getMouldingTexture();
	public abstract void drawHUD(int width, int height);
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.brakeOn=tagCompound.getBoolean("brakeOn");
		this.parkingBrakeOn=tagCompound.getBoolean("parkingBrakeOn");
		this.openTop=tagCompound.getBoolean("openTop");
		this.lightStatus=tagCompound.getByte("lightStatus");
		this.textureOptions=tagCompound.getByte("textureOptions");
		this.throttle=tagCompound.getByte("throttle");
		this.fuel=tagCompound.getDouble("fuel");
		this.electricPower=tagCompound.getDouble("electricPower");
		this.ownerName=tagCompound.getString("ownerName");
		this.displayName=tagCompound.getString("displayName");
		
		byte[] instrumentSlots = tagCompound.getByteArray("instrumentSlots");
		byte[] instrumentTypes = tagCompound.getByteArray("instrumentTypes");
		for(byte i = 0; i<instrumentSlots.length; ++i){
			instruments.put(instrumentSlots[i], instrumentTypes[i]);
		}
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("brakeOn", this.brakeOn);
		tagCompound.setBoolean("parkingBrakeOn", this.parkingBrakeOn);
		tagCompound.setBoolean("openTop", this.openTop);
		tagCompound.setByte("lightStatus", this.lightStatus);
		tagCompound.setByte("textureOptions", this.textureOptions);
		tagCompound.setByte("throttle", this.throttle);
		tagCompound.setDouble("fuel", this.fuel);
		tagCompound.setDouble("electricPower", this.electricPower);
		tagCompound.setString("ownerName", this.ownerName);
		tagCompound.setString("displayName", this.displayName);
		
		byte[] instrumentSlots = new byte[instruments.size()];
		byte[] instrumentTypes = new byte[instruments.size()];
		byte i = 0;
		for(Entry<Byte, Byte> instrument : instruments.entrySet()){
			instrumentSlots[i] = instrument.getKey();
			instrumentTypes[i] =  instrument.getValue();
			++i;
		}
		tagCompound.setByteArray("instrumentSlots", instrumentSlots);
		tagCompound.setByteArray("instrumentTypes", instrumentTypes);
	}
}
