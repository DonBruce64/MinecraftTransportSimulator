package minecrafttransportsimulator.dataclasses;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.BlockPropellerBench;
import minecrafttransportsimulator.blocks.BlockSurveyFlag;
import minecrafttransportsimulator.blocks.BlockTrack;
import minecrafttransportsimulator.blocks.BlockTrackFake;
import minecrafttransportsimulator.entities.core.EntityChild;
import minecrafttransportsimulator.entities.core.EntityCore;
import minecrafttransportsimulator.entities.parts.EntityChest;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftLarge;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftSmall;
import minecrafttransportsimulator.entities.parts.EntityPontoon;
import minecrafttransportsimulator.entities.parts.EntityPontoonDummy;
import minecrafttransportsimulator.entities.parts.EntityPropeller;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntitySkid;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.items.ItemEngine;
import minecrafttransportsimulator.items.ItemEngine.ItemEngineAircraftLarge;
import minecrafttransportsimulator.items.ItemEngine.ItemEngineAircraftSmall;
import minecrafttransportsimulator.items.ItemFlightInstrument;
import minecrafttransportsimulator.items.ItemPlane;
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
import minecrafttransportsimulator.packets.general.InstrumentFlyerPacket;
import minecrafttransportsimulator.packets.general.ServerDataPacket;
import minecrafttransportsimulator.packets.general.ServerSyncPacket;
import minecrafttransportsimulator.packets.general.TileEntityClientRequestDataPacket;
import minecrafttransportsimulator.packets.general.TileEntitySyncPacket;
import minecrafttransportsimulator.planes.Comanche.EntityComanche;
import minecrafttransportsimulator.planes.MC172.EntityMC172;
import minecrafttransportsimulator.planes.PZLP11.EntityPZLP11;
import minecrafttransportsimulator.planes.Trimotor.EntityTrimotor;
import minecrafttransportsimulator.planes.Vulcanair.EntityVulcanair;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**Main registry class.  This class should be referenced by any class looking for
 * MTS items or blocks.  Adding new items and blocks is a simple as adding them
 * as a field; the init method automatically registers all items and blocks in the class
 * and orders them according to the order in which they were declared.
 * Recipes should be added using the methods in the CommonProxy, as they allow the class
 * to be static and work with any MCVersion.
 * 
 * @author don_bruce
 */
public class MTSRegistry{
	public static final MTSRegistry instance = new MTSRegistry();
	
	public static final Item planeMC172 = new ItemPlane(EntityMC172.class, 6).setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item planePZLP11 = new ItemPlane(EntityPZLP11.class, 1).setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item planeVulcanair = new ItemPlane(EntityVulcanair.class, 7).setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item planeTrimotor = new ItemPlane(EntityTrimotor.class, 15).setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item planeComanche = new ItemPlane(EntityComanche.class, 4).setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	
	public static final Item seat = new ItemSeat().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item propeller = new ItemPropeller().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item engineAircraftSmall = new ItemEngineAircraftSmall().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item engineAircraftLarge = new ItemEngineAircraftLarge().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item wheelSmall = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item wheelLarge = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item skid = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item pontoon = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item flightInstrument = new ItemFlightInstrument().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item pointerShort = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item pointerLong = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item flightInstrumentBase = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item wrench = new ItemWrench().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	public static final Item flightManual = new Item().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	
	public static final Block propellerBench = new BlockPropellerBench().setCreativeTab(MTSCreativeTabs.tabMTSPlanes);
	
	public static final Item track = new Item().setCreativeTab(MTSCreativeTabs.tabMTSTrains);
	
	public static final Block blockTrack = new BlockTrack();
	public static final Block blockTrackFake = new BlockTrackFake();
	public static final Block surveyFlag = new BlockSurveyFlag().setCreativeTab(MTSCreativeTabs.tabMTSTrains);
	
	public static List<Item> itemList = new ArrayList<Item>();
	
	/**
	 * Maps child classes to the items that spawn them.
	 * Useful for inventory operations.
	 */
	public static Map<Item, Class<? extends EntityChild>> entityItems = new HashMap<Item, Class<? extends EntityChild>>();
	
	public void init(){
		initItems();
		initBlocks();
		initEntites();
		initPackets();
		initRecipies();
	}
	
