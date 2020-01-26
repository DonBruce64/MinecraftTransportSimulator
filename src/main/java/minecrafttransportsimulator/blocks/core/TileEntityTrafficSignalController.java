package minecrafttransportsimulator.blocks.core;

import java.awt.*;
import java.awt.image.ColorModel;
import java.util.*;
import java.util.List;

import jdk.nashorn.internal.ir.Block;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.tileentities.PacketTrafficSignalControllerChange;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Car;
import minecrafttransportsimulator.wrappers.CrossingSignalData;
import minecrafttransportsimulator.wrappers.TrafficSignalData;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Optional;

@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "opencomputers")
public class TileEntityTrafficSignalController extends TileEntityBase implements ITickable, SimpleComponent {
	public boolean orientedOnX = false;
	/** 0 - Disabled, 1 - Manual, 2 - Time Delay, 3 - Vehicle Trigger **/
	public byte mode = 1;
	public int greenMainTime = 20;
	public int greenCrossTime = 10;
	public int yellowTime = 2;
	public int allRedTime = 1;
	//public final List<BlockPos> trafficSignalLocations = new ArrayList<BlockPos>();
	//public final List<BlockPos> crossingSignalLocations = new ArrayList<BlockPos>();
	public final Map<BlockPos, TrafficSignalData> trafficSignals = new HashMap<>();
	public final Map<BlockPos, CrossingSignalData> crossingSignals = new HashMap<>();

	public byte operationIndex;
	public long timeOperationStarted;

	@Override
	public void update(){
		long worldTime = world.getTotalWorldTime();
		if (mode > 1) switch (operationIndex) {
			case 0: {
				//First step, main light turns green, cross light stays red.
				//If we are a signal-controlled system, we stay here until we get a signal.
				//If we are a timed system, we immediately move to state 1 as our
				//green time is an extra state to enable looping.
				if (mode == 3) {
					//Only check every two seconds to prevent lag.
					if (worldTime % 40 == 0) {
						//Get a bounding box for all lights in the controller system.
						int minX = Integer.MAX_VALUE;
						int maxX = Integer.MIN_VALUE;
						int minZ = Integer.MAX_VALUE;
						int maxZ = Integer.MIN_VALUE;
						for (BlockPos controllerSignalPos : trafficSignals.keySet()) {
							minX = Math.min(minX, controllerSignalPos.getX());
							maxX = Math.max(maxX, controllerSignalPos.getX());
							minZ = Math.min(minX, controllerSignalPos.getZ());
							maxZ = Math.max(maxX, controllerSignalPos.getZ());
						}

						//Take 10 off to expand the detection boxes for the axis.
						if (orientedOnX) {
							minZ -= 10;
							maxZ += 10;
						} else {
							minX -= 10;
							maxX += 10;
						}

						//Now we have min-max, check for any vehicles in the area.
						//We need to check along the non-primary axis.
						for (Entity entity : world.loadedEntityList) {
							if (entity instanceof EntityVehicleG_Car) {
								if (orientedOnX) {
									if ((entity.posZ > minZ && entity.posZ < minZ + (maxZ - minZ) / 2F) || (entity.posZ < maxZ && entity.posZ > maxZ - (maxZ - minZ) / 2F)) {
										if (entity.posX > minX && entity.posX < maxX) {
											timeOperationStarted = worldTime;
											operationIndex = 1;
										}
									}
								} else {
									if ((entity.posX > minX && entity.posX < minX + (maxX - minX) / 2F) || (entity.posX < maxX && entity.posX > maxX - (maxX - minX) / 2F)) {
										if (entity.posZ > minZ && entity.posZ < maxZ) {
											timeOperationStarted = worldTime;
											operationIndex = 1;
										}
									}
								}
							}
						}
					}
				} else {
					timeOperationStarted = worldTime;
					operationIndex = 1;
				}
				break;
			}
			case 1: {
				//Second step, main light turns yellow, cross light stays red.
				if (timeOperationStarted + yellowTime * 20 <= worldTime) {
					timeOperationStarted = worldTime;
					operationIndex = 2;
				}
				break;
			}
			case 2: {
				//Third step, main light turns red, cross light stays red.
				if (timeOperationStarted + allRedTime * 20 <= worldTime) {
					timeOperationStarted = worldTime;
					operationIndex = 3;
				}
				break;
			}
			case 3: {
				//Fourth step, main light stays red, cross light turns green.
				if (timeOperationStarted + greenCrossTime * 20 <= worldTime) {
					timeOperationStarted = worldTime;
					operationIndex = 4;
				}
				break;
			}
			case 4: {
				//Fifth step, main light stays red, cross light turns yellow.
				if (timeOperationStarted + yellowTime * 20 <= worldTime) {
					timeOperationStarted = worldTime;
					operationIndex = 5;
				}
				break;
			}
			case 5: {
				//Sixth step, main light stays red, cross light turns red.
				if (timeOperationStarted + allRedTime * 20 <= worldTime) {
					timeOperationStarted = worldTime;
					//If we are a triggered light, we go back to 0.
					//Otherwise, we perform another cycle and go back to 1.
					operationIndex = (byte) ((mode == 3) ? 0 : 6);
				}
				break;
			}
			default: {
				//Seventh step, main light turns green, cross light stays red.
				if (timeOperationStarted + greenMainTime * 20 <= worldTime) {
					timeOperationStarted = worldTime;
					operationIndex = 0;
				}
				break;
			}
		}
	}

	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        this.orientedOnX = tagCompound.getBoolean("orientedOnX");
        this.mode = tagCompound.getByte("mode");
        this.greenMainTime = tagCompound.getInteger("greenMainTime");
        this.greenCrossTime = tagCompound.getInteger("greenCrossTime");
        this.yellowTime = tagCompound.getInteger("yellowTime");
        this.allRedTime = tagCompound.getInteger("allRedTime");

