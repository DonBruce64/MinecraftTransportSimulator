package mcinterface1165;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IInterfaceRender;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.GIFParser;
import minecrafttransportsimulator.rendering.GIFParser.GIFImageFrame;
import minecrafttransportsimulator.rendering.GIFParser.ParsedGIF;
import minecrafttransportsimulator.rendering.RenderableObject;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

/**
 * Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, etc.
 *
 * @author don_bruce
 */
public class InterfaceRender implements IInterfaceRender {
    private static final DoubleBuffer buffer = ByteBuffer.allocateDirect(16 * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
    private static final Map<String, ResourceLocation> internalTextures = new HashMap<>();
    private static final Map<String, Integer> onlineTextures = new HashMap<>();
    private static final Map<String, ParsedGIF> animatedGIFs = new HashMap<>();
    private static final Map<ParsedGIF, Map<GIFImageFrame, Integer>> animatedGIFFrames = new LinkedHashMap<>();
    private static final Map<IWrapperItemStack, TransformationMatrix> stacksToRender = new LinkedHashMap<>();
    private static float lastLightmapX;
    private static float lastLightmapY;
    private static final ResourceLocation MISSING_TEXTURE = new ResourceLocation("mts:textures/rendering/missing.png");

    //Copied from ParticleManager as it's not accessable.
    private static final ResourceLocation PARTICLE_TEXTURES = new ResourceLocation("textures/particle/particles.png");

    @Override
    public float[] getBlockBreakTexture(AWrapperWorld world, Point3D position) {
        //Get normal model.
        IBlockState state = ((WrapperWorld) world).world.getBlockState(new BlockPos(position.x, position.y, position.z));
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getTexture(state);
        return new float[]{sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV()};
    }

    @Override
    public float[] getDefaultBlockTexture(String name) {
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getTextureMap().getAtlasSprite(name.replace(":", ":blocks/"));
        return new float[]{sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV()};
    }

    @Override
    public void renderItemModel(IWrapperItemStack stack, TransformationMatrix transform) {
        stacksToRender.put(stack, transform);
    }

    /**
     * Does the actual stack render.  Put into a batch at the end of GUI rendering as item
     * stack rendering changes the OpenGL state and can muck up normal rendering.
     */
    protected void renderAllStacks() {
        for (Entry<IWrapperItemStack, TransformationMatrix> stackEntry : stacksToRender.entrySet()) {
            GL11.glPushMatrix();
            setInternalLightingState(false);

            //Apply existing transform.
            applyTransformOpenGL(stackEntry.getValue(), false);

            //Need to translate back to pre-undo the renderer offset.
            float offset = 100.0F + Minecraft.getMinecraft().getRenderItem().zLevel;
            GL11.glTranslated(0, 0, -offset);

            //Now invert y-axis scaling to account for GUI scaling differences.
            GL11.glScalef(1, -1, 1);

            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(((WrapperItemStack) stackEntry.getKey()).stack, 0, 0);
            setInternalLightingState(true);
            GL11.glPopMatrix();
        }
        stacksToRender.clear();
    }

    @Override
    public void renderVertices(RenderableObject object) {
        if (object.disableLighting) {
            setLightingState(false);
        }
        if (object.ignoreWorldShading) {
            setSystemLightingState(false);
        }
        if (object.enableBrightBlending) {
            setBlendBright(true);
        }
        if (object.texture != null) {
            bindTexture(object.texture);
        } else {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }
        setColorState(object.color, object.alpha);

        GL11.glPushMatrix();
        applyTransformOpenGL(object.transform, false);
        if (object.cacheVertices) {
            if (object.cachedVertexIndex == -1) {
                object.cachedVertexIndex = cacheVertices(object.vertices);
                object.vertices = null;
            }
            renderVertices(object.cachedVertexIndex);
        } else if (object.lineWidth != 0) {
            renderLines(object.vertices, object.lineWidth);
        } else {
            renderVertices(object.vertices);
        }
        GL11.glPopMatrix();

        if (object.texture == null) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
        if (object.disableLighting || object.ignoreWorldShading) {
            setLightingState(true);
        }
        if (object.enableBrightBlending) {
            setBlendBright(false);
        }
    }

    @Override
    public void applyTransformOpenGL(TransformationMatrix matrix, boolean inverted) {
        buffer.clear();
        if (inverted) {
            buffer.put(matrix.m00);
            buffer.put(matrix.m01);
            buffer.put(matrix.m02);
            buffer.put(matrix.m30);
            buffer.put(matrix.m10);
            buffer.put(matrix.m11);
            buffer.put(matrix.m12);
            buffer.put(matrix.m31);
            buffer.put(matrix.m20);
            buffer.put(matrix.m21);
            buffer.put(matrix.m22);
            buffer.put(matrix.m32);
            buffer.put(-matrix.m03);
            buffer.put(-matrix.m13);
            buffer.put(-matrix.m23);
            buffer.put(matrix.m33);
        } else {
            buffer.put(matrix.m00);
            buffer.put(matrix.m10);
            buffer.put(matrix.m20);
            buffer.put(matrix.m30);
            buffer.put(matrix.m01);
            buffer.put(matrix.m11);
            buffer.put(matrix.m21);
            buffer.put(matrix.m31);
            buffer.put(matrix.m02);
            buffer.put(matrix.m12);
            buffer.put(matrix.m22);
            buffer.put(matrix.m32);
            buffer.put(matrix.m03);
            buffer.put(matrix.m13);
            buffer.put(matrix.m23);
            buffer.put(matrix.m33);
        }
        buffer.flip();
        GL11.glMultMatrix(buffer);
    }

    @Override
    public void deleteVertices(RenderableObject object) {
        GL11.glDeleteLists(object.cachedVertexIndex, 1);
    }

    /**
     * Renders a set of raw vertices without any caching.
     */
    private static void renderVertices(FloatBuffer vertices) {
        GL11.glBegin(GL11.GL_TRIANGLES);
        while (vertices.hasRemaining()) {
            GL11.glNormal3f(vertices.get(), vertices.get(), vertices.get());
            GL11.glTexCoord2f(vertices.get(), vertices.get());
            GL11.glVertex3f(vertices.get(), vertices.get(), vertices.get());
        }
        GL11.glEnd();
        //Rewind buffer for next read.
        vertices.rewind();
    }

    /**
     * Renders a set of vertices previously cached with {@link #cacheVertices(FloatBuffer)}
     */
    private static void renderVertices(int index) {
        GL11.glCallList(index);
    }

    /**
     * Renders a set of raw lines without any caching.
     */
    private static void renderLines(FloatBuffer vertices, float width) {
        GL11.glLineWidth(width);
        GL11.glBegin(GL11.GL_LINES);
        while (vertices.hasRemaining()) {
            GL11.glVertex3f(vertices.get(), vertices.get(), vertices.get());
        }
        GL11.glEnd();
        //Rewind buffer for next read.
        vertices.rewind();
        GL11.glLineWidth(1);
    }

    /**
     * Caches the vertices in some form for quick rendering.  This form is version-dependent,
     * but no matter which version is used, the returned value is assured to be unique for each
     * call to this function.  This should be used in tandem with {@link #renderVertices(int)},
     * which will render the cached vertices from this function.  Note that the vertex format
     * is expected to be the same as what is in {@link RenderableObject}
     */
    private static int cacheVertices(FloatBuffer vertices) {
        int displayListIndex = GL11.glGenLists(1);
        GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
        renderVertices(vertices);
        GL11.glEndList();
        return displayListIndex;
    }

    /**
     * Binds the passed-in texture to be rendered.  The instance of the texture is
     * cached in this class once created for later use, so feel free to not cache
     * the string values that are passed-in.
     */
    private static void bindTexture(String textureLocation) {
        if (animatedGIFs.containsKey(textureLocation)) {
            //Special case for GIFs.
            ParsedGIF parsedGIF = animatedGIFs.get(textureLocation);
            GlStateManager.bindTexture(animatedGIFFrames.get(parsedGIF).get(parsedGIF.getCurrentFrame()));
        } else if (onlineTextures.containsKey(textureLocation)) {
            //Online texture.
            GlStateManager.bindTexture(onlineTextures.get(textureLocation));
        } else if (textureLocation.equals(RenderableObject.GLOBAL_TEXTURE_NAME)) {
            //Default texture.
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        } else if (textureLocation.equals(RenderableObject.PARTICLE_TEXTURE_NAME)) {
            //Particle texture.
            Minecraft.getMinecraft().getTextureManager().bindTexture(PARTICLE_TEXTURES);
        } else {
            //Parse texture if we don't have it yet.
            if (!internalTextures.containsKey(textureLocation)) {
                //If the texture has a colon, it's a short-hand form that needs to be converted.
                String formattedLocation = textureLocation;
                if (textureLocation.contains(":")) {
                    formattedLocation = "/assets/" + textureLocation.replace(":", "/");
                }

                //Check if the texture exists.
                if (InterfaceRender.class.getResource(formattedLocation) != null) {
                    //Convert the classpath-location to a domain-location path for MC.
                    String domain = formattedLocation.substring("/assets/".length(), formattedLocation.indexOf("/", "/assets/".length()));
                    String location = formattedLocation.substring("/assets/".length() + domain.length() + 1);
                    internalTextures.put(textureLocation, new ResourceLocation(domain, location));
                } else {
                    InterfaceManager.coreInterface.logError("Could not find texture: " + formattedLocation + " Reverting to fallback texture.");
                    internalTextures.put(textureLocation, MISSING_TEXTURE);
                }
            }
            Minecraft.getMinecraft().getTextureManager().bindTexture(internalTextures.get(textureLocation));
        }
    }

    @Override
    public String downloadURLTexture(String textureURL) {
        if (!onlineTextures.containsKey(textureURL) && !animatedGIFs.containsKey(textureURL)) {
            //Parse the texture, get the OpenGL integer that represents this texture, and save it.
            //FAR less jank than using MC's resource system.
            try {
                URL url = new URL(textureURL);
                URLConnection connection = url.openConnection();
                try {
                    List<String> validContentTypes = new ArrayList<>();
                    for (String imageSuffix : ImageIO.getReaderFileSuffixes()) {
                        validContentTypes.add("image/" + imageSuffix);
                    }
                    String contentType = connection.getHeaderField("Content-Type");
                    if (validContentTypes.contains(contentType)) {
                        if (contentType.endsWith("gif")) {
                            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
                            ImageInputStream stream = ImageIO.createImageInputStream(url.openStream());
                            reader.setInput(stream);
                            ParsedGIF gif = GIFParser.parseGIF(reader);
                            if (gif != null) {
                                animatedGIFs.put(textureURL, gif);
                                Map<GIFImageFrame, Integer> gifFrameIndexes = new HashMap<>();
                                for (GIFImageFrame frame : gif.frames.values()) {
                                    int glTexturePointer = TextureUtil.glGenTextures();
                                    TextureUtil.uploadTextureImageAllocate(glTexturePointer, frame.getImage(), false, false);
                                    gifFrameIndexes.put(frame, glTexturePointer);
                                }
                                animatedGIFFrames.put(gif, gifFrameIndexes);
                            } else {
                                return "Could not parse GIF due to no frames being present.  Is this a real direct link or a fake one?";
                            }
                        } else {
                            BufferedImage bufferedimage = TextureUtil.readBufferedImage(url.openStream());
                            int glTexturePointer = TextureUtil.glGenTextures();
                            TextureUtil.uploadTextureImageAllocate(glTexturePointer, bufferedimage, false, false);
                            onlineTextures.put(textureURL, glTexturePointer);
                        }
                    } else {
                        StringBuilder errorString = new StringBuilder("Invalid content type found.  Found:" + contentType + ", but the only valid types are: ");
                        for (String validType : validContentTypes) {
                            errorString.append(validType).append(", ");
                        }
                        onlineTextures.put(textureURL, TextureUtil.MISSING_TEXTURE.getGlTextureId());
                        return errorString.toString();
                    }
                } catch (Exception e) {
                    onlineTextures.put(textureURL, TextureUtil.MISSING_TEXTURE.getGlTextureId());
                    e.printStackTrace();
                    return "Could not parse images.  Error was: " + e.getMessage();
                }
            } catch (Exception e) {
                onlineTextures.put(textureURL, TextureUtil.MISSING_TEXTURE.getGlTextureId());
                e.printStackTrace();
                return "Could not open URL for processing.  Error was: " + e.getMessage();
            }
        }
        return null;
    }

    @Override
    public void setLightingState(boolean enabled) {
        setSystemLightingState(enabled);
        setInternalLightingState(enabled);
    }

    /**
     * Enables or disables OpenGL lighting for this draw sequence.
     * This effectively prevents OpenGL lighting calculations on textures.
     * Do note that the normal internal lightmapping will still be applied.
     * This essentially prevents shadow creation on models based on their face
     * orientation relative to the main light "source".
     */
    private static void setSystemLightingState(boolean enabled) {
        if (enabled) {
            GlStateManager.enableLighting();
        } else {
            GlStateManager.disableLighting();
        }
    }

    /**
     * Enables or disables internal lighting for this draw sequence.
     * This disables the internal lightmapping, effectively making the rendered
     * texture as bright as it would be during daytime.  Do note that the system
     * lighting calculations for shadowing will still be applied to the model.
     */
    private static void setInternalLightingState(boolean enabled) {
        if (enabled) {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastLightmapX, lastLightmapY);
        } else {
            lastLightmapX = OpenGlHelper.lastBrightnessX;
            lastLightmapY = OpenGlHelper.lastBrightnessY;
            int lightVar = (15 << 20) | (15 << 4);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightVar % 65536, lightVar / 65536);
        }
    }

    @Override
    public void setLightingToPosition(Point3D position) {
        //Get lighting 1 block above position, as actual position will result in blocked light.
        int lightVar = Minecraft.getMinecraft().world.getCombinedLight(new BlockPos(position.x, position.y + 1, position.z), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightVar % 65536, lightVar / 65536);
    }

    @Override
    public void setBlend(boolean enabled) {
        if (enabled) {
            GlStateManager.enableBlend();
        } else {
            GlStateManager.disableBlend();
        }
    }

    /**
     * Sets the blend state to bright.  This does special blending
     * when blending is enabled.
     */
    private static void setBlendBright(boolean enabled) {
        if (enabled) {
            GlStateManager.blendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
        } else {
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    /**
     * Sets MC color to the passed-in color and alpha.  Required when needing to keep MC states happy.
     * In particular, this is needed if colors are changed during MC internal draw calls,
     * such as rendering a string, changing the color, and then rendering another string.
     */
    private static void setColorState(ColorRGB color, float alpha) {
        GlStateManager.color(color.red, color.green, color.blue, alpha);
    }

    @Override
    public boolean shouldRenderBoundingBoxes() {
        return Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox();
    }
}
