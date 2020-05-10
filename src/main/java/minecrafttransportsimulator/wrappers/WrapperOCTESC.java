package minecrafttransportsimulator.wrappers;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.OpMode;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.OpState;
import net.minecraftforge.fml.common.Optional;

/**Wrapper for interfacing the {@link TileEntitySignalController} with OpenComputers.
 * This provides hooks to allow the TESC to be controlled via OC systems.
 *
 * @author don_bruce
 */
@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "opencomputers")
public interface WrapperOCTESC extends SimpleComponent{
	
	public TileEntitySignalController getController();

	@Override
	public default String getComponentName(){
		return "iv_signalcntlr"; // INFO: Max length is 14 chars
	}
	
	@Callback(doc = "function(boolean):boolean; Returns true if primary axis is X.  Boolean arg sets state.", direct = true)
	@Optional.Method(modid = "opencomputers")
	public default Object[] mainDirectionXAxis(Context context, Arguments args){
		if(args.count() != 0 && args.isBoolean(0)){
			getController().mainDirectionXAxis = args.checkBoolean(0);
		}
		return new Object[]{getController().mainDirectionXAxis};
	}
	
	@Callback(doc = "function(int):int; Returns signal mode.  Int arg sets mode. (0 - TIMED_CYCLE, 1 - VEHICLE_TRIGGER, 2 - REMOTE_CONTROL)", direct = true)
	@Optional.Method(modid = "opencomputers")
	public default Object[] mode(Context context, Arguments args){
		if(args.count() != 0 && args.isInteger(0)){
			getController().currentOpMode = OpMode.values()[args.checkInteger(0)];
		}
		return new Object[]{getController().currentOpMode.ordinal()};
	}
	
	@Callback(doc = "function(int):int; Returns signal state.  Int arg sets state. (0 - GREEN_MAIN_RED_CROSS, 1 - YELLOW_MAIN_RED_CROSS, 2 - RED_MAIN_RED_CROSS, 3 - RED_MAIN_GREEN_CROSS, 4 - RED_MAIN_YELLOW_CROSS)", direct = true)
	@Optional.Method(modid = "opencomputers")
	public default Object[] signalState(Context context, Arguments args){
		if(args.count() != 0 && args.isInteger(0)){
			getController().updateState(OpState.values()[args.checkInteger(0)], false);
		}
		return new Object[]{getController().currentOpState.ordinal()};
	}
	
	@Callback(doc = "function(boolean):boolean; Returns street light state.  Boolean arg sets state.", direct = true)
	@Optional.Method(modid = "opencomputers")
	public default Object[] streetLightState(Context context, Arguments args){
		if(args.count() != 0 && args.isBoolean(0)){
			getController().lightsOn = args.checkBoolean(0);
			getController().updateState(getController().currentOpState, false);
		}
		return new Object[]{getController().lightsOn};
	}
}
