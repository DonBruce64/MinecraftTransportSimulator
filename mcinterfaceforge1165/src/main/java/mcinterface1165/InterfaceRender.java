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
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IInterfaceRender;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.GIFParser;
import minecrafttransportsimulator.rendering.GIFParser.GIFImageFrame;
import minecrafttransportsimulator.rendering.GIFParser.ParsedGIF;
import minecrafttransportsimulator.rendering.RenderableObject;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.world.LightType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, etc.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Dist.CLIENT)
public class InterfaceRender implements IInterfaceRender {
    private static final DoubleBuffer buffer = ByteBuffer.allocateDirect(16 * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
    private static final Map<String, ResourceLocation> internalTextures = new HashMap<>();
    private static final Map<String, Integer> onlineTextures = new HashMap<>();
    private static final Map<String, ParsedGIF> animatedGIFs = new HashMap<>();
    private static final Map<ParsedGIF, Map<GIFImageFrame, Integer>> animatedGIFFrames = new LinkedHashMap<>();
    private static final Map<IWrapperItemStack, Point3D> stacksToRender = new LinkedHashMap<>();
    private static int currentPackedLight;
    private static float lastLightmapX;
    private static float lastLightmapY;
    private static final ResourceLocation MISSING_TEXTURE = new ResourceLocation("mts:textures/rendering/missing.png");
    private static MatrixStack matrixStack;
    private static IRenderTypeBuffer renderBuffer;
    private static float[] matrixConvertArray = new float[16];

    @Override
    public float[] getBlockBreakTexture(AWrapperWorld world, Point3D position) {
        //Get normal model.
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        BlockState state = ((WrapperWorld) world).world.getBlockState(pos);
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getTexture(state, ((WrapperWorld) world).world, pos);
        return new float[] { sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1() };
    }

    @SuppressWarnings("deprecation")
    @Override
    public float[] getDefaultBlockTexture(String name) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getModelManager().getAtlas(AtlasTexture.LOCATION_BLOCKS).getSprite(new ResourceLocation(name.replace(":", ":blocks/")));
        return new float[] { sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1() };
    }