	private void initItems(){
		for(Field feild : this.getClass().getFields()){
			if(feild.getType().equals(Item.class)){
				try{
					Item item = (Item) feild.get(Item.class);
					if(item.getUnlocalizedName().equals("item.null")){
						item.setUnlocalizedName(feild.getName().toLowerCase());
					}
					MTS.proxy.registerItem(item);
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
					MTS.proxy.registerBlock(block);
				}catch(Exception e){}
			}
		}
	}
	
	private void initEntites(){
		MTS.proxy.registerEntity(EntityMC172.class);
		MTS.proxy.registerEntity(EntityPZLP11.class);
		MTS.proxy.registerEntity(EntityVulcanair.class);
		MTS.proxy.registerEntity(EntityTrimotor.class);
		MTS.proxy.registerEntity(EntityComanche.class);
		
		MTS.proxy.registerChildEntity(EntityCore.class, null);
		MTS.proxy.registerChildEntity(EntitySeat.class, seat);
		MTS.proxy.registerChildEntity(EntityChest.class, Item.getItemFromBlock(Blocks.chest));
		MTS.proxy.registerChildEntity(EntityWheel.EntityWheelSmall.class, wheelSmall);
		MTS.proxy.registerChildEntity(EntityWheel.EntityWheelLarge.class, wheelLarge);
		MTS.proxy.registerChildEntity(EntitySkid.class, skid);
		MTS.proxy.registerChildEntity(EntityPontoon.class, pontoon);
		MTS.proxy.registerChildEntity(EntityPontoonDummy.class, pontoon);
		MTS.proxy.registerChildEntity(EntityPropeller.class, propeller);
		MTS.proxy.registerChildEntity(EntityEngineAircraftSmall.class, engineAircraftSmall);
		MTS.proxy.registerChildEntity(EntityEngineAircraftLarge.class, engineAircraftLarge);
	}
	
	private void initPackets(){
		MTS.proxy.registerPacket(ChatPacket.class, ChatPacket.Handler.class, true, false);
		MTS.proxy.registerPacket(ServerDataPacket.class, ServerDataPacket.Handler.class, true, false);
		MTS.proxy.registerPacket(ServerSyncPacket.class, ServerSyncPacket.Handler.class, true, false);
		
		MTS.proxy.registerPacket(EntityClientRequestDataPacket.class, EntityClientRequestDataPacket.Handler.class, false, true);
		MTS.proxy.registerPacket(TileEntityClientRequestDataPacket.class, TileEntityClientRequestDataPacket.Handler.class, false, true);

		MTS.proxy.registerPacket(InstrumentFlyerPacket.class, InstrumentFlyerPacket.Handler.class, true, true);
		MTS.proxy.registerPacket(TileEntitySyncPacket.class, TileEntitySyncPacket.Handler.class, true, true);
		
		MTS.proxy.registerPacket(AileronPacket.class, AileronPacket.Handler.class, true, true);
		MTS.proxy.registerPacket(BrakePacket.class, BrakePacket.Handler.class, true, true);
		MTS.proxy.registerPacket(ElevatorPacket.class, ElevatorPacket.Handler.class, true, true);
		MTS.proxy.registerPacket(EnginePacket.class, EnginePacket.Handler.class, true, true);
		MTS.proxy.registerPacket(FlapPacket.class, FlapPacket.Handler.class, true, true);
		MTS.proxy.registerPacket(LightPacket.class, LightPacket.Handler.class, true, true);
		MTS.proxy.registerPacket(RudderPacket.class, RudderPacket.Handler.class, true, true);
		MTS.proxy.registerPacket(ThrottlePacket.class, ThrottlePacket.Handler.class, true, true);
		MTS.proxy.registerPacket(TrimPacket.class, TrimPacket.Handler.class, true, true);
	}
	
	private void initRecipies(){
		this.initPlaneRecipes();
		this.initPartRecipes();
		this.initEngineRecipes();
		this.initFlightInstrumentRecipes();
	}
	
