package minecrafttransportsimulator.dataclasses;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.BlockFuelPump;
import minecrafttransportsimulator.blocks.BlockPropellerBench;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftLarge;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftSmall;
import minecrafttransportsimulator.entities.parts.EntityEngineCarSmall;
import minecrafttransportsimulator.entities.parts.EntityPontoon;
import minecrafttransportsimulator.entities.parts.EntityPropeller;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntitySkid;
import minecrafttransportsimulator.entities.parts.EntityVehicleChest;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.items.ItemEngine;
import minecrafttransportsimulator.items.ItemEngine.ItemEngineAircraftLarge;
import minecrafttransportsimulator.items.ItemEngine.ItemEngineAircraftSmall;
import minecrafttransportsimulator.items.ItemEngine.ItemEngineCar;
import minecrafttransportsimulator.items.ItemInstrument;
import minecrafttransportsimulator.items.ItemKey;
import minecrafttransportsimulator.items.ItemManual;
import minecrafttransportsimulator.items.ItemMultipartMoving;
import minecrafttransportsimulator.items.ItemPropeller;
import minecrafttransportsimulator.items.ItemSeat;
import minecrafttransportsimulator.items.ItemWrench;
import minecrafttransportsimulator.packets.control.AileronPacket;
import minecrafttransportsimulator.packets.control.BrakePacket;
import minecrafttransportsimulator.packets.control.ElevatorPacket;
import minecrafttransportsimulator.packets.control.EnginePacket;
import minecrafttransportsimulator.packets.control.FlapPacket;
import minecrafttransportsimulator.packets.control.HornPacket;
import minecrafttransportsimulator.packets.control.LightPacket;
import minecrafttransportsimulator.packets.control.RudderPacket;
import minecrafttransportsimulator.packets.control.ShiftPacket;
import minecrafttransportsimulator.packets.control.SteeringPacket;
import minecrafttransportsimulator.packets.control.ThrottlePacket;
import minecrafttransportsimulator.packets.control.TrimPacket;
import minecrafttransportsimulator.packets.general.ChatPacket;
import minecrafttransportsimulator.packets.general.EntityClientRequestDataPacket;
import minecrafttransportsimulator.packets.general.FlatWheelPacket;
import minecrafttransportsimulator.packets.general.FuelPumpConnectDisconnectPacket;
import minecrafttransportsimulator.packets.general.FuelPumpFillDrainPacket;
import minecrafttransportsimulator.packets.general.InstrumentAddRemovePacket;
import minecrafttransportsimulator.packets.general.ManualPageUpdatePacket;
import minecrafttransportsimulator.packets.general.MultipartDeltaPacket;
import minecrafttransportsimulator.packets.general.MultipartParentDamagePacket;
import minecrafttransportsimulator.packets.general.PackReloadPacket;
import minecrafttransportsimulator.packets.general.PropellerBenchUpdatePacket;
import minecrafttransportsimulator.packets.general.ServerDataPacket;
import minecrafttransportsimulator.packets.general.TileEntityClientServerHandshakePacket;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem.MultipartTypes;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

/**Main registry class.  This class should be referenced by any class looking for
 * MTS items or blocks.  Adding new items and blocks is a simple as adding them
 * as a field; the init method automatically registers all items and blocks in the class
 * and orders them according to the order in which they were declared.
 * This calls the {@link PackParserSystem} to get the custom vehicles from there.
 * 
 * @author don_bruce
 */
