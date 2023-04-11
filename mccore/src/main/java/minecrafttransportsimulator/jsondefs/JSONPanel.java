package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

@JSONDescription("Panels are simply things that show the instruments in vehicles, and allow interacting with those instruments.  While not required, they do make standardizing vehicle instrument dispays far easier.  Panels go into the 'panels' folder located in the main pack folder.")
public class JSONPanel extends AJSONBase {

    @JSONRequired
    public JSONPanelMain panel;

    public static class JSONPanelMain {
        @JSONRequired
        @JSONDescription("The texture sheet to use for this panel.  Contains the background, buttons, switches, etc.  Make sure to include a _lit variant otherwise things will look weird!")
        public String texture;

        @JSONDescription("The width of the texture, in pixels.")
        public int textureWidth;

        @JSONDescription("The height of the texture, in pixels.")
        public int textureHeight;

        @JSONDescription("The width of the background section on the texture, in pixels.  The background must touch the left side!")
        public int backgroundWidth;

        @JSONDescription("The height of the background section on the texture, in pixels.  The background must touch the top!")
        public int backgroundHeight;

        @JSONDescription("The color for the text in the panel that renders below components.  If this is not included it will default to white.")
        public ColorRGB textColor;

        @JSONDescription("Same as textColor, but for the text when the vehicle's lights are on.")
        public ColorRGB litTextColor;

        @JSONDescription("A list of components that go onto this panel.  These are things that can be seen and interacted with.")
        public List<JSONPanelComponent> components;
    }

    public static class JSONPanelComponent {

        @JSONDescription("The position of where to place this component on the panel, with the anchor at the top-left.")
        public Point3D pos;

        @JSONDescription("The width of the texture area, and the component itself.")
        public int width;

        @JSONDescription("The height of the texture area, and the component itself.")
        public int height;

        @JSONDescription("The top-left coordinate of where this panel's 0-state texture is located.")
        public Point3D textureStart;

        @JSONDescription("Descriptive text to be rendered below this control.")
        public String text;

        @JSONDescription("The variable used to choose the texture state.  May be omitted if this component doesn't change textures.")
        public String statusVariable;

        @JSONDescription("A list of conditions that determines if this component is visibile (default is visible).")
        public List<JSONCondition> visibilityConditions;

        @JSONDescription("The special component to make this component.  May be null.  If set, special logic will be applied that is per-component.")
        public SpecialComponent specialComponent;

        @JSONDescription("The action to perform when this control is clicked.")
        public JSONPanelClickAction clickAction;

        @JSONDescription("The action to perform when this control is clicked on the left side.")
        public JSONPanelClickAction clickActionLeft;

        @JSONDescription("The action to perform when this control is clicked on the right side.")
        public JSONPanelClickAction clickActionRight;

        @Deprecated
        public List<List<String>> visibilityVariables;
    }

    public static class JSONPanelClickAction {

        @JSONDescription("The variable to act on.")
        public String variable;

        @JSONDescription("The type of action to perform when clicked.")
        public ActionType action;

        @JSONDescription("The value to use in the action on the variable (if applicable).")
        public double value;

        @JSONDescription("The min value for this control to set.  Only used when the action type is increment.")
        public float clampMin;

        @JSONDescription("The max value for this control to set.  Only used when the action type is increment.")
        public float clampMax;
    }

    public static enum SpecialComponent {
        @JSONDescription("Makes this a light switch, with animates based on running_light and headlight.  Will not be visible if hasRunningLights and hasHeadlights are false.")
        CAR_LIGHT,
        @JSONDescription("Makes this a turn signal switch, which animates based on left_turn_signal and right_turn_signal.  Will not be visible if hasTurnSignals is false.")
        TURN_SIGNAL,
        @JSONDescription("Makes this a nav light switch, which animates based on the navigation_light variable.  Will not be visible if hasNavLights is false.")
        NAVIGATION_LIGHT,
        @JSONDescription("Makes this a strobe light switch, which animates based on the stobe_light variable.  Will not be visible if hasStobeLights is false.")
        STROBE_LIGHT,
        @JSONDescription("Makes this a taxi light switch, which animates based on the taxi_light variable.  Will not be visible if hasTaxiLights is false.")
        TAXI_LIGHT,
        @JSONDescription("Makes this a landing light switch, which animates based on the landing_light variable.  Will not be visible if hasLandingLights is false.")
        LANDING_LIGHT,
        @JSONDescription("Makes this a engine switch, which animates based on the engine state and can turn the engine on and off, and engage the starter.  The first switch present will be for the first engine, the second the second, unless hasSingleEngineControl is set, in which the switch will control all engines.  Will not be visible if the vehicle has no engines placed in it.")
        ENGINE_CONTROL,
        @JSONDescription("Makes this a engine magneto switch, which animates based on the engine state and can turn the engine on and off.  The first switch present will be for the first engine, the second the second, unless hasSingleEngineControl is set, in which the switch will control all engines.  Will not be visible if the vehicle has no engines placed in it.")
        ENGINE_ON,
        @JSONDescription("Makes this a engine starter switch, which animates based on the engine state and can engage the starter.  The first switch present will be for the first engine, the second the second, unless hasSingleEngineControl is set, in which the switch will control all engines.  Will not be visible if the vehicle has no engines placed in it.")
        ENGINE_START,
        @JSONDescription("Makes this a trailer switch, which animates based on if a connection is made or not.  The first switch present will be for the first connection, the second the second.  Will not be visible if the vehicle has no possible connections.")
        TRAILER,
        @JSONDescription("Makes this a custom variable switch, which animates based on the state of the custom variable.  The first switch present will be for the first variable, the second the second.  Will not be visible if the vehicle and all its parts have no customVariables.")
        CUSTOM_VARIABLE,
        @JSONDescription("Makes this a beacon box, which allows changing the selected beacon.  Will not be visible if hasRadioNav is false for the vehicle and allPlanesWithNav is false in the config.")
        BEACON_BOX,
        @JSONDescription("Makes this a roll trim button.  Clicking the left and right will adjust the trim in the limits.")
        ROLL_TRIM,
        @JSONDescription("Makes this a pitch trim button.  Clicking the left and right will adjust the trim in the limits.")
        PITCH_TRIM,
        @JSONDescription("Makes this a yaw trim button.  Clicking the left and right will adjust the trim in the limits.")
        YAW_TRIM;
    }

    public static enum ActionType {
        @JSONDescription("Toggles the value of the variable from 0 to 1 or 1 to 0.")
        TOGGLE,
        @JSONDescription("Sets the value of the variable to 'value'.")
        SET,
        @JSONDescription("Increments the variable by 'value', subject to the bounds in clampMin and clampMax.")
        INCREMENT;
    }
}
