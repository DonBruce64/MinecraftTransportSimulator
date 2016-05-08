package minecraftflightsimulator;

import minecraftflightsimulator.containers.GUIHandler;
import minecraftflightsimulator.entities.EntityCore;
import minecraftflightsimulator.entities.EntityEngine;
import minecraftflightsimulator.entities.EntityEngineLarge;
import minecraftflightsimulator.entities.EntityEngineSmall;
import minecraftflightsimulator.entities.EntityPlaneChest;
import minecraftflightsimulator.entities.EntityPropeller;
import minecraftflightsimulator.entities.EntitySeat;
import minecraftflightsimulator.entities.EntityWheelLarge;
import minecraftflightsimulator.entities.EntityWheelSmall;
import minecraftflightsimulator.items.ItemEngineLarge;
import minecraftflightsimulator.items.ItemEngineSmall;
import minecraftflightsimulator.items.ItemFlightInstrument;
import minecraftflightsimulator.items.ItemFlightInstrumentBase;
import minecraftflightsimulator.items.ItemPlane;
import minecraftflightsimulator.items.ItemPointerLong;
import minecraftflightsimulator.items.ItemPointerShort;
import minecraftflightsimulator.items.ItemPropeller;
import minecraftflightsimulator.items.ItemSeat;
import minecraftflightsimulator.items.ItemWheelLarge;
import minecraftflightsimulator.items.ItemWheelSmall;
import minecraftflightsimulator.packets.control.AileronPacket;
import minecraftflightsimulator.packets.control.BrakePacket;
import minecraftflightsimulator.packets.control.ElevatorPacket;
import minecraftflightsimulator.packets.control.EnginePacket;
import minecraftflightsimulator.packets.control.FlapPacket;
import minecraftflightsimulator.packets.control.RudderPacket;
import minecraftflightsimulator.packets.control.ThrottlePacket;
import minecraftflightsimulator.packets.general.ChatPacket;
import minecraftflightsimulator.packets.general.ClientRequestDataPacket;
import minecraftflightsimulator.packets.general.FuelPacket;
import minecraftflightsimulator.packets.general.ServerSendDataPacket;
import minecraftflightsimulator.packets.general.ServerSyncPacket;
import minecraftflightsimulator.planes.MC172.EntityMC172;
import minecraftflightsimulator.planes.Otter.EntityOtter;
import minecraftflightsimulator.planes.PZLP11.EntityPZLP11;
import minecraftflightsimulator.planes.Trimotor.EntityTrimotor;
import minecraftflightsimulator.sounds.EngineSound;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.minecraftforge.fluids.FluidRegistry;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;


public class CommonProxy{
	public static final Item planeMC172 = new ItemPlane(EntityMC172.class, 6);
	public static final Item planeTrimotor = new ItemPlane(EntityTrimotor.class, 1);
	public static final Item planeOtter = new ItemPlane(EntityOtter.class, 1);
	public static final Item planePLZP11 = new ItemPlane(EntityPZLP11.class, 1);
	
	public static final Item seat = new ItemSeat();
	public static final Item wheelSmall = new ItemWheelSmall();
	public static final Item wheelLarge = new ItemWheelLarge();
	public static final Item propeller = new ItemPropeller();
	public static final Item engineSmall = new ItemEngineSmall();
	public static final Item engineLarge = new ItemEngineLarge();
	public static final Item pointerShort = new ItemPointerShort();
	public static final Item pointerLong = new ItemPointerLong();
	public static final Item flightInstrumentBase = new ItemFlightInstrumentBase();
	public static final Item flightInstrument = new ItemFlightInstrument();
	
	private static int entityNumber = 0;
	private static int packetNumber = 0;
	
	public void init(){
		initEntites();
		initPackets();
		initItems();
		initRecipies();
		initFuels();
		NetworkRegistry.INSTANCE.registerGuiHandler(MFS.instance, new GUIHandler());
	}
	
