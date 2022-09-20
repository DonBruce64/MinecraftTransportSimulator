package mcinterface1122;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.input.Controllers;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

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
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(Side.CLIENT)
public class InterfaceInput implements IInterfaceInput {
    //Common variables.
    private static KeyBinding configKey;

    //Joystick variables.
    private static boolean runningJoystickThread = false;
    private static boolean runningClassicMode = false;
    private static boolean joystickLoadingAttempted = false;
    private static boolean joystickEnabled = false;
    private static boolean joystickBlocked = false;
    private static boolean joystickInhibited = false;
    private static final Map<String, Integer> joystickNameCounters = new HashMap<>();

    //Normal mode joystick variables.
    private static final Map<String, org.lwjgl.input.Controller> joystickMap = new LinkedHashMap<>();
    private static final Map<String, Integer> joystickAxisCountMap = new LinkedHashMap<>();

    //Classic mode joystick variables.
    private static final Map<String, net.java.games.input.Controller> classicJoystickMap = new LinkedHashMap<>();

    @Override
    public void initConfigKey() {
        configKey = new KeyBinding(JSONConfigLanguage.GUI_MASTERCONFIG.value, Keyboard.KEY_P, InterfaceLoader.MODNAME);
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
                        if (!Controllers.isCreated()) {
                            if (ConfigSystem.settings.general.devMode.value)
                                InterfaceManager.coreInterface.logError("Creating controller object.");
                            Controllers.create();
                        }
                        joystickMap.clear();
                        joystickAxisCountMap.clear();
                        if (ConfigSystem.settings.general.devMode.value)
                            InterfaceManager.coreInterface.logError("Found this many controllers: " + Controllers.getControllerCount());
                        for (int i = 0; i < Controllers.getControllerCount(); ++i) {
                            joystickEnabled = true;
                            org.lwjgl.input.Controller joystick = Controllers.getController(i);
                            if (joystick.getAxisCount() > 0 && joystick.getButtonCount() > 0 && joystick.getName() != null) {
                                String joystickName = joystick.getName();
                                if (ConfigSystem.settings.general.devMode.value)
                                    InterfaceManager.coreInterface.logError("Found valid controller: " + joystickName);

                                //Add an index on this joystick to be sure we don't override multi-component units.
                                if (!joystickNameCounters.containsKey(joystickName)) {
                                    joystickNameCounters.put(joystickName, 0);
                                }
                                joystickMap.put(joystickName + "_" + joystickNameCounters.get(joystickName), joystick);
                                joystickAxisCountMap.put(joystickName + "_" + joystickNameCounters.get(joystickName), joystick.getAxisCount());
                                joystickNameCounters.put(joystickName, joystickNameCounters.get(joystickName) + 1);
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
                                        if (joystickMap.get(config.joystickName).getAxisCount() <= config.buttonIndex) {
                                            iterator.remove();
                                        }
                                    } else {
                                        if (joystickMap.get(config.joystickName).getButtonCount() <= config.buttonIndex - joystickAxisCountMap.get(config.joystickName)) {
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
        return Keyboard.getKeyName(keyCode);
    }

    @Override
    public int getKeyCodeForName(String name) {
        return Keyboard.getKeyIndex(name);
    }

    @Override
    public boolean isKeyPressed(int keyCode) {
        return Keyboard.isKeyDown(keyCode);
    }

    @Override
    public void setKeyboardRepeat(boolean enabled) {
        Keyboard.enableRepeatEvents(enabled);
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
        return runningClassicMode ? classicJoystickMap.get(joystickName).getComponents().length : joystickMap.get(joystickName).getAxisCount() + joystickMap.get(joystickName).getButtonCount();
    }

    @Override
    public String getJoystickComponentName(String joystickName, int index) {
        return runningClassicMode ? classicJoystickMap.get(joystickName).getComponents()[index].getName() : (isJoystickComponentAxis(joystickName, index) ? joystickMap.get(joystickName).getAxisName(index) : joystickMap.get(joystickName).getButtonName(index - joystickAxisCountMap.get(joystickName)));
    }

    @Override
    public boolean isJoystickComponentAxis(String joystickName, int index) {
        return runningClassicMode ? classicJoystickMap.get(joystickName).getComponents()[index].isAnalog() : joystickMap.get(joystickName).getAxisCount() > index;
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
            if (joystickMap.containsKey(joystickName)) {
                if (isJoystickComponentAxis(joystickName, index)) {
                    joystickMap.get(joystickName).poll();
                    return joystickMap.get(joystickName).getAxisValue(index);
                } else {
                    return getJoystickButtonValue(joystickName, index) ? 1 : 0;
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
                joystickMap.get(joystickName).poll();
                return joystickMap.get(joystickName).isButtonPressed(index - joystickAxisCountMap.get(joystickName));
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
        return Mouse.hasWheel() ? Mouse.getDWheel() : 0;
    }

    @Override
    public boolean isLeftMouseButtonDown() {
        return Minecraft.getMinecraft().gameSettings.keyBindAttack.isKeyDown();
    }

    @Override
    public boolean isRightMouseButtonDown() {
        return Minecraft.getMinecraft().gameSettings.keyBindUseItem.isKeyDown();
    }

    /**
     * Opens the config screen when the config key is pressed.
     * Also init the joystick system if we haven't already.
     */
    @SubscribeEvent
    public static void on(InputEvent.KeyInputEvent event) {
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
        if (configKey.isPressed() && !InterfaceManager.clientInterface.isGUIOpen()) {
            new GUIConfig();
        }
    }
}
