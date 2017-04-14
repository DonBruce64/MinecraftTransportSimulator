package minecrafttransportsimulator.entities.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.parts.EntityEngine;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**This class is tailored for moving vehicles such as planes, trains, and automobiles.
 * Contains numerous methods for gauges, HUDs, and fuel systems.
 * Essentially, if it has parts and an engine, use this.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartVehicle extends EntityMultipartMoving{
	public byte lightSetup;
	public byte numberPowerfulLights;
	public byte lightStatus;
	public byte throttle;
	public int fuelCapacity;
	public float emptyMass;
	public double fuel;
	public double electricPower = 12;
	public double electricUsage;
	public double electricFlow;
	public double airDensity;
	public double trackAngle;
	
	/**Map of instrument slots to type.
	 * If there's not a key for a slot, it doesn't exist.
	 * Note that engines use slots 10-14, 20-24, 30-34, and 40-44.
	 **/
	public Map<Byte, Byte> instruments;
	
	private Map<AxisAlignedBB, Integer[]> collisionMap = new HashMap<AxisAlignedBB, Integer[]>();
	protected List<AxisAlignedBB> collidingBoxes = new ArrayList<AxisAlignedBB>();
	
	private byte numberEngineBays = 0;
	private Map<Byte, EntityEngine> engineByNumber = new HashMap<Byte, EntityEngine>();
	
	public EntityMultipartVehicle(World world){
		super(world);
	}
	
	public EntityMultipartVehicle(World world, float posX, float posY, float posZ, float playerRotation, byte textureOptions){
		super(world, posX, posY, posZ, playerRotation, textureOptions);
	}
	
	@Override
	protected void entityInit(){
		super.entityInit();
		initProperties();
		instruments = new HashMap<Byte, Byte>();
		initInstruments();
	}
	
	protected abstract void initProperties();
	protected abstract void initInstruments();
	
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

	@Override
	public void setDead(){
		if(!worldObj.isRemote){
			for(Byte instrumentNumber : instruments.values()){
				if(instrumentNumber != 0){
					ItemStack stack = new ItemStack(MTSRegistry.flightInstrument, 1, instrumentNumber);
					worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, stack));
				}
			}
		}
		super.setDead();
	}
	
	@Override
	protected float getExplosionStrength(){
		return (float) (fuel/1000 + 1F);
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
				for(Class<? extends EntityMultipartChild> aClass : data.acceptableClasses){
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
			for(Class<? extends EntityMultipartChild> aClass : data.acceptableClasses){
				if(EntityEngine.class.isAssignableFrom(aClass)){
					for(EntityMultipartChild child : this.getChildren()){
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
		this.lightStatus=tagCompound.getByte("lightStatus");
		this.throttle=tagCompound.getByte("throttle");
		this.fuel=tagCompound.getDouble("fuel");
		this.electricPower=tagCompound.getDouble("electricPower");
		
		byte[] instrumentSlots = tagCompound.getByteArray("instrumentSlots");
		byte[] instrumentTypes = tagCompound.getByteArray("instrumentTypes");
		for(byte i = 0; i<instrumentSlots.length; ++i){
			instruments.put(instrumentSlots[i], instrumentTypes[i]);
		}
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setByte("lightStatus", this.lightStatus);
		tagCompound.setByte("throttle", this.throttle);
		tagCompound.setDouble("fuel", this.fuel);
		tagCompound.setDouble("electricPower", this.electricPower);
		
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
