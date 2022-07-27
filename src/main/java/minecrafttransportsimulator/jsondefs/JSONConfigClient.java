package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;

import java.util.HashMap;
import java.util.Map;

/**
 * Config class for client settings. This is only loaded on the client. This allows the
 * server to send over its config data, but the client-side stuff gets left alone.
 *
 * @author don_bruce
 */
public class JSONConfigClient {
    public final JSONRenderingSettings renderingSettings = new JSONRenderingSettings();
    public final JSONControlSettings controlSettings = new JSONControlSettings();
    public final JSONControls controls = new JSONControls();

    public static class JSONRenderingSettings {
        public final JSONConfigEntry<Boolean> renderHUD_1P = new JSONConfigEntry<>(true, "If false, the HUD in vehicles will not render in 1st-person mode.");
        public final JSONConfigEntry<Boolean> renderHUD_3P = new JSONConfigEntry<>(true, "If false, the HUD in vehicles will not render in 3rd-person mode.");

        public final JSONConfigEntry<Boolean> fullHUD_1P = new JSONConfigEntry<>(false, "If true, the full-size HUD will render in 1st-person rather than the half-size HUD.");
        public final JSONConfigEntry<Boolean> fullHUD_3P = new JSONConfigEntry<>(false, "If true, the full-size HUD will render in 3rd-person rather than the half-size HUD.");

        public final JSONConfigEntry<Boolean> transpHUD_1P = new JSONConfigEntry<>(false, "If true, the background textures for the HUD will not be rendered in 1st-person.");
        public final JSONConfigEntry<Boolean> transpHUD_3P = new JSONConfigEntry<>(false, "If true, the background textures for the HUD will not be rendered in 1st-person.");

        public final JSONConfigEntry<Boolean> renderWindows = new JSONConfigEntry<>(true, "Should the glass on windows be rendered on vehicles?");
        public final JSONConfigEntry<Boolean> innerWindows = new JSONConfigEntry<>(false, "Should the glass on windows be rendered on the inside of the vehicle? Note: if renderWindows is false, this config has no effect.");

        public final JSONConfigEntry<Boolean> vehicleBeams = new JSONConfigEntry<>(true, "If false, beams on vehicles will not render.");
        public final JSONConfigEntry<Boolean> blockBeams = new JSONConfigEntry<>(true, "If false, beams on blocks will not render.");

        public final JSONConfigEntry<Boolean> brightLights = new JSONConfigEntry<>(true, "If false, lights from vehicles and blocks will not do brightness blending and will render as if they were part of the model at that same brightness. Useful if you have shaders and this is causing troubles.");
        public final JSONConfigEntry<Boolean> blendedLights = new JSONConfigEntry<>(true, "If false, beam-based lights from vehicles and blocks will not do brightness blending. This is different from the general brightness setting as this will do OpenGL blending on the world to make it brighter, not just the beams themselves.");

        public final JSONConfigEntry<Boolean> playerTweaks = new JSONConfigEntry<>(true, "If true, player hands will be modified when holding guns, and hands and legs will be modified when riding in vehicles. Set this to false (and restart the game) if mods cause issues, like two-hand rendering or player model issues. Automatically set to false if some mods are detected.");

    }

    public static class JSONControlSettings {
        public final JSONConfigEntry<Boolean> mouseYoke = new JSONConfigEntry<>(false, "Enable mouse yoke for vehicles? Prevents looking around unless unlocked. Think MCHeli controls.");
        public final JSONConfigEntry<Boolean> kbOverride = new JSONConfigEntry<>(true, "Should keyboard controls be ignored when a joystick control is mapped? Leave true to free up the keyboard while using a joystick.");

        public final JSONConfigEntry<Boolean> simpleThrottle = new JSONConfigEntry<>(true, "If true, then vehicles will automatically go into reverse after stopped with the brake rather than staying stopped and waiting for you to shift. When going in reverse, the opposite is true: the vehicle will shift into forwards when pressing forwards when stopped. Additionally, the parking brake will automatically be set when leaving the vehicle.");
        public final JSONConfigEntry<Boolean> halfThrottle = new JSONConfigEntry<>(false, "If true, then the gas key will only be a half-throttle, with the MOD+Throttle key becoming the full-speed control. Useful if you want a more controlled vehicle experience. Only valid on car/boat types with on-off throttles, and does not work in conjunction with simpleThrottle as that changes how the MOD key works with gas and brake keys.");

        public final JSONConfigEntry<Boolean> autostartEng = new JSONConfigEntry<>(true, "If true, engines will automatically start when a driver enters a vehicle, and will turn off when they leave. The parking brake will also be applied when leaving the vehicle. Note: this does not bypass the fuel or electrical system.");
        public final JSONConfigEntry<Boolean> autoTrnSignals = new JSONConfigEntry<>(true, "If true, turns signals will come on automatically when you start a turn, and will turn off when the turn completes. If this is false, then they will only be able to be activated with the keybinds or via the panel.");

        public final JSONConfigEntry<Boolean> heliAutoLevel = new JSONConfigEntry<>(true, "If true, helicopters will automatically return to level flight when you let off the control stick. However, this will prevent them from doing loops. The realistic value for this config is false, but the one that's more player-friendly is true. Hence it being the default.");
        public final JSONConfigEntry<Boolean> classicJystk = new JSONConfigEntry<>(false, "If true, the classic controller code will be used. Note: THIS CODE MAY CRASH MOBILE DEVICES!  Also note that switching will probably mess up your keybinds. Only do this if you are having issues with a joystick or controller not being recognized. After changing this setting, reboot the game to make it take effect.");

        public final JSONConfigEntry<Boolean> north360 = new JSONConfigEntry<>(false, "If true, instruments will represent North as 360 degrees, instead of the Minecraft default of 180. Allows using the heading system that real-world pilots and militaries do.");

        public final JSONConfigEntry<Double> steeringControlRate = new JSONConfigEntry<>(EntityVehicleF_Physics.RUDDER_DAMPEN_RATE, "How many degrees to turn the wheels on vehicles for every tick the button is held down. This is not used when using a joystick.");
        public final JSONConfigEntry<Double> flightControlRate = new JSONConfigEntry<>(EntityVehicleF_Physics.AILERON_DAMPEN_RATE, "How many degrees to move the elevators and ailerons on aircraft for every tick the button is held down. This is not used when using a joystick.");
        public JSONConfigEntry<Double> mouseYokeRate = new JSONConfigEntry<>(0.1D, "How many degrees to move control surfaces for every 1 mouse unit change. Used for mouse yoke controls.");
        public final JSONConfigEntry<Double> joystickDeadZone = new JSONConfigEntry<>(0.03D, "Dead zone for joystick axis. This is NOT joystick specific.");
        public final JSONConfigEntry<Float> masterVolume = new JSONConfigEntry<>(1.0F, "Volume for all sounds in the mod. This is used instead of the game's master volume.");
    }

    public static class JSONControls {
        public final Map<String, ConfigKeyboard> keyboard = new HashMap<>();
        public final Map<String, ConfigJoystick> joystick = new HashMap<>();
    }

    public static class ConfigKeyboard {
        public int keyCode;
    }

    public static class ConfigJoystick {
        public String joystickName;
        public int buttonIndex;
        public boolean invertedAxis;
        public double axisMinTravel;
        public double axisMaxTravel;
    }
}
