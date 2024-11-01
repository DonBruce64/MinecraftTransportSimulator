package mcinterface1182;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import minecrafttransportsimulator.baseclasses.EntityManager;
import minecrafttransportsimulator.guis.instances.GUIConfig;
import minecrafttransportsimulator.jsondefs.JSONConfigClient.ConfigJoystick;
import minecrafttransportsimulator.mcinterface.IInterfaceInput;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem.ControlsJoystick;
import minecrafttransportsimulator.systems.LanguageSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(Dist.CLIENT)
public class InterfaceInput implements IInterfaceInput {
    //Common variables.
    private static KeyMapping configKey;
    private static KeyMapping importKey;
    private static int lastScrollValue;

    //Joystick variables.
    private static boolean runningJoystickThread = false;
    private static boolean joystickEnabled = false;
    private static boolean joystickBlocked = false;
    private static final Map<String, Integer> joystickNameCounters = new HashMap<>();

    //Normal mode joystick variables.
    private static final Map<String, Integer> joystickMap = new LinkedHashMap<>();
    private static final Map<String, Integer> joystickAxisCounts = new LinkedHashMap<>();
    private static final Map<String, Integer> joystickHatCounts = new LinkedHashMap<>();
    private static final Map<String, Integer> joystickButtonCounts = new LinkedHashMap<>();
    private static final Map<String, Integer> joystickComponentCounts = new LinkedHashMap<>();

    @Override
    public int getKeysetID() {
        return 16;
    }

    @Override
    public void initConfigKey() {
        configKey = new KeyMapping(LanguageSystem.GUI_MASTERCONFIG.getCurrentValue(), GLFW.GLFW_KEY_P, InterfaceLoader.MODNAME);
        ClientRegistry.registerKeyBinding(configKey);
        importKey = new KeyMapping(LanguageSystem.GUI_IMPORT.getCurrentValue(), GLFW.GLFW_KEY_UNKNOWN, InterfaceLoader.MODNAME);
        ClientRegistry.registerKeyBinding(importKey);
    }

