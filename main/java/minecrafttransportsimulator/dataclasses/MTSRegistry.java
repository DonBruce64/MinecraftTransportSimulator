package minecrafttransportsimulator.dataclasses;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.BlockPropellerBench;
import minecrafttransportsimulator.blocks.BlockSurveyFlag;
import minecrafttransportsimulator.blocks.BlockTrack;
import minecrafttransportsimulator.blocks.BlockTrackFake;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.main.EntityCore;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.entities.parts.EntityBogie;
import minecrafttransportsimulator.entities.parts.EntityChest;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftLarge;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftSmall;
import minecrafttransportsimulator.entities.parts.EntityPontoon;
import minecrafttransportsimulator.entities.parts.EntityPropeller;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntitySkid;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.items.ItemEngine;
import minecrafttransportsimulator.items.ItemEngine.ItemEngineAircraftLarge;
import minecrafttransportsimulator.items.ItemEngine.ItemEngineAircraftSmall;
import minecrafttransportsimulator.items.ItemFlightInstrument;
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
import minecrafttransportsimulator.packets.general.EntityClientRequestDataPacket;
import minecrafttransportsimulator.packets.general.InstrumentPlanePacket;
import minecrafttransportsimulator.packets.general.ServerDataPacket;
import minecrafttransportsimulator.packets.general.ServerSyncPacket;
import minecrafttransportsimulator.packets.general.TileEntityClientRequestDataPacket;
import minecrafttransportsimulator.packets.general.TileEntitySyncPacket;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

/**Main registry class.  This class should be referenced by any class looking for
 * MTS items or blocks.  Adding new items and blocks is a simple as adding them
 * as a field; the init method automatically registers all items and blocks in the class
 * and orders them according to the order in which they were declared.
 * This calls the {@link PackParserSystem} to get the custom vehicles from there.
 * 
 * @author don_bruce
 */
public class MTSRegistry{
	public static final MTSRegistry instance = new MTSRegistry();
	
	public static final Item wheelSmall = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item wheelLarge = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item skid = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item pontoon = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item engineAircraftSmall = new ItemEngineAircraftSmall().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item engineAircraftLarge = new ItemEngineAircraftLarge().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item propeller = new ItemPropeller().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item seat = new ItemSeat().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item flightInstrument = new ItemFlightInstrument().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item pointerShort = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item pointerLong = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item wrench = new ItemWrench().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item flightManual = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Block propellerBench = new BlockPropellerBench().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	
	public static final Item track = new Item().setCreativeTab(MTSCreativeTabs.tabMTSTrains);
	public static final Item bogie = new Item().setCreativeTab(MTSCreativeTabs.tabMTSTrains);
	public static final Block blockTrack = new BlockTrack();
	public static final Block blockTrackFake = new BlockTrackFake();
	public static final Block surveyFlag = new BlockSurveyFlag().setCreativeTab(MTSCreativeTabs.tabMTSTrains);
	
	private static int entityNumber = 0;
	private static int packetNumber = 0;
	public static List<Item> itemList = new ArrayList<Item>();
	/**Maps child class names to classes for quicker lookup during spawn operations.*/
	public static Map<String, Class<? extends EntityMultipartChild>> partClasses = new HashMap<String, Class<? extends EntityMultipartChild>>();
	
	/**All run-time things go here.**/
	public void init(){
		initCustomEntities();
		initItems();
		initBlocks();
		initEntites();
		initPackets();
		initRecipies();
	}
	
	private void initCustomEntities(){
		
	}
	
	private void initItems(){
		for(Field feild : this.getClass().getFields()){
			if(feild.getType().equals(Item.class)){
				try{
					Item item = (Item) feild.get(Item.class);
					if(item.getUnlocalizedName().equals("item.null")){
						item.setUnlocalizedName(feild.getName().toLowerCase());
					}
					registerItem(item);
				}catch(Exception e){}
			}
		}
	}
	
	private void initBlocks(){
		for(Field feild : this.getClass().getFields()){
			if(feild.getType().equals(Block.class)){
				try{
					Block block = (Block) feild.get(Block.class);
					if(block.getUnlocalizedName().equals("tile.null")){
						block.setBlockName(feild.getName().toLowerCase());
					}
					registerBlock(block);
				}catch(Exception e){}
			}
		}
	}
	
	private void initEntites(){
		registerEntity(EntityPlane.class);
		
		registerChildEntity(EntityCore.class, null);
		registerChildEntity(EntitySeat.class, seat);
		registerChildEntity(EntityChest.class, Item.getItemFromBlock(Blocks.chest));
		registerChildEntity(EntityWheel.EntityWheelSmall.class, wheelSmall);
		registerChildEntity(EntityWheel.EntityWheelLarge.class, wheelLarge);
		registerChildEntity(EntitySkid.class, skid);
		registerChildEntity(EntityPontoon.class, pontoon);
		registerChildEntity(EntityPontoon.EntityPontoonDummy.class, pontoon);
		registerChildEntity(EntityPropeller.class, propeller);
		registerChildEntity(EntityEngineAircraftSmall.class, engineAircraftSmall);
		registerChildEntity(EntityEngineAircraftLarge.class, engineAircraftLarge);
		
		registerChildEntity(EntityBogie.class, bogie);
	}
	
