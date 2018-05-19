package minecrafttransportsimulator.dataclasses;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.BlockFuelPump;
import minecrafttransportsimulator.blocks.BlockPropellerBench;
import minecrafttransportsimulator.items.core.ItemInstrument;
import minecrafttransportsimulator.items.core.ItemKey;
import minecrafttransportsimulator.items.core.ItemManual;
import minecrafttransportsimulator.items.core.ItemMultipart;
import minecrafttransportsimulator.items.core.ItemWrench;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.items.parts.ItemPartEngine;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Car;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;
import minecrafttransportsimulator.packets.control.AileronPacket;
import minecrafttransportsimulator.packets.control.BrakePacket;
import minecrafttransportsimulator.packets.control.ElevatorPacket;
import minecrafttransportsimulator.packets.control.FlapPacket;
import minecrafttransportsimulator.packets.control.HornPacket;
import minecrafttransportsimulator.packets.control.LightPacket;
import minecrafttransportsimulator.packets.control.RudderPacket;
import minecrafttransportsimulator.packets.control.ShiftPacket;
import minecrafttransportsimulator.packets.control.SteeringPacket;
import minecrafttransportsimulator.packets.control.ThrottlePacket;
import minecrafttransportsimulator.packets.control.TrimPacket;
import minecrafttransportsimulator.packets.crafting.PropellerBenchUpdatePacket;
import minecrafttransportsimulator.packets.general.ChatPacket;
import minecrafttransportsimulator.packets.general.FuelPumpConnectionPacket;
import minecrafttransportsimulator.packets.general.FuelPumpFillDrainPacket;
import minecrafttransportsimulator.packets.general.ManualPageUpdatePacket;
import minecrafttransportsimulator.packets.general.PackReloadPacket;
import minecrafttransportsimulator.packets.general.TileEntityClientServerHandshakePacket;
import minecrafttransportsimulator.packets.multipart.PacketMultipartAttacked;
import minecrafttransportsimulator.packets.multipart.PacketMultipartClientInit;
import minecrafttransportsimulator.packets.multipart.PacketMultipartClientInitResponse;
import minecrafttransportsimulator.packets.multipart.PacketMultipartClientPartAddition;
import minecrafttransportsimulator.packets.multipart.PacketMultipartClientPartRemoval;
import minecrafttransportsimulator.packets.multipart.PacketMultipartDeltas;
import minecrafttransportsimulator.packets.multipart.PacketMultipartInstruments;
import minecrafttransportsimulator.packets.multipart.PacketMultipartKey;
import minecrafttransportsimulator.packets.multipart.PacketMultipartNameTag;
import minecrafttransportsimulator.packets.multipart.PacketMultipartServerPartAddition;
import minecrafttransportsimulator.packets.multipart.PacketMultipartWindowBreak;
import minecrafttransportsimulator.packets.multipart.PacketMultipartWindowFix;
import minecrafttransportsimulator.packets.parts.PacketPartEngineDamage;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal;
import minecrafttransportsimulator.packets.parts.PacketPartGroundDeviceFlat;
import minecrafttransportsimulator.packets.parts.PacketPartInteraction;
import minecrafttransportsimulator.systems.PackParserSystem;
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
 * This calls the {@link PackParserSystem} to register any custom vehicles and parts
 * that were loaded by packs.
 * 
 * @author don_bruce
 */
@Mod.EventBusSubscriber
public final class MTSRegistry{
	/**All registered items are stored in this list as they are added.  Used to sort items in creative tabs.**/
	public static List<Item> itemList = new ArrayList<Item>();
	
	/**Maps multipart item names to items.  All multipart items for all packs will be populated here.*/
	public static Map<String, ItemMultipart> multipartItemMap = new HashMap<String, ItemMultipart>();
	
	/**Maps part item names to items.  All part items for all packs will be populated here.*/
	public static Map<String, AItemPart> partItemMap = new HashMap<String, AItemPart>();
	
	/**Core creative tab for base MTS items**/
	public static final CreativeTabCore coreTab = new CreativeTabCore();
	
	/**Map of creative tabs for packs.  Keyed by pack IDs.  Populated by the {@link PackParserSystem}**/
	public static final Map<String, CreativeTabPack> packTabs = new HashMap<String, CreativeTabPack>();

	
	public static final Item manual = new ItemManual().setCreativeTab(coreTab);
	public static final Item wrench = new ItemWrench().setCreativeTab(coreTab);
	public static final Item key = new ItemKey().setCreativeTab(coreTab);
	
