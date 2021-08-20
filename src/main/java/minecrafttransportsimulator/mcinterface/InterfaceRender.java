package minecrafttransportsimulator.mcinterface;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.instances.EntityParticle;
import minecrafttransportsimulator.guis.components.AGUIBase.TextPosition;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.rendering.components.AModelParser;
import minecrafttransportsimulator.rendering.components.GIFParser;
import minecrafttransportsimulator.rendering.components.GIFParser.ParsedGIF;
import net.minecraft.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

/**Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, etc.
 *
 * @author don_bruce
 */
public class InterfaceRender{
	private static final Map<String, Integer> textures = new HashMap<String, Integer>();
	private static final Map<String, ParsedGIF> animatedGIFs = new HashMap<String, ParsedGIF>();
	private static String pushedTextureLocation;
	private static boolean boundSinceLastPush;
	
	/**
	 *  Caches the vertices in some form for quick rendering.  This form is version-dependent,
	 *  but no matter which version is used, the returned value is assured to be unique for each
	 *  call to this function.  This should be used in tandem with {@link #renderVertices(int)},
	 *  which will render the cached vertices from this function.  Note that the vertex format
	 *  is expected to be the same returned b {@link AModelParser#parseModel(String)}
	 */
	public static int cacheVertices(Float[][] vertices){
		int displayListIndex = GL11.glGenLists(1);
		GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Float[] vertex : vertices){
			GL11.glTexCoord2f(vertex[3], vertex[4]);
			GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
			GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
		}
		GL11.glEnd();
		GL11.glEndList();
		return displayListIndex;
	}
	
	/**
	 *  Like {@link #cacheVertices(Float[][])}, but in this takes
	 *  a list of vertex float arrays rather than a single one.
	 *  Used for caching whole models rather than individual objects.
	 */
	public static int cacheVertices(Collection<Float[][]> vertices){
		int displayListIndex = GL11.glGenLists(1);
		GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Float[][] vertexGroup : vertices){
			for(Float[] vertex : vertexGroup){
				GL11.glTexCoord2f(vertex[3], vertex[4]);
				GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
				GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
			}
		}
		GL11.glEnd();
		GL11.glEndList();
		return displayListIndex;
	}
	
	/**
	 *  Renders a set of vertices previously cached with {@link #cacheVertices(Float[][])}
	 */
	public static void renderVertices(int index){
		GL11.glCallList(index);
	}
	
	/**
	 *  Renders a set of raw vertices without any caching.
	 */
	public static void renderVertices(Float[][] vertices){
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Float[] vertex : vertices){
			GL11.glTexCoord2f(vertex[3], vertex[4]);
			GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
			GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
		}
		GL11.glEnd();
	}
	
	/**
	 *  Deletes the cached vertices with the specified index.
	 */
	public static void deleteVertices(int index){
		GL11.glDeleteLists(index, 1);
	}
	
	/**
	 *  Returns true if bounding boxes should be rendered.
	 */
	public static boolean shouldRenderBoundingBoxes(){
		return Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox();
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
		boundSinceLastPush = true;
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
		if(!textures.containsKey(textureURL) && !animatedGIFs.containsKey(textureURL)){
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
						if(contentType.endsWith("gif")){
							ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
						    ImageInputStream stream = ImageIO.createImageInputStream(url.openStream());
						    reader.setInput(stream);
						    ParsedGIF gif = GIFParser.parseGIF(reader);
						    if(gif != null){
						    	animatedGIFs.put(textureURL, gif);
						    }else{
						    	return "Could not parse GIF due to no frames being present.  Is this a real direct link or a fake one?";
						    }
						}else{
							BufferedImage bufferedimage = TextureUtil.readBufferedImage(url.openStream());
							int glTexturePointer = TextureUtil.glGenTextures();
					        TextureUtil.uploadTextureImageAllocate(glTexturePointer, bufferedimage, false, false);
					        textures.put(textureURL, glTexturePointer);
						}
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
		if(textures.containsKey(textureURL)){
			GlStateManager.bindTexture(textures.get(textureURL));
		}else{
			ParsedGIF parsedGIF = animatedGIFs.get(textureURL);
			GlStateManager.bindTexture(parsedGIF.getCurrentTextureIndex());
		}
		boundSinceLastPush = true;
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
		boundSinceLastPush = false;
	}
	
	/**
	 *  Re-binds the last saved texture.  If this texture is already bound, then no re-binding occurs.
	 */
	public static void recallTexture(){
		if(pushedTextureLocation != null && boundSinceLastPush){
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
	 *  passed-in position.
	 */
	public static void setLightingToPosition(Point3d position){
		int lightVar = Minecraft.getMinecraft().world.getCombinedLight(new BlockPos(position.x, position.y, position.z), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightVar%65536, lightVar/65536);
	}
	
	/**
	 *  Manually enables and disables blending. Do NOT use this during normal model rendering, as it
	 *  can seriously mess up states.  Only use this for simple things, like GUIs or screen overlays.
	 *  This is not reset with resetting states, so make sure to turn it back off when you're done.
	 */
	public static void setBlend(boolean enabled){
		if(enabled){
			GlStateManager.enableBlend();
		}else{
			GlStateManager.disableBlend();
		}
	}
	
	/**
	 *  Sets the blend state to bright.  This does special blending
	 *  when blending is enabled.
	 */
	public static void setBlendBright(boolean enabled){
		if(enabled){
			GlStateManager.disableAlpha();
			GlStateManager.blendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
		}else{
			GlStateManager.enableAlpha();
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
	 *  Enables or disables textures.  If textures are disabled, rendering will be
	 *  solid-color shapes.
	 */
	public static void setTextureState(boolean enabled){
		if(enabled){
			GlStateManager.enableTexture2D();
		}else{
			GlStateManager.disableTexture2D();
		}
	}
	
	/**
	 *  Resets all the rendering states.
	 *  Useful after doing a rendering routine where states may not be correct.
	 */
	//TODO move away from this.  It's a hack for bad code!
	public static void resetStates(){
		setColorState(1.0F, 1.0F, 1.0F, 1.0F);
		setLightingState(true);
	}
	
	/**
	 *  This method manually renders all riders on an entity.  Useful if you're rendering the entity manually
	 *  and the entity and its riders have been culled from rendering.
	 */
	public static void renderEntityRiders(AEntityD_Interactable<?> entity, float partialTicks){
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
	public static void spawnParticle(EntityParticle particle){
		if(Minecraft.getMinecraft().effectRenderer != null){
			Minecraft.getMinecraft().effectRenderer.addEffect(new BuilderParticle(particle));
		}
	}
	
	/**
	 *  Spawns the particles for the block at the passed-in position.
	 *  This also allows for playing the block breaking sound.
	 *  It does not actually break the block.  Such breakage must be done on the server.
	 */
	//TODO remove this on bullets (potentially LC if needed?)
	public static void spawnBlockBreakParticles(Point3d position){
		if(Minecraft.getMinecraft().effectRenderer != null){
			BlockPos pos = new BlockPos(position.x, position.y, position.z);
			if(!Minecraft.getMinecraft().world.isAirBlock(pos)){
				Minecraft.getMinecraft().effectRenderer.addBlockHitEffects(pos, EnumFacing.UP);
				SoundType soundType = Minecraft.getMinecraft().world.getBlockState(pos).getBlock().getSoundType(Minecraft.getMinecraft().world.getBlockState(pos), Minecraft.getMinecraft().player.world, pos, null);
				Minecraft.getMinecraft().world.playSound(Minecraft.getMinecraft().player, pos, soundType.getBreakSound(), SoundCategory.BLOCKS, soundType.getVolume(), soundType.getPitch());
			}
		}
	}
	
	/**
	 *  Renders all the text markings on the passed-in entity.
	 *  This should only be done in the main pass, as we don't do any alpha blending in this routine.
	 *  Return true if we rendered anything.  This lets any rendering systems reset their bound texture if required.
	 */
	public static boolean renderTextMarkings(AEntityC_Definable<?> entity, String objectRendering){
		boolean systemLightingEnabled = true;
		boolean internalLightingEnabled = true;
		for(Entry<JSONText, String> textLine : entity.text.entrySet()){
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
				if(textDefinition.lightsUp && entity.renderTextLit()){
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
				if(!textDefinition.rot.isZero()){
					GL11.glRotated(-textDefinition.rot.y, 0, 1, 0);
					GL11.glRotated(textDefinition.rot.x, 1, 0, 0);
					GL11.glRotated(textDefinition.rot.z, 0, 0, 1);
				}
				//Scale by 1/16.  This converts us from block units to pixel units, which is what the GUIs use.
				GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
				//Finally, render the text.
				String inheritedColor = entity.getSecondaryTextColor();
				String colorString = textDefinition.colorInherited && inheritedColor != null ? inheritedColor : textDefinition.color;
				InterfaceGUI.drawScaledText(text, textDefinition.fontName, 0, 0, Color.decode(colorString), TextPosition.values()[textDefinition.renderPosition], textDefinition.wrapWidth, textDefinition.scale, textDefinition.autoScale);
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
	}
}