	private void initPackets(){
		registerPacket(ChatPacket.class, ChatPacket.Handler.class, true, false);
		registerPacket(ServerDataPacket.class, ServerDataPacket.Handler.class, true, false);
		registerPacket(ServerSyncPacket.class, ServerSyncPacket.Handler.class, true, false);
		
		registerPacket(EntityClientRequestDataPacket.class, EntityClientRequestDataPacket.Handler.class, false, true);
		registerPacket(TileEntityClientRequestDataPacket.class, TileEntityClientRequestDataPacket.Handler.class, false, true);

		registerPacket(InstrumentPlanePacket.class, InstrumentPlanePacket.Handler.class, true, true);
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
	
	private void initRecipies(){
		this.initPartRecipes();
		this.initEngineRecipes();
		this.initFlightInstrumentRecipes();
	}
	
	private void initPartRecipes(){
		//Seats
		for(int i=0; i<96; ++i){
			registerRecpie(new ItemStack(seat, 1, i),
				" BA",
				" BA",
				"AAA",
				'A', new ItemStack(Blocks.wooden_slab, 1, i%6), 
				'B', new ItemStack(Blocks.wool, 1, i/6));
		}
		for(int i=0; i<6; ++i){
			registerRecpie(new ItemStack(seat, 1, 96 + i),
				" BA",
				" BA",
				"AAA",
				'A', new ItemStack(Blocks.wooden_slab, 1, i%6), 
				'B', new ItemStack(Items.leather));
		}
		
		//Wheels
		registerRecpie(new ItemStack(wheelSmall),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.wool, 
				'B', new ItemStack(Items.dye, 1, 0), 
				'C', Items.iron_ingot);
		registerRecpie(new ItemStack(wheelLarge),
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.wool, 
				'B', new ItemStack(Items.dye, 1, 0), 
				'C', Items.iron_ingot);
		//Skid
		registerRecpie(new ItemStack(skid),
				"A A",
				" A ",
				"  A",
				'A', Blocks.iron_bars);
		//Pontoon
		registerRecpie(new ItemStack(pontoon, 2),
				"AAA",
				"BBB",
				"AAA",
				'A', Items.iron_ingot, 
				'B', Blocks.wool);
		
		//Propeller bench
		registerRecpie(new ItemStack(propellerBench),
				"AAA",
				" BA",
				"ACA",
				'A', Items.iron_ingot,
				'B', Items.diamond,
				'C', Blocks.anvil);
				
	}
	
	private void initEngineRecipes(){
		//New engines
		registerRecpie(((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[0],
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.piston, 
				'B', Blocks.obsidian,
				'C', Items.iron_ingot);
		registerRecpie(((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[1],
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.piston, 
				'B', Blocks.obsidian,
				'C', Items.diamond);
		registerRecpie(((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[0],
				"ACA",
				"ACA",
				"ACA",
				'A', Blocks.piston, 
				'B', Blocks.obsidian,
				'C', Items.iron_ingot);
		registerRecpie(((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[1],
				"ACA",
				"ACA",
				"ACA",
				'A', Blocks.piston, 
				'B', Blocks.obsidian,
				'C', Items.diamond);
		
		//Repaired engines
		registerRecpie(((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[0],
				"B B",
				" C ",
				"B B",
				'B', Blocks.obsidian,
				'C', ((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[0]);
		registerRecpie(((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[1],
				"B B",
				" C ",
				"B B",
				'B', Blocks.obsidian,
				'C', ((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[1]);
		registerRecpie(((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[0],
				"B B",
				"BCB",
				"B B",
				'B', Blocks.obsidian,
				'C', ((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[0]);
		registerRecpie(((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[1],
				"B B",
				"BCB",
				"B B",
				'B', Blocks.obsidian,
				'C', ((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[1]);
	}
	
	private void initFlightInstrumentRecipes(){		
		registerRecpie(new ItemStack(pointerShort),
				" WW",
				" WW",
				"B  ",
				'W', new ItemStack(Items.dye, 1, 15), 
				'B', new ItemStack(Items.dye, 1, 0));
		
		registerRecpie(new ItemStack(pointerLong),
				"  W",
				" W ",
				"B  ",
				'W', new ItemStack(Items.dye, 1, 15), 
				'B', new ItemStack(Items.dye, 1, 0));
		
		
		registerRecpie(new ItemStack(flightInstrument, 16, 0),
				"III",
				"IGI",
				"III",
				'I', Items.iron_ingot, 
				'G', Blocks.glass_pane);
		registerRecpie(new ItemStack(flightInstrument, 1, 1),
				"LLL",
				"RRR",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', new ItemStack(Items.dye, 1, 4), 
				'R', new ItemStack(Items.dye, 1, 3));
		
		registerRecpie(new ItemStack(flightInstrument, 1, 2),
				"WLW",
				"WSW",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'S', pointerShort, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		registerRecpie(new ItemStack(flightInstrument, 1, 3),
				" W ",
				"WIW",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'I', Items.iron_ingot, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		registerRecpie(new ItemStack(flightInstrument, 1, 4),
				"R W",
				"YLG",
				"GBG",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'R', new ItemStack(Items.dye, 1, 1),
				'Y', new ItemStack(Items.dye, 1, 11), 
				'G', new ItemStack(Items.dye, 1, 10), 
				'W', new ItemStack(Items.dye, 1, 15));
		
		registerRecpie(new ItemStack(flightInstrument, 1, 5),
				"   ",
				"WIW",
				"WBW",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'I', Items.iron_ingot, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		registerRecpie(new ItemStack(flightInstrument, 1, 6),
				"WWW",
				" I ",
				"WBW",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'I', Items.iron_ingot, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		registerRecpie(new ItemStack(flightInstrument, 1, 7),
				"W W",
				" L ",
				"WBW",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		registerRecpie(new ItemStack(flightInstrument, 1, 8),
				"RYG",
				" LG",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'R', new ItemStack(Items.dye, 1, 1), 
				'Y', new ItemStack(Items.dye, 1, 11), 
				'G', new ItemStack(Items.dye, 1, 10), 
				'W', new ItemStack(Items.dye, 1, 15));
		
		registerRecpie(new ItemStack(flightInstrument, 1, 9),
				"GLG",
				"LGL",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'G', new ItemStack(Items.dye, 1, 10));

		//Instrument 10 does not exist
		
		registerRecpie(new ItemStack(flightInstrument, 1, 11),
				"W W",
				" L ",
				"WBR",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'R', new ItemStack(Items.dye, 1, 1), 
				'W', new ItemStack(Items.dye, 1, 15));
		
		registerRecpie(new ItemStack(flightInstrument, 1, 12),
				"RWW",
				" L ",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'R', new ItemStack(Items.dye, 1, 1), 
				'W', new ItemStack(Items.dye, 1, 15));
		
		registerRecpie(new ItemStack(flightInstrument, 1, 13),
				" W ",
				"WLW",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		registerRecpie(new ItemStack(flightInstrument, 1, 14),
				"YGR",
				" L ",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'Y', new ItemStack(Items.dye, 1, 11), 
				'G', new ItemStack(Items.dye, 1, 10), 
				'R', new ItemStack(Items.dye, 1, 1), 
				'W', new ItemStack(Items.dye, 1, 15));
				
		registerRecpie(new ItemStack(flightInstrument, 1, 15),
				"   ",
				"LGL",
				"RB ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong,  
				'G', new ItemStack(Items.dye, 1, 10), 
				'R', new ItemStack(Items.dye, 1, 1));
	}
	
	
	/**
	 * Registers the given item and adds it to the creative tab list.
	 * @param item
	 */
	private static void registerItem(Item item){
		item.setTextureName(MTS.MODID + ":" + item.getUnlocalizedName().substring(5).toLowerCase());
		GameRegistry.registerItem(item, item.getUnlocalizedName().substring(5));
		MTSRegistry.itemList.add(item);
	}
	
	/**
	 * Registers the given block and adds it to the creative tab list.
	 * Also adds the respective TileEntity if the block has one.
	 * @param block
	 */
	private static void registerBlock(Block block){
		block.setBlockTextureName(MTS.MODID + ":" + block.getUnlocalizedName().substring(5).toLowerCase());
		GameRegistry.registerBlock(block, block.getUnlocalizedName().substring(5));
		MTSRegistry.itemList.add(Item.getItemFromBlock(block));
		if(block instanceof ITileEntityProvider){
			Class<? extends TileEntity> tileEntityClass = ((ITileEntityProvider) block).createNewTileEntity(null, 0).getClass();
			GameRegistry.registerTileEntity(tileEntityClass, tileEntityClass.getSimpleName());
		}
	}

	/**
	 * Registers an entity.
	 * Optionally pairs the entity with an item for GUI operations.
	 * @param entityClass
	 * @param entityItem
	 */
	private static void registerEntity(Class entityClass){
		EntityRegistry.registerModEntity(entityClass, entityClass.getSimpleName().substring(6), entityNumber++, MTS.MODID, 80, 5, false);
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
			partClasses.put(entityItem.getUnlocalizedName(), entityClass);
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
		if(client)MTS.MFSNet.registerMessage(handlerClass, packetClass, ++packetNumber, Side.CLIENT);
		if(server)MTS.MFSNet.registerMessage(handlerClass, packetClass, ++packetNumber, Side.SERVER);
	}
	
	private static void registerRecpie(ItemStack output, Object...params){
		GameRegistry.addRecipe(output, params);
	}
}