	public static final Block propellerBench = new BlockPropellerBench().setCreativeTab(coreTab);
	public static final Block fuelPump = new BlockFuelPump().setCreativeTab(coreTab);	
	public static final Item itemBlockPropellerBench = new ItemBlock(propellerBench);
	public static final Item itemBlockFuelPump = new ItemBlock(fuelPump);
	
	public static final Item pointerShort = new Item().setCreativeTab(coreTab);
	public static final Item pointerLong = new Item().setCreativeTab(coreTab);
	public static final Item instrument = new ItemInstrument().setCreativeTab(coreTab);
	
	//Counters for registry systems.
	private static int entityNumber = 0;
	private static int packetNumber = 0;
	private static int craftingNumber = 0;
	
	
	/**All run-time things go here.**/
	public static void init(){
		initMultipartEntities();
		initPackets();
		initPartRecipes();
		initEngineRecipes();
		initAircraftInstrumentRecipes();
	}
	
	/**
	 * Registers all blocks present in this class.
	 * Also adds the respective TileEntity if the block has one.
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
	 * Registers all items (and itemblocks) present in this class.
	 * Additionally registers multipart items and multipart part items
	 * that were loaded into MTS earlier during init.  During this section we
	 * also set up the creative tabs to hold the packs and set any registered
	 * items to the appropriate tab.
	 */
	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event){
		//First register all core items.
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
		
		
		//Next register multipart items.
		List<String> nameList = new ArrayList<String>(PackParserSystem.getAllMultipartPackNames());
		for(String multipartName : nameList){
			ItemMultipart itemMultipart = new ItemMultipart(multipartName);
			multipartItemMap.put(multipartName, itemMultipart);
			event.getRegistry().register(itemMultipart);
			MTSRegistry.itemList.add(itemMultipart);
		}
		
		//Now register part items.
		nameList = new ArrayList<String>(PackParserSystem.getAllPartPackNames());
		for(String partName : nameList){
			try{
				Class<? extends AItemPart> itemClass = PackParserSystem.getPartItemClass(partName);
				Constructor<? extends AItemPart> construct = itemClass.getConstructor(String.class);
				AItemPart itemPart = construct.newInstance(partName);
				partItemMap.put(partName, itemPart);
				event.getRegistry().register(itemPart);
				MTSRegistry.itemList.add(itemPart);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	/**
	 * Registers all entities with the entity registry.
	 * For multiparts we only register the main classes as
	 * the pack data stored in NBT is what makes for different vehicles.
	 */
	private static void initMultipartEntities(){
		EntityRegistry.registerModEntity(EntityMultipartF_Car.class, "multipartcar", entityNumber++, MTS.MODID, 80, 5, false);
		EntityRegistry.registerModEntity(EntityMultipartF_Plane.class, "multipartplane", entityNumber++, MTS.MODID, 80, 5, false);
	}
	
	private static void initPackets(){
		//Packets in packets.control
		registerPacket(AileronPacket.class, AileronPacket.Handler.class, true, true);
		registerPacket(BrakePacket.class, BrakePacket.Handler.class, true, true);
		registerPacket(ElevatorPacket.class, ElevatorPacket.Handler.class, true, true);
		registerPacket(FlapPacket.class, FlapPacket.Handler.class, true, true);
		registerPacket(HornPacket.class, HornPacket.Handler.class, true, true);
		registerPacket(LightPacket.class, LightPacket.Handler.class, true, true);
		registerPacket(RudderPacket.class, RudderPacket.Handler.class, true, true);
		registerPacket(ShiftPacket.class, ShiftPacket.Handler.class, true, true);
		registerPacket(SteeringPacket.class, SteeringPacket.Handler.class, true, true);
		registerPacket(ThrottlePacket.class, ThrottlePacket.Handler.class, true, true);
		registerPacket(TrimPacket.class, TrimPacket.Handler.class, true, true);
		
		//Packets in packets.general
		registerPacket(ChatPacket.class, ChatPacket.Handler.class, true, false);
		registerPacket(FuelPumpConnectionPacket.class, FuelPumpConnectionPacket.Handler.class, true, false);
		registerPacket(FuelPumpFillDrainPacket.class, FuelPumpFillDrainPacket.Handler.class, true, false);
		registerPacket(ManualPageUpdatePacket.class, ManualPageUpdatePacket.Handler.class, false, true);
		registerPacket(PackReloadPacket.class, PackReloadPacket.Handler.class, true, true);
		registerPacket(PropellerBenchUpdatePacket.class, PropellerBenchUpdatePacket.Handler.class, true, true);
		registerPacket(TileEntityClientServerHandshakePacket.class, TileEntityClientServerHandshakePacket.Handler.class, true, true);
		
		//Packets in packets.multipart.
		registerPacket(PacketMultipartAttacked.class, PacketMultipartAttacked.Handler.class, false, true);
		registerPacket(PacketMultipartClientInit.class, PacketMultipartClientInit.Handler.class, false, true);
		registerPacket(PacketMultipartClientInitResponse.class, PacketMultipartClientInitResponse.Handler.class, true, false);
		registerPacket(PacketMultipartClientPartAddition.class, PacketMultipartClientPartAddition.Handler.class, true, false);
		registerPacket(PacketMultipartClientPartRemoval.class, PacketMultipartClientPartRemoval.Handler.class, true, false);
		registerPacket(PacketMultipartDeltas.class, PacketMultipartDeltas.Handler.class, true, false);
		registerPacket(PacketMultipartInstruments.class, PacketMultipartInstruments.Handler.class, true, true);
		registerPacket(PacketMultipartKey.class, PacketMultipartKey.Handler.class, true, true);
		registerPacket(PacketMultipartNameTag.class, PacketMultipartNameTag.Handler.class, true, true);
		registerPacket(PacketMultipartServerPartAddition.class, PacketMultipartServerPartAddition.Handler.class, false, true);
		registerPacket(PacketMultipartWindowBreak.class, PacketMultipartWindowBreak.Handler.class, true, false);
		registerPacket(PacketMultipartWindowFix.class, PacketMultipartWindowFix.Handler.class, true, true);
		
		//Packets in packets.parts
		registerPacket(PacketPartEngineDamage.class, PacketPartEngineDamage.Handler.class, true, false);
		registerPacket(PacketPartEngineSignal.class, PacketPartEngineSignal.Handler.class, true, true);
		registerPacket(PacketPartGroundDeviceFlat.class, PacketPartGroundDeviceFlat.Handler.class, true, false);
		registerPacket(PacketPartInteraction.class, PacketPartInteraction.Handler.class, false, true);
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
		//Crate
		registerRecipe(new ItemStack(crate),
				"AAA",
				"ABA",
				"AAA",
				'A', Blocks.PLANKS,
				'B', Blocks.WOODEN_SLAB);
		
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
		registerRecipe(ItemPartEngine.getStackWithData((ItemPartEngine) MTSRegistry.engineAMCI4, false),
				"AAA",
				"BCB",
				"BBB",
				'A', Blocks.PISTON,
				'B', Blocks.OBSIDIAN,
				'C', Items.IRON_INGOT);
		registerRecipe(ItemPartEngine.getStackWithData((ItemPartEngine) MTSRegistry.engineLycomingO360, false),
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.PISTON,
				'B', Blocks.OBSIDIAN,
				'C', Items.IRON_INGOT);
		registerRecipe(ItemPartEngine.getStackWithData((ItemPartEngine) MTSRegistry.engineBristolMercury, false),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.PISTON,
				'B', Blocks.OBSIDIAN,
				'C', Items.IRON_INGOT);
		registerRecipe(ItemPartEngine.getStackWithData((ItemPartEngine) MTSRegistry.engineDetroitDiesel, false),
				"AAA",
				"ACA",
				"BBB",
				'A', Blocks.PISTON,
				'B', Blocks.OBSIDIAN,
				'C', Items.IRON_INGOT);
		
		//Repaired engines
		registerRecipe(ItemPartEngine.getStackWithData((ItemPartEngine) MTSRegistry.engineAMCI4, false),
				"B B",
				" C ",
				"B B",
				'B', Blocks.OBSIDIAN,
				'C', MTSRegistry.engineAMCI4);
		registerRecipe(ItemPartEngine.getStackWithData((ItemPartEngine) MTSRegistry.engineLycomingO360, false),
				"B B",
				" C ",
				"B B",
				'B', Blocks.OBSIDIAN,
				'C', MTSRegistry.engineLycomingO360);
		registerRecipe(ItemPartEngine.getStackWithData((ItemPartEngine) MTSRegistry.engineBristolMercury, false),
				"B B",
				"BCB",
				"B B",
				'B', Blocks.OBSIDIAN,
				'C', MTSRegistry.engineBristolMercury);
		registerRecipe(ItemPartEngine.getStackWithData((ItemPartEngine) MTSRegistry.engineDetroitDiesel, false),
				"B B",
				"BCB",
				"B B",
				'B', Blocks.OBSIDIAN,
				'C', MTSRegistry.engineDetroitDiesel);
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
