package minecrafttransportsimulator;

import java.io.File;

import org.apache.logging.log4j.Logger;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

@Mod(modid = MTS.MODID, name = MTS.MODNAME, version = MTS.MODVER)
public class MTS {
	public static final String MODID="mts";
	public static final String MODNAME="Minecraft Transport Simulator";
	public static final String MODVER="15.4.0";
	
	@Instance(value = MTS.MODID)
	public static MTS instance;
	public static Logger MTSLog;
	public static File MTSDevDir;
	public static final SimpleNetworkWrapper MTSNet = NetworkRegistry.INSTANCE.newSimpleChannel("MTSNet");
	@SidedProxy(clientSide="minecrafttransportsimulator.ClientProxy", serverSide="minecrafttransportsimulator.CommonProxy")
	public static CommonProxy proxy;
	
	public MTS(){
		FluidRegistry.enableUniversalBucket();
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent event){
		MTSLog = event.getModLog();
		PackParserSystem.outputLog();
		proxy.initConfig(event.getSuggestedConfigurationFile());
		proxy.initControls();
		MTSDevDir = new File(event.getModConfigurationDirectory().getParent() + File.separator + "mts");
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event){
		MTSRegistry.init();
		ConfigSystem.initFuels();
	}
}
