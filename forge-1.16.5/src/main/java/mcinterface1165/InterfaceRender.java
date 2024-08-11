package mcinterface1165;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IInterfaceRender;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.GIFParser.GIFImageFrame;
import minecrafttransportsimulator.rendering.GIFParser.ParsedGIF;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.world.LightType;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.Buffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, etc.
 *
 * @author don_bruce
 */
public class InterfaceRender implements IInterfaceRender {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final Map<String, Identifier> ONLINE_TEXTURES = new HashMap<>();
    private static final Map<String, ParsedGIF> ANIMATED_GIFS = new HashMap<>();
    private static final Map<ParsedGIF, Map<GIFImageFrame, Identifier>> GIF_FRAMES = new LinkedHashMap<>();

    private static final List<GUIComponentItem> RENDER_STACKS = new ArrayList<>();

    private static final Map<String, RenderLayer> RENDER_LAYERS = new HashMap<>();
    private static final Map<RenderableData, BufferData> BUFFERS = new HashMap<>();
    private static final Map<RenderLayer, List<RenderData>> QUEUED_RENDERS = new HashMap<>();
    private static final ConcurrentLinkedQueue<BufferData> REMOVED_RENDERS = new ConcurrentLinkedQueue<>();

    private static RenderPhase.Texture MISSING_STATE;
    private static RenderPhase.Texture BLOCK_STATE;
    private static MatrixStack matrices;
    private static VertexConsumerProvider renderBuffer;
    public static Point3D renderCameraOffset = new Point3D();
    private static boolean renderingGUI;
    private static final float[] matrixConvertArray = new float[16];

    @Override
    public float[] getBlockBreakTexture(AWrapperWorld world, Point3D position) {
        //Get normal model.
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        BlockState state = ((WrapperWorld) world).world.getBlockState(pos);
        Sprite sprite = CLIENT.getBlockRenderManager().getModels().getTexture(state, ((WrapperWorld) world).world, pos);
        return new float[]{sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV()};
    }

    @Override
    public float[] getDefaultBlockTexture(String name) {
        Sprite sprite = CLIENT.getBlockRenderManager().getModels().getModelManager().method_24153(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).getSprite(new Identifier(name.replace(":", ":blocks/")));
        return new float[]{sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV()};
    }

