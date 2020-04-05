package minecrafttransportsimulator.wrappers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.rendering.vehicles.RenderVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Wrapper for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, and registering rendering systems
 * for TESRs, items, and entities.
 *
 * @author don_bruce
 */
@Mod.EventBusSubscriber(Side.CLIENT)
public class WrapperRender{
	private static final Map<String, Map<String, ResourceLocation>> textures = new HashMap<String, Map<String, ResourceLocation>>(); 
	
	/**
	 *  Binds the passed-in texture to be rendered.  The instance of the texture is 
	 *  cached in this class once created for later use, so feel free to not cache
	 *  the string values that are passed-in.
	 */
	public static void bindTexture(String textureDomain, String textureLocation){
		//Bind texture if we have it.
		ResourceLocation texture;
		if(textures.containsKey(textureDomain)){
			texture = textures.get(textureDomain).get(textureLocation);
			if(texture == null){
				//Make new texture for the domain.
				texture = new ResourceLocation(textureDomain, textureLocation);
				textures.get(textureDomain).put(textureLocation, texture);
			}
		}else{
			//Make new domain and new texture for the domain.
			texture = new ResourceLocation(textureDomain, textureLocation);
			Map<String, ResourceLocation> textureMap = new HashMap<String, ResourceLocation>();
			textureMap.put(textureLocation, texture);
			textures.put(textureDomain, textureMap);
		}
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
	}
	
	/**
	 *  Sets the lightmap to either enabled or disabled based on the passed-in parameter.
	 */
	public static void setLightmapState(boolean enabled){
		if(enabled){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}else{
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}
	}
	
	/**
	 *  Event that's called to register models.  We register our render wrapper
	 *  classes here, as well as all item JSONs.
	 */
	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event){
		//Register the vehicle rendering class.
		RenderingRegistry.registerEntityRenderingHandler(EntityVehicleE_Powered.class, MTSRenderFactory);
				
		//Register the TESR wrapper.
		ClientRegistry.bindTileEntitySpecialRenderer(WrapperTileEntity.class, new WrapperTileEntityRender());
		
		//Register the item models.
		//First register the core items.
		for(Field field : MTSRegistry.class.getFields()){
			//Regular item.
			if(field.getType().equals(Item.class)){
				try{
					registerCoreItemRender((Item) field.get(null));
				}catch(Exception e){
					e.printStackTrace();
				}
			}else if(field.getType().equals(WrapperBlock.class)){
				//Wrapper block item, get item from it to register.
				try{
					WrapperBlock wrapper = (WrapperBlock) field.get(null);
					registerCoreItemRender(Item.getItemFromBlock(wrapper));
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
	
	/**
	 *  Helper method to register renders.
	 */
	private static void registerCoreItemRender(Item item){
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(MTS.MODID + ":" + item.getRegistryName().getResourcePath(), "inventory"));
	}
	
	/**
	 *  Stupid Forge with their stupid factories...
	 */
	private static final IRenderFactory<EntityVehicleE_Powered> MTSRenderFactory = new IRenderFactory<EntityVehicleE_Powered>(){
		@Override
		public Render<? super EntityVehicleE_Powered> createRenderFor(RenderManager manager){
			return new RenderVehicle(manager);
		}
	};
}
