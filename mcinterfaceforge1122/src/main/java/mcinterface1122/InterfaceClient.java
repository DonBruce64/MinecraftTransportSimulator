package mcinterface1122;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIPackMissing;
import minecrafttransportsimulator.mcinterface.IInterfaceClient;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import net.minecraft.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(Side.CLIENT)
public class InterfaceClient implements IInterfaceClient {
    private static CameraMode actualCameraMode;
    private static CameraMode cameraModeRequest;
    private static int ticksToCullingWarning = 200;
    private static BuilderEntityRenderForwarder activeFollower;
    private static int ticksSincePlayerJoin;

    @Override
    public boolean isGamePaused() {
        return Minecraft.getMinecraft().isGamePaused();
    }

    @Override
    public String getLanguageName() {
        return Minecraft.getMinecraft().gameSettings.language;
    }

    @Override
    public List<String> getAllLanguages() {
        List<String> list = new ArrayList<>();
        Minecraft.getMinecraft().getLanguageManager().getLanguages().forEach(language -> list.add(language.getLanguageCode()));
        return list;
    }

    @Override
    public String getFluidName(String fluidID, String fluidMod) {
        Fluid fluid = FluidRegistry.getFluid(fluidID);
        return fluid != null ? new FluidStack(fluid, 1).getLocalizedName() : "INVALID";
    }

    @Override
    public Map<String, String> getAllFluidNames() {
        Map<String, String> fluidIDsToNames = new HashMap<>();
        for (String fluidID : FluidRegistry.getRegisteredFluids().keySet()) {
            fluidIDsToNames.put(fluidID, new FluidStack(FluidRegistry.getFluid(fluidID), 1).getLocalizedName());
        }
        return fluidIDsToNames;
    }

    @Override
    public boolean isChatOpen() {
        return Minecraft.getMinecraft().ingameGUI.getChatGUI().getChatOpen();
    }

    @Override
    public boolean isGUIOpen() {
        return Minecraft.getMinecraft().currentScreen != null;
    }

    @Override
    public void displayOverlayMessage(String message) {
        Minecraft.getMinecraft().ingameGUI.setOverlayMessage(message, false);
    }

    @Override
    public CameraMode getCameraMode() {
        return actualCameraMode;
    }

    @Override
    public void setCameraMode(CameraMode mode) {
        cameraModeRequest = mode;
    }

    @Override
    public int getCameraDefaultZoom() {
        return 4;
    }

    @Override
    public long getPackedDisplaySize() {
        ScaledResolution screenResolution = new ScaledResolution(Minecraft.getMinecraft());
        return (((long) screenResolution.getScaledWidth()) << Integer.SIZE) | (screenResolution.getScaledHeight() & 0xffffffffL);
    }

    @Override
    public float getMouseSensitivity() {
        return Minecraft.getMinecraft().gameSettings.mouseSensitivity;
    }

    @Override
    public void setMouseSensitivity(float setting) {
        Minecraft.getMinecraft().gameSettings.mouseSensitivity = setting;
    }

    @Override
    public float getFOV() {
        return Minecraft.getMinecraft().gameSettings.fovSetting;
    }

    @Override
    public void setFOV(float setting) {
        Minecraft.getMinecraft().gameSettings.fovSetting = setting;
    }

    @Override
    public boolean isGUIHidden() {
        return Minecraft.getMinecraft().gameSettings.hideGUI;
    }

    @Override
    public void closeGUI() {
        Minecraft.getMinecraft().displayGuiScreen(null);
    }

    @Override
    public void setActiveGUI(AGUIBase gui) {
        FMLCommonHandler.instance().showGuiScreen(new BuilderGUI(gui));
    }

    @Override
    public WrapperWorld getClientWorld() {
        return WrapperWorld.getWrapperFor(Minecraft.getMinecraft().world);
    }

