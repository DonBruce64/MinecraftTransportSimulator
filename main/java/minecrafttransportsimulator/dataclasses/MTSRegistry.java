package minecrafttransportsimulator.dataclasses;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.BlockFuelPump;
import minecrafttransportsimulator.blocks.core.BlockPartBench;
import minecrafttransportsimulator.blocks.decor.BlockDecor1AxisIsolated;
import minecrafttransportsimulator.blocks.decor.BlockDecor2AxisIsolated;
import minecrafttransportsimulator.blocks.decor.BlockDecor6AxisOriented;
import minecrafttransportsimulator.blocks.decor.BlockDecor6AxisRegular;
import minecrafttransportsimulator.blocks.decor.BlockDecor6AxisSolidConnector;
import minecrafttransportsimulator.items.core.ItemInstrument;
import minecrafttransportsimulator.items.core.ItemKey;
import minecrafttransportsimulator.items.core.ItemManual;
import minecrafttransportsimulator.items.core.ItemMultipart;
import minecrafttransportsimulator.items.core.ItemWrench;
import minecrafttransportsimulator.items.parts.AItemPart;
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
import minecrafttransportsimulator.packets.general.ChatPacket;
import minecrafttransportsimulator.packets.general.ManualPageUpdatePacket;
import minecrafttransportsimulator.packets.general.PlayerCraftingPacket;
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
import minecrafttransportsimulator.packets.parts.PacketPartSeatRiderChange;
import minecrafttransportsimulator.packets.tileentities.FuelPumpConnectionPacket;
import minecrafttransportsimulator.packets.tileentities.FuelPumpFillDrainPacket;
import minecrafttransportsimulator.packets.tileentities.TileEntityClientServerHandshakePacket;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
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
	
	/**Maps multipart item names to items.  All multipart items for all packs will be populated here.*/
	public static Map<String, ItemMultipart> multipartItemMap = new LinkedHashMap<String, ItemMultipart>();
	
	/**Maps part item names to items.  All part items for all packs will be populated here.*/
	public static Map<String, AItemPart> partItemMap = new LinkedHashMap<String, AItemPart>();
	
	/**Maps instrument item names to items.  All instrument items for all packs will be populated here.*/
	public static Map<String, ItemInstrument> instrumentItemMap = new LinkedHashMap<String, ItemInstrument>();
	
	/**Core creative tab for base MTS items**/
	public static final CreativeTabCore coreTab = new CreativeTabCore();
	
	/**Map of creative tabs for packs.  Keyed by pack IDs.  Populated by the {@link PackParserSystem}**/
	public static final Map<String, CreativeTabPack> packTabs = new HashMap<String, CreativeTabPack>();

	//Multipart interaction items.
	public static final Item manual = new ItemManual().setCreativeTab(coreTab);
	public static final Item wrench = new ItemWrench().setCreativeTab(coreTab);
	public static final Item key = new ItemKey().setCreativeTab(coreTab);
	
	//Crafting bench blocks.
	public static final Block propellerBench = new BlockPartBench(new String[]{"propeller"}).setCreativeTab(coreTab);
	public static final Item itemBlockPropellerBench = new ItemBlock(propellerBench);
	public static final Block engineHoist = new BlockPartBench(new String[]{"engine_aircraft", "engine_car"}).setCreativeTab(coreTab);
	public static final Item itemBlockEngineHoist = new ItemBlock(engineHoist);
	
	//Fuel pump.
	public static final Block fuelPump = new BlockFuelPump().setCreativeTab(coreTab);		
	public static final Item itemBlockFuelPump = new ItemBlock(fuelPump);
	
	//Decorative pole-based blocks.
	public static final Block pole = new BlockDecor6AxisRegular(Material.IRON, 5.0F, 10.0F);
	public static final Item itemBlockPole = new ItemBlock(pole);
	public static final Block poleBase = new BlockDecor6AxisSolidConnector(Material.IRON, 5.0F, 10.0F);
	public static final Item itemBlockPoleBase = new ItemBlock(poleBase);
	public static final Block trafficSignal = new BlockDecor6AxisOriented(Material.IRON, 5.0F, 10.0F);
	public static final Item itemBlockTrafficSignal = new ItemBlock(trafficSignal);
	public static final Block streetLight = new BlockDecor6AxisOriented(Material.IRON, 5.0F, 10.0F);
	public static final Item itemBlockStreetLight = new ItemBlock(streetLight);
		
	//Decorative ground blocks.
	public static final Block trafficCone = new BlockDecor1AxisIsolated(Material.CLAY, 0.4375F, 0.75F, 0.6F, 0.75F);
	public static final Item itemBlockTrafficCone = new ItemBlock(trafficCone);
	public static final Block crashBarrier = new BlockDecor2AxisIsolated(Material.ROCK, 1.5F, 10.0F, 0.5625F, 0.84375F, 1.0F);
	public static final Item itemBlockCrashBarrier = new ItemBlock(crashBarrier);
	
	//Counters for registry systems.
	private static int entityNumber = 0;
	private static int packetNumber = 0;
	private static int craftingNumber = 0;
	
	
	/**All run-time things go here.**/
	public static void init(){
		initMultipartEntities();
		initPackets();
		initCoreItemRecipes();
	}
	
	/**
	 * This is called by packs to query what items they have registered.
	 * Used to allow packs to register their own items after core mod processing.
	 */
	public static List<Item> getItemsForPack(String modID){
		List<Item> packItems = new ArrayList<Item>();
		for(ItemMultipart item : multipartItemMap.values()){
			if(item.multipartName.startsWith(modID)){
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
		return packItems;
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
					String name = field.getName().toLowerCase();
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
		
		
		//Next add multipart items to the lists and creative tabs.
		for(String multipartName : PackParserSystem.getAllMultipartPackNames()){
			ItemMultipart itemMultipart = new ItemMultipart(multipartName);
			multipartItemMap.put(multipartName, itemMultipart);
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
		registerPacket(ManualPageUpdatePacket.class, ManualPageUpdatePacket.Handler.class, false, true);
		registerPacket(PlayerCraftingPacket.class, PlayerCraftingPacket.Handler.class, false, true);
		
		//Packets in packets.tileentity
		registerPacket(FuelPumpConnectionPacket.class, FuelPumpConnectionPacket.Handler.class, true, false);
		registerPacket(FuelPumpFillDrainPacket.class, FuelPumpFillDrainPacket.Handler.class, true, false);
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
		registerPacket(PacketPartSeatRiderChange.class, PacketPartSeatRiderChange.Handler.class, true, false);
	}
	
	private static void initCoreItemRecipes(){
		//Propeller bench
		registerRecipe(new ItemStack(itemBlockPropellerBench),
				"AAA",
				" BA",
				"ACA",
				'A', Items.IRON_INGOT,
				'B', Items.DIAMOND,
				'C', Blocks.ANVIL);
		//Engine hoist
		registerRecipe(new ItemStack(itemBlockEngineHoist),
				"AAA",
				"BDA",
				" CC",
				'A', Items.IRON_INGOT,
				'B', Blocks.IRON_BARS,
				'C', Blocks.IRON_BLOCK,
				'D', new ItemStack(Items.DYE, 1, 1));
		
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