        trafficSignals.clear();
        for(byte i=0; i<tagCompound.getInteger("trafficSignalCount"); ++i){
        	NBTTagCompound trafficSignalData = (NBTTagCompound) tagCompound.getTag("trafficSignal" + i);
        	int[] posArray = trafficSignalData.getIntArray("location");
        	BlockPos blockPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
        	Color color = Color.decode(trafficSignalData.getString("color"));
        	trafficSignals.put(blockPos, new TrafficSignalData(blockPos, trafficSignalData.getBoolean("isEnabled"), color, trafficSignalData.getBoolean("shouldFlash")));
        }
    }

	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("orientedOnX", this.orientedOnX);
        tagCompound.setInteger("mode", this.mode);
        tagCompound.setInteger("greenMainTime", this.greenMainTime);
        tagCompound.setInteger("greenCrossTime", this.greenCrossTime);
        tagCompound.setInteger("yellowTime", this.yellowTime);
        tagCompound.setInteger("allRedTime", this.allRedTime);

        //Save all pos data to NBT.
        //for(byte i=0; i<trafficSignals.size(); ++i){
		int i = 0;
		for(TrafficSignalData trafficSignal : trafficSignals.values()) {
			NBTTagCompound trafficSignalData = new NBTTagCompound();
			trafficSignalData.setIntArray("location", new int[]{trafficSignal.getBlockPos().getX(), trafficSignal.getBlockPos().getY(), trafficSignal.getBlockPos().getZ()});
			trafficSignalData.setBoolean("isEnabled", trafficSignal.isEnabled());
			trafficSignalData.setString("color", "#" + Integer.toHexString(trafficSignal.getColor().getRGB()).substring(2));
			trafficSignalData.setBoolean("shouldFlash", trafficSignal.isShouldFlash());
			tagCompound.setTag("trafficSignal" + i, trafficSignalData);
	    	i++;
        }
        tagCompound.setInteger("trafficSignalCount", trafficSignals.size());
        return tagCompound;
    }

	//region OpenComputers implementation
	@Override
	public String getComponentName() {
		return "iv_signalcntlr"; // INFO: Max length is 14 chars
	}

	/* Getter */

	@Callback(doc = "function():boolean; Returns if the primary axis is X", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] isPrimaryAxisX(Context context, Arguments args) {
		return new Object[] { orientedOnX };
	}

	@Callback(doc = "function():int; Returns what mode is currently set. (0 - DISABLED, 1 - MANUAL, 2 - TIMED, 3 - VEHICLE TRIGGER)", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getMode(Context context, Arguments args) {
		return new Object[] { (int) mode };
	}

	@Callback(doc = "function():string; Returns the name of what mode it's currently set.", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getModeName(Context context, Arguments args) {
		return new Object[] { mode == 1 ? "MANUAL": mode == 2 ? "TIMED": mode == 3 ? "VEHICLE_TRIGGER" : "DISABLED" };
	}

	@Callback(doc = "function():int; Returns how long is main signal in green (in ticks)", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getGreenMainTime(Context context, Arguments args) {
		return new Object[] { greenMainTime };
	}

	@Callback(doc = "function():int; Returns how long is cross signal in green (in ticks)", direct = true)
	public Object[] getGreenCrossTime(Context context, Arguments args) {
		return new Object[] { greenCrossTime };
	}

	@Callback(doc = "function():int; Returns how long are all signals yellow (in ticks)", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getYellowTime(Context context, Arguments args) {
		return new Object[] { yellowTime };
	}

	@Callback(doc = "function():int; Returns how long are all signals red (in ticks)", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getAllRedTime(Context context, Arguments args) {
		return new Object[] { allRedTime };
	}

	/* Setters */

	@Callback(doc = "function(boolean):boolean; This will save all changes to the Traffic Signal Controller. You need to do this when you want to save all changes !", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] confirmChanges(Context context, Arguments args) {
		MTS.MTSNet.sendToServer(new PacketTrafficSignalControllerChange(this));
		return new Object[] { true };
	}

	@Callback(doc = "function(boolean):boolean; Set if the primary axis is X. Returns true on success", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] setPrimaryAxisX(Context context, Arguments args) {
		orientedOnX = args.isBoolean(0) && args.checkBoolean(0);
		return new Object[] { orientedOnX };
	}

	@Callback(doc = "function(int):boolean; Set signal mode. (0 - DISABLED, 1 - MANUAL, 2 - TIMED, 3 - VEHICLE TRIGGER)", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] setMode(Context context, Arguments args) {
		return new Object[] { args.isInteger(0) ? mode = (byte) args.checkInteger(0) : false };
	}

	@Callback(doc = "function(int):boolean; Set the time main signal is Green. Returns true on success", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] setGreenMainTime(Context context, Arguments args) {
		return new Object[] { args.isInteger(0) ? greenMainTime = args.checkInteger(0) : false };
	}

	@Callback(doc = "function(int):boolean; Set the time cross signal is Green. Returns true on success", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] setGreenCrossTime(Context context, Arguments args) {
		return new Object[] { args.isInteger(0) ? greenCrossTime = args.checkInteger(0) : false };
	}

	@Callback(doc = "function(int):boolean; Set the time all signals are Yellow. Returns true on success", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] setYellowTime(Context context, Arguments args) {
		return new Object[] { args.isInteger(0) ? yellowTime = args.checkInteger(0) : false };
	}

	@Callback(doc = "function(int):boolean; Set the time all signals are Red. Returns true on success", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] setAllRedTime(Context context, Arguments args) {
		return new Object[] { args.isInteger(0) ? allRedTime = args.checkInteger(0) : false };
	}

	// Manual mode
	private boolean isValidTrafficSignalColor(String color) {
		return color.equalsIgnoreCase("red") || color.equalsIgnoreCase("yellow") || color.equalsIgnoreCase("green");
	}
	private boolean isValidBlockPosition(Map table) {
		return table.containsKey("x") && table.get("x") instanceof Double && table.containsKey("y") && table.get("y") instanceof Double && table.containsKey("z") && table.get("z") instanceof Double;
	}
	private Map createBlockPosTable(BlockPos blockPos) {
		Map<String, Integer> coords = new HashMap<>();
		coords.put("x", blockPos.getX());
		coords.put("y", blockPos.getY());
		coords.put("z", blockPos.getZ());
		return coords;
	}
	private BlockPos createBlockPos(Map table) {
		return isValidBlockPosition(table) ? new BlockPos((Double) table.get("x"),(Double) table.get("y"),(Double) table.get("z")) : null;
	}

	@Callback(doc = "function():int; ", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getTrafficSignalAmount(Context context, Arguments args) {
		return new Object[] { trafficSignals.size() - trafficSignals.size() };
	}

	@Callback(doc = "function():int; ", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getCrossingSignalAmount(Context context, Arguments args) {
		return new Object[] { crossingSignals.size() };
	}

	@Callback(doc = "function(int, string):boolean; ", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] setTrafficSignalColor(Context context, Arguments args) {
		if (args.isString(0) && isValidTrafficSignalColor(args.checkString(0)) && args.isTable(1) && isValidBlockPosition(args.checkTable(1))) {
			if (trafficSignals.containsKey(createBlockPos(args.checkTable(1)))) {
				trafficSignals.replace(createBlockPos(args.checkTable(1)), trafficSignals.get(createBlockPos(args.checkTable(1))).setColor(args.checkString(0)));
				return new Object[]{ true };
			}
		}
		return new Object[] { false };
	}

	@Callback(doc = "function(int, string):boolean; ", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] setTrafficSignalFlashing(Context context, Arguments args) {
		if (args.isBoolean(0) && args.isTable(1) && isValidBlockPosition(args.checkTable(1))) {
			if (trafficSignals.containsKey(createBlockPos(args.checkTable(1)))) {
				trafficSignals.replace(createBlockPos(args.checkTable(1)), trafficSignals.get(createBlockPos(args.checkTable(1))).setShouldFlash(args.checkBoolean(0)));
				return new Object[]{ true };
			}
		}
		return new Object[] { false };
	}

	@Callback(doc = "function(int, string):boolean; ", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] setTrafficSignalEnabled(Context context, Arguments args) {
		if (args.isBoolean(0)&& args.isTable(1) && isValidBlockPosition(args.checkTable(1))) {
			if (trafficSignals.containsKey(createBlockPos(args.checkTable(1)))) {
				trafficSignals.replace(createBlockPos(args.checkTable(1)), trafficSignals.get(createBlockPos(args.checkTable(1))).setEnabled(args.checkBoolean(0)));
				return new Object[]{ true };
			}
		}
		return new Object[] { false };
	}

	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] devCmd(Context context, Arguments args) {
		return new Object[] { createBlockPosTable(trafficSignals.entrySet().iterator().next().getKey()) };
	}
	//endregion
}