@Mod.EventBusSubscriber
public final class MTSRegistry{	
	public static final Item wheelSmall = new Item().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item wheelMedium = new Item().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item wheelLarge = new Item().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item skid = new Item().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item pontoon = new Item().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item engineCarSmall = new ItemEngineCar().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item engineAircraftSmall = new ItemEngineAircraftSmall().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item engineAircraftLarge = new ItemEngineAircraftLarge().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item propeller = new ItemPropeller().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item seat = new ItemSeat().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item pointerShort = new Item().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item pointerLong = new Item().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item wrench = new ItemWrench().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item manual = new ItemManual().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item key = new ItemKey().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item instrument = new ItemInstrument().setCreativeTab(MTSCreativeTabs.tabMTS);
	
	
	public static final Block propellerBench = new BlockPropellerBench().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item itemBlockPropellerBench = new ItemBlock(propellerBench);
	public static final Block fuelPump = new BlockFuelPump().setCreativeTab(MTSCreativeTabs.tabMTS);
	public static final Item itemBlockFuelPump = new ItemBlock(fuelPump);
		
	
	
	private static int entityNumber = 0;
	private static int packetNumber = 0;
	private static int craftingNumber = 0;
	public static List<Item> itemList = new ArrayList<Item>();
	/**Maps multipart item names to items.*/
	public static Map<String, ItemMultipartMoving> multipartItemMap = new HashMap<String, ItemMultipartMoving>();
	/**Maps child class names to classes for quicker lookup during spawn operations.*/
	public static Map<String, Class<? extends EntityMultipartChild>> partClasses = new HashMap<String, Class<? extends EntityMultipartChild>>();
	/**Maps item names to their recipes.*/
	public static Map<String, ItemStack[]> craftingItemMap = new HashMap<String, ItemStack[]>();

	/**All run-time things go here.**/
	public static void init(){
		initEntities();
		initPackets();
		initPartRecipes();
		initEngineRecipes();
		initAircraftInstrumentRecipes();
		initPackRecipes();
	}
	
