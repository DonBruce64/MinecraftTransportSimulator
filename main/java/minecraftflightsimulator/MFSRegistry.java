package minecraftflightsimulator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecraftflightsimulator.blocks.BlockPropellerBench;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.core.EntityCore;
import minecraftflightsimulator.entities.parts.EntityEngine.EngineTypes;
import minecraftflightsimulator.entities.parts.EntityEngineAircraft;
import minecraftflightsimulator.entities.parts.EntityPlaneChest;
import minecraftflightsimulator.entities.parts.EntityPontoon;
import minecraftflightsimulator.entities.parts.EntityPontoonDummy;
import minecraftflightsimulator.entities.parts.EntityPropeller;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.entities.parts.EntitySkid;
import minecraftflightsimulator.entities.parts.EntityWheelLarge;
import minecraftflightsimulator.entities.parts.EntityWheelSmall;
import minecraftflightsimulator.items.ItemEngine;
import minecraftflightsimulator.items.ItemFlightInstrument;
import minecraftflightsimulator.items.ItemPlane;
import minecraftflightsimulator.items.ItemPropeller;
import minecraftflightsimulator.items.ItemSeat;
import minecraftflightsimulator.packets.control.AileronPacket;
import minecraftflightsimulator.packets.control.BrakePacket;
import minecraftflightsimulator.packets.control.ElevatorPacket;
import minecraftflightsimulator.packets.control.EnginePacket;
import minecraftflightsimulator.packets.control.FlapPacket;
import minecraftflightsimulator.packets.control.LightPacket;
import minecraftflightsimulator.packets.control.RudderPacket;
import minecraftflightsimulator.packets.control.ThrottlePacket;
import minecraftflightsimulator.packets.control.TrimPacket;
import minecraftflightsimulator.packets.general.ChatPacket;
import minecraftflightsimulator.packets.general.ClientRequestDataPacket;
import minecraftflightsimulator.packets.general.GUIPacket;
import minecraftflightsimulator.packets.general.PropellerBenchTilepdatePacket;
import minecraftflightsimulator.packets.general.ServerDataPacket;
import minecraftflightsimulator.packets.general.ServerSyncPacket;
import minecraftflightsimulator.planes.MC172.EntityMC172;
import minecraftflightsimulator.planes.PZLP11.EntityPZLP11;
import minecraftflightsimulator.planes.Trimotor.EntityTrimotor;
import minecraftflightsimulator.planes.Vulcanair.EntityVulcanair;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**Main registry class.  This class should be referenced by any class looking for
 * MFS items or blocks.  Adding new items and blocks is a simple as adding them
 * as a field; the init method automatically registers all items and blocks in the class.
 * Recipes should be added using the methods in the CommonProxy, as they allow the class
 * to be static and work with any MCVersion.
 * 
 * @author don_bruce
 */
public class MFSRegistry{
	public static final MFSRegistry instance = new MFSRegistry();
	
	public static final Item planeMC172 = new ItemPlane(EntityMC172.class, 6);
	public static final Item planePZLP11 = new ItemPlane(EntityPZLP11.class, 1);
	public static final Item planeVulcanair = new ItemPlane(EntityVulcanair.class, 7);
	public static final Item planeTrimotor = new ItemPlane(EntityTrimotor.class, 15);
	public static final Item seat = new ItemSeat();
	public static final Item propeller = new ItemPropeller();
	public static final Item pointerShort = new Item();
	public static final Item pointerLong = new Item();
	public static final Item wheelSmall = new Item();
	public static final Item wheelLarge = new Item();
	public static final Item skid = new Item();
	public static final Item pontoon = new Item();
	public static final Item engineSmall = new ItemEngine(EngineTypes.PLANE_SMALL);
	public static final Item engineLarge = new ItemEngine(EngineTypes.PLANE_LARGE);
	public static final Item flightInstrument = new ItemFlightInstrument();
	public static final Item flightInstrumentBase = new Item();
	public static final Block blockPropellerBench = new BlockPropellerBench();
	
