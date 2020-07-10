package mcinterface;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.OpMode;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.OpState;
import minecrafttransportsimulator.packets.instances.PacketTileEntitySignalControllerControlled;
import net.minecraftforge.fml.common.Optional;

/**Builder Tile Entity for {@link TileEntitySignalController}.  Allows for interfacing with OpenComputers.
 *
 * @author don_bruce
 */
@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "opencomputers")
public class BuilderTileEntitySignalController extends BuilderTileEntity.Tickable<TileEntitySignalController> implements SimpleComponent{
	
	public BuilderTileEntitySignalController(){
		//Blank constructor for MC.  We set the TE variable in NBT instead.
	}
	
	BuilderTileEntitySignalController(TileEntitySignalController tileEntity){
		super(tileEntity);
	}
	
	@Override
	public String getComponentName(){
		return "iv_signalcntlr"; // INFO: Max length is 14 chars
	}
	
	@Callback(doc = "function(boolean):nil; Returns true if primary axis is X.", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] mainDirectionXAxis(Context context, Arguments args){
		return new Object[]{tileEntity.mainDirectionXAxis};
	}
	
	@Callback(doc = "function(int):int; Returns signal mode.  Int arg sets mode. (0 - TIMED_CYCLE, 1 - VEHICLE_TRIGGER, 2 - REMOTE_CONTROL)", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] mode(Context context, Arguments args){
		if(args.count() != 0 && args.isInteger(0)){
			tileEntity.currentOpMode = OpMode.values()[args.checkInteger(0)];
			InterfaceNetwork.sendToAllClients(new PacketTileEntitySignalControllerControlled(tileEntity));
		}
		return new Object[]{tileEntity.currentOpMode.ordinal()};
	}
	
	@Callback(doc = "function(int):int; Returns signal state.  Int arg sets state. (0 - GREEN_MAIN_RED_CROSS, 1 - YELLOW_MAIN_RED_CROSS, 2 - RED_MAIN_RED_CROSS, 3 - RED_MAIN_GREEN_CROSS, 4 - RED_MAIN_YELLOW_CROSS)", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] signalState(Context context, Arguments args){
		if(args.count() != 0 && args.isInteger(0)){
			tileEntity.updateState(OpState.values()[args.checkInteger(0)], false);
			InterfaceNetwork.sendToAllClients(new PacketTileEntitySignalControllerControlled(tileEntity));
		}
		return new Object[]{tileEntity.currentOpState.ordinal()};
	}
	
	@Callback(doc = "function(boolean):boolean; Returns street light state.  Boolean arg sets state.", direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] streetLightState(Context context, Arguments args){
		if(args.count() != 0 && args.isBoolean(0)){
			tileEntity.lightsOn = args.checkBoolean(0);
			tileEntity.updateState(tileEntity.currentOpState, false);
			InterfaceNetwork.sendToAllClients(new PacketTileEntitySignalControllerControlled(tileEntity));
		}
		return new Object[]{tileEntity.lightsOn};
	}
}
