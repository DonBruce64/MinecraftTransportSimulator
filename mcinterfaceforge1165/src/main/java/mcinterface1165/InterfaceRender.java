package mcinterface1165;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IInterfaceRender;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.GIFParser;
import minecrafttransportsimulator.rendering.GIFParser.GIFImageFrame;
import minecrafttransportsimulator.rendering.GIFParser.ParsedGIF;
import minecrafttransportsimulator.rendering.RenderableObject;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.State.Builder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.LightType;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, etc.
 *
 * @author don_bruce
 */
public class InterfaceRender implements IInterfaceRender {
    private static final Map<String, ResourceLocation> onlineTextures = new HashMap<>();
    private static final Map<String, ParsedGIF> animatedGIFs = new HashMap<>();
    private static final Map<ParsedGIF, Map<GIFImageFrame, ResourceLocation>> animatedGIFFrames = new LinkedHashMap<>();

    private static final List<GUIComponentItem> stacksToRender = new ArrayList<>();

    private static Map<String, RenderType> renderTypes = new HashMap<>();
    private static final BufferData testData = new BufferData();
    private static boolean onBufferSet2 = false;
    private static Map<RenderableObject, List<BufferData>> bufferSet1 = new HashMap<>();
    private static Map<RenderableObject, List<BufferData>> bufferSet2 = new HashMap<>();
    private static Map<RenderType, List<RenderData>> queuedRenders = new HashMap<>();
    private static ConcurrentLinkedQueue<BufferData> removedRenders = new ConcurrentLinkedQueue<>();

