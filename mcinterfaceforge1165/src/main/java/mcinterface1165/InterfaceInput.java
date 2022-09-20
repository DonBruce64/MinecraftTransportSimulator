package mcinterface1165;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.glfw.GLFW;

import minecrafttransportsimulator.guis.instances.GUIConfig;
import minecrafttransportsimulator.jsondefs.JSONConfigClient.ConfigJoystick;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.mcinterface.IInterfaceInput;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem.ControlsJoystick;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.client.event.InputEvent.MouseScrollEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(Dist.CLIENT)
public class InterfaceInput implements IInterfaceInput {
    //Common variables.
    private static KeyBinding configKey;
    private static boolean repeatEnabled = false;
    private static int lastScrollValue;

    //Joystick variables.
    private static boolean runningJoystickThread = false;
    private static boolean runningClassicMode = false;
    private static boolean joystickLoadingAttempted = false;
    private static boolean joystickEnabled = false;
    private static boolean joystickBlocked = false;
    private static boolean joystickInhibited = false;
    private static final Map<String, Integer> joystickNameCounters = new HashMap<>();

    //Normal mode joystick variables.
    private static final Map<String, Integer> joystickMap = new LinkedHashMap<>();
    private static final Map<String, Integer> joystickAxisCounts = new LinkedHashMap<>();
    private static final Map<String, Integer> joystickHatCounts = new LinkedHashMap<>();
    private static final Map<String, Integer> joystickButtonCounts = new LinkedHashMap<>();
    private static final Map<String, Integer> joystickComponentCounts = new LinkedHashMap<>();

    //Classic mode joystick variables.
    private static final Map<String, net.java.games.input.Controller> classicJoystickMap = new LinkedHashMap<>();

