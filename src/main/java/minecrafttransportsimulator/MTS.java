package minecrafttransportsimulator;

import java.io.File;
import java.io.InputStreamReader;

import org.apache.logging.log4j.Logger;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.wrappers.WrapperNetwork;
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
	public static final String MODVER="18.1.4";
	
	@Instance(value = MTS.MODID)
	public static MTS instance;
	public static Logger MTSLog;
	public static File minecraftDir;
	public static final SimpleNetworkWrapper MTSNet = NetworkRegistry.INSTANCE.newSimpleChannel("MTSNet");
	@SidedProxy(clientSide="minecrafttransportsimulator.ClientProxy", serverSide="minecrafttransportsimulator.CommonProxy")
	public static CommonProxy proxy;
	
	static{
		//Enable universal bucket so we can use buckets on fuel pumps.
		FluidRegistry.enableUniversalBucket();
		
		//Manually create the internal core mod pack items.
		//These need to be created before we do checks for block registration.
		//If we don't, then we risk not creating and registering the blocks.
		try{
			PackParserSystem.addBookletDefinition(new InputStreamReader(MTSRegistry.class.getResourceAsStream("/assets/" + MTS.MODID + "/jsondefs/booklets/handbook_en.json"), "UTF-8"), "handbook_en", MTS.MODID);
			PackParserSystem.addBookletDefinition(new InputStreamReader(MTSRegistry.class.getResourceAsStream("/assets/" + MTS.MODID + "/jsondefs/booklets/handbook_ru.json"), "UTF-8"), "handbook_ru", MTS.MODID);
			PackParserSystem.addDecorDefinition(new InputStreamReader(MTSRegistry.class.getResourceAsStream("/assets/" + MTS.MODID + "/jsondefs/decors/fuelpump.json"), "UTF-8"), "fuelpump", MTS.MODID);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent event){
		MTSLog = event.getModLog();
		for(String logEntry : PackParserSystem.logEntries){
			MTSLog.error(logEntry);
		}
		ConfigSystem.loadFromDisk(new File(event.getSuggestedConfigurationFile().getParent(), "mtsconfig.json"));
		minecraftDir = new File(event.getModConfigurationDirectory().getParent());
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event){
		MTSRegistry.init();
		WrapperNetwork.init();
	}
}
