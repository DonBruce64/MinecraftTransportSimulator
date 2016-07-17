package minecraftflightsimulator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import minecraftflightsimulator.blocks.BlockPropellerBench;
import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.containers.GUIHandler;
import minecraftflightsimulator.entities.core.EntityCore;
import minecraftflightsimulator.entities.parts.EntityEngine;
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
import minecraftflightsimulator.planes.Otter.EntityOtter;
import minecraftflightsimulator.planes.PZLP11.EntityPZLP11;
import minecraftflightsimulator.planes.Trimotor.EntityTrimotor;
import minecraftflightsimulator.planes.Vulcanair.EntityVulcanair;
import minecraftflightsimulator.sounds.BenchSound;
import minecraftflightsimulator.sounds.EngineSound;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;


public class CommonProxy{
	public static final Block blockPropellerBench = new BlockPropellerBench();
	
	public static List<Item> itemList = new ArrayList<Item>();
	public static final Item planeMC172 = new ItemPlane(EntityMC172.class, 6);
	public static final Item planePZLP11 = new ItemPlane(EntityPZLP11.class, 1);
	public static final Item planeTrimotor = new ItemPlane(EntityTrimotor.class, 1);
	public static final Item planeOtter = new ItemPlane(EntityOtter.class, 1);
	
	public static final Item seat = new ItemSeat();
	public static final Item propeller = new ItemPropeller();
	public static final Item engineSmall = new ItemEngine(EngineTypes.SMALL);
	public static final Item engineLarge = new ItemEngine(EngineTypes.LARGE);
	public static final Item flightInstrument = new ItemFlightInstrument();
	
	public static final Item flightInstrumentBase = new Item();
	public static final Item pointerShort = new Item();
	public static final Item pointerLong = new Item();
	public static final Item wheelSmall = new Item();
	public static final Item wheelLarge = new Item();
	public static final Item skid = new Item();
	public static final Item pontoon = new Item();
		
	private static int entityNumber = 0;
	private static int packetNumber = 0;
	
	public void preInit(){
		
	}
	
	public void init(){
		initTileEntites();
		initEntites();
		initPackets();
		initItems();
		initBlocks();
		initRecipies();
		initFuels();
		NetworkRegistry.INSTANCE.registerGuiHandler(MFS.instance, new GUIHandler());
	}
	
	private void initTileEntites(){
		registerTileEntity(TileEntityPropellerBench.class);
	}
	
	private void registerTileEntity(Class tileEntityClass){
		GameRegistry.registerTileEntity(tileEntityClass, tileEntityClass.getSimpleName());
	}
	
	private void initEntites(){
		registerEntity(EntityMC172.class);
		registerEntity(EntityPZLP11.class);
		//registerEntity(EntityTrimotor.class);
		//registerEntity(EntityOtter.class);
		
		registerEntity(EntityCore.class);
		registerEntity(EntitySeat.class);
		registerEntity(EntityPlaneChest.class);
		registerEntity(EntityWheelSmall.class);
		registerEntity(EntityWheelLarge.class);
		registerEntity(EntitySkid.class);
		registerEntity(EntityPontoon.class);
		registerEntity(EntityPontoonDummy.class);
		registerEntity(EntityPropeller.class);
		registerEntity(EntityEngineSmall.class);
		registerEntity(EntityEngineLarge.class);
	}
	
