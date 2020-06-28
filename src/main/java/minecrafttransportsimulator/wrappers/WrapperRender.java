package minecrafttransportsimulator.wrappers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.rendering.instances.RenderVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.fml.client.FMLClientHandler;
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
	private static String pushedTextureDomain;
	private static String pushedTextureLocation;
	
	/**
	 *  Gets the current render pass.  0 for solid blocks, 1 for transparent,
	 *  and -1 for end-of world final renders.
	 */
	public static int getRenderPass(){
		return MinecraftForgeClient.getRenderPass();
	}
	
	/**
	 *  Returns true if bounding boxes should be rendered.
	 */
	public static boolean shouldRenderBoundingBoxes(){
		return Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox() && getRenderPass() != 1;
	}
	
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
	 *  Like bindTexture, but this method also sets the texture for binding recall later via recallTexture.
	 *  This allows for us to recall specific textures anywhere in the code.  Useful when we don't know what
	 *  we will render between this call and another call, but we do know that we want this texture to be
	 *  re-bound if any other textures were bound.
	 */
	public static void setTexture(String textureDomain, String textureLocation){
		pushedTextureDomain = textureDomain;
		pushedTextureLocation = textureLocation;
		bindTexture(textureDomain, textureLocation);
	}
	
	/**
	 *  Re-binds the last saved texture.
	 */
	public static void recallTexture(){
		if(pushedTextureDomain != null){
			Minecraft.getMinecraft().getTextureManager().bindTexture(textures.get(pushedTextureDomain).get(pushedTextureLocation));
		}
	}
	
	/**
	 *  Helper method to completely disable or enable lighting.
	 *  This disables both the system lighting and internal lighting.
	 */
	public static void setLightingState(boolean enabled){
		setSystemLightingState(enabled);
		setInternalLightingState(enabled);
	}
	
	/**
	 *  Enables or disables OpenGL lighting for this draw sequence.
	 *  This effectively prevents OpenGL lighting calculations on textures.
	 *  Do note that the normal internal lightmapping will still be applied.
	 *  This can be used to prevent OpenGL from doing shadowing on things
	 *  that it gets wrong, such as text. 
	 */
	public static void setSystemLightingState(boolean enabled){
		if(enabled){
			GlStateManager.enableLighting();
		}else{
			GlStateManager.disableLighting();
		}
	}
	
	/**
	 *  Enables or disables internal lighting for this draw sequence.
	 *  This disables the internal lightmapping, effectively making the rendered
	 *  texture as bright as it would be during daytime.  Do note that the system
	 *  lighting calculations for shadowing will still be applied to the model.
	 */
	public static void setInternalLightingState(boolean enabled){
		if(enabled){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}else{
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}
	}
	
	/**
	 *  Updates the internal lightmap to be consistent with the light at the
	 *  passed-in vehicle's location.  This will also enable lighting should
	 *  the current render pass be -1.
	 */
	public static void setLightingToVehicle(EntityVehicleF_Physics vehicle){
		if(getRenderPass() == -1){
	        RenderHelper.enableStandardItemLighting();
	        setLightingState(true);
        }
		int lightVar = vehicle.getBrightnessForRender();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightVar%65536, lightVar/65536);
	}
	
	/**
	 *  Updates the internal lightmap to be consistent with the light at the
	 *  passed-in block's location.  This will also enable lighting should
	 *  the current render pass be -1.
	 */
	public static void setLightingToBlock(Point3i location){
		if(getRenderPass() == -1){
	        RenderHelper.enableStandardItemLighting();
	        setLightingState(true);
	        int lightVar = WrapperGame.getClientWorld().world.getCombinedLight(new BlockPos(location.x, location.y, location.z), 0);
	        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightVar%65536, lightVar/65536);
        }
	}
	
	/**
	 *  Sets the blend state to enabled or disabled.  Also allows for
	 *  the blend state to be set to accommodate beam lights with brightening
	 *  properties rather than regular alpha blending.
	 */
	public static void setBlendState(boolean enabled, boolean brightBlend){
		if(enabled){
			GlStateManager.enableBlend();
			GlStateManager.disableAlpha();
			GlStateManager.depthMask(false);
			if(brightBlend){
				GlStateManager.blendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
			}else{
				GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			}
		}else{
			GlStateManager.disableBlend();
			GlStateManager.enableAlpha();
			GlStateManager.depthMask(true);
			GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		}
	}
	
	/**
	 *  Sets MC color to the passed-in color.  Required when needing to keep MC states happy.
	 *  In particular, this is needed if colors are changed during MC internal draw calls,
	 *  such as rendering a string, changing the color, and then rendering another string.
	 */
	public static void setColorState(float red, float green, float blue, float alpha){
		GlStateManager.color(red, green, blue, alpha);
	}
	
	/**
	 *  Resets all the rendering states to the appropriate values for the pass we are in.
	 *  Useful after doing a rendering routine where states may not be correct for the pass.
	 */
	public static void resetStates(){
		//For pass 0, we do lighting but not blending.
		//For pass 1, we do blending and lighting.
		//For pass -1, we don't do blending or lighting.
		setColorState(1.0F, 1.0F, 1.0F, 1.0F);
		setBlendState(getRenderPass() == 1, false);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		if(getRenderPass() != -1){
			setLightingState(true);
		}else{
			setLightingState(false);
			RenderHelper.disableStandardItemLighting();
		}
	}
	
	/**
	 *  This method manually renders all riders on a vehicle.  Useful if you're rendering the vehicle manually
	 *  and the vehicle and its riders have been culled from rendering.
	 */
	public static void renderVehicleRiders(EntityVehicleF_Physics vehicle, float partialTicks){
		for(Entity passenger : vehicle.getPassengers()){
			if(!(WrapperGame.getClientPlayer().equals(passenger) && WrapperGame.inFirstPerson()) && passenger.posY > passenger.world.getHeight()){
				PartSeat seat = vehicle.getSeatForRider(passenger);
				if(seat != null){
					GL11.glPushMatrix();
					Point3d offset = vehicle.positionVector.copy().add(seat.worldPos);
					GL11.glTranslated(offset.x, offset.y - seat.getHeight()/2F + passenger.getYOffset(), offset.z);
					Minecraft.getMinecraft().getRenderManager().renderEntityStatic(passenger, partialTicks, false);
					GL11.glPopMatrix();
				}
			}
		}
	}
	
	/**
	 *  Event that's called to register models.  We register our render wrapper
	 *  classes here, as well as all item JSONs.
	 */
	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event){
		//Create the custom JSON parser class.
		//We need to register a custom resource handler here to auto-generate JSON.
		//FAR easier than trying to use the bloody bakery system.
		for(Field field : Minecraft.class.getDeclaredFields()){
			if(field.getName().equals("defaultResourcePacks") || field.getName().equals("field_110449_ao")){
				try{
					if(!field.isAccessible()){
						field.setAccessible(true);
					}
					
					@SuppressWarnings("unchecked")
					List<IResourcePack> defaultPacks = (List<IResourcePack>) field.get(Minecraft.getMinecraft());
					defaultPacks.add(new PackResourcePack());
					FMLClientHandler.instance().refreshResources(VanillaResourceType.MODELS);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		//Register the vehicle rendering class.
		RenderingRegistry.registerEntityRenderingHandler(EntityVehicleF_Physics.class, new IRenderFactory<EntityVehicleF_Physics>(){
			@Override
			public Render<? super EntityVehicleF_Physics> createRenderFor(RenderManager manager){
				return new RenderVehicle(manager);
			}});
				
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
				ModelLoader.setCustomModelResourceLocation(packItem, 0, new ModelResourceLocation(MTS.MODID + "_packs:" + packItem.definition.packID + "." + packItem.definition.classification.assetFolder + "/" + packItem.definition.systemName, "inventory"));
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
	 *  Custom ResourcePack class for auto-generating item JSONs.
	 */
	private static class PackResourcePack implements IResourcePack{
		private final Set<String> domains;
		
		private PackResourcePack(){
			 domains = new HashSet<String>();
			 domains.add(MTS.MODID + "_packs");
		}

		@Override
		public InputStream getInputStream(ResourceLocation location) throws IOException{
			String jsonPath = location.getResourcePath();
			//Strip header and suffix.
			jsonPath = jsonPath.substring("models/item/".length(), jsonPath.length() - ".json".length());
			//Get the packID.
			String packID = jsonPath.substring(0, jsonPath.indexOf('.'));
			//Get the asset name by stripping off the packID.
			String asset = jsonPath.substring(packID.length() + 1);
			//Attempt to get a JSON file normally from the path.  If this fails, generate a default JSON.
			InputStream stream = getClass().getResourceAsStream("/assets/" + packID + "/models/item/" + asset + ".json");
			if(stream != null){
				return stream;
			}else{
				String fakeJSON = "{\"parent\":\"mts:item/basic\",\"textures\":{\"layer0\": \"" + packID + ":items/" + asset + "\"}}";
				return new ByteArrayInputStream(fakeJSON.getBytes(StandardCharsets.UTF_8));
			}
		}

		@Override
		public boolean resourceExists(ResourceLocation location){
			return domains.contains(location.getResourceDomain()) && location.getResourcePath().startsWith("models/item/") && location.getResourcePath().endsWith(".json");
		}

		@Override
		public Set<String> getResourceDomains(){
			return domains;
		}

		@Override
		public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) throws IOException{
			return null;
		}

		@Override
		public BufferedImage getPackImage() throws IOException{
			return null;
		}

		@Override
		public String getPackName(){
			return MTS.MODID + "_packs";
		}
	}
	
	/*
	private static final ICustomModelLoader packModelLoader = new ICustomModelLoader(){

		@Override
		public void onResourceManagerReload(IResourceManager resourceManager){
			//Do nothing.  Packs don't change.
		}

		@Override
		public boolean accepts(ResourceLocation modelLocation){
			System.out.println(modelLocation.toString());
			return modelLocation.getResourceDomain().equals(MTS.MODID) && modelLocation.getResourcePath().startsWith("pack_");
		}

		@Override
		public IModel loadModel(ResourceLocation modelLocation) throws Exception{
			final List<ResourceLocation> textures = new ArrayList<ResourceLocation>();
			textures.add(modelLocation);
			
			return new IModel(){
				
				@Override
				public Collection<ResourceLocation> getTextures(){
			        return textures;
			    }
			    
				@Override
				public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter){
					return new IBakedModel(){
						private final Map<EnumFacing, List<BakedQuad>> quadCache = new HashMap<EnumFacing, List<BakedQuad>>();
						
						@Override
						public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand){
							if(quadCache.containsKey(side)){
								int[] newData = Arrays.copyOf(quad.getVertexData(), quad.getVertexData().length);

				                VertexFormat format = quad.getFormat();

				                for (int i = 0; i < 4; ++i) {
				                    int j = format.getIntegerSize() * i;
				                    newData[j + 0] = Float.floatToRawIntBits(Float.intBitsToFloat(newData[j + 0]) * (float) scale.x + (float) transform.x);
				                    newData[j + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(newData[j + 1]) * (float) scale.y + (float) transform.y);
				                    newData[j + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(newData[j + 2]) * (float) scale.z + (float) transform.z);
				                }

				                quadCache.get(side).add(new BakedQuad(newData, quad.getTintIndex(), quad.getFace(), quad.getSprite(), quad.shouldApplyDiffuseLighting(), quad.getFormat()));
							}
							return quads;
						}

						@Override
						public boolean isAmbientOcclusion(){
							//Not a block, don't care.
							return false;
						}

						@Override
						public boolean isGui3d(){
							//3D models just look better.
							return true;
						}

						@Override
						public boolean isBuiltInRenderer(){
							//This smells like code that will go away sometime...
							return false;
						}

						@Override
						public TextureAtlasSprite getParticleTexture(){
							return bakedTextureGetter.apply(textures.get(0));
						}

						@Override
						public ItemOverrideList getOverrides(){
							return ItemOverrideList.NONE;
						}
					};
				}
			};
		}
		
	};*/
}