	private void initPlaneRecipes(){	
		//MC172
		for(int i=0; i<6; ++i){
			MTS.proxy.registerRecpie(new ItemStack(planeMC172, 1, i),
				"AAA",
				" B ",
				"ABA",
				'A', new ItemStack(Blocks.wooden_slab, 1, i), 
				'B', new ItemStack(Blocks.planks, 1, i));
		}
		
		//PZLP11
		MTS.proxy.registerRecpie(new ItemStack(planePZLP11),
				"AAA",
				" B ",
				"ABA",
				'A', Items.iron_ingot, 
				'B', Blocks.iron_bars);
		
		//Vulcanair
		MTS.proxy.registerRecpie(new ItemStack(planeVulcanair, 1, 0),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 
				'C', new ItemStack(Items.dye, 1, 15));
		MTS.proxy.registerRecpie(new ItemStack(planeVulcanair, 1, 1),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 
				'C', new ItemStack(Items.dye, 1, 14));
		MTS.proxy.registerRecpie(new ItemStack(planeVulcanair, 1, 2),
				"AAA",
				"CAD",
				"AAA",
				'A', Items.iron_ingot, 
				'C', new ItemStack(Items.dye, 1, 1),
				'D', new ItemStack(Items.dye, 1, 7));
		MTS.proxy.registerRecpie(new ItemStack(planeVulcanair, 1, 3),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 
				'C', new ItemStack(Items.dye, 1, 1));
		MTS.proxy.registerRecpie(new ItemStack(planeVulcanair, 1, 4),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 
				'C', new ItemStack(Items.dye, 1, 10));
		MTS.proxy.registerRecpie(new ItemStack(planeVulcanair, 1, 5),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 
				'C', new ItemStack(Items.dye, 1, 0));
		MTS.proxy.registerRecpie(new ItemStack(planeVulcanair, 1, 6),
				"AAA",
				"CAD",
				"AAA",
				'A', Items.iron_ingot, 
				'C', new ItemStack(Items.dye, 1, 15),
				'D', new ItemStack(Items.dye, 1, 14));
		
		//Trimotor
		for(byte i=0; i<16; ++i){
			MTS.proxy.registerRecpie(new ItemStack(planeTrimotor, 1, i),
					"AAA",
					"CB ",
					"AAA",
					'A', Items.iron_ingot, 
					'B', Blocks.iron_block,
					'D', new ItemStack(Items.dye, 1, i));
		}
	}
	
	private void initPartRecipes(){
		//Seats
		for(int i=0; i<96; ++i){
			MTS.proxy.registerRecpie(new ItemStack(seat, 1, i),
				" BA",
				" BA",
				"AAA",
				'A', new ItemStack(Blocks.wooden_slab, 1, i%6), 
				'B', new ItemStack(Blocks.wool, 1, i/6));
		}
		for(int i=0; i<6; ++i){
			MTS.proxy.registerRecpie(new ItemStack(seat, 1, 96 + i),
				" BA",
				" BA",
				"AAA",
				'A', new ItemStack(Blocks.wooden_slab, 1, i%6), 
				'B', new ItemStack(Items.leather));
		}
		
		//Wheels
		MTS.proxy.registerRecpie(new ItemStack(wheelSmall),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.wool, 
				'B', new ItemStack(Items.dye, 1, 0), 
				'C', Items.iron_ingot);
		MTS.proxy.registerRecpie(new ItemStack(wheelLarge),
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.wool, 
				'B', new ItemStack(Items.dye, 1, 0), 
				'C', Items.iron_ingot);
		//Skid
		MTS.proxy.registerRecpie(new ItemStack(skid),
				"A A",
				" A ",
				"  A",
				'A', Blocks.iron_bars);
		//Pontoon
		MTS.proxy.registerRecpie(new ItemStack(pontoon, 2),
				"AAA",
				"BBB",
				"AAA",
				'A', Items.iron_ingot, 
				'B', Blocks.wool);
		
		//Propeller bench
		MTS.proxy.registerRecpie(new ItemStack(propellerBench),
				"AAA",
				" BA",
				"ACA",
				'A', Items.iron_ingot,
				'B', Items.diamond,
				'C', Blocks.anvil);
				
	}
	
