package minecrafttransportsimulator.dataclasses;

import java.lang.reflect.Field;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.BlockDecor;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.rendering.vehicles.RenderVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Sub-class for client-only registration.  Used for models, item texture, entity rendering and the like.
 * Registrations are fired on events, so should be good for the next few MC versions.
 * 
 * @author don_bruce
 */
@Mod.EventBusSubscriber(Side.CLIENT)
public final class MTSRegistryClient{
	
	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event){
		//Register the vehicle rendering class.
		RenderingRegistry.registerEntityRenderingHandler(EntityVehicleE_Powered.class, MTSRenderFactory);
		
		//Register the item models.
		//First register the core items.
		for(Field field : MTSRegistry.class.getFields()){
			if(field.getType().equals(Item.class)){
				try{
					registerCoreItemRender((Item) field.get(null));
				}catch(Exception e){
					e.printStackTrace();
				}
			}else if(field.getType().equals(Block.class)){
				try{
					Block block = (Block) field.get(null);
					if(!(block instanceof BlockDecor)){
						registerCoreItemRender(Item.getItemFromBlock(block));
					}
				}catch(Exception e){
					e.printStackTrace();
				}	
			}
		}
		
		//Now register items for the packs.
		for(String packID : MTSRegistry.packItemMap.keySet()){
			for(AItemPack<? extends AJSONItem<?>> packItem : MTSRegistry.packItemMap.get(packID).values()){
				ModelLoader.setCustomModelResourceLocation(packItem, 0, new ModelResourceLocation(packItem.definition.packID + ":" + packItem.definition.classification.assetFolder + "/" + packItem.definition.systemName, "inventory"));
			}	
		}
	}
	
	private static void registerCoreItemRender(Item item){
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(MTS.MODID + ":" + item.getRegistryName().getResourcePath(), "inventory"));
	}
	
	private static final IRenderFactory<EntityVehicleE_Powered> MTSRenderFactory = new IRenderFactory<EntityVehicleE_Powered>(){
		@Override
		public Render<? super EntityVehicleE_Powered> createRenderFor(RenderManager manager){
			return new RenderVehicle(manager);
		}
	};
}
