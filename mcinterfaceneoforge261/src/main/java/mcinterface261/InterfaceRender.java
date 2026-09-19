package mcinterface261;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.AGUIComponent;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.instances.GUIHUD;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IInterfaceRender;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.GIFParser.GIFImageFrame;
import minecrafttransportsimulator.rendering.GIFParser.ParsedGIF;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;

/**
 * Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, etc.
 *
 * @author don_bruce
 */
public class InterfaceRender implements IInterfaceRender {
    private static final Map<String, Identifier> onlineTextures = new HashMap<>();
    private static final Map<String, ParsedGIF> animatedGIFs = new HashMap<>();
    private static final Map<ParsedGIF, Map<GIFImageFrame, Identifier>> animatedGIFFrames = new LinkedHashMap<>();

    private static final List<GUIComponentItem> stacksToRender = new ArrayList<>();

    private static final ConcurrentHashMap<String, RenderType> renderTypes = new ConcurrentHashMap<>();
    private static final SequencedMap<RenderType, ByteBufferBuilder> fastRenderFixedBuffers = new LinkedHashMap<>();
    private static final ByteBufferBuilder fastRenderFallbackBuffer = new ByteBufferBuilder(262144);
    private static MultiBufferSource.BufferSource fastRenderBuffer;

    /**Cache for resolved texture Identifiers to avoid repeated classpath lookups via getPackResource().*/
    private static final ConcurrentHashMap<String, Identifier> textureIdentifierCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Identifier> alphaCorrectedTextureIdentifierCache = new ConcurrentHashMap<>();

    @SuppressWarnings("deprecation")
    private static final Identifier BLOCK_TEXTURE_LOCATION = TextureAtlas.LOCATION_BLOCKS;
    private static Identifier MISSING_TEXTURE;
    private static Identifier BLOCK_TEXTURE;
    public static PoseStack matrixStack;
    public static Matrix4f projectionMatrix;
    public static Matrix4f viewMatrix;
    public static MultiBufferSource renderBuffer;
    public static Point3D renderCameraOffset = new Point3D();
    private static boolean renderingGUI;
    private static boolean renderingFastMode;

