package minecrafttransportsimulator.mcinterface;

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

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.AGUIBase.TextPosition;
import minecrafttransportsimulator.guis.instances.GUIHUD;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;
import minecrafttransportsimulator.rendering.components.AParticle;
import minecrafttransportsimulator.rendering.components.ITextProvider;
import minecrafttransportsimulator.rendering.components.RenderEventHandler;
import minecrafttransportsimulator.rendering.components.RenderTickData;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityPlayerGun;
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
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, and registering rendering systems
 * for TESRs, items, and entities.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceRender{
	private static final Map<String, Integer> textures = new HashMap<String, Integer>();
	private static final Map<BuilderEntity, RenderTickData> renderData = new HashMap<BuilderEntity, RenderTickData>();
	private static String pushedTextureLocation;
	private static BuilderGUI currentGUI = null;
	public static boolean shadersDetected;
	
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
	public static void bindTexture(String textureLocation){
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
				InterfaceCore.logError("ERROR: Could not find texture: " + textureLocation + " Reverting to fallback texture.");
				textures.put(textureLocation, TextureUtil.MISSING_TEXTURE.getGlTextureId());
			}
		}
		GlStateManager.bindTexture(textures.get(textureLocation));
	}
	
	/**
	 *  Like bindTexture, but this method also sets the texture for binding recall later via recallTexture.
	 *  This allows for us to recall specific textures anywhere in the code.  Useful when we don't know what
	 *  we will render between this call and another call, but we do know that we want this texture to be
	 *  re-bound if any other textures were bound.
	 */
	public static void setTexture(String textureLocation){
		pushedTextureLocation = textureLocation;
		bindTexture(textureLocation);
	}
	
	/**
	 *  Re-binds the last saved texture.
	 */
	public static void recallTexture(){
		if(pushedTextureLocation != null){
			GlStateManager.bindTexture(textures.get(pushedTextureLocation));
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
	 *  passed-in entitie's location.  This will also enable lighting should
	 *  the current render pass be -1.
	 */
	public static void setLightingToEntity(AEntityBase entity){
		if(getRenderPass() == -1){
	        RenderHelper.enableStandardItemLighting();
	        setLightingState(true);
        }
		int lightVar = entity.wrapper.entity.getBrightnessForRender();
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
        }
		int lightVar = Minecraft.getMinecraft().world.getCombinedLight(new BlockPos(location.x, location.y, location.z), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightVar%65536, lightVar/65536);
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
	 *  This method manually renders all riders on an entity.  Useful if you're rendering the entity manually
	 *  and the entity and its riders have been culled from rendering.
	 */
	public static void renderEntityRiders(AEntityBase entity, float partialTicks){
		for(WrapperEntity rider : entity.locationRiderMap.values()){
			Entity riderEntity = rider.entity;
			if(!(InterfaceClient.getClientPlayer().equals(rider) && InterfaceClient.inFirstPerson()) && riderEntity.posY > riderEntity.world.getHeight()){
				GL11.glPushMatrix();
				Point3d riderPosition = rider.getRenderedPosition(partialTicks);
				GL11.glTranslated(riderPosition.x, riderPosition.y, riderPosition.z);
				Minecraft.getMinecraft().getRenderManager().renderEntityStatic(riderEntity, partialTicks, false);
				GL11.glPopMatrix();
			}
		}
	}
	
	/**
	 *  Spawns a particle into the world.  Particles are simply entities that are client-side only.
	 *  This is handy if you have a lot of them flying around but could care less where they are and
	 *  don't want to hamper the server with tons of ticking entities.
	 */
	public static void spawnParticle(AParticle particle){
		if(Minecraft.getMinecraft().effectRenderer != null){
			Minecraft.getMinecraft().effectRenderer.addEffect(new BuilderParticle(particle));
		}
	}
	
	/**
	 *  Spawns the particles for the block at the passed-in position.
	 *  This also allows for playing the block breaking sound.
	 *  It does not actually break the block.  Such breakage must be done on the server.
	 */
	public static void spawnBlockBreakParticles(Point3i point, boolean playSound){
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
	
	/**
	 *  Renders all the text markings on the passed-in provider.
	 *  This should only be called in pass 0, as we don't do any alpha blending in this routine.
	 *  Return true if we rendered anything.  This lets any rendering systems reset their bound texture if required.
	 */
	public static boolean renderTextMarkings(ITextProvider provider, String objectRendering){
		if(getRenderPass() != 1){
			boolean systemLightingEnabled = true;
			boolean internalLightingEnabled = true;
			for(Entry<JSONText, String> textLine : provider.getText().entrySet()){
				JSONText textDefinition = textLine.getKey();
				String text = textLine.getValue();
				
				//Render if our attached object and the object we are rendering on match.
				if(textDefinition.attachedTo == null ? objectRendering == null : textDefinition.attachedTo.equals(objectRendering)){
					//Disable system lighting if we haven't already.
					//System lighting doesn't work well with text.
					if(systemLightingEnabled){
						setSystemLightingState(false);
						systemLightingEnabled = false;
					}
					
					//If we have light-up text, disable lightmap.
					if(textDefinition.lightsUp && provider.renderTextLit()){
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
					String inheritedColor = provider.getSecondaryTextColor();
					String colorString = textDefinition.colorInherited && inheritedColor != null ? inheritedColor : textDefinition.color;
					InterfaceGUI.drawScaledText(text, 0, 0, Color.decode(colorString), TextPosition.values()[textDefinition.renderPosition], textDefinition.wrapWidth, textDefinition.scale, textDefinition.autoScale);
					GL11.glPopMatrix();
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
        		Point3d entityAngles = ridingEntity.angles.copy();
        		Point3d ridingAngles = new Point3d(0, 0, 0);
	            if(ridingEntity instanceof EntityVehicleF_Physics){
	            	for(WrapperEntity rider : ridingEntity.locationRiderMap.values()){
						if(Minecraft.getMinecraft().player.equals(rider.entity)){
							PartSeat seat = (PartSeat) ((EntityVehicleF_Physics) ridingEntity).getPartAtLocation(ridingEntity.locationRiderMap.inverse().get(rider));
							ridingAngles = seat.placementRotation.copy().add(seat.getPositionRotation(event.getPartialRenderTick()));
		            		if(seat.parentPart != null){
		            			ridingAngles.add(seat.parentPart.placementRotation).add(seat.parentPart.getPositionRotation(event.getPartialRenderTick()));
			            	}
						}
	            	}
	            }
	            
        		//Set the player yaw offset to 0.  This is needed as we are rotating the player manually.
	            renderedPlayer.renderYawOffset = 0;
	            
	            //Set the player's head yaw to the delta between their yaw and their angled yaw.
	            renderedPlayer.rotationYawHead = (float) (renderedPlayer.rotationYaw + entityAngles.y + ridingAngles.y);
	            
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
	                GL11.glRotated(entityAngles.y, 0, 1, 0);
	                GL11.glRotated(entityAngles.x, 1, 0, 0);
	                GL11.glRotated(entityAngles.z, 0, 0, 1);
	                if(!ridingAngles.isZero()){
		                GL11.glRotated(ridingAngles.y, 0, 1, 0);
		                GL11.glRotated(ridingAngles.x, 1, 0, 0);
		                GL11.glRotated(ridingAngles.z, 0, 0, 1);
	                }
	                GL11.glTranslated(0, -renderedPlayer.getEyeHeight(), 0);
	                
	                GL11.glTranslated(-playerDistanceX, -playerDistanceY, -playerDistanceZ);
	            }else{
	            	GL11.glTranslated(0, renderedPlayer.getEyeHeight(), 0);
	            	GL11.glRotated(entityAngles.y, 0, 1, 0);
	                GL11.glRotated(entityAngles.x, 1, 0, 0);
	                GL11.glRotated(entityAngles.z, 0, 0, 1);
	                if(!ridingAngles.isZero()){
		                GL11.glRotated(ridingAngles.y, 0, 1, 0);
		                GL11.glRotated(ridingAngles.x, 1, 0, 0);
		                GL11.glRotated(ridingAngles.z, 0, 0, 1);
	                }
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
    	if(event.getEntity() instanceof EntityPlayer){
	    	WrapperPlayer playerWrapper = WrapperWorld.getWrapperFor(event.getEntity().world).getWrapperFor((EntityPlayer) event.getEntity());
	    	float currentPitch = event.getPitch();
	    	float currentYaw = event.getYaw();
	    	event.setPitch(0);
			event.setYaw(0);
	    	if(!RenderEventHandler.onCameraSetup(playerWrapper, (float) event.getRenderPartialTicks())){
	    		event.setPitch(currentPitch);
	    		event.setYaw(currentYaw);
	    	}
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
        		if(currentGUI.gui instanceof GUIHUD && (InterfaceClient.inFirstPerson() ? !ConfigSystem.configObject.clientRendering.fullHUD_1P.value : !ConfigSystem.configObject.clientRendering.fullHUD_3P.value)){
        			GL11.glTranslated(0, currentGUI.gui.getHeight()/2D, 0);
        		}
        		
        		//Enable alpha testing.  This can be disabled by mods doing bad state management during their event calls.
        		//We don't want to enable blending though, as that's on-demand.
        		//Just in case it is enabled, however, disable it.
        		//This ensures the blending state is as it will be for the main rendering pass of -1.
        		GL11.glDisable(GL11.GL_BLEND);
        		GL11.glEnable(GL11.GL_ALPHA_TEST);
        		
        		//Draw the GUI.
        		currentGUI.drawScreen(0, 0, event.getPartialTicks());
        		
        		//Pop the matrix, and set blending and lighting back to normal.
        		GL11.glPopMatrix();
        		GL11.glEnable(GL11.GL_BLEND);
        		setInternalLightingState(false);
    		}
    	}
    }
    
    @SubscribeEvent
    public static void on(RenderHandEvent event){
    	EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getMinecraft().player.getUniqueID().toString());
    	if(entity != null && entity.gun != null){
    		event.setCanceled(true);
    	}
    }
    
    @SubscribeEvent
    public static void on(RenderSpecificHandEvent event){
    	EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getMinecraft().player.getUniqueID().toString());
    	if(entity != null && entity.gun != null){
    		event.setCanceled(true);
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
						int renderPass = getRenderPass();
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
			InterfaceCore.logError("ERROR: Could not get default pack list. Item icons will be disabled.");
			return;
		}
		
		//Now that we have the custom resource pack location, add our built-in loader.
		//This one auto-generates item JSONs.
		defaultPacks.add(new PackResourcePack(MasterLoader.MODID + "_packs"));
		
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
				ModelLoader.setCustomModelResourceLocation(packItem.getBuilder(), 0, new ModelResourceLocation(MasterLoader.MODID + "_packs:" + packItem.definition.packID + AItemPack.PACKID_SEPARATOR + packItem.getRegistrationName(), "inventory"));
			}else{
				if(!PackResourcePack.createdLoaders.containsKey(packItem.definition.packID)){
					defaultPacks.add(new PackResourcePack(packItem.definition.packID));
				}
				ModelLoader.setCustomModelResourceLocation(packItem.getBuilder(), 0, new ModelResourceLocation(MasterLoader.MODID + "_packs:" + packItem.getRegistrationName(), "inventory"));
			}
		}
		
		//Now that we've created all the pack loaders, reload the resource manager to add them to the systems.
		FMLClientHandler.instance().refreshResources(VanillaResourceType.MODELS);
	}
	
	/**
	 *  Helper method to register renders.
	 */
	private static void registerCoreItemRender(Item item){
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(MasterLoader.MODID + ":" + item.getRegistryName().getPath(), "inventory"));
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
				if(!strippedSuffix.contains(AItemPack.PACKID_SEPARATOR)){
					//JSON reference.  Get the specified file.
					stream = getClass().getResourceAsStream("/assets/" + domain + "/" + rawPackInfo);
					if(stream == null){
						InterfaceCore.logError("ERROR: Could not find JSON-specified file: " + rawPackInfo);
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
						String packID = combinedPackInfo.substring(0, combinedPackInfo.indexOf(AItemPack.PACKID_SEPARATOR));
						String systemName = combinedPackInfo.substring(combinedPackInfo.indexOf(AItemPack.PACKID_SEPARATOR) + 1);
						AItemPack<?> packItem = PackParserSystem.getItem(packID, systemName);
						resourcePath = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_JSON, systemName);
						
						//Try to load the item JSON, or create it if it doesn't exist.
						stream = getClass().getResourceAsStream(resourcePath);
						if(stream == null){
							//Get the actual texture path.
							itemTexturePath = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_PNG, systemName);
							
							//Remove the "/assets/packID/" portion as it's implied with JSON.
							itemTexturePath = itemTexturePath.substring(("/assets/"  + packID + "/").length());
							
							//If the packloader is internal, remove the "textures/" prefix.  This is auto-generated.
							//If we don't do this, then the assets won't load right.
							//TODO remove this when generators aren't internal.
							if(PackParserSystem.getPackConfiguration(packID).internallyGenerated){
								itemTexturePath = itemTexturePath.substring("textures/".length());
							}
							
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
						InterfaceCore.logError("ERROR: Could not parse out item JSON from: " + rawPackInfo + "  Looked for JSON at:" + resourcePath + (itemTexturePath.isEmpty() ? (", with fallback at:" + itemTexturePath) : ", but could not find it."));
						throw new FileNotFoundException(rawPackInfo);
					}
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
					
					String streamLocation = null;
					if(packItem != null){
						//Get the actual resource path for this resource and return its stream.
						streamLocation = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_PNG, systemName);
						stream = getClass().getResourceAsStream(streamLocation);
					}else{
						stream = null;
					}
					
					if(stream == null){
						//We might not have this file, but we also might have a JSON-defined item here.
						//Try the JSON standards before throwing an error.
						String streamJSONLocation = "/assets/" + packID + "/" + rawPackInfo;
						stream = getClass().getResourceAsStream(streamJSONLocation);
						if(stream == null){
							if(streamLocation != null){
								InterfaceCore.logError("ERROR: Could not find item PNG at specified location: " + streamLocation + "  Or potential JSON location: " + streamJSONLocation);
							}else{
								InterfaceCore.logError("ERROR: Could not find JSON PNG: " + streamJSONLocation);
							}
							throw new FileNotFoundException(rawPackInfo);
						}
					}
				}catch(Exception e){
					if(e instanceof FileNotFoundException){
						throw e;
					}else{
						InterfaceCore.logError("ERROR: Could not parse which item PNG to get from: " + rawPackInfo);
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
}
