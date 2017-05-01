package minecrafttransportsimulator;

import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

import java.io.File;

@Mod(modid = MTS.MODID, name = MTS.MODNAME, version = MTS.MODVER)
public class MTS {
	public static final String MODID="mts";
	public static final String MODNAME="Minecraft Transport Simulator";
	public static final String MODVER="8.0.0-ALPHA-9";
	public static final String assetDir = System.getProperty("user.dir") + File.separator + MTS.MODID;
	
	@Instance(value = MTS.MODID)
	public static MTS instance;
	public static final SimpleNetworkWrapper MFSNet = NetworkRegistry.INSTANCE.newSimpleChannel("MTSNet");
	@SidedProxy(clientSide="minecrafttransportsimulator.ClientProxy", serverSide="minecrafttransportsimulator.CommonProxy")
	public static CommonProxy proxy;
	
	public MTS(){
		FluidRegistry.enableUniversalBucket();
	}

	@EventHandler
	public void PreInit(FMLPreInitializationEvent event){
		proxy.preInit(event);
		this.initModMetadata(event);
	}
	
	@EventHandler
	public void Init(FMLInitializationEvent event){
		proxy.init(event);
	}
	
	private void initModMetadata(FMLPreInitializationEvent event){
        ModMetadata meta = event.getModMetadata();
        meta.name = "Minecraft Transport Simulator";
        meta.description = "Realistic transport for Minecraft!";
        meta.authorList.clear();
        meta.authorList.add("don_bruce & CO.");
        meta.logoFile = "Vingette.png";
        meta.url = "http://minecraft.curseforge.com/projects/minecraft-transport-simulator";
        meta.modId = this.MODID;
        meta.version = this.MODVER;
	}
}

