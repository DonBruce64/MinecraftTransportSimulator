package minecrafttransportsimulator.dataclasses;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import mcinterface.BuilderBlock;
import mcinterface.BuilderEntity;
import mcinterface.WrapperEntityPlayer;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.instances.BlockPartsBench;
import minecrafttransportsimulator.guis.instances.GUIPartBench;
import minecrafttransportsimulator.items.core.ItemJerrycan;
import minecrafttransportsimulator.items.core.ItemJumperCable;
import minecrafttransportsimulator.items.core.ItemKey;
import minecrafttransportsimulator.items.core.ItemTicket;
import minecrafttransportsimulator.items.core.ItemWrench;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONBooklet;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.packets.general.PacketBulletHit;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.packets.parts.PacketPartEngineDamage;
import minecrafttransportsimulator.packets.parts.PacketPartEngineLinked;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal;
import minecrafttransportsimulator.packets.parts.PacketPartGroundDeviceWheelFlat;
import minecrafttransportsimulator.packets.parts.PacketPartGunReload;
import minecrafttransportsimulator.packets.parts.PacketPartGunSignal;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientInit;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleClientInitResponse;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleDeltas;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleInteract;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleJerrycan;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleKey;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleNameTag;
import minecrafttransportsimulator.systems.PackParserSystem;
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
	public static TreeMap<String, LinkedHashMap<String, AItemPack<? extends AJSONItem<? extends AJSONItem<?>.General>>>> packItemMap = new TreeMap<String, LinkedHashMap<String, AItemPack<? extends AJSONItem<? extends AJSONItem<?>.General>>>>();
	
	/**Maps pack items to their list of crafting ingredients.  This is used rather than the core JSON to allow for
	 * overriding the crafting materials in said JSON, and to concatenate the materials in {@link JSONVehicle}*/
	public static final Map<AItemPack<? extends AJSONItem<?>>, String[]> packCraftingMap = new HashMap<AItemPack<? extends AJSONItem<?>>, String[]>();
	
	/**Core creative tab for base MTS items**/
	public static final CreativeTabCore coreTab = new CreativeTabCore();
	
	/**Map of creative tabs for packs.  Keyed by packID.  Populated by the {@link PackParserSystem}**/
	public static final Map<String, CreativeTabPack> packTabs = new HashMap<String, CreativeTabPack>();

	//Vehicle interaction items.
	public static final Item wrench = new ItemWrench();
	public static final Item key = new ItemKey();
	public static final Item jumperCable = new ItemJumperCable();
	public static final Item jerrycan = new ItemJerrycan();
	public static final Item ticket = new ItemTicket();
	
	//Crafting benches.
	public static final BuilderBlock vehicleBench = new BuilderBlock(new BlockPartsBench(JSONVehicle.class));
	public static final BuilderBlock propellerBench = new BuilderBlock(new BlockPartsBench(JSONPart.class, "propeller"));
	public static final BuilderBlock engineBench = new BuilderBlock(new BlockPartsBench(JSONPart.class, "engine_"));
	public static final BuilderBlock wheelBench = new BuilderBlock(new BlockPartsBench(JSONPart.class, "ground_"));
	public static final BuilderBlock seatBench = new BuilderBlock(new BlockPartsBench(JSONPart.class, "seat", "crate", "barrel", "crafting_table", "furnace", "brewing_stand"));
	public static final BuilderBlock gunBench = new BuilderBlock(new BlockPartsBench(JSONPart.class, "gun_", "bullet"));
	public static final BuilderBlock customBench = new BuilderBlock(new BlockPartsBench(JSONPart.class, "custom"));
	public static final BuilderBlock instrumentBench = new BuilderBlock(new BlockPartsBench(JSONInstrument.class));
	public static final BuilderBlock componentBench = new BuilderBlock(new BlockPartsBench(JSONItem.class).addValidClass(JSONBooklet.class));
	public static final BuilderBlock decorBench = new BuilderBlock(new BlockPartsBench(JSONDecor.class).addValidClass(JSONPoleComponent.class));
	
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
	 * amd {@link WrapperEntityPlayer#hasMaterials(AItemPack)} as well as any other systems that 
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
		//Register all core items.
		for(Field field : MTSRegistry.class.getFields()){
			if(field.getType().equals(Item.class)){
				try{
					Item item = (Item) field.get(null);
					item.setCreativeTab(coreTab);
					String name = field.getName().toLowerCase();
					event.getRegistry().register(item.setRegistryName(name).setUnlocalizedName(name));
					coreItems.add(item);
				}catch(Exception e){
					e.printStackTrace();
				}
			}else if(field.getType().equals(BuilderBlock.class)){
				//Also need to make itemblocks for all blocks.
				//This doesn't include packs, which have their own items.
				try{
					BuilderBlock block = (BuilderBlock) field.get(null);
					block.setCreativeTab(coreTab);
					ItemBlock itemBlock = new ItemBlock(block);
					itemBlock.setCreativeTab(coreTab);
					event.getRegistry().register(itemBlock.setRegistryName(block.getRegistryName()).setUnlocalizedName(block.getRegistryName().toString()));
					MTSRegistry.coreItems.add(itemBlock);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		//Register all core MTS "pack" items.
		for(AItemPack<?> item : MTSRegistry.packItemMap.get(MTS.MODID).values()){
			item.setCreativeTab(coreTab);
			String name = item.definition.systemName;
			event.getRegistry().register(item.setRegistryName(name).setUnlocalizedName(name));
			coreItems.add(item);
		}
	}

	/**
	 * Registers all entities with the entity registry.
	 * For vehicles we only register the main classes as
	 * the pack data stored in NBT is what makes for different vehicles.
	 */
	private static void initEntities(){
		int entityNumber = 0;
		EntityRegistry.registerModEntity(new ResourceLocation(MTS.MODID, "mts_vehicle"), BuilderEntity.class, "mts_vehicle", entityNumber++, MTS.MODID, 32*16, 5, false);
	}
	
	private static void initPackets(){
		//Packets in packets.general
		registerPacket(PacketBulletHit.class, PacketBulletHit.Handler.class, true, true);
		registerPacket(PacketChat.class, PacketChat.Handler.class, true, false);
		registerPacket(PacketPartGunReload.class, PacketPartGunReload.Handler.class, true, false);
		
		//Packets in packets.vehicles.
		registerPacket(PacketVehicleClientInit.class, PacketVehicleClientInit.Handler.class, false, true);
		registerPacket(PacketVehicleClientInitResponse.class, PacketVehicleClientInitResponse.Handler.class, true, false);
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