    @Override
    public void renderItemModel(IWrapperItemStack stack, Point3D translation) {
        stacksToRender.put(stack, translation);
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

        GL11.glPushMatrix();
        applyTransformOpenGL(object.transform);
        if (object.cacheVertices) {
            if (object.cachedVertexIndex == -1) {
                object.cachedVertexIndex = cacheVertices(object.vertices);
                object.vertices = null;
            }
            GL11.glCallList(object.cachedVertexIndex);
        } else if (object.isLines) {
            IVertexBuilder buffer = renderBuffer.getBuffer(RenderType.lines());
            Matrix4f matrix = convertMatrix(object.transform);
            while (object.vertices.hasRemaining()) {
                buffer.vertex(matrix, object.vertices.get(), object.vertices.get(), object.vertices.get());
                buffer.color(object.color.red, object.color.green, object.color.blue, object.alpha);
            }
            //Rewind buffer for next read.
            object.vertices.rewind();
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
    public void deleteVertices(RenderableObject object) {
        GL11.glDeleteLists(object.cachedVertexIndex, 1);
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
    public void setLightingToPosition(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        currentPackedLight = LightTexture.pack(Minecraft.getInstance().level.getBrightness(LightType.SKY, pos), Minecraft.getInstance().level.getBrightness(LightType.BLOCK, pos));
    }

    @Override
    public boolean shouldRenderBoundingBoxes() {
        return Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes();
    }

    /**
     * Renders a set of raw vertices without any caching.
     */
    private static void renderVertices(FloatBuffer vertices, ColorRGB color) {

        GL11.glBegin(GL11.GL_TRIANGLES);
        while (vertices.hasRemaining()) {
            GL11.glNormal3f(vertices.get(), vertices.get(), vertices.get());
            GL11.glTexCoord2f(vertices.get(), vertices.get());
            GL11.glVertex3f(vertices.get(), vertices.get(), vertices.get());
            GlStateManager.color(object.color.red, object.color.green, object.color.blue, object.alpha);
        }
        GL11.glEnd();
        //Rewind buffer for next read.
        vertices.rewind();
    }

    /**
     * Caches the vertices in some form for quick rendering.  This form is version-dependent,
     * but no matter which version is used, the returned value is assured to be unique for each
     * call to this function.  Note that the vertex format is expected to be the same as what 
     * is in {@link RenderableObject}
     */
    private static int cacheVertices(FloatBuffer vertices) {
        int displayListIndex = GL11.glGenLists(1);
        GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
        renderVertices(vertices);
        GL11.glEndList();
        return displayListIndex;
    }

    /**
     * Applies an OpenGL transform to the current pipeline based on the
     * passed-in matrix.
     */
    protected static void applyTransformOpenGL(TransformationMatrix matrix) {
        buffer.clear();
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
        buffer.flip();
        GL11.glMultMatrix(buffer);
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

    /**
     * Renders the main GUI, setting up any transforms or operations as required.
     */
    protected static void renderGUI(int mouseX, int mouseY, int screenWidth, int screenHeight, float partialTicks, boolean updateGUIs) {
        //Render GUIs, re-creating their components if needed.
        //Set Y-axis to inverted to have correct orientation.
        GL11.glScalef(1.0F, -1.0F, 1.0F);

        //Enable alpha testing.  This can be disabled by mods doing bad state management during their event calls.
        //We don't want to enable blending though, as that's on-demand.
        //Just in case it is enabled, however, disable it.
        //This ensures the blending state is as it will be for the main rendering pass of -1.
        InterfaceRender.setBlend(false);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        //Enable lighting.
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().entityRenderer.enableLightmap();
        setLightingState(true);

        //Render main pass, then blended pass.
        int displayGUIIndex = 0;
        for (AGUIBase gui : AGUIBase.activeGUIs) {
            if (updateGUIs || gui.components.isEmpty()) {
                gui.setupComponentsInit(screenWidth, screenHeight);
            }
            GL11.glPushMatrix();
            if (gui.capturesPlayer()) {
                //Translate in front of the main GUI components.
                GL11.glTranslated(0, 0, 250);
            } else {
                //Translate far enough to render behind the chat window.
                GL11.glTranslated(0, 0, -500 + 250 * displayGUIIndex++);
            }
            gui.render(mouseX, mouseY, false, partialTicks);

            //Render all stacks.  These have to be in the standard GUI reference frame or they won't render.
            GL11.glScalef(1.0F, -1.0F, 1.0F);

            //FIXME we probably don't need to change lighting here anymore, if not, delete.
            //setInternalLightingState(false);
            for (Entry<IWrapperItemStack, Point3D> stackEntry : stacksToRender.entrySet()) {
                //Apply existing transform.
                //Need to translate the z-offset to our value, which includes a -100 for the default added value.
                Point3D translation = stackEntry.getValue();
                float zOffset = Minecraft.getInstance().getItemRenderer().blitOffset;
                Minecraft.getInstance().getItemRenderer().blitOffset = (float) translation.z - 100;
                Minecraft.getInstance().getItemRenderer().renderGuiItem(((WrapperItemStack) stackEntry.getKey()).stack, (int) translation.x, (int) -translation.y);
                Minecraft.getInstance().getItemRenderer().blitOffset = zOffset;
            }
            stacksToRender.clear();
            //setInternalLightingState(true);

            GL11.glPopMatrix();
        }
        displayGUIIndex = 0;
        setBlend(true);
        for (AGUIBase gui : AGUIBase.activeGUIs) {
            GL11.glPushMatrix();
            if (gui.capturesPlayer()) {
                //Translate in front of the main GUI components.
                GL11.glTranslated(0, 0, 250);
            } else {
                //Translate far enough to render behind the chat window.
                GL11.glTranslated(0, 0, -500 + 250 * displayGUIIndex++);
            }
            gui.render(mouseX, mouseY, true, partialTicks);
            GL11.glPopMatrix();
        }

        //Set state back to normal.
        setLightingState(false);
        Minecraft.getMinecraft().entityRenderer.disableLightmap();
        RenderHelper.disableStandardItemLighting();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glScalef(1.0F, -1.0F, 1.0F);
    }

    /**
     * Manually renders all entities, in case {@link BuilderEntityRenderForwarder} is not rendered.
     */
    protected static void doManualEntityRender(float partialTicks) {
        //Enable lighting as pass -1 has that disabled.
        RenderHelper.enableStandardItemLighting();
        setLightingState(true);

        //Render pass 0 and 1 here manually.
        for (int pass = 0; pass < 2; ++pass) {
            if (pass == 1) {
                setBlend(true);
                GlStateManager.depthMask(false);
            }

            if (BuilderEntityRenderForwarder.lastClientInstance != null) {
                Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(BuilderEntityRenderForwarder.lastClientInstance).doRender(BuilderEntityRenderForwarder.lastClientInstance, 0, 0, 0, 0, partialTicks);
            }

            if (pass == 1) {
                setBlend(false);
                GlStateManager.depthMask(true);
            }
        }

        //Turn lighting back off.
        RenderHelper.disableStandardItemLighting();
        setLightingState(false);
    }

    /**
     * Helper method to completely disable or enable lighting.
     * This disables both the system lighting and internal lighting.
     */
    private static void setLightingState(boolean enabled) {
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

    /**
     * Manually enables and disables blending. Do NOT use this during normal model rendering, as it
     * can seriously mess up states.  Only use this for simple things, like GUIs or screen overlays.
     * This is not reset with resetting states, so make sure to turn it back off when you're done.
     */
    private static void setBlend(boolean enabled) {
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
     * Converts internal matrix to new external matrix.
     */
    private static Matrix4f convertMatrix(TransformationMatrix transform) {
        matrixConvertArray[0] = (float) transform.m00;
        matrixConvertArray[1] = (float) transform.m01;
        matrixConvertArray[2] = (float) transform.m02;
        matrixConvertArray[3] = (float) transform.m03;
        matrixConvertArray[4] = (float) transform.m10;
        matrixConvertArray[5] = (float) transform.m11;
        matrixConvertArray[6] = (float) transform.m12;
        matrixConvertArray[7] = (float) transform.m13;
        matrixConvertArray[8] = (float) transform.m20;
        matrixConvertArray[9] = (float) transform.m21;
        matrixConvertArray[10] = (float) transform.m22;
        matrixConvertArray[11] = (float) transform.m23;
        matrixConvertArray[12] = (float) transform.m30;
        matrixConvertArray[13] = (float) transform.m31;
        matrixConvertArray[14] = (float) transform.m32;
        matrixConvertArray[15] = (float) transform.m33;
        return new Matrix4f(matrixConvertArray);
    }

    /**
     * Event that's called to setup the client.  We register our render wrapper
     * class here.
     */
    @SubscribeEvent
    public static void registerRenderer(FMLClientSetupEvent event) {
        //Register the global entity rendering class.
        RenderingRegistry.registerEntityRenderingHandler(BuilderEntityRenderForwarder.E_TYPE4, manager -> new EntityRenderer<BuilderEntityRenderForwarder>(manager) {
            @Override
            public ResourceLocation getTextureLocation(BuilderEntityRenderForwarder builder) {
                return null;
            }

            @Override
            public boolean shouldRender(BuilderEntityRenderForwarder builder, ClippingHelper camera, double camX, double camY, double camZ) {
                //Always render the forwarder, no matter where the camera is.
                return true;
            }

            @Override
            public void render(BuilderEntityRenderForwarder builder, float entityYaw, float partialTicks, MatrixStack stack, IRenderTypeBuffer buffer, int packedLight) {
                //Get all entities in the world, and render them manually for this one builder.
                //Only do this if the player the builder is following is the client player.
                WrapperWorld world = WrapperWorld.getWrapperFor(builder.level);
                if (Minecraft.getInstance().player.equals(builder.playerFollowing)) {
                    ConcurrentLinkedQueue<AEntityC_Renderable> allEntities = world.renderableEntities;
                    if (allEntities != null) {
                        matrixStack = stack;
                        renderBuffer = buffer;
                        buffer.getBuffer(RenderType.solid());

                        //Use smooth shading for model rendering.
                        GL11.glShadeModel(GL11.GL_SMOOTH);

                        //Enable normal re-scaling for model rendering.
                        //This prevents bad lighting.
                        GlStateManager.enableRescaleNormal();

                        //Start master profiling section and run entity rendering routines.
                        for (AEntityC_Renderable entity : allEntities) {
                            world.beginProfiling("MTSRendering", true);
                            entity.render(false, partialTicks);
                            //Disable alpha testing on blended pass as it discards transparent fragments.
                            GlStateManager.disableAlpha();
                            entity.render(true, partialTicks);
                            GlStateManager.enableAlpha();
                            world.endProfiling();
                        }

                        //Reset states.
                        GL11.glShadeModel(GL11.GL_FLAT);
                        GlStateManager.disableRescaleNormal();
                    }
                }
            }
        });
    }
}
