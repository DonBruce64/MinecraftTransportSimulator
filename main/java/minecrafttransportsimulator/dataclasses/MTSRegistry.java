package minecrafttransportsimulator.dataclasses;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.BlockPropellerBench;
import minecrafttransportsimulator.blocks.BlockSurveyFlag;
import minecrafttransportsimulator.blocks.BlockTrack;
import minecrafttransportsimulator.blocks.BlockTrackFake;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.main.EntityCore;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.entities.parts.*;
import minecrafttransportsimulator.items.*;
import minecrafttransportsimulator.items.ItemEngine.ItemEngineAircraftLarge;
import minecrafttransportsimulator.items.ItemEngine.ItemEngineAircraftSmall;
import minecrafttransportsimulator.packets.control.*;
import minecrafttransportsimulator.packets.general.*;
import minecrafttransportsimulator.systems.pack.PackObject;
import minecrafttransportsimulator.systems.pack.PackParserSystem;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	/**Maps multipart class names to classes.*/
	public static Map<String, Class<? extends EntityMultipartMoving>> multipartClasses = new HashMap<String, Class<? extends EntityMultipartMoving>>();
	/**Maps child class names to classes for quicker lookup during spawn operations.*/
	public static Map<String, Class<? extends EntityMultipartChild>> partClasses = new HashMap<String, Class<? extends EntityMultipartChild>>();
	
	/**All run-time things go here.**/
	public void init(){
		initCustomEntities();
		initPlaneItems();
		initItems();
		initBlocks();
		initEntities();
		initPackets();
		initRecipes();
	}
	
	private void initCustomEntities(){
		for(String name : PackParserSystem.getRegisteredNames()){
			PackObject pack = PackParserSystem.getPack(name);
			if(pack.general.type.equals("plane")){
				multipartClasses.put(pack.general.name, EntityPlane.class);
			}
		}
	}


	private void initPlaneItems(){
		for(String planeName: PackParserSystem.getRegisteredNames()){
			PackObject plane = PackParserSystem.getPack(planeName);

			String[] itemTextures = plane.general.itemTexture;
			for (int i = 0; i < itemTextures.length; i++) {
				String itemTexture = itemTextures[i];
				//TODO Actually add the item textures
				ItemMultipartMoving item = new ItemMultipartMoving(planeName, MTSCreativeTabs.tabMTSPlanes);
				registerItem(item);
			}


		}
	}

	private void initItems(){
		for(Field field : this.getClass().getFields()){
			if(field.getType().equals(Item.class)){
				try{
					Item item = (Item) field.get(Item.class);
					if(item.getUnlocalizedName().equals("item.null")){
						item.setUnlocalizedName(field.getName().toLowerCase());
					}
					registerItem(item);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	private void initBlocks(){
		for(Field field : this.getClass().getFields()){
			if(field.getType().equals(Block.class)){
				try{
					Block block = (Block) field.get(Block.class);
					if(block.getUnlocalizedName().equals("tile.null")){
						block.setUnlocalizedName(field.getName().toLowerCase());
					}
					registerBlock(block);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	private void initEntities(){
		registerEntity(EntityPlane.class);
		
		registerChildEntity(EntityCore.class, null);
		registerChildEntity(EntitySeat.class, seat);
		registerChildEntity(EntityChest.class, Item.getItemFromBlock(Blocks.CHEST));
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
	
	private void initRecipes(){
		this.initPartRecipes();
		this.initEngineRecipes();
		this.initFlightInstrumentRecipes();
	}
	
	private void initPartRecipes(){
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
				
	}
	
	private void initEngineRecipes(){
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
	
	private void initFlightInstrumentRecipes(){		
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
		
		
		registerRecipe(new ItemStack(flightInstrument, 16, 0),
				"III",
				"IGI",
				"III",
				'I', Items.IRON_INGOT,
				'G', Blocks.GLASS_PANE);
		registerRecipe(new ItemStack(flightInstrument, 1, 1),
				"LLL",
				"RRR",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', new ItemStack(Items.DYE, 1, 4),
				'R', new ItemStack(Items.DYE, 1, 3));
		
		registerRecipe(new ItemStack(flightInstrument, 1, 2),
				"WLW",
				"WSW",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'S', pointerShort, 
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(flightInstrument, 1, 3),
				" W ",
				"WIW",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'I', Items.IRON_INGOT,
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(flightInstrument, 1, 4),
				"R W",
				"YLG",
				"GBG",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'R', new ItemStack(Items.DYE, 1, 1),
				'Y', new ItemStack(Items.DYE, 1, 11),
				'G', new ItemStack(Items.DYE, 1, 10),
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(flightInstrument, 1, 5),
				"   ",
				"WIW",
				"WBW",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'I', Items.IRON_INGOT,
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(flightInstrument, 1, 6),
				"WWW",
				" I ",
				"WBW",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'I', Items.IRON_INGOT,
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(flightInstrument, 1, 7),
				"W W",
				" L ",
				"WBW",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(flightInstrument, 1, 8),
				"RYG",
				" LG",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'R', new ItemStack(Items.DYE, 1, 1),
				'Y', new ItemStack(Items.DYE, 1, 11),
				'G', new ItemStack(Items.DYE, 1, 10),
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(flightInstrument, 1, 9),
				"GLG",
				"LGL",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'G', new ItemStack(Items.DYE, 1, 10));

		//Instrument 10 does not exist
		
		registerRecipe(new ItemStack(flightInstrument, 1, 11),
				"W W",
				" L ",
				"WBR",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'R', new ItemStack(Items.DYE, 1, 1),
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(flightInstrument, 1, 12),
				"RWW",
				" L ",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'R', new ItemStack(Items.DYE, 1, 1),
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(flightInstrument, 1, 13),
				" W ",
				"WLW",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'W', new ItemStack(Items.DYE, 1, 15));
		
		registerRecipe(new ItemStack(flightInstrument, 1, 14),
				"YGR",
				" L ",
				" B ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong, 
				'Y', new ItemStack(Items.DYE, 1, 11),
				'G', new ItemStack(Items.DYE, 1, 10),
				'R', new ItemStack(Items.DYE, 1, 1),
				'W', new ItemStack(Items.DYE, 1, 15));
				
		registerRecipe(new ItemStack(flightInstrument, 1, 15),
				"   ",
				"LGL",
				"RB ",
				'B', new ItemStack(flightInstrument, 1, 0), 
				'L', pointerLong,  
				'G', new ItemStack(Items.DYE, 1, 10),
				'R', new ItemStack(Items.DYE, 1, 1));
	}
	
	
	/**
	 * Registers the given item and adds it to the creative tab list.
	 * @param item
	 */
	private static void registerItem(Item item){
		String registryName = item.getUnlocalizedName().split("\\.")[1].toLowerCase();
		GameRegistry.register(item.setRegistryName(registryName));
		MTSRegistry.itemList.add(item);
	}
	
	/**x
	 * Registers the given block and adds it to the creative tab list.
	 * Also adds the respective TileEntity if the block has one.
	 * @param block
	 */
	private static void registerBlock(Block block){
		GameRegistry.register(block.setRegistryName(block.getUnlocalizedName().split("\\.")[1].toLowerCase()));
		GameRegistry.register(new ItemBlock(block).setRegistryName(block.getUnlocalizedName().substring(5).toLowerCase()));
		MTSRegistry.itemList.add(Item.getItemFromBlock(block));
		if(block instanceof ITileEntityProvider){
			Class<? extends TileEntity> tileEntityClass = ((ITileEntityProvider) block).createNewTileEntity(null, 0).getClass();
			GameRegistry.registerTileEntity(tileEntityClass, tileEntityClass.getSimpleName());
		}
	}

	/**
	 * Registers an entity.
	 * Adds it to the multipartClassess map if appropriate.
	 * @param entityClass
	 */
	private static void registerEntity(Class entityClass){
		EntityRegistry.registerModEntity(entityClass, entityClass.getSimpleName().substring(6).toLowerCase(), entityNumber++, MTS.MODID, 80, 5, false);
		if(EntityMultipartMoving.class.isAssignableFrom(entityClass)){
			multipartClasses.put(entityClass.getSimpleName().substring(6).toLowerCase(), entityClass);
		}
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
	
	private static void registerRecipe(ItemStack output, Object...params){
		GameRegistry.addRecipe(output, params);
	}
}