    /**True when renderGUI has accumulated vertex data that needs to be flushed to the GPU.**/
    public static boolean pendingGUIRender;
    /**Reference to the active GuiGraphicsExtractor stored during extract phase for deferred item rendering.**/
    public static GuiGraphicsExtractor pendingMcGUI;
    private static final List<GuiBatch> pendingGuiBatches = new ArrayList<>();
    private static MultiBufferSource.BufferSource pendingGuiBuffer;
    private static MultiBufferSource.BufferSource pendingTooltipBuffer;
    private static SequencedMap<RenderType, ByteBufferBuilder> activeGuiFixedBuffers;
    private static final SequencedMap<RenderType, ByteBufferBuilder> guiTooltipFixedBuffers = new LinkedHashMap<>();
    private static final ByteBufferBuilder guiTooltipFallbackBuffer = new ByteBufferBuilder(256);
    private static int pendingMouseX;
    private static int pendingMouseY;
    private static final int HUD_BACKGROUND_WIDTH = 400;
    private static final int HUD_BACKGROUND_HEIGHT = 140;
    private static final Identifier HUD_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "textures/guis/hud.png");

    private record GuiBatch(MultiBufferSource.BufferSource buffer, SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers) {
    }

    /**Custom render state that carries entity reference for the forwarder renderer.**/
    private static class ForwarderRenderState extends EntityRenderState {
        BuilderEntityRenderForwarder builder;
    }

    /**
     * Event that's called to setup the client.  We register our render wrapper
     * class here.
     */
    public static void onIVRegisterRenderersEvent(RegisterRenderers event) {
        //Register the global entity rendering class.
        event.registerEntityRenderer(BuilderEntityRenderForwarder.E_TYPE4.get(), manager -> new EntityRenderer<BuilderEntityRenderForwarder, ForwarderRenderState>(manager) {
            @Override
            public ForwarderRenderState createRenderState() {
                return new ForwarderRenderState();
            }

            @Override
            public void extractRenderState(BuilderEntityRenderForwarder entity, ForwarderRenderState state, float partialTicks) {
                super.extractRenderState(entity, state, partialTicks);
                state.builder = entity;
            }

            @Override
            public boolean shouldRender(BuilderEntityRenderForwarder builder, Frustum camera, double camX, double camY, double camZ) {
                //Always render the forwarder, no matter where the camera is.
                return true;
            }

            @Override
            public void submit(ForwarderRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
                if (state.builder != null && state.builder.playerFollowing == Minecraft.getInstance().player && !ConfigSystem.settings.general.forceRenderLastSolid.value) {
                    //Set camera offset point for later.
                    renderCameraOffset.set(
                        Mth.lerp(state.partialTick, state.builder.xOld, state.builder.getX()),
                        Mth.lerp(state.partialTick, state.builder.yOld, state.builder.getY()),
                        Mth.lerp(state.partialTick, state.builder.zOld, state.builder.getZ()));

                    //Set the stack variables and render.
                    matrixStack = stack;
                    renderBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
                    doRenderCall(false, state.partialTick);
                }
            }
        });

        //Register blank classes for the other builders.
        //If we don't, the game crashes when trying to render them.
        event.registerEntityRenderer(BuilderEntityExisting.E_TYPE2.get(), manager -> new BlankRender<BuilderEntityExisting>(manager));
        event.registerEntityRenderer(BuilderEntityLinkedSeat.E_TYPE3.get(), manager -> new BlankRender<BuilderEntityLinkedSeat>(manager));
    }

    @Override
    public float[] getBlockBreakTexture(AWrapperWorld world, Point3D position) {
        //Get normal model.
        BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
        BlockState state = ((WrapperWorld) world).world.getBlockState(pos);
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite();
        return new float[] { sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1() };
    }

    @Override
    public float[] getDefaultBlockTexture(String name) {
        net.minecraft.client.renderer.texture.TextureAtlas atlas = (net.minecraft.client.renderer.texture.TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(BLOCK_TEXTURE_LOCATION);
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = atlas.getSprite(Identifier.parse(name.replace(":", ":blocks/")));
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
        matrixStack.pushPose();
        Matrix4f matrix4f = convertMatrix4f(data.transform);
        Matrix3f matrix3f = new Matrix3f(matrix4f);
        PoseStack.Pose stackEntry = matrixStack.last();
        stackEntry.pose().mul(matrix4f);

        if (data.vertexObject.isLines) {
            stackEntry.normal().mul(matrix3f);
            VertexConsumer buffer = renderBuffer.getBuffer(MTS_LINES);
            while (data.vertexObject.vertices.hasRemaining()) {
                //Read both endpoints of the line segment to compute the direction normal.
                //RenderType.lines() uses the normal to determine screen-space line expansion,
                //so it must be the normalized direction of each line segment, not a fixed value.
                float x1 = data.vertexObject.vertices.get();
                float y1 = data.vertexObject.vertices.get();
                float z1 = data.vertexObject.vertices.get();
                float x2 = data.vertexObject.vertices.get();
                float y2 = data.vertexObject.vertices.get();
                float z2 = data.vertexObject.vertices.get();
                float dx = x2 - x1;
                float dy = y2 - y1;
                float dz = z2 - z1;
                float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len > 0.0F) {
                    dx /= len;
                    dy /= len;
                    dz /= len;
                }
                buffer.addVertex(stackEntry.pose(), x1, y1, z1)
                    .setColor(data.color.red, data.color.green, data.color.blue, data.alpha)
                    .setNormal(stackEntry, dx, dy, dz)
                    .setLineWidth(2.0f);
                buffer.addVertex(stackEntry.pose(), x2, y2, z2)
                    .setColor(data.color.red, data.color.green, data.color.blue, data.alpha)
                    .setNormal(stackEntry, dx, dy, dz)
                    .setLineWidth(2.0f);
            }
            //Rewind buffer for next read.
            data.vertexObject.vertices.rewind();
        } else {
            boolean noDepthWrite = shouldDisableDepthWrite(data);
            boolean particleSprite = isParticleSprite(data);
            boolean hudBackground = renderingGUI && isHUDBackground(data);
            String typeID = data.texture + (data.isTranslucent || hudBackground) + data.lightingMode + data.enableBrightBlending + noDepthWrite + particleSprite + hudBackground;
            //Use a separate cache key for GUI rendering to avoid using wrong depth-test pipeline.
            final String cacheKey = renderingGUI ? "gui_" + typeID : typeID;
            final RenderType renderType = renderTypes.computeIfAbsent(cacheKey, k -> createRenderType("mts_" + (renderingGUI ? "gui_entity_" : "entity_") + typeID, data));
            if (renderingGUI) {
                //Ensure this RenderType has a dedicated fixed buffer before we call getBuffer().
                //Without this, BufferSource uses the single shared slot and eagerly flushes the
                //previous type the moment a new one is requested, discarding GUI geometry.
                activeGuiFixedBuffers.computeIfAbsent(renderType, k -> new ByteBufferBuilder(262144));
            } else if (renderingFastMode && data.vertexObject.cacheVertices) {
                fastRenderFixedBuffers.computeIfAbsent(renderType, k -> new ByteBufferBuilder(2097152));
            }
            stackEntry.normal().mul(matrix3f);
            VertexConsumer buffer = renderBuffer.getBuffer(renderType);
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

                if (renderingGUI) {
                    //GUI pipeline uses POSITION_TEX_COLOR format — no overlay, light, or normal attributes.
                    buffer.addVertex(stackEntry.pose(), posX, posY, posZ)
                        .setUv(texU, texV)
                        .setColor(data.color.red, data.color.green, data.color.blue, data.alpha);
                } else {
                    buffer.addVertex(stackEntry.pose(), posX, posY, posZ)
                        .setColor(data.color.red, data.color.green, data.color.blue, data.alpha)
                        .setUv(texU, texV)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(data.worldLightValue)
                        .setNormal(stackEntry, normalX, normalY, normalZ);
                }
            }
            //Rewind buffer for next read.
            data.vertexObject.vertices.rewind();
        }
        matrixStack.popPose();
    }

    public static void doRenderCall(boolean blendingEnabled, float partialTicks) {
        AWrapperWorld world = InterfaceManager.clientInterface.getClientWorld();
        ConcurrentLinkedQueue<AEntityC_Renderable> allEntities = world.renderableEntities;
        if (allEntities != null) {
            world.beginProfiling("MTSRendering_Setup", true);
            MultiBufferSource previousBuffer = renderBuffer;
            boolean previousFastMode = renderingFastMode;
            if (ConfigSystem.client.renderingSettings.renderingMode.value != 2) {
                if (fastRenderBuffer == null) {
                    fastRenderBuffer = MultiBufferSource.immediateWithBuffers(fastRenderFixedBuffers, fastRenderFallbackBuffer);
                }
                renderBuffer = fastRenderBuffer;
                renderingFastMode = true;
            }

            //NOTE: this operation occurs on a ConcurrentLinkedQueue.  Therefore, updates will
            //not occur one after another.  Sanitize your inputs!
            for (AEntityC_Renderable entity : allEntities) {
                matrixStack.pushPose();
                matrixStack.translate(entity.position.x - renderCameraOffset.x, entity.position.y - renderCameraOffset.y, entity.position.z - renderCameraOffset.z);
                entity.render(blendingEnabled, partialTicks);
                matrixStack.popPose();
            }
            if (renderingFastMode && fastRenderBuffer != null) {
                if (ConfigSystem.client.renderingSettings.renderingMode.value == 0) {
                    for (RenderType renderType : fastRenderFixedBuffers.keySet()) {
                        fastRenderBuffer.endBatch(renderType);
                    }
                } else {
                    fastRenderBuffer.endBatch();
                }
            }
            renderBuffer = previousBuffer;
            renderingFastMode = previousFastMode;

            world.endProfiling();
        }
    }

    //MTS entity pipelines using TRIANGLES topology to match ModelParserOBJ's triangulated output.
    //The stock RenderPipelines.ENTITY_CUTOUT/TRANSLUCENT use QUADS and withCull(false), which both
    //scrambles triangle meshes AND makes back faces visible.  Use the _CULL variants instead, which
    //have face culling enabled by default (matching the explicit setCullState(CULL) from 1211's render types).
    private static final RenderPipeline MTS_ENTITY_CUTOUT = RenderPipelines.ENTITY_CUTOUT_CULL.toBuilder()
        .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "mts/entity_cutout"))
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES)
        .build();
    private static final RenderPipeline MTS_ENTITY_CUTOUT_NO_DEPTH = RenderPipelines.ENTITY_CUTOUT_CULL.toBuilder()
        .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "mts/entity_cutout_no_depth"))
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES)
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build();
    private static final RenderPipeline MTS_ENTITY_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "mts/entity_translucent"))
        .withSampler("Sampler1")
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES)
        .withCull(true)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build();
    private static final RenderPipeline MTS_ENTITY_CUTOUT_NOSHADOWS = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "mts/entity_cutout_noshadows"))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withSampler("Sampler1")
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES)
        .withCull(true)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .build();
    private static final RenderPipeline MTS_ENTITY_CUTOUT_NOSHADOWS_NO_DEPTH = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "mts/entity_cutout_noshadows_no_depth"))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withSampler("Sampler1")
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES)
        .withCull(true)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build();
    private static final RenderPipeline MTS_ENTITY_TRANSLUCENT_NOSHADOWS = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "mts/entity_translucent_noshadows"))
        .withShaderDefine("ALPHA_CUTOUT", 0.01F)
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withSampler("Sampler1")
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES)
        .withCull(true)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build();
    //GUI-specific pipelines using GUI_TEXTURED_SNIPPET (core/position_tex_color shader).
    //This shader has no world-rendering dependencies (no Fog, Lighting, Sampler1/lightmap,
    //Sampler2/overlay uniforms), so it works correctly inside Minecraft's GUI rendering context.
    //TRIANGLES topology overrides the snippet's default QUADS to match MTS's triangulated meshes.
    //withCull(false): renderGUI applies scale(1,-1,1) which flips triangle winding, causing cull to
    //remove MTS front faces instead of back faces.  Disable cull and rely on depth to occlude back faces.
    //LESS_THAN_OR_EQUAL + depthWrite=true: opaque front face writes z; back face fails the depth test.
    private static final RenderPipeline GUI_MTS_TEXTURED = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "gui/mts_textured"))
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
        .withCull(false)
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
        .build();
    //Same cull/depth reasoning as GUI_MTS_TEXTURED; depth write disabled for correct translucent layering.
    private static final RenderPipeline GUI_MTS_TEXTURED_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "gui/mts_textured_translucent"))
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build();
    private static final RenderPipeline GUI_MTS_HUD_BACKGROUND = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "gui/mts_hud_background"))
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .withDepthStencilState(Optional.empty())
        .build();
    //Emissive (fully-lit) cutout pipeline built directly from ENTITY_EMISSIVE_SNIPPET.
    //Does NOT inherit PER_FACE_LIGHTING from ENTITY_TRANSLUCENT_EMISSIVE, so directional shading
    //is not applied — surface renders at full brightness driven only by texture+vertexColor.
    //Does NOT have Sampler2 (lightmap) — do NOT call useLightmap() in createRenderType for this pipeline.
    //Equivalent of mts_entity_lights shader from 1.20.x adapters.
    private static final RenderPipeline MTS_ENTITY_EMISSIVE = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "mts/entity_emissive"))
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withSampler("Sampler1")
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES)
        .withCull(true)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
        .build();
    //Translucent variant: same as above but with translucent blend and depth write disabled.
    private static final RenderPipeline MTS_ENTITY_TRANSLUCENT_EMISSIVE_MTS = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "mts/entity_translucent_emissive"))
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withSampler("Sampler1")
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES)
        .withCull(true)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build();
    //Brightness-blending pipeline — blendFunc(DST_COLOR, SRC_ALPHA) with depth write off.
    //Equivalent of BRIGHTNESS_TRANSPARENCY from 1.20.x: makes objects behind appear brighter
    //based on the alpha of the rendered geometry (used for headlight cones, glow effects).
    private static final RenderPipeline MTS_ENTITY_BRIGHT_BLEND = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "mts/entity_bright_blend"))
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withSampler("Sampler1")
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES)
        .withCull(true)
        .withColorTargetState(new ColorTargetState(new BlendFunction(SourceFactor.DST_COLOR, DestFactor.SRC_ALPHA)))
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build();
    private static final RenderPipeline MTS_ENTITY_BRIGHT_BLEND_NOSHADOWS = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "mts/entity_bright_blend_noshadows"))
        .withShaderDefine("NO_CARDINAL_LIGHTING")
        .withSampler("Sampler1")
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.TRIANGLES)
        .withCull(true)
        .withColorTargetState(new ColorTargetState(new BlendFunction(SourceFactor.DST_COLOR, DestFactor.SRC_ALPHA)))
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build();
    //Lines render type that targets the MAIN framebuffer instead of ITEM_ENTITY_TARGET.
    //Vanilla RenderTypes.LINES uses ITEM_ENTITY_TARGET whose depth buffer is not reliably populated
    //when MTS flushes its bufferSource at TAIL of renderLevel, causing hitboxes to appear through
    //all geometry.  Routing to the main target ensures depth test runs against fully-written terrain.
    private static final RenderType MTS_LINES = RenderType.create(
        "mts_lines",
        RenderSetup.builder(RenderPipelines.LINES)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .createRenderSetup()
    );

    /**
     * Creates a RenderType for MTS entity rendering using the new pipeline-based API.
     */
    private static RenderType createRenderType(String name, RenderableData data) {
        RenderPipeline pipeline;
        boolean noDepthWrite = shouldDisableDepthWrite(data);
        boolean particleSprite = isParticleSprite(data);
        if (renderingGUI) {
            //Use GUI-specific pipelines with depth test disabled to render over the world depth buffer.
            pipeline = isHUDBackground(data) ? GUI_MTS_HUD_BACKGROUND : ((data.isTranslucent || data.enableBrightBlending) ? GUI_MTS_TEXTURED_TRANSLUCENT : GUI_MTS_TEXTURED);
        } else if (data.enableBrightBlending) {
            //Brightness blending: DST_COLOR/SRC_ALPHA blend with depth write off.
            //Equivalent of BRIGHTNESS_TRANSPARENCY from 1.20.x (headlight cones, glow effects).
            pipeline = data.lightingMode.disableWorldLighting ? MTS_ENTITY_BRIGHT_BLEND : MTS_ENTITY_BRIGHT_BLEND_NOSHADOWS;
        } else if (data.lightingMode.disableWorldLighting) {
            //Emissive mode: ignore lightmap so surface renders at full brightness.
            //Equivalent of mts_entity_lights / mts_entity_cutout_noshadows shaders from 1.20.x.
            pipeline = (data.isTranslucent || particleSprite) ? MTS_ENTITY_TRANSLUCENT_EMISSIVE_MTS : MTS_ENTITY_EMISSIVE;
        } else if (data.lightingMode.disableTextureShadows) {
            pipeline = (data.isTranslucent || particleSprite) ? MTS_ENTITY_TRANSLUCENT_NOSHADOWS : (noDepthWrite ? MTS_ENTITY_CUTOUT_NOSHADOWS_NO_DEPTH : MTS_ENTITY_CUTOUT_NOSHADOWS);
        } else {
            //Use MTS-specific pipelines with TRIANGLES topology; stock pipelines use QUADS which corrupts triangle meshes.
            pipeline = (data.isTranslucent || particleSprite) ? MTS_ENTITY_TRANSLUCENT : (noDepthWrite ? MTS_ENTITY_CUTOUT_NO_DEPTH : MTS_ENTITY_CUTOUT);
        }
        RenderSetup.RenderSetupBuilder builder = RenderSetup.builder(pipeline);
        if (!renderingGUI) {
            if (data.lightingMode.disableWorldLighting || data.enableBrightBlending) {
                //These pipelines use ENTITY_EMISSIVE_SNIPPET, which declares Sampler0 and Sampler1 only.
                //Do not bind Sampler2/lightmap here.
                builder.useOverlay();
            } else {
                //Normal cutout/translucent pipelines use ENTITY_SNIPPET, which declares Sampler2.
                builder.useLightmap().useOverlay();
            }
        }
        if (data.texture != null) {
            builder.withTexture("Sampler0", getTextureIdentifier(data.texture, particleSprite));
        }
        return RenderType.create(name, builder.createRenderSetup());
    }

    private static boolean shouldDisableDepthWrite(RenderableData data) {
        return data.isTranslucent || data.enableBrightBlending || data.lightingMode.disableWorldLighting || isParticleSprite(data);
    }

    private static boolean isParticleSprite(RenderableData data) {
        return data.vertexObject.name.equals("2D_TEXTURE") || data.vertexObject.name.equals("PARTICLE_3D");
    }

    private static boolean isHUDBackground(RenderableData data) {
        return data.texture != null && data.texture.contains("textures/guis/hud") && data.transform.m23 <= 0.0;
    }

    @Override
    public void deleteVertices(RenderableData data) {
        if (fastRenderBuffer != null) {
            fastRenderBuffer.endBatch();
        }
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
                Identifier textureLocation = Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "mts-url-" + java.util.UUID.randomUUID().toString().replace("-", ""));
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
                Identifier textureLocation = Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "mts-gif-" + java.util.UUID.randomUUID().toString().replace("-", ""));
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
     * Helper function to get an Identifier for the specified texture location.
     */
    private static Identifier getTextureIdentifier(String textureLocation, boolean correctTransparentRGB) {
        //Check to make sure textures exist.  We delay creating because some mods screw up this stuff in boot.
        if (MISSING_TEXTURE == null) {
            MISSING_TEXTURE = Identifier.parse("mts:textures/rendering/missing.png");
            BLOCK_TEXTURE = BLOCK_TEXTURE_LOCATION;
        }

        if (animatedGIFs.containsKey(textureLocation)) {
            //Special case for GIFs — these change frames, so no caching.
            ParsedGIF parsedGIF = animatedGIFs.get(textureLocation);
            return animatedGIFFrames.get(parsedGIF).get(parsedGIF.getCurrentFrame());
        } else if (onlineTextures.containsKey(textureLocation)) {
            //Online texture.
            Identifier onlineTexture = onlineTextures.get(textureLocation);
            return onlineTexture != null ? onlineTexture : MISSING_TEXTURE;
        } else if (textureLocation.equals(RenderableData.GLOBAL_TEXTURE_NAME)) {
            //Default texture.
            return BLOCK_TEXTURE;
        } else if (correctTransparentRGB) {
            Identifier correctedTexture = alphaCorrectedTextureIdentifierCache.get(textureLocation);
            if (correctedTexture != null) {
                return correctedTexture;
            }

            String formattedLocation = textureLocation;
            if (textureLocation.contains(":")) {
                formattedLocation = "/assets/" + textureLocation.replace(":", "/");
            }

            try (InputStream textureStream = InterfaceManager.coreInterface.getPackResource(formattedLocation)) {
                if (textureStream != null) {
                    NativeImage image = NativeImage.read(textureStream);
                    correctTransparentPixels(image);
                    Identifier textureIdentifier = Identifier.fromNamespaceAndPath(InterfaceLoader.MODID, "alpha_corrected/" + textureLocation.replace(':', '/').replace('\\', '/'));
                    Minecraft.getInstance().getTextureManager().register(textureIdentifier, new DynamicTexture(() -> textureLocation, image));
                    alphaCorrectedTextureIdentifierCache.put(textureLocation, textureIdentifier);
                    return textureIdentifier;
                }
            } catch (Exception e) {
            }
        } else {
            //Check the cache first to avoid repeated classpath lookups.
            Identifier cached = textureIdentifierCache.get(textureLocation);
            if (cached != null) {
                return cached;
            }

            //If the texture has a colon, it's a short-hand form that needs to be converted.
            String formattedLocation = textureLocation;
            if (textureLocation.contains(":")) {
                formattedLocation = "/assets/" + textureLocation.replace(":", "/");
            }

            //Check if the texture exists.  Close the stream immediately — we only need to verify existence.
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
            textureIdentifierCache.put(textureLocation, result);
            return result;
        }

        return getTextureIdentifier(textureLocation, false);
    }

    private static void correctTransparentPixels(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] originalPixels = new int[width * height];
        int[] correctedPixels = new int[width * height];
        boolean[] hasColor = new boolean[width * height];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int index = x + y * width;
                int pixel = image.getPixel(x, y);
                originalPixels[index] = pixel;
                correctedPixels[index] = pixel;
                hasColor[index] = (pixel >>> 24) >= 128;
            }
        }

        for (int pass = 0; pass < width + height; ++pass) {
            boolean changed = false;
            int[] nextPixels = correctedPixels.clone();
            boolean[] nextHasColor = hasColor.clone();
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    int index = x + y * width;
                    if (!hasColor[index]) {
                        int red = 0;
                        int green = 0;
                        int blue = 0;
                        int count = 0;
                        for (int sampleY = Math.max(0, y - 1); sampleY <= Math.min(height - 1, y + 1); ++sampleY) {
                            for (int sampleX = Math.max(0, x - 1); sampleX <= Math.min(width - 1, x + 1); ++sampleX) {
                                int sampleIndex = sampleX + sampleY * width;
                                if (hasColor[sampleIndex]) {
                                    int sample = correctedPixels[sampleIndex];
                                    red += sample & 0xFF;
                                    green += sample >>> 8 & 0xFF;
                                    blue += sample >>> 16 & 0xFF;
                                    ++count;
                                }
                            }
                        }
                        if (count != 0) {
                            nextPixels[index] = (originalPixels[index] & 0xFF000000) | (blue / count << 16) | (green / count << 8) | (red / count);
                            nextHasColor[index] = true;
                            changed = true;
                        }
                    }
                }
            }
            correctedPixels = nextPixels;
            hasColor = nextHasColor;
            if (!changed) {
                break;
            }
        }

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int index = x + y * width;
                int alpha = originalPixels[index] >>> 24;
                if (alpha < 128 && correctedPixels[index] != originalPixels[index]) {
                    image.setPixel(x, y, (originalPixels[index] & 0xFF000000) | (correctedPixels[index] & 0x00FFFFFF));
                }
            }
        }
    }

    /**
     * Renders the main GUI, setting up any transforms or operations as required.
     *
     * This runs during the GUI extract phase (GuiLayerManager / RenderGuiLayerEvent).  We accumulate
     * vertex data into {@link #pendingGuiBuffer} here and render item stacks through mcGUI (which
     * feeds the deferred GuiRenderState so items show when guiRenderer.render() runs).
     *
     * The 3D mesh data accumulated in pendingGuiBuffer is NOT flushed here because guiRenderer.render()
     * has not executed yet — calling RenderType.draw() now would target the wrong render target / frame.
     * Instead we set {@link #pendingGUIRender} = true and let GameRendererMixin call
     * {@link #drawPendingGUI()} after guiRenderer.render() to flush the meshes to the GPU.
     */
    protected static void renderGUI(GuiGraphicsExtractor mcGUI, int mouseX, int mouseY, int screenWidth, int screenHeight, float partialTicks, boolean updateGUIs) {
        matrixStack = new PoseStack();
        matrixStack.pushPose();
        renderingGUI = true;
        pendingMcGUI = mcGUI;
        pendingMouseX = mouseX;
        pendingMouseY = mouseY;
        pendingGuiBatches.clear();

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
            if (gui instanceof GUIHUD && shouldRenderHUDBackground()) {
                renderHUDBackground(screenWidth, screenHeight);
            }
            startPendingGUIBatch();
            gui.render(mouseX, mouseY, false, partialTicks);
            startPendingGUIBatch();
            gui.render(mouseX, mouseY, true, partialTicks);
        
            //Item stacks are deferred: they will be submitted to mcGUI and rendered
            //via a second guiRenderer.render() call in renderItemsAfterGUI(), which runs
            //after drawPendingGUI() so items appear on top of MTS GUI backgrounds.
        
            matrixStack.popPose();
        }
        matrixStack.popPose();
        renderingGUI = false;

        //Signal GameRendererMixin to flush pendingGuiBuffer after guiRenderer.render().
        pendingGUIRender = !AGUIBase.activeGUIs.isEmpty();
    }

    private static void startPendingGUIBatch() {
        activeGuiFixedBuffers = new LinkedHashMap<>();
        pendingGuiBuffer = MultiBufferSource.immediateWithBuffers(activeGuiFixedBuffers, new ByteBufferBuilder(256));
        pendingGuiBatches.add(new GuiBatch(pendingGuiBuffer, activeGuiFixedBuffers));
        renderBuffer = pendingGuiBuffer;
    }

    private static boolean shouldRenderHUDBackground() {
        if (CameraSystem.customCameraOverlay != null) {
            return false;
        }
        return InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON ? ConfigSystem.client.renderingSettings.renderHUD_1P.value : ConfigSystem.client.renderingSettings.renderHUD_3P.value;
    }

    private static void renderHUDBackground(int screenWidth, int screenHeight) {
        int x = (screenWidth - HUD_BACKGROUND_WIDTH) / 2;
        int y = screenHeight - (isFullHUDActive() ? HUD_BACKGROUND_HEIGHT : HUD_BACKGROUND_HEIGHT / 2);
        pendingMcGUI.blit(RenderPipelines.GUI_TEXTURED, HUD_BACKGROUND_TEXTURE, x, y, 0.0F, 0.0F, HUD_BACKGROUND_WIDTH, HUD_BACKGROUND_HEIGHT, 512, 256);
    }

    private static boolean isFullHUDActive() {
        return InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON ? ConfigSystem.client.renderingSettings.fullHUD_1P.value : ConfigSystem.client.renderingSettings.fullHUD_3P.value;
    }

    /**
     * Flushes accumulated GUI vertex data to the GPU.  Called by GameRendererMixin after
     * GuiRenderer.render() has run, so we are in the correct GPU submission phase.
     */
    public static void drawPendingGUI() {
        pendingGUIRender = false;
        for (GuiBatch batch : pendingGuiBatches) {
            batch.buffer.endBatch();
        }
        pendingGuiBatches.clear();
        pendingGuiBuffer = null;
        activeGuiFixedBuffers = null;
    }

    public static void drawPendingTooltips() {
        pendingTooltipBuffer = MultiBufferSource.immediateWithBuffers(guiTooltipFixedBuffers, guiTooltipFallbackBuffer);
        MultiBufferSource previousBuffer = renderBuffer;
        SequencedMap<RenderType, ByteBufferBuilder> previousFixedBuffers = activeGuiFixedBuffers;
        renderBuffer = pendingTooltipBuffer;
        activeGuiFixedBuffers = guiTooltipFixedBuffers;
        matrixStack = new PoseStack();
        matrixStack.pushPose();
        renderingGUI = true;
        matrixStack.scale(1.0F, -1.0F, 1.0F);
        int displayGUIIndex = 0;
        for (AGUIBase gui : AGUIBase.activeGUIs) {
            matrixStack.pushPose();
            if (gui.capturesPlayer()) {
                matrixStack.translate(0, 0, 250);
            } else {
                matrixStack.translate(0, 0, -500 + 250 * displayGUIIndex++);
            }
            for (AGUIComponent component : gui.components) {
                if (component.visible && component.isMouseInBounds(pendingMouseX, pendingMouseY)) {
                    component.renderTooltip(gui, pendingMouseX, pendingMouseY);
                }
            }
            matrixStack.popPose();
        }
        matrixStack.popPose();
        renderingGUI = false;
        pendingTooltipBuffer.endBatch();
        pendingTooltipBuffer = null;
        renderBuffer = previousBuffer;
        activeGuiFixedBuffers = previousFixedBuffers;
    }

    /**
     * Submits deferred item stacks to the now-empty GuiRenderState and triggers a second
     * GuiRenderer pass so items render on top of MTS GUI backgrounds (which were flushed
     * by drawPendingGUI just before this call).
     */
    public static void renderItemsAfterGUI(GuiRenderer guiRenderer, GpuBufferSlice fogBuffer) {
        if (pendingMcGUI == null || stacksToRender.isEmpty()) {
            stacksToRender.clear();
            pendingMcGUI = null;
            return;
        }
        for (GUIComponentItem component : stacksToRender) {
            if ((WrapperItemStack) component.stackToRender != null) {
                net.minecraft.world.item.ItemStack stack = ((WrapperItemStack) component.stackToRender).stack;
                pendingMcGUI.pose().pushMatrix();
                if (component.scale != 1.0) {
                    pendingMcGUI.pose().scale((float) component.scale, (float) component.scale);
                    int ix = (int) (component.translation.x / component.scale);
                    int iy = (int) (-component.translation.y / component.scale) + 1;
                    pendingMcGUI.item(stack, ix, iy);
                    pendingMcGUI.itemDecorations(Minecraft.getInstance().font, stack, ix, iy);
                } else {
                    int ix = (int) component.translation.x;
                    int iy = (int) -component.translation.y;
                    pendingMcGUI.item(stack, ix, iy);
                    pendingMcGUI.itemDecorations(Minecraft.getInstance().font, stack, ix, iy);
                }
                pendingMcGUI.pose().popMatrix();
            }
        }
        stacksToRender.clear();
        pendingMcGUI = null;
        guiRenderer.render(fogBuffer);
    }

    /**
     * Converts internal matrix to new external matrix.
     * Note that MC does multiplication operations backwards of us in this version of the game
     * so this is actually the transpose of the passed-in matrix.
     */
    public static Matrix4f convertMatrix4f(TransformationMatrix transform) {
        return new Matrix4f((float) transform.m00, (float) transform.m10, (float) transform.m20, (float) transform.m30, (float) transform.m01, (float) transform.m11, (float) transform.m21, (float) transform.m31, (float) transform.m02, (float) transform.m12, (float) transform.m22, (float) transform.m32, (float) transform.m03, (float) transform.m13, (float) transform.m23, (float) transform.m33);
    }
    

    /** Blank render class used to bypass rendering for all other builders.**/
    private static class BlankRender<T extends ABuilderEntityBase> extends EntityRenderer<T, EntityRenderState> {

        protected BlankRender(EntityRendererProvider.Context p_i46179_1_) {
            super(p_i46179_1_);
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

}