    private static RenderState.TextureState MISSING_STATE;
    private static RenderState.TextureState BLOCK_STATE;
    private static MatrixStack matrixStack;
    private static IRenderTypeBuffer renderBuffer;
    private static Point3D renderCameraOffset = new Point3D();
    private static boolean renderingGUI;
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
    public InputStream getTextureStream(String name) {
        try {
            String domain = name.substring("/assets/".length(), name.indexOf("/", "/assets/".length()));
            String location = name.substring("/assets/".length() + domain.length() + 1);
            return Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(domain, location)).getInputStream();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void renderItemModel(GUIComponentItem component) {
        stacksToRender.add(component);
    }
    
    @Override
    public void renderVertices(RenderableObject object) {
        matrixStack.pushPose();
        Matrix4f matrix4f = convertMatrix4f(object.transform);
        MatrixStack.Entry stackEntry = matrixStack.last();
        stackEntry.pose().multiply(matrix4f);

        if (object.isLines) {
            IVertexBuilder buffer = renderBuffer.getBuffer(RenderType.lines());
            while (object.vertices.hasRemaining()) {
                buffer.vertex(stackEntry.pose(), object.vertices.get(), object.vertices.get(), object.vertices.get());
                buffer.color(object.color.red, object.color.green, object.color.blue, object.alpha);
                buffer.endVertex();
            }
            //Rewind buffer for next read.
            object.vertices.rewind();
        } else {
            String typeID = object.texture + object.isTranslucent + object.enableBrightBlending + object.ignoreWorldShading + object.disableLighting;
            RenderType renderType = renderTypes.get(typeID);
            if (object.cacheVertices && !renderingGUI) {
                if (renderType == null) {
                    Builder stateBuilder = CustomRenderType.createForObject(object);
                    renderType = CustomRenderType.create("mts_entity", DefaultVertexFormats.NEW_ENTITY, 7, 2097152, true, object.isTranslucent, stateBuilder.createCompositeState(false));
                    renderTypes.put(typeID, renderType);
                }

                //Get the active buffer set.  We flip-flop each render.
                List<BufferData> availableBuffers;
                List<BufferData> usedBuffers;
                if (onBufferSet2) {
                    availableBuffers = bufferSet2.get(object);
                    if (availableBuffers == null) {
                        availableBuffers = new ArrayList<>();
                        bufferSet2.put(object, availableBuffers);
                    }
                    usedBuffers = bufferSet1.get(object);
                    if (usedBuffers == null) {
                        usedBuffers = new ArrayList<>();
                        bufferSet1.put(object, usedBuffers);
                    }
                } else {
                    availableBuffers = bufferSet1.get(object);
                    if (availableBuffers == null) {
                        availableBuffers = new ArrayList<>();
                        bufferSet1.put(object, availableBuffers);
                    }
                    usedBuffers = bufferSet2.get(object);
                    if (usedBuffers == null) {
                        usedBuffers = new ArrayList<>();
                        bufferSet2.put(object, usedBuffers);
                    }
                }

                //Try to get a already-created buffer rather than make a new one.
                testData.setTo(object);
                int index = availableBuffers.indexOf(testData);
                BufferData data;
                if (index != -1) {
                    //Matching data, use it.
                    data = availableBuffers.remove(index);
                } else if (!availableBuffers.isEmpty()) {
                    //Get last data, since it's least likely to be static.
                    data = availableBuffers.remove(availableBuffers.size() - 1);
                    data.setTo(object);
                } else {
                    //No data available, need to make it.
                    data = new BufferData(renderType, object);
                }
                usedBuffers.add(data);

                //Make sure data is ready, if not, init it.
                if (!data.isReady) {
                    index = 0;
                    data.builder.begin(GL11.GL_QUADS, renderType.format());
                    while (object.vertices.hasRemaining()) {
                        //Need to parse these out first since our order differs.
                        float normalX = object.vertices.get();
                        float normalY = object.vertices.get();
                        float normalZ = object.vertices.get();
                        float texU = object.vertices.get();
                        float texV = object.vertices.get();
                        float posX = object.vertices.get();
                        float posY = object.vertices.get();
                        float posZ = object.vertices.get();

                        //Add the vertex format bits.
                        do {
                            data.builder.vertex(posX, posY, posZ, object.color.red, object.color.green, object.color.blue, object.alpha, texU, texV, OverlayTexture.NO_OVERLAY, object.worldLightValue, normalX, normalY, normalZ);
                        } while (++index == 3);
                        if (index == 4) {
                            index = 0;
                        }
                    }
                    data.isReady = true;
                    data.builder.end();
                    data.buffer.upload(data.builder);
                    object.vertices.rewind();
                }

                //Add this buffer to the list to render later.
                List<RenderData> renders = queuedRenders.get(renderType);
                if (renders == null) {
                    renders = new ArrayList<>();
                    queuedRenders.put(renderType, renders);
                }
                renders.add(new RenderData(stackEntry.pose(), data.buffer));
            } else {
                if (renderType == null) {
                    Builder stateBuilder = CustomRenderType.createForObject(object);
                    renderType = CustomRenderType.create("mts_entity", DefaultVertexFormats.NEW_ENTITY, 7, 256, true, object.isTranslucent, stateBuilder.createCompositeState(false));
                    renderTypes.put(typeID, renderType);
                }
                IVertexBuilder buffer = renderBuffer.getBuffer(renderType);
                
                //Now populate the state we requested.
                int index = 0;
                while (object.vertices.hasRemaining()) {
                    //Need to parse these out first since our order differs.
                    float normalX = object.vertices.get();
                    float normalY = object.vertices.get();
                    float normalZ = object.vertices.get();
                    float texU = object.vertices.get();
                    float texV = object.vertices.get();
                    float posX = object.vertices.get();
                    float posY = object.vertices.get();
                    float posZ = object.vertices.get();

                    //Add the vertex.  Yes, we have to multiply this here on the CPU.  Yes, it's retarded because the GPU should be doing the matrix math.
                    //Blaze3d my ass, this is SLOWER than DisplayLists!
                    //We also need to add the 3rd vertex twice, since the buffer wants quads rather than tris.
                    //Yes, we have to render 25% more data because Mojang doesn't wanna move to tris like literally every other game.
                    //Yes, they're stupid.
                    do {
                        buffer.vertex(stackEntry.pose(), posX, posY, posZ);
                        buffer.color(object.color.red, object.color.green, object.color.blue, object.alpha);
                        buffer.uv(texU, texV);
                        buffer.overlayCoords(OverlayTexture.NO_OVERLAY);
                        buffer.uv2(object.worldLightValue);
                        buffer.normal(stackEntry.normal(), normalX, normalY, normalZ);
                        buffer.endVertex();
                    } while (++index == 3);
                    if (index == 4) {
                        index = 0;
                    }
                }
                //Rewind buffer for next read.
                object.vertices.rewind();
            }
        }
        matrixStack.popPose();
    }

    @Override
    public void deleteVertices(RenderableObject object) {
        //Add to removed render list, we should only remove renders AFTER they are rendered.
        List<BufferData> dataSet = bufferSet1.remove(object);
        if (dataSet != null) {
            removedRenders.addAll(dataSet);
        }
        dataSet = bufferSet2.remove(object);
        if (dataSet != null) {
            removedRenders.addAll(dataSet);
        }
    }

    @Override
    public int getLightingAtPosition(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        return LightTexture.pack(Minecraft.getInstance().level.getBrightness(LightType.BLOCK, pos), Minecraft.getInstance().level.getBrightness(LightType.SKY, pos));
    }

    @Override
    public boolean shouldRenderBoundingBoxes() {
        return Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes();
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
                                Map<GIFImageFrame, ResourceLocation> gifFrameIndexes = new HashMap<>();
                                for (GIFImageFrame frame : gif.frames.values()) {
                                    BufferedImage frameBuffer = frame.getImage();
                                    ByteArrayOutputStream frameArrayStream = new ByteArrayOutputStream();
                                    ImageIO.write(frameBuffer, "gif", frameArrayStream);
                                    InputStream frameStream = new ByteArrayInputStream(frameArrayStream.toByteArray());

                                    NativeImage image = NativeImage.read(NativeImage.PixelFormat.RGB, frameStream);
                                    DynamicTexture texture = new DynamicTexture(image);
                                    ResourceLocation textureLocation = Minecraft.getInstance().textureManager.register("mts-gif", texture);
                                    gifFrameIndexes.put(frame, textureLocation);
                                }
                                animatedGIFFrames.put(gif, gifFrameIndexes);
                            } else {
                                return "Could not parse GIF due to no frames being present.  Is this a real direct link or a fake one?";
                            }
                        } else {
                            NativeImage image = NativeImage.read(NativeImage.PixelFormat.RGB, url.openStream());
                            DynamicTexture texture = new DynamicTexture(image);
                            ResourceLocation textureLocation = Minecraft.getInstance().textureManager.register("mts-url", texture);
                            onlineTextures.put(textureURL, textureLocation);
                        }
                    } else {
                        StringBuilder errorString = new StringBuilder("Invalid content type found.  Found:" + contentType + ", but the only valid types are: ");
                        for (String validType : validContentTypes) {
                            errorString.append(validType).append(", ");
                        }
                        onlineTextures.put(textureURL, null);
                        return errorString.toString();
                    }
                } catch (Exception e) {
                    onlineTextures.put(textureURL, null);
                    e.printStackTrace();
                    return "Could not parse images.  Error was: " + e.getMessage();
                }
            } catch (Exception e) {
                onlineTextures.put(textureURL, null);
                e.printStackTrace();
                return "Could not open URL for processing.  Error was: " + e.getMessage();
            }
        }
        return null;
    }

    /**
     * Helper function to create a new texture state for the specified texture location.
     */
    private static RenderState.TextureState getTexture(String textureLocation) {
        //Check to make sure textures exist.  We delay creating because some mods screw up this stuff in boot.  Cray, looking at you buddy.
        if (MISSING_STATE == null) {
            MISSING_STATE = new RenderState.TextureState(new ResourceLocation("mts:textures/rendering/missing.png"), false, false);
            BLOCK_STATE = new RenderState.TextureState(PlayerContainer.BLOCK_ATLAS, false, false);
        }

        if (animatedGIFs.containsKey(textureLocation)) {
            //Special case for GIFs.
            ParsedGIF parsedGIF = animatedGIFs.get(textureLocation);
            return new RenderState.TextureState(animatedGIFFrames.get(parsedGIF).get(parsedGIF.getCurrentFrame()), false, false);
        } else if (onlineTextures.containsKey(textureLocation)) {
            //Online texture.
            ResourceLocation onlineTexture = onlineTextures.get(textureLocation);
            return onlineTexture != null ? new RenderState.TextureState(onlineTextures.get(textureLocation), false, false) : MISSING_STATE;
        } else if (textureLocation.equals(RenderableObject.GLOBAL_TEXTURE_NAME)) {
            //Default texture.
            return BLOCK_STATE;
        } else {
            //If the texture has a colon, it's a short-hand form that needs to be converted.
            String formattedLocation = textureLocation;
            if (textureLocation.contains(":")) {
                formattedLocation = "/assets/" + textureLocation.replace(":", "/");
            }

            //Check if the texture exists.
            if (InterfaceManager.coreInterface.getPackResource(formattedLocation) != null) {
                //Convert the classpath-location to a domain-location path for MC.
                String domain = formattedLocation.substring("/assets/".length(), formattedLocation.indexOf("/", "/assets/".length()));
                String location = formattedLocation.substring("/assets/".length() + domain.length() + 1);
                return new RenderState.TextureState(new ResourceLocation(domain, location), false, false);
            } else {
                InterfaceManager.coreInterface.logError("Could not find texture: " + formattedLocation + " Reverting to fallback texture.");
                return MISSING_STATE;
            }
        }
    }

    /**
     * Renders the main GUI, setting up any transforms or operations as required.
     */
    @SuppressWarnings("deprecation") //This is for RenderSystem calls.
    protected static void renderGUI(MatrixStack stack, int mouseX, int mouseY, int screenWidth, int screenHeight, float partialTicks, boolean updateGUIs) {
        //Get the buffer for GUI rendering.
        matrixStack = stack;
        matrixStack.pushPose();
        renderingGUI = true;
        IRenderTypeBuffer.Impl guiBuffer = IRenderTypeBuffer.immediate(Tessellator.getInstance().getBuilder());
        renderBuffer = guiBuffer;

        //Render GUIs, re-creating their components if needed.
        //Set Y-axis to inverted to have correct orientation.
        matrixStack.scale(1.0F, -1.0F, 1.0F);
        
        //Render main pass, then blended pass.
        int displayGUIIndex = 0;
        for (AGUIBase gui : AGUIBase.activeGUIs) {
            if (updateGUIs || gui.components.isEmpty()) {
                gui.setupComponentsInit(screenWidth, screenHeight);
            }
            matrixStack.pushPose();
            if (gui.capturesPlayer()) {
                //Translate in front of the main GUI components.
                matrixStack.translate(0, 0, 250);
            } else {
                //Translate far enough to render behind the chat window.
                matrixStack.translate(0, 0, -500 + 250 * displayGUIIndex++);
            }
            gui.render(mouseX, mouseY, false, partialTicks);
            guiBuffer.endBatch();
            //Not needed, since we can't draw to custom buffers with GUIs.
            //renderBuffers();

            //Need to use RenderSystem here, since this is a direct buffer.
            RenderSystem.enableBlend();
            gui.render(mouseX, mouseY, true, partialTicks);
            guiBuffer.endBatch();
            //renderBuffers();
            RenderSystem.disableBlend();
        
            //Render all stacks.  These have to be in the standard GUI reference frame or they won't render.
            matrixStack.scale(1.0F, -1.0F, 1.0F);

            for (GUIComponentItem component : stacksToRender) {
                //Double-check the stack is still present, it might have been un-set since this call.
                if ((WrapperItemStack) component.stackToRender != null) {
                    //Apply existing transform.
                    //Need to translate the z-offset to our value, which includes a -100 for the default added value.
                    //Blit starts at 200 though, plus 32 for the default item render.
                    float zOffset = Minecraft.getInstance().getItemRenderer().blitOffset;
                    Minecraft.getInstance().getItemRenderer().blitOffset = (float) (200 + component.translation.z - 100);
                    if (component.scale != 1.0) {
                        //Need to use RenderSystem here, since we can't access the stack directly for rendering scaling.
                        RenderSystem.pushMatrix();
                        RenderSystem.scalef(component.scale, component.scale, 1.0F);
                        Minecraft.getInstance().getItemRenderer().renderGuiItem(((WrapperItemStack) component.stackToRender).stack, (int) (component.translation.x / component.scale), (int) (-component.translation.y / component.scale) + 1);
                        RenderSystem.popMatrix();
                    } else {
                        Minecraft.getInstance().getItemRenderer().renderGuiItem(((WrapperItemStack) component.stackToRender).stack, (int) component.translation.x, (int) -component.translation.y);
                    }
                    Minecraft.getInstance().getItemRenderer().blitOffset = zOffset;
                }
            }
            stacksToRender.clear();
        
            matrixStack.popPose();
        }
        matrixStack.popPose();
        renderingGUI = false;
    }

    /**
     * Converts internal matrix to new external matrix.
     */
    protected static Matrix4f convertMatrix4f(TransformationMatrix transform) {
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
    public static void registerRenderer(FMLClientSetupEvent event) {
        //Register blank classes for the builders.
        //If we don't, the game crashes when trying to render them.
        RenderingRegistry.registerEntityRenderingHandler(BuilderEntityExisting.E_TYPE2.get(), manager -> new BlankRender<BuilderEntityExisting>(manager));
        RenderingRegistry.registerEntityRenderingHandler(BuilderEntityLinkedSeat.E_TYPE3.get(), manager -> new BlankRender<BuilderEntityLinkedSeat>(manager));
    }

    public static void doRenderCall(MatrixStack stack, IRenderTypeBuffer buffer, boolean blendingEnabled, float partialTicks) {
        if (!blendingEnabled) {
            //Flip the buffer set to the next one prior to rendering.
            onBufferSet2 = !onBufferSet2;
        }

        //Set camera offfset point for later.
        Vector3d position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        renderCameraOffset.set(position.x, position.y, position.z);

        //Set the global variables.
        matrixStack = stack;
        renderBuffer = buffer;

        AWrapperWorld world = InterfaceManager.clientInterface.getClientWorld();
        ConcurrentLinkedQueue<AEntityC_Renderable> allEntities = world.renderableEntities;
        if (allEntities != null) {
            world.beginProfiling("MTSRendering_Setup", true);
            //NOTE: this operation occurs on a ConcurrentLinkedQueue.  Therefore, updates will
            //not occur one after another.  Sanitize your inputs!
            for (AEntityC_Renderable entity : allEntities) {
                matrixStack.pushPose();
                matrixStack.translate(entity.position.x - renderCameraOffset.x, entity.position.y - renderCameraOffset.y, entity.position.z - renderCameraOffset.z);
                entity.render(blendingEnabled, partialTicks);
                matrixStack.popPose();
            }

            //Need to tell the immediate buffer  it's done rendering, else it'll hold onto the data and crash other systems.
            if (renderBuffer instanceof IRenderTypeBuffer.Impl) {
                ((IRenderTypeBuffer.Impl) renderBuffer).endBatch();
            }

            //Now do the actual render.
            world.beginProfiling("MTSRendering_Execution", false);
            renderBuffers();
            world.endProfiling();
        }
    }

    private static void renderBuffers() {
        //Call order is CRITICAL and will lead to random JME faults with no stacktrace if modified!
        for (Entry<RenderType, List<RenderData>> renderEntry : queuedRenders.entrySet()) {
            RenderType renderType = renderEntry.getKey();
            List<RenderData> datas = renderEntry.getValue();
            if (!datas.isEmpty()) {
                renderType.setupRenderState();
                for (RenderData data : datas) {
                    data.buffer.bind();
                    renderType.format().setupBufferState(0L);
                    data.buffer.draw(data.matrix, GL11.GL_QUADS);
                }
                renderType.format().clearBufferState();
                renderType.clearRenderState();
                datas.clear();
            }
        }
        VertexBuffer.unbind();
        if (!removedRenders.isEmpty()) {
            removedRenders.forEach(render -> render.buffer.close());
            removedRenders.clear();
        }
    }
    

    /** Blank render class used to bypass rendering for all other builders.**/
    private static class BlankRender<T extends ABuilderEntityBase> extends EntityRenderer<T> {

        protected BlankRender(EntityRendererManager p_i46179_1_) {
            super(p_i46179_1_);
        }

        @Override
        public ResourceLocation getTextureLocation(T pEntity) {
            return null;
        }

        @Override
        public boolean shouldRender(T builder, ClippingHelper camera, double camX, double camY, double camZ) {
            return false;
        }
    }

    private static class CustomRenderType extends RenderType {
        private CustomRenderType(String name, VertexFormat fmt, int glMode, int size, boolean doCrumbling, boolean depthSorting, Runnable onEnable, Runnable onDisable) {
            super(name, fmt, glMode, size, doCrumbling, depthSorting, onEnable, onDisable);
            throw new IllegalStateException("This class must not be instantiated, this is only here to gain access to the rendering constants.");
        }

        private static Builder createForObject(RenderableObject object) {
            //Create the state builder.  Changed states are active, default states are commented to save processing but still show the state.
            RenderType.State.Builder stateBuilder = RenderType.State.builder();

            stateBuilder.setTextureState(getTexture(object.texture));
            //Transparency is also blend function, so we need to override that with a custom one if we are doing bright blending.
            stateBuilder.setTransparencyState(object.enableBrightBlending ? BRIGHTNESS_TRANSPARENCY : (object.isTranslucent ? PROPER_TRANSLUCENT_TRANSPARENCY : RenderType.NO_TRANSPARENCY));
            //Diffuse lighting is the ambient lighting that auto-shades models.
            stateBuilder.setDiffuseLightingState(object.ignoreWorldShading || object.disableLighting ? NO_DIFFUSE_LIGHTING : DIFFUSE_LIGHTING);
            //Always smooth shading.
            stateBuilder.setShadeModelState(SMOOTH_SHADE);
            //Use default alpha to remove alpha fragments in cut-out textures.
            stateBuilder.setAlphaState(DEFAULT_ALPHA);
            //Depth test is fine, it ensures translucent things don't render in front of everything.
            //stateBuilder.setDepthTestState(LEQUAL_DEPTH_TEST);
            //Cull is fine.  Not sure what this does, actually...
            //stateBuilder.setCullState(NO_CULL);
            //Lightmap is on unless we are bright.
            stateBuilder.setLightmapState(object.disableLighting ? NO_LIGHTMAP : LIGHTMAP);
            //No overlays ever.
            stateBuilder.setOverlayState(NO_OVERLAY);
            //No fog.
            //stateBuilder.setFogState(NO_FOG);
            //No layering.
            //stateBuilder.setLayeringState(NO_LAYERING);
            //Target depends on blending.
            stateBuilder.setOutputState(object.isTranslucent ? TRANSLUCENT_TARGET : MAIN_TARGET);
            //Default texture is fine.
            //stateBuilder.setTexturingState(DEFAULT_TEXTURING);
            //Not sure what this does, but it should be fine as-is?
            //stateBuilder.setWriteMaskState(COLOR_DEPTH_WRITE);
            //Don't need to poke lines, there's only one state.
            //stateBuilder.setLineState(DEFAULT_LINE);

            //Return.
            return stateBuilder;
        }
    }

    private static class RenderData {
        final Matrix4f matrix;
        final VertexBuffer buffer;

        private RenderData(Matrix4f matrix, VertexBuffer buffer) {
            this.matrix = new Matrix4f(matrix);
            this.buffer = buffer;
        }
    }

    private static class BufferData {
        final BufferBuilder builder;
        final VertexBuffer buffer;
        float red;
        float green;
        float blue;
        float alpha;
        int lightIndex;
        boolean isReady;

        private BufferData() {
            builder = null;
            buffer = null;
        }

        private BufferData(RenderType type, RenderableObject object) {
            int vertices = object.vertices.limit() / 8;
            //Convert verts to faces, then back to quad-verts for MC rendering.
            //Add one face extra, since MC will want to increase the buffer if sees it can't handle another vert.
            vertices = ((vertices / 3) + 1) * 4;
            this.builder = new BufferBuilder(type.format().getIntegerSize() * vertices);
            this.buffer = new VertexBuffer(type.format());
            setTo(object);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof BufferData) {
                BufferData data = (BufferData) other;
                return data.lightIndex == lightIndex && data.alpha == alpha && data.blue == blue && data.green == green && data.red == red;
            } else {
                return false;
            }
        }

        private void setTo(RenderableObject object) {
            this.red = object.color.red;
            this.green = object.color.green;
            this.blue = object.color.blue;
            this.alpha = object.alpha;
            this.lightIndex = object.worldLightValue;
            this.isReady = false;
            if (builder != null) {
                builder.discard();
            }
        }
    }

    /**
     * Proper translucent transparency.  MC's one has the wrong blending function, and
     * we need to disable writing to the depth buffer to prevent culling of other translucent
     * objects, since we don't sort them and we could render behind an already-rendered transparent
     * fragment.  Say if we have two headlight flares.
     */
    private static final RenderState.TransparencyState PROPER_TRANSLUCENT_TRANSPARENCY = new RenderState.TransparencyState("proper_translucent_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
    });

    /**
     * Brightness transparency.  Does a special blending operation to make things behind
     * the object brighter based on the object's alpha value.  More alpha means more bright.
     */
    private static final RenderState.TransparencyState BRIGHTNESS_TRANSPARENCY = new RenderState.TransparencyState("brightness_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
    });
}
