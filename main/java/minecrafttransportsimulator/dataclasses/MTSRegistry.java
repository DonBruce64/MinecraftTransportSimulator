package minecrafttransportsimulator.dataclasses;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.BlockDecor;
import minecrafttransportsimulator.blocks.core.BlockRotatable;
import minecrafttransportsimulator.blocks.pole.BlockPoleAttachment;
import minecrafttransportsimulator.blocks.pole.BlockPoleNormal;
import minecrafttransportsimulator.blocks.pole.BlockPoleSign;
import minecrafttransportsimulator.blocks.pole.BlockPoleWallConnector;
import minecrafttransportsimulator.items.blocks.ItemBlockBench;
import minecrafttransportsimulator.items.blocks.ItemBlockFuelPump;
import minecrafttransportsimulator.items.blocks.ItemBlockRotatable;
import minecrafttransportsimulator.items.blocks.ItemBlockTrafficSignalController;
import minecrafttransportsimulator.items.core.ItemDecor;
import minecrafttransportsimulator.items.core.ItemInstrument;
import minecrafttransportsimulator.items.core.ItemKey;
import minecrafttransportsimulator.items.core.ItemManual;
import minecrafttransportsimulator.items.core.ItemVehicle;
import minecrafttransportsimulator.items.core.ItemWrench;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.packets.control.AileronPacket;
import minecrafttransportsimulator.packets.control.BrakePacket;
import minecrafttransportsimulator.packets.control.ElevatorPacket;
import minecrafttransportsimulator.packets.control.FlapPacket;
import minecrafttransportsimulator.packets.control.HornPacket;
import minecrafttransportsimulator.packets.control.LightPacket;
import minecrafttransportsimulator.packets.control.ReverseThrustPacket;
import minecrafttransportsimulator.packets.control.RudderPacket;
import minecrafttransportsimulator.packets.control.ShiftPacket;
import minecrafttransportsimulator.packets.control.SirenPacket;
import minecrafttransportsimulator.packets.control.SteeringPacket;
import minecrafttransportsimulator.packets.control.ThrottlePacket;
import minecrafttransportsimulator.packets.control.TrimPacket;
import minecrafttransportsimulator.packets.general.PacketBulletHit;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.packets.general.PacketManualPageUpdate;
import minecrafttransportsimulator.packets.general.PacketPackReload;
import minecrafttransportsimulator.packets.general.PacketPlayerCrafting;
import minecrafttransportsimulator.packets.parts.PacketPartEngineDamage;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal;
import minecrafttransportsimulator.packets.parts.PacketPartGroundDeviceWheelFlat;
import minecrafttransportsimulator.packets.parts.PacketPartGunReload;
import minecrafttransportsimulator.packets.parts.PacketPartGunSignal;
import minecrafttransportsimulator.packets.parts.PacketPartInteraction;
import minecrafttransportsimulator.packets.parts.PacketPartSeatRiderChange;
import minecrafttransportsimulator.packets.tileentities.PacketFuelPumpConnection;
import minecrafttransportsimulator.packets.tileentities.PacketFuelPumpFillDrain;
import minecrafttransportsimulator.packets.tileentities.PacketSignChange;
import minecrafttransportsimulator.packets.tileentities.PacketTileEntityClientServerHandshake;
import minecrafttransportsimulator.packets.tileentities.PacketTrafficSignalControllerChange;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleAttacked;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientInit;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientInitResponse;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientPartAddition;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientPartRemoval;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientRemoval;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleDeltas;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleInstruments;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleKey;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleNameTag;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleServerPartAddition;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleWindowBreak;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleWindowFix;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Car;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Plane;
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
	/**All registered core items are stored in this list as they are added.  Used to sort items in the creative tab.**/
	public static List<Item> itemList = new ArrayList<Item>();
	
	/**Maps vehicle item names to items.  All vehicle items for all packs will be populated here.*/
	public static Map<String, ItemVehicle> vehicleItemMap = new LinkedHashMap<String, ItemVehicle>();
	
	/**Maps part item names to items.  All part items for all packs will be populated here.*/
	public static Map<String, AItemPart> partItemMap = new LinkedHashMap<String, AItemPart>();
	
	/**Maps instrument item names to items.  All instrument items for all packs will be populated here.*/
	public static Map<String, ItemInstrument> instrumentItemMap = new LinkedHashMap<String, ItemInstrument>();
	
	/**Maps decor item names to items.  All decor items for all packs will be populated here.*/
	public static Map<String, ItemDecor> decorItemMap = new LinkedHashMap<String, ItemDecor>();
	
	/**Maps rotatable blocks to their items.  Used to return the correct item when they are broken.*/
	public static Map<BlockRotatable, ItemBlockRotatable> rotatableItemMap = new LinkedHashMap<BlockRotatable, ItemBlockRotatable>();
	
	/**Core creative tab for base MTS items**/
	public static final CreativeTabCore coreTab = new CreativeTabCore();
	
	/**Map of creative tabs for packs.  Keyed by pack IDs.  Populated by the {@link PackParserSystem}**/
	public static final Map<String, CreativeTabPack> packTabs = new HashMap<String, CreativeTabPack>();

	//Vehicle interaction items.
	public static final Item manual = new ItemManual().setCreativeTab(coreTab);
	public static final Item wrench = new ItemWrench().setCreativeTab(coreTab);
	public static final Item key = new ItemKey().setCreativeTab(coreTab);
	
	//Crafting benches.
	public static final Item vehicleBench = new ItemBlockBench("plane", "car").createBlocks();
	public static final Item propellerBench = new ItemBlockBench("propeller").createBlocks();
	public static final Item engineBench = new ItemBlockBench("engine_aircraft", "engine_jet", "engine_car").createBlocks();
	public static final Item wheelBench = new ItemBlockBench("pontoon", "skid", "tread", "wheel").createBlocks();
	public static final Item seatBench = new ItemBlockBench("seat", "crate", "barrel").createBlocks();
	public static final Item gunBench = new ItemBlockBench("gun_fixed", "gun_").createBlocks();
	public static final Item customBench = new ItemBlockBench("custom").createBlocks();
	public static final Item instrumentBench = new ItemBlockBench("instrument").createBlocks();
	
	//Fuel pump.
	public static final Item fuelPump = new ItemBlockFuelPump().createBlocks();		
	
	//Traffic Controller
	public static final Item trafficSignalController = new ItemBlockTrafficSignalController().createBlocks();
	
	//Pole-based blocks.
	public static final Block pole = new BlockPoleNormal(0.125F);
	public static final Item itemBlockPole = new ItemBlock(pole);
	public static final Block poleBase = new BlockPoleWallConnector(0.125F);
	public static final Item itemBlockPoleBase = new ItemBlock(poleBase);
	public static final Block trafficSignal = new BlockPoleAttachment(0.125F);
	public static final Item itemBlockTrafficSignal = new ItemBlock(trafficSignal);
	public static final Block streetLight = new BlockPoleAttachment(0.125F);
	public static final Item itemBlockStreetLight = new ItemBlock(streetLight);
	public static final Block trafficSign = new BlockPoleSign(0.125F);
	public static final Item itemBlockTrafficSign = new ItemBlock(trafficSign);
		
	//Decor blocks.
	public static final Block decorBasicDark = new BlockDecor(false, false);
	public static final Block decorOrientedDark = new BlockDecor(true, false);
	public static final Block decorBasicLight = new BlockDecor(false, true);
	public static final Block decorOrientedLight = new BlockDecor(true, true);
	
	//Counters for registry systems.
	private static int entityNumber = 0;
	private static int packetNumber = 0;
	private static int craftingNumber = 0;
	
	
	/**All run-time things go here.**/
	public static void init(){
		initEntities();
		initPackets();
		initCoreItemRecipes();
		initCraftingBenchItemRecipes();
		initDecorItemRecipes();
	}
	
	/**
	 * This is called by packs to query what items they have registered.
	 * Used to allow packs to register their own items after core mod processing.
	 */
	public static List<Item> getItemsForPack(String modID){
		List<Item> packItems = new ArrayList<Item>();
		for(ItemVehicle item : vehicleItemMap.values()){
			if(item.vehicleName.startsWith(modID)){
				packItems.add(item);
			}
		}
		for(AItemPart item : partItemMap.values()){
			if(item.partName.startsWith(modID)){
				packItems.add(item);
			}
		}
		for(ItemInstrument item : instrumentItemMap.values()){
			if(item.instrumentName.startsWith(modID)){
				packItems.add(item);
			}
		}
		for(ItemDecor item : decorItemMap.values()){
			if(item.decorName.startsWith(modID)){
				packItems.add(item);
			}
		}
		return packItems;
	}
	
	/**
	 * Registers all blocks present in this class, as well as blocks for rotatable items.
	 * Also adds the respective TileEntity if the block has one.
	 */
	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event){
		//Need to keep track of which TE classes we've registered so we don't double-register them for blocks that use the same TE.
		List<Class<? extends TileEntity>> registeredTileEntityClasses = new ArrayList<Class<? extends TileEntity>>();
		for(Field field : MTSRegistry.class.getFields()){
			if(field.getType().equals(Block.class)){
				try{
					Block block = (Block) field.get(null);
					String name = field.getName().toLowerCase();
					event.getRegistry().register(block.setRegistryName(name).setUnlocalizedName(name));
					if(block instanceof ITileEntityProvider){
						Class<? extends TileEntity> tileEntityClass = ((ITileEntityProvider) block).createNewTileEntity(null, 0).getClass();
						if(!registeredTileEntityClasses.contains(tileEntityClass)){
							GameRegistry.registerTileEntity(tileEntityClass, tileEntityClass.getSimpleName());
							registeredTileEntityClasses.add(tileEntityClass);
						}
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}else if(field.getType().equals(Item.class)){
				try{
					if(field.get(null) instanceof ItemBlockRotatable){
						ItemBlockRotatable item = (ItemBlockRotatable) field.get(null);
						for(byte i=0; i<item.blocks.length; ++i){
							BlockRotatable block = item.blocks[i];
							String name = field.getName().toLowerCase() + "_" + i;
							event.getRegistry().register(block.setRegistryName(name).setUnlocalizedName(name));
							rotatableItemMap.put(block, item);
						}
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Registers all items (and itemblocks) present in this class.
	 * Does not register any items from packs as Forge doesn't like us
	 * registering pack-mod prefixed items from the core class.
	 * We can, however, add them to the appropriate maps pending the registration
	 * on the pack side.  That way all the pack has to do is set the
	 * registry name of an item and register it, which doesn't involve
	 * anything complicated.
	 */
	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event){
		//First register all core items.
		for(Field field : MTSRegistry.class.getFields()){
			if(field.getType().equals(Item.class)){
				try{
					Item item = (Item) field.get(null);
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
		
		
		//Next add multipart items to the lists and creative tabs.
		for(String multipartName : PackParserSystem.getAllVehiclePackNames()){
			ItemVehicle itemMultipart = new ItemVehicle(multipartName);
			vehicleItemMap.put(multipartName, itemMultipart);
		}
		
		//Now add part items to the lists.
		for(String partName : PackParserSystem.getAllPartPackNames()){
			try{
				Class<? extends AItemPart> itemClass = PackParserSystem.getPartItemClass(partName);
				Constructor<? extends AItemPart> construct = itemClass.getConstructor(String.class);
				AItemPart itemPart = construct.newInstance(partName);
				partItemMap.put(partName, itemPart);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		//Then add instrument items to the lists.
		for(String instrumentName : PackParserSystem.getAllInstruments()){
			ItemInstrument itemInstrument = new ItemInstrument(instrumentName);
			instrumentItemMap.put(instrumentName, itemInstrument);
		}
		
		//Next add decor items to the lists.
		for(String decorName : PackParserSystem.getAllDecor()){
			ItemDecor itemDecor = new ItemDecor(decorName);
			decorItemMap.put(decorName, itemDecor);
		}
	}

	/**
	 * Registers all entities with the entity registry.
	 * For vehicles we only register the main classes as
	 * the pack data stored in NBT is what makes for different vehicles.
	 */
	private static void initEntities(){
		EntityRegistry.registerModEntity(EntityVehicleF_Car.class, "vehiclecar", entityNumber++, MTS.MODID, 80, 5, false);
		EntityRegistry.registerModEntity(EntityVehicleF_Plane.class, "vehicleplane", entityNumber++, MTS.MODID, 80, 5, false);
	}
	
	private static void initPackets(){
		//Packets in packets.control
		registerPacket(AileronPacket.class, AileronPacket.Handler.class, true, true);
		registerPacket(BrakePacket.class, BrakePacket.Handler.class, true, true);
		registerPacket(ElevatorPacket.class, ElevatorPacket.Handler.class, true, true);
		registerPacket(FlapPacket.class, FlapPacket.Handler.class, true, true);
		registerPacket(HornPacket.class, HornPacket.Handler.class, true, true);
		registerPacket(LightPacket.class, LightPacket.Handler.class, true, true);
		registerPacket(ReverseThrustPacket.class, ReverseThrustPacket.Handler.class, true, true);
		registerPacket(RudderPacket.class, RudderPacket.Handler.class, true, true);
		registerPacket(SirenPacket.class, SirenPacket.Handler.class, true, true);
		registerPacket(ShiftPacket.class, ShiftPacket.Handler.class, true, true);
		registerPacket(SteeringPacket.class, SteeringPacket.Handler.class, true, true);
		registerPacket(ThrottlePacket.class, ThrottlePacket.Handler.class, true, true);
		registerPacket(TrimPacket.class, TrimPacket.Handler.class, true, true);
		
		//Packets in packets.general
		registerPacket(PacketBulletHit.class, PacketBulletHit.Handler.class, true, true);
		registerPacket(PacketChat.class, PacketChat.Handler.class, true, false);
		registerPacket(PacketPartGunReload.class, PacketPartGunReload.Handler.class, true, false);
		registerPacket(PacketManualPageUpdate.class, PacketManualPageUpdate.Handler.class, false, true);
		registerPacket(PacketPackReload.class, PacketPackReload.Handler.class, false, true);
		registerPacket(PacketPlayerCrafting.class, PacketPlayerCrafting.Handler.class, false, true);
		
		//Packets in packets.tileentity
		registerPacket(PacketFuelPumpConnection.class, PacketFuelPumpConnection.Handler.class, true, false);
		registerPacket(PacketFuelPumpFillDrain.class, PacketFuelPumpFillDrain.Handler.class, true, false);
		registerPacket(PacketSignChange.class, PacketSignChange.Handler.class, true, true);
		registerPacket(PacketTileEntityClientServerHandshake.class, PacketTileEntityClientServerHandshake.Handler.class, true, true);
		registerPacket(PacketTrafficSignalControllerChange.class, PacketTrafficSignalControllerChange.Handler.class, true, true);
		
		//Packets in packets.vehicles.
		registerPacket(PacketVehicleAttacked.class, PacketVehicleAttacked.Handler.class, false, true);
		registerPacket(PacketVehicleClientInit.class, PacketVehicleClientInit.Handler.class, false, true);
		registerPacket(PacketVehicleClientInitResponse.class, PacketVehicleClientInitResponse.Handler.class, true, false);
		registerPacket(PacketVehicleClientPartAddition.class, PacketVehicleClientPartAddition.Handler.class, true, false);
		registerPacket(PacketVehicleClientPartRemoval.class, PacketVehicleClientPartRemoval.Handler.class, true, false);
		registerPacket(PacketVehicleClientRemoval.class, PacketVehicleClientRemoval.Handler.class, true, false);
		registerPacket(PacketVehicleDeltas.class, PacketVehicleDeltas.Handler.class, true, false);
		registerPacket(PacketVehicleInstruments.class, PacketVehicleInstruments.Handler.class, true, true);
		registerPacket(PacketVehicleKey.class, PacketVehicleKey.Handler.class, true, true);
		registerPacket(PacketVehicleNameTag.class, PacketVehicleNameTag.Handler.class, true, true);
		registerPacket(PacketVehicleServerPartAddition.class, PacketVehicleServerPartAddition.Handler.class, false, true);
		registerPacket(PacketVehicleWindowBreak.class, PacketVehicleWindowBreak.Handler.class, true, false);
		registerPacket(PacketVehicleWindowFix.class, PacketVehicleWindowFix.Handler.class, true, true);
		
		//Packets in packets.parts
		registerPacket(PacketPartEngineDamage.class, PacketPartEngineDamage.Handler.class, true, false);
		registerPacket(PacketPartEngineSignal.class, PacketPartEngineSignal.Handler.class, true, true);
		registerPacket(PacketPartGroundDeviceWheelFlat.class, PacketPartGroundDeviceWheelFlat.Handler.class, true, false);
		registerPacket(PacketPartGunSignal.class, PacketPartGunSignal.Handler.class, true, true);
		registerPacket(PacketPartInteraction.class, PacketPartInteraction.Handler.class, false, true);
		registerPacket(PacketPartSeatRiderChange.class, PacketPartSeatRiderChange.Handler.class, true, false);
	}
	
	private static void initCoreItemRecipes(){
		//Manual
		registerRecipe(new ItemStack(manual),
				" A ",
				"CBC",
				" D ",
				'A', Items.FEATHER,
				'B', Items.BOOK,
				'C', new ItemStack(Items.DYE, 1, 0),
				'D', Items.PAPER);
		//Wrench
		registerRecipe(new ItemStack(wrench),
				"  A",
				" A ",
				"A  ",
				'A', Items.IRON_INGOT);
		//Key
		registerRecipe(new ItemStack(key),
				" A ",
				" A ",
				" S ",
				'A', Items.IRON_INGOT,
				'S', Items.STRING);
		//Fuel pump
		registerRecipe(new ItemStack(fuelPump),
				"DED",
				"CBC",
				"AAA",
				'A', new ItemStack(Blocks.STONE_SLAB, 1, 0),
				'B', Items.IRON_INGOT,
				'C', new ItemStack(Items.DYE, 1, 0),
				'D', new ItemStack(Items.DYE, 1, 1),
				'E', Blocks.GLASS_PANE);
		//Traffic Signal Controller
		registerRecipe(new ItemStack(trafficSignalController),
				"AAA",
				"BCB",
				"DBD",
				'A', new ItemStack(Blocks.STONE_SLAB, 1, 0),
				'B', Items.IRON_INGOT,
				'C', Items.COMPARATOR,
				'D', Items.REDSTONE);
	}
	
	private static void initCraftingBenchItemRecipes(){
		//Vehicle benches.  Need to iterate through all fence types.
		for(Block fenceBlock : new Block[]{Blocks.OAK_FENCE, Blocks.SPRUCE_FENCE, Blocks.BIRCH_FENCE, Blocks.JUNGLE_FENCE, Blocks.ACACIA_FENCE, Blocks.DARK_OAK_FENCE}){
			registerRecipe(new ItemStack(vehicleBench),
					"AAA",
					"ACA",
					"B B",
					'A', Blocks.PLANKS,
					'B', fenceBlock,
					'C', Blocks.GLASS_PANE);
		}
		//Propeller bench
		registerRecipe(new ItemStack(propellerBench),
				"AAA",
				" BA",
				"ACA",
				'A', Items.IRON_INGOT,
				'B', Items.DIAMOND,
				'C', Blocks.ANVIL);
		//Engine bench
		registerRecipe(new ItemStack(engineBench),
				"AAA",
				"BDA",
				" CC",
				'A', Items.IRON_INGOT,
				'B', Blocks.IRON_BARS,
				'C', Blocks.IRON_BLOCK,
				'D', new ItemStack(Items.DYE, 1, 1));
		//Wheel bench
		registerRecipe(new ItemStack(wheelBench),
				"A  ",
				"ACC",
				"BCC",
				'A', Items.IRON_INGOT,
				'B', Blocks.ANVIL,
				'C', Blocks.PLANKS);
		//Seat bench
		registerRecipe(new ItemStack(seatBench),
				"   ",
				"ABA",
				"A A",
				'A', Items.IRON_INGOT,
				'B', Items.IRON_AXE);
		//Custom bench
		registerRecipe(new ItemStack(customBench),
				"AAA",
				"ABA",
				"AAA",
				'A', Items.IRON_INGOT,
				'B', Blocks.CRAFTING_TABLE);
		//Instrument bench
		registerRecipe(new ItemStack(instrumentBench),
				"AGA",
				"PRP",
				"P P",
				'A', Items.IRON_INGOT,
				'G', Blocks.GLOWSTONE,
				'R', Blocks.REDSTONE_BLOCK,
				'P', Blocks.PLANKS);
	}
	
	private static void initDecorItemRecipes(){
		//Regular pole
		registerRecipe(new ItemStack(itemBlockPole, 4),
				" S ",
				" S ",
				" S ",
				'S', Blocks.COBBLESTONE_WALL);
		//Base pole
		registerRecipe(new ItemStack(itemBlockPoleBase),
				"   ",
				" P ",
				" S ",
				'S', Blocks.STONE_SLAB,
				'P', itemBlockPole);
		//Street light
		registerRecipe(new ItemStack(itemBlockStreetLight),
				" SP",
				" G ",
				"   ",
				'S', Blocks.STONE_SLAB,
				'P', itemBlockPole,
				'G', Items.GLOWSTONE_DUST);
		//Traffic signal
		registerRecipe(new ItemStack(itemBlockTrafficSignal),
				"RLS",
				"YLP",
				"GLA",
				'R', new ItemStack(Items.DYE, 1, 1),
				'Y', new ItemStack(Items.DYE, 1, 11),
				'G', new ItemStack(Items.DYE, 1, 10),
				'L', Items.GLOWSTONE_DUST,
				'A', Items.REDSTONE,
				'S', Blocks.STONE_SLAB,
				'P', itemBlockPole);
		//Traffic sign
		registerRecipe(new ItemStack(itemBlockTrafficSign),
				"   ",
				" I ",
				" P ",
				'I', Items.IRON_INGOT,
				'P', itemBlockPole);
	}
	
	/**Registers a crafting recipe.  This is segmented out here as the method changes in 1.12 and the single location makes it easy for the script to update it.**/
	private static void registerRecipe(ItemStack output, Object...params){
		GameRegistry.addShapedRecipe(output, params);
		++craftingNumber;
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
}
