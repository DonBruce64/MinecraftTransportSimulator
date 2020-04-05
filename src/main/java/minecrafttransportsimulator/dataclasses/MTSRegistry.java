package minecrafttransportsimulator.dataclasses;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.instances.BlockFuelPump;
import minecrafttransportsimulator.blocks.instances.BlockPartsBench;
import minecrafttransportsimulator.blocks.instances.BlockPole;
import minecrafttransportsimulator.blocks.instances.BlockPoleCrossingSignal;
import minecrafttransportsimulator.blocks.instances.BlockPoleSign;
import minecrafttransportsimulator.blocks.instances.BlockPoleSolidConnector;
import minecrafttransportsimulator.blocks.instances.BlockPoleStreetlight;
import minecrafttransportsimulator.blocks.instances.BlockPoleTrafficSignal;
import minecrafttransportsimulator.blocks.instances.BlockTrafficSignalController;
import minecrafttransportsimulator.guis.GUIPartBench;
import minecrafttransportsimulator.items.core.ItemJerrycan;
import minecrafttransportsimulator.items.core.ItemJumperCable;
import minecrafttransportsimulator.items.core.ItemKey;
import minecrafttransportsimulator.items.core.ItemWrench;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONSign;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.packets.general.PacketBulletHit;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.packets.general.PacketPlayerCrafting;
import minecrafttransportsimulator.packets.parts.PacketPartEngineDamage;
import minecrafttransportsimulator.packets.parts.PacketPartEngineLinked;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal;
import minecrafttransportsimulator.packets.parts.PacketPartGroundDeviceWheelFlat;
import minecrafttransportsimulator.packets.parts.PacketPartGunReload;
import minecrafttransportsimulator.packets.parts.PacketPartGunSignal;
import minecrafttransportsimulator.packets.parts.PacketPartSeatRiderChange;
import minecrafttransportsimulator.packets.tileentities.PacketSignChange;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientInit;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientInitResponse;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientPartAddition;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientPartRemoval;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleDeltas;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleInteract;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleJerrycan;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleKey;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleNameTag;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Blimp;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Boat;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Car;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Plane;
import minecrafttransportsimulator.wrappers.WrapperBlock;
import minecrafttransportsimulator.wrappers.WrapperBlockAxial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.registry.EntityRegistry;
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
	public static List<Item> coreItems = new ArrayList<Item>();
	
	/**All registered pack items are stored in this map as they are added.  Used to sort items in the creative tab,
	 * and will be sent to packs for item registration when so asked via {@link #getItemsForPack(String)}.  May also
	 * be used if we need to lookup a registered part item.  Map is keyed by packID to allow sorting for items from 
	 * different packs, while the sub-map is keyed by the part's {@link AJSONItem#systemName}.**/
	public static TreeMap<String, LinkedHashMap<String, AItemPack<? extends AJSONItem<?>>>> packItemMap = new TreeMap<String, LinkedHashMap<String, AItemPack<? extends AJSONItem<?>>>>();
	
	/**Special map for signs, as they don't have items associated with them.  Instead, they just have a definition
	 * that's linked to a unique two-string keyset like the items do.**/
	public static TreeMap<String, LinkedHashMap<String, JSONSign>> packSignMap = new TreeMap<String, LinkedHashMap<String, JSONSign>>();
	
	/**Maps pack items to their list of crafting ingredients.  This is used rather than the core JSON to allow for
	 * overriding the crafting materials in said JSON, and to concatonate the materials in {@link JSONVehicle}*/
	public static final Map<AItemPack<? extends AJSONItem<?>>, String[]> packCraftingMap = new HashMap<AItemPack<? extends AJSONItem<?>>, String[]>();
	
	/**Core creative tab for base MTS items**/
	public static final CreativeTabCore coreTab = new CreativeTabCore();
	
	/**Map of creative tabs for packs.  Keyed by packID.  Populated by the {@link PackParserSystem}**/
	public static final Map<String, CreativeTabPack> packTabs = new HashMap<String, CreativeTabPack>();

	//Booklets for manuals.  Not made final as they are created dynamically at runtime.
	public static Item handbook_en;
	public static Item handbook_ru;
	
	//Vehicle interaction items.
	public static final Item wrench = new ItemWrench().setCreativeTab(coreTab);
	public static final Item key = new ItemKey().setCreativeTab(coreTab);
	public static final Item jumperCable = new ItemJumperCable().setCreativeTab(coreTab);
	public static final Item jerrycan = new ItemJerrycan().setCreativeTab(coreTab);
	
	//Crafting benches.
	public static final WrapperBlock vehicleBench = new WrapperBlock(new BlockPartsBench(JSONVehicle.class));
	public static final WrapperBlock propellerBench = new WrapperBlock(new BlockPartsBench(JSONPart.class, "propeller"));
	public static final WrapperBlock engineBench = new WrapperBlock(new BlockPartsBench(JSONPart.class, "engine_aircraft", "engine_jet", "engine_car", "engine_boat"));
	public static final WrapperBlock wheelBench = new WrapperBlock(new BlockPartsBench(JSONPart.class, "wheel", "skid", "pontoon", "tread"));
	public static final WrapperBlock seatBench = new WrapperBlock(new BlockPartsBench(JSONPart.class, "seat", "crate", "barrel", "crafting_table", "furnace", "brewing_stand"));
	public static final WrapperBlock gunBench = new WrapperBlock(new BlockPartsBench(JSONPart.class, "gun_fixed", "gun_tripod", "bullet"));
	public static final WrapperBlock customBench = new WrapperBlock(new BlockPartsBench(JSONPart.class, "custom"));
	public static final WrapperBlock instrumentBench = new WrapperBlock(new BlockPartsBench(JSONInstrument.class));
	public static final WrapperBlock componentBench = new WrapperBlock(new BlockPartsBench(JSONItem.class));
	public static final WrapperBlock decorBench = new WrapperBlock(new BlockPartsBench(JSONDecor.class));
	
	//Fuel pump.
	public static final WrapperBlock fuelPump = new WrapperBlock(new BlockFuelPump());
	
	//Traffic signal Controller
	public static final WrapperBlock trafficSignalController = new WrapperBlock(new BlockTrafficSignalController());
	
	//Pole-based blocks.
	public static final WrapperBlock pole = new WrapperBlockAxial(new BlockPole());
	public static final WrapperBlock poleBase = new WrapperBlockAxial(new BlockPoleSolidConnector());
	public static final WrapperBlock trafficSignal = new WrapperBlockAxial(new BlockPoleTrafficSignal());
	public static final WrapperBlock crossingSignal = new WrapperBlockAxial(new BlockPoleCrossingSignal());
	public static final WrapperBlock streetLight = new WrapperBlockAxial(new BlockPoleStreetlight());
	public static final WrapperBlock trafficSign = new WrapperBlockAxial(new BlockPoleSign());
	
	//Counter for packets.
	private static int packetNumber = 0;
	
	/**All run-time things go here.**/
	public static void init(){
		initEntities();
		initPackets();
	}
	
	/**
	 * This is called by packs to query what items they have registered.
	 * Used to allow packs to register their own items after core mod processing.
	 * We need to cast-down the items to the Item class as a List with type Item is what
	 * the packloader is expecting.
	 */
	public static List<Item> getItemsForPack(String packID){
		List<Item> items = new ArrayList<Item>();
		for(AItemPack<? extends AJSONItem<?>> packItem : packItemMap.get(packID).values()){
			items.add(packItem);
		}
		return items;
	}
	
	/**
	 * This method returns a list of ItemStacks that are required
	 * to craft the passed-in pack item.  Used by {@link GUIPartBench}
	 * amd {@link PacketPlayerCrafting} as well as any other systems that 
	 * need to know what materials make up pack items.
	 */
    public static List<ItemStack> getMaterials(AItemPack<? extends AJSONItem<?>> item){
    	final List<ItemStack> materialList = new ArrayList<ItemStack>();
		try{
	    	for(String itemText : MTSRegistry.packCraftingMap.get(item)){
				int itemQty = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
				itemText = itemText.substring(0, itemText.lastIndexOf(':'));
				
				int itemMetadata = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
				itemText = itemText.substring(0, itemText.lastIndexOf(':'));
				materialList.add(new ItemStack(Item.getByNameOrId(itemText), itemQty, itemMetadata));
			}
		}catch(Exception e){
			throw new NullPointerException("ERROR: Could not parse crafting ingredients for item: " + item.definition.packID + item.definition.systemName + ".  Report this to the pack author!");
		}
    	return materialList;
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
		//Before doing any item registration, create the pack handbooks.
		//These are special, as they don't come from any packs, yet they use the booklet code.
		//This is done to avoid the need to make a new GUI.
		try {
			PackParserSystem.addBookletDefinition(new InputStreamReader(MTSRegistry.class.getResourceAsStream("/assets/" + MTS.MODID + "/jsondefs/booklets/handbook_en.json"), "UTF-8"), "handbook_en", MTS.MODID);
			PackParserSystem.addBookletDefinition(new InputStreamReader(MTSRegistry.class.getResourceAsStream("/assets/" + MTS.MODID + "/jsondefs/booklets/handbook_ru.json"), "UTF-8"), "handbook_ru", MTS.MODID);
			handbook_en = MTSRegistry.packItemMap.get(MTS.MODID).get("handbook_en").setUnlocalizedName("mts:handbook_en");
			handbook_ru = MTSRegistry.packItemMap.get(MTS.MODID).get("handbook_ru").setUnlocalizedName("mts:handbook_ru");
		}catch(Exception e){
			MTS.MTSLog.error("ERROR PARSING HANDBOOK AS UTF-8 STRING ENCODING!  HANDBOOKS MAY NOT APPEAR!");
			MTS.MTSLog.error(e.getMessage());
		}finally{
			//Get rid of the handbooks from the pack item map as those shouldn't exist.
			MTSRegistry.packItemMap.remove(MTS.MODID);
		}
		
		
		//Now register all core items.
		for(Field field : MTSRegistry.class.getFields()){
			if(field.getType().equals(Item.class)){
				try{
					Item item = (Item) field.get(null);
					String name = field.getName().toLowerCase();
					event.getRegistry().register(item.setRegistryName(name).setUnlocalizedName(name));
					item.setCreativeTab(coreTab);
					coreItems.add(item);
				}catch(Exception e){
					e.printStackTrace();
				}
			}else if(field.getType().equals(WrapperBlock.class)){
				//Also need to make itemblocks for all blocks.
				//This doesn't include packs, which have their own items.
				try{
					WrapperBlock block = (WrapperBlock) field.get(null);
					ItemBlock itemBlock = new ItemBlock(block);
					itemBlock.setCreativeTab(coreTab);
					event.getRegistry().register(itemBlock.setRegistryName(block.getRegistryName()).setUnlocalizedName(block.getRegistryName().toString()));
					MTSRegistry.coreItems.add(itemBlock);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Registers all entities with the entity registry.
	 * For vehicles we only register the main classes as
	 * the pack data stored in NBT is what makes for different vehicles.
	 */
	private static void initEntities(){
		int entityNumber = 0;
		EntityRegistry.registerModEntity(new ResourceLocation(MTS.MODID, EntityVehicleG_Car.class.getSimpleName().substring(6).toLowerCase()), EntityVehicleG_Car.class, "vehiclecar", entityNumber++, MTS.MODID, 256, 5, false);
		EntityRegistry.registerModEntity(new ResourceLocation(MTS.MODID, EntityVehicleG_Boat.class.getSimpleName().substring(6).toLowerCase()), EntityVehicleG_Boat.class, "vehicleboat", entityNumber++, MTS.MODID, 256, 5, false);
		EntityRegistry.registerModEntity(new ResourceLocation(MTS.MODID, EntityVehicleG_Plane.class.getSimpleName().substring(6).toLowerCase()), EntityVehicleG_Plane.class, "vehicleplane", entityNumber++, MTS.MODID, 256, 5, false);
		EntityRegistry.registerModEntity(new ResourceLocation(MTS.MODID, EntityVehicleG_Blimp.class.getSimpleName().substring(6).toLowerCase()), EntityVehicleG_Blimp.class, "vehicleblimp", entityNumber++, MTS.MODID, 256, 5, false);
	}
	
	private static void initPackets(){
		//Packets in packets.general
		registerPacket(PacketBulletHit.class, PacketBulletHit.Handler.class, true, true);
		registerPacket(PacketChat.class, PacketChat.Handler.class, true, false);
		registerPacket(PacketPartGunReload.class, PacketPartGunReload.Handler.class, true, false);
		registerPacket(PacketPlayerCrafting.class, PacketPlayerCrafting.Handler.class, false, true);
		
		//Packets in packets.tileentity
		registerPacket(PacketSignChange.class, PacketSignChange.Handler.class, true, true);
		
		//Packets in packets.vehicles.
		registerPacket(PacketVehicleClientInit.class, PacketVehicleClientInit.Handler.class, false, true);
		registerPacket(PacketVehicleClientInitResponse.class, PacketVehicleClientInitResponse.Handler.class, true, false);
		registerPacket(PacketVehicleClientPartAddition.class, PacketVehicleClientPartAddition.Handler.class, true, false);
		registerPacket(PacketVehicleClientPartRemoval.class, PacketVehicleClientPartRemoval.Handler.class, true, false);
		registerPacket(PacketVehicleDeltas.class, PacketVehicleDeltas.Handler.class, true, false);
		registerPacket(PacketVehicleInteract.class, PacketVehicleInteract.Handler.class, false, true);
		registerPacket(PacketVehicleJerrycan.class, PacketVehicleJerrycan.Handler.class, true, false);
		registerPacket(PacketVehicleKey.class, PacketVehicleKey.Handler.class, true, false);
		registerPacket(PacketVehicleNameTag.class, PacketVehicleNameTag.Handler.class, true, false);
		
		//Packets in packets.parts
		registerPacket(PacketPartEngineDamage.class, PacketPartEngineDamage.Handler.class, true, false);
		registerPacket(PacketPartEngineLinked.class, PacketPartEngineLinked.Handler.class, true, false);
		registerPacket(PacketPartEngineSignal.class, PacketPartEngineSignal.Handler.class, true, true);
		registerPacket(PacketPartGroundDeviceWheelFlat.class, PacketPartGroundDeviceWheelFlat.Handler.class, true, false);
		registerPacket(PacketPartGunSignal.class, PacketPartGunSignal.Handler.class, true, true);
		registerPacket(PacketPartSeatRiderChange.class, PacketPartSeatRiderChange.Handler.class, true, false);
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
