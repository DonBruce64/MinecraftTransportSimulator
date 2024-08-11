package mcinterface1165;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIPackMissing;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IInterfaceClient;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.LanguageSystem;
import net.minecraft.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(Dist.CLIENT)
public class InterfaceClient implements IInterfaceClient {
    private static CameraMode actualCameraMode;
    private static CameraMode cameraModeRequest;
    private static int ticksToCullingWarning = 200;

    @Override
    public boolean isGamePaused() {
        return Minecraft.getInstance().isPaused();
    }

    @Override
    public String getLanguageName() {
        if (Minecraft.getInstance().getLanguageManager() != null) {
            return Minecraft.getInstance().getLanguageManager().getSelected().getCode();
        } else {
            return "en_us";
        }
    }

    @Override
    public List<String> getAllLanguages() {
        List<String> list = new ArrayList<>();
        Minecraft.getInstance().getLanguageManager().getLanguages().forEach(language -> list.add(language.getCode()));
        return list;
    }

    @Override
    public String getFluidName(String fluidID) {
        for (Entry<RegistryKey<Fluid>, Fluid> fluidEntry : ForgeRegistries.FLUIDS.getEntries()) {
            if (fluidEntry.getKey().location().getPath().equals(fluidID)) {
                return new TranslationTextComponent(fluidEntry.getValue().getAttributes().getTranslationKey()).getString();
            }
        }
        return "INVALID";
    }

    @Override
    public Map<String, String> getAllFluidNames() {
        Map<String, String> fluidIDsToNames = new HashMap<>();
        for (Fluid fluid : ForgeRegistries.FLUIDS.getValues()) {
            fluidIDsToNames.put(fluid.getRegistryName().getPath(), new FluidStack(fluid, 1).getDisplayName().getString());
        }
        return fluidIDsToNames;
    }

    @Override
    public boolean isChatOpen() {
        return Minecraft.getInstance().screen instanceof ChatScreen;
    }

    @Override
    public boolean isGUIOpen() {
        return Minecraft.getInstance().screen != null;
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
        return 0;
    }

    @Override
    public long getPackedDisplaySize() {
        return (((long) Minecraft.getInstance().getWindow().getGuiScaledWidth()) << Integer.SIZE) | (Minecraft.getInstance().getWindow().getGuiScaledHeight() & 0xffffffffL);
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
    public Point3D getCameraPosition() {
        Vector3d cameraOffset = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        mutablePosition.set(cameraOffset.x, cameraOffset.y, cameraOffset.z);
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
        for (int i = 0; i < tooltipLines.size(); ++i) {
            ITextComponent component = tooltipLines.get(i);
            Style style = component.getStyle();
            String stringToAdd = "";
            if (style.isBold()) {
                stringToAdd += RenderText.FORMATTING_CHAR + RenderText.BOLD_FORMATTING_CHAR;
            }
            if (style.isItalic()) {
                stringToAdd += RenderText.FORMATTING_CHAR + RenderText.ITALIC_FORMATTING_CHAR;
            }
            if (style.isUnderlined()) {
                stringToAdd += RenderText.FORMATTING_CHAR + RenderText.UNDERLINE_FORMATTING_CHAR;
            }
            if (style.isStrikethrough()) {
                stringToAdd += RenderText.FORMATTING_CHAR + RenderText.STRIKETHROUGH_FORMATTING_CHAR;
            }
            if (style.isObfuscated()) {
                stringToAdd += RenderText.FORMATTING_CHAR + RenderText.RANDOM_FORMATTING_CHAR;
            }
            if (style.getColor() != null) {
                TextFormatting legacyColor = null;
                for (TextFormatting format : TextFormatting.values()) {
                    if (format.isColor()) {
                        if (style.getColor().equals(Color.fromLegacyFormat(format))) {
                            legacyColor = format;
                            break;
                        }
                    }
                }
                if (legacyColor != null) {
                    stringToAdd += RenderText.FORMATTING_CHAR + Integer.toHexString(legacyColor.ordinal());
                }
            }
            tooltipText.add(stringToAdd + tooltipLines.get(i).getString());
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
            AWrapperWorld world = InterfaceManager.clientInterface.getClientWorld();
            if (world != null) {
                if (event.phase.equals(Phase.START)) {
                    world.beginProfiling("MTS_ClientVehicleUpdates", true);
                    world.tickAll(true);

                    //Need to update world brightness since sky darken isn't calculated normally on clients.
                    ((WrapperWorld) world).world.updateSkyBrightness();

                    //Open pack missing screen if we don't have packs.
                    if (!player.isSpectator()) {
                        ControlSystem.controlGlobal(player);
                        if (((WrapperPlayer) player).player.tickCount % 100 == 0) {
                            if (!InterfaceManager.clientInterface.isGUIOpen() && !PackParser.arePacksPresent()) {
                                new GUIPackMissing();
                            }
                        }
                    }
                    
                    //Complain about compats at 10 second mark.
                    if (ConfigSystem.settings.general.performModCompatFunctions.value) {
                    	if(ticksToCullingWarning > 0) {
                    		if(--ticksToCullingWarning == 0) {
                                if (InterfaceManager.coreInterface.isModPresent("entityculling")) {
                                    player.displayChatMessage(LanguageSystem.SYSTEM_DEBUG, "IV HAS DETECTED THAT ENTITY CULLING MOD IS PRESENT.  THIS MOD CULLS ALL IV VEHICLES UNLESS \"mts:builder_existing\", \"mts:builder_rendering\", AND \"mts:builder_seat\" ARE ADDED TO THE WHITELIST.");
                                }
                                if (InterfaceManager.coreInterface.isModPresent("modernfix")) {
                                    player.displayChatMessage(LanguageSystem.SYSTEM_DEBUG, "IV HAS DETECTED THAT MODERNFIX MOD IS PRESENT.  IF DYNAMIC RESOURCES IS SET TO TRUE IV ITEMS WILL NOT RENDER PROPERLY.");
                                }
                    		}
                    	}
                    }
                    world.endProfiling();
                } else {
                    world.tickAll(false);
                    
                    //Handle camera requests.
                    if(cameraModeRequest != null) {
                    	switch(cameraModeRequest) {
	                    	case FIRST_PERSON:{
	                    		Minecraft.getInstance().options.setCameraType(PointOfView.FIRST_PERSON);
	                    		break;
	                    	}
	                    	case THIRD_PERSON:{
	                    		Minecraft.getInstance().options.setCameraType(PointOfView.THIRD_PERSON_BACK);
	                    		break;
	                    	}
	                    	case THIRD_PERSON_INVERTED:{
	                    		Minecraft.getInstance().options.setCameraType(PointOfView.THIRD_PERSON_FRONT);
	                    		break;
	                    	}
                    	}
                    	cameraModeRequest = null;
                    }

                    //Update camera state, since this can change depending on tick if we check during renders.
                    PointOfView cameraModeEnum  = Minecraft.getInstance().options.getCameraType();
                    switch(cameraModeEnum) {
                    	case FIRST_PERSON:{
                    		actualCameraMode = CameraMode.FIRST_PERSON;
                    		break;
                    	}
                    	case THIRD_PERSON_BACK:{
                    		actualCameraMode = CameraMode.THIRD_PERSON;
                    		break;
                    	}
                    	case THIRD_PERSON_FRONT:{
                    		actualCameraMode = CameraMode.THIRD_PERSON_INVERTED;
                    		break;
                    	}
                    }
                }
            }
        }
    }
}