	/**
	 * Registers the given block and adds it to the creative tab list.
	 * Also adds the respective TileEntity if the block has one.
	 * @param block
	 */
	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event){
		for(Field field : MTSRegistry.class.getFields()){
			if(field.getType().equals(Block.class)){
				try{
					Block block = (Block) field.get(Block.class);
					String name = block.getClass().getSimpleName().toLowerCase().substring(5);
					event.getRegistry().register(block.setRegistryName(name).setUnlocalizedName(name));
					if(block instanceof ITileEntityProvider){
						Class<? extends TileEntity> tileEntityClass = ((ITileEntityProvider) block).createNewTileEntity(null, 0).getClass();
						GameRegistry.registerTileEntity(tileEntityClass, tileEntityClass.getSimpleName());
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Registers all items in-game.  Registers multiparts, then regular items, then itemblocks.
	 * This order ensures correct order in creative tabs.
	 * @param item
	 */
	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event){
		List<String> nameList = new ArrayList<String>(PackParserSystem.getRegisteredNames());
		Collections.sort(nameList);
		for(String name : nameList){
			MultipartTypes type = PackParserSystem.getMultipartType(name);
			if(type != null){
				ItemMultipartMoving itemMultipart = new ItemMultipartMoving(name);
				multipartItemMap.put(name, itemMultipart);
				event.getRegistry().register(itemMultipart.setRegistryName(name).setUnlocalizedName(name));
				MTSRegistry.itemList.add(itemMultipart);
			}
		}
		
		for(Field field : MTSRegistry.class.getFields()){
			if(field.getType().equals(Item.class)){
				try{
					Item item = (Item) field.get(Item.class);
					String name = field.getName().toLowerCase();
					if(!name.startsWith("itemblock")){
						event.getRegistry().register(item.setRegistryName(name).setUnlocalizedName(name));
						MTSRegistry.itemList.add(item);
					}else{
						name = name.substring("itemblock".length());
						event.getRegistry().register(item.setRegistryName(name).setUnlocalizedName(name));
						MTSRegistry.itemList.add(item);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		MTSAchievements.init();
	}

	private static void initEntities(){
		for(MultipartTypes type : PackParserSystem.MultipartTypes.values()){
			registerEntity(type.multipartClass);
		}
		
		registerChildEntity(EntitySeat.class, seat);
		registerChildEntity(EntityVehicleChest.class, Item.getItemFromBlock(Blocks.CHEST));
		registerChildEntity(EntityWheel.EntityWheelSmall.class, wheelSmall);
		registerChildEntity(EntityWheel.EntityWheelMedium.class, wheelMedium);
		registerChildEntity(EntityWheel.EntityWheelLarge.class, wheelLarge);
		registerChildEntity(EntitySkid.class, skid);
		registerChildEntity(EntityPontoon.class, pontoon);
		registerChildEntity(EntityPontoon.EntityPontoonDummy.class, null);
		registerChildEntity(EntityPropeller.class, propeller);
		registerChildEntity(EntityEngineCarSmall.Automatic.class, engineCarSmall);
		registerChildEntity(EntityEngineAircraftSmall.class, engineAircraftSmall);
		registerChildEntity(EntityEngineAircraftLarge.class, engineAircraftLarge);
	}
	
	private static void initPackets(){
		registerPacket(ChatPacket.class, ChatPacket.Handler.class, true, false);
		registerPacket(MultipartParentDamagePacket.class, MultipartParentDamagePacket.Handler.class, true, false);
		registerPacket(EntityClientRequestDataPacket.class, EntityClientRequestDataPacket.Handler.class, false, true);
		registerPacket(FlatWheelPacket.class, FlatWheelPacket.Handler.class, true, false);
		registerPacket(FuelPumpConnectDisconnectPacket.class, FuelPumpConnectDisconnectPacket.Handler.class, true, false);
		registerPacket(FuelPumpFillDrainPacket.class, FuelPumpFillDrainPacket.Handler.class, true, false);
		registerPacket(InstrumentAddRemovePacket.class, InstrumentAddRemovePacket.Handler.class, true, true);
		registerPacket(ManualPageUpdatePacket.class, ManualPageUpdatePacket.Handler.class, false, true);
		registerPacket(MultipartDeltaPacket.class, MultipartDeltaPacket.Handler.class, true, false);
		registerPacket(PackReloadPacket.class, PackReloadPacket.Handler.class, true, true);
		registerPacket(PropellerBenchUpdatePacket.class, PropellerBenchUpdatePacket.Handler.class, true, true);
		registerPacket(ServerDataPacket.class, ServerDataPacket.Handler.class, true, false);
		registerPacket(TileEntityClientServerHandshakePacket.class, TileEntityClientServerHandshakePacket.Handler.class, true, true);
		
		registerPacket(AileronPacket.class, AileronPacket.Handler.class, true, true);
		registerPacket(BrakePacket.class, BrakePacket.Handler.class, true, true);
		registerPacket(ElevatorPacket.class, ElevatorPacket.Handler.class, true, true);
		registerPacket(EnginePacket.class, EnginePacket.Handler.class, true, true);
		registerPacket(FlapPacket.class, FlapPacket.Handler.class, true, true);
		registerPacket(HornPacket.class, HornPacket.Handler.class, true, true);
		registerPacket(LightPacket.class, LightPacket.Handler.class, true, true);
		registerPacket(RudderPacket.class, RudderPacket.Handler.class, true, true);
		registerPacket(ShiftPacket.class, ShiftPacket.Handler.class, true, true);
		registerPacket(SteeringPacket.class, SteeringPacket.Handler.class, true, true);
		registerPacket(ThrottlePacket.class, ThrottlePacket.Handler.class, true, true);
		registerPacket(TrimPacket.class, TrimPacket.Handler.class, true, true);
	}
	
	private static void initPartRecipes(){
		//Seats
		for(int i=0; i<96; ++i){
			registerRecipe(new ItemStack(seat, 1, i),
				" BA",
				" BA",
				"AAA",
				'A', new ItemStack(Blocks.WOODEN_SLAB, 1, i%6),
				'B', new ItemStack(Blocks.WOOL, 1, i/6));
		}
		for(int i=0; i<6; ++i){
			registerRecipe(new ItemStack(seat, 1, 96 + i),
				" BA",
				" BA",
				"AAA",
				'A', new ItemStack(Blocks.WOODEN_SLAB, 1, i%6),
				'B', new ItemStack(Items.LEATHER));
		}
		
		//Wheels
		registerRecipe(new ItemStack(wheelSmall, 2),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.WOOL,
				'B', new ItemStack(Items.DYE, 1, 0),
				'C', Items.IRON_INGOT);
		registerRecipe(new ItemStack(wheelMedium, 2),
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.WOOL,
				'B', new ItemStack(Items.DYE, 1, 0),
				'C', Items.IRON_INGOT);
		registerRecipe(new ItemStack(wheelLarge, 2),
				"BBB",
				"ACA",
				"BBB",
				'A', Blocks.WOOL,
				'B', new ItemStack(Items.DYE, 1, 0),
				'C', Items.IRON_INGOT);
		//Skid
		registerRecipe(new ItemStack(skid),
				"A A",
				" A ",
				"  A",
				'A', Blocks.IRON_BARS);
		//Pontoon
		registerRecipe(new ItemStack(pontoon, 2),
				"AAA",
				"BBB",
				"AAA",
				'A', Items.IRON_INGOT,
				'B', Blocks.WOOL);
		
		//Propeller bench
		registerRecipe(new ItemStack(itemBlockPropellerBench),
				"AAA",
				" BA",
				"ACA",
				'A', Items.IRON_INGOT,
				'B', Items.DIAMOND,
				'C', Blocks.ANVIL);
		
		//Fuel pump
		registerRecipe(new ItemStack(itemBlockFuelPump),
				"DED",
				"CBC",
				"AAA",
				'A', new ItemStack(Blocks.STONE_SLAB, 1, 0),
				'B', Items.IRON_INGOT,
				'C', new ItemStack(Items.DYE, 1, 0),
				'D', new ItemStack(Items.DYE, 1, 1),
				'E', Blocks.GLASS_PANE);
		
		//Manual
		registerRecipe(new ItemStack(manual),
				" A ",
				"CBC",
				" D ",
				'A', Items.FEATHER,
				'B', Items.BOOK,
				'C', new ItemStack(Items.DYE, 1, 0),
				'D', Items.PAPER);
		
		//Key
		registerRecipe(new ItemStack(key),
				" A ",
				" A ",
				" S ",
				'A', Items.IRON_INGOT,
				'S', Items.STRING);
		//Wrench
		registerRecipe(new ItemStack(wrench),
				"  A",
				" A ",
				"A  ",
				'A', Items.IRON_INGOT);
	}
	
	private static void initEngineRecipes(){
		//New engines
		registerRecipe(ItemEngine.getStackWithData((ItemEngine) MTSRegistry.engineCarSmall, false),
				"AAA",
				"BCB",
				"BBB",
				'A', Blocks.PISTON,
				'B', Blocks.OBSIDIAN,
				'C', Items.IRON_INGOT);
		registerRecipe(ItemEngine.getStackWithData((ItemEngine) MTSRegistry.engineAircraftSmall, false),
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.PISTON,
				'B', Blocks.OBSIDIAN,
				'C', Items.IRON_INGOT);
		registerRecipe(ItemEngine.getStackWithData((ItemEngine) MTSRegistry.engineAircraftLarge, false),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.PISTON,
				'B', Blocks.OBSIDIAN,
				'C', Items.IRON_INGOT);
		
		//Repaired engines
		registerRecipe(ItemEngine.getStackWithData((ItemEngine) MTSRegistry.engineCarSmall, false),
				"B B",
				" C ",
				"B B",
				'B', Blocks.OBSIDIAN,
				'C', MTSRegistry.engineCarSmall);
		registerRecipe(ItemEngine.getStackWithData((ItemEngine) MTSRegistry.engineAircraftSmall, false),
				"B B",
				" C ",
				"B B",
				'B', Blocks.OBSIDIAN,
				'C', MTSRegistry.engineAircraftSmall);
		registerRecipe(ItemEngine.getStackWithData((ItemEngine) MTSRegistry.engineAircraftLarge, false),
				"B B",
				"BCB",
				"B B",
				'B', Blocks.OBSIDIAN,
				'C', MTSRegistry.engineAircraftLarge);
	}
	
	private static void initAircraftInstrumentRecipes(){		
		registerRecipe(new ItemStack(pointerShort, 2),
				" WW",
				" WW",
				"I  ",
				'W', new ItemStack(Items.DYE, 1, 15),
				'I', Items.IRON_INGOT);
		
		registerRecipe(new ItemStack(pointerLong, 2),
				"  W",
				" W ",
				"I  ",
				'W', new ItemStack(Items.DYE, 1, 15),
				'I', Items.IRON_INGOT);
		
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 16, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()),
				"III",
				"IGI",
				"III",
				'I', Items.IRON_INGOT,
				'G', Blocks.GLASS_PANE);
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_ATTITUDE.ordinal()),
				"LLL",
				"RRR",
				" B ",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'L', new ItemStack(Items.DYE, 1, 4),
				'R', new ItemStack(Items.DYE, 1, 3));
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_ALTIMETER.ordinal()),
				"WLW",
				"WSW",
				" B ",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'L', pointerLong, 
				'S', pointerShort, 
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_HEADING.ordinal()),
				" W ",
				"WIW",
				" B ",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'I', Items.IRON_INGOT,
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_AIRSPEED.ordinal()),
				"R W",
				"YLG",
				"GBG",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'L', pointerLong, 
				'R', new ItemStack(Items.DYE, 1, 1),
				'Y', new ItemStack(Items.DYE, 1, 11),
				'G', new ItemStack(Items.DYE, 1, 10),
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_TURNCOORD.ordinal()),
				"   ",
				"WIW",
				"WBW",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'I', Items.IRON_INGOT,
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_TURNSLIP.ordinal()),
				"WWW",
				" I ",
				"WBW",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'I', Items.IRON_INGOT,
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_VERTICALSPEED.ordinal()),
				"W W",
				" L ",
				"WBW",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'L', pointerLong, 
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_LIFTRESERVE.ordinal()),
				"RYG",
				" LG",
				" B ",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'L', pointerLong, 
				'R', new ItemStack(Items.DYE, 1, 1),
				'Y', new ItemStack(Items.DYE, 1, 11),
				'G', new ItemStack(Items.DYE, 1, 10));
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_TRIM.ordinal()),
				"GLG",
				"LGL",
				" B ",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'L', pointerLong, 
				'G', new ItemStack(Items.DYE, 1, 10));
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_ELECTRIC.ordinal()),
				"G W",
				"LGL",
				"RBW",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'L', pointerLong, 
				'G', new ItemStack(Items.DYE, 1, 10),
				'R', new ItemStack(Items.DYE, 1, 1),
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_TACHOMETER.ordinal()),
				"W W",
				" L ",
				"WBR",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'L', pointerLong, 
				'R', new ItemStack(Items.DYE, 1, 1),
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_FUELQTY.ordinal()),
				"RWW",
				" L ",
				" B ",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'L', pointerLong, 
				'R', new ItemStack(Items.DYE, 1, 1),
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_FUELFLOW.ordinal()),
				" W ",
				"WLW",
				" B ",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'L', pointerLong, 
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_ENGINETEMP.ordinal()),
				"YGR",
				" L ",
				" B ",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'L', pointerLong, 
				'Y', new ItemStack(Items.DYE, 1, 11),
				'G', new ItemStack(Items.DYE, 1, 10),
				'R', new ItemStack(Items.DYE, 1, 1));
				
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_OILPRESSURE.ordinal()),
				"   ",
				"LGL",
				"RB ",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'L', pointerLong,  
				'G', new ItemStack(Items.DYE, 1, 10),
				'R', new ItemStack(Items.DYE, 1, 1));
	}

	private static void initPackRecipes(){
		for(Entry<String, ItemMultipartMoving> mapEntry : multipartItemMap.entrySet()){
			String name = mapEntry.getKey();
			MultipartTypes type = PackParserSystem.getMultipartType(name);
			//Init multipart recipes.
			//Convert strings into ItemStacks
			Character[] indexes = new Character[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
			String[] craftingRows = new String[]{"", "", ""};
			List<ItemStack> stacks = new ArrayList<ItemStack>();
			ItemStack[] craftingArray = new ItemStack[9];
			String[] recipe = PackParserSystem.getDefinitionForPack(name).recipe;
			for(byte i=0; i<9; ++i){
				if(!recipe[i].isEmpty()){
					Item item = Item.getByNameOrId(recipe[i].substring(0, recipe[i].lastIndexOf(':')));
					int damage = Integer.valueOf(recipe[i].substring(recipe[i].lastIndexOf(':') + 1));
					craftingRows[i/3] = craftingRows[i/3] + indexes[stacks.size()];
					stacks.add(new ItemStack(item, 1, damage));
					craftingArray[i] = stacks.get(stacks.size() - 1); 
				}else{
					craftingRows[i/3] = craftingRows[i/3] + ' ';
				}
			}

			//Create the object array that is going to be registered.
			Object[] registryObject = new Object[craftingRows.length + stacks.size()*2];
			registryObject[0] = craftingRows[0];
			registryObject[1] = craftingRows[1];
			registryObject[2] = craftingRows[2];
			for(byte i=0; i<stacks.size(); ++i){
				registryObject[3 + i*2] = indexes[i];
				registryObject[3 + i*2 + 1] = stacks.get(i);
			}
			//Now register the recipe and add the stacks to the crafting item map.
			registerRecipe(new ItemStack(mapEntry.getValue()), registryObject);
			craftingItemMap.put(name, craftingArray);
		}
	}

	/**
	 * Registers an entity.
	 * Adds it to the multipartClassess map if appropriate.
	 * @param entityClass
	 */
	private static void registerEntity(Class<? extends Entity> entityClass){
		EntityRegistry.registerModEntity(entityClass, entityClass.getSimpleName().substring(6).toLowerCase(), entityNumber++, MTS.MODID, 80, 5, false);
	}
	
	/**
	 * Registers a child entity.
	 * Optionally pairs the entity with an item for GUI operations.
	 * Note that the name of the item is what name you should use in the {@link PackParserSystem}
	 * @param entityClass
	 * @param entityItem
	 */
	private static void registerChildEntity(Class<? extends EntityMultipartChild> entityClass, Item entityItem){
		if(entityItem != null){
			partClasses.put(entityItem.getRegistryName().getResourcePath(), entityClass);
		}
		registerEntity(entityClass);
	}
	
	/**
	 * Registers a packet and its handler on the client and/or the server.
	 * @param packetClass
	 * @param handlerClass
	 * @param client
	 * @param server
	 */
	private static <REQ extends IMessage, REPLY extends IMessage> void registerPacket(Class<REQ> packetClass, Class<? extends IMessageHandler<REQ, REPLY>> handlerClass, boolean client, boolean server){
		if(client)MTS.MTSNet.registerMessage(handlerClass, packetClass, ++packetNumber, Side.CLIENT);
		if(server)MTS.MTSNet.registerMessage(handlerClass, packetClass, ++packetNumber, Side.SERVER);
	}
	
	private static void registerRecipe(ItemStack output, Object...params){
		GameRegistry.addShapedRecipe(output, params);
		++craftingNumber;
	}
}
