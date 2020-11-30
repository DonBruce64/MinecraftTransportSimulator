package mcinterface1122;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.AGUIBase.TextPosition;
import minecrafttransportsimulator.guis.instances.GUIHUD;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.IInterfaceRender;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;
import minecrafttransportsimulator.rendering.components.AParticle;
import minecrafttransportsimulator.rendering.components.RenderEventHandler;
import minecrafttransportsimulator.rendering.components.RenderTickData;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import net.minecraft.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(Side.CLIENT)
class InterfaceRender implements IInterfaceRender{
	private static final Map<String, Integer> textures = new HashMap<String, Integer>();
	private static final Map<BuilderEntity, RenderTickData> renderData = new HashMap<BuilderEntity, RenderTickData>();
	private static String pushedTextureLocation;
	private static BuilderGUI currentGUI = null;
	
	@Override
	public int getRenderPass(){
		return MinecraftForgeClient.getRenderPass();
	}
	
	@Override
	public boolean shouldRenderBoundingBoxes(){
		return Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox() && getRenderPass() != 1;
	}
	
	@Override
	public void bindTexture(String textureLocation){
		//If the texture has a colon, it's a short-hand form that needs to be converted.
		if(textureLocation.indexOf(":") != -1){
			textureLocation = "/assets/" + textureLocation.replace(":", "/");
		}
		//Bind texture if we have it.
		if(!textures.containsKey(textureLocation)){
			//Don't have this texture created yet.  Do so now.
			//Parse the texture, get the OpenGL integer that represents this texture, and save it.
			//FAR less jank than using MC's resource system.
			try{
				BufferedImage bufferedimage = TextureUtil.readBufferedImage(InterfaceRender.class.getResourceAsStream(textureLocation));
				int glTexturePointer = TextureUtil.glGenTextures();
		        TextureUtil.uploadTextureImageAllocate(glTexturePointer, bufferedimage, false, false);
		        textures.put(textureLocation, glTexturePointer);
			}catch(Exception e){
				MasterInterface.coreInterface.logError("ERROR: Could not find texture: " + textureLocation + " Reverting to fallback texture.");
				textures.put(textureLocation, TextureUtil.MISSING_TEXTURE.getGlTextureId());
			}
		}
		GlStateManager.bindTexture(textures.get(textureLocation));
	}
	
	@Override
	public void setTexture(String textureLocation){
		pushedTextureLocation = textureLocation;
		bindTexture(textureLocation);
	}
	
	@Override
	public void recallTexture(){
		if(pushedTextureLocation != null){
			GlStateManager.bindTexture(textures.get(pushedTextureLocation));
		}
	}
	
	@Override
	public void setLightingState(boolean enabled){
		setSystemLightingState(enabled);
		setInternalLightingState(enabled);
	}
	
	@Override
	public void setSystemLightingState(boolean enabled){
		if(enabled){
			GlStateManager.enableLighting();
		}else{
			GlStateManager.disableLighting();
		}
	}
	
	@Override
	public void setInternalLightingState(boolean enabled){
		if(enabled){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}else{
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}
	}
	
