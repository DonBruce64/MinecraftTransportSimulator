package mcinterface262;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

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
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/**
 * Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, etc.
 *
 * @author don_bruce
 */
public class InterfaceRender implements IInterfaceRender {
    private static final Identifier MISSING_TEXTURE = Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "textures/rendering/missing.png");
    private static final Map<String, Identifier> onlineTextures = new HashMap<>();
    private static final Map<String, ParsedGIF> animatedGIFs = new HashMap<>();
    private static final Map<ParsedGIF, Map<GIFImageFrame, Identifier>> animatedGIFFrames = new LinkedHashMap<>();
    private static final AtomicInteger dynamicTextureIndex = new AtomicInteger();

    private static final List<GUIComponentItem> stacksToRender = new ArrayList<>();

    private static final ConcurrentHashMap<String, RenderType> renderTypes = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Identifier> textureLocations = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Identifier, RenderType> mtsEntityCutoutTypes = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Identifier, RenderType> mtsEntityTranslucentTypes = new ConcurrentHashMap<>();

    /**
     * Cached copies of vertex buffers, keyed by the vertex object.  The submit callbacks run later,
     * so the data must be copied off the MTS buffers.  Only re-copy when the model changes.
     */
    private static final Map<RenderableVertices, float[]> vertexCopies = new ConcurrentHashMap<>();

    /**Scratch objects for the render thread, to avoid per-renderable allocations.*/
    private static final Matrix4f matrixScratch = new Matrix4f();
    private static final Vector3f vectorScratch = new Vector3f();

    private static RenderPipeline mtsEntityCutoutPipeline;
    private static RenderPipeline mtsEntityTranslucentPipeline;

    public static PoseStack matrixStack;
    public static SubmitNodeCollector renderCollector;
    public static Point3D renderCameraOffset = new Point3D();
    private static boolean renderingGUI;
    private static GuiGraphicsExtractor guiExtractor;

    /**
     * Event that's called to register our render pipelines.  MTS models are triangle lists,
     * so we need TRIANGLES variants of the entity pipelines.
     */
    public static void onIVRegisterPipelinesEvent(RegisterRenderPipelinesEvent event) {
        mtsEntityCutoutPipeline = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "pipeline/entity_cutout_triangles"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withCull(false)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .build();
        event.registerPipeline(mtsEntityCutoutPipeline);

        mtsEntityTranslucentPipeline = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "pipeline/entity_translucent_triangles"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .build();
        event.registerPipeline(mtsEntityTranslucentPipeline);
    }

    /**
     * Event that's called to setup the client.  We register our render wrapper
     * class here.
     */
    public static void onIVRegisterRenderersEvent(RegisterRenderers event) {
        //Register the global entity rendering class.
        event.registerEntityRenderer(BuilderEntityRenderForwarder.E_TYPE4.get(), ForwarderRenderer::new);

        //Register blank classes for the other builders.
        //If we don't, the game crashes when trying to render them.
        event.registerEntityRenderer(BuilderEntityExisting.E_TYPE2.get(), BlankRender::new);
        event.registerEntityRenderer(BuilderEntityLinkedSeat.E_TYPE3.get(), BlankRender::new);
    }

    @Override
    public float[] getBlockBreakTexture(AWrapperWorld world, Point3D position) {
        BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
        BlockState state = ((WrapperWorld) world).world.getBlockState(pos);
        Material.Baked material = Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(state, net.minecraft.client.renderer.block.BlockAndTintGetter.EMPTY, pos);
        TextureAtlasSprite sprite = material != null ? material.sprite() : Minecraft.getInstance().getModelManager().getBlockStateModelSet().missingModel().particleMaterial().sprite();
        return new float[] { sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1() };
    }

    @Override
    public float[] getDefaultBlockTexture(String name) {
        TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite sprite = atlas.getSprite(Identifier.parse(name.replace(":", ":blocks/")));
        return new float[] { sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1() };
    }

    @Override
    public String getDefaultFontTextureFolder() {
        return "/assets/mts/textures/mcfont";
    }

    @Override
    public InputStream getTextureStream(String name) {
        try {
            String domain = name.substring("/assets/".length(), name.indexOf("/", "/assets/".length()));
            String location = name.substring("/assets/".length() + domain.length() + 1);
            return Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath(domain, location)).get().open();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void renderItemModel(GUIComponentItem component) {
        stacksToRender.add(component);
    }

    @Override
    public void renderVertices(RenderableData data, boolean changedSinceLastRender) {
        if (data.vertexObject == null || data.vertexObject.vertices == null) {
            return;
        }
        if (renderingGUI) {
            renderGUIVertices(data);
            return;
        }

        PoseStack stack = matrixStack;
        SubmitNodeCollector collector = renderCollector;
        if (stack == null || collector == null) {
            //Not in a render pass, nothing to submit geometry to.
            return;
        }

        //Copy the vertices out now; the submit callback runs later, after the MTS buffers move on.
        float[] vertices = getVertexCopy(data, changedSinceLastRender);

        stack.pushPose();
        stack.mulPose(convertMatrix4f(data.transform, matrixScratch));
        if (data.vertexObject.isLines) {
            int lineCount = vertices.length / 12;
            float red = data.color.red;
            float green = data.color.green;
            float blue = data.color.blue;
            float alpha = data.alpha;
            collector.submitCustomGeometry(stack, RenderTypes.lines(), (pose, buffer) -> {
                for (int i = 0; i < lineCount; ++i) {
                    int offset = i * 12;
                    float x1 = vertices[offset];
                    float y1 = vertices[offset + 1];
                    float z1 = vertices[offset + 2];
                    float x2 = vertices[offset + 3];
                    float y2 = vertices[offset + 4];
                    float z2 = vertices[offset + 5];
                    float dx = x2 - x1;
                    float dy = y2 - y1;
                    float dz = z2 - z1;
                    float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (length > 0.0F) {
                        dx /= length;
                        dy /= length;
                        dz /= length;
                    }
                    buffer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, alpha).setNormal(pose, dx, dy, dz).setLineWidth(1.0F);
                    buffer.addVertex(pose, x2, y2, z2).setColor(red, green, blue, alpha).setNormal(pose, dx, dy, dz).setLineWidth(1.0F);
                }
            });
        } else {
            RenderType renderType = getEntityRenderType(data);
            int vertexCount = vertices.length / 8;
            float red = data.color.red;
            float green = data.color.green;
            float blue = data.color.blue;
            float alpha = data.alpha;
            //MTS's old custom shaders ignored world lighting for some objects.  Approximate that
            //by using full brightness for those lighting modes.
            int light = data.lightingMode.disableWorldLighting ? LightCoordsUtil.FULL_BRIGHT : data.worldLightValue;
            collector.submitCustomGeometry(stack, renderType, (pose, buffer) -> {
                for (int i = 0; i < vertexCount; ++i) {
                    int offset = i * 8;
                    float normalX = vertices[offset];
                    float normalY = vertices[offset + 1];
                    float normalZ = vertices[offset + 2];
                    float texU = vertices[offset + 3];
                    float texV = vertices[offset + 4];
                    float posX = vertices[offset + 5];
                    float posY = vertices[offset + 6];
                    float posZ = vertices[offset + 7];
                    buffer.addVertex(pose, posX, posY, posZ)
                        .setColor(red, green, blue, alpha)
                        .setUv(texU, texV)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(pose, normalX, normalY, normalZ);
                }
            });
        }
        stack.popPose();
    }

    private static RenderType getEntityRenderType(RenderableData data) {
        Identifier texture = getTexture(data.texture);
        //Vanilla entity render types use a QUADS topology, but MTS models are triangle lists.
        //Use our own TRIANGLES variants or the geometry gets assembled into the wrong primitives.
        if (data.isTranslucent || data.enableBrightBlending) {
            return mtsEntityTranslucentTypes.computeIfAbsent(texture, t -> RenderType.create("mts_entity_translucent_triangles", RenderSetup.builder(mtsEntityTranslucentPipeline).withTexture("Sampler0", t).useLightmap().useOverlay().affectsCrumbling().createRenderSetup()));
        } else {
            return mtsEntityCutoutTypes.computeIfAbsent(texture, t -> RenderType.create("mts_entity_cutout_triangles", RenderSetup.builder(mtsEntityCutoutPipeline).withTexture("Sampler0", t).useLightmap().useOverlay().affectsCrumbling().createRenderSetup()));
        }
    }

    public static void doRenderCall(boolean blendingEnabled, float partialTicks) {
        AWrapperWorld world = InterfaceManager.clientInterface.getClientWorld();
        if (world == null) {
            return;
        }
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

            world.endProfiling();
        }
    }

    @Override
    public void deleteVertices(RenderableData data) {
        //Geometry is submitted to the feature renderer each frame, but drop our cached vertex copy.
        vertexCopies.remove(data.vertexObject);
    }

    /**Returns a stable copy of the passed-in data's vertices, re-copying only when they changed.*/
    private static float[] getVertexCopy(RenderableData data, boolean changedSinceLastRender) {
        float[] cached = vertexCopies.get(data.vertexObject);
        if (cached == null || changedSinceLastRender) {
            FloatBuffer vertexBuffer = data.vertexObject.vertices;
            vertexBuffer.rewind();
            cached = new float[vertexBuffer.remaining()];
            vertexBuffer.get(cached);
            vertexBuffer.rewind();
            vertexCopies.put(data.vertexObject, cached);
        }
        return cached;
    }

    @Override
    public int getLightingAtPosition(Point3D position) {
        BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
        return LightCoordsUtil.pack(Minecraft.getInstance().level.getBrightness(LightLayer.BLOCK, pos), Minecraft.getInstance().level.getBrightness(LightLayer.SKY, pos));
    }

    @Override
    public boolean shouldRenderBoundingBoxes() {
        return Minecraft.getInstance().debugEntries.isCurrentlyEnabled(net.minecraft.client.gui.components.debug.DebugScreenEntries.ENTITY_HITBOXES);
    }

    @Override
    public boolean bindURLTexture(String textureURL, InputStream stream) {
        if (stream != null) {
            try {
                NativeImage image = NativeImage.read(NativeImage.Format.RGB, stream);
                DynamicTexture texture = new DynamicTexture(() -> "mts-url", image);
                Identifier textureLocation = Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "dynamic/url_" + dynamicTextureIndex.incrementAndGet());
                Minecraft.getInstance().getTextureManager().register(textureLocation, texture);
                onlineTextures.put(textureURL, textureLocation);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            onlineTextures.put(textureURL, null);
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

                NativeImage image = NativeImage.read(NativeImage.Format.RGB, frameStream);
                DynamicTexture texture = new DynamicTexture(() -> "mts-gif", image);
                Identifier textureLocation = Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "dynamic/gif_" + dynamicTextureIndex.incrementAndGet());
                Minecraft.getInstance().getTextureManager().register(textureLocation, texture);
                gifFrameIndexes.put(frame, textureLocation);
            } catch (Exception e) {
                return false;
            }
        }
        animatedGIFs.put(textureURL, gif);
        animatedGIFFrames.put(gif, gifFrameIndexes);
        return true;
    }

    /**
     * Helper function to resolve the MC texture location for the passed-in renderable texture.
     */
    private static Identifier getTexture(String textureLocation) {
        if (textureLocation == null) {
            return MISSING_TEXTURE;
        }

        if (animatedGIFs.containsKey(textureLocation)) {
            //Special case for GIFs.  These change frames, so no caching.
            ParsedGIF parsedGIF = animatedGIFs.get(textureLocation);
            return animatedGIFFrames.get(parsedGIF).get(parsedGIF.getCurrentFrame());
        } else if (onlineTextures.containsKey(textureLocation)) {
            //Online texture.
            Identifier onlineTexture = onlineTextures.get(textureLocation);
            return onlineTexture != null ? onlineTexture : MISSING_TEXTURE;
        } else if (textureLocation.equals(RenderableData.GLOBAL_TEXTURE_NAME)) {
            //Default texture.
            return TextureAtlas.LOCATION_BLOCKS;
        } else {
            //Check the cache first to avoid repeated classpath lookups.
            Identifier cached = textureLocations.get(textureLocation);
            if (cached != null) {
                return cached;
            }

            //If the texture has a colon, it's a short-hand form that needs to be converted.
            String formattedLocation = textureLocation;
            if (textureLocation.contains(":")) {
                formattedLocation = "/assets/" + textureLocation.replace(":", "/");
            }

            //Check if the texture exists.  Close the stream immediately, as we only need to verify existence.
            Identifier result;
            InputStream testStream = InterfaceManager.coreInterface.getPackResource(formattedLocation);
            if (testStream != null) {
                try { testStream.close(); } catch (Exception ignored) {}
                //Convert the classpath-location to a domain-location path for MC.
                String domain = formattedLocation.substring("/assets/".length(), formattedLocation.indexOf("/", "/assets/".length()));
                String location = formattedLocation.substring("/assets/".length() + domain.length() + 1);
                result = Identifier.fromNamespaceAndPath(domain, location);
            } else {
                InterfaceManager.coreInterface.logError("Could not find texture: " + formattedLocation + " Reverting to fallback texture.");
                result = MISSING_TEXTURE;
            }
            textureLocations.put(textureLocation, result);
            return result;
        }
    }

    /**
     * Renders all MTS custom GUIs and HUDs into the current GUI render state.
     */
    protected static void renderGUI(GuiGraphicsExtractor mcGUI, int mouseX, int mouseY, int screenWidth, int screenHeight, float partialTicks, boolean updateGUIs) {
        guiExtractor = mcGUI;
        renderingGUI = true;

        //Render main pass, then blended pass.
        for (AGUIBase gui : AGUIBase.activeGUIs) {
            if (updateGUIs || gui.components.isEmpty()) {
                gui.setupComponentsInit(screenWidth, screenHeight);
            }
            gui.render(mouseX, mouseY, false, partialTicks);
            gui.render(mouseX, mouseY, true, partialTicks);

            //Render all stacks.  These have to be in the standard GUI reference frame or they won't render.
            for (GUIComponentItem component : stacksToRender) {
                //Double-check the stack is still present, it might have been un-set since this call.
                if ((WrapperItemStack) component.stackToRender != null) {
                    mcGUI.item(((WrapperItemStack) component.stackToRender).stack, (int) component.translation.x, (int) -component.translation.y);
                }
            }
            stacksToRender.clear();
        }

        guiExtractor = null;
        renderingGUI = false;
    }

    /**
     * Submits MTS GUI geometry to the new GUI render state system.
     * 2D sprites are stored as triangle pairs and are converted to quads here.
     * 3D GUI models (bench previews, instruments) are stored as triangle lists, so
     * they are transformed to the GUI plane and submitted as degenerate quads
     * (a quad with the last vertex repeated is just a triangle).
     */
    private static void renderGUIVertices(RenderableData data) {
        GuiGraphicsExtractor extractor = guiExtractor;
        if (extractor == null) {
            return;
        }

        FloatBuffer vertexBuffer = data.vertexObject.vertices;
        vertexBuffer.rewind();
        float[] vertices = new float[vertexBuffer.remaining()];
        vertexBuffer.get(vertices);
        vertexBuffer.rewind();

        int vertexCount = vertices.length / 8;
        if (vertexCount == 0) {
            return;
        }

        float[] quads;
        int quadCount;
        if (data.vertexObject.cacheVertices) {
            //3D model.  Transform each vertex with the component's full transform and
            //project it onto the GUI plane (MTS is Y-up, the GUI renderer is Y-down).
            Matrix4f matrix = convertMatrix4f(data.transform, matrixScratch);
            int triangleCount = vertexCount / 3;
            if (triangleCount == 0) {
                return;
            }
            float[] triangleDepth = new float[triangleCount];
            float[] triangleVerts = new float[triangleCount * 4 * 4];
            Vector3f transformed = vectorScratch;
            for (int triangle = 0; triangle < triangleCount; ++triangle) {
                float depth = 0;
                for (int vertex = 0; vertex < 3; ++vertex) {
                    int source = (triangle * 3 + vertex) * 8;
                    transformed.set(vertices[source + 5], vertices[source + 6], vertices[source + 7]);
                    matrix.transformPosition(transformed);
                    //Repeat the last vertex to pad the triangle out to a quad.
                    int target = (triangle * 4 + Math.min(vertex, 3)) * 4;
                    triangleVerts[target] = transformed.x;
                    triangleVerts[target + 1] = -transformed.y;
                    triangleVerts[target + 2] = vertices[source + 3];
                    triangleVerts[target + 3] = vertices[source + 4];
                    depth += transformed.z;
                }
                //Pad triangle out to a quad with the final vertex repeated.
                int last = (triangle * 4 + 3) * 4;
                int previous = (triangle * 4 + 2) * 4;
                triangleVerts[last] = triangleVerts[previous];
                triangleVerts[last + 1] = triangleVerts[previous + 1];
                triangleVerts[last + 2] = triangleVerts[previous + 2];
                triangleVerts[last + 3] = triangleVerts[previous + 3];
                triangleDepth[triangle] = depth / 3.0F;
            }

            //Draw farther triangles first.  The GUI pipeline has no depth buffer, so this
            //painter's sort is needed to keep models from looking inside-out.
            Integer[] order = new Integer[triangleCount];
            for (int i = 0; i < triangleCount; ++i) {
                order[i] = i;
            }
            java.util.Arrays.sort(order, java.util.Comparator.comparingDouble(i -> triangleDepth[i]));
            quads = new float[triangleCount * 4 * 4];
            for (int i = 0; i < triangleCount; ++i) {
                System.arraycopy(triangleVerts, order[i] * 4 * 4, quads, i * 4 * 4, 4 * 4);
            }
            quadCount = triangleCount;
        } else {
            //2D sprite.  Triangles are ordered BR, TR, TL, BR, TL, BL.  Quads need BR, TR, TL, BL.
            quadCount = vertexCount / 6;
            if (quadCount == 0) {
                return;
            }
            quads = new float[quadCount * 4 * 4];
            int[] order = new int[] { 0, 1, 2, 5 };
            //Components store their position in a bottom-left origin frame (Y is negated on construction),
            //but the GUI renderer uses a top-left origin.  Apply the component's full transform (this
            //includes scaling, which instruments use to size themselves) and then flip Y.
            Matrix4f matrix = convertMatrix4f(data.transform, matrixScratch);
            Vector3f transformed = vectorScratch;
            for (int quad = 0; quad < quadCount; ++quad) {
                for (int vertex = 0; vertex < 4; ++vertex) {
                    int source = (quad * 6 + order[vertex]) * 8;
                    int target = (quad * 4 + vertex) * 4;
                    transformed.set(vertices[source + 5], vertices[source + 6], vertices[source + 7]);
                    matrix.transformPosition(transformed);
                    quads[target] = transformed.x;
                    quads[target + 1] = -transformed.y;
                    quads[target + 2] = vertices[source + 3];
                    quads[target + 3] = vertices[source + 4];
                }
            }
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < quadCount * 4; ++i) {
            minX = Math.min(minX, quads[i * 4]);
            minY = Math.min(minY, quads[i * 4 + 1]);
            maxX = Math.max(maxX, quads[i * 4]);
            maxY = Math.max(maxY, quads[i * 4 + 1]);
        }

        Identifier texture = getTexture(data.texture);
        AbstractTexture mcTexture = Minecraft.getInstance().getTextureManager().getTexture(texture);
        TextureSetup textureSetup = TextureSetup.singleTexture(mcTexture.getTextureView(), mcTexture.getSampler());
        Matrix3x2f pose = new Matrix3x2f(extractor.pose());
        int color = ((int) (data.alpha * 255) << 24) | ((int) (data.color.red * 255) << 16) | ((int) (data.color.green * 255) << 8) | (int) (data.color.blue * 255);
        //Elements in a stratum get sorted by texture, which re-orders components relative to each other.
        //Put each component in its own stratum so MTS's own draw order is preserved.
        extractor.nextStratum();
        extractor.submitGuiElementRenderState(new MTSGuiElement(pose, textureSetup, quads, quadCount, color, new ScreenRectangle((int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY))));
    }

    /**
     * Converts internal matrix to new external matrix.
     * Note that MC does multiplication operations backwards of us in this version of the game
     * so this is actually the transpose of the passed-in matrix.
     */
    public static Matrix4f convertMatrix4f(TransformationMatrix transform) {
        return convertMatrix4f(transform, new Matrix4f());
    }

    /**Like {@link #convertMatrix4f(TransformationMatrix)}, but writes into the passed-in matrix.*/
    public static Matrix4f convertMatrix4f(TransformationMatrix transform, Matrix4f dest) {
        return dest.set((float) transform.m00, (float) transform.m10, (float) transform.m20, (float) transform.m30, (float) transform.m01, (float) transform.m11, (float) transform.m21, (float) transform.m31, (float) transform.m02, (float) transform.m12, (float) transform.m22, (float) transform.m32, (float) transform.m03, (float) transform.m13, (float) transform.m23, (float) transform.m33);
    }

    /** Render state for the render-forwarding entity. **/
    private static class ForwarderRenderState extends EntityRenderState {
        private BuilderEntityRenderForwarder builder;
        private float partialTicks;
    }

    /** Renderer that forwards MTS entity rendering into the entity submit pass. **/
    private static class ForwarderRenderer extends EntityRenderer<BuilderEntityRenderForwarder, ForwarderRenderState> {
        protected ForwarderRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ForwarderRenderState createRenderState() {
            return new ForwarderRenderState();
        }

        @Override
        public void extractRenderState(BuilderEntityRenderForwarder entity, ForwarderRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.builder = entity;
            state.partialTicks = partialTicks;
        }

        @Override
        public boolean shouldRender(BuilderEntityRenderForwarder builder, Frustum camera, double camX, double camY, double camZ) {
            //Always render the forwarder, no matter where the camera is.
            return true;
        }

        @Override
        public void submit(ForwarderRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            BuilderEntityRenderForwarder builder = state.builder;
            if (builder != null && builder.playerFollowing == Minecraft.getInstance().player) {
                //Set camera offset point for later.
                renderCameraOffset.set(Mth.lerp(state.partialTicks, builder.xOld, builder.getX()), Mth.lerp(state.partialTicks, builder.yOld, builder.getY()), Mth.lerp(state.partialTicks, builder.zOld, builder.getZ()));

                //Set the stack variables and render.
                matrixStack = poseStack;
                renderCollector = collector;
                doRenderCall(false, state.partialTicks);
                matrixStack = null;
                renderCollector = null;
            }
        }
    }

    /** Blank render class used to bypass rendering for all other builders.**/
    private static class BlankRender<T extends ABuilderEntityBase> extends EntityRenderer<T, EntityRenderState> {
        protected BlankRender(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public EntityRenderState createRenderState() {
            return new EntityRenderState();
        }

        @Override
        public boolean shouldRender(T builder, Frustum camera, double camX, double camY, double camZ) {
            return false;
        }
    }

    /**
     * Custom GUI render state for MTS 2D geometry.  Uses the vanilla textured GUI pipeline.
     */
    private static class MTSGuiElement implements GuiElementRenderState {
        private final Matrix3x2f pose;
        private final TextureSetup textureSetup;
        private final float[] quads;
        private final int quadCount;
        private final int color;
        private final ScreenRectangle bounds;

        private MTSGuiElement(Matrix3x2f pose, TextureSetup textureSetup, float[] quads, int quadCount, int color, ScreenRectangle bounds) {
            this.pose = pose;
            this.textureSetup = textureSetup;
            this.quads = quads;
            this.quadCount = quadCount;
            this.color = color;
            this.bounds = bounds;
        }

        @Override
        public void buildVertices(VertexConsumer vertexConsumer) {
            for (int quad = 0; quad < quadCount; ++quad) {
                for (int vertex = 0; vertex < 4; ++vertex) {
                    int offset = (quad * 4 + vertex) * 4;
                    vertexConsumer.addVertexWith2DPose(pose, quads[offset], quads[offset + 1]).setColor(color).setUv(quads[offset + 2], quads[offset + 3]);
                }
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI_TEXTURED;
        }

        @Override
        public TextureSetup textureSetup() {
            return textureSetup;
        }

        @Override
        public ScreenRectangle scissorArea() {
            return null;
        }

        @Override
        public ScreenRectangle bounds() {
            return bounds;
        }
    }
}
