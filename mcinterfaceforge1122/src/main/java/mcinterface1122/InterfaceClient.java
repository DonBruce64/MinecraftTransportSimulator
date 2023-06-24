package mcinterface1122;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIPackMissing;
import minecrafttransportsimulator.mcinterface.IInterfaceClient;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
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
    private static boolean actuallyFirstPerson;
    private static boolean actuallyThirdPerson;
    private static boolean changedCameraState;
    private static boolean changeCameraRequest;
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
    public boolean usingDefaultLanguage() {
        return Minecraft.getMinecraft().gameSettings.language.equals("en_us");
    }

    @Override
    public String getFluidName(String fluidID) {
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
        Vec3d position = ActiveRenderInfo.getCameraPosition();
        mutablePosition.set(position.x, position.y, position.z);
        return mutablePosition;
    }

    private static final Point3D mutablePosition = new Point3D();

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
    public static void on(TickEvent.ClientTickEvent event) {
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        if (!InterfaceManager.clientInterface.isGamePaused() && player != null) {
            WrapperWorld world = WrapperWorld.getWrapperFor(Minecraft.getMinecraft().world);
            if (world != null) {
                if (event.phase.equals(Phase.START)) {
                    world.beginProfiling("MTS_ClientVehicleUpdates", true);
                    world.tickAll();

                    //Open pack missing screen if we don't have packs.
                    if (!player.isSpectator()) {
                        ControlSystem.controlGlobal(player);
                        if (((WrapperPlayer) player).player.ticksExisted % 100 == 0) {
                            if (!InterfaceManager.clientInterface.isGUIOpen() && !PackParser.arePacksPresent()) {
                                new GUIPackMissing();
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
                    //Update player guns.  These happen at the end since they need the player to update first.
                    world.beginProfiling("MTS_PlayerGunUpdates", true);
                    for (EntityPlayerGun gun : world.getEntitiesOfType(EntityPlayerGun.class)) {
                        gun.update();
                        gun.doPostUpdateLogic();
                    }

                    changedCameraState = false;
                    if (actuallyFirstPerson ^ Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
                        changedCameraState = true;
                        actuallyFirstPerson = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
                    }
                    if (actuallyThirdPerson ^ Minecraft.getMinecraft().gameSettings.thirdPersonView == 1) {
                        changedCameraState = true;
                        actuallyThirdPerson = Minecraft.getMinecraft().gameSettings.thirdPersonView == 1;
                    }
                    if (changeCameraRequest) {
                        if (actuallyFirstPerson) {
                            Minecraft.getMinecraft().gameSettings.thirdPersonView = 1;
                            actuallyFirstPerson = false;
                            actuallyThirdPerson = true;
                        } else {
                            Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
                            actuallyFirstPerson = true;
                            actuallyThirdPerson = false;
                        }
                        changeCameraRequest = false;
                    }
                }
                world.endProfiling();
            }
        }
    }
}
