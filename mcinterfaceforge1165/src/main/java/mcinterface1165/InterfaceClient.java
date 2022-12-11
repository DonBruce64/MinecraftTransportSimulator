package mcinterface1165;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.EntityParticle;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIPackMissing;
import minecrafttransportsimulator.mcinterface.IInterfaceClient;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ControlSystem;
import net.minecraft.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ChatVisibility;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(Dist.CLIENT)
public class InterfaceClient implements IInterfaceClient {
    private static boolean actuallyFirstPerson;
    private static boolean actuallyThirdPerson;
    private static boolean changedCameraState;
    private static boolean changeCameraRequest;
    private static BuilderEntityRenderForwarder activeFollower;
    private static int ticksSincePlayerJoin;

    @Override
    public boolean isGamePaused() {
        return Minecraft.getInstance().isPaused();
    }

    @Override
    public String getLanguageName() {
        return Minecraft.getInstance().getLanguageManager().getSelected().getCode();
    }

    @Override
    public boolean usingDefaultLanguage() {
        return Minecraft.getInstance().getLanguageManager().getSelected().getCode().equals("en_us");
    }

    @Override
    public String getFluidName(String fluidID) {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluidID));
        return fluid != null ? new FluidStack(fluid, 1).getDisplayName().toString() : "INVALID";
    }

    @Override
    public Map<String, String> getAllFluidNames() {
        Map<String, String> fluidIDsToNames = new HashMap<>();
        for (Fluid fluid : ForgeRegistries.FLUIDS.getValues()) {
            fluidIDsToNames.put(fluid.getRegistryName().getPath(), new FluidStack(fluid, 1).getDisplayName().toString());
        }
        return fluidIDsToNames;
    }

    @Override
    public boolean isChatOpen() {
        return Minecraft.getInstance().options.chatVisibility != ChatVisibility.HIDDEN;
    }

    @Override
    public boolean isGUIOpen() {
        return Minecraft.getInstance().screen != null;
    }

    @Override
    public boolean inFirstPerson() {
        return actuallyFirstPerson;
    }

    @Override
    public boolean inThirdPerson() {
        return actuallyThirdPerson;
    }

    @Override
    public boolean changedCameraState() {
        return changedCameraState && !changeCameraRequest;
    }

    @Override
    public void toggleFirstPerson() {
        changeCameraRequest = true;
    }

    @Override
    public long getPackedDisplaySize() {
        return (((long) Minecraft.getInstance().getWindow().getWidth()) << Integer.SIZE) | (Minecraft.getInstance().getWindow().getHeight() & 0xffffffffL);
    }

    @Override
    public float getFOV() {
        return (float) Minecraft.getInstance().options.fov;
    }

    @Override
    public void setFOV(float setting) {
        Minecraft.getInstance().options.fov = setting;
    }

    @Override
    public float getMouseSensitivity() {
        return (float) Minecraft.getInstance().options.sensitivity;
    }

    @Override
    public void setMouseSensitivity(float setting) {
        Minecraft.getInstance().options.sensitivity = setting;
    }

    @Override
    public AEntityB_Existing getMousedOverEntity() {
        //See what we are hitting.
        RayTraceResult lastHit = Minecraft.getInstance().hitResult;
        if (lastHit != null) {
            Point3D mousedOverPoint = new Point3D(lastHit.getLocation().x, lastHit.getLocation().y, lastHit.getLocation().z);
            if (lastHit.getType() == RayTraceResult.Type.ENTITY) {
                Entity entityHit = ((EntityRayTraceResult) lastHit).getEntity();
                if (entityHit instanceof BuilderEntityExisting) {
                    AEntityB_Existing mousedOverEntity = ((BuilderEntityExisting) entityHit).entity;
                    if (mousedOverEntity instanceof AEntityF_Multipart) {
                        AEntityF_Multipart<?> multipart = (AEntityF_Multipart<?>) mousedOverEntity;
                        for (BoundingBox box : multipart.allInteractionBoxes) {
                            if (box.isPointInside(mousedOverPoint)) {
                                APart part = multipart.getPartWithBox(box);
                                if (part != null) {
                                    return part;
                                }
                            }
                        }
                    }
                    return mousedOverEntity;
                }
            } else if (lastHit.getType() != RayTraceResult.Type.MISS) {
                BlockPos posHit = ((BlockRayTraceResult) lastHit).getBlockPos();
                TileEntity mcTile = getClientWorld().world.getBlockEntity(posHit);
                if (mcTile instanceof BuilderTileEntityFluidTank) {
                    BuilderTileEntityFluidTank builder = (BuilderTileEntityFluidTank) mcTile;
                    return builder.tileEntity;
                }
            }
        }
        return null;
    }

    @Override
    public void closeGUI() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void setActiveGUI(AGUIBase gui) {
        Minecraft.getInstance().setScreen(new BuilderGUI(gui));
    }

    @Override
    public WrapperWorld getClientWorld() {
        return WrapperWorld.getWrapperFor(Minecraft.getInstance().level);
    }

    @Override
    public WrapperPlayer getClientPlayer() {
        return WrapperPlayer.getWrapperFor(Minecraft.getInstance().player);
    }

    @Override
    public IWrapperEntity getRenderViewEntity() {
        return WrapperEntity.getWrapperFor(Minecraft.getInstance().getCameraEntity());
    }

    @Override
    public Point3D getCameraPosition() {
        Vector3d position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        mutablePosition.set(position.x, position.y, position.z);
        return mutablePosition;
    }

    private static final Point3D mutablePosition = new Point3D();

    @Override
    public void playBlockBreakSound(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        if (!Minecraft.getInstance().level.isEmptyBlock(pos)) {
            SoundType soundType = Minecraft.getInstance().level.getBlockState(pos).getBlock().getSoundType(Minecraft.getInstance().level.getBlockState(pos), Minecraft.getInstance().player.level, pos, null);
            Minecraft.getInstance().level.playSound(Minecraft.getInstance().player, pos, soundType.getBreakSound(), SoundCategory.BLOCKS, soundType.getVolume(), soundType.getPitch());
        }
    }

    @Override
    public List<String> getTooltipLines(IWrapperItemStack stack) {
        List<String> tooltipText = new ArrayList<>();
        List<ITextComponent> tooltipLines = ((WrapperItemStack) stack).stack.getTooltipLines(Minecraft.getInstance().player, Minecraft.getInstance().options.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
        //Add grey formatting text to non-first line tooltips.
        for (int i = 1; i < tooltipLines.size(); ++i) {
            tooltipText.set(i, TextFormatting.GRAY + tooltipLines.get(i).getString());
        }
        return tooltipText;
    }

    /**
     * Tick client-side entities like bullets and particles.
     * These don't get ticked normally due to the world tick event
     * not being called on clients.
     */
    @SubscribeEvent
    public static void on(TickEvent.ClientTickEvent event) {
        if (!InterfaceManager.clientInterface.isGamePaused() && event.phase.equals(Phase.END)) {
            changedCameraState = false;
            if (actuallyFirstPerson ^ Minecraft.getInstance().options.getCameraType() == PointOfView.FIRST_PERSON) {
                changedCameraState = true;
                actuallyFirstPerson = Minecraft.getInstance().options.getCameraType() == PointOfView.FIRST_PERSON;
            }
            if (actuallyThirdPerson ^ Minecraft.getInstance().options.getCameraType() == PointOfView.THIRD_PERSON_BACK) {
                changedCameraState = true;
                actuallyThirdPerson = Minecraft.getInstance().options.getCameraType() == PointOfView.THIRD_PERSON_BACK;
            }
            if (changeCameraRequest) {
                if (actuallyFirstPerson) {
                    Minecraft.getInstance().options.setCameraType(PointOfView.THIRD_PERSON_BACK);
                    actuallyFirstPerson = false;
                    actuallyThirdPerson = true;
                } else {
                    Minecraft.getInstance().options.setCameraType(PointOfView.FIRST_PERSON);
                    actuallyFirstPerson = true;
                    actuallyThirdPerson = false;
                }
                changeCameraRequest = false;
            }

            WrapperWorld clientWorld = WrapperWorld.getWrapperFor(Minecraft.getInstance().level);
            if (clientWorld != null) {
                clientWorld.beginProfiling("MTS_BulletUpdates", true);
                for (EntityBullet bullet : clientWorld.getEntitiesOfType(EntityBullet.class)) {
                    bullet.update();
                }

                clientWorld.beginProfiling("MTS_ParticleUpdates", false);
                for (EntityParticle particle : clientWorld.getEntitiesOfType(EntityParticle.class)) {
                    particle.update();
                }
                clientWorld.endProfiling();

                IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
                if (player != null && !player.isSpectator()) {
                    ControlSystem.controlGlobal(player);
                    if (((WrapperPlayer) player).player.tickCount % 100 == 0) {
                        if (!InterfaceManager.clientInterface.isGUIOpen() && !PackParser.arePacksPresent()) {
                            new GUIPackMissing();
                        }
                    }
                }

                if (activeFollower != null) {
                    //Follower exists, check if world is the same and it is actually updating.
                    //We check basic states, and then the watchdog bit that gets reset every tick.
                    //This way if we're in the world, but not valid we will know.
                    PlayerEntity mcPlayer = ((WrapperPlayer) player).player;
                    if (activeFollower.level != mcPlayer.level || activeFollower.playerFollowing != mcPlayer || !mcPlayer.isAlive() || !activeFollower.isAlive() || activeFollower.idleTickCounter == 20) {
                        //Follower is not linked.  Remove it and re-create in code below.
                        activeFollower.remove();
                        activeFollower = null;
                        ticksSincePlayerJoin = 0;
                    } else {
                        ++activeFollower.idleTickCounter;
                    }
                } else {
                    //Follower does not exist, check if player has been present for 3 seconds and spawn it.
                    if (++ticksSincePlayerJoin == 60) {
                        activeFollower = new BuilderEntityRenderForwarder(((WrapperPlayer) player).player);
                        activeFollower.loadedFromSavedNBT = true;
                        clientWorld.world.addFreshEntity(activeFollower);
                    }
                }
            }
        }
    }
}