	@Override
	public void setLightingToEntity(AEntityBase entity){
		if(getRenderPass() == -1){
	        RenderHelper.enableStandardItemLighting();
	        setLightingState(true);
        }
		int lightVar = ((WrapperEntity) entity.wrapper).entity.getBrightnessForRender();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightVar%65536, lightVar/65536);
	}
	
	@Override
	public void setLightingToBlock(Point3i location){
		if(getRenderPass() == -1){
	        RenderHelper.enableStandardItemLighting();
	        setLightingState(true);
        }
		int lightVar = Minecraft.getMinecraft().world.getCombinedLight(new BlockPos(location.x, location.y, location.z), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightVar%65536, lightVar/65536);
	}
	
	@Override
	public void setBlendState(boolean enabled, boolean brightBlend){
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
	
	@Override
	public void setColorState(float red, float green, float blue, float alpha){
		GlStateManager.color(red, green, blue, alpha);
	}
	
	@Override
	public void resetStates(){
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
	
	@Override
	public void renderEntityRiders(AEntityBase entity, float partialTicks){
		for(IWrapperEntity rider : entity.locationRiderMap.values()){
			Entity riderEntity = ((WrapperEntity) rider).entity;
			if(!(MasterInterface.gameInterface.getClientPlayer().equals(rider) && MasterInterface.gameInterface.inFirstPerson()) && riderEntity.posY > riderEntity.world.getHeight()){
				GL11.glPushMatrix();
				Point3d riderPosition = rider.getRenderedPosition(partialTicks);
				GL11.glTranslated(riderPosition.x, riderPosition.y, riderPosition.z);
				Minecraft.getMinecraft().getRenderManager().renderEntityStatic(riderEntity, partialTicks, false);
				GL11.glPopMatrix();
			}
		}
	}
	
	@Override
	public void spawnParticle(AParticle particle){
		if(Minecraft.getMinecraft().effectRenderer != null){
			Minecraft.getMinecraft().effectRenderer.addEffect(new BuilderParticle(particle));
		}
	}
	
	@Override
	public void spawnBlockBreakParticles(Point3i point, boolean playSound){
		if(Minecraft.getMinecraft().effectRenderer != null){
			BlockPos pos = new BlockPos(point.x, point.y, point.z);
			if(!Minecraft.getMinecraft().world.isAirBlock(pos)){
				Minecraft.getMinecraft().effectRenderer.addBlockHitEffects(pos, EnumFacing.UP);
				if(playSound){
					SoundType soundType = Minecraft.getMinecraft().world.getBlockState(pos).getBlock().getSoundType(Minecraft.getMinecraft().world.getBlockState(pos), Minecraft.getMinecraft().player.world, pos, null);
					Minecraft.getMinecraft().world.playSound(Minecraft.getMinecraft().player, pos, soundType.getBreakSound(), SoundCategory.BLOCKS, soundType.getVolume(), soundType.getPitch());
				}
			}
		}
	}
	
	@Override
	public boolean renderTextMarkings(List<JSONText> textDefinitions, List<String> textLines, String inheritedColor, String objectRendering, boolean lightsOn){
		if(getRenderPass() != 1){
			boolean systemLightingEnabled = true;
			boolean internalLightingEnabled = true;
			if(textDefinitions != null){
				for(byte i=0; i<textDefinitions.size(); ++i){
					JSONText textDefinition = textDefinitions.get(i);
					String text = textLines.get(i);
					
					//Render if our attached object and the object we are rendering on match.
					if(textDefinition.attachedTo == null ? objectRendering == null : textDefinition.attachedTo.equals(objectRendering)){
						//Disable system lighting if we haven't already.
						//System lighting doesn't work well with text.
						if(systemLightingEnabled){
							setSystemLightingState(false);
							systemLightingEnabled = false;
						}
						
						//If we have light-up text, disable lightmap.
						if(textDefinition.lightsUp && lightsOn){
							if(internalLightingEnabled){
								internalLightingEnabled = false;
								setInternalLightingState(internalLightingEnabled);
							}
						}else if(!internalLightingEnabled){
							internalLightingEnabled = true;
							setInternalLightingState(internalLightingEnabled);
						}
						
						GL11.glPushMatrix();
						//Translate to the position to render.
						GL11.glTranslated(textDefinition.pos.x, textDefinition.pos.y, textDefinition.pos.z);
						//First rotate 180 along the X-axis to get us rendering right-side up.
						GL11.glRotatef(180F, 1, 0, 0);
						//Next, apply rotations.  Y is inverted due to the inverted X axis.
						GL11.glRotated(-textDefinition.rot.y, 0, 1, 0);
						GL11.glRotated(textDefinition.rot.x, 1, 0, 0);
						GL11.glRotated(textDefinition.rot.z, 0, 0, 1);
						//Scale by 1/16.  This converts us from block units to pixel units, which is what the GUIs use.
						GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
						//Finally, render the text.
						String colorString = textDefinition.colorInherited && inheritedColor != null ? inheritedColor : textDefinition.color;
						MasterInterface.guiInterface.drawScaledText(text, 0, 0, Color.decode(colorString), TextPosition.values()[textDefinition.renderPosition], textDefinition.wrapWidth, textDefinition.scale, textDefinition.autoScale);
						GL11.glPopMatrix();
					}
				}
			}
			
			//Reset lighting.
			if(!internalLightingEnabled){
				setInternalLightingState(true);
			}
			if(!systemLightingEnabled){
				setSystemLightingState(true);
				//Set color back to white, the font renderer sets this to not-white.
				setColorState(1.0F, 1.0F, 1.0F, 1.0F);
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	
	
	
	
	//--------------------START OF EVENT METHODS--------------------
	 /**
     * Pre-post methods for adjusting player angles while seated.
     */
    @SubscribeEvent
    public static void on(RenderPlayerEvent.Pre event){
    	EntityPlayer renderedPlayer = event.getEntityPlayer();
    	if(renderedPlayer.getRidingEntity() instanceof BuilderEntity){
        	AEntityBase ridingEntity = ((BuilderEntity) renderedPlayer.getRidingEntity()).entity;
        	GL11.glPushMatrix();
        	if(ridingEntity != null){
        		//Get total angles for the entity the player is riding.
        		Point3d totalAngles = ridingEntity.angles.copy();
	            if(ridingEntity instanceof EntityVehicleF_Physics){
	            	for(IWrapperEntity rider : ridingEntity.locationRiderMap.values()){
						if(Minecraft.getMinecraft().player.equals(((WrapperEntity) rider).entity)){
							PartSeat seat = (PartSeat) ((EntityVehicleF_Physics) ridingEntity).getPartAtLocation(ridingEntity.locationRiderMap.inverse().get(rider));
		            		totalAngles = ridingEntity.angles.copy().add(seat.placementRotation).add(seat.getPositionRotation(event.getPartialRenderTick()).add(seat.getActionRotation(event.getPartialRenderTick())));
		            		if(seat.parentPart != null){
		            			totalAngles.add(seat.parentPart.placementRotation).add(seat.parentPart.getPositionRotation(event.getPartialRenderTick()).add(seat.parentPart.getActionRotation(event.getPartialRenderTick())));
			            	}
						}
	            	}
	            }
	            
        		//Set the player yaw offset to 0.  This is needed as we are rotating the player manually.
	            renderedPlayer.renderYawOffset = 0;
	            
	            //Set the player's head yaw to the delta between their yaw and their angled yaw.
	            renderedPlayer.rotationYawHead = (float) (renderedPlayer.rotationYaw + totalAngles.y); 
	            
	            //Now add the rotations.
	            //We have to do this via OpenGL, as changing the player's pitch doesn't make them tilt in the seat, and roll doesn't exist for them.
	            //In this case, the player's eyes are their center point for rotation, but these aren't the same as 
	            //their actual position.  Means we have to do funky math.
	            //We also need to check if we are the client player or another player, as other players require a
	            //different pre-render offset to be performed to get them into the right place. 
	            if(!renderedPlayer.equals(Minecraft.getMinecraft().player)){
	            	EntityPlayerSP masterPlayer = Minecraft.getMinecraft().player;
	            	double playerDistanceX = renderedPlayer.lastTickPosX + - masterPlayer.lastTickPosX + (renderedPlayer.posX - renderedPlayer.lastTickPosX -(masterPlayer.posX - masterPlayer.lastTickPosX))*event.getPartialRenderTick();
	            	double playerDistanceY = renderedPlayer.lastTickPosY + - masterPlayer.lastTickPosY + (renderedPlayer.posY - renderedPlayer.lastTickPosY -(masterPlayer.posY - masterPlayer.lastTickPosY))*event.getPartialRenderTick();
	            	double playerDistanceZ = renderedPlayer.lastTickPosZ + - masterPlayer.lastTickPosZ + (renderedPlayer.posZ - renderedPlayer.lastTickPosZ -(masterPlayer.posZ - masterPlayer.lastTickPosZ))*event.getPartialRenderTick();
	                GL11.glTranslated(playerDistanceX, playerDistanceY, playerDistanceZ);
	                
	                GL11.glTranslated(0, renderedPlayer.getEyeHeight(), 0);
	                GL11.glRotated(totalAngles.y, 0, 1, 0);
	                GL11.glRotated(totalAngles.x, 1, 0, 0);
	                GL11.glRotated(totalAngles.z, 0, 0, 1);
	                GL11.glTranslated(0, -renderedPlayer.getEyeHeight(), 0);
	                
	                GL11.glTranslated(-playerDistanceX, -playerDistanceY, -playerDistanceZ);
	            }else{
	            	GL11.glTranslated(0, renderedPlayer.getEyeHeight(), 0);
	            	GL11.glRotated(totalAngles.y, 0, 1, 0);
	            	GL11.glRotated(totalAngles.x, 1, 0, 0);
	            	GL11.glRotated(totalAngles.z, 0, 0, 1);
	            	GL11.glTranslated(0, -renderedPlayer.getEyeHeight(), 0);
	            }
        	}
        }
    }

    @SubscribeEvent
    public static void on(RenderPlayerEvent.Post event){
    	if(event.getEntityPlayer().getRidingEntity() instanceof BuilderEntity){
    		GL11.glPopMatrix();
        }
    }
    
    @SubscribeEvent
    public static void on(CameraSetup event){
    	WrapperEntity renderEntity = WrapperWorld.getWrapperFor(event.getEntity().world).getWrapperFor(event.getEntity());
    	float currentPitch = event.getPitch();
    	float currentYaw = event.getYaw();
    	event.setPitch(0);
		event.setYaw(0);
    	if(!RenderEventHandler.onCameraSetup(renderEntity, (float) event.getRenderPartialTicks())){
    		event.setPitch(currentPitch);
    		event.setYaw(currentYaw);
    	}
    }
    
    @SubscribeEvent
    public static void on(RenderGameOverlayEvent.Pre event){
    	if(event.getType().equals(RenderGameOverlayEvent.ElementType.CROSSHAIRS) || event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR)){
    		if(RenderEventHandler.disableHUDComponents()){
    			event.setCanceled(true);
    		}
    	}else if(event.getType().equals(RenderGameOverlayEvent.ElementType.CHAT)){
    		RayTraceResult lastHit = Minecraft.getMinecraft().objectMouseOver;
    		AEntityBase mousedOverEntity = null;
    		Point3d mousedOverPoint = null;
			if(lastHit != null && lastHit.entityHit instanceof BuilderEntity){
				mousedOverEntity = ((BuilderEntity) lastHit.entityHit).entity;
				mousedOverPoint = new Point3d(lastHit.hitVec.x, lastHit.hitVec.y, lastHit.hitVec.z);
			}
    		AGUIBase requestedGUI = RenderEventHandler.onOverlayRender(event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight(), event.getPartialTicks(), mousedOverEntity, mousedOverPoint);
    		
    		//Make a new overlay GUI if we need to, or null out the savved GUI.
    		if(requestedGUI != null){
				if(currentGUI == null || !currentGUI.gui.equals(requestedGUI)){
					currentGUI = new BuilderGUI(requestedGUI);
					currentGUI.initGui();
					currentGUI.setWorldAndResolution(Minecraft.getMinecraft(), event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight());
				}
    		}else{
    			currentGUI = null;
    		}
    		
    		//If we have a GUI, render it.
    		if(currentGUI != null){
    			//Translate far enough to not render behind the items.
				//Also translate down if we are a half-HUD.
				GL11.glPushMatrix();
        		GL11.glTranslated(0, 0, 250);
        		if(currentGUI.gui instanceof GUIHUD && (MasterInterface.gameInterface.inFirstPerson() ? !ConfigSystem.configObject.clientRendering.fullHUD_1P.value : !ConfigSystem.configObject.clientRendering.fullHUD_3P.value)){
        			GL11.glTranslated(0, currentGUI.gui.getHeight()/2D, 0);
        		}
        		
        		//Enable alpha testing.  This can be disabled by mods doing bad state management during their event calls.
        		//We don't want to enable blending though, as that's on-demand.
        		GL11.glEnable(GL11.GL_ALPHA_TEST);
        		
        		//Draw the GUI.
        		currentGUI.drawScreen(0, 0, event.getPartialTicks());
        		
        		//Pop the matrix, and set blending and lighting back to normal.
        		GL11.glPopMatrix();
        		GL11.glEnable(GL11.GL_BLEND);
        		MasterInterface.renderInterface.setInternalLightingState(false);
    		}
    	}
    }
	
    @SubscribeEvent
    public static void on(RenderWorldLastEvent event){
    	Minecraft.getMinecraft().world.profiler.startSection("iv_render_pass_-1");
        for(Entity entity : Minecraft.getMinecraft().world.loadedEntityList){
            if(entity instanceof BuilderEntity){
            	Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(entity).doRender(entity, 0, 0, 0, 0, event.getPartialTicks());
            }
        }
        Entity renderViewEntity = Minecraft.getMinecraft().getRenderViewEntity();
		double playerX = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * event.getPartialTicks();
		double playerY = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * event.getPartialTicks();
		double playerZ = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * event.getPartialTicks();
        List<TileEntity> teList = Minecraft.getMinecraft().world.loadedTileEntityList; 
		for(int i=0; i<teList.size(); ++i){
			TileEntity tile = teList.get(i);
			if(tile instanceof BuilderTileEntity){
        		Vec3d delta = new Vec3d(tile.getPos()).add(-playerX, -playerY, -playerZ);
        		//Prevent crashing on corrupted TEs.
        		if(TileEntityRendererDispatcher.instance.getRenderer(tile) != null){
        			TileEntityRendererDispatcher.instance.getRenderer(tile).render(tile, delta.x, delta.y, delta.z, event.getPartialTicks(), 0, 0);
        		}
        	}
        }
        Minecraft.getMinecraft().world.profiler.endSection();
    }
    
	/**
	 *  Event that's called to register models.  We register our render wrapper
	 *  classes here, as well as all item JSONs.
	 */
	@SuppressWarnings("unchecked")
	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event){
		//Register the vehicle rendering class.
		RenderingRegistry.registerEntityRenderingHandler(BuilderEntity.class, new IRenderFactory<BuilderEntity>(){
			@Override
			public Render<? super BuilderEntity> createRenderFor(RenderManager manager){
			return new Render<BuilderEntity>(manager){
				@Override
				protected ResourceLocation getEntityTexture(BuilderEntity builder){
					return null;
				}
				
				@Override
				public void doRender(BuilderEntity builder, double x, double y, double z, float entityYaw, float partialTicks){
					if(builder.entity != null){
						//If we don't have render data yet, create one now.
						if(!renderData.containsKey(builder)){
							renderData.put(builder, new RenderTickData(builder.entity.world));
						}
						
						//Get render pass.  Render data uses 2 for pass -1 as it uses arrays and arrays can't have a -1 index.
						int renderPass = MasterInterface.renderInterface.getRenderPass();
						if(renderPass == -1){
							renderPass = 2;
						}
						
						//If we need to render, do so now.
						if(renderData.get(builder).shouldRender(renderPass, partialTicks)){
							builder.entity.render(partialTicks);
						}
					}
				}
			};
		}});
		
		//Register the TESR wrapper.
		ClientRegistry.bindTileEntitySpecialRenderer(BuilderTileEntity.class, new BuilderTileEntityRender());
		
		//Get the list of default resource packs here to inject a custom parser for auto-generating JSONS.
		//FAR easier than trying to use the bloody bakery system.
		//Normally we'd add our pack to the current loader, but this gets wiped out during reloads and unless we add our pack to the main list, it won't stick.
		//To do this, we use reflection to get the field from the main MC class that holds the master list to add our custom ones.
		//((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).reloadResourcePack(new PackResourcePack(MasterInterface.MODID + "_packs"));
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
			MasterInterface.coreInterface.logError("ERROR: Could not get default pack list. Item icons will be disabled.");
			return;
		}
		
		//Now that we have the custom resource pack location, add our built-in loader.
		//This one auto-generates item JSONs.
		defaultPacks.add(new PackResourcePack(MasterInterface.MODID + "_packs"));
		
		//Register the core item models.  Some of these are pack-based.
		//Don't add those as they get added during the pack registration processing. 
		for(Entry<AItemBase, BuilderItem> entry : BuilderItem.itemMap.entrySet()){
			try{
				//TODO remove this when we don't have non-pack items.
				if(!(entry.getValue().item instanceof AItemPack)){
					registerCoreItemRender(entry.getValue());
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		//Now register items for the packs.
		//If we ever register a pack item from a non-external pack, we'll need to make a resource loader for it.
		//This is done to allow MC/Forge to play nice with item textures.
		for(AItemPack<?> packItem : PackParserSystem.getAllPackItems()){
			//TODO remove this when the internal system actually works.
			if(PackParserSystem.getPackConfiguration(packItem.definition.packID) == null || PackParserSystem.getPackConfiguration(packItem.definition.packID).internallyGenerated){
				ModelLoader.setCustomModelResourceLocation(BuilderItem.itemMap.get(packItem), 0, new ModelResourceLocation(MasterInterface.MODID + "_packs:" + packItem.definition.packID + AItemPack.PACKID_SEPARATOR + packItem.getRegistrationName(), "inventory"));
			}else{
				if(!PackResourcePack.createdLoaders.containsKey(packItem.definition.packID)){
					defaultPacks.add(new PackResourcePack(packItem.definition.packID));
				}
				ModelLoader.setCustomModelResourceLocation(BuilderItem.itemMap.get(packItem), 0, new ModelResourceLocation(MasterInterface.MODID + "_packs:" + packItem.getRegistrationName(), "inventory"));
			}
		}
		
		//Now that we've created all the pack loaders, reload the resource manager to add them to the systems.
		FMLClientHandler.instance().refreshResources(VanillaResourceType.MODELS);
	}
	
	/**
	 *  Helper method to register renders.
	 */
	private static void registerCoreItemRender(Item item){
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(MasterInterface.MODID + ":" + item.getRegistryName().getPath(), "inventory"));
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
			String rawPackInfo = location.getPath();
			
			//Strip the suffix from the packInfo, and then test to see if it's an internal
			//resource or one being handled by an external loader.
			String strippedSuffix = rawPackInfo.substring(0, rawPackInfo.lastIndexOf("."));
			if(!strippedSuffix.contains(AItemPack.PACKID_SEPARATOR)){
				try{
					return getClass().getResourceAsStream("/assets/" + domain + "/" + rawPackInfo);
				}catch(Exception e){
					MasterInterface.coreInterface.logError("ERROR: Could not find JSON-specified file: " + rawPackInfo);
					throw new FileNotFoundException(rawPackInfo);
				}
			}else{
				//Create stream return variable.
				InputStream stream;
				
				//If we are for an item JSON, try to find that JSON, or generate one automatically.
				//If we are for an item PNG, just load the PNG as-is.  If we don't find it, then just let MC purple checker it.
				//Note that the internal mts_packs loader does not do PNG loading, as it re-directs the PNG files to the pack's loaders.
				if(rawPackInfo.endsWith(".json")){
					String resourcePath = "";
					String itemTexturePath = "";
						
					//Strip off the auto-generated prefix.
					String combinedPackInfo = rawPackInfo;
					combinedPackInfo = strippedSuffix.substring("models/item/".length());
					
					//Get the pack information, and try to load the resource.
					try{
						String packID = combinedPackInfo.substring(0, combinedPackInfo.indexOf(AItemPack.PACKID_SEPARATOR));
						String systemName = combinedPackInfo.substring(combinedPackInfo.indexOf(AItemPack.PACKID_SEPARATOR) + 1);
						AItemPack<?> packItem = PackParserSystem.getItem(packID, systemName);
						resourcePath = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_JSON, systemName);
						
						//Try to load the item JSON, or create it if it doesn't exist.
						stream = getClass().getResourceAsStream(resourcePath);
						if(stream == null){
							//Get the actual texture path.
							itemTexturePath = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_PNG, systemName);
							
							//Remove the "/assets/textures/" portion as it's implied with JSON.
							itemTexturePath = itemTexturePath.substring(("/assets/"  + packID + "/textures/").length());
							
							//Remove the .png suffix as it's also implied.
							itemTexturePath = itemTexturePath.substring(0, itemTexturePath.length() - ".png".length());
							
							//Need to add packID domain to this to comply with JSON domains.
							itemTexturePath = packID + ":" + itemTexturePath;
							
							//Generate fake JSON and return as stream to MC loader.
							String fakeJSON = "{\"parent\":\"mts:item/basic\",\"textures\":{\"layer0\": \"" + itemTexturePath + "\"}}";
							stream = new ByteArrayInputStream(fakeJSON.getBytes(StandardCharsets.UTF_8));
						}
						return stream;
					}catch(Exception e){
						MasterInterface.coreInterface.logError("ERROR: Could not parse out item JSON from: " + rawPackInfo + "  Looked for JSON at:" + resourcePath + (itemTexturePath.isEmpty() ? (", with fallback at:" + itemTexturePath) : ", but could not find it."));
						throw new FileNotFoundException(rawPackInfo);
					}
				}else{
					try{
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
						
						//Get the actual resource path for this resource and return its stream.
						stream = getClass().getResourceAsStream(PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_PNG, systemName));
						if(stream != null){
							return stream;
						}else{
							MasterInterface.coreInterface.logError("ERROR: Could not find item PNG: " + rawPackInfo);
							throw new FileNotFoundException(rawPackInfo);
						}
					}catch(Exception e){
						MasterInterface.coreInterface.logError("ERROR: Could not parse which item PNG to get from: " + rawPackInfo);
						throw new FileNotFoundException(rawPackInfo);
					}
				}
			}
		}

		@Override
		public boolean resourceExists(ResourceLocation location){
			return domains.contains(location.getNamespace()) 
					&& !location.getPath().contains("blockstates") 
					&& !location.getPath().contains("armatures") 
					&& !location.getPath().contains("mcmeta")
					&& (location.getPath().startsWith("models/item/") || location.getPath().startsWith("textures/"));
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
	/*
	private static final ICustomModelLoader packModelLoader = new ICustomModelLoader(){

		@Override
		public void onResourceManagerReload(IResourceManager resourceManager){
			//Do nothing.  Packs don't change.
		}

		@Override
		public boolean accepts(ResourceLocation modelLocation){
			return modelLocation.getResourceDomain().equals("mts_packs");
		}

		@Override
		public IModel loadModel(ResourceLocation modelLocation) throws Exception{
			//Get the resource from the path.  Domain is mts_packs always.
			String resource = modelLocation.getResourcePath();
			
			//Strip off the mts_packs: prefix. 
			resource.substring("mts_packs:".length());
			
			//Get the pack information.
			String packID = resource.substring(0, resource.indexOf('.'));
			String systemName = resource.substring(resource.indexOf('.') + 1);
			AItemPack<?> packItem = PackParserSystem.getItem(packID, systemName);
			
			//Add the texture to the sprite system.
			TextureAtlasSprite itemSprite = new CustomTextureLoader(packItem.getRegistrationName());
			//TextureMap textureMap = Minecraft.getMinecraft().getTextureMapBlocks().registerSprite(modelLocation);
			
			
			//Return the Un-baked model.
			return packItem != null ? new UnbakedItemModelWrapper(packItem) : null;
		}
	};
	
	private static class CustomTextureLoader extends TextureAtlasSprite{
		public CustomTextureLoader(String spriteName){
			super(spriteName);
		}
	}
	
	private static class UnbakedItemModelWrapper implements IModel{
		private static final List<ResourceLocation> EMPTY_TEXTURE_LIST = new ArrayList<ResourceLocation>();
		
		private final AItemPack<?> packItem;
		
		UnbakedItemModelWrapper(AItemPack<?> packItem){
			this.packItem = packItem;
		}
		
		@Override
		public Collection<ResourceLocation> getTextures(){
	        return EMPTY_TEXTURE_LIST;
	    }
	    
		@Override
		public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter){
			
			//Get the texture location.
			final String texturePath = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_PNG, packItem.definition.systemName);
			
			Minecraft.getMinecraft().getItemRenderer().renderItemInFirstPerson(partialTicks);
		}
	};
	
	private static class BakedItemModelWrapper implements IBakedModel{
		private static final List<BakedQuad> EMPTY_QUAD_LIST = new ArrayList<BakedQuad>();
		
		
		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand){
			if(side == null){
				int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
				MasterInterface.renderInterface.bindTexture(texturePath);
				GL11.glBegin(GL11.GL_TRIANGLES);
                GL11.glTexCoord2f(0, 0);
                GL11.glVertex3f(0, 0, 0);
                GL11.glTexCoord2f(0, 1);
                GL11.glVertex3f(0, 1, 0);
                GL11.glTexCoord2f(1, 1);
                GL11.glVertex3f(1, 1, 0);
                GL11.glTexCoord2f(1, 0);
                GL11.glVertex3f(1, 0, 0);
                GL11.glEnd();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture);
			}
			return EMPTY_QUAD_LIST;
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
			return null;
		}

		@Override
		public ItemOverrideList getOverrides(){
			return ItemOverrideList.NONE;
		}
	};*/
}
