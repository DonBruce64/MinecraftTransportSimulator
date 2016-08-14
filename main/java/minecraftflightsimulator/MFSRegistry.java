package minecraftflightsimulator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecraftflightsimulator.blocks.BlockPropellerBench;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.core.EntityCore;
import minecraftflightsimulator.entities.parts.EntityEngineLarge;
import minecraftflightsimulator.entities.parts.EntityEngineSmall;
import minecraftflightsimulator.entities.parts.EntityPlaneChest;
import minecraftflightsimulator.entities.parts.EntityPontoon;
import minecraftflightsimulator.entities.parts.EntityPontoonDummy;
import minecraftflightsimulator.entities.parts.EntityPropeller;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.entities.parts.EntitySkid;
import minecraftflightsimulator.entities.parts.EntityWheelLarge;
import minecraftflightsimulator.entities.parts.EntityWheelSmall;
import minecraftflightsimulator.items.ItemEngine;
import minecraftflightsimulator.items.ItemEngine.EngineTypes;
import minecraftflightsimulator.items.ItemFlightInstrument;
import minecraftflightsimulator.items.ItemPlane;
import minecraftflightsimulator.items.ItemPropeller;
import minecraftflightsimulator.items.ItemSeat;
import minecraftflightsimulator.packets.control.AileronPacket;
import minecraftflightsimulator.packets.control.BrakePacket;
import minecraftflightsimulator.packets.control.ElevatorPacket;
import minecraftflightsimulator.packets.control.EnginePacket;
import minecraftflightsimulator.packets.control.FlapPacket;
import minecraftflightsimulator.packets.control.RudderPacket;
import minecraftflightsimulator.packets.control.ThrottlePacket;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.registry.GameRegistry;

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
	public static final Item engineSmall = new ItemEngine(EngineTypes.SMALL);
	public static final Item engineLarge = new ItemEngine(EngineTypes.LARGE);
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
		MFS.proxy.registerEntity(EntityPlaneChest.class, MFS.proxy.getStackByItemName("chest", -1).getItem());
		MFS.proxy.registerEntity(EntityWheelSmall.class, wheelSmall);
		MFS.proxy.registerEntity(EntityWheelLarge.class, wheelLarge);
		MFS.proxy.registerEntity(EntitySkid.class, skid);
		MFS.proxy.registerEntity(EntityPontoon.class, pontoon);
		MFS.proxy.registerEntity(EntityPontoonDummy.class, null);
		MFS.proxy.registerEntity(EntityPropeller.class, propeller);
		MFS.proxy.registerEntity(EntityEngineSmall.class, engineSmall);
		MFS.proxy.registerEntity(EntityEngineLarge.class, engineLarge);
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
		MFS.proxy.registerPacket(RudderPacket.class, RudderPacket.Handler.class, true, true);
		MFS.proxy.registerPacket(ThrottlePacket.class, ThrottlePacket.Handler.class, true, true);
	}
	
	ItemStack ingotStack = MFS.proxy.getStackByItemName("iron_ingot", -1);
	ItemStack barsStack = MFS.proxy.getStackByItemName("iron_bars", -1);
	ItemStack blockStack = MFS.proxy.getStackByItemName("iron_block", -1);
	ItemStack diamondStack = MFS.proxy.getStackByItemName("diamond", -1);
	ItemStack obsidianStack = MFS.proxy.getStackByItemName("obsidian", -1);
	ItemStack pistonStack = MFS.proxy.getStackByItemName("piston", -1);
	ItemStack anyWoolStack = MFS.proxy.getStackByItemName("wool", -1);
	ItemStack[] coloredWoolStack = new ItemStack[16];
	ItemStack[] woodSlabStack = new ItemStack[6];
	ItemStack[] dyeStack = new ItemStack[16];
	
	private void initRecipies(){
		for(byte i=0; i<6; ++i){woodSlabStack[i] = MFS.proxy.getStackByItemName("wooden_slab", i);}
		for(byte i=0; i<16; ++i){dyeStack[i] = MFS.proxy.getStackByItemName("dye", i);}
		for(byte i=0; i<16; ++i){coloredWoolStack[i] = MFS.proxy.getStackByItemName("wool", i);}
		this.initPlaneRecipes();
		this.initPartRecipes();
		this.initEngineRecipes();
		this.initFlightInstrumentRecipes();
	}
	
	private void initPlaneRecipes(){	
		//MC172
		for(int i=0; i<6; ++i){
			GameRegistry.addRecipe(new ItemStack(planeMC172, 1, i),
				"AAA",
				" B ",
				"ABA",
				'A', woodSlabStack[i], 
				'B', MFS.proxy.getStackByItemName("planks", i));
		}
		
		//PZLP11
		GameRegistry.addRecipe(new ItemStack(planePZLP11),
				"AAA",
				" B ",
				"ABA",
				'A', ingotStack, 
				'B', barsStack);
		
		//Vulcanair
		GameRegistry.addRecipe(new ItemStack(planeVulcanair, 1, 0),
				"AAA",
				"CAC",
				"AAA",
				'A', ingotStack, 
				'C', dyeStack[15]);
		GameRegistry.addRecipe(new ItemStack(planeVulcanair, 1, 1),
				"AAA",
				"CAC",
				"AAA",
				'A', ingotStack, 
				'C', dyeStack[14]);
		GameRegistry.addRecipe(new ItemStack(planeVulcanair, 1, 2),
				"AAA",
				"CAD",
				"AAA",
				'A', ingotStack, 
				'C', dyeStack[1],
				'D', dyeStack[7]);
		GameRegistry.addRecipe(new ItemStack(planeVulcanair, 1, 3),
				"AAA",
				"CAC",
				"AAA",
				'A', ingotStack, 
				'C', dyeStack[1]);
		GameRegistry.addRecipe(new ItemStack(planeVulcanair, 1, 4),
				"AAA",
				"CAC",
				"AAA",
				'A', ingotStack, 
				'C', dyeStack[10]);
		GameRegistry.addRecipe(new ItemStack(planeVulcanair, 1, 5),
				"AAA",
				"CAC",
				"AAA",
				'A', ingotStack, 
				'C', dyeStack[0]);
		GameRegistry.addRecipe(new ItemStack(planeVulcanair, 1, 6),
				"AAA",
				"CAD",
				"AAA",
				'A', ingotStack, 
				'C', dyeStack[15],
				'D', dyeStack[14]);
		
		//Trimotor
		for(byte i=0; i<16; ++i){
			GameRegistry.addRecipe(new ItemStack(planeTrimotor, 1, i),
					"AAA",
					"CB ",
					"AAA",
					'A', ingotStack, 
					'B', blockStack,
					'D', dyeStack[i]);
		}
	}
	
	private void initPartRecipes(){
		//Seat
		for(int i=0; i<6; ++i){
			for(int j=0; j<16; ++j){
				GameRegistry.addRecipe(new ItemStack(seat, 1, (i << 4) + j),
					" BA",
					" BA",
					"AAA",
					'A', woodSlabStack[i], 
					'B', coloredWoolStack[j]);
			}
		}
		
		//Wheels
		GameRegistry.addRecipe(new ItemStack(wheelSmall, 1, 0),
				"ABA",
				"ACA",
				"ABA",
				'A', anyWoolStack, 
				'B', dyeStack[0], 
				'C', ingotStack);
		GameRegistry.addRecipe(new ItemStack(wheelLarge, 1, 1),
				"ABA",
				"BCB",
				"ABA",
				'A', anyWoolStack, 
				'B', dyeStack[0], 
				'C', ingotStack);
		//Skid
		GameRegistry.addRecipe(new ItemStack(skid),
				"A A",
				" A ",
				"  A",
				'A', barsStack);
		//Pontoon
		GameRegistry.addRecipe(new ItemStack(pontoon, 2),
				"AAA",
				"BBB",
				"AAA",
				'A', ingotStack, 
				'B', anyWoolStack);
		
		//Propeller bench
		GameRegistry.addRecipe(new ItemStack(blockPropellerBench),
				"AAA",
				" BA",
				"ACA",
				'A', ingotStack,
				'B', diamondStack,
				'C', MFS.proxy.getStackByItemName("anvil", -1));
				
	}
	
	private void initEngineRecipes(){
		//New engines
		GameRegistry.addRecipe(new ItemStack(engineSmall, 1, 2805),
				"ABA",
				"BCB",
				"ABA",
				'A', pistonStack, 
				'B', obsidianStack,
				'C', ingotStack);
		GameRegistry.addRecipe(new ItemStack(engineSmall, 1, 3007),
				"ABA",
				"BCB",
				"ABA",
				'A', pistonStack, 
				'B', obsidianStack,
				'C', diamondStack);
		GameRegistry.addRecipe(new ItemStack(engineLarge, 1, 2907),
				"ABA",
				"ACA",
				"ABA",
				'A', pistonStack, 
				'B', obsidianStack,
				'C', ingotStack);
		GameRegistry.addRecipe(new ItemStack(engineLarge, 1, 3210),
				"ABA",
				"ACA",
				"ABA",
				'A', pistonStack, 
				'B', obsidianStack,
				'C', diamondStack);
		
		//Repaired engines
		GameRegistry.addRecipe(new ItemStack(engineSmall, 1, 2805),
				"B B",
				" C ",
				"B B",
				'B', obsidianStack,
				'C', new ItemStack(engineSmall, 1, 2805));
		GameRegistry.addRecipe(new ItemStack(engineSmall, 1, 3007),
				"B B",
				" C ",
				"B B",
				'B', obsidianStack,
				'C', new ItemStack(engineSmall, 1, 3007));
		GameRegistry.addRecipe(new ItemStack(engineLarge, 1, 2907),
				"B B",
				"BCB",
				"B B",
				'B', obsidianStack,
				'C', new ItemStack(engineLarge, 1, 2907));
		GameRegistry.addRecipe(new ItemStack(engineLarge, 1, 3210),
				"B B",
				"BCB",
				"B B",
				'B', obsidianStack,
				'C', new ItemStack(engineLarge, 1, 3210));
	}
	
	private void initFlightInstrumentRecipes(){
		GameRegistry.addRecipe(new ItemStack(flightInstrumentBase, 16),
				"III",
				"IGI",
				"III",
				'I', ingotStack, 
				'G', MFS.proxy.getStackByItemName("glass_pane", -1));
		
		GameRegistry.addRecipe(new ItemStack(pointerShort),
				" WW",
				" WW",
				"B  ",
				'W', dyeStack[15], 
				'B', dyeStack[0]);
		
		GameRegistry.addRecipe(new ItemStack(pointerLong),
				"  W",
				" W ",
				"B  ",
				'W', dyeStack[15], 
				'B', dyeStack[0]);
		
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 0),
				"LLL",
				"RRR",
				" B ",
				'B', flightInstrumentBase, 
				'L', dyeStack[4], 
				'R', dyeStack[3]);
		
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 1),
				"WLW",
				"WSW",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'S', pointerShort, 
				'W', dyeStack[15]);
		
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 2),
				" W ",
				"WIW",
				" B ",
				'B', flightInstrumentBase, 
				'I', ingotStack, 
				'W', dyeStack[15]);
		
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 3),
				"R W",
				"YLG",
				"GBG",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'R', dyeStack[1],
				'Y', dyeStack[11], 
				'G', dyeStack[10], 
				'W', dyeStack[15]);
		
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 4),
				"   ",
				"WIW",
				"WBW",
				'B', flightInstrumentBase, 
				'I', ingotStack, 
				'W', dyeStack[15]);
		
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 5),
				"WWW",
				" I ",
				"WBW",
				'B', flightInstrumentBase, 
				'I', ingotStack, 
				'W', dyeStack[15]);
		
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 6),
				"W W",
				" L ",
				"WBW",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'W', dyeStack[15]);
		
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 7),
				"RYG",
				" LG",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'R', dyeStack[1], 
				'Y', dyeStack[11], 
				'G', dyeStack[10], 
				'W', dyeStack[15]);
		
		//Instrument 8 does not exist
		//Instrument 9 does not exist
		
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 10),
				"W W",
				" L ",
				"WBR",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'R', dyeStack[1], 
				'W', dyeStack[15]);
		
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 11),
				"RWW",
				" L ",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'R', dyeStack[1], 
				'W', dyeStack[15]);
		
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 12),
				" W ",
				"WLW",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'W', dyeStack[15]);
		
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 13),
				"YGR",
				" L ",
				" B ",
				'B', flightInstrumentBase, 
				'L', pointerLong, 
				'Y', dyeStack[11], 
				'G', dyeStack[10], 
				'R', dyeStack[1], 
				'W', dyeStack[15]);
		
		//Instrument 14 does not exist
	}
}
