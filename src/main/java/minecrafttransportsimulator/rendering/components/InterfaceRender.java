package minecrafttransportsimulator.rendering.components;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.guis.components.AGUIBase.TextPosition;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.BuilderParticle;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import net.minecraft.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.MinecraftForgeClient;

/**Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, etc.
 *
 * @author don_bruce
 */
public class InterfaceRender{
	private static final Map<String, Integer> textures = new HashMap<String, Integer>();
	private static String pushedTextureLocation;
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
				InterfaceCore.logError("Could not find texture: " + textureLocation + " Reverting to fallback texture.");
				textures.put(textureLocation, TextureUtil.MISSING_TEXTURE.getGlTextureId());
			}
		}
		GlStateManager.bindTexture(textures.get(textureLocation));
	}
	
	/**
	 *  Binds the passed-in texture to be rendered.  The texture is downloaded from the
	 *  URL and then added to the texture rendering system.  The integer of the The instance 
	 *  of the texture is  cached in this class once created for later use, so feel free to not 
	 *  cache the string URL that is passed-in.  If the texture binding was successful, null is
	 *  returned.  Otherwise, an error message is returned.
	 */
	public static String bindURLTexture(String textureURL){
		//Bind texture if we have it.
		if(!textures.containsKey(textureURL)){
			//Don't have this texture created yet.  Do so now.
			//Parse the texture, get the OpenGL integer that represents this texture, and save it.
			//FAR less jank than using MC's resource system.
			try{
				URL url = new URL(textureURL);
				URLConnection connection = url.openConnection();
				try{
					List<String> validContentTypes = new ArrayList<String>();
					for(String imageSuffix : ImageIO.getReaderFileSuffixes()){
						validContentTypes.add("image/" + imageSuffix);
					}
					String contentType = connection.getHeaderField("Content-Type");
					if(validContentTypes.contains(contentType)){
						BufferedImage bufferedimage = TextureUtil.readBufferedImage(url.openStream());
						int glTexturePointer = TextureUtil.glGenTextures();
				        TextureUtil.uploadTextureImageAllocate(glTexturePointer, bufferedimage, false, false);
				        textures.put(textureURL, glTexturePointer);
					}else{
						String errorString = "Invalid content type found.  Found:" + contentType + ", but the only valid types are: ";
						for(String validType : validContentTypes){
							errorString += validType + ", ";
						}
						textures.put(textureURL, TextureUtil.MISSING_TEXTURE.getGlTextureId());
						return errorString;
					}
				}catch(Exception e){
					textures.put(textureURL, TextureUtil.MISSING_TEXTURE.getGlTextureId());
					e.printStackTrace();
					return "Could not parse images.  Error was: " + e.getMessage();
				}
			}catch(Exception e){
				textures.put(textureURL, TextureUtil.MISSING_TEXTURE.getGlTextureId());
				e.printStackTrace();
				return "Could not open URL for processing.  Error was: " + e.getMessage();
			}
		}
		GlStateManager.bindTexture(textures.get(textureURL));
		return null;
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
}
