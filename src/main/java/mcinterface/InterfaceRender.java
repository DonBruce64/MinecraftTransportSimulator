package mcinterface;

import java.awt.Color;
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
import minecrafttransportsimulator.guis.instances.GUIHUD;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.rendering.components.AParticle;
import minecrafttransportsimulator.rendering.components.RenderTickData;
import minecrafttransportsimulator.systems.ConfigSystem;
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

/**Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, and registering rendering systems
 * for TESRs, items, and entities.
 *
 * @author don_bruce
 */
@Mod.EventBusSubscriber(Side.CLIENT)
public class InterfaceRender{
	private static final Map<String, Map<String, ResourceLocation>> textures = new HashMap<String, Map<String, ResourceLocation>>();
	private static final Map<BuilderEntity, RenderTickData> renderData = new HashMap<BuilderEntity, RenderTickData>();
	private static String pushedTextureDomain;
	private static String pushedTextureLocation;
	private static int zoomLevel = 0;
	
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
	 *  passed-in entitie's location.  This will also enable lighting should
	 *  the current render pass be -1.
	 */
	public static void setLightingToEntity(AEntityBase entity){
		if(getRenderPass() == -1){
	        RenderHelper.enableStandardItemLighting();
	        setLightingState(true);
        }
		int lightVar = BuilderEntity.entitiesToBuilders.get(entity).getBrightnessForRender();
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
		int lightVar = InterfaceGame.getClientWorld().world.getCombinedLight(new BlockPos(location.x, location.y, location.z), 0);
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
	 *  Adjusts the camera zoom, zooming in or out depending on the flag.
	 */
	public static void changeCameraZoom(boolean zoomIn){
		if(zoomIn && zoomLevel > 0){
			zoomLevel -= 2;
		}else if(!zoomIn){
			zoomLevel += 2;
		}
	}
	
	/**
	 *  This method manually renders all riders on an entity.  Useful if you're rendering the entity manually
	 *  and the entity and its riders have been culled from rendering.
	 */
	public static void renderEntityRiders(AEntityBase entity, float partialTicks){
		for(WrapperEntity rider : entity.ridersToLocations.keySet()){
			if(!(InterfaceGame.getClientPlayer().equals(rider.entity) && InterfaceGame.inFirstPerson()) && rider.entity.posY > rider.entity.world.getHeight()){
				GL11.glPushMatrix();
				Point3d riderPosition = rider.getRenderedPosition(partialTicks);
				GL11.glTranslated(riderPosition.x, riderPosition.y, riderPosition.z);
				Minecraft.getMinecraft().getRenderManager().renderEntityStatic(rider.entity, partialTicks, false);
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
	 *  This also plays the block breaking sound.  It does not actually break
	 *  the block.  Such breakage must be done on the server.
	 */
	public static void spawnBlockBreakParticles(Point3i point){
		if(Minecraft.getMinecraft().effectRenderer != null){
			BlockPos pos = new BlockPos(point.x, point.y, point.z);
			SoundType soundType = Minecraft.getMinecraft().world.getBlockState(pos).getBlock().getSoundType(Minecraft.getMinecraft().world.getBlockState(pos), Minecraft.getMinecraft().player.world, pos, null);
			Minecraft.getMinecraft().world.playSound(null, pos, soundType.getBreakSound(), SoundCategory.BLOCKS, soundType.getVolume(), soundType.getPitch());
			Minecraft.getMinecraft().effectRenderer.addBlockHitEffects(pos, EnumFacing.UP);
		}
	}
	
	/**
	 *  Renders all the text markings given the passed-in parameters.
	 *  This should only be called in pass 0, as we don't do any alpha blending in this routine.
	 *  Return true if we rendered anything.  This lets any rendering systems reset their bound states if required.
	 */
	public static boolean renderTextMarkings(List<JSONText> textDefinitions, List<String> textLines, String objectRendering, boolean lightsOn){
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
						//System.out.println(text);
						GL11.glPushMatrix();
						//Offset by 1/2 a block to account for text centering.
						GL11.glTranslated(textDefinition.pos[0], textDefinition.pos[1] + 0.5D*textDefinition.scale/16D, textDefinition.pos[2]);
						GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
						//First rotate 180 along the X-axis to get us rendering right-side up.
						GL11.glRotatef(180F, 1, 0, 0);
						//Next, apply rotations.  Y is inverted due to the inverted X axis.
						GL11.glRotated(-textDefinition.rot[1], 0, 1, 0);
						GL11.glRotated(textDefinition.rot[0], 1, 0, 0);
						GL11.glRotated(textDefinition.rot[2], 0, 0, 1);
						
						//Finally, render the text.
						if(textDefinition.alignLeft){
							BuilderGUI.drawScaledText(text, 0, 0, Color.decode(textDefinition.color), false, false, 0, textDefinition.scale);
						}else if(textDefinition.alignRight){
							BuilderGUI.drawScaledText(text, -BuilderGUI.getStringWidth(text), 0, Color.decode(textDefinition.color), false, false, 0, textDefinition.scale);
						}else{
							BuilderGUI.drawScaledText(text, 0, 0, Color.decode(textDefinition.color), true, false, 0, textDefinition.scale);
						}
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
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	
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
	            	for(WrapperEntity rider : ridingEntity.ridersToLocations.keySet()){
						if(Minecraft.getMinecraft().player.equals(rider.entity)){
							PartSeat seat = (PartSeat) ((EntityVehicleF_Physics) ridingEntity).getPartAtLocation(ridingEntity.ridersToLocations.get(rider));
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
    
    /**
     * Adjusts roll, pitch, and zoom for camera.
     * Roll and pitch only gets updated when in first-person as we use OpenGL transforms.
     * For external rotations, we just let the entity adjust the player's pitch and yaw.
     * This is because first-person view is for direct control, while third-person is for passive control.
     */
    @SubscribeEvent
    public static void on(CameraSetup event){
    	if(event.getEntity().getRidingEntity() instanceof BuilderEntity){
    		if(InterfaceGame.inFirstPerson()){            	
    			AEntityBase ridingEntity = ((BuilderEntity) event.getEntity().getRidingEntity()).entity;
            	//FIXME this probably should be some sort of vector calculation.
    			
    			//Need to check if the riding entity exists.  Player may be loading the entity on the client but it hasn't
    			//gotten the supplemental data from the server yet.
    			if(ridingEntity != null){
	            	//Get yaw delta between entity and player from-180 to 180.
	            	double playerYawDelta = (360 + (ridingEntity.angles.y - -event.getEntity().rotationYaw)%360)%360;
	            	if(playerYawDelta > 180){
	            		playerYawDelta-=360;
	            	}
	            	
	            	//Get the component of the pitch and roll that should be applied based on the yaw delta.
	            	//This is based on where the player is looking.  If the player is looking straight forwards, then we want 100% of the
	            	//pitch to be applied as pitch.  But, if they are looking to the side, then we need to apply that as roll, not pitch.
	            	double pitchPitchComponent = Math.cos(Math.toRadians(playerYawDelta))*(ridingEntity.prevAngles.x + (ridingEntity.angles.x - ridingEntity.prevAngles.x)*event.getRenderPartialTicks());
	            	double rollPitchComponent = Math.sin(Math.toRadians(playerYawDelta))*(ridingEntity.prevAngles.z + (ridingEntity.angles.z - ridingEntity.prevAngles.z)*event.getRenderPartialTicks());
	            	double rollRollComponent = Math.cos(Math.toRadians(playerYawDelta))*(ridingEntity.prevAngles.z + (ridingEntity.angles.z - ridingEntity.prevAngles.z)*event.getRenderPartialTicks());
	            	double pitchRollComponent = (1 - Math.cos(Math.toRadians(playerYawDelta)))*(ridingEntity.prevAngles.x + (ridingEntity.angles.x - ridingEntity.prevAngles.x)*event.getRenderPartialTicks());
	            	GL11.glRotated(rollRollComponent + pitchRollComponent, 0, 0, 1);
	            	GL11.glRotated(pitchPitchComponent + rollPitchComponent, 1, 0, 0);
    			}
        	}else if(InterfaceGame.inThirdPerson()){
        		GL11.glTranslatef(0, 0F, -zoomLevel);
        		GL11.glTranslated(-1D, 1D, -2D);
            }else{
                GL11.glTranslatef(0, 0F, zoomLevel);
        	}
        }
    }
    

    private static BuilderGUI currentHUD = null;
    /**
     * Renders the HUD on vehicles.
     */
    @SubscribeEvent
    public static void on(RenderGameOverlayEvent.Post event){
    	if(event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR)){
    		if(InterfaceGame.inFirstPerson() ? ConfigSystem.configObject.client.renderHUD_1P.value : ConfigSystem.configObject.client.renderHUD_3P.value){
				if(Minecraft.getMinecraft().player.getRidingEntity() instanceof BuilderEntity){
					AEntityBase ridingEntity = ((BuilderEntity) Minecraft.getMinecraft().player.getRidingEntity()).entity;
					if(ridingEntity instanceof EntityVehicleF_Physics){
						for(WrapperEntity rider : ridingEntity.ridersToLocations.keySet()){
							if(Minecraft.getMinecraft().player.equals(rider.entity)){
								PartSeat seat = (PartSeat) ((EntityVehicleF_Physics) ridingEntity).getPartAtLocation(ridingEntity.ridersToLocations.get(rider));
								if(seat.vehicleDefinition.isController){
									//Make a new HUD if we need to.
									if(currentHUD == null){
										currentHUD = new BuilderGUI(new GUIHUD((EntityVehicleF_Physics) ridingEntity));
										currentHUD.initGui();
										currentHUD.setWorldAndResolution(Minecraft.getMinecraft(), event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight());
									}
									
									//Translate far enough to not render behind the items.
									//Also translate down if we are a helf-HUD.
									GL11.glPushMatrix();
			                		GL11.glTranslated(0, 0, 250);
			                		if(InterfaceGame.inFirstPerson() ? !ConfigSystem.configObject.client.fullHUD_1P.value : !ConfigSystem.configObject.client.fullHUD_3P.value){
			                			GL11.glTranslated(0, currentHUD.gui.getHeight()/2D, 0);
			                		}
			                		
			                		//Enable alpha testing.
			                		GL11.glEnable(GL11.GL_ALPHA_TEST);
			                		
			                		//Draw the HUD.
			                		currentHUD.drawScreen(0, 0, event.getPartialTicks());
			                		
			                		//Disable the translating, lightmap, alpha to put it back to its old state.
			                		GL11.glPopMatrix();
			                		InterfaceRender.setInternalLightingState(false);
			                		GL11.glDisable(GL11.GL_ALPHA_TEST);
			                		
			                		//Return to prevent saved HUD from being wiped.
			                		return;
								}
							}
						}
					}
				}
			}
    	}
    	
    	//No HUD rendered, set it to null.
    	currentHUD = null;
    }
	
	/**
     * Used to force rendering of entities above the world height limit, as
     * newer versions suppress this as part of the chunk visibility
     * feature.
     */
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
        for(TileEntity tile : Minecraft.getMinecraft().world.loadedTileEntityList){
        	if(tile instanceof BuilderTileEntity){
        		Vec3d delta = new Vec3d(tile.getPos()).addVector(-playerX, -playerY, -playerZ);
        		TileEntityRendererDispatcher.instance.getRenderer(tile).render(tile, delta.x, delta.y, delta.z, event.getPartialTicks(), 0, 0);
        	}
        }
        Minecraft.getMinecraft().world.profiler.endSection();
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
							Minecraft.getMinecraft().world.profiler.startSection("iv_render_entity_" + builder.entity.lookupID);
							//If we don't have render data yet, create one now.
							if(!renderData.containsKey(builder)){
								renderData.put(builder, new RenderTickData(builder.entity.world));
							}
							
							//Get render pass.  Render data uses 2 for pass -1 as it uses arrays and arrays can't have a -1 index.
							int renderPass = InterfaceRender.getRenderPass();
							if(renderPass == -1){
								renderPass = 2;
							}
							
							//If we need to render, do so now.
							if(renderData.get(builder).shouldRender(renderPass, partialTicks)){
								builder.entity.render(partialTicks);
							}
							Minecraft.getMinecraft().world.profiler.endSection();
						}
					}
				};
			}});
				
		//Register the TESR wrapper.
		ClientRegistry.bindTileEntitySpecialRenderer(BuilderTileEntity.class, new BuilderTileEntityRender());
		
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
			}else if(field.getType().equals(BuilderBlock.class)){
				//Wrapper block item, get item from it to register.
				try{
					BuilderBlock wrapper = (BuilderBlock) field.get(null);
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
