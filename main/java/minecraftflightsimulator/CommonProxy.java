package minecraftflightsimulator;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.sounds.BenchSound;
import minecraftflightsimulator.sounds.EngineSound;
import minecraftflightsimulator.systems.ConfigSystem;
import minecraftflightsimulator.systems.ForgeContainerGUISystem;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

/**Contains registration methods used by {@link MFSRegistry} and methods overridden by ClientProxy. 
 * See the latter for more info on overridden methods.
 * 
 * @author don_bruce
 */
public class CommonProxy{
	private static int entityNumber = 0;
	private static int packetNumber = 0;

	public void preInit(FMLPreInitializationEvent event){
		ConfigSystem.initCommon(event.getSuggestedConfigurationFile());
	}
	
	public void init(FMLInitializationEvent event){
		MFSRegistry.instance.init();
		NetworkRegistry.INSTANCE.registerGuiHandler(MFS.instance, new ForgeContainerGUISystem());
	}
	
	/**
	 * Registers the given item and adds it to the creative tab list.
	 * @param item
	 */
	public void registerItem(Item item){
		item.setCreativeTab(MFS.tabMFS);
		item.setTextureName("mfs:" + item.getUnlocalizedName().substring(5).toLowerCase());
		GameRegistry.registerItem(item, item.getUnlocalizedName().substring(5));
		MFSRegistry.itemList.add(item);
	}
	
	/**
	 * Registers the given block and adds it to the creative tab list.
	 * Also adds the respective TileEntity if the block has one.
	 * @param block
	 */
	public void registerBlock(Block block){
		block.setCreativeTab(MFS.tabMFS);
		block.setBlockTextureName("mfs:" + block.getUnlocalizedName().substring(5).toLowerCase());
		GameRegistry.registerBlock(block, block.getUnlocalizedName().substring(5));
		MFSRegistry.itemList.add(Item.getItemFromBlock(block));
		if(block instanceof ITileEntityProvider){
			Class<? extends TileEntity> tileEntityClass = ((ITileEntityProvider) block).createNewTileEntity(null, 0).getClass();
			GameRegistry.registerTileEntity(tileEntityClass, tileEntityClass.getSimpleName());
		}
	}

	/**
	 * Registers an entity.
	 * Optionally pairs the entity with an item for GUI operations.
	 * @param entityClass
	 * @param entityItem
	 */
	public void registerEntity(Class entityClass){
		EntityRegistry.registerModEntity(entityClass, entityClass.getSimpleName().substring(6), entityNumber++, MFS.MODID, 80, 5, false);
	}
	
	/**
	 * Registers an entity.
	 * Optionally pairs the entity with an item for GUI operations.
	 * @param entityClass
	 * @param entityItem
	 */
	public void registerChildEntity(Class<? extends EntityChild> entityClass, Item entityItem){
		if(entityItem != null){
			MFSRegistry.entityItems.put(entityItem, entityClass);
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
	public <REQ extends IMessage, REPLY extends IMessage> void registerPacket(Class<REQ> packetClass, Class<? extends IMessageHandler<REQ, REPLY>> handlerClass, boolean client, boolean server){
		if(client)MFS.MFSNet.registerMessage(handlerClass, packetClass, ++packetNumber, Side.CLIENT);
		if(server)MFS.MFSNet.registerMessage(handlerClass, packetClass, ++packetNumber, Side.SERVER);
	}
	
	public void registerRecpie(ItemStack output, Object...params){
		GameRegistry.addRecipe(output, params);
	}
	
	public void openGUI(Object clicked, EntityPlayer clicker){}
	public void playSound(Entity noisyEntity, String soundName, float volume, float pitch){}
	public EngineSound updateEngineSoundAndSmoke(EngineSound sound, EntityEngine engine){return null;}
	public BenchSound updateBenchSound(BenchSound sound, TileEntityPropellerBench bench){return null;}
}
