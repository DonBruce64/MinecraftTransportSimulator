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
import minecrafttransportsimulator.entities.main.EntityCore;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftLarge;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftSmall;
import minecrafttransportsimulator.entities.parts.EntityPontoon;
import minecrafttransportsimulator.entities.parts.EntityPropeller;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntitySkid;
import minecrafttransportsimulator.entities.parts.EntityVehicleChest;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.items.ItemEngine;
import minecrafttransportsimulator.items.ItemEngine.ItemEngineAircraftLarge;
import minecrafttransportsimulator.items.ItemEngine.ItemEngineAircraftSmall;
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
import minecrafttransportsimulator.packets.control.LightPacket;
import minecrafttransportsimulator.packets.control.RudderPacket;
import minecrafttransportsimulator.packets.control.ThrottlePacket;
import minecrafttransportsimulator.packets.control.TrimPacket;
import minecrafttransportsimulator.packets.general.ChatPacket;
import minecrafttransportsimulator.packets.general.DamagePacket;
import minecrafttransportsimulator.packets.general.EntityClientRequestDataPacket;
import minecrafttransportsimulator.packets.general.FlatWheelPacket;
import minecrafttransportsimulator.packets.general.FuelPumpFillDrainPacket;
import minecrafttransportsimulator.packets.general.InstrumentPacket;
import minecrafttransportsimulator.packets.general.ManualPageUpdatePacket;
import minecrafttransportsimulator.packets.general.PackReloadPacket;
import minecrafttransportsimulator.packets.general.ServerDataPacket;
import minecrafttransportsimulator.packets.general.ServerSyncPacket;
import minecrafttransportsimulator.packets.general.TileEntityClientRequestDataPacket;
import minecrafttransportsimulator.packets.general.TileEntitySyncPacket;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem.MultipartTypes;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
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
	public static final Item wheelSmall = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item wheelLarge = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item skid = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item pontoon = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item engineAircraftSmall = new ItemEngineAircraftSmall().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item engineAircraftLarge = new ItemEngineAircraftLarge().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item propeller = new ItemPropeller().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item seat = new ItemSeat().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item pointerShort = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item pointerLong = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item wrench = new ItemWrench().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item manual = new ItemManual().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Block propellerBench = new BlockPropellerBench().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Block fuelPump = new BlockFuelPump().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
		
	public static final Item key = new ItemKey();
	public static final Item instrument = new ItemInstrument();
	
	private static int entityNumber = 0;
	private static int packetNumber = 0;
	public static List<Item> itemList = new ArrayList<Item>();
	/**Maps multipart item names to items.*/
	public static Map<String, ItemMultipartMoving> multipartItemMap = new HashMap<String, ItemMultipartMoving>();
	/**Maps child class names to classes for quicker lookup during spawn operations.*/
	public static Map<String, Class<? extends EntityMultipartChild>> partClasses = new HashMap<String, Class<? extends EntityMultipartChild>>();

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
				ItemMultipartMoving itemMultipart = new ItemMultipartMoving(name, type.tabToDisplayOn);
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
					event.getRegistry().register(item.setRegistryName(name).setUnlocalizedName(name));
					MTSRegistry.itemList.add(item);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		for(Field field : MTSRegistry.class.getFields()){
			if(field.getType().equals(Block.class)){
				try{
					Block block = (Block) field.get(Block.class);
					String name = block.getClass().getSimpleName().toLowerCase().substring(5);
					if(block.getCreativeTabToDisplayOn() != null){
						event.getRegistry().register(new ItemBlock(block).setRegistryName(name));
						MTSRegistry.itemList.add(Item.getItemFromBlock(block));
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}

	private static void initEntities(){
		registerEntity(EntityPlane.class);
		
		registerChildEntity(EntityCore.class, null);
		registerChildEntity(EntitySeat.class, seat);
		registerChildEntity(EntityVehicleChest.class, Item.getItemFromBlock(Blocks.CHEST));
		registerChildEntity(EntityWheel.EntityWheelSmall.class, wheelSmall);
		registerChildEntity(EntityWheel.EntityWheelLarge.class, wheelLarge);
		registerChildEntity(EntitySkid.class, skid);
		registerChildEntity(EntityPontoon.class, pontoon);
		registerChildEntity(EntityPontoon.EntityPontoonDummy.class, null);
		registerChildEntity(EntityPropeller.class, propeller);
		registerChildEntity(EntityEngineAircraftSmall.class, engineAircraftSmall);
		registerChildEntity(EntityEngineAircraftLarge.class, engineAircraftLarge);
	}
	
	private static void initPackets(){
		registerPacket(ChatPacket.class, ChatPacket.Handler.class, true, false);
		registerPacket(DamagePacket.class, DamagePacket.Handler.class, true, false);
		registerPacket(EntityClientRequestDataPacket.class, EntityClientRequestDataPacket.Handler.class, false, true);
		registerPacket(FlatWheelPacket.class, FlatWheelPacket.Handler.class, true, false);
		registerPacket(FuelPumpFillDrainPacket.class, FuelPumpFillDrainPacket.Handler.class, true, false);
		registerPacket(InstrumentPacket.class, InstrumentPacket.Handler.class, true, true);
		registerPacket(ManualPageUpdatePacket.class, ManualPageUpdatePacket.Handler.class, false, true);
		registerPacket(PackReloadPacket.class, PackReloadPacket.Handler.class, true, true);
		registerPacket(ServerDataPacket.class, ServerDataPacket.Handler.class, true, false);
		registerPacket(ServerSyncPacket.class, ServerSyncPacket.Handler.class, true, false);
		registerPacket(TileEntityClientRequestDataPacket.class, TileEntityClientRequestDataPacket.Handler.class, false, true);
		registerPacket(TileEntitySyncPacket.class, TileEntitySyncPacket.Handler.class, true, true);
		
		registerPacket(AileronPacket.class, AileronPacket.Handler.class, true, true);
		registerPacket(BrakePacket.class, BrakePacket.Handler.class, true, true);
		registerPacket(ElevatorPacket.class, ElevatorPacket.Handler.class, true, true);
		registerPacket(EnginePacket.class, EnginePacket.Handler.class, true, true);
		registerPacket(FlapPacket.class, FlapPacket.Handler.class, true, true);
		registerPacket(LightPacket.class, LightPacket.Handler.class, true, true);
		registerPacket(RudderPacket.class, RudderPacket.Handler.class, true, true);
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
		registerRecipe(new ItemStack(wheelSmall),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.WOOL,
				'B', new ItemStack(Items.DYE, 1, 0),
				'C', Items.IRON_INGOT);
		registerRecipe(new ItemStack(wheelLarge),
				"ABA",
				"BCB",
				"ABA",
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
		registerRecipe(new ItemStack(propellerBench),
				"AAA",
				" BA",
				"ACA",
				'A', Items.IRON_INGOT,
				'B', Items.DIAMOND,
				'C', Blocks.ANVIL);
		
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
				"   ",
				" A ",
				" A ",
				'A', Items.IRON_INGOT);
	}
	
	private static void initEngineRecipes(){
		//New engines
		registerRecipe(((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[0],
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.PISTON,
				'B', Blocks.OBSIDIAN,
				'C', Items.IRON_INGOT);
		registerRecipe(((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[1],
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.PISTON,
				'B', Blocks.OBSIDIAN,
				'C', Items.DIAMOND);
		registerRecipe(((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[0],
				"ACA",
				"ACA",
				"ACA",
				'A', Blocks.PISTON,
				'B', Blocks.OBSIDIAN,
				'C', Items.IRON_INGOT);
		registerRecipe(((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[1],
				"ACA",
				"ACA",
				"ACA",
				'A', Blocks.PISTON,
				'B', Blocks.OBSIDIAN,
				'C', Items.DIAMOND);
		
		//Repaired engines
		registerRecipe(((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[0],
				"B B",
				" C ",
				"B B",
				'B', Blocks.OBSIDIAN,
				'C', ((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[0]);
		registerRecipe(((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[1],
				"B B",
				" C ",
				"B B",
				'B', Blocks.OBSIDIAN,
				'C', ((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[1]);
		registerRecipe(((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[0],
				"B B",
				"BCB",
				"B B",
				'B', Blocks.OBSIDIAN,
				'C', ((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[0]);
		registerRecipe(((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[1],
				"B B",
				"BCB",
				"B B",
				'B', Blocks.OBSIDIAN,
				'C', ((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[1]);
	}
	
	private static void initAircraftInstrumentRecipes(){		
		registerRecipe(new ItemStack(pointerShort),
				" WW",
				" WW",
				"B  ",
				'W', new ItemStack(Items.DYE, 1, 15),
				'B', new ItemStack(Items.DYE, 1, 0));
		
		registerRecipe(new ItemStack(pointerLong),
				"  W",
				" W ",
				"B  ",
				'W', new ItemStack(Items.DYE, 1, 15),
				'B', new ItemStack(Items.DYE, 1, 0));
		
		
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
				'G', new ItemStack(Items.DYE, 1, 10),
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_TRIM.ordinal()),
				"GLG",
				"LGL",
				" B ",
				'B', new ItemStack(MTSRegistry.instrument, 1, MTSInstruments.Instruments.AIRCRAFT_BLANK.ordinal()), 
				'L', pointerLong, 
				'G', new ItemStack(Items.DYE, 1, 10));
		
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
				'R', new ItemStack(Items.DYE, 1, 1),
				'W', new ItemStack(Items.DYE, 1, 15));
				
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
			String[] recipe = PackParserSystem.getDefinitionForPack(name).recipe;
			for(byte i=0; i<9; ++i){
				if(!recipe[i].isEmpty()){
					Item item = Item.getByNameOrId(recipe[i].substring(0, recipe[i].lastIndexOf(':')));
					int damage = Integer.valueOf(recipe[i].substring(recipe[i].lastIndexOf(':') + 1));
					craftingRows[i/3] = craftingRows[i/3] + indexes[stacks.size()];
					stacks.add(new ItemStack(item, 1, damage));
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
			//Now register the recipe
			registerRecipe(new ItemStack(mapEntry.getValue()), registryObject);
		}
	}

	/**
	 * Registers an entity.
	 * Adds it to the multipartClassess map if appropriate.
	 * @param entityClass
	 */
	private static void registerEntity(Class entityClass){
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
		GameRegistry.addRecipe(output, params);
	}
}
