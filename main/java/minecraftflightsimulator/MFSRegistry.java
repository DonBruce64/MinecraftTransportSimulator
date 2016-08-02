package minecraftflightsimulator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecraftflightsimulator.blocks.BlockPropellerBench;
import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.containers.GUIHandler;
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
import minecraftflightsimulator.packets.general.ServerSendDataPacket;
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
import net.minecraftforge.fluids.FluidRegistry;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;

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
	private static int entityNumber = 0;
	private static int packetNumber = 0;
	
	public void init(){
		initItems();
		initBlocks();
		initTileEntites();
		initEntites();
		initPackets();
		initRecipies();
		initFuels();
		NetworkRegistry.INSTANCE.registerGuiHandler(MFS.instance, new GUIHandler());
	}
	
	private void initItems(){
		for(Field feild : this.getClass().getFields()){
			if(feild.getType().equals(Item.class)){
				try{
					Item item = (Item) feild.get(Item.class);
					if(!item.equals(MFSRegistry.planeTrimotor)){
						if(item.getUnlocalizedName().equals("item.null")){
							item.setUnlocalizedName(feild.getName().substring(0, 1).toUpperCase() + feild.getName().substring(1));
						}
						item.setCreativeTab(MFS.tabMFS);
						item.setTextureName("mfs:" + item.getUnlocalizedName().substring(5).toLowerCase());
						GameRegistry.registerItem(item, item.getUnlocalizedName().substring(5));
						itemList.add(item);
					}
				}catch(Exception e){}
			}
		}
	}
	
	private void initBlocks(){
		for(Field feild : this.getClass().getFields()){
			if(feild.getType().equals(Block.class)){
				try{
					Block block = (Block) feild.get(Block.class);
					GameRegistry.registerBlock(block, block.getUnlocalizedName().substring(5));
					block.setBlockTextureName("mfs:" + block.getUnlocalizedName().substring(5).toLowerCase());
					itemList.add(Item.getItemFromBlock(block));
				}catch(Exception e){}
			}
		}
	}
	
	private void initTileEntites(){
		registerTileEntity(TileEntityPropellerBench.class);
	}
	
	private void initEntites(){
		registerEntity(EntityMC172.class, null);
		registerEntity(EntityPZLP11.class, null);
		registerEntity(EntityVulcanair.class, null);
		registerEntity(EntityTrimotor.class, null);
		
		registerEntity(EntityCore.class, null);
		registerEntity(EntitySeat.class, seat);
		registerEntity(EntityPlaneChest.class, Item.getItemFromBlock(Blocks.chest));
		registerEntity(EntityWheelSmall.class, wheelSmall);
		registerEntity(EntityWheelLarge.class, wheelLarge);
		registerEntity(EntitySkid.class, skid);
		registerEntity(EntityPontoon.class, pontoon);
		registerEntity(EntityPontoonDummy.class, null);
		registerEntity(EntityPropeller.class, propeller);
		registerEntity(EntityEngineSmall.class, engineSmall);
		registerEntity(EntityEngineLarge.class, engineLarge);
	}
	
	private void registerTileEntity(Class tileEntityClass){
		GameRegistry.registerTileEntity(tileEntityClass, tileEntityClass.getSimpleName());
	}
	
	private void registerEntity(Class entityClass, Item entityItem){
		if(EntityChild.class.isAssignableFrom(entityClass) && entityItem != null){
			entityItems.put(entityClass, entityItem);
		}
		EntityRegistry.registerModEntity(entityClass, entityClass.getName().substring(7), ++entityNumber, MFS.MODID, 80, 5, false);
	}
	
	private void initPackets(){
		MFS.MFSNet.registerMessage(ChatPacket.ChatPacketHandler.class,  ChatPacket.class, ++packetNumber, Side.CLIENT);
		MFS.MFSNet.registerMessage(ServerSendDataPacket.ServerSendDataPacketHandler.class,  ServerSendDataPacket.class, ++packetNumber, Side.CLIENT);
		MFS.MFSNet.registerMessage(ServerSyncPacket.ServerSyncPacketHandler.class,  ServerSyncPacket.class, ++packetNumber, Side.CLIENT);
		MFS.MFSNet.registerMessage(PropellerBenchTilepdatePacket.CraftingTileUpdatePacketHandler.class,  PropellerBenchTilepdatePacket.class, ++packetNumber, Side.CLIENT);
		
		MFS.MFSNet.registerMessage(GUIPacket.GUIPacketHandler.class,  GUIPacket.class, ++packetNumber, Side.SERVER);
		MFS.MFSNet.registerMessage(ClientRequestDataPacket.ClientRequestDataPacketHandler.class,  ClientRequestDataPacket.class, ++packetNumber, Side.SERVER);
		MFS.MFSNet.registerMessage(PropellerBenchTilepdatePacket.CraftingTileUpdatePacketHandler.class,  PropellerBenchTilepdatePacket.class, ++packetNumber, Side.SERVER);
		
		MFS.MFSNet.registerMessage(AileronPacket.AileronPacketHandler.class,  AileronPacket.class, ++packetNumber, Side.SERVER);
		MFS.MFSNet.registerMessage(AileronPacket.AileronPacketHandler.class,  AileronPacket.class, ++packetNumber, Side.CLIENT);
		MFS.MFSNet.registerMessage(BrakePacket.BrakePacketHandler.class,  BrakePacket.class, ++packetNumber, Side.SERVER);
		MFS.MFSNet.registerMessage(BrakePacket.BrakePacketHandler.class,  BrakePacket.class, ++packetNumber, Side.CLIENT);
		MFS.MFSNet.registerMessage(ElevatorPacket.ElevatorPacketHandler.class,  ElevatorPacket.class, ++packetNumber, Side.SERVER);
		MFS.MFSNet.registerMessage(ElevatorPacket.ElevatorPacketHandler.class,  ElevatorPacket.class, ++packetNumber, Side.CLIENT);
		MFS.MFSNet.registerMessage(EnginePacket.EnginePacketHandler.class,  EnginePacket.class, ++packetNumber, Side.SERVER);
		MFS.MFSNet.registerMessage(EnginePacket.EnginePacketHandler.class,  EnginePacket.class, ++packetNumber, Side.CLIENT);
		MFS.MFSNet.registerMessage(FlapPacket.FlapPacketHandler.class,  FlapPacket.class, ++packetNumber, Side.SERVER);
		MFS.MFSNet.registerMessage(FlapPacket.FlapPacketHandler.class,  FlapPacket.class, ++packetNumber, Side.CLIENT);
		MFS.MFSNet.registerMessage(RudderPacket.RudderPacketHandler.class,  RudderPacket.class, ++packetNumber, Side.SERVER);
		MFS.MFSNet.registerMessage(RudderPacket.RudderPacketHandler.class,  RudderPacket.class, ++packetNumber, Side.CLIENT);
		MFS.MFSNet.registerMessage(ThrottlePacket.ThrottlePacketHandler.class,  ThrottlePacket.class, ++packetNumber, Side.SERVER);
		MFS.MFSNet.registerMessage(ThrottlePacket.ThrottlePacketHandler.class,  ThrottlePacket.class, ++packetNumber, Side.CLIENT);
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
			GameRegistry.addRecipe(new ItemStack(MFSRegistry.planeMC172, 1, i),
				"AAA",
				" B ",
				"ABA",
				'A', new ItemStack(Blocks.wooden_slab, 1, i), 'B', new ItemStack(Blocks.planks, 1, i));
		}
		
		//PZLP11
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.planePZLP11),
				"AAA",
				" B ",
				"ABA",
				'A', Items.iron_ingot, 'B', Blocks.iron_bars);
		
		//Vulcanair
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.planeVulcanair, 1, 0),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 'C', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.planeVulcanair, 1, 1),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 'C', new ItemStack(Items.dye, 1, 4));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.planeVulcanair, 1, 2),
				"AAA",
				"CAD",
				"AAA",
				'A', Items.iron_ingot, 'C', new ItemStack(Items.dye, 1, 1), 'D', new ItemStack(Items.dye, 1, 7));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.planeVulcanair, 1, 3),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 'C', new ItemStack(Items.dye, 1, 1));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.planeVulcanair, 1, 4),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 'C', new ItemStack(Items.dye, 1, 10));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.planeVulcanair, 1, 5),
				"AAA",
				"CAC",
				"AAA",
				'A', Items.iron_ingot, 'C', new ItemStack(Items.dye, 1, 0));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.planeVulcanair, 1, 6),
				"AAA",
				"CAD",
				"AAA",
				'A', Items.iron_ingot, 'C', new ItemStack(Items.dye, 1, 15), 'D', new ItemStack(Items.dye, 1, 14));
	}
	
	private void initPartRecipes(){
		//Seat
		for(int i=0; i<6; ++i){
			for(int j=0; j<16; ++j){
				GameRegistry.addRecipe(new ItemStack(MFSRegistry.seat, 1, (i << 4) + j),
					" BA",
					" BA",
					"AAA",
					'A', new ItemStack(Blocks.wooden_slab, 1, i), 'B', new ItemStack(Blocks.wool, 1, j));
			}
		}
		
		//Wheels
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.wheelSmall, 1, 0),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.wool, 'B', new ItemStack(Items.dye, 1, 0), 'C', Items.iron_ingot);
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.wheelLarge, 1, 1),
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.wool, 'B', new ItemStack(Items.dye, 1, 0), 'C', Items.iron_ingot);
		//Skid
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.skid),
				"A A",
				" A ",
				"  A",
				'A', Blocks.iron_bars);
		//Pontoon
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.pontoon, 2),
				"AAA",
				"BBB",
				"AAA",
				'A', Items.iron_ingot, 'B', Blocks.wool);
	}
	
	private void initEngineRecipes(){
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.engineSmall, 1, 2805),
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.piston, 'B', Blocks.obsidian,'C', Items.iron_ingot);
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.engineSmall, 1, 3007),
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.piston, 'B', Blocks.obsidian,'C', Items.diamond);
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.engineLarge, 1, 2907),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.piston, 'B', Blocks.obsidian,'C', Items.iron_ingot);
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.engineLarge, 1, 3210),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.piston, 'B', Blocks.obsidian,'C', Items.diamond);
	}
	
	private void initFlightInstrumentRecipes(){
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.flightInstrumentBase, 16),
				"III",
				"IGI",
				"III",
				'I', Items.iron_ingot, 'G', Blocks.glass_pane);
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.pointerShort),
				" WW",
				" WW",
				"B  ",
				'W', new ItemStack(Items.dye, 1, 15), 'B', new ItemStack(Items.dye, 1, 0));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.pointerLong),
				"  W",
				" W ",
				"B  ",
				'W', new ItemStack(Items.dye, 1, 15), 'B', new ItemStack(Items.dye, 1, 0));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.flightInstrument, 1, 0),
				"LLL",
				"RRR",
				" B ",
				'B', MFSRegistry.flightInstrumentBase, 'L', new ItemStack(Items.dye, 1, 4), 'R', new ItemStack(Items.dye, 1, 3));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.flightInstrument, 1, 1),
				"WLW",
				"WSW",
				" B ",
				'B', MFSRegistry.flightInstrumentBase, 'L', MFSRegistry.pointerLong, 'S', MFSRegistry.pointerShort, 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.flightInstrument, 1, 2),
				" W ",
				"WIW",
				" B ",
				'B', MFSRegistry.flightInstrumentBase, 'I', Items.iron_ingot, 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.flightInstrument, 1, 3),
				"R W",
				"YLG",
				"GBG",
				'B', MFSRegistry.flightInstrumentBase, 'L', MFSRegistry.pointerLong, 'R', new ItemStack(Items.dye, 1, 1), 'Y', new ItemStack(Items.dye, 1, 11), 'G', new ItemStack(Items.dye, 1, 10), 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.flightInstrument, 1, 4),
				"   ",
				"WIW",
				"WBW",
				'B', MFSRegistry.flightInstrumentBase, 'I', Items.iron_ingot, 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.flightInstrument, 1, 5),
				"WWW",
				" I ",
				"WBW",
				'B', MFSRegistry.flightInstrumentBase, 'I', Items.iron_ingot, 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.flightInstrument, 1, 6),
				"W W",
				" L ",
				"WBW",
				'B', MFSRegistry.flightInstrumentBase, 'L', MFSRegistry.pointerLong, 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.flightInstrument, 1, 7),
				"RYG",
				" LG",
				" B ",
				'B', MFSRegistry.flightInstrumentBase, 'L', MFSRegistry.pointerLong, 'R', new ItemStack(Items.dye, 1, 1), 'Y', new ItemStack(Items.dye, 1, 11), 'G', new ItemStack(Items.dye, 1, 10), 'W', new ItemStack(Items.dye, 1, 15));
		
		//Instrument 8 does not exist
		//Instrument 9 does not exist
		
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.flightInstrument, 1, 10),
				"W W",
				" L ",
				"WBR",
				'B', MFSRegistry.flightInstrumentBase, 'L', MFSRegistry.pointerLong, 'R', new ItemStack(Items.dye, 1, 1), 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.flightInstrument, 1, 11),
				"RWW",
				" L ",
				" B ",
				'B', MFSRegistry.flightInstrumentBase, 'L', MFSRegistry.pointerLong, 'R', new ItemStack(Items.dye, 1, 1), 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.flightInstrument, 1, 12),
				" W ",
				"WLW",
				" B ",
				'B', MFSRegistry.flightInstrumentBase, 'L', MFSRegistry.pointerLong, 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(MFSRegistry.flightInstrument, 1, 13),
				"YGR",
				" L ",
				" B ",
				'B', MFSRegistry.flightInstrumentBase, 'L', MFSRegistry.pointerLong, 'Y', new ItemStack(Items.dye, 1, 11), 'G', new ItemStack(Items.dye, 1, 10), 'R', new ItemStack(Items.dye, 1, 1), 'W', new ItemStack(Items.dye, 1, 15));
		//Instrument 14 does not exist
	}
	
	private void initFuels(){
		for(String fluidName : FluidRegistry.getRegisteredFluids().keySet()){
			MFS.fluidValues.put(fluidName, MFS.config.get("fuels", fluidName, fluidName.equals(FluidRegistry.LAVA.getName()) ? 1.0F : 0.0F).getDouble());
		}
		MFS.config.save();
	}
}
