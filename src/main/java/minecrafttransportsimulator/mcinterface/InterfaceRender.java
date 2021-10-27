package minecrafttransportsimulator.mcinterface;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.entities.instances.EntityParticle;
import minecrafttransportsimulator.rendering.components.GIFParser;
import minecrafttransportsimulator.rendering.components.GIFParser.ParsedGIF;
import minecrafttransportsimulator.rendering.components.RenderableObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/**Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, etc.
 *
 * @author don_bruce
 */
public class InterfaceRender{
	private static final Map<String, Integer> textures = new HashMap<String, Integer>();
	private static final Map<String, ParsedGIF> animatedGIFs = new HashMap<String, ParsedGIF>();
	private static float lastLightmapX;
	private static float lastLightmapY;
	
	/**
	 *  Renders the vertices stored in the passed-in {@link RenderableObject}.
	 *  If the vertices should be cached per {@link RenderableObject#cacheVertices},
	 *  then they are done so and a pointer-index is stored into {@link RenderableObject#cachedVertexIndex}.
	 *  {@link RenderableObject#vertices} is then set to null to free memory.
	 *  If the object is ever deleted, then {@link #deleteVertices(RenderableObject)}
	 *  should be called to free up the respective GPU memory.
	 */
	public static void renderVertices(RenderableObject object){
		if(object.disableLighting){
			setLightingState(false);
		}
		if(object.enableBrightBlending){
			setBlendBright(true);
		}
		if(object.texture != null){
			bindTexture(object.texture);
		}else{
			setTextureState(false);
		}
		setColorState(object.color, object.alpha);
		
		GL11.glPushMatrix();
		if(object.isMirrored){
			GL11.glScalef(-object.scale, object.scale, object.scale);
			GL11.glCullFace(GL11.GL_FRONT);
		}else{
			GL11.glScalef(object.scale, object.scale, object.scale);
		}
		if(object.cacheVertices){
			if(object.cachedVertexIndex == -1){
				object.cachedVertexIndex = cacheVertices(object.vertices);
				object.vertices = null;
			}
			renderVertices(object.cachedVertexIndex);
		}else if(object.lineWidth != 0){
			renderLines(object.vertices, object.lineWidth);
		}else{
			renderVertices(object.vertices);
		}
		if(object.isMirrored){
			GL11.glCullFace(GL11.GL_BACK);
		}
		GL11.glPopMatrix();
		
		
		if(object.disableLighting){
			setLightingState(true);
		}
		if(object.enableBrightBlending){
			setBlendBright(false);
		}
		if(object.texture == null){
			setTextureState(true);
		}
	}
	
	/**
	 *  Deletes the cached vertices associated with the specified {@link RenderableObject}.
	 */
	public static void deleteVertices(RenderableObject object){
		GL11.glDeleteLists(object.cachedVertexIndex, 1);
	}
	
	/**
	 *  Renders a set of raw vertices without any caching.
	 */
	private static void renderVertices(FloatBuffer vertices){
		GL11.glBegin(GL11.GL_TRIANGLES);
		while(vertices.hasRemaining()){
			GL11.glNormal3f(vertices.get(), vertices.get(), vertices.get());
			GL11.glTexCoord2f(vertices.get(), vertices.get());
			GL11.glVertex3f(vertices.get(), vertices.get(), vertices.get());
		}
		GL11.glEnd();
		//Rewind buffer for next read.
		vertices.rewind();
	}
	
	/**
	 *  Renders a set of vertices previously cached with {@link #cacheVertices(FloatBuffer)}
	 */
	private static void renderVertices(int index){
		GL11.glCallList(index);
	}
	
	/**
	 *  Renders a set of raw lines without any caching.
	 */
	private static void renderLines(FloatBuffer vertices, float width){
		GL11.glLineWidth(width);
		GL11.glBegin(GL11.GL_LINES);
		while(vertices.hasRemaining()){
			GL11.glVertex3f(vertices.get(), vertices.get(), vertices.get());
		}
		GL11.glEnd();
		//Rewind buffer for next read.
		vertices.rewind();
		GL11.glLineWidth(1);
	}
	