	private void initEntites(){
		registerEntity(EntityMC172.class);
		registerEntity(EntityTrimotor.class);
		registerEntity(EntityOtter.class);
		registerEntity(EntityPZLP11.class);
		
		registerEntity(EntityCore.class);
		registerEntity(EntitySeat.class);
		registerEntity(EntityPlaneChest.class);
		registerEntity(EntityWheelSmall.class);
		registerEntity(EntityWheelLarge.class);
		registerEntity(EntityPropeller.class);
		registerEntity(EntityEngineSmall.class);
		registerEntity(EntityEngineLarge.class);
	}
	
	private void registerEntity(Class entityClass){
		EntityRegistry.registerModEntity(entityClass, entityClass.getName().substring(7), ++entityNumber, MFS.MODID, 80, 5, false);
	}
	
	private void initPackets(){
		MFS.MFSNet.registerMessage(ChatPacket.ChatPacketHandler.class,  ChatPacket.class, ++packetNumber, Side.CLIENT);
		MFS.MFSNet.registerMessage(FuelPacket.FuelPacketHandler.class,  FuelPacket.class, ++packetNumber, Side.CLIENT);
		MFS.MFSNet.registerMessage(ServerSendDataPacket.ServerSendDataPacketHandler.class,  ServerSendDataPacket.class, ++packetNumber, Side.CLIENT);
		MFS.MFSNet.registerMessage(ServerSyncPacket.ServerSyncPacketHandler.class,  ServerSyncPacket.class, ++packetNumber, Side.CLIENT);
		MFS.MFSNet.registerMessage(ClientRequestDataPacket.ClientRequestDataPacketHandler.class,  ClientRequestDataPacket.class, ++packetNumber, Side.SERVER);
		
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
		registerItem(planeMC172);
		//registerItem(planeTrimotor);
		registerItem(planeOtter);
		registerItem(planePLZP11);
		
		registerItem(seat);
		registerItem(wheelSmall);
		registerItem(wheelLarge);
		registerItem(propeller);
		registerItem(engineSmall);
		registerItem(engineLarge);
		registerItem(pointerShort);
		registerItem(pointerLong);
		registerItem(flightInstrumentBase);
		registerItem(flightInstrument);
	}
	
	private void registerItem(Item item){
		GameRegistry.registerItem(item, item.getUnlocalizedName().substring(5));
	}
	
	private void initRecipies(){
		this.initPropellerRecipes();
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
	}
	
	private void initPropellerRecipes(){
		GameRegistry.addRecipe(new ItemStack(propeller, 1, 1120),
				"  A",
				" B ",
				"A  ",
				'A', Blocks.planks, 'B', Items.iron_ingot);
		GameRegistry.addRecipe(new ItemStack(propeller, 1, 1121),
				"  A",
				" A ",
				"A  ",
				'A', Items.iron_ingot);
		GameRegistry.addRecipe(new ItemStack(propeller, 1, 1122),
				"  A",
				" B ",
				"A  ",
				'A', Blocks.obsidian, 'B', Items.iron_ingot);
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
		
		//Instrument 13 does not exist
		//Instrument 14 does not exist
	}
	
	private void initFuels(){
    	FluidContainerData[] fluidData = FluidContainerRegistry.getRegisteredFluidContainerData();
		for(FluidContainerData data : fluidData){
			if(data.emptyContainer.equals(FluidContainerRegistry.EMPTY_BUCKET)){
				MFS.fluidValues.put(data.fluid.getFluid().getName(), MFS.config.get("fuels", data.fluid.getFluid().getName(), data.fluid.getFluid().equals(FluidRegistry.LAVA) ? 1000 :0).getInt());
			}
		}
		MFS.config.save();
	}
	
	public void updateSeatedRider(EntitySeat seat, EntityLivingBase rider){}
	public EngineSound updateEngineSound(EngineSound sound, EntityEngine engine){return null;}
}
