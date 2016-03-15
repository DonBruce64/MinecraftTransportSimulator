package minecraftflightsimulator;

import minecraftflightsimulator.other.EntityController;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;

import org.lwjgl.input.Keyboard;

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
	public static float planeSpeedFactor;
	public static float fuelUsageFactor;
	public static String heavyItems;
	
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
		planeSpeedFactor = config.getFloat("PlaneSpeedFactor", config.CATEGORY_GENERAL, 0.5F, 0.1F, 5F, "Factor to apply to plane movement.  1 is the realistic value, but this makes planes move too fast for Minecraft.  \nAdjust with caution.");
		fuelUsageFactor = config.getFloat("FuelUsageFactor", config.CATEGORY_GENERAL, 1.0F, 0F, 100F, "Factor times which engines use fuel.  \nChange this if you think engines use fuel too fast or slow.");
		heavyItems = config.getString("HeavyItems", config.CATEGORY_GENERAL, "diamond, iron, gold, coal, ore, stone", "Any item that contains these words will be counted as heavy (double mass) when considering plane mass.  Change and tweak to your liking.");
		
		EntityController.modKey = config.get("controls", "ModKey", Keyboard.KEY_RSHIFT).getInt();
		EntityController.camLockKey = config.get("controls", "CamLockKey", Keyboard.KEY_RCONTROL).getInt();
		EntityController.pitchUpKey = config.get("controls", "PitchUpKey", Keyboard.KEY_S).getInt();
		EntityController.pitchDownKey = config.get("controls", "PitchDownKey", Keyboard.KEY_W).getInt();
		EntityController.rollLeftKey = config.get("controls", "RollLeftKey", Keyboard.KEY_A).getInt();
		EntityController.rollRightKey = config.get("controls", "RollRightKey", Keyboard.KEY_D).getInt();
		EntityController.rollRightKey = config.get("controls", "RollRightKey", Keyboard.KEY_D).getInt();
		EntityController.throttleUpKey = config.get("controls", "ThrottleUpKey", Keyboard.KEY_I).getInt();
		EntityController.throttleDownKey = config.get("controls", "ThrottleDownKey", Keyboard.KEY_K).getInt();
		EntityController.yawLeftKey = config.get("controls", "YawLeftKey", Keyboard.KEY_J).getInt();
		EntityController.yawRightKey = config.get("controls", "YawRightKey", Keyboard.KEY_L).getInt();
		EntityController.flapsUpKey = config.get("controls", "FlapsUpKey", Keyboard.KEY_Y).getInt();
		EntityController.flapsDownKey = config.get("controls", "FlapsDownKey", Keyboard.KEY_H).getInt();
		EntityController.brakeKey = config.get("controls", "BrakeKey", Keyboard.KEY_B).getInt();
		EntityController.starterKey = config.get("controls", "StarterKey", Keyboard.KEY_M).getInt();
		EntityController.zoomInKey = config.get("controls", "ZoomInKey", Keyboard.KEY_PRIOR).getInt();
		EntityController.zoomOutKey = config.get("controls", "ZoomOutKey", Keyboard.KEY_NEXT).getInt();
		config.save();
	}
	
	@EventHandler
	public void Init(FMLInitializationEvent event){
		proxy.init();
	}
}