    @Override
    public WrapperPlayer getClientPlayer() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        return WrapperPlayer.getWrapperFor(player);
    }

    @Override
    public Point3D getCameraPosition() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        Vec3d cameraOffset = ActiveRenderInfo.getCameraPosition();
        mutablePosition.set(player.posX + cameraOffset.x, player.posY + cameraOffset.y, player.posZ + cameraOffset.z);
        return mutablePosition;
    }

    private static final Point3D mutablePosition = new Point3D();
    private static final RotationMatrix cameraProjectionOrientation = new RotationMatrix();

    @Override
    public Point3D projectToScreen(Point3D worldPos, int screenWidth, int screenHeight) {
        double camX, camY, camZ;
        double fwdX, fwdY, fwdZ;
        double upX, upY, upZ;
        double rgtX, rgtY, rgtZ;

        if (InterfaceEventsEntityRendering.adjustedCamera) {
            // Use MTS camera data — includes roll and correct vehicle-relative position.
            camX = InterfaceEventsEntityRendering.projectionCameraPosition.x;
            camY = InterfaceEventsEntityRendering.projectionCameraPosition.y;
            camZ = InterfaceEventsEntityRendering.projectionCameraPosition.z;
            RotationMatrix ori = getCameraProjectionOrientation(InterfaceEventsEntityRendering.projectionCameraOrientation);
            fwdX = ori.m02; fwdY = ori.m12; fwdZ = ori.m22;
            upX  = ori.m01; upY  = ori.m11; upZ  = ori.m21;
            // MTS (1,0,0) rotated = camera LEFT (not right); negate to get camera right.
            rgtX = -ori.m00; rgtY = -ori.m10; rgtZ = -ori.m20;
        } else {
            // 1.12.2 has no Camera class — compute direction from player view angles.
            net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().player;
            net.minecraft.util.math.Vec3d camOffset = net.minecraft.client.renderer.ActiveRenderInfo.getCameraPosition();
            camX = player.posX + camOffset.x;
            camY = player.posY + camOffset.y;
            camZ = player.posZ + camOffset.z;

            float yaw   = (float) Math.toRadians(player.rotationYaw);
            float pitch = (float) Math.toRadians(player.rotationPitch);

            fwdX = -Math.sin(yaw) * Math.cos(pitch);
            fwdY = -Math.sin(pitch);
            fwdZ =  Math.cos(yaw) * Math.cos(pitch);
            rgtX =  Math.cos(yaw);
            rgtY =  0;
            rgtZ =  Math.sin(yaw);
            upX  =  fwdY * rgtZ - fwdZ * rgtY;
            upY  =  fwdZ * rgtX - fwdX * rgtZ;
            upZ  =  fwdX * rgtY - fwdY * rgtX;
            if (actualCameraMode == CameraMode.THIRD_PERSON_INVERTED) {
                fwdX = -fwdX; fwdY = -fwdY; fwdZ = -fwdZ;
                rgtX = -rgtX; rgtY = -rgtY; rgtZ = -rgtZ;
            }
        }

        double dx = worldPos.x - camX;
        double dy = worldPos.y - camY;
        double dz = worldPos.z - camZ;

        double depth = dx * fwdX + dy * fwdY + dz * fwdZ;
        if (depth <= 0.001) return null;

        double xView = dx * rgtX + dy * rgtY + dz * rgtZ;
        double yView = dx * upX  + dy * upY  + dz * upZ;

        double fovRad = Math.toRadians(getFOV());
        double tanHalfFov = Math.tan(fovRad / 2.0);
        double aspect = (double) screenWidth / screenHeight;

        double ndcX = xView / (depth * tanHalfFov * aspect);
        double ndcY = yView / (depth * tanHalfFov);

        if (ndcX < -1.1 || ndcX > 1.1 || ndcY < -1.1 || ndcY > 1.1) return null;

        screenProjectionResult.set(
                (ndcX + 1.0) / 2.0 * screenWidth,
                (1.0 - ndcY) / 2.0 * screenHeight,
                depth);
        return screenProjectionResult;
    }

    private static RotationMatrix getCameraProjectionOrientation(RotationMatrix cameraOrientation) {
        if (actualCameraMode == CameraMode.THIRD_PERSON_INVERTED) {
            cameraOrientation.convertToAngles();
            cameraProjectionOrientation.angles.set(-cameraOrientation.angles.x, cameraOrientation.angles.y - 180, -cameraOrientation.angles.z);
            cameraProjectionOrientation.updateToAngles();
            return cameraProjectionOrientation;
        } else {
            return cameraOrientation;
        }
    }

    private static final Point3D screenProjectionResult = new Point3D();

    @Override
    public void playBlockBreakSound(Point3D position) {
        BlockPos pos = new BlockPos(position.x, position.y, position.z);
        if (!Minecraft.getMinecraft().world.isAirBlock(pos)) {
            SoundType soundType = Minecraft.getMinecraft().world.getBlockState(pos).getBlock().getSoundType(Minecraft.getMinecraft().world.getBlockState(pos), Minecraft.getMinecraft().player.world, pos, null);
            Minecraft.getMinecraft().world.playSound(Minecraft.getMinecraft().player, pos, soundType.getBreakSound(), SoundCategory.BLOCKS, soundType.getVolume(), soundType.getPitch());
        }
    }

    @Override
    public List<String> getTooltipLines(IWrapperItemStack stack) {
        List<String> tooltipText = ((WrapperItemStack) stack).stack.getTooltip(Minecraft.getMinecraft().player, Minecraft.getMinecraft().gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
        //Add grey formatting text to non-first line tooltips.
        for (int i = 1; i < tooltipText.size(); ++i) {
            tooltipText.set(i, TextFormatting.GRAY + tooltipText.get(i));
        }
        return tooltipText;
    }

    /**
     * Tick client-side entities like bullets and particles.
     * These don't get ticked normally due to the world tick event
     * not being called on clients.
     */
    @SubscribeEvent
    public static void onIVClientTick(TickEvent.ClientTickEvent event) {
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        if (!InterfaceManager.clientInterface.isGamePaused() && player != null) {
            WrapperWorld world = WrapperWorld.getWrapperFor(Minecraft.getMinecraft().world);
            if (world != null) {
                if (event.phase.equals(Phase.START)) {
                    if (!player.isSpectator()) {
                        //Handle controls.  This has to happen prior to vehicle updates to ensure click handling is based on current position of the player.
                        ControlSystem.controlGlobal(player);
                        if (((WrapperPlayer) player).player.ticksExisted % 100 == 0) {
                            if (!InterfaceManager.clientInterface.isGUIOpen() && !PackParser.arePacksPresent()) {
                                new GUIPackMissing();
                            }
                        }
                    }

                    world.tickAll(true);

                    //Complain about compats at 10 second mark.
                    if (ConfigSystem.settings.general.performModCompatFunctions.value) {
                        if (ticksToCullingWarning > 0) {
                            if (--ticksToCullingWarning == 0) {
                                //Nothing to complain about here!
                            }
                        }
                    }

                    //Check follower.
                    if (activeFollower != null) {
                        //Follower exists, check if world is the same and it is actually updating.
                        //We check basic states, and then the watchdog bit that gets reset every tick.
                        //This way if we're in the world, but not valid we will know.
                        EntityPlayer mcPlayer = ((WrapperPlayer) player).player;
                        if (activeFollower.world != mcPlayer.world || activeFollower.playerFollowing != mcPlayer || mcPlayer.isDead || activeFollower.isDead || activeFollower.idleTickCounter == 20) {
                            //Follower is not linked.  Remove it and re-create in code below.
                            activeFollower.setDead();
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
                            world.world.spawnEntity(activeFollower);
                        }
                    }
                } else {
                    world.tickAll(false);
                    
                    //Handle camera requests.
                    if(cameraModeRequest != null) {
                    	switch(cameraModeRequest) {
	                    	case FIRST_PERSON:{
	                    		Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
	                    		break;
	                    	}
	                    	case THIRD_PERSON:{
	                    		Minecraft.getMinecraft().gameSettings.thirdPersonView = 1;
	                    		break;
	                    	}
	                    	case THIRD_PERSON_INVERTED:{
	                    		Minecraft.getMinecraft().gameSettings.thirdPersonView = 2;
	                    		break;
	                    	}
                    	}
                    	cameraModeRequest = null;
                    }

                    //Update camera state, since this can change depending on tick if we check during renders.
                    int cameraModeInt = Minecraft.getMinecraft().gameSettings.thirdPersonView;
                    switch(cameraModeInt) {
                    	case(0):{
                    		actualCameraMode = CameraMode.FIRST_PERSON;
                    		break;
                    	}
                    	case(1):{
                    		actualCameraMode = CameraMode.THIRD_PERSON;
                    		break;
                    	}
                    	case(2):{
                    		actualCameraMode = CameraMode.THIRD_PERSON_INVERTED;
                    		break;
                    	}
                    }
                }
            }
        }
    }
}
