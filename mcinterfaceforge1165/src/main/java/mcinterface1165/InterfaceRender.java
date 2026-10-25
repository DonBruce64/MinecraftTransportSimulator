package mcinterface1165;

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
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
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
import minecrafttransportsimulator.mcinterface.IInterfaceDynamicLightingGL;
import minecrafttransportsimulator.mcinterface.IInterfaceRender;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.GIFParser.GIFImageFrame;
import minecrafttransportsimulator.rendering.GIFParser.ParsedGIF;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightBlockShapes;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightFrustum;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightShadowAtlas;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightSystem;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightVoxelWindow;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
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
import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
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

    private static final Map<String, RenderType> renderTypes = new HashMap<>();
    private static final Map<RenderableData, BufferData> buffers = new HashMap<>();
    private static final Map<RenderType, List<RenderData>> queuedRenders = new HashMap<>();
    private static final ConcurrentLinkedQueue<BufferData> removedRenders = new ConcurrentLinkedQueue<>();

    private static RenderState.TextureState MISSING_STATE;
    private static RenderState.TextureState BLOCK_STATE;
    private static MatrixStack matrixStack;
    public static Matrix4f projectionMatrix;
    private static IRenderTypeBuffer renderBuffer;
    public static Point3D renderCameraOffset = new Point3D();
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
    public String getDefaultFontTextureFolder() {
        return "/assets/minecraft/textures/font";
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
    public void renderVertices(RenderableData data, boolean changedSinceLastRender) {
        matrixStack.pushPose();
        Matrix4f matrix4f = convertMatrix4f(data.transform);
        MatrixStack.Entry stackEntry = matrixStack.last();
        stackEntry.pose().multiply(matrix4f);

        if (data.vertexObject.isLines) {
            IVertexBuilder buffer = renderBuffer.getBuffer(RenderType.lines());
            while (data.vertexObject.vertices.hasRemaining()) {
                buffer.vertex(stackEntry.pose(), data.vertexObject.vertices.get(), data.vertexObject.vertices.get(), data.vertexObject.vertices.get());
                buffer.color(data.color.red, data.color.green, data.color.blue, data.alpha);
                buffer.endVertex();
            }
            //Rewind buffer for next read.
            data.vertexObject.vertices.rewind();
        } else {
            String typeID = data.texture + data.isTranslucent + data.lightingMode + data.enableBrightBlending;
            final RenderType renderType;
            if (data.vertexObject.cacheVertices && !renderingGUI && ConfigSystem.client.renderingSettings.renderingMode.value != 2) {
            	//Get the render type and data buffer for this entity.
                renderType = renderTypes.computeIfAbsent(typeID, k -> CustomRenderType.create("mts_entity", DefaultVertexFormats.NEW_ENTITY, 7, 2097152, true, data.isTranslucent, CustomRenderType.createForObject(data).createCompositeState(false)));
                BufferData buffer = buffers.computeIfAbsent(data, k -> new BufferData(renderType, data));

                //Reset buffer if it's not ready.
                if (changedSinceLastRender) {
                    buffer.builder.clear();
                    buffer.isReady = false;
                }
                if (!buffer.isReady) {
                    int index = 0;
                    buffer.radius = 0;
                    buffer.vertexCount = 0;
                    buffer.builder.begin(GL11.GL_QUADS, renderType.format());
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
                        buffer.radius = Math.max(buffer.radius, posX * posX + posY * posY + posZ * posZ);

                        //Add the vertex format bits.
                        do {
                            buffer.builder.vertex(posX, posY, posZ, data.color.red, data.color.green, data.color.blue, data.alpha, texU, texV, OverlayTexture.NO_OVERLAY, data.worldLightValue, normalX, normalY, normalZ);
                            buffer.vertexCount++;
                        } while (++index == 3);
                        if (index == 4) {
                            index = 0;
                        }
                    }
                    buffer.isReady = true;
                    buffer.radius = (float) Math.sqrt(buffer.radius);
                    buffer.builder.end();
                    buffer.buffer.upload(buffer.builder);
                    data.vertexObject.vertices.rewind();
                }

                //Add this buffer to the list to render later.
                List<RenderData> renders = queuedRenders.get(renderType);
                if (renders == null) {
                    renders = new ArrayList<>();
                    queuedRenders.put(renderType, renders);
                }
                renders.add(new RenderData(stackEntry.pose(), buffer));
            } else {
                renderType = renderTypes.computeIfAbsent(typeID, k -> CustomRenderType.create("mts_entity", DefaultVertexFormats.NEW_ENTITY, 7, 256, true, data.isTranslucent, CustomRenderType.createForObject(data).createCompositeState(false)));
                IVertexBuilder buffer = renderBuffer.getBuffer(renderType);
                
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
                        buffer.vertex(stackEntry.pose(), posX, posY, posZ);
                        buffer.color(data.color.red, data.color.green, data.color.blue, data.alpha);
                        buffer.uv(texU, texV);
                        buffer.overlayCoords(OverlayTexture.NO_OVERLAY);
                        buffer.uv2(data.worldLightValue);
                        buffer.normal(stackEntry.normal(), normalX, normalY, normalZ);
                        buffer.endVertex();
                    } while (++index == 3);
                    if (index == 4) {
                        index = 0;
                    }
                }
                //Rewind buffer for next read.
                data.vertexObject.vertices.rewind();
                if (data.vertexObject.cacheVertices && !renderingGUI && DynamicLightShadowAtlas.isCapturing()) {
                    //Slow mode: no cached buffer to capture, so dynamic light shadows copy the vertices as drawn, with
                    //the texture they are drawn with (none for a blended type).
                    int shadowTexture = InterfaceDynamicLightingGL.shadowTexture(renderType);
                    if (shadowTexture >= 0) {
                        toColumnMajor(stackEntry.pose(), dynamicLightPose);
                        DynamicLightShadowAtlas.captureVertices(data.vertexObject.vertices, dynamicLightPose, shadowTexture);
                    }
                }
            }
        }
        matrixStack.popPose();
    }

    @Override
    public void deleteVertices(RenderableData data) {
        if (data.vertexObject.cacheVertices) {
	    	//Add to removed render list, we should only remove renders AFTER they are rendered.
	    	//This ensures they are un-bound, if the were bound prior.
            //Make sure we actually bound a buffer; just because the main system asks for a bound buffer,
    	    //doesn't mean we actually can give it one.  GUI models are one such case, as they don't work right
            //with bound buffers due to matrix differences.
            BufferData buffer = buffers.remove(data);
            if (buffer != null) {
                removedRenders.add(buffer);
            }
    	}
    }

    @Override
    public int getLightingAtPosition(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        //Under a shader pack, dynamic lights raise block light: IV models take it like the world's meshes do.
        int blockLight = Math.max(Minecraft.getInstance().level.getBrightness(LightType.BLOCK, pos), DynamicLightSystem.blockLightAt(pos.getX(), pos.getY(), pos.getZ()));
        return LightTexture.pack(blockLight, Minecraft.getInstance().level.getBrightness(LightType.SKY, pos));
    }

    /**
     * Called at the start of the world render: forgets the last frame's shadow body captures, which a frame without
     * a light pass never consumed.
     */
    public static void startDynamicLightFrame() {
        DynamicLightShadowAtlas.frameEnd();
    }

    /**
     * Builds and draws this frame's dynamic lights; call after the solid passes and before IV's blended pass.
     * {@code pose} is the world render's camera rotation, {@code projection} its projection.  1.16.5 keeps no frustum
     * or camera side vectors to borrow, so the frustum is rebuilt from the same two matrices and the left vector is
     * the camera's up crossed with its look direction.
     */
    @SuppressWarnings("deprecation")
    public static void renderDynamicLightFrame(Matrix4f pose, Matrix4f projection, ActiveRenderInfo camera, float partialTicks) {
        Vector3d position = camera.getPosition();
        ClippingHelper frustum = new ClippingHelper(pose, projection);
        frustum.prepare(position.x, position.y, position.z);
        DynamicLightFrustum test = (minX, minY, minZ, maxX, maxY, maxZ) -> frustum.isVisible(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        dynamicLightLeft[0] = up.y() * look.z() - up.z() * look.y();
        dynamicLightLeft[1] = up.z() * look.x() - up.x() * look.z();
        dynamicLightLeft[2] = up.x() * look.y() - up.y() * look.x();
        dynamicLightUp[0] = up.x();
        dynamicLightUp[1] = up.y();
        dynamicLightUp[2] = up.z();
        toColumnMajor(pose, dynamicLightView);
        toColumnMajor(projection, dynamicLightProjection);

        //The passes draw with their own programs, but 1.16.5's fixed-function alpha test still discards their zero-alpha
        //fragments: off for the frame, then back to what MC had.
        boolean alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        RenderSystem.disableAlphaTest();
        try {
            DynamicLightSystem.renderFrame(partialTicks, test, position.x, position.y, position.z, dynamicLightLeft, dynamicLightUp, dynamicLightView, dynamicLightProjection);
        } finally {
            if (alphaTest) {
                RenderSystem.enableAlphaTest();
            }
        }
    }

    /** MC's matrix as 16 floats, column-major (its {@code store} writes the OpenGL layout). */
    private static void toColumnMajor(Matrix4f matrix, float[] out) {
        matrixStaging.clear();
        matrix.store(matrixStaging);
        matrixStaging.rewind();
        matrixStaging.get(out, 0, 16);
    }

    private static final FloatBuffer matrixStaging = BufferUtils.createFloatBuffer(16);
    private static final float[] dynamicLightLeft = new float[3];
    private static final float[] dynamicLightUp = new float[3];
    private static final float[] dynamicLightView = new float[16];
    private static final float[] dynamicLightProjection = new float[16];
    /** A drawn buffer's model-view for {@code DynamicLightShadowAtlas.captureBody}, which copies it. */
    private static final float[] dynamicLightPose = new float[16];
    /** Whether the buffers being drawn are the solid pass's, for the shadow atlas to keep. */
    private static boolean capturingBodies;

    @Override
    public IInterfaceDynamicLightingGL getDynamicLightGL() {
        return InterfaceDynamicLightingGL.supported() ? InterfaceDynamicLightingGL.INSTANCE : null;
    }

    @Override
    public float getDaylight(AWrapperWorld world, Point3D position) {
        World mcWorld = ((WrapperWorld) world).world;
        if (!(mcWorld instanceof ClientWorld)) {
            return MathHelper.clamp(world.getLightBrightness(position, false), 0F, 1F);
        }
        float open = mcWorld.getBrightness(LightType.SKY, new BlockPos(position.x, position.y, position.z)) / 15F;
        //getSkyDarken(partial) runs 0.2 (night) .. 1 (clear noon); sky light alone is 15 at midnight too.
        float sun = MathHelper.clamp((((ClientWorld) mcWorld).getSkyDarken(1F) - 0.2F) / 0.8F, 0F, 1F);
        return open * sun;
    }

    @Override
    public float getRainLevel(AWrapperWorld world, float partialTicks) {
        return ((WrapperWorld) world).world.getRainLevel(partialTicks);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean readDynamicLightSection(AWrapperWorld wrapper, int sx, int sy, int sz, DynamicLightVoxelWindow window) {
        World world = ((WrapperWorld) wrapper).world;
        //1.16.5 worlds are 16 sections tall from y = 0; anything else is air.
        ChunkSection section = null;
        Chunk chunk = world.getChunkSource().getChunk(sx, sz, false);
        if (chunk != null && sy >= 0 && sy < chunk.getSections().length) {
            section = chunk.getSections()[sy];
        }
        int x0 = sx << 4;
        int y0 = sy << 4;
        int z0 = sz << 4;
        lightProbe.set(x0, y0, z0);
        //A section of one kind (air, stone) is filled whole from its palette.
        if (section == null || section.isEmpty() || !section.maybeHas(state -> shadowByte(world, state, window) != 0)) {
            window.fillSection(sx, sy, sz, false);
            return true;
        }
        if (!section.maybeHas(state -> shadowByte(world, state, window) != FULL_SHADOW)) {
            window.fillSection(sx, sy, sz, true);
            return true;
        }
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    lightProbe.set(x0 + x, y0 + y, z0 + z);
                    window.setShape(x0 + x, y0 + y, z0 + z, shadowByte(world, section.getBlockState(x, y, z), window));
                }
            }
        }
        return true;
    }

    /**
     * {@code state}'s byte in the dynamic light grid, worked out once per state at {@link #lightProbe}: a full block
     * casts a full shadow; one that is not (slabs, stairs, fences, doors, hoppers, bells, banners, signs, mob heads,
     * candles) casts its outline shape's; none for air, IV's own blocks, blocks without a block model and without a
     * tile entity to draw them (liquids, barriers), see-through blocks (glass, leaves, bars and panes, the translucent
     * layer, ladders, scaffolding and other climbables, spawners), blocks with neither a collision shape nor a solid
     * layer (plants, torches, rails) and blocks drawn at a random offset.
     */
    @SuppressWarnings("deprecation")
    private static int shadowByte(World world, BlockState state, DynamicLightVoxelWindow window) {
        DynamicLightBlockShapes shapes = window.shapes();
        int value = shapes.byteOf(state);
        if (value < 0) {
            shapes.begin();
            if (state.isAir() || state.getBlock() instanceof BuilderBlock || (state.getRenderShape() == BlockRenderType.INVISIBLE && !state.hasTileEntity())) {
                //No boxes: IV's own blocks are drawn by the entity renderer, whose bodies cast the shadow; liquids,
                //barriers and other blocks without a block model, even full ones, show nothing to shade with.  A tile
                //entity block (sign, mob head, banner) is invisible to the chunk mesh too, but its tile entity renderer
                //draws it, so it goes on to cast its outline.
            } else if (state.isSolidRender(world, lightProbe)) {
                shapes.box(0, 0, 0, 1, 1, 1);
            } else if (castsShapedShadow(world, state)) {
                for (AxisAlignedBB box : state.getShape(world, lightProbe).toAabbs()) {
                    shapes.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
                }
            }
            value = window.endShape(state);
        }
        return value;
    }

    /** Whether {@code state}, visible but not a full block, casts its shape's shadow: see {@link #shadowByte}. */
    @SuppressWarnings("deprecation")
    private static boolean castsShapedShadow(World world, BlockState state) {
        //Climbables and spawners are mostly holes, which the grid cannot see: their box would shade as a wall.
        if (state.getBlock().getOffsetType() != AbstractBlock.OffsetType.NONE || state.getMaterial() == Material.GLASS || state.is(BlockTags.LEAVES) || state.is(BlockTags.CLIMBABLE) || state.is(Blocks.SPAWNER)) {
            return false;
        }
        if (state.getRenderShape() == BlockRenderType.ENTITYBLOCK_ANIMATED) {
            return true;
        }
        //Bars and panes (glass panes, stained or not) are mostly holes; they share the cutout mipped layer with solid
        //things like hoppers, so the layer cannot tell them apart.
        if (state.getBlock() instanceof PaneBlock || RenderTypeLookup.canRenderInLayer(state, RenderType.translucent())) {
            return false;
        }
        return RenderTypeLookup.canRenderInLayer(state, RenderType.solid()) || !state.getCollisionShape(world, lightProbe).isEmpty();
    }

    /** {@link #shadowByte} of a full block. */
    private static final int FULL_SHADOW = DynamicLightVoxelWindow.SOLID & 0xFF;
    /** Scratch position the block shape queries run at; the dynamic light grid is read on the render thread only. */
    private static final BlockPos.Mutable lightProbe = new BlockPos.Mutable();

    @Override
    public void markLightSectionsDirty(int[] boxes, int count) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        //Grown by one block: a face takes the light of the block beside it.  1.16.5 worlds have sections 0..15 only.
        for (int i = 0, o = 0; i < count; i++, o += 6) {
            int x0 = (boxes[o] - 1) >> 4, x1 = (boxes[o + 3] + 1) >> 4;
            int y0 = Math.max(0, (boxes[o + 1] - 1) >> 4), y1 = Math.min(15, (boxes[o + 4] + 1) >> 4);
            int z0 = (boxes[o + 2] - 1) >> 4, z1 = (boxes[o + 5] + 1) >> 4;
            for (int sx = x0; sx <= x1; sx++) {
                for (int sy = y0; sy <= y1; sy++) {
                    for (int sz = z0; sz <= z1; sz++) {
                        Minecraft.getInstance().levelRenderer.setSectionDirty(sx, sy, sz);
                    }
                }
            }
        }
    }

    @Override
    public boolean shouldRenderBoundingBoxes() {
        return Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes();
    }

    @Override
    public boolean bindURLTexture(String textureURL, InputStream stream) {
        if (stream != null) {
            try {
                NativeImage image = NativeImage.read(NativeImage.PixelFormat.RGB, stream);
                DynamicTexture texture = new DynamicTexture(image);
                ResourceLocation textureLocation = Minecraft.getInstance().textureManager.register("mts-url", texture);
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
        Map<GIFImageFrame, ResourceLocation> gifFrameIndexes = new HashMap<>();
        for (GIFImageFrame frame : gif.frames.values()) {
            try {
                BufferedImage frameBuffer = frame.getImage();
                ByteArrayOutputStream frameArrayStream = new ByteArrayOutputStream();
                ImageIO.write(frameBuffer, "gif", frameArrayStream);
                InputStream frameStream = new ByteArrayInputStream(frameArrayStream.toByteArray());

                NativeImage image = NativeImage.read(NativeImage.PixelFormat.RGB, frameStream);
                DynamicTexture texture = new DynamicTexture(image);
                ResourceLocation textureLocation = Minecraft.getInstance().textureManager.register("mts-gif", texture);
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
    @SuppressWarnings("deprecation")
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
        //Register the global entity rendering class.
        RenderingRegistry.registerEntityRenderingHandler(BuilderEntityRenderForwarder.E_TYPE4.get(), manager -> new EntityRenderer<BuilderEntityRenderForwarder>(manager) {
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
                if (builder.playerFollowing == Minecraft.getInstance().player && !ConfigSystem.settings.general.forceRenderLastSolid.value) {
                    //Set camera offset point for later.
                    renderCameraOffset.set(MathHelper.lerp(partialTicks, builder.xOld, builder.getX()), MathHelper.lerp(partialTicks, builder.yOld, builder.getY()), MathHelper.lerp(partialTicks, builder.zOld, builder.getZ()));

                    //Set the stack variables and render.
                    doRenderCall(stack, buffer, false, partialTicks);
                }
            }
        });

        //Register blank classes for the other builders.
        //If we don't, the game crashes when trying to render them.
        RenderingRegistry.registerEntityRenderingHandler(BuilderEntityExisting.E_TYPE2.get(), manager -> new BlankRender<BuilderEntityExisting>(manager));
        RenderingRegistry.registerEntityRenderingHandler(BuilderEntityLinkedSeat.E_TYPE3.get(), manager -> new BlankRender<BuilderEntityLinkedSeat>(manager));
    }

    public static void doRenderCall(MatrixStack stack, IRenderTypeBuffer buffer, boolean blendingEnabled, float partialTicks) {
        matrixStack = stack;
        renderBuffer = buffer;
        AWrapperWorld world = InterfaceManager.clientInterface.getClientWorld();
        ConcurrentLinkedQueue<AEntityC_Renderable> allEntities = world.renderableEntities;
        if (allEntities != null) {
            world.beginProfiling("MTSRendering_Setup", true);

            boolean captureBodies = !blendingEnabled && DynamicLightSystem.getMode() == DynamicLightSystem.Mode.DEFERRED && ConfigSystem.client.renderingSettings.DynL_shadows.value;

            //NOTE: this operation occurs on a ConcurrentLinkedQueue.  Therefore, updates will
            //not occur one after another.  Sanitize your inputs!
            //The Slow mode draws its vertices here, without buffers: dynamic light shadows copy them as they go.
            DynamicLightShadowAtlas.setCapturing(captureBodies && ConfigSystem.client.renderingSettings.renderingMode.value == 2);
            for (AEntityC_Renderable entity : allEntities) {
                matrixStack.pushPose();
                matrixStack.translate(entity.position.x - renderCameraOffset.x, entity.position.y - renderCameraOffset.y, entity.position.z - renderCameraOffset.z);
                entity.render(blendingEnabled, partialTicks);
                matrixStack.popPose();
            }
            DynamicLightShadowAtlas.setCapturing(false);

            //Need to tell the immediate buffer  it's done rendering, else it'll hold onto the data and crash other systems.
            if (renderBuffer instanceof IRenderTypeBuffer.Impl) {
                ((IRenderTypeBuffer.Impl) renderBuffer).endBatch();
            }

            //Now do the actual render.  The solid pass's buffers are also the bodies dynamic light shadows draw.
            world.beginProfiling("MTSRendering_Execution", false);
            capturingBodies = captureBodies;
            DynamicLightShadowAtlas.setCapturing(capturingBodies);
            renderBuffers(blendingEnabled);
            DynamicLightShadowAtlas.setCapturing(false);
            capturingBodies = false;
            world.endProfiling();
        }
    }

    /**
     * Draws the queued cached buffers.  Removed buffers are closed only after the blended pass, since the dynamic
     * light shadows between the passes may still draw a buffer captured in the solid pass.
     */
    private static void renderBuffers(boolean closeRemoved) {
        //Call order is CRITICAL and will lead to random JME faults with no stacktrace if modified!
        for (Entry<RenderType, List<RenderData>> renderEntry : queuedRenders.entrySet()) {
            RenderType renderType = renderEntry.getKey();
            List<RenderData> datas = renderEntry.getValue();
            if (!datas.isEmpty()) {
                renderType.setupRenderState();
                //The texture its bodies cast dynamic light shadows through, -1 for none; read after the setup, which
                //loads it.
                int shadowTexture = capturingBodies ? InterfaceDynamicLightingGL.shadowTexture(renderType) : -1;
                for (RenderData data : datas) {
                    data.bufferData.buffer.bind();
                    renderType.format().setupBufferState(0L);
                    data.bufferData.buffer.draw(data.matrix, GL11.GL_QUADS);
                    if (shadowTexture >= 0) {
                        toColumnMajor(data.matrix, dynamicLightPose);
                        DynamicLightShadowAtlas.captureBody(data.bufferData, dynamicLightPose, data.bufferData.radius, shadowTexture);
                    }
                }
                renderType.format().clearBufferState();
                renderType.clearRenderState();
                datas.clear();
            }
        }
        VertexBuffer.unbind();
        if (closeRemoved && !removedRenders.isEmpty()) {
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

        private static Builder createForObject(RenderableData data) {
            //Create the state builder.  Changed states are active, default states are commented to save processing but still show the state.
            RenderType.State.Builder stateBuilder = RenderType.State.builder();

            stateBuilder.setTextureState(data.texture != null ? getTexture(data.texture) : NO_TEXTURE);
            //Transparency is also blend function, so we need to override that with a custom one if we are doing bright blending.
            stateBuilder.setTransparencyState(data.enableBrightBlending ? BRIGHTNESS_TRANSPARENCY : (data.isTranslucent ? PROPER_TRANSLUCENT_TRANSPARENCY : RenderType.NO_TRANSPARENCY));
            //Diffuse lighting is the ambient lighting that auto-shades models.
            stateBuilder.setDiffuseLightingState(data.lightingMode.disableTextureShadows ? NO_DIFFUSE_LIGHTING : DIFFUSE_LIGHTING);
            //Always smooth shading.
            stateBuilder.setShadeModelState(SMOOTH_SHADE);
            //Use default alpha to remove alpha fragments in cut-out textures.
            stateBuilder.setAlphaState(DEFAULT_ALPHA);
            //Depth test is fine, it ensures translucent things don't render in front of everything.
            //stateBuilder.setDepthTestState(LEQUAL_DEPTH_TEST);
            //Cull is fine.  Not sure what this does, actually...
            //stateBuilder.setCullState(NO_CULL);
            //Lightmap is on unless we are bright.
            stateBuilder.setLightmapState(data.lightingMode.disableWorldLighting ? NO_LIGHTMAP : LIGHTMAP);
            //No overlays ever.
            stateBuilder.setOverlayState(NO_OVERLAY);
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
        private final BufferData bufferData;

        private RenderData(Matrix4f matrix, BufferData bufferData) {
            this.matrix = new Matrix4f(matrix);
            this.bufferData = bufferData;
        }
    }

    /** A cached quad buffer; the shadow atlas draws it too ({@link InterfaceDynamicLightingGL#drawBody}). */
    static class BufferData {
        final BufferBuilder builder;
        final VertexBuffer buffer;
        boolean isReady;
        /** Bounding radius of the vertices around their origin; the squared radius while uploading. */
        float radius;
        /** Vertices in the buffer (a quad's repeated corner included) and the size of one in bytes. */
        int vertexCount;
        final int stride;

        private BufferData() {
            builder = null;
            buffer = null;
            stride = 0;
        }

        private BufferData(RenderType type, RenderableData data) {
            int vertices = data.vertexObject.vertices.limit() / 8;
            //Convert verts to faces, then back to quad-verts for MC rendering.
            //Add one face extra, since MC will want to increase the buffer if sees it can't handle another vert.
            vertices = ((vertices / 3) + 1) * 4;
            this.builder = new BufferBuilder(type.format().getIntegerSize() * vertices);
            this.buffer = new VertexBuffer(type.format());
            this.stride = type.format().getVertexSize();
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
