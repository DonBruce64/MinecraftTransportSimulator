package minecraftflightsimulator;

import java.util.HashMap;
import java.util.Map;

import minecraftflightsimulator.other.EntityController;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(modid = MFS.MODID, name = MFS.MODNAME, version = MFS.MODVER)
public class MFS {
	public static final String MODID="mfs";
	public static final String MODNAME="Minecraft Flight Simulator";
	public static final String MODVER="3.0.0";
	
	public static Configuration config;
	public static int controlSurfaceCooldown;
	public static double planeSpeedFactor;
	public static double fuelUsageFactor;
	public static String heavyItems;
	public static Map<String, Integer> fluidValues = new HashMap<String, Integer>();
	
	@Instance(value = MFS.MODID)
	public static MFS instance;
	public static final SimpleNetworkWrapper MFSNet = NetworkRegistry.INSTANCE.newSimpleChannel("MFSNet");
	@SidedProxy(clientSide="minecraftflightsimulator.ClientProxy", serverSide="minecraftflightsimulator.CommonProxy")
	public static CommonProxy proxy;
	public static final CreativeTabs tabMFS = new CreativeTabs("tabMFS") {
	    @Override
		@SideOnly(Side.CLIENT)
	    public Item getTabIconItem() {
	    	return MFS.proxy.planeMC172;
	    }
	};
	
	@EventHandler
	public void PreInit(FMLPreInitializationEvent event){
		config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();
		controlSurfaceCooldown = config.get(config.CATEGORY_GENERAL, "ControlSurfaceCooldown", 4, "How long (in ticks) it takes before control surfaces try to return to their natural angle.").getInt();
		planeSpeedFactor = config.get(config.CATEGORY_GENERAL, "PlaneSpeedFactor", 0.5F, "Factor to apply to plane movement.  1 is the realistic value, but this makes planes move too fast for Minecraft.  \nAdjust with caution.").getDouble();
		fuelUsageFactor = config.get(config.CATEGORY_GENERAL, "FuelUsageFactor", 1.0F, "Factor times which engines use fuel.  \nChange this if you think engines use fuel too fast or slow.").getDouble();
		heavyItems = config.get(config.CATEGORY_GENERAL, "HeavyItems", "diamond, iron, gold, coal, ore, stone", "Any item that contains these words will be counted as heavy (double mass) when considering plane mass.  \nChange and tweak to your liking.").getString();
		EntityController.initKeys();
		config.save();
	}
	
	@EventHandler
	public void Init(FMLInitializationEvent event){
		proxy.init();
	}
}

