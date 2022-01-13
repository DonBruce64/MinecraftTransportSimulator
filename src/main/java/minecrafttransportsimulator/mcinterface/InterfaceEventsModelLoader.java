package minecrafttransportsimulator.mcinterface;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.vecmath.Matrix4f;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;
import minecrafttransportsimulator.rendering.components.RenderableObject;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Interface for handling events pertaining to loading models into MC.  These events are mainly for item models,
 * though events for Entity and Tile Entity model rendering classes are also included here as they are registered
 * like item models.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceEventsModelLoader{
    
	/**
	 *  Returns a {@link RenderableObject} of the passed-in item model for item rendering.
	 *  Note that this does not include the count of the items in the stack: this must be
	 *  rendered on its own.  Also note the item is in block-coords.  This means that normally
	 *  the model will be from 0->1 in the axial directions.
	 */
	public static RenderableObject getItemModel(ItemStack stack){
		//Get normal model.
		IBakedModel itemModel = Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(stack, null, Minecraft.getMinecraft().player);
		
		//Get transformation matrix, if this model has one.
		Matrix4f matrix = itemModel.handlePerspective(ItemCameraTransforms.TransformType.GUI).getRight();
		
		//Get all quads for the model. We assume that 
		List<BakedQuad> quads = new ArrayList<BakedQuad>();
		for(EnumFacing enumfacing : EnumFacing.values()){
			quads.addAll(itemModel.getQuads((IBlockState)null, enumfacing, 0L));
        }
		quads.addAll(itemModel.getQuads((IBlockState)null, null, 0L));
		
		//Convert quads to floatbuffer for our rendering.
		//Each 4-vertex quad becomes two tris, with standard rendering for normals and UVs.
		//Note that the offsets here are the byte index, so we need to convert them when addressing the int array.
		FloatBuffer vertexData = FloatBuffer.allocate(quads.size()*6*8);
		for(BakedQuad quad : quads){
			//Get a byte buffer of data to handle for conversion.
			int[] quadArray = quad.getVertexData();
			ByteBuffer quadData = ByteBuffer.allocate(quadArray.length*Integer.BYTES);
			quadData.asIntBuffer().put(quadArray);
			
			VertexFormat format = quad.getFormat();
			int quadDataIndexOffset = 0;
			for(int i=0; i<6; ++i){
				int offsetThisCycle = 0;
				if(i==3){
					//4th vertex is the same as 3rd vertex.
					quadDataIndexOffset -= format.getSize();
					offsetThisCycle = format.getSize();
				}else if(i==5){
					//6th vertex is the same as 1st vertex.
					quadDataIndexOffset -= 4*format.getSize();
				}else{
					//Actual vertex, add to buffer at current position.
					offsetThisCycle = format.getSize();
				}
				
				//Default normal to face direction.
				Vec3i vec3i = quad.getFace().getDirectionVec();
				vertexData.put(vec3i.getX());
				vertexData.put(vec3i.getY());
				vertexData.put(vec3i.getZ());
				
				//Use UV data.
				int uvOffset = format.getUvOffsetById(0);
				vertexData.put(quadData.getFloat(quadDataIndexOffset + uvOffset));
				vertexData.put(quadData.getFloat(quadDataIndexOffset + uvOffset + Float.BYTES));
				
				//For some reason, position isn't saved as an index.  Rather, it's in the general list.
				//Loop through the elements to find it.
				for(VertexFormatElement element : format.getElements()){
					if(element.isPositionElement()){
						int vertexOffset = format.getOffset(format.getElements().indexOf(element));
						float x = quadData.getFloat(quadDataIndexOffset + vertexOffset);
						float y = quadData.getFloat(quadDataIndexOffset + vertexOffset + Float.BYTES);
						float z = quadData.getFloat(quadDataIndexOffset + vertexOffset + 2*Float.BYTES);
						
						if(matrix != null){
							float xNew = matrix.m00*x + matrix.m01*y + matrix.m02*z + matrix.m03 + 1;
							float yNew = matrix.m10*x + matrix.m11*y + matrix.m12*z + matrix.m13 + 0.25F;
							float zNew = matrix.m30*x + matrix.m31*y + matrix.m32*z + matrix.m33;
							//Don't multiply by w, we don't care about that value, and it really won't matter anyways.
							vertexData.put(xNew);
							vertexData.put(yNew);
							vertexData.put(zNew);
						}else{
							vertexData.put(x);
							vertexData.put(y);
							vertexData.put(z);
						}
						break;
					}
				}
				quadDataIndexOffset += offsetThisCycle;
			}
		}
		vertexData.flip();
		return new RenderableObject("item_generated", RenderableObject.GLOBAL_TEXTURE_NAME, ColorRGB.WHITE, vertexData, false);
	}
	
	/**
	 *  Returns a 4-float array for the block break texture at the passed-in position in the passed-in world.
	 */
	public static float[] getBlockBreakTexture(WrapperWorld world, Point3d position){
		//Get normal model.
		IBlockState state = world.world.getBlockState(new BlockPos(position.x, position.y, position.z));
		TextureAtlasSprite sprite = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getTexture(state);
		return new float[]{sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV()};
	}
	
	/**
	 *  Event that's called to register models.  We register our render wrapper
	 *  classes here, as well as all item JSONs.
	 */
	@SuppressWarnings({ "unchecked" })
	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event){
		//Register the global entity rendering class.
		RenderingRegistry.registerEntityRenderingHandler(BuilderEntityRenderForwarder.class, new IRenderFactory<BuilderEntityRenderForwarder>(){
			@Override
			public Render<? super BuilderEntityRenderForwarder> createRenderFor(RenderManager manager){
			return new Render<BuilderEntityRenderForwarder>(manager){
				@Override
				protected ResourceLocation getEntityTexture(BuilderEntityRenderForwarder builder){
					return null;
				}
				
				@Override
				public boolean shouldRender(BuilderEntityRenderForwarder builder, ICamera camera, double camX, double camY, double camZ){
					//Always render the forwarder, no matter where the camera is.
					return true;
				}
				
				@Override
				public void doRender(BuilderEntityRenderForwarder builder, double x, double y, double z, float entityYaw, float partialTicks){
					//Get all entities in the world, and render them manually for this one builder.
					//Only do this if the player the builder is following is the client player.
					WrapperWorld world = WrapperWorld.getWrapperFor(builder.world);
					if(Minecraft.getMinecraft().player.equals(builder.playerFollowing) && builder.shouldRenderEntity(partialTicks)){
						ConcurrentLinkedQueue<AEntityC_Renderable> allEntities = world.renderableEntities;
						if(allEntities != null){
					        //Use smooth shading for model rendering.
							GL11.glShadeModel(GL11.GL_SMOOTH);
							//Enable normal re-scaling for model rendering.
							//This prevents bad lighting.
							GlStateManager.enableRescaleNormal();
							
							//Start master profiling section.
							for(AEntityC_Renderable entity : allEntities){
								world.beginProfiling("MTSRendering", true);
								entity.getRenderer().render(entity, MinecraftForgeClient.getRenderPass() == 1, partialTicks);
								world.endProfiling();
							}
							
							//Set shade model back to flat for other rendering.
							GL11.glShadeModel(GL11.GL_FLAT);
						}
					}
				}
			};
		}});
		
		//Get the list of default resource packs here to inject a custom parser for auto-generating JSONS.
		//FAR easier than trying to use the bloody bakery system.
		//Normally we'd add our pack to the current loader, but this gets wiped out during reloads and unless we add our pack to the main list, it won't stick.
		//To do this, we use reflection to get the field from the main MC class that holds the master list to add our custom ones.
		//((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).reloadResourcePack(new PackResourcePack(MasterLoader.MODID + "_packs"));
		List<IResourcePack> defaultPacks = null;
		for(Field field : Minecraft.class.getDeclaredFields()){
			if(field.getName().equals("defaultResourcePacks") || field.getName().equals("field_110449_ao")){
				try{
					if(!field.isAccessible()){
						field.setAccessible(true);
					}
					
					defaultPacks = (List<IResourcePack>) field.get(Minecraft.getMinecraft());
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		//Check to make sure we have the pack list before continuing.
		if(defaultPacks == null){
			InterfaceCore.logError("Could not get default pack list. Item icons will be disabled.");
			return;
		}
		
		//Now that we have the custom resource pack location, add our built-in loader.
		//This one auto-generates item JSONs.
		defaultPacks.add(new PackResourcePack(MasterLoader.MODID + "_packs"));
		
		//Now register items for the packs.
		//When we register a pack item from an external pack, we'll need to make a resource loader for it.
		//This is done to allow MC/Forge to play nice with item textures.
		for(AItemBase item : BuilderItem.itemMap.keySet()){
			if(item instanceof AItemPack){
				AItemPack<?> packItem = (AItemPack<?>) item;
				if(!PackResourcePack.createdLoaders.containsKey(packItem.definition.packID)){
					defaultPacks.add(new PackResourcePack(packItem.definition.packID));
				}
				ModelLoader.setCustomModelResourceLocation(BuilderItem.itemMap.get(packItem), 0, new ModelResourceLocation(MasterLoader.MODID + "_packs:" + packItem.getRegistrationName(), "inventory"));
			}
		}
		
		//Now that we've created all the pack loaders, reload the resource manager to add them to the systems.
		FMLClientHandler.instance().refreshResources(VanillaResourceType.MODELS);
	}
	
	/**
	 *  Custom ResourcePack class for auto-generating item JSONs.
	 */
	private static class PackResourcePack implements IResourcePack{
	    private static final Map<String, PackResourcePack> createdLoaders = new HashMap<String, PackResourcePack>();
		private final String domain;
	    private final Set<String> domains;
		
		private PackResourcePack(String domain){
			this.domain = domain;
			domains = new HashSet<String>();
			domains.add(domain);
			createdLoaders.put(domain, this);
		}

		@Override
		public InputStream getInputStream(ResourceLocation location) throws IOException{
			//Create stream return variable and get raw data.
			InputStream stream;
			String rawPackInfo = location.getPath();
			
			//If we are for an item JSON, try to find that JSON, or generate one automatically.
			//If we are for an item PNG, just load the PNG as-is.  If we don't find it, then just let MC purple checker it.
			//Note that the internal mts_packs loader does not do PNG loading, as it re-directs the PNG files to the pack's loaders.
			if(rawPackInfo.endsWith(".json")){
				//Strip the suffix from the packInfo, and then test to see if it's an internal
				//JSON reference from an item JSON, or if it's the primary JSON for the item being loaded..
				String strippedSuffix = rawPackInfo.substring(0, rawPackInfo.lastIndexOf("."));
				if(!strippedSuffix.contains(".")){
					//JSON reference.  Get the specified file.
					stream = getClass().getResourceAsStream("/assets/" + domain + "/" + rawPackInfo);
					if(stream == null){
						InterfaceCore.logError("Could not find JSON-specified file: " + rawPackInfo);
						throw new FileNotFoundException(rawPackInfo);
					}
				}else{
					String resourcePath = "";
					String itemTexturePath = "";
						
					//Strip off the auto-generated prefix.
					String combinedPackInfo = rawPackInfo;
					combinedPackInfo = strippedSuffix.substring("models/item/".length());
					
					//Get the pack information, and try to load the resource.
					try{
						String packID = combinedPackInfo.substring(0, combinedPackInfo.indexOf("."));
						String systemName = combinedPackInfo.substring(combinedPackInfo.indexOf(".") + 1);
						AItemPack<?> packItem = PackParserSystem.getItem(packID, systemName);
						resourcePath = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_JSON, systemName);
						
						//Try to load the item JSON, or create it if it doesn't exist.
						stream = getClass().getResourceAsStream(resourcePath);
						if(stream == null){
							//Get the actual texture path.
							itemTexturePath = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_PNG, systemName);
							
							//Remove the "/assets/packID/" portion as it's implied with JSON.
							itemTexturePath = itemTexturePath.substring(("/assets/"  + packID + "/").length());
							
							//Remove the .png suffix as it's also implied.
							itemTexturePath = itemTexturePath.substring(0, itemTexturePath.length() - ".png".length());
							
							//Need to add packID domain to this to comply with JSON domains.
							//If we don't, the PNG won't get sent to the right loader.
							itemTexturePath = packID + ":" + itemTexturePath;
							
							//Generate fake JSON and return as stream to MC loader.
							String fakeJSON = "{\"parent\":\"mts:item/basic\",\"textures\":{\"layer0\": \"" + itemTexturePath + "\"}}";
							stream = new ByteArrayInputStream(fakeJSON.getBytes(StandardCharsets.UTF_8));
						}
					}catch(Exception e){
						InterfaceCore.logError("Could not parse out item JSON from: " + rawPackInfo + "  Looked for JSON at:" + resourcePath + (itemTexturePath.isEmpty() ? (", with fallback at:" + itemTexturePath) : ", but could not find it."));
						throw new FileNotFoundException(rawPackInfo);
					}
				}
			}else{
				try{
					//First check if this is for an item or a model.
					boolean isItemPNG = rawPackInfo.contains("/items/") || rawPackInfo.contains("_item");
					
					//Strip off the auto-generated prefix and suffix data.
					String combinedPackInfo = rawPackInfo;
					combinedPackInfo = combinedPackInfo.substring("textures/".length(), combinedPackInfo.length() - ".png".length());
					
					//Get the pack information.
					//If we are ending in _item, it means we are getting a JSON for a modular-pack's item PNG.
					//Need to remove this suffix to get the correct systemName to look-up in the systems.
					String packID = domain;
					String systemName = combinedPackInfo.substring(combinedPackInfo.lastIndexOf('/') + 1);
					if(systemName.endsWith("_item")){
						systemName = systemName.substring(0, systemName.length() - "_item".length());
					}
					AItemPack<?> packItem = PackParserSystem.getItem(packID, systemName);
					
					if(packItem != null){
						//Get the actual resource path for this resource and return its stream.
						String streamLocation = PackResourceLoader.getPackResource(packItem.definition, isItemPNG ? ResourceType.ITEM_PNG : ResourceType.PNG, systemName);
						stream = getClass().getResourceAsStream(streamLocation);
						
						if(stream == null){
							if(isItemPNG){
								//We might not have this file, but we also might have a JSON-defined item here.
								//Try the JSON standards before throwing an error.
								String streamJSONLocation = "/assets/" + packID + "/" + rawPackInfo;
								stream = getClass().getResourceAsStream(streamJSONLocation);
								if(stream == null){
									if(streamLocation != null){
										InterfaceCore.logError("Could not find item PNG at specified location: " + streamLocation + "  Or potential JSON location: " + streamJSONLocation);
									}else{
										InterfaceCore.logError("Could not find JSON PNG: " + streamJSONLocation);
									}
									throw new FileNotFoundException(rawPackInfo);
								}
							}else{
								InterfaceCore.logError("Could not find OBJ PNG: " + streamLocation);
								throw new FileNotFoundException(rawPackInfo);
							}
						}
					}else{
						//No pack item for this texture.  Must be an internal texture for other things.
						//In this case, we just get the stream exact location.
						String streamLocation = "/assets/" + domain + "/" + rawPackInfo;
						stream = getClass().getResourceAsStream(streamLocation);
						if(stream == null){
							InterfaceCore.logError("Couldn't find...whatever this is: " + streamLocation);
							throw new FileNotFoundException(rawPackInfo);	
						}
					}
				}catch(Exception e){
					if(e instanceof FileNotFoundException){
						throw e;
					}else{
						InterfaceCore.logError("Could not parse which item PNG to get from: " + rawPackInfo);
						throw new FileNotFoundException(rawPackInfo);
					}
				}
				
			}
			
			//Return whichever stream we found.
			return stream;
		}

		@Override
		public boolean resourceExists(ResourceLocation location){
			return domains.contains(location.getNamespace()) 
					&& !location.getPath().contains("blockstates") 
					&& !location.getPath().contains("armatures") 
					&& !location.getPath().contains("mcmeta")
					&& ((location.getPath().endsWith(".json") && !location.getPath().equals("sounds.json")) || location.getPath().endsWith(".png"));
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
			return "Internal:" + domain;
		}
	}
}
