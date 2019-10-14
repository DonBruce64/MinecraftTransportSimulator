package minecrafttransportsimulator.packets.parts;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.packets.vehicles.APacketVehiclePart;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.nbt.NBTTagCompound;

/**This packet is sent to clients when an engine is damaged.
 * Allows the clients to get the hours that were added, as well
 * as any leaks or breakages that the engine incurred from the
 * damage.
 * 
 * @author don_bruce
 */
public class PacketPartClientEngineDamage extends APacketVehiclePart{
	private float hours;
	private boolean oilLeak;
	private boolean fuelLeak;
	private boolean brokenStarter;

	public PacketPartClientEngineDamage(){}
	
	public PacketPartClientEngineDamage(APartEngine engine, float hours){
		super(engine);
		this.hours = hours;
		this.oilLeak = engine.oilLeak;
		this.fuelLeak = engine.fuelLeak;
		this.brokenStarter = engine.brokenStarter;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		hours = tag.getFloat("hours");
		oilLeak = tag.getBoolean("oilLeak");
		fuelLeak = tag.getBoolean("fuelLeak");
		brokenStarter = tag.getBoolean("brokenStarter");
	}
	
	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setFloat("hours", hours);
		tag.setBoolean("oilLeak", oilLeak);
		tag.setBoolean("fuelLeak", fuelLeak);
		tag.setBoolean("brokenStarter", brokenStarter);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		APartEngine engine = (APartEngine) getPart(world);
		engine.hours += hours;
		if(!engine.fuelLeak){
			engine.fuelLeak = fuelLeak;
		}
		if(!engine.oilLeak){
			engine.oilLeak = oilLeak;
		}
		if(!engine.brokenStarter){
			engine.brokenStarter = brokenStarter;
		}
	}
}