    @Override
    public void initJoysticks() {
        //Populate the joystick device map.
        //Joystick will be enabled if at least one controller is found.  If none are found, we likely have an error.
        //We can re-try this if the user removes their mouse and we re-run this method.
        if (!runningJoystickThread) {
            runningJoystickThread = true;
            joystickBlocked = true;
            Thread joystickThread = new Thread(() -> {
                try {
                    joystickNameCounters.clear();
                    joystickMap.clear();
                    joystickAxisCounts.clear();
                    joystickHatCounts.clear();
                    joystickButtonCounts.clear();
                    joystickComponentCounts.clear();
                    for (int i = GLFW.GLFW_JOYSTICK_1; i < GLFW.GLFW_JOYSTICK_16; ++i) {
                        joystickEnabled = true;
                        if (GLFW.glfwGetJoystickName(i) != null && GLFW.glfwGetJoystickAxes(i).limit() > 0 && GLFW.glfwGetJoystickButtons(i).limit() > 0) {
                            String joystickName = GLFW.glfwGetJoystickName(i);

                            //Add an index on this joystick to be sure we don't override multi-component units.
                            if (!joystickNameCounters.containsKey(joystickName)) {
                                joystickNameCounters.put(joystickName, 0);
                            }
                            String joystickID = joystickName + "_" + joystickNameCounters.get(joystickName);
                            joystickMap.put(joystickID, i);
                            joystickNameCounters.put(joystickName, joystickNameCounters.get(joystickName) + 1);
                            joystickAxisCounts.put(joystickID, GLFW.glfwGetJoystickAxes(i).limit());
                            joystickHatCounts.put(joystickID, GLFW.glfwGetJoystickHats(i).limit());
                            joystickButtonCounts.put(joystickID, GLFW.glfwGetJoystickButtons(i).limit());
                            joystickComponentCounts.put(joystickID, joystickAxisCounts.get(joystickID) + joystickHatCounts.get(joystickID) + joystickButtonCounts.get(joystickID));
                        }
                    }

                    //Validate joysticks are valid for this setup by making sure indexes aren't out of bounds.
                    Iterator<Entry<String, ConfigJoystick>> iterator = ConfigSystem.client.controls.joystick.entrySet().iterator();
                    while (iterator.hasNext()) {
                        try {
                            Entry<String, ConfigJoystick> controllerEntry = iterator.next();
                            ControlsJoystick control = ControlsJoystick.valueOf(controllerEntry.getKey().toUpperCase(Locale.ROOT));
                            ConfigJoystick config = controllerEntry.getValue();
                            if (joystickMap.containsKey(config.joystickName)) {
                                if (control.isAxis) {
                                    if (joystickAxisCounts.get(config.joystickName) <= config.buttonIndex) {
                                        iterator.remove();
                                        InterfaceManager.coreInterface.logError("Removed joystick with too low axis count.  Had " + joystickAxisCounts.get(config.joystickName) + " requested " + config.buttonIndex);
                                    }
                                } else {
                                    if (joystickComponentCounts.get(config.joystickName) <= config.buttonIndex) {
                                        iterator.remove();
                                        InterfaceManager.coreInterface.logError("Removed joystick with too low button count.  Had " + joystickComponentCounts.get(config.joystickName) + " requested " + config.buttonIndex);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            //Invalid control.
                            iterator.remove();
                        }
                    }

                    joystickBlocked = false;
                } catch (Exception e) {
                    e.printStackTrace();
                    InterfaceManager.coreInterface.logError(e.getMessage());
                    for (StackTraceElement s : e.getStackTrace()) {
                        InterfaceManager.coreInterface.logError(s.toString());
                    }
                }
                runningJoystickThread = false;
            });
            joystickThread.start();
        }
    }

    @Override
    public String getNameForKeyCode(int keyCode) {
        return InputConstants.getKey(keyCode, 0).getDisplayName().getString();
    }

    @Override
    public int getKeyCodeForName(String name) {
        //Convert input to new control system prior to checking.
        //We only need to convert defaults.
        switch (name) {
            case "RSHIFT":
                return InputConstants.getKey("key.keyboard.right.shift").getValue();
            case "PRIOR":
                return InputConstants.getKey("key.keyboard.page.up").getValue();
            case "NEXT":
                return InputConstants.getKey("key.keyboard.page.down").getValue();
            case "SCROLL":
                return InputConstants.getKey("key.keyboard.scroll.lock").getValue();
            default: {
                if (name.contains("NUMPAD")) {
                    return InputConstants.getKey("key.keyboard.keypad." + name.substring(name.length() - 1)).getValue();
                } else {
                    return InputConstants.getKey("key.keyboard." + name.toLowerCase(Locale.ROOT)).getValue();
                }
            }
        }
    }

    @Override
    public boolean isKeyPressed(int keyCode) {
        return GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), keyCode) == GLFW.GLFW_PRESS;
    }

    @Override
    public void setGUIControls(boolean enabled) {
        //Nothing to do as these are always enabled it would seem.
    }

    @Override
    public boolean isJoystickSupportEnabled() {
        return joystickEnabled;
    }

    @Override
    public boolean isJoystickSupportBlocked() {
        return joystickBlocked;
    }

    @Override
    public boolean isJoystickPresent(String joystickName) {
        return joystickMap.containsKey(joystickName);
    }

    @Override
    public List<String> getAllJoystickNames() {
        return new ArrayList<>(joystickMap.keySet());
    }

    @Override
    public int getJoystickComponentCount(String joystickName) {
        return joystickComponentCounts.get(joystickName);
    }

    @Override
    public String getJoystickComponentName(String joystickName, int index) {
        if (isJoystickComponentAxis(joystickName, index)) {
            return "Axis: " + String.valueOf(index);
        } else {
            if (index < joystickAxisCounts.get(joystickName) + joystickHatCounts.get(joystickName)) {
                return "Hat: " + String.valueOf(index - joystickAxisCounts.get(joystickName));
            } else {
                return "Button: " + String.valueOf(index - joystickAxisCounts.get(joystickName) - joystickHatCounts.get(joystickName));
            }
        }
    }

    @Override
    public boolean isJoystickComponentAxis(String joystickName, int index) {
        return GLFW.glfwGetJoystickAxes(joystickMap.get(joystickName)).limit() > index;
    }

    @Override
    public float getJoystickAxisValue(String joystickName, int index) {
        //Check to make sure this control is operational before testing.  It could have been removed from a prior game.
        //Make sure we're not calling this on non-axis.
        //This could be a hat switch hiding.
        if (joystickMap.containsKey(joystickName)) {
            if (isJoystickComponentAxis(joystickName, index)) {
                return GLFW.glfwGetJoystickAxes(joystickMap.get(joystickName)).get(index);
            } else if (index < joystickAxisCounts.get(joystickName) + joystickHatCounts.get(joystickName)) {
                switch (GLFW.glfwGetJoystickHats(joystickMap.get(joystickName)).get(index - joystickAxisCounts.get(joystickName))) {
                    case (GLFW.GLFW_HAT_UP):
                        return 0.25F;
                    case (GLFW.GLFW_HAT_LEFT):
                        return 0.5F;
                    case (GLFW.GLFW_HAT_DOWN):
                        return 0.75F;
                    case (GLFW.GLFW_HAT_RIGHT):
                        return 1.0F;
                    default:
                        return 1.0F;
                }
            } else {
                return GLFW.glfwGetJoystickButtons(joystickMap.get(joystickName)).get(index - joystickAxisCounts.get(joystickName) - joystickHatCounts.get(joystickName)) == GLFW.GLFW_PRESS ? 1 : 0;
            }
        } else {
            return 0;
        }
    }

    @Override
    public boolean getJoystickButtonValue(String joystickName, int index) {
        //Check to make sure this control is operational before testing.  It could have been removed from a prior game.
        if (joystickMap.containsKey(joystickName)) {
            return GLFW.glfwGetJoystickButtons(joystickMap.get(joystickName)).get(index - joystickAxisCounts.get(joystickName) - joystickHatCounts.get(joystickName)) == GLFW.GLFW_PRESS;
        } else {
            return false;
        }
    }

    @Override
    public int getTrackedMouseWheel() {
        int returnValue = lastScrollValue;
        lastScrollValue = 0;
        return returnValue;
    }

    @Override
    public boolean isLeftMouseButtonDown() {
        return Minecraft.getInstance().options.keyAttack.isDown();
    }

    @Override
    public boolean isRightMouseButtonDown() {
        return Minecraft.getInstance().options.keyUse.isDown();
    }

    /**
     * Opens the config screen when the config key is pressed.
     * Also init the joystick system if we haven't already.
     */
    @SubscribeEvent
    public static void onIVKeyInput(KeyInputEvent event) {
        //Check if we pressed the config or import key.
        if (configKey.isDown() && !InterfaceManager.clientInterface.isGUIOpen()) {
            new GUIConfig();
        } else if (ConfigSystem.settings.general.devMode.value && importKey.isDown()) {
        	EntityManager.doImports(() -> InterfaceManager.clientInterface.getClientPlayer().displayChatMessage(LanguageSystem.SYSTEM_DEBUG, JSONParser.importAllJSONs(true)));
        }
    }

    /**
     * Gets mouse scroll data, since we have to register a listner, and MC already does this for us.
     */
    @SubscribeEvent
    public static void onIVMouseScroll(ScreenEvent.MouseScrollEvent.Post event) {
        if (InterfaceManager.clientInterface.isGUIOpen()) {
            lastScrollValue = (int) event.getScrollDelta();
            event.setCanceled(true);
        }
    }
}