	public static List<Item> itemList = new ArrayList<Item>();
	public static Map<Class<? extends EntityChild>, Item> entityItems = new HashMap<Class<? extends EntityChild>, Item>();
	
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
						item.setUnlocalizedName(feild.getName().substring(0, 1).toUpperCase() + feild.getName().substring(1));
					}
					MFS.proxy.registerItem(item);
				}catch(Exception e){}
			}
		}
	}
	
	private void initBlocks(){
		for(Field feild : this.getClass().getFields()){
			if(feild.getType().equals(Block.class)){
				try{
					Block block = (Block) feild.get(Block.class);
					MFS.proxy.registerBlock(block);
				}catch(Exception e){}
			}
		}
	}
	
	private void initEntites(){
		MFS.proxy.registerEntity(EntityMC172.class, null);
		MFS.proxy.registerEntity(EntityPZLP11.class, null);
		MFS.proxy.registerEntity(EntityVulcanair.class, null);
		MFS.proxy.registerEntity(EntityTrimotor.class, null);
		
		MFS.proxy.registerEntity(EntityCore.class, null);
		MFS.proxy.registerEntity(EntitySeat.class, seat);
		MFS.proxy.registerEntity(EntityPlaneChest.class, new ItemStack(Blocks.chest).getItem());
		MFS.proxy.registerEntity(EntityWheelSmall.class, wheelSmall);
		MFS.proxy.registerEntity(EntityWheelLarge.class, wheelLarge);
		MFS.proxy.registerEntity(EntitySkid.class, skid);
		MFS.proxy.registerEntity(EntityPontoon.class, pontoon);
		MFS.proxy.registerEntity(EntityPontoonDummy.class, pontoon);
		MFS.proxy.registerEntity(EntityPropeller.class, propeller);
		MFS.proxy.registerEntity(EntityEngineAircraft.class, null);
	}
	
	private void initPackets(){
		MFS.proxy.registerPacket(ChatPacket.class, ChatPacket.Handler.class, true, false);
		MFS.proxy.registerPacket(ServerDataPacket.class, ServerDataPacket.Handler.class, true, false);
		MFS.proxy.registerPacket(ServerSyncPacket.class, ServerSyncPacket.Handler.class, true, false);
		
		MFS.proxy.registerPacket(GUIPacket.class, GUIPacket.Handler.class, false, true);
		MFS.proxy.registerPacket(ClientRequestDataPacket.class, ClientRequestDataPacket.Handler.class, false, true);

		MFS.proxy.registerPacket(PropellerBenchTilepdatePacket.class, PropellerBenchTilepdatePacket.Handler.class, true, true);
		MFS.proxy.registerPacket(AileronPacket.class, AileronPacket.Handler.class, true, true);
		MFS.proxy.registerPacket(BrakePacket.class, BrakePacket.Handler.class, true, true);
		MFS.proxy.registerPacket(ElevatorPacket.class, ElevatorPacket.Handler.class, true, true);
		MFS.proxy.registerPacket(EnginePacket.class, EnginePacket.Handler.class, true, true);
		MFS.proxy.registerPacket(FlapPacket.class, FlapPacket.Handler.class, true, true);
		MFS.proxy.registerPacket(LightPacket.class, LightPacket.Handler.class, true, true);
		MFS.proxy.registerPacket(RudderPacket.class, RudderPacket.Handler.class, true, true);
		MFS.proxy.registerPacket(ThrottlePacket.class, ThrottlePacket.Handler.class, true, true);
		MFS.proxy.registerPacket(TrimPacket.class, TrimPacket.Handler.class, true, true);
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
			MFS.proxy.registerRecpie(new ItemStack(planeMC172, 1, i),
				"AAA",
				" B ",
				"ABA",
				'A', new ItemStack(Blocks.wooden_slab, 1, i), 
				'B', new ItemStack(Blocks.planks, 1, i));
		}
		
		//PZLP11
		MFS.proxy.registerRecpie(new ItemStack(planePZLP11),
				"AAA",
				" B ",
				"ABA",
				'A', Items.iron_ingot, 
				'B', Blocks.iron_bars);
		
		//Vulcanair
		MFS.proxy.registerRecpie(new ItemStack(planeVulcanair, 1, 0),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 
				'C', new ItemStack(Items.dye, 1, 15));
		MFS.proxy.registerRecpie(new ItemStack(planeVulcanair, 1, 1),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 
				'C', new ItemStack(Items.dye, 1, 14));
		MFS.proxy.registerRecpie(new ItemStack(planeVulcanair, 1, 2),
				"AAA",
				"CAD",
				"AAA",
				'A', Items.iron_ingot, 
				'C', new ItemStack(Items.dye, 1, 1),
				'D', new ItemStack(Items.dye, 1, 7));
		MFS.proxy.registerRecpie(new ItemStack(planeVulcanair, 1, 3),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 
				'C', new ItemStack(Items.dye, 1, 1));
		MFS.proxy.registerRecpie(new ItemStack(planeVulcanair, 1, 4),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 
				'C', new ItemStack(Items.dye, 1, 10));
		MFS.proxy.registerRecpie(new ItemStack(planeVulcanair, 1, 5),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 
				'C', new ItemStack(Items.dye, 1, 0));
		MFS.proxy.registerRecpie(new ItemStack(planeVulcanair, 1, 6),
				"AAA",
				"CAD",
				"AAA",
				'A', Items.iron_ingot, 
				'C', new ItemStack(Items.dye, 1, 15),
				'D', new ItemStack(Items.dye, 1, 14));
		
		//Trimotor
		for(byte i=0; i<16; ++i){
			MFS.proxy.registerRecpie(new ItemStack(planeTrimotor, 1, i),
					"AAA",
					"CB ",
					"AAA",
					'A', Items.iron_ingot, 
					'B', Blocks.iron_block,
					'D', new ItemStack(Items.dye, 1, i));
		}
	}
	
	private void initPartRecipes(){
		//Seat
		for(int i=0; i<6; ++i){
			for(int j=0; j<16; ++j){
				MFS.proxy.registerRecpie(new ItemStack(seat, 1, (i << 4) + j),
					" BA",
					" BA",
					"AAA",
					'A', new ItemStack(Blocks.wooden_slab, 1, i), 
					'B', new ItemStack(Blocks.wool, 1, j));
			}
		}
		
		//Wheels
		MFS.proxy.registerRecpie(new ItemStack(wheelSmall),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.wool, 
				'B', new ItemStack(Items.dye, 1, 0), 
				'C', Items.iron_ingot);
		MFS.proxy.registerRecpie(new ItemStack(wheelLarge),
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.wool, 
				'B', new ItemStack(Items.dye, 1, 0), 
				'C', Items.iron_ingot);
		//Skid
		MFS.proxy.registerRecpie(new ItemStack(skid),
				"A A",
				" A ",
				"  A",
				'A', Blocks.iron_bars);
		//Pontoon
		MFS.proxy.registerRecpie(new ItemStack(pontoon, 2),
				"AAA",
				"BBB",
				"AAA",
				'A', Items.iron_ingot, 
				'B', Blocks.wool);
		
		//Propeller bench
		MFS.proxy.registerRecpie(new ItemStack(blockPropellerBench),
				"AAA",
				" BA",
				"ACA",
				'A', Items.iron_ingot,
				'B', Items.diamond,
				'C', Blocks.anvil);
				
	}
	
	private void initEngineRecipes(){
		//New engines
		MFS.proxy.registerRecpie(new ItemStack(engineSmall, 1, 2805),
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.piston, 
				'B', Blocks.obsidian,
				'C', Items.iron_ingot);
		MFS.proxy.registerRecpie(new ItemStack(engineSmall, 1, 3007),
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.piston, 
				'B', Blocks.obsidian,
				'C', Items.diamond);
		MFS.proxy.registerRecpie(new ItemStack(engineLarge, 1, 2907),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.piston, 
				'B', Blocks.obsidian,
				'C', Items.iron_ingot);
		MFS.proxy.registerRecpie(new ItemStack(engineLarge, 1, 3210),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.piston, 
				'B', Blocks.obsidian,
				'C', Items.diamond);
		
		//Repaired engines
		MFS.proxy.registerRecpie(new ItemStack(engineSmall, 1, 2805),
				"B B",
				" C ",
				"B B",
				'B', Blocks.obsidian,
				'C', new ItemStack(engineSmall, 1, 2805));
		MFS.proxy.registerRecpie(new ItemStack(engineSmall, 1, 3007),
				"B B",
				" C ",
				"B B",
				'B', Blocks.obsidian,
				'C', new ItemStack(engineSmall, 1, 3007));
		MFS.proxy.registerRecpie(new ItemStack(engineLarge, 1, 2907),
				"B B",
				"BCB",
				"B B",
				'B', Blocks.obsidian,
				'C', new ItemStack(engineLarge, 1, 2907));
		MFS.proxy.registerRecpie(new ItemStack(engineLarge, 1, 3210),
				"B B",
				"BCB",
				"B B",
				'B', Blocks.obsidian,
				'C', new ItemStack(engineLarge, 1, 3210));
	}
	
	private void initFlightInstrumentRecipes(){
		MFS.proxy.registerRecpie(new ItemStack(flightInstrumentBase, 16),
				"III",
				"IGI",
				"III",
				'I', Items.iron_ingot, 
				'G', Blocks.glass_pane);
		
		MFS.proxy.registerRecpie(new ItemStack(pointerShort),
				" WW",
				" WW",
				"B  ",
				'W', new ItemStack(Items.dye, 1, 15), 
				'B', new ItemStack(Items.dye, 1, 0));
		
		MFS.proxy.registerRecpie(new ItemStack(pointerLong),
				"  W",
				" W ",
				"B  ",
				'W', new ItemStack(Items.dye, 1, 15), 
				'B', new ItemStack(Items.dye, 1, 0));
		
		MFS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 0),
				"LLL",
				"RRR",
				" B ",
				'B', flightInstrumentBase, 
				'L', new ItemStack(Items.dye, 1, 4), 
				'R', new ItemStack(Items.dye, 1, 3));
		
		MFS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 1),
				"WLW",
				"WSW",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'S', pointerShort, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MFS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 2),
				" W ",
				"WIW",
				" B ",
				'B', flightInstrumentBase, 
				'I', Items.iron_ingot, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MFS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 3),
				"R W",
				"YLG",
				"GBG",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'R', new ItemStack(Items.dye, 1, 1),
				'Y', new ItemStack(Items.dye, 1, 11), 
				'G', new ItemStack(Items.dye, 1, 10), 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MFS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 4),
				"   ",
				"WIW",
				"WBW",
				'B', flightInstrumentBase, 
				'I', Items.iron_ingot, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MFS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 5),
				"WWW",
				" I ",
				"WBW",
				'B', flightInstrumentBase, 
				'I', Items.iron_ingot, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MFS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 6),
				"W W",
				" L ",
				"WBW",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MFS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 7),
				"RYG",
				" LG",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'R', new ItemStack(Items.dye, 1, 1), 
				'Y', new ItemStack(Items.dye, 1, 11), 
				'G', new ItemStack(Items.dye, 1, 10), 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MFS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 8),
				"GLG",
				"LGL",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'G', new ItemStack(Items.dye, 1, 10));

		//Instrument 9 does not exist
		
		MFS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 10),
				"W W",
				" L ",
				"WBR",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'R', new ItemStack(Items.dye, 1, 1), 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MFS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 11),
				"RWW",
				" L ",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'R', new ItemStack(Items.dye, 1, 1), 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MFS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 12),
				" W ",
				"WLW",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'W', new ItemStack(Items.dye, 1, 15));
		
		MFS.proxy.registerRecpie(new ItemStack(flightInstrument, 1, 13),
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
	}
}