    @Override
    public void initConfigKey() {
        configKey = new KeyBinding(JSONConfigLanguage.GUI_MASTERCONFIG.value, GLFW.GLFW_KEY_P, InterfaceLoader.MODNAME);
        ClientRegistry.registerKeyBinding(configKey);
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
                    if (ConfigSystem.settings.general.devMode.value)
                        InterfaceManager.coreInterface.logError("Starting controller init.");
                    if (runningClassicMode) {
                        if (ConfigSystem.settings.general.devMode.value)
                            InterfaceManager.coreInterface.logError("Running classic mode.");
                        classicJoystickMap.clear();
                        if (ConfigSystem.settings.general.devMode.value)
                            InterfaceManager.coreInterface.logError("Found this many controllers: " + ControllerEnvironment.getDefaultEnvironment().getControllers().length);
                        for (Controller joystick : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
                            joystickEnabled = true;
                            if (joystick.getType() != null && !joystick.getType().equals(Controller.Type.MOUSE) && !joystick.getType().equals(Controller.Type.KEYBOARD) && joystick.getName() != null && joystick.getComponents().length != 0) {
                                String joystickName = joystick.getName();
                                if (ConfigSystem.settings.general.devMode.value)
                                    InterfaceManager.coreInterface.logError("Found valid controller: " + joystickName);

                                //Add an index on this joystick to be sure we don't override multi-component units.
                                if (!joystickNameCounters.containsKey(joystickName)) {
                                    joystickNameCounters.put(joystickName, 0);
                                }
                                classicJoystickMap.put(joystickName + "_" + joystickNameCounters.get(joystickName), joystick);
                                joystickNameCounters.put(joystickName, joystickNameCounters.get(joystickName) + 1);
                            }
                        }
                    } else {
                        if (ConfigSystem.settings.general.devMode.value)
                            InterfaceManager.coreInterface.logError("Running modern mode.");
                        joystickMap.clear();
                        joystickAxisCounts.clear();
                        joystickHatCounts.clear();
                        joystickButtonCounts.clear();
                        joystickComponentCounts.clear();
                        for (int i = GLFW.GLFW_JOYSTICK_1; i < GLFW.GLFW_JOYSTICK_16; ++i) {
                            joystickEnabled = true;
                            if (GLFW.glfwGetJoystickName(i) != null && GLFW.glfwGetJoystickAxes(i).limit() > 0 && GLFW.glfwGetJoystickButtons(i).limit() > 0) {
                                String joystickName = GLFW.glfwGetJoystickName(i);
                                if (ConfigSystem.settings.general.devMode.value)
                                    InterfaceManager.coreInterface.logError("Found valid controller: " + joystickName);

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
                    }

                    //Validate joysticks are valid for this setup by making sure indexes aren't out of bounds.
                    Iterator<Entry<String, ConfigJoystick>> iterator = ConfigSystem.client.controls.joystick.entrySet().iterator();
                    if (ConfigSystem.settings.general.devMode.value)
                        InterfaceManager.coreInterface.logError("Performing button validity checks.");
                    while (iterator.hasNext()) {
                        try {
                            Entry<String, ConfigJoystick> controllerEntry = iterator.next();
                            ControlsJoystick control = ControlsJoystick.valueOf(controllerEntry.getKey().toUpperCase());
                            ConfigJoystick config = controllerEntry.getValue();
                            if (runningClassicMode) {
                                if (classicJoystickMap.containsKey(config.joystickName)) {
                                    if (classicJoystickMap.get(config.joystickName).getComponents().length <= config.buttonIndex) {
                                        iterator.remove();
                                    }
                                }
                            } else {
                                if (joystickMap.containsKey(config.joystickName)) {
                                    if (control.isAxis) {
                                        if (joystickAxisCounts.get(config.joystickName) <= config.buttonIndex) {
                                            iterator.remove();
                                        }
                                    } else {
                                        if (joystickComponentCounts.get(config.joystickName) <= config.buttonIndex) {
                                            iterator.remove();
                                        }
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
        return GLFW.glfwGetKeyName(keyCode, GLFW.glfwGetKeyScancode(keyCode));
    }

    @Override
    public int getKeyCodeForName(String name) {
        for (int i = 0; i < GLFW.GLFW_KEY_LAST; ++i) {
            if (name.equals(GLFW.glfwGetKeyName(i, 0))) {
                return i;
            }
        }
        return GLFW.GLFW_KEY_UNKNOWN;
    }

    @Override
    public boolean isKeyPressed(int keyCode) {
        return GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), keyCode) == GLFW.GLFW_PRESS;
    }

    @Override
    public void setKeyboardRepeat(boolean enabled) {
        repeatEnabled = enabled;
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
        return !joystickInhibited && runningClassicMode ? classicJoystickMap.containsKey(joystickName) : joystickMap.containsKey(joystickName);
    }

    @Override
    public List<String> getAllJoystickNames() {
        return new ArrayList<>(runningClassicMode ? classicJoystickMap.keySet() : joystickMap.keySet());
    }

    @Override
    public int getJoystickComponentCount(String joystickName) {
        return runningClassicMode ? classicJoystickMap.get(joystickName).getComponents().length : joystickComponentCounts.get(joystickName);
    }

    @Override
    public String getJoystickComponentName(String joystickName, int index) {
        if (runningClassicMode) {
            return classicJoystickMap.get(joystickName).getComponents()[index].getName();
        } else {
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
    }

    @Override
    public boolean isJoystickComponentAxis(String joystickName, int index) {
        return runningClassicMode ? classicJoystickMap.get(joystickName).getComponents()[index].isAnalog() : GLFW.glfwGetJoystickAxes(joystickMap.get(joystickName)).limit() > index;
    }

    @Override
    public float getJoystickAxisValue(String joystickName, int index) {
        //Check to make sure this control is operational before testing.  It could have been removed from a prior game.
        if (runningClassicMode) {
            if (classicJoystickMap.containsKey(joystickName)) {
                classicJoystickMap.get(joystickName).poll();
                return classicJoystickMap.get(joystickName).getComponents()[index].getPollData();
            } else {
                return 0;
            }
        } else {
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
    }

    @Override
    public boolean getJoystickButtonValue(String joystickName, int index) {
        //Check to make sure this control is operational before testing.  It could have been removed from a prior game.
        if (runningClassicMode) {
            if (classicJoystickMap.containsKey(joystickName)) {
                classicJoystickMap.get(joystickName).poll();
                return classicJoystickMap.get(joystickName).getComponents()[index].getPollData() > 0;
            } else {
                return false;
            }
        } else {
            if (joystickMap.containsKey(joystickName)) {
                return GLFW.glfwGetJoystickButtons(joystickMap.get(joystickName)).get(index - joystickAxisCounts.get(joystickName) - joystickHatCounts.get(joystickName)) == GLFW.GLFW_PRESS;
            } else {
                return false;
            }
        }
    }

    @Override
    public void inhibitJoysticks(boolean inhibited) {
        joystickInhibited = inhibited;
    }

    @Override
    public int getTrackedMouseWheel() {
        return lastScrollValue;
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
    public static void on(KeyInputEvent event) {
        //Check if we switched joystick modes.
        if (runningClassicMode ^ ConfigSystem.client.controlSettings.classicJystk.value) {
            runningClassicMode = ConfigSystem.client.controlSettings.classicJystk.value;
            joystickLoadingAttempted = false;
        }

        //Init joysticks if we haven't already tried or if we switched loaders.
        if (!joystickLoadingAttempted) {
            InterfaceManager.inputInterface.initJoysticks();
            joystickLoadingAttempted = true;
        }

        //Check if we pressed the config key.
        if (configKey.isDown() && !InterfaceManager.clientInterface.isGUIOpen()) {
            new GUIConfig();
        }
    }

    /**
     * Gets mouse scroll data, since we have to register a listner, and MC already does this for us.
     */
    @SubscribeEvent
    public static void on(MouseScrollEvent event) {
        lastScrollValue = (int) event.getScrollDelta();
    }

    /**
     * Custom MouseHelper class that can have movemgent checks inhibited based on
     * settings in this class.  Allows us to prevent player movement.
     */
    /*private static class InhibitableMouseHelper extends MouseHelper {
        private int deltaXForced;
        private int deltaYForced;
    
        @Override
        public void mouseXYChange() {
            //If the mouse is disabled, capture the deltas and prevent MC from seeing them.
            //Don't capture high deltas, as this is likely due to the game pausing.
            super.mouseXYChange();
            if (!enableMouse) {
                deltaXForced = deltaX;
                if (deltaXForced > 100) {
                    deltaXForced = 0;
                }
                deltaYForced = deltaY;
                if (deltaYForced > 100) {
                    deltaYForced = 0;
                }
                deltaX = 0;
                deltaY = 0;
            }
        }
    }*/

}