	private void registerEntity(Class entityClass){
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
	
	private void initItems(){
		for(Field feild : this.getClass().getFields()){
			if(feild.getType().equals(Item.class)){
				try{
					Item item = (Item) feild.get(Item.class);
					if(!item.equals(planeOtter) && !item.equals(planeTrimotor)){
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
					itemList.add(Item.getItemFromBlock(block));
				}catch(Exception e){}
			}
		}
	}
	
	private void initRecipies(){
		this.initEngineRecipes();
		this.initFlightInstrumentRecipes();
		
		//MC172
		for(int i=0; i<6; ++i){
			GameRegistry.addRecipe(new ItemStack(planeMC172, 1, i),
				"AAA",
				" B ",
				"ABA",
				'A', new ItemStack(Blocks.wooden_slab, 1, i), 'B', new ItemStack(Blocks.planks, 1, i));
		}
		
		//PZLP11
		GameRegistry.addRecipe(new ItemStack(planePZLP11),
				"AAA",
				" B ",
				"ABA",
				'A', Items.iron_ingot, 'B', Blocks.iron_bars);
		
		//Seat
		for(int i=0; i<6; ++i){
			for(int j=0; j<16; ++j){
				GameRegistry.addRecipe(new ItemStack(seat, 1, (i << 4) + j),
					" BA",
					" BA",
					"AAA",
					'A', new ItemStack(Blocks.wooden_slab, 1, i), 'B', new ItemStack(Blocks.wool, 1, j));
			}
		}
		
		//Wheels
		GameRegistry.addRecipe(new ItemStack(wheelSmall, 1, 0),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.wool, 'B', new ItemStack(Items.dye, 1, 0), 'C', Items.iron_ingot);
		GameRegistry.addRecipe(new ItemStack(wheelLarge, 1, 1),
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.wool, 'B', new ItemStack(Items.dye, 1, 0), 'C', Items.iron_ingot);
		//Skid
		GameRegistry.addRecipe(new ItemStack(skid),
				"A A",
				" A ",
				"  A",
				'A', Blocks.iron_bars);
		//Pontoon
		GameRegistry.addRecipe(new ItemStack(pontoon, 2),
				"AAA",
				"BBB",
				"AAA",
				'A', Items.iron_ingot, 'B', Blocks.wool);
	}
	
	private void initEngineRecipes(){
		GameRegistry.addRecipe(new ItemStack(engineSmall, 1, 2805),
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.piston, 'B', Blocks.obsidian,'C', Items.iron_ingot);
		GameRegistry.addRecipe(new ItemStack(engineSmall, 1, 3007),
				"ABA",
				"BCB",
				"ABA",
				'A', Blocks.piston, 'B', Blocks.obsidian,'C', Items.diamond);
		GameRegistry.addRecipe(new ItemStack(engineLarge, 1, 2907),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.piston, 'B', Blocks.obsidian,'C', Items.iron_ingot);
		GameRegistry.addRecipe(new ItemStack(engineLarge, 1, 3210),
				"ABA",
				"ACA",
				"ABA",
				'A', Blocks.piston, 'B', Blocks.obsidian,'C', Items.diamond);
	}
	
	private void initFlightInstrumentRecipes(){
		GameRegistry.addRecipe(new ItemStack(flightInstrumentBase, 16),
				"III",
				"IGI",
				"III",
				'I', Items.iron_ingot, 'G', Blocks.glass_pane);
		GameRegistry.addRecipe(new ItemStack(pointerShort),
				" WW",
				" WW",
				"B  ",
				'W', new ItemStack(Items.dye, 1, 15), 'B', new ItemStack(Items.dye, 1, 0));
		GameRegistry.addRecipe(new ItemStack(pointerLong),
				"  W",
				" W ",
				"B  ",
				'W', new ItemStack(Items.dye, 1, 15), 'B', new ItemStack(Items.dye, 1, 0));
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 0),
				"LLL",
				"RRR",
				" B ",
				'B', flightInstrumentBase, 'L', new ItemStack(Items.dye, 1, 4), 'R', new ItemStack(Items.dye, 1, 3));
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 1),
				"WLW",
				"WSW",
				" B ",
				'B', flightInstrumentBase, 'L', pointerLong, 'S', pointerShort, 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 2),
				" W ",
				"WIW",
				" B ",
				'B', flightInstrumentBase, 'I', Items.iron_ingot, 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 3),
				"R W",
				"YLG",
				"GBG",
				'B', flightInstrumentBase, 'L', pointerLong, 'R', new ItemStack(Items.dye, 1, 1), 'Y', new ItemStack(Items.dye, 1, 11), 'G', new ItemStack(Items.dye, 1, 10), 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 4),
				"   ",
				"WIW",
				"WBW",
				'B', flightInstrumentBase, 'I', Items.iron_ingot, 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 5),
				"WWW",
				" I ",
				"WBW",
				'B', flightInstrumentBase, 'I', Items.iron_ingot, 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 6),
				"W W",
				" L ",
				"WBW",
				'B', flightInstrumentBase, 'L', pointerLong, 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 7),
				"RYG",
				" LG",
				" B ",
				'B', flightInstrumentBase, 'L', pointerLong, 'R', new ItemStack(Items.dye, 1, 1), 'Y', new ItemStack(Items.dye, 1, 11), 'G', new ItemStack(Items.dye, 1, 10), 'W', new ItemStack(Items.dye, 1, 15));
		
		//Instrument 8 does not exist
		//Instrument 9 does not exist
		
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 10),
				"W W",
				" L ",
				"WBR",
				'B', flightInstrumentBase, 'L', pointerLong, 'R', new ItemStack(Items.dye, 1, 1), 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 11),
				"RWW",
				" L ",
				" B ",
				'B', flightInstrumentBase, 'L', pointerLong, 'R', new ItemStack(Items.dye, 1, 1), 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 12),
				" W ",
				"WLW",
				" B ",
				'B', flightInstrumentBase, 'L', pointerLong, 'W', new ItemStack(Items.dye, 1, 15));
		GameRegistry.addRecipe(new ItemStack(flightInstrument, 1, 13),
				"YGR",
				" L ",
				" B ",
				'B', flightInstrumentBase, 'L', pointerLong, 'Y', new ItemStack(Items.dye, 1, 11), 'G', new ItemStack(Items.dye, 1, 10), 'R', new ItemStack(Items.dye, 1, 1), 'W', new ItemStack(Items.dye, 1, 15));
		//Instrument 14 does not exist
	}
	
	private void initFuels(){
		for(String fluidName : FluidRegistry.getRegisteredFluids().keySet()){
			MFS.fluidValues.put(fluidName, MFS.config.get("fuels", fluidName, fluidName.equals(FluidRegistry.LAVA.getName()) ? 1.0F : 0.0F).getDouble());
		}
		MFS.config.save();
	}
	
	public void playSound(Entity noisyEntity, String soundName, float volume, float pitch){}
	public void updateSeatedRider(EntitySeat seat, EntityLivingBase rider){}
	public EngineSound updateEngineSoundAndSmoke(EngineSound sound, EntityEngine engine){return null;}
	public BenchSound updateBenchSound(BenchSound sound, TileEntityPropellerBench bench){return null;}
}
