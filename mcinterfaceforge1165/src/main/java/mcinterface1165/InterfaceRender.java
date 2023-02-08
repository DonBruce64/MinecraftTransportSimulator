package mcinterface1165;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IInterfaceRender;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderableObject;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
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
    private static final Map<IWrapperItemStack, Point3D> stacksToRender = new LinkedHashMap<>();
    private static int currentPackedLight;
    private static RenderState.TextureState MISSING_STATE = new RenderState.TextureState(new ResourceLocation("mts:textures/rendering/missing.png"), false, false);
    private static RenderState.TextureState BLOCK_STATE = new RenderState.TextureState(PlayerContainer.BLOCK_ATLAS, false, false);
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
        matrixStack.pushPose();
        Matrix4f matrix4f = convertMatrix4f(object.transform);
        Matrix3f matrix3f = new Matrix3f(matrix4f);
        MatrixStack.Entry stackEntry = matrixStack.last();
        stackEntry.pose().multiply(matrix4f);
        stackEntry.normal().mul(matrix3f);


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
            //Create the state, we don't care about outline.
            Builder stateBuilder = CustomRenderType.createForObject(object);
            RenderType renderType = RenderType.create("mts_entity", DefaultVertexFormats.NEW_ENTITY, 7, 256, true, object.isTranslucent, stateBuilder.createCompositeState(false));
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
                    buffer.uv2(currentPackedLight);
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
        matrixStack.popPose();
    }

    @Override
    public void deleteVertices(RenderableObject object) {
        //No-op on 1.16.5 since we can't cache vertexes.
    }

    @Override
    public void setLightingToPosition(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        currentPackedLight = LightTexture.pack(Minecraft.getInstance().level.getBrightness(LightType.BLOCK, pos), Minecraft.getInstance().level.getBrightness(LightType.SKY, pos));
    }

    @Override
    public boolean shouldRenderBoundingBoxes() {
        return Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes();
    }

    @Override
    public String downloadURLTexture(String textureURL) {
        return "URL textures are not supported in this version.  Sorry.";
    }

    /**
     * Helper function to create a new texture state for the specified texture location.
     */
    private static RenderState.TextureState getTexture(String textureLocation) {
        if (textureLocation.equals(RenderableObject.GLOBAL_TEXTURE_NAME)) {
            //Default texture.
            return BLOCK_STATE;
        } else {
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
    protected static void renderGUI(MatrixStack stack, int mouseX, int mouseY, int screenWidth, int screenHeight, float partialTicks, boolean updateGUIs) {
        //Get the buffer for GUI rendering.
        matrixStack = stack;
        matrixStack.pushPose();
        IRenderTypeBuffer.Impl guiBuffer = IRenderTypeBuffer.immediate(Tessellator.getInstance().getBuilder());
        renderBuffer = guiBuffer;

        //Render GUIs, re-creating their components if needed.
        //Set Y-axis to inverted to have correct orientation.
        matrixStack.scale(1.0F, -1.0F, 1.0F);
        
        //We don't want to enable blending though, as that's on-demand.
        //Just in case it is enabled, however, disable it.
        //This ensures the blending state is as it will be for the main rendering pass of -1.
        //InterfaceRender.setBlend(false);
        
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
            //FIXME this should work now since rendering is state-dependent...  
            gui.render(mouseX, mouseY, true, partialTicks);
        
            //Render all stacks.  These have to be in the standard GUI reference frame or they won't render.
            matrixStack.scale(1.0F, -1.0F, 1.0F);

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
        
            matrixStack.popPose();
        }
        /*displayGUIIndex = 0;
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
        */
        //GL11.glScalef(1.0F, -1.0F, 1.0F);
        matrixStack.popPose();//.scale(1.0F, -1.0F, 1.0F);
        guiBuffer.endBatch();
    }

    /**
     * Converts internal matrix to new external matrix.
     */
    private static Matrix4f convertMatrix4f(TransformationMatrix transform) {
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
                //Get all entities in the world, and render them manually for this one builder.
                //Only do this if the player the builder is following is the client player.
                WrapperWorld world = WrapperWorld.getWrapperFor(builder.level);
                if (Minecraft.getInstance().player.equals(builder.playerFollowing)) {
                    ConcurrentLinkedQueue<AEntityC_Renderable> allEntities = world.renderableEntities;
                    if (allEntities != null) {
                        matrixStack = stack;
                        renderBuffer = buffer;

                        //Pop the pose so we aren't relative to the forwarder and are instead relative to the player.
                        stack.popPose();

                        //Push on a new pose offset by the player's eye height to account for camera height from them.
                        stack.pushPose();
                        ActiveRenderInfo camera = Minecraft.getInstance().gameRenderer.getMainCamera();
                        PlayerEntity player = Minecraft.getInstance().player;
                        stack.translate(0, MathHelper.lerp(partialTicks, player.yOld, player.getY()) - camera.getPosition().y, 0);

                        //Enable normal re-scaling for model rendering.
                        //This prevents bad lighting.
                        //FIXME we probably need this, not sure how to do it though given it's internal...
                        //GlStateManager.enableRescaleNormal();

                        //Start master profiling section and run entity rendering routines.
                        for (AEntityC_Renderable entity : allEntities) {
                            world.beginProfiling("MTSRendering", true);
                            entity.render(false, partialTicks);
                            entity.render(true, partialTicks);
                            world.endProfiling();
                        }

                        //Reset states.
                        //GlStateManager.disableRescaleNormal();
                    }
                }
            }
        });

        //Register blank classes for the other builders.
        //If we don't, the game crashes when trying to render them.
        RenderingRegistry.registerEntityRenderingHandler(BuilderEntityExisting.E_TYPE2.get(), manager -> new BlankRender<BuilderEntityExisting>(manager));
        RenderingRegistry.registerEntityRenderingHandler(BuilderEntityLinkedSeat.E_TYPE3.get(), manager -> new BlankRender<BuilderEntityLinkedSeat>(manager));
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
            stateBuilder.setTransparencyState(object.enableBrightBlending ? BRIGHTNESS_TRANSPARENCY : (object.isTranslucent ? RenderType.TRANSLUCENT_TRANSPARENCY : RenderType.NO_TRANSPARENCY));
            //Diffuse lighting is the ambient lighting that auto-shades models.
            stateBuilder.setDiffuseLightingState(object.ignoreWorldShading || object.disableLighting ? NO_DIFFUSE_LIGHTING : DIFFUSE_LIGHTING);
            //Always smooth shading.
            stateBuilder.setShadeModelState(SMOOTH_SHADE);
            //Disable alpha testing on blended pass as it discards transparent fragments.
            stateBuilder.setAlphaState(object.isTranslucent ? NO_ALPHA : DEFAULT_ALPHA);
            //Depth is fine, as is cull.
            //stateBuilder.setDepthTestState(LEQUAL_DEPTH_TEST);
            //stateBuilder.setCullState(CULL);
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

    private static final RenderState.TransparencyState BRIGHTNESS_TRANSPARENCY = new RenderState.TransparencyState("brightness_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });
}