	/**
	 *  Caches the vertices in some form for quick rendering.  This form is version-dependent,
	 *  but no matter which version is used, the returned value is assured to be unique for each
	 *  call to this function.  This should be used in tandem with {@link #renderVertices(int)},
	 *  which will render the cached vertices from this function.  Note that the vertex format
	 *  is expected to be the same as what is in {@link RenderableObject}
	 */
	private static int cacheVertices(FloatBuffer vertices){
		int displayListIndex = GL11.glGenLists(1);
		GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
		renderVertices(vertices);
		GL11.glEndList();
		return displayListIndex;
	}
	
	/**
	 *  Binds the passed-in texture to be rendered.  The instance of the texture is 
	 *  cached in this class once created for later use, so feel free to not cache
	 *  the string values that are passed-in.
	 */
	public static void bindTexture(String textureLocation){
		//Special case for GIFs.
		if(animatedGIFs.containsKey(textureLocation)){
			ParsedGIF parsedGIF = animatedGIFs.get(textureLocation);
			GlStateManager.bindTexture(parsedGIF.getCurrentTextureIndex());
		}else{
			//Parse texture if we don't have it yet.
			if(!textures.containsKey(textureLocation)){
				//If the texture has a colon, it's a short-hand form that needs to be converted.
				String formattedLocation = textureLocation;
				if(textureLocation.indexOf(":") != -1){
					formattedLocation = "/assets/" + textureLocation.replace(":", "/");
				}
				
				//Parse the texture, get the OpenGL integer that represents this texture, and save it.
				//FAR less jank than using MC's resource system.
				try{
					BufferedImage bufferedimage = TextureUtil.readBufferedImage(InterfaceRender.class.getResourceAsStream(formattedLocation));
					int glTexturePointer = TextureUtil.glGenTextures();
			        TextureUtil.uploadTextureImageAllocate(glTexturePointer, bufferedimage, false, false);
			        textures.put(textureLocation, glTexturePointer);
				}catch(Exception e){
					InterfaceCore.logError("Could not find texture: " + formattedLocation + " Reverting to fallback texture.");
					textures.put(textureLocation, TextureUtil.MISSING_TEXTURE.getGlTextureId());
				}
			}
			GlStateManager.bindTexture(textures.get(textureLocation));
		}
	}
	
	/**
	 *  Downloads the passed-in texture to be parsed and bound.  The texture is downloaded from the
	 *  URL and then added to the texture rendering system.  The integer of the The instance 
	 *  of the texture is cached in this class once created for later use, so feel free to not 
	 *  cache the string URL that is passed-in.  If the texture downloading was successful, null is
	 *  returned.  Otherwise, an error message is returned.  Bind the downloaded texture by calling
	 *  {@link #bindTexture(String)} with the passed-in URL.
	 */
	public static String downloadURLTexture(String textureURL){
		if(!textures.containsKey(textureURL) && !animatedGIFs.containsKey(textureURL)){
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
		return null;
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
	private static void setSystemLightingState(boolean enabled){
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
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastLightmapX, lastLightmapY);
		}else{
			lastLightmapX = OpenGlHelper.lastBrightnessX;
			lastLightmapY = OpenGlHelper.lastBrightnessY;
			int lightVar = (15 << 20) | (15 << 4);
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightVar%65536, lightVar/65536);
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
	private static void setBlendBright(boolean enabled){
		if(enabled){
			GlStateManager.disableAlpha();
			GlStateManager.blendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
		}else{
			GlStateManager.enableAlpha();
			GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		}
	}
	
	/**
	 *  Sets MC color to the passed-in color and alpha.  Required when needing to keep MC states happy.
	 *  In particular, this is needed if colors are changed during MC internal draw calls,
	 *  such as rendering a string, changing the color, and then rendering another string.
	 */
	private static void setColorState(ColorRGB color, float alpha){
		GlStateManager.color(color.red, color.green, color.blue, alpha);
	}
	
	/**
	 *  Enables or disables textures.  If textures are disabled, rendering will be
	 *  solid-color shapes.
	 */
	private static void setTextureState(boolean enabled){
		if(enabled){
			GlStateManager.enableTexture2D();
		}else{
			GlStateManager.disableTexture2D();
		}
	}
	
	/**
	 *  Returns true if bounding boxes should be rendered.
	 */
	public static boolean shouldRenderBoundingBoxes(){
		return Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox();
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
}