    @Override
    public InputStream getTextureStream(String name) {
        try {
            String domain = name.substring("/assets/".length(), name.indexOf("/", "/assets/".length()));
            String location = name.substring("/assets/".length() + domain.length() + 1);
            return CLIENT.getResourceManager().getResource(new Identifier(domain, location)).getInputStream();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void renderItemModel(GUIComponentItem component) {
        RENDER_STACKS.add(component);
    }

    @Override
    public void renderVertices(RenderableData data, boolean changedSinceLastRender) {
        matrices.push();
        Matrix4f matrix4f = convertMatrix4f(data.transform);
        MatrixStack.Entry stackEntry = matrices.peek();
        stackEntry.getModel().multiply(matrix4f);

        if (data.vertexObject.isLines) {
            VertexConsumer buffer = renderBuffer.getBuffer(RenderLayer.getLines());
            while (data.vertexObject.vertices.hasRemaining()) {
                buffer.vertex(stackEntry.getModel(), data.vertexObject.vertices.get(), data.vertexObject.vertices.get(), data.vertexObject.vertices.get());
                buffer.color(data.color.red, data.color.green, data.color.blue, data.alpha);
                buffer.next();
            }
            //Rewind buffer for next read.
            ((Buffer) data.vertexObject.vertices).rewind();
        } else {
            String typeID = data.texture + data.isTranslucent + data.lightingMode + data.enableBrightBlending;
            final RenderLayer renderLayer;
            if (data.vertexObject.cacheVertices && !renderingGUI) {
                //Get the render type and data buffer for this entity.
                renderLayer = RENDER_LAYERS.computeIfAbsent(typeID, k -> CustomRenderType.of("mts_entity", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, 7, 2097152, true, data.isTranslucent, CustomRenderType.createForObject(data).build(false)));
                BufferData buffer = BUFFERS.computeIfAbsent(data, k -> new BufferData(renderLayer, data));

                //Reset buffer if it's not ready.
                if (changedSinceLastRender) {
                    buffer.builder.clear();
                    buffer.isReady = false;
                }
                if (!buffer.isReady) {
                    int index = 0;
                    buffer.builder.begin(GL11.GL_QUADS, renderLayer.getVertexFormat());
                    while (data.vertexObject.vertices.hasRemaining()) {
                        //Need to parse these out first since our order differs.
                        float normalX = data.vertexObject.vertices.get();
                        float normalY = data.vertexObject.vertices.get();
                        float normalZ = data.vertexObject.vertices.get();
                        float texU = data.vertexObject.vertices.get();
                        float texV = data.vertexObject.vertices.get();
                        float posX = data.vertexObject.vertices.get();
                        float posY = data.vertexObject.vertices.get();
                        float posZ = data.vertexObject.vertices.get();

                        //Add the vertex format bits.
                        do {
                            buffer.builder.vertex(posX, posY, posZ, data.color.red, data.color.green, data.color.blue, data.alpha, texU, texV, OverlayTexture.DEFAULT_UV, data.worldLightValue, normalX, normalY, normalZ);
                        } while (++index == 3);
                        if (index == 4) {
                            index = 0;
                        }
                    }
                    buffer.isReady = true;
                    buffer.builder.end();
                    buffer.buffer.upload(buffer.builder);
                    ((Buffer) data.vertexObject.vertices).rewind();
                }

                //Add this buffer to the list to render later.
                List<RenderData> renders = QUEUED_RENDERS.computeIfAbsent(renderLayer, k -> new ArrayList<>());
                renders.add(new RenderData(stackEntry.getModel(), buffer.buffer));
            } else {
                renderLayer = RENDER_LAYERS.computeIfAbsent(typeID, k -> CustomRenderType.of("mts_entity", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, 7, 256, true, data.isTranslucent, CustomRenderType.createForObject(data).build(false)));
                VertexConsumer buffer = renderBuffer.getBuffer(renderLayer);

                //Now populate the state we requested.
                int index = 0;
                while (data.vertexObject.vertices.hasRemaining()) {
                    //Need to parse these out first since our order differs.
                    float normalX = data.vertexObject.vertices.get();
                    float normalY = data.vertexObject.vertices.get();
                    float normalZ = data.vertexObject.vertices.get();
                    float texU = data.vertexObject.vertices.get();
                    float texV = data.vertexObject.vertices.get();
                    float posX = data.vertexObject.vertices.get();
                    float posY = data.vertexObject.vertices.get();
                    float posZ = data.vertexObject.vertices.get();

                    //Add the vertex.  Yes, we have to multiply this here on the CPU.  Yes, it's retarded because the GPU should be doing the matrix math.
                    //Blaze3d my ass, this is SLOWER than DisplayLists!
                    //We also need to add the 3rd vertex twice, since the buffer wants quads rather than tris.
                    //Yes, we have to render 25% more data because Mojang doesn't wanna move to tris like literally every other game.
                    //Yes, they're stupid.
                    do {
                        buffer.vertex(stackEntry.getModel(), posX, posY, posZ);
                        buffer.color(data.color.red, data.color.green, data.color.blue, data.alpha);
                        buffer.texture(texU, texV);
                        buffer.overlay(OverlayTexture.DEFAULT_UV);
                        buffer.light(data.worldLightValue);
                        buffer.normal(stackEntry.getNormal(), normalX, normalY, normalZ);
                        buffer.next();
                    } while (++index == 3);
                    if (index == 4) {
                        index = 0;
                    }
                }
                //Rewind buffer for next read.
                ((Buffer) data.vertexObject.vertices).rewind();
            }
        }
        matrices.pop();
    }

    @Override
    public void deleteVertices(RenderableData data) {
        if (data.vertexObject.cacheVertices) {
            //Add to removed render list, we should only remove renders AFTER they are rendered.
            //This ensures they are un-bound, if the were bound prior.
            //Make sure we actually bound a buffer; just because the main system asks for a bound buffer,
            //doesn't mean we actually can give it one.  GUI models are one such case, as they don't work right
            //with bound buffers due to matrix differences.
            BufferData buffer = BUFFERS.remove(data);
            if (buffer != null) {
                REMOVED_RENDERS.add(buffer);
            }
        }
    }

    @Override
    public int getLightingAtPosition(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        return LightmapTextureManager.pack(CLIENT.world.getLightLevel(LightType.BLOCK, pos), CLIENT.world.getLightLevel(LightType.SKY, pos));
    }

    @Override
    public boolean shouldRenderBoundingBoxes() {
        return CLIENT.getEntityRenderDispatcher().shouldRenderHitboxes();
    }

    @Override
    public boolean bindURLTexture(String textureURL, InputStream stream) {
        if (stream != null) {
            try {
                NativeImage image = NativeImage.read(NativeImage.Format.BGR, stream);
                NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                Identifier textureLocation = CLIENT.textureManager.registerDynamicTexture("mts-url", texture);
                ONLINE_TEXTURES.put(textureURL, textureLocation);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            ONLINE_TEXTURES.put(textureURL, null);
            return true;
        }
    }

    @Override
    public boolean bindURLGIF(String textureURL, ParsedGIF gif) {
        Map<GIFImageFrame, Identifier> gifFrameIndexes = new HashMap<>();
        for (GIFImageFrame frame : gif.frames.values()) {
            try {
                BufferedImage frameBuffer = frame.getImage();
                ByteArrayOutputStream frameArrayStream = new ByteArrayOutputStream();
                ImageIO.write(frameBuffer, "gif", frameArrayStream);
                InputStream frameStream = new ByteArrayInputStream(frameArrayStream.toByteArray());

                NativeImage image = NativeImage.read(NativeImage.Format.BGR, frameStream);
                NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                Identifier textureLocation = CLIENT.textureManager.registerDynamicTexture("mts-gif", texture);
                gifFrameIndexes.put(frame, textureLocation);
            } catch (Exception e) {
                return false;
            }
        }
        ANIMATED_GIFS.put(textureURL, gif);
        GIF_FRAMES.put(gif, gifFrameIndexes);
        return true;
    }

    /**
     * Helper function to create a new texture state for the specified texture location.
     */
    private static RenderPhase.Texture getTexture(String textureLocation) {
        //Check to make sure textures exist.  We delay creating because some mods screw up this stuff in boot.  Cray, looking at you buddy.
        if (MISSING_STATE == null) {
            MISSING_STATE = new RenderPhase.Texture(new Identifier("mts:textures/rendering/missing.png"), false, false);
            BLOCK_STATE = new RenderPhase.Texture(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, false, false);
        }

        if (ANIMATED_GIFS.containsKey(textureLocation)) {
            //Special case for GIFs.
            ParsedGIF parsedGIF = ANIMATED_GIFS.get(textureLocation);
            return new RenderPhase.Texture(GIF_FRAMES.get(parsedGIF).get(parsedGIF.getCurrentFrame()), false, false);
        } else if (ONLINE_TEXTURES.containsKey(textureLocation)) {
            //Online texture.
            Identifier onlineTexture = ONLINE_TEXTURES.get(textureLocation);
            return onlineTexture != null ? new RenderPhase.Texture(ONLINE_TEXTURES.get(textureLocation), false, false) : MISSING_STATE;
        } else if (textureLocation.equals(RenderableData.GLOBAL_TEXTURE_NAME)) {
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
                return new RenderPhase.Texture(new Identifier(domain, location), false, false);
            } else {
                InterfaceManager.coreInterface.logError("Could not find texture: " + formattedLocation + " Reverting to fallback texture.");
                return MISSING_STATE;
            }
        }
    }

    /**
     * Renders the main GUI, setting up any transforms or operations as required.
     */
    @SuppressWarnings("deprecation")
    protected static void renderGUI(MatrixStack stack, int mouseX, int mouseY, int screenWidth, int screenHeight, float partialTicks, boolean updateGUIs) {
        //Get the buffer for GUI rendering.
        matrices = stack;
        matrices.push();
        renderingGUI = true;
        VertexConsumerProvider.Immediate guiBuffer = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        renderBuffer = guiBuffer;

        //Render GUIs, re-creating their components if needed.
        //Set Y-axis to inverted to have correct orientation.
        matrices.scale(1.0F, -1.0F, 1.0F);

        //Render main pass, then blended pass.
        int displayGUIIndex = 0;
        for (AGUIBase gui : AGUIBase.activeGUIs) {
            if (updateGUIs || gui.components.isEmpty()) {
                gui.setupComponentsInit(screenWidth, screenHeight);
            }
            matrices.push();
            if (gui.capturesPlayer()) {
                //Translate in front of the main GUI components.
                matrices.translate(0, 0, 250);
            } else {
                //Translate far enough to render behind the chat window.
                matrices.translate(0, 0, -500 + 250 * displayGUIIndex++);
            }
            gui.render(mouseX, mouseY, false, partialTicks);
            guiBuffer.draw();
            //Not needed, since we can't draw to custom buffers with GUIs.
            //renderBuffers();

            //Need to use RenderSystem here, since this is a direct buffer.
            RenderSystem.enableBlend();
            gui.render(mouseX, mouseY, true, partialTicks);
            guiBuffer.draw();
            //renderBuffers();
            RenderSystem.disableBlend();

            //Render all stacks.  These have to be in the standard GUI reference frame or they won't render.
            matrices.scale(1.0F, -1.0F, 1.0F);

            for (GUIComponentItem component : RENDER_STACKS) {
                //Double-check the stack is still present, it might have been un-set since this call.
                if (component.stackToRender != null) {
                    //Apply existing transform.
                    //Need to translate the z-offset to our value, which includes a -100 for the default added value.
                    //Blit starts at 200 though, plus 32 for the default item render.
                    float zOffset = CLIENT.getItemRenderer().zOffset;
                    CLIENT.getItemRenderer().zOffset = (float) (200 + component.translation.z - 100);
                    if (component.scale != 1.0) {
                        //Need to use RenderSystem here, since we can't access the stack directly for rendering scaling.
                        RenderSystem.pushMatrix();
                        RenderSystem.scalef(component.scale, component.scale, 1.0F);
                        CLIENT.getItemRenderer().renderGuiItemIcon(((WrapperItemStack) component.stackToRender).stack, (int) (component.translation.x / component.scale), (int) (-component.translation.y / component.scale) + 1);
                        RenderSystem.popMatrix();
                    } else {
                        CLIENT.getItemRenderer().renderGuiItemIcon(((WrapperItemStack) component.stackToRender).stack, (int) component.translation.x, (int) -component.translation.y);
                    }
                    CLIENT.getItemRenderer().zOffset = zOffset;
                }
            }
            RENDER_STACKS.clear();

            matrices.pop();
        }
        matrices.pop();
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
        //Register the global entity rendering class.
        RenderingRegistry.registerEntityRenderingHandler(BuilderEntityRenderForwarder.ENTITY_RENDER.get(), manager -> new EntityRenderer<BuilderEntityRenderForwarder>(manager) {
            @Override
            public Identifier getTexture(BuilderEntityRenderForwarder builder) {
                return null;
            }

            @Override
            public boolean shouldRender(BuilderEntityRenderForwarder entity, Frustum frustum, double x, double y, double z) {
                //Always render the forwarder, no matter where the camera is.
                return true;
            }

            @Override
            public void render(BuilderEntityRenderForwarder builder, float entityYaw, float partialTicks, MatrixStack stack, VertexConsumerProvider buffer, int packedLight) {
                if (builder.playerFollowing == CLIENT.player && !ConfigSystem.settings.general.forceRenderLastSolid.value) {
                    //Set camera offset point for later.
                    renderCameraOffset.set(MathHelper.lerp(partialTicks, builder.lastRenderX, builder.getX()), MathHelper.lerp(partialTicks, builder.lastRenderY, builder.getY()), MathHelper.lerp(partialTicks, builder.lastRenderZ, builder.getZ()));

                    //Set the stack variables and render.
                    doRenderCall(stack, buffer, false, partialTicks);
                }
            }
        });

        //Register blank classes for the other builders.
        //If we don't, the game crashes when trying to render them.
        RenderingRegistry.registerEntityRenderingHandler(BuilderEntityExisting.ENTITY_EXISTING.get(), BlankRender::new);
        RenderingRegistry.registerEntityRenderingHandler(BuilderEntityLinkedSeat.ENTITY_SEAT.get(), BlankRender::new);
    }

    public static void doRenderCall(MatrixStack stack, VertexConsumerProvider buffer, boolean blendingEnabled, float partialTicks) {
        matrices = stack;
        renderBuffer = buffer;
        AWrapperWorld world = InterfaceManager.clientInterface.getClientWorld();
        ConcurrentLinkedQueue<AEntityC_Renderable> allEntities = world.renderableEntities;
        if (!allEntities.isEmpty()) {
            world.beginProfiling("MTSRendering_Setup", true);

            //NOTE: this operation occurs on a ConcurrentLinkedQueue.  Therefore, updates will
            //not occur one after another.  Sanitize your inputs!
            for (AEntityC_Renderable entity : allEntities) {
                matrices.push();
                matrices.translate(entity.position.x - renderCameraOffset.x, entity.position.y - renderCameraOffset.y, entity.position.z - renderCameraOffset.z);
                entity.render(blendingEnabled, partialTicks);
                matrices.pop();
            }

            //Need to tell the immediate buffer  it's done rendering, else it'll hold onto the data and crash other systems.
            if (renderBuffer instanceof VertexConsumerProvider.Immediate) {
                ((VertexConsumerProvider.Immediate) renderBuffer).draw();
            }

            //Now do the actual render.
            world.beginProfiling("MTSRendering_Execution", false);
            renderBuffers();
            world.endProfiling();
        }
    }

    private static void renderBuffers() {
        //Call order is CRITICAL and will lead to random JME faults with no stacktrace if modified!
        for (Entry<RenderLayer, List<RenderData>> renderEntry : QUEUED_RENDERS.entrySet()) {
            RenderLayer renderLayer = renderEntry.getKey();
            List<RenderData> datas = renderEntry.getValue();
            if (!datas.isEmpty()) {
                renderLayer.startDrawing();
                for (RenderData data : datas) {
                    data.buffer.bind();
                    renderLayer.getVertexFormat().startDrawing(0L);
                    data.buffer.draw(data.matrix, GL11.GL_QUADS);
                }
                renderLayer.getVertexFormat().endDrawing();
                renderLayer.endDrawing();
                datas.clear();
            }
        }
        VertexBuffer.unbind();
        if (!REMOVED_RENDERS.isEmpty()) {
            REMOVED_RENDERS.forEach(render -> render.buffer.close());
            REMOVED_RENDERS.clear();
        }
    }


    /**
     * Blank render class used to bypass rendering for all other builders.
     **/
    private static class BlankRender<T extends ABuilderEntityBase> extends EntityRenderer<T> {

        protected BlankRender(EntityRenderDispatcher renderDispatcher) {
            super(renderDispatcher);
        }

        @Override
        public Identifier getTexture(T entity) {
            return null;
        }

        @Override
        public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
            return false;
        }
    }

    private static class CustomRenderType extends RenderLayer {
        private CustomRenderType(String name, VertexFormat fmt, int glMode, int size, boolean doCrumbling, boolean depthSorting, Runnable onEnable, Runnable onDisable) {
            super(name, fmt, glMode, size, doCrumbling, depthSorting, onEnable, onDisable);
            throw new IllegalStateException("This class must not be instantiated, this is only here to gain access to the rendering constants.");
        }

        private static RenderLayer.MultiPhaseParameters.Builder createForObject(RenderableData data) {
            //Create the state builder.  Changed states are active, default states are commented to save processing but still show the state.
            RenderLayer.MultiPhaseParameters.Builder stateBuilder = RenderLayer.MultiPhaseParameters.builder();

            stateBuilder.texture(getTexture(data.texture));
            //Transparency is also blend function, so we need to override that with a custom one if we are doing bright blending.
            stateBuilder.transparency(data.enableBrightBlending ? BRIGHTNESS_TRANSPARENCY : (data.isTranslucent ? PROPER_TRANSLUCENT_TRANSPARENCY : RenderLayer.NO_TRANSPARENCY));
            //Diffuse lighting is the ambient lighting that auto-shades models.
            stateBuilder.diffuseLighting(data.lightingMode.disableTextureShadows ? DISABLE_DIFFUSE_LIGHTING : ENABLE_DIFFUSE_LIGHTING);
            //Always smooth shading.
            stateBuilder.shadeModel(SMOOTH_SHADE_MODEL);
            //Use default alpha to remove alpha fragments in cut-out textures.
            stateBuilder.alpha(ONE_TENTH_ALPHA);
            //Depth test is fine, it ensures translucent things don't render in front of everything.
            //stateBuilder.setDepthTestState(LEQUAL_DEPTH_TEST);
            //Cull is fine.  Not sure what this does, actually...
            //stateBuilder.setCullState(NO_CULL);
            //Lightmap is on unless we are bright.
            stateBuilder.lightmap(data.lightingMode.disableWorldLighting ? DISABLE_LIGHTMAP : ENABLE_LIGHTMAP);
            //No overlays ever.
            stateBuilder.overlay(DISABLE_OVERLAY_COLOR);
            //No fog.
            //stateBuilder.setFogState(NO_FOG);
            //No layering.
            //stateBuilder.setLayeringState(NO_LAYERING);
            //Target is always main.
            //stateBuilder.setOutputState(MAIN_TARGET);
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
        private final Matrix4f matrix;
        private final VertexBuffer buffer;

        private RenderData(Matrix4f matrix, VertexBuffer buffer) {
            this.matrix = new Matrix4f(matrix);
            this.buffer = buffer;
        }
    }

    private static class BufferData {
        final BufferBuilder builder;
        final VertexBuffer buffer;
        boolean isReady;

        private BufferData() {
            builder = null;
            buffer = null;
        }

        private BufferData(RenderLayer type, RenderableData data) {
            int vertices = data.vertexObject.vertices.limit() / 8;
            //Convert verts to faces, then back to quad-verts for MC rendering.
            //Add one face extra, since MC will want to increase the buffer if sees it can't handle another vert.
            vertices = ((vertices / 3) + 1) * 4;
            this.builder = new BufferBuilder(type.getVertexFormat().getVertexSizeInteger() * vertices);
            this.buffer = new VertexBuffer(type.getVertexFormat());
        }
    }

    /**
     * Proper translucent transparency.  MC's one has the wrong blending function, and
     * we need to disable writing to the depth buffer to prevent culling of other translucent
     * objects, since we don't sort them and we could render behind an already-rendered transparent
     * fragment.  Say if we have two headlight flares.
     */
    private static final RenderPhase.Transparency PROPER_TRANSLUCENT_TRANSPARENCY = new RenderPhase.Transparency("proper_translucent_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
    });

    /**
     * Brightness transparency.  Does a special blending operation to make things behind
     * the object brighter based on the object's alpha value.  More alpha means more bright.
     */
    private static final RenderPhase.Transparency BRIGHTNESS_TRANSPARENCY = new RenderPhase.Transparency("brightness_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.DST_COLOR, GlStateManager.DstFactor.SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
    });
}