	private void initEngineRecipes(){
		//New engines
		MTS.proxy.registerRecpie(((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[0],
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.piston, 
				'B', Blocks.obsidian,
				'C', Items.iron_ingot);
		MTS.proxy.registerRecpie(((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[1],
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.piston, 
				'B', Blocks.obsidian,
				'C', Items.diamond);
		MTS.proxy.registerRecpie(((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[0],
				"ACA",
				"ACA",
				"ACA",
				'A', Blocks.piston, 
				'B', Blocks.obsidian,
				'C', Items.iron_ingot);
		MTS.proxy.registerRecpie(((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[1],
				"ACA",
				"ACA",
				"ACA",
				'A', Blocks.piston, 
				'B', Blocks.obsidian,
				'C', Items.diamond);
		
		//Repaired engines
		MTS.proxy.registerRecpie(((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[0],
				"B B",
				" C ",
				"B B",
				'B', Blocks.obsidian,
				'C', ((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[0]);
		MTS.proxy.registerRecpie(((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[1],
				"B B",
				" C ",
				"B B",
				'B', Blocks.obsidian,
				'C', ((ItemEngine) MTSRegistry.engineAircraftSmall).getAllPossibleStacks()[1]);
		MTS.proxy.registerRecpie(((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[0],
				"B B",
				"BCB",
				"B B",
				'B', Blocks.obsidian,
				'C', ((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[0]);
		MTS.proxy.registerRecpie(((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[1],
				"B B",
				"BCB",
				"B B",
				'B', Blocks.obsidian,
				'C', ((ItemEngine) MTSRegistry.engineAircraftLarge).getAllPossibleStacks()[1]);
	}
	
	private void initFlightInstrumentRecipes(){
		MTS.proxy.registerRecpie(new ItemStack(flightInstrumentBase, 16),
				"III",
				"IGI",
				"III",
				'I', Items.iron_ingot, 
				'G', Blocks.glass_pane);
		
		MTS.proxy.registerRecpie(new ItemStack(pointerShort),
				" WW",
				" WW",
				"B  ",
				'W', new ItemStack(Items.dye, 1, 15), 
				'B', new ItemStack(Items.dye, 1, 0));
		
		MTS.proxy.registerRecpie(new ItemStack(pointerLong),
				"  W",
				" W ",
				"B  ",
				'W', new ItemStack(Items.dye, 1, 15), 
				'B', new ItemStack(Items.dye, 1, 0));
		
		MTS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 0),
				"LLL",
				"RRR",
				" B ",
				'B', flightInstrumentBase, 
				'L', new ItemStack(Items.dye, 1, 4), 
				'R', new ItemStack(Items.dye, 1, 3));
		
		MTS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 1),
				"WLW",
				"WSW",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'S', pointerShort, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MTS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 2),
				" W ",
				"WIW",
				" B ",
				'B', flightInstrumentBase, 
				'I', Items.iron_ingot, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MTS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 3),
				"R W",
				"YLG",
				"GBG",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'R', new ItemStack(Items.dye, 1, 1),
				'Y', new ItemStack(Items.dye, 1, 11), 
				'G', new ItemStack(Items.dye, 1, 10), 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MTS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 4),
				"   ",
				"WIW",
				"WBW",
				'B', flightInstrumentBase, 
				'I', Items.iron_ingot, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MTS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 5),
				"WWW",
				" I ",
				"WBW",
				'B', flightInstrumentBase, 
				'I', Items.iron_ingot, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MTS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 6),
				"W W",
				" L ",
				"WBW",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MTS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 7),
				"RYG",
				" LG",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'R', new ItemStack(Items.dye, 1, 1), 
				'Y', new ItemStack(Items.dye, 1, 11), 
				'G', new ItemStack(Items.dye, 1, 10), 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MTS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 8),
				"GLG",
				"LGL",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'G', new ItemStack(Items.dye, 1, 10));

		//Instrument 9 does not exist
		
		MTS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 10),
				"W W",
				" L ",
				"WBR",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'R', new ItemStack(Items.dye, 1, 1), 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MTS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 11),
				"RWW",
				" L ",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'R', new ItemStack(Items.dye, 1, 1), 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MTS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 12),
				" W ",
				"WLW",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MTS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 13),
				"YGR",
				" L ",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'Y', new ItemStack(Items.dye, 1, 11), 
				'G', new ItemStack(Items.dye, 1, 10), 
				'R', new ItemStack(Items.dye, 1, 1), 
				'W', new ItemStack(Items.dye, 1, 15));
		
		//Instrument 14 does not exist
		
		MTS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 15),
				"   ",
				"LGL",
				"RB ",
				'B', flightInstrumentBase, 
				'L', pointerLong,  
				'G', new ItemStack(Items.dye, 1, 10), 
				'R', new ItemStack(Items.dye, 1, 1));
	}
}
