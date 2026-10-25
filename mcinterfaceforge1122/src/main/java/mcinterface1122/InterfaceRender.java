package mcinterface1122;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IInterfaceDynamicLightingGL;
import minecrafttransportsimulator.mcinterface.IInterfaceRender;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.GIFParser.GIFImageFrame;
import minecrafttransportsimulator.rendering.GIFParser.ParsedGIF;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightBlockShapes;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightFrustum;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightMatrix;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightShadowAtlas;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightSystem;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightVoxelWindow;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, etc.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceRender implements IInterfaceRender {
    private static final DoubleBuffer buffer = ByteBuffer.allocateDirect(16 * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
    private static final Map<String, ResourceLocation> internalTextures = new HashMap<>();
    private static final Map<String, Integer> onlineTextures = new HashMap<>();
    private static final Map<String, ParsedGIF> animatedGIFs = new HashMap<>();
    private static final Map<ParsedGIF, Map<GIFImageFrame, Integer>> animatedGIFFrames = new LinkedHashMap<>();
    private static final List<GUIComponentItem> stacksToRender = new ArrayList<>();
    private static float lastLightmapX;
    private static float lastLightmapY;
    private static final ResourceLocation MISSING_TEXTURE = new ResourceLocation("mts:textures/rendering/missing.png");
    private static final Map<RenderableVertices, Set<RenderableData>> objectMap = new HashMap<>();
    private static final Map<RenderableVertices, Integer> cachedIndexMap = new HashMap<>();
    protected static int lastRenderPassActualPass;
    /** Bounding radius of each cached display list's vertices around its origin, for the dynamic light shadow bodies. */
    private static final Map<RenderableVertices, Float> cachedRadiusMap = new HashMap<>();
    private static final FloatBuffer modelViewBuffer = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);
    private static final float[] dynamicLightPose = new float[16];
    private static final float[] dynamicLightView = new float[16];
    private static final float[] dynamicLightProjection = new float[16];
    private static final float[] dynamicLightLeft = new float[3];
    private static final float[] dynamicLightUp = new float[3];
    private static final float[] dynamicLightClip = new float[16];
    /** Whether the solid pass's cached models are being captured as dynamic light shadow bodies. */
    private static boolean capturingBodies;
    private static long dynamicLightFrame;
    private static long dynamicLightFrameDrawn = -1;

    @Override
    public float[] getBlockBreakTexture(AWrapperWorld world, Point3D position) {
        //Get normal model.
        IBlockState state = ((WrapperWorld) world).world.getBlockState(new BlockPos(position.x, position.y, position.z));
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getTexture(state);
        if (sprite != null) {
            return new float[] { sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV() };
        } else {
            return new float[] { 0, 0, 0, 0 };
        }
    }

    @Override
    public float[] getDefaultBlockTexture(String name) {
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getTextureMap().getAtlasSprite(name.replace(":", ":blocks/"));
        return new float[]{sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV()};
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
            return Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(domain, location)).getInputStream();
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
        if (data.lightingMode.disableWorldLighting || data.vertexObject.isLines) {
            setLightingState(false);
        }
        if (data.lightingMode.disableTextureShadows) {
            setSystemLightingState(false);
        }
        if (data.enableBrightBlending) {
            setBlendBright(true);
        }
        //The GL texture drawn with, which a dynamic light shadow body cuts its faces by; 0 for none (casts whole).
        int texture = 0;
        if (data.texture != null) {
            texture = bindTexture(data.texture);
        } else {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }
        GlStateManager.color(data.color.red, data.color.green, data.color.blue, data.alpha);
        if (!data.lightingMode.disableWorldLighting && !data.vertexObject.isLines) {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, data.worldLightValue % 65536, data.worldLightValue / 65536);
        }

        GL11.glPushMatrix();
        applyTransformOpenGL(data.transform);
        if (data.vertexObject.cacheVertices && ConfigSystem.client.renderingSettings.renderingMode.value != 2) {
        	//Add entity to the rendering mapping once rendered.
            objectMap.computeIfAbsent(data.vertexObject, k -> new HashSet<>()).add(data);
            int cachedVertexIndex = cachedIndexMap.computeIfAbsent(data.vertexObject, k -> {
                int newIndex = GL11.glGenLists(1);
                GL11.glNewList(newIndex, GL11.GL_COMPILE);
                renderVertices(data.vertexObject.vertices, InterfaceDynamicLightingGL.supported());
                GL11.glEndList();
                cachedRadiusMap.put(data.vertexObject, getVerticesRadius(data.vertexObject.vertices));
                return newIndex;
            });
            GL11.glCallList(cachedVertexIndex);
            if (capturingBodies && !isBlended(data)) {
                readModelView();
                DynamicLightShadowAtlas.captureBody(cachedVertexIndex, dynamicLightPose, cachedRadiusMap.get(data.vertexObject), texture);
            }
        } else if (data.vertexObject.isLines) {
            renderLines(data.vertexObject.vertices);
        } else {
            renderVertices(data.vertexObject.vertices, false);
            if (capturingBodies && data.vertexObject.cacheVertices && !isBlended(data)) {
                //The Slow rendering mode has no cached list to record: the vertices just written are the shadow body.
                readModelView();
                DynamicLightShadowAtlas.captureVertices(data.vertexObject.vertices, dynamicLightPose, texture);
            }
        }
        GL11.glPopMatrix();

        if (data.texture == null) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
        if (data.lightingMode.disableWorldLighting || data.lightingMode.disableTextureShadows || data.vertexObject.isLines) {
            setLightingState(true);
        }
        if (data.enableBrightBlending) {
            setBlendBright(false);
        }
    }

    /**
     * Whether {@code data} draws blended (translucent or bright blending): such faces hide nothing, so they are no
     * dynamic light shadow body.
     */
    private static boolean isBlended(RenderableData data) {
        return data.isTranslucent || data.enableBrightBlending;
    }

    /** Reads the current model-view (the camera's, the entity's offset and the object's transform) into {@link #dynamicLightPose}. */
    private static void readModelView() {
        modelViewBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewBuffer);
        modelViewBuffer.get(dynamicLightPose);
    }

    @Override
    public void deleteVertices(RenderableData data) {
        if (data.vertexObject.cacheVertices) {
            //Only delete display list if no data objects are using it.
            Set<RenderableData> set = objectMap.get(data.vertexObject);
            if (set != null) {
                set.remove(data);
                if (set.isEmpty()) {
                    objectMap.remove(data.vertexObject);
                    cachedRadiusMap.remove(data.vertexObject);
                    GL11.glDeleteLists(cachedIndexMap.remove(data.vertexObject), 1);
                }
            }
    	}
    }

    @Override
    public boolean bindURLTexture(String textureURL, InputStream stream) {
        if (stream != null) {
            try {
                BufferedImage image = TextureUtil.readBufferedImage(stream);
                int glTexturePointer = TextureUtil.glGenTextures();
                TextureUtil.uploadTextureImageAllocate(glTexturePointer, image, false, false);
                onlineTextures.put(textureURL, glTexturePointer);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            onlineTextures.put(textureURL, TextureUtil.MISSING_TEXTURE.getGlTextureId());
            return false;
        }
    }

    @Override
    public boolean bindURLGIF(String textureURL, ParsedGIF gif) {
        Map<GIFImageFrame, Integer> gifFrameIndexes = new HashMap<>();
        for (GIFImageFrame frame : gif.frames.values()) {
            int glTexturePointer = TextureUtil.glGenTextures();
            TextureUtil.uploadTextureImageAllocate(glTexturePointer, frame.getImage(), false, false);
            gifFrameIndexes.put(frame, glTexturePointer);
        }
        animatedGIFs.put(textureURL, gif);
        animatedGIFFrames.put(gif, gifFrameIndexes);
        return true;
    }

    @Override
    public int getLightingAtPosition(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        int light = Minecraft.getMinecraft().world.getCombinedLight(pos, 0);
        //Under a shader pack, dynamic lights raise block light: IV models take it like the world's meshes would.  Packed as sky << 20 | block << 4.
        int raised = DynamicLightSystem.blockLightAt(pos.getX(), pos.getY(), pos.getZ());
        if (raised > ((light >> 4) & 15)) {
            light = (light & ~(15 << 4)) | (raised << 4);
        }
        return light;
    }

    /** The bounding radius of an interleaved normal, uv, position buffer round the origin, as {@link #renderVertices(FloatBuffer, boolean)} reads it. */
    private static float getVerticesRadius(FloatBuffer vertices) {
        float radiusSquared = 0;
        for (int i = vertices.position(); i + 7 < vertices.limit(); i += 8) {
            float x = vertices.get(i + 5);
            float y = vertices.get(i + 6);
            float z = vertices.get(i + 7);
            radiusSquared = Math.max(radiusSquared, x * x + y * y + z * z);
        }
        return (float) Math.sqrt(radiusSquared);
    }

    /**
     * Called at the start of every frame (render tick): forgets the last frame's shadow body captures, which a
     * frame without a light pass never consumed.
     */
    @SubscribeEvent
    public static void onIVRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ++dynamicLightFrame;
            DynamicLightShadowAtlas.frameEnd();
        }
    }

    /**
     * Whether the solid pass's cached models are drawn as dynamic light shadow bodies: set around the pass that draws
     * the solid ({@code !blendingEnabled}) models, while the light pass is going to run and wants shadows.
     */
    protected static void setCapturingBodies(boolean capturing) {
        capturingBodies = capturing && DynamicLightSystem.getMode() == DynamicLightSystem.Mode.DEFERRED && ConfigSystem.client.renderingSettings.DynL_shadows.value;
        DynamicLightShadowAtlas.setCapturing(capturingBodies);
    }

    /**
     * Builds and draws this frame's dynamic lights.  Called after the solid models are drawn and before IV's blended
     * pass, once a frame, with the world render's camera still on the GL matrix stack.
     * <p>
     * The camera matrix of 1.12.2 is not rotation alone: it carries the third person distance, the eye height and
     * view bobbing as a translation in eye space ({@code eye = R * (p - feet) + t}).  The camera sits at
     * {@code feet - R^T t}; the view mccore gets is {@code R} alone, relative to that point.
     */
    protected static void renderDynamicLightFrame(float partialTicks) {
        if (dynamicLightFrameDrawn == dynamicLightFrame) {
            return;
        }
        dynamicLightFrameDrawn = dynamicLightFrame;
        Entity camera = Minecraft.getMinecraft().getRenderViewEntity();
        if (camera == null) {
            return;
        }
        modelViewBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewBuffer);
        modelViewBuffer.get(dynamicLightView);
        projectionBuffer.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionBuffer);
        projectionBuffer.get(dynamicLightProjection);

        double tx = dynamicLightView[12];
        double ty = dynamicLightView[13];
        double tz = dynamicLightView[14];
        //R^T t: column-major, so R's column c is view[4c..4c+2] and (R^T t)[c] is that column dotted with t.
        double cameraX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partialTicks - (dynamicLightView[0] * tx + dynamicLightView[1] * ty + dynamicLightView[2] * tz);
        double cameraY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partialTicks - (dynamicLightView[4] * tx + dynamicLightView[5] * ty + dynamicLightView[6] * tz);
        double cameraZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partialTicks - (dynamicLightView[8] * tx + dynamicLightView[9] * ty + dynamicLightView[10] * tz);
        //Rotation only from here on.
        dynamicLightView[12] = 0;
        dynamicLightView[13] = 0;
        dynamicLightView[14] = 0;
        //World right is R's first row, up its second: left = -right.
        dynamicLightLeft[0] = -dynamicLightView[0];
        dynamicLightLeft[1] = -dynamicLightView[4];
        dynamicLightLeft[2] = -dynamicLightView[8];
        dynamicLightUp[0] = dynamicLightView[1];
        dynamicLightUp[1] = dynamicLightView[5];
        dynamicLightUp[2] = dynamicLightView[9];

        DynamicLightMatrix.mul(dynamicLightProjection, dynamicLightView, dynamicLightClip);
        final double cx = cameraX;
        final double cy = cameraY;
        final double cz = cameraZ;
        DynamicLightFrustum frustum = (minX, minY, minZ, maxX, maxY, maxZ) -> isBoxInClipVolume(minX - cx, minY - cy, minZ - cz, maxX - cx, maxY - cy, maxZ - cz);
        DynamicLightSystem.renderFrame(partialTicks, frustum, cameraX, cameraY, cameraZ, dynamicLightLeft, dynamicLightUp, dynamicLightView, dynamicLightProjection);
    }

    /** Whether a camera-relative box touches the view volume of {@link #dynamicLightClip} (Gribb-Hartmann planes). */
    private static boolean isBoxInClipVolume(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        float[] m = dynamicLightClip;
        for (int plane = 0; plane < 6; plane++) {
            int axis = plane >> 1;
            float sign = (plane & 1) == 0 ? 1F : -1F;
            //Row 3 +- row `axis` of the column-major clip matrix.
            double a = m[3] + sign * m[axis];
            double b = m[7] + sign * m[4 + axis];
            double c = m[11] + sign * m[8 + axis];
            double d = m[15] + sign * m[12 + axis];
            double x = a >= 0 ? maxX : minX;
            double y = b >= 0 ? maxY : minY;
            double z = c >= 0 ? maxZ : minZ;
            if (a * x + b * y + c * z + d < 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public float getDaylight(AWrapperWorld world, Point3D position) {
        World mcWorld = ((WrapperWorld) world).world;
        //Sky light alone is 15 at midnight too; the sun's height (weather included) brings it down: 0 at night.
        float open = mcWorld.getLightFor(EnumSkyBlock.SKY, new BlockPos(position.x, position.y, position.z)) / 15F;
        return open * Math.max(0F, Math.min(1F, mcWorld.getSunBrightnessFactor(1F)));
    }

    @Override
    public float getRainLevel(AWrapperWorld world, float partialTicks) {
        return ((WrapperWorld) world).world.getRainStrength(partialTicks);
    }

    @Override
    public boolean readDynamicLightSection(AWrapperWorld world, int sx, int sy, int sz, DynamicLightVoxelWindow window) {
        World mcWorld = ((WrapperWorld) world).world;
        Chunk chunk = mcWorld.getChunkProvider().getLoadedChunk(sx, sz);
        ExtendedBlockStorage[] storages = chunk != null ? chunk.getBlockStorageArray() : null;
        ExtendedBlockStorage storage = storages != null && sy >= 0 && sy < storages.length ? storages[sy] : null;
        if (storage == null || storage == Chunk.NULL_BLOCK_STORAGE || storage.isEmpty()) {
            window.fillSection(sx, sy, sz, false);
            return true;
        }
        int x0 = sx << 4;
        int y0 = sy << 4;
        int z0 = sz << 4;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    lightProbe.setPos(x0 + x, y0 + y, z0 + z);
                    window.setShape(x0 + x, y0 + y, z0 + z, shadowByte(mcWorld, storage.get(x, y, z), window));
                }
            }
        }
        return true;
    }

    /**
     * {@code state}'s byte in the dynamic light grid, worked out once per state at {@link #lightProbe}: a full block
     * casts a full shadow; one that is not (slabs, stairs, fences, doors, hoppers, chests, beds, banners, signs, mob
     * heads) casts its collision boxes' (its bounding box when it has none); none for air, IV's own blocks, blocks
     * without a block model and without a tile entity to draw them (barriers), see-through blocks (glass, leaves,
     * bars and panes, the translucent layer, ladders, vines, spawners), liquids, blocks with neither a collision box
     * nor a solid layer (plants, torches, rails) and blocks drawn at a random offset.  A state's boxes are read in the world at the first position it is seen at, so a block whose shape
     * follows its neighbours (fence arms) keeps the shape of that first position.
     */
    private int shadowByte(World world, IBlockState state, DynamicLightVoxelWindow window) {
        DynamicLightBlockShapes shapes = window.shapes();
        int value = shapes.byteOf(state);
        if (value < 0) {
            shapes.begin();
            if (state.getBlock() == Blocks.AIR || state.getBlock() instanceof BuilderBlock || (state.getRenderType() == EnumBlockRenderType.INVISIBLE && !state.getBlock().hasTileEntity(state))) {
                //No boxes: IV's own blocks are drawn by the entity renderer, whose bodies cast the shadow; barriers and
                //other blocks without a block model, even full ones, show nothing to shade with.  A tile entity block
                //(sign, mob head, banner) is invisible to the chunk mesh too, but its tile entity renderer draws it, so
                //it goes on to cast its shape.
            } else if (state.isOpaqueCube() && state.isFullCube()) {
                shapes.box(0, 0, 0, 1, 1, 1);
            } else if (castsShapedShadow(world, state)) {
                collisionBoxesAtProbe(world, state);
                for (AxisAlignedBB box : lightBoxes) {
                    shapes.box(box.minX - lightProbe.getX(), box.minY - lightProbe.getY(), box.minZ - lightProbe.getZ(), box.maxX - lightProbe.getX(), box.maxY - lightProbe.getY(), box.maxZ - lightProbe.getZ());
                }
                if (lightBoxes.isEmpty()) {
                    //Banners and signs have a shape but nothing to collide with.
                    AxisAlignedBB box = state.getBoundingBox(world, lightProbe);
                    if (box != null) {
                        shapes.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
                    }
                }
            }
            value = window.endShape(state);
        }
        return value;
    }

    /** Whether {@code state}, visible but not a full block, casts its shape's shadow: see {@link #shadowByte}. */
    private boolean castsShapedShadow(World world, IBlockState state) {
        Block block = state.getBlock();
        Material material = state.getMaterial();
        if (material == Material.GLASS || material == Material.LEAVES || material.isLiquid() || block.getOffsetType() != Block.EnumOffsetType.NONE) {
            return false;
        }
        //Ladders and spawners are mostly holes, which the grid cannot see: their box would shade as a wall.
        if (block == Blocks.MOB_SPAWNER || block.isLadder(state, world, lightProbe, null)) {
            return false;
        }
        if (state.getRenderType() != EnumBlockRenderType.MODEL) {
            //Drawn by a tile entity renderer (chests, beds, signs, heads, banners): solid things.
            return true;
        }
        //Bars and panes are mostly holes; they share the cutout mipped layer with solid things like hoppers, so the
        //layer cannot tell them apart.
        if (block instanceof BlockPane || block.canRenderInLayer(state, BlockRenderLayer.TRANSLUCENT)) {
            return false;
        }
        if (block.canRenderInLayer(state, BlockRenderLayer.SOLID)) {
            return true;
        }
        collisionBoxesAtProbe(world, state);
        return !lightBoxes.isEmpty();
    }

    /** Fills {@link #lightBoxes} with {@code state}'s collision boxes at {@link #lightProbe}, in world space. */
    private void collisionBoxesAtProbe(World world, IBlockState state) {
        lightBoxes.clear();
        state.addCollisionBoxToList(world, lightProbe, new AxisAlignedBB(lightProbe).grow(1), lightBoxes, null, false);
    }

    private final BlockPos.MutableBlockPos lightProbe = new BlockPos.MutableBlockPos();
    private final List<AxisAlignedBB> lightBoxes = new ArrayList<>();

    /**
     * A client world appeared: its block changes mark the dynamic light block grid, so shadows follow them.  The
     * server's worlds are skipped; each client world fires this once.
     */
    @SubscribeEvent
    public static void onIVWorldLoad(WorldEvent.Load event) {
        if (event.getWorld().isRemote) {
            event.getWorld().addEventListener(new DynamicLightBlockListener());
        }
    }

    /** A client block change: the dynamic light block grid reads its section again, so shadows follow it. */
    private static final class DynamicLightBlockListener implements IWorldEventListener {
        @Override
        public void notifyBlockUpdate(World world, BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {
            if (oldState != newState) {
                DynamicLightSystem.markBlock(pos.getX(), pos.getY(), pos.getZ());
            }
        }

        @Override
        public void notifyLightSet(BlockPos pos) {
        }

        @Override
        public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
        }

        @Override
        public void playSoundToAllNearExcept(EntityPlayer player, SoundEvent sound, SoundCategory category, double x, double y, double z, float volume, float pitch) {
        }

        @Override
        public void playRecord(SoundEvent sound, BlockPos pos) {
        }

        @Override
        public void spawnParticle(int particleID, boolean ignoreRange, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        }

        @Override
        public void spawnParticle(int id, boolean ignoreRange, boolean minimiseParticleLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        }

        @Override
        public void onEntityAdded(Entity entity) {
        }

        @Override
        public void onEntityRemoved(Entity entity) {
        }

        @Override
        public void broadcastSound(int soundID, BlockPos pos, int data) {
        }

        @Override
        public void playEvent(EntityPlayer player, int type, BlockPos pos, int data) {
        }

        @Override
        public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {
        }
    }

    @Override
    public IInterfaceDynamicLightingGL getDynamicLightGL() {
        return InterfaceDynamicLightingGL.supported() ? InterfaceDynamicLightingGL.INSTANCE : null;
    }

    @Override
    public boolean shouldRenderBoundingBoxes() {
        return Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox();
    }

    /**
     * Renders a set of raw vertices without any caching.  With {@code shadowUVs} (a display list the dynamic light
     * shadow pass will call) each vertex's texture coordinates also go to generic attribute
     * {@link InterfaceDynamicLightingGL#UV_ATTRIBUTE}, which that pass's program reads; fixed function ignores it.
     */
    private static void renderVertices(FloatBuffer vertices, boolean shadowUVs) {
        GL11.glBegin(GL11.GL_TRIANGLES);
        while (vertices.hasRemaining()) {
            GL11.glNormal3f(vertices.get(), vertices.get(), vertices.get());
            float u = vertices.get();
            float v = vertices.get();
            GL11.glTexCoord2f(u, v);
            if (shadowUVs) {
                GL20.glVertexAttrib2f(InterfaceDynamicLightingGL.UV_ATTRIBUTE, u, v);
            }
            GL11.glVertex3f(vertices.get(), vertices.get(), vertices.get());
        }
        GL11.glEnd();
        //Rewind buffer for next read.
        vertices.rewind();
    }

    /**
     * Renders a set of raw lines without any caching.
     */
    private static void renderLines(FloatBuffer vertices) {
        GL11.glLineWidth(2);
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
     * the string values that are passed-in.  Returns the bound texture's GL id.
     */
    private static int bindTexture(String textureLocation) {
        if (animatedGIFs.containsKey(textureLocation)) {
            //Special case for GIFs.
            ParsedGIF parsedGIF = animatedGIFs.get(textureLocation);
            int texture = animatedGIFFrames.get(parsedGIF).get(parsedGIF.getCurrentFrame());
            GlStateManager.bindTexture(texture);
            return texture;
        } else if (onlineTextures.containsKey(textureLocation)) {
            //Online texture.
            int texture = onlineTextures.get(textureLocation);
            GlStateManager.bindTexture(texture);
            return texture;
        } else if (textureLocation.equals(RenderableData.GLOBAL_TEXTURE_NAME)) {
            //Default texture.
            return bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
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
            return bindTexture(internalTextures.get(textureLocation));
        }
    }

    /** Binds {@code location} through MC's texture manager, which loads it first if needed, and returns its GL id. */
    private static int bindTexture(ResourceLocation location) {
        TextureManager textures = Minecraft.getMinecraft().getTextureManager();
        textures.bindTexture(location);
        //bindTexture registered it, the missing texture when it would not load.
        return textures.getTexture(location).getGlTextureId();
    }

    /**
     * Renders the main GUI, setting up any transforms or operations as required.
     */
    protected static void renderGUI(int mouseX, int mouseY, int screenWidth, int screenHeight, float partialTicks, boolean updateGUIs) {
        //Render GUIs, re-creating their components if needed.
        //Set Y-axis to inverted to have correct orientation.
        GL11.glScalef(1.0F, -1.0F, 1.0F);

        //Enable alpha testing.  This can be disabled by mods doing bad state management during their event calls.
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        //Enable lighting.
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().entityRenderer.enableLightmap();
        setLightingState(true);

        //Render main pass, then blended pass, for each GUI.
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

            //Disable blending for main render.
            InterfaceRender.setBlend(false);
            gui.render(mouseX, mouseY, false, partialTicks);

            //Now render blended pass.  Needs to happen after normal GUI rendering, but before others, otherwise buffers get fouled.
            //Need to re-enable blending now.
            setBlend(true);
            gui.render(mouseX, mouseY, true, partialTicks);

            if (!stacksToRender.isEmpty()) {
                //Blending needs to be off for this.
                setBlend(false);

                //Render all stacks.  These have to be in the standard GUI reference frame or they won't render.
                GL11.glScalef(1.0F, -1.0F, 1.0F);

                //Need to disable internal lighting due to it messing up stack shading.
                setInternalLightingState(false);
                for (GUIComponentItem component : stacksToRender) {
                    //Double-check the stack is still present, it might have been un-set since this call.
                    if ((WrapperItemStack) component.stackToRender != null) {
                        //Apply existing transform.
                        //Need to translate the z-offset to our value, which includes a -100 for the default added value.
                        float zOffset = Minecraft.getMinecraft().getRenderItem().zLevel;
                        Minecraft.getMinecraft().getRenderItem().zLevel = (float) component.translation.z - 100;
                        if (component.scale != 1.0) {
                            GL11.glPushMatrix();
                            GL11.glScalef(component.scale, component.scale, 1.0F);
                            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(((WrapperItemStack) (component.stackToRender)).stack, (int) (component.translation.x / component.scale), (int) -(component.translation.y / component.scale) + 1);
                            GL11.glPopMatrix();
                        } else {
                            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(((WrapperItemStack) (component.stackToRender)).stack, (int) component.translation.x, (int) -component.translation.y);
                        }
                        Minecraft.getMinecraft().getRenderItem().zLevel = zOffset;
                    }
                }
                stacksToRender.clear();
                //Put states back to where they were before stack rendering.
                setInternalLightingState(true);
                setBlend(true);
            }
            GL11.glPopMatrix();
        }

        //Set state back to normal.
        setLightingState(false);
        Minecraft.getMinecraft().entityRenderer.disableLightmap();
        RenderHelper.disableStandardItemLighting();
        GL11.glScalef(1.0F, -1.0F, 1.0F);
    }

    /**
     * World last event.  This occurs at the end of rendering in a special pass of -1.
     * We normally don't do anything here.  The exception is if the {@link BuilderEntityRenderForwarder}
     * didn't get rendered.  In this case, we manually render it.  The rendering pipelines
     * of those methods are set up to handle this and will tread a -1 pass as a combined 0/1 pass.
     */
    @SubscribeEvent
    public static void onIVRenderLast(RenderWorldLastEvent event) {
        //Enable lighting as pass -1 has that disabled.
        RenderHelper.enableStandardItemLighting();
        setLightingState(true);

        //Render pass 0 and 1 here manually.
        for (lastRenderPassActualPass = 0; lastRenderPassActualPass < 2; ++lastRenderPassActualPass) {
            if (lastRenderPassActualPass == 1) {
                setBlend(true);
                GlStateManager.depthMask(false);
            }

            if (BuilderEntityRenderForwarder.lastClientInstance != null) {
                Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(BuilderEntityRenderForwarder.lastClientInstance).doRender(BuilderEntityRenderForwarder.lastClientInstance, 0, 0, 0, 0, event.getPartialTicks());
            }

            if (lastRenderPassActualPass == 1) {
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
}
