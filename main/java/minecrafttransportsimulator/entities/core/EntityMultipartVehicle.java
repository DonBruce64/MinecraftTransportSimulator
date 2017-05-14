package minecrafttransportsimulator.entities.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.parts.EntityEngine;
import minecrafttransportsimulator.systems.pack.PackInstrument;
import minecrafttransportsimulator.systems.pack.PackObject;
import minecrafttransportsimulator.systems.pack.PackParserSystem;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**This class is tailored for moving vehicles such as planes, trains, and automobiles.
 * Contains numerous methods for gauges, HUDs, and fuel systems.
 * Essentially, if it has parts and an engine, use this.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartVehicle extends EntityMultipartMoving{

	public byte numberPowerfulLights;
	public byte throttle;
	public int lightSetup;
	public int lightStatus;
	public int fuelCapacity;
	public int emptyMass;
	public double fuel;
	public double electricPower = 12;
	public double electricUsage;
	public double electricFlow;
	public double airDensity;
	public double trackAngle;

	private ResourceLocation backplateTexture;
	private ResourceLocation mouldingTexture;
	
	public final Map<Byte, Instrument> instruments = new HashMap<Byte, Instrument>();;
	
	private byte numberEngineBays = 0;
	private final Map<Byte, EntityEngine> engineByNumber = new HashMap<Byte, EntityEngine>();
	
	public EntityMultipartVehicle(World world){
		super(world);
	}
	
	public EntityMultipartVehicle(World world, float posX, float posY, float posZ, float playerRotation, String name){
		super(world, posX, posY, posZ, playerRotation, name);
		PackObject pack = PackParserSystem.getPack(name);
		this.backplateTexture = pack.rendering.useCustomBackplateTexture ? new ResourceLocation(MTS.MODID, pack.rendering.backplateTexture) : new ResourceLocation(pack.rendering.backplateTexture);
		this.mouldingTexture = pack.rendering.useCustomMouldingTexture ? new ResourceLocation(MTS.MODID, pack.rendering.customMouldingTexture) : new ResourceLocation(pack.rendering.customMouldingTexture);
	}
	
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
			for(Instrument instrument : instruments.values()){
				if(instrument.currentInstrument != 0){
					ItemStack stack = new ItemStack(MTSRegistry.flightInstrument, 1, instrument.currentInstrument);
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
	 * 5 is a backfire from a high-hour engine.
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
				for(String name : data.validNames){
					if(name.contains("engine")){
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
			for(String name : data.validNames){
				if(name.contains("engine")){
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
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.throttle=tagCompound.getByte("throttle");
		this.lightStatus=tagCompound.getInteger("lightStatus");
		this.fuel=tagCompound.getDouble("fuel");
		this.electricPower=tagCompound.getDouble("electricPower");

		PackObject pack = PackParserSystem.getPack(name);
		if(pack != null){
			this.lightSetup = Integer.parseInt(pack.motorized.lightSetup);
			this.numberPowerfulLights = (byte) pack.motorized.numberPowerfulLights;
			this.fuelCapacity = pack.motorized.fuelCapacity;
			this.emptyMass = pack.motorized.emptyMass;

			for (byte i = 0; i < pack.motorized.instruments.size(); i++) {
				PackInstrument instrument = pack.motorized.instruments.get(i);
				instruments.put(i, new Instrument(instrument.pos[0], instrument.pos[1], instrument.pos[2], instrument.rot[0], instrument.rot[1], instrument.rot[2], (byte) instrument.defaultInstrument));
			}

			byte[] instrumentSlots = tagCompound.getByteArray("instrumentSlots");
			byte[] instrumentTypes = tagCompound.getByteArray("instrumentTypes");
			for(byte i = 0; i<instrumentSlots.length; ++i){
				instruments.get(instrumentSlots[i]).currentInstrument = instrumentTypes[i];
			}
		}
	}

	public ResourceLocation getBackplateTexture(){
		return backplateTexture;
	}

	public ResourceLocation getMouldingTexture(){
		return mouldingTexture;
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);		
		tagCompound.setByte("throttle", this.throttle);
		tagCompound.setInteger("lightStatus", this.lightStatus);
		tagCompound.setDouble("fuel", this.fuel);
		tagCompound.setDouble("electricPower", this.electricPower);
		
		byte[] instrumentSlots = new byte[instruments.size()];
		byte[] instrumentTypes = new byte[instruments.size()];
		byte i = 0;
		for(Entry<Byte, Instrument> instrument : instruments.entrySet()){
			instrumentSlots[i] = instrument.getKey();
			instrumentTypes[i] =  instrument.getValue().currentInstrument;
			++i;
		}
		tagCompound.setByteArray("instrumentSlots", instrumentSlots);
		tagCompound.setByteArray("instrumentTypes", instrumentTypes);
		return tagCompound;
	}
	
	public static final class Instrument{
		public final float xPos;
		public final float yPos;
		public final float zPos;
		public final float xRot;
		public final float yRot;
		public final float zRot;
		public byte currentInstrument;
		
		Instrument(float xPos, float yPos, float zPos, float xRot, float yRot, float zRot, byte defaultInstrument){
			this.xPos = xPos;
			this.yPos = yPos;
			this.zPos = zPos;
			this.xRot = xRot;
			this.yRot = yRot;
			this.zRot = zRot;
			this.currentInstrument = defaultInstrument;
		}
	}
}
